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



package FlowFieldParticles_MouseImpulse;

import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge;

import processing.core.*;
import processing.opengl.PGraphics2D;



public class FlowFieldParticles_MouseImpulse extends PApplet {
  
  //
  //
  // FlowFieldParticles Demo that shows how to add custom mouse-interactions.
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
    viewport_w = (int) min(viewport_w, displayWidth  * 0.9f);
    viewport_h = (int) min(viewport_h, displayHeight * 0.9f);
    size(viewport_w, viewport_h, P2D);
    smooth(0);
  }
  

  public void setup(){
    surface.setLocation(viewport_x, viewport_y);

    pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
    pg_canvas.smooth(0);
    
    pg_impulse = (PGraphics2D) createGraphics(width, height, P2D);
    pg_impulse.smooth(0);

    pg_gravity = (PGraphics2D) createGraphics(width, height, P2D);
    pg_gravity.smooth(0);
    pg_gravity.beginDraw();
    pg_gravity.blendMode(REPLACE);
    pg_gravity.background(0, 255, 0);
    pg_gravity.endDraw();
    

    pg_obstacles = (PGraphics2D) createGraphics(width, height, P2D);
    pg_obstacles.smooth(0);
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    pg_obstacles.noStroke();
    pg_obstacles.blendMode(REPLACE);
    pg_obstacles.rectMode(CORNER);
    pg_obstacles.fill(0, 255);
    pg_obstacles.rect(0, 0, width, height);
    pg_obstacles.fill(0, 0);
    pg_obstacles.rect(25, 25, width-50, height-50);
    pg_obstacles.endDraw();
    
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    ff_acc = new DwFlowField(context);
    ff_acc.param.blur_iterations = 0;
    ff_acc.param.blur_radius     = 1;
       
    ff_impulse = new DwFlowField(context);
    ff_impulse.param.blur_iterations = 1;
    ff_impulse.param.blur_radius     = 1;

    
    particles = new DwFlowFieldParticles(context, 1024 * 1024);
    particles.param.col_A = new float[]{0.80f, 0.30f, 0.15f, 5};
    particles.param.col_B = new float[]{0.40f, 0.15f, 0.07f, 0};
    particles.param.shader_collision_mult = 0.2f;
    particles.param.steps = 2;
    particles.param.velocity_damping  = 0.995f;
    particles.param.size_display   = 5;
    particles.param.size_collision = 4;
    particles.param.size_cohesion  = 0;
    particles.param.mul_coh = 0.00f;
    particles.param.mul_col = 1.00f;
    particles.param.mul_obs = 2.00f;
    
    
    particles.resizeWorld(width, height); 
    particles.createObstacleFlowField(pg_obstacles, new int[]{0,0,0,255}, false);

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
    final int mid = 127;
    vx = map(vx, -impulse_max, +impulse_max, 0, mid<<1);
    vy = map(vy, -impulse_max, +impulse_max, 0, mid<<1);
    // render "velocity"
    pg_impulse.beginDraw();
    pg_impulse.background(mid, mid, mid);
    pg_impulse.noStroke();
    if(mousePressed){
      pg_impulse.fill(vx, vy, mid);
      pg_impulse.ellipse(mx, my, 100, 100);
    }
    pg_impulse.endDraw();
    
    
    // create impulse texture
    ff_impulse.resize(w, h);
    {
      Merge.TexMad ta = new Merge.TexMad(ff_impulse.tex_vel, impulse_tsmooth, 0);
      Merge.TexMad tb = new Merge.TexMad(pg_impulse,  1, -mid/255f);
      DwFilter.get(context).merge.apply(ff_impulse.tex_vel, ta, tb);
      ff_impulse.blur(1, impulse_blur);
    }
    
    
    // create acceleration texture
    ff_acc.resize(w, h);
    {
      Merge.TexMad ta = new Merge.TexMad(ff_impulse.tex_vel, 1, 0);
      Merge.TexMad tb = new Merge.TexMad(pg_gravity, -0.05f, 0);
      DwFilter.get(context).merge.apply(ff_acc.tex_vel, ta, tb);
    }
  }
  


  public void draw(){
    
    spawnParticles();

    addImpulse();
    
    // update particle simulation
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
    
    DwFlowFieldParticles.SpawnRadial sr = new DwFlowFieldParticles.SpawnRadial();
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
      vy = 0;
      
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
    PApplet.main(new String[] { FlowFieldParticles_MouseImpulse.class.getName() });
  }

}