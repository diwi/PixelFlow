/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package FlowFieldParticles_BasicMouseImpulse;

import java.util.Locale;

import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureUtils;
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles;
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles.SpawnRadial;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge.TexMad;
import processing.core.*;
import processing.opengl.PGraphics2D;



public class FlowFieldParticles_BasicMouseImpulse extends PApplet {
  
  //
  //
  // Basic starter demo for a FlowFieldParticle simulation.
  //
  // Gravity, Particle Spawning, Animated Obstacles, Mouse Impulse
  //
  // --- controls ----
  // LMB       ... spawn particles
  // mousedrag ... add velocity impulse
  // 'r'       ... reset Scene
  //
  //
  
  int viewport_w = 1680;
  int viewport_h = 1024;
  int viewport_x = 230;
  int viewport_y = 0;
  
 
  PGraphics2D pg_canvas;
  PGraphics2D pg_obstacles;
  PGraphics2D pg_gravity;
  PGraphics2D pg_impulse;

  DwPixelFlow context;
  
  DwFlowFieldParticles particles;
  DwFlowField ff_acc;
  DwFlowField ff_impulse;
  
  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(0);
  }
  

  public void setup(){
    surface.setLocation(viewport_x, viewport_y);

    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    
    particles = new DwFlowFieldParticles(context, 1024 * 1024);
    particles.param.col_A = new float[]{1.00f, 0.30f, 0.15f, 5};
    particles.param.col_B = new float[]{0.50f, 0.15f, 0.07f, 0};
    particles.param.shader_collision_mult = 0.2f;
    particles.param.steps = 2;
    particles.param.velocity_damping  = 0.99f;
    particles.param.size_display   = 8;
    particles.param.size_collision = 8;
    particles.param.size_cohesion  = 4;
    particles.param.mul_coh = 2.00f;
    particles.param.mul_col = 1.00f;
    particles.param.mul_obs = 2.00f;
    
    
    ff_acc = new DwFlowField(context);
    ff_acc.param.blur_iterations = 0;
    ff_acc.param.blur_radius     = 1;
       
    ff_impulse = new DwFlowField(context);
    ff_impulse.param.blur_iterations = 1;
    ff_impulse.param.blur_radius     = 1;
    
    
    pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
    pg_canvas.smooth(0);
    
    pg_obstacles = (PGraphics2D) createGraphics(width, height, P2D);
    pg_obstacles.smooth(8);

    pg_impulse = (PGraphics2D) createGraphics(width, height, P2D);
    pg_impulse.smooth(0);
    DwGLTextureUtils.changeTextureFormat(pg_impulse, GL3.GL_RGBA16_SNORM, GL3.GL_RGBA, GL3.GL_FLOAT);

    pg_gravity = (PGraphics2D) createGraphics(width, height, P2D);
    pg_gravity.smooth(0);
    pg_gravity.beginDraw();
    pg_gravity.blendMode(REPLACE);
    pg_gravity.background(0, 255, 0);
    pg_gravity.endDraw();

    frameRate(1000);
  }
  
  

  
  
  
  
  
  float impulse_max = 256;
  float impulse_mul = 15;
  float impulse_tsmooth = 0.90f;
  int   impulse_blur  = 0;
  
  public void addImpulse(){
    
    int w = width;
    int h = height;
    
    // impulse center/velocity
    float mx = mouseX;
    float my = mouseY;
    float vx = (mouseX - pmouseX) * +impulse_mul;
    float vy = (mouseY - pmouseY) * -impulse_mul; // flip vertically
    // clamp velocity
    float vv_sq = vx*vx + vy*vy;
    float vv_sq_max = impulse_max*impulse_max;
    if(vv_sq > vv_sq_max){
      vx = impulse_max * vx / sqrt(vv_sq);
      vy = impulse_max * vy / sqrt(vv_sq);
    }
    // map velocity, to UNSIGNED_BYTE range
    vx = map(vx, -impulse_max, +impulse_max, 0, 256);
    vy = map(vy, -impulse_max, +impulse_max, 0, 256);
    // render "velocity"
    pg_impulse.beginDraw();
    pg_impulse.clear();
    pg_impulse.blendMode(BLEND);
    pg_impulse.background(127.5f, 127.5f, 127.5f, 255);
    pg_impulse.noStroke();
    if(mousePressed){
      pg_impulse.fill(vx, vy, 0, 255);
      pg_impulse.ellipse(mx, my, 100, 100);
    }
    pg_impulse.endDraw();
    
    
    // create impulse texture
    ff_impulse.resize(w, h);
    {
      TexMad ta = new TexMad(ff_impulse.tex_vel, impulse_tsmooth, 0);
      TexMad tb = new TexMad(pg_impulse,  1, -0.5f); // -0.5f ... -127.5f
      DwFilter.get(context).merge.apply(ff_impulse.tex_vel, ta, tb);
      ff_impulse.blur(1, impulse_blur);
    }
    
    
    // create acceleration texture
    ff_acc.resize(w, h);
    {
      TexMad ta = new TexMad(ff_impulse.tex_vel, 1, 0);
      TexMad tb = new TexMad(pg_gravity, -0.05f, 0);
      DwFilter.get(context).merge.apply(ff_acc.tex_vel, ta, tb);
    }
  }
  


  public void draw(){
    
    updateScene();
 
    spawnParticles();

    addImpulse();
    
    // update particle simulation
    particles.resizeWorld(width, height); 
    particles.createObstacleFlowField(pg_obstacles, new int[]{0,0,0,255}, false);
    particles.update(ff_acc);
    
    // render obstacles + particles
    pg_canvas.beginDraw(); 
    pg_canvas.background(255);
    pg_canvas.image(pg_obstacles, 0, 0);
    pg_canvas.endDraw();
    particles.display(pg_canvas);

    blendMode(REPLACE);
    image(pg_canvas, 0, 0);
    blendMode(BLEND);
    
    String txt_fps = String.format(Locale.ENGLISH, "[%s]   [%7.2f fps]   [particles %,d] ",  getClass().getSimpleName(), frameRate, particles.getCount() );
    surface.setTitle(txt_fps);
  }
  

  void updateScene(){

    int w = pg_obstacles.width;
    int h = pg_obstacles.height;
    float dim = h/2f;
    
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    pg_obstacles.noStroke();
    pg_obstacles.blendMode(REPLACE);
    pg_obstacles.rectMode(CORNER);

    // border
    pg_obstacles.fill(0, 255);
    pg_obstacles.rect(0, 0, w, h);
    pg_obstacles.fill(0, 0);
    pg_obstacles.rect(25, 25, w-50, h-50);

    // animated obstacles
    pg_obstacles.rectMode(CENTER);
    pg_obstacles.pushMatrix();
    {
      float px = sin(frameCount/120f) * 0.8f * w/2;
      pg_obstacles.translate(w/2 + px, h-dim/2);
      pg_obstacles.fill(0, 255);
      pg_obstacles.rect(0, 0,  30, dim);
    }
    pg_obstacles.popMatrix();
    pg_obstacles.endDraw();
    
  }
  


  public void spawnParticles(){
    
    float px,py,vx,vy,radius;
    int count, vw, vh;
    
    vw = width;
    vh = height;

    count = 1;
    radius = 10;
    px = vw/2f;
    py = vh/4f;
    vx = 0;
    vy = 4;
    
    SpawnRadial sr = new SpawnRadial();
    sr.num(count);
    sr.dim(radius, radius);
    sr.pos(px, vh-1-py);
    sr.vel(vx, vy);
    particles.spawn(vw, vh, sr);

    if(mousePressed && mouseButton == LEFT){     
      count = ceil(particles.getCount() * 0.01f);
      count = min(max(count, 1), 10000);  
      radius = ceil(sqrt(count));
      px = mouseX;
      py = mouseY;
      vx = 0;
      vx = 0;
      
      sr.num(count);
      sr.dim(radius, radius);
      sr.pos(px, vh-1-py);
      sr.vel(vx, vy);
      particles.spawn(vw, vh, sr);
    }
  }
  
  
  
  public void keyReleased(){
    if(key == 'r') particles.reset();
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { FlowFieldParticles_BasicMouseImpulse.class.getName() });
  }

}