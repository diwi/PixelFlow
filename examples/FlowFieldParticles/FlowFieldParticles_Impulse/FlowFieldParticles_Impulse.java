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



package FlowFieldParticles.FlowFieldParticles_Impulse;

import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge;

import processing.core.*;
import processing.opengl.PGraphics2D;




public class FlowFieldParticles_Impulse extends PApplet {
  
  //
  //
  // FlowFieldParticle demo showing how to add custom velocity fields
  // by simply drawing them into an offscreen graphics.
  //
  // Of course, additional velocity/impulse etc... can be applied in many ways.
  // For example by designing some custom shaders, or from any kind of image data.
  // E.g. see the optical flow example.
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
    pg_obstacles.rect(20, 20, width-40, height-40);
    pg_obstacles.rectMode(CENTER);
    pg_obstacles.fill(0, 255);
    pg_obstacles.rect(width/2, height/2-100, width-300, 20);
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
    particles.param.col_A = new float[]{0.20f, 0.40f, 0.80f, 5};
    particles.param.col_B = new float[]{0.10f, 0.20f, 0.40f, 0};
    particles.param.shader_collision_mult = 0.2f;
    particles.param.steps = 1;
    particles.param.velocity_damping  = 0.995f;
    particles.param.size_display   = 8;
    particles.param.size_collision = 8;
    particles.param.size_cohesion  = 8;
    particles.param.mul_coh = 4.00f;
    particles.param.mul_col = 1.00f;
    particles.param.mul_obs = 2.00f;
    
    
    resetParticles();

    frameRate(1000);
  }
  
  

  
  
  
  float impulse_max = 256;
  float impulse_mul = 25;
  float impulse_tsmooth = 0.80f;
  int   impulse_blur  = 0;
  
  public void addImpulse(){
    
    int w = width;
    int h = height;
    
    float vx, vy;
    final int mid = 127;

    // render "velocity"
    pg_impulse.beginDraw();
    pg_impulse.blendMode(REPLACE);
    pg_impulse.background(mid, mid, mid, 0);
    pg_impulse.noStroke();
    pg_impulse.rectMode(CENTER);
    
    // draw some impulse/acceleration fields
    // note: only the red and green channel is used for velocity.

    float tx = sin(frameCount*0.01f)*100;
    vx = 0;
    vy = 100;
    pg_impulse.pushMatrix();
    pg_impulse.translate(w/2-200 + tx, 0);
    pg_impulse.fill(mid+vx, mid+vy, 0);
    pg_impulse.rect(0, h-100, 100, 200);
    
    vx = 127;
    vy = 64;
    pg_impulse.fill(mid+vx, mid+vy, 0);
    pg_impulse.ellipse(0, h-400, 100, 100);
    
    pg_impulse.popMatrix();
    
    vx = 0;
    vy = 127;
    pg_impulse.fill(mid+vx, mid+vy, 0);
    pg_impulse.rect(w-50, h-200, 100, 400);
    
    vx = -127;
    vy = 64;
    pg_impulse.fill(mid+vx, mid+vy, 0);
    pg_impulse.rect(w-100, height/2-100, 200, 20);

    
    if(mousePressed){
      // impulse center/velocity
      float mx = mouseX;
      float my = mouseY;
      vx = (mouseX - pmouseX) * +impulse_mul;
      vy = (mouseY - pmouseY) * -impulse_mul; // flip vertically
      // clamp velocity
      float vv_sq = vx*vx + vy*vy;
      float vv_sq_max = impulse_max*impulse_max;
      if(vv_sq > vv_sq_max){
        vx = impulse_max * vx / sqrt(vv_sq);
        vy = impulse_max * vy / sqrt(vv_sq);
      }
      // map velocity, to UNSIGNED_BYTE range
      vx = 127 * vx / impulse_max;
      vy = 127 * vy / impulse_max;
      if(vv_sq != 0){
        pg_impulse.fill(mid+vx, mid+vy, 0);
        pg_impulse.ellipse(mx, my, 100, 100);
      }
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
      Merge.TexMad ta = new Merge.TexMad(ff_impulse.tex_vel, 3, 0);
      Merge.TexMad tb = new Merge.TexMad(pg_gravity, -0.035f, 0);
      DwFilter.get(context).merge.apply(ff_acc.tex_vel, ta, tb);
    }
  }
  


  public void draw(){
    
    particles.param.timestep = 1f/frameRate;
    
    spawnParticles();

    addImpulse();
    
    // update particle simulation
    particles.update(ff_acc);
    
    // render obstacles + particles
    pg_canvas.beginDraw(); 
    pg_canvas.background(255);
    pg_canvas.image(pg_impulse, 0, 0);
    pg_canvas.image(pg_obstacles, 0, 0);
    pg_canvas.endDraw();
    particles.displayParticles(pg_canvas);

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
    px = 50;
    py = vh-50;
    vx = 5;
    vy = 20;
    
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
    if(key == 'r') resetParticles();
  }
  
  public void resetParticles(){
    particles.resizeWorld(width, height); 
    particles.reset();
    particles.createObstacleFlowField(pg_obstacles, new int[]{0,0,0,255}, false);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { FlowFieldParticles_Impulse.class.getName() });
  }

}