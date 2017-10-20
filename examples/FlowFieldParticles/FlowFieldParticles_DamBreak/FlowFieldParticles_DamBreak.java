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



package FlowFieldParticles.FlowFieldParticles_DamBreak;

import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.core.*;
import processing.opengl.PGraphics2D;



public class FlowFieldParticles_DamBreak extends PApplet {
  
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
  
  boolean START_FULLSCREEN = !true;
  int viewport_w = 1680;
  int viewport_h = 1024;
//  int viewport_w = 1280;
//  int viewport_h = 720;
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
  
  public boolean UPDATE_PHYSICS = true;
  public boolean DISPLAY_DIST   = false;
  public boolean DISPLAY_FLOW   = false;
  
  public void settings() {
    if(START_FULLSCREEN){
      viewport_w = displayWidth;
      viewport_h = displayHeight;
      viewport_x = 0;
      viewport_y = 0;
      fullScreen(P2D);
    } else {
      viewport_w = (int) min(viewport_w, displayWidth  * 0.9f);
      viewport_h = (int) min(viewport_h, displayHeight * 0.9f);
      size(viewport_w, viewport_h, P2D);
    }
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
    pg_obstacles.rect(10, 10, width-20, height-20);
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
    particles.param.col_A = new float[]{0.80f, 0.10f, 0.20f, 5};
    particles.param.col_B = new float[]{0.20f, 0.05f, 0.10f, 0};
    particles.param.shader_type = 1;
    particles.param.shader_collision_mult = 0.30f;
    particles.param.steps = 1;
    particles.param.velocity_damping  = 1;
    particles.param.size_display   = 5;
    particles.param.size_collision = 5;
    particles.param.size_cohesion  = 5;
    particles.param.mul_coh = 1.00f;
    particles.param.mul_col = 2.00f;
    particles.param.mul_obs = 3.00f;
    
    particles.param.wh_scale_col =  0;
    particles.param.wh_scale_coh =  4;
    particles.param.wh_scale_obs =  0;
        
    
  //  PFont font = createDefaultFont(12);
    PFont font = createFont("Arial", 12, false);
    textFont(font);
    
    reset();

    frameRate(1000);
//    frameRate(120);
  }
  
  
  public void setTimestep(){
    particles.param.timestep = 1f/(frameRate);
//    particles.param.timestep = 1f/120;
  }

  
  
  
  float impulse_max = 256;
  float impulse_mul = 15;
  float impulse_tsmooth = 0.90f;
  int   impulse_blur  = 0;
  float impulse_size = 60;
  public void addImpulse(){
    
    impulse_size = min(width, height) / 10f;
    
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
      pg_impulse.ellipse(mx, my, impulse_size, impulse_size);
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
      Merge.TexMad tb = new Merge.TexMad(pg_gravity, -0.08f, 0);
      DwFilter.get(context).merge.apply(ff_acc.tex_vel, ta, tb);
    }
  }
  
  
  float[][] pallette = {
      { 32,  32, 32},
      {196,  96,  0},
      { 96,  96, 96},
      {  0,  96,164},
      { 96,  96, 96},
  };
  
//  float[][] pallette = {
//      {255,  0, 128},
//      {255, 128,  0},
//      {  0, 128,255},
//      {128,  0,255},
//  };
  
  
  public void updateColor(){
//    float mix = sin(frameCount*0.001f) * 0.5f + 0.5f;
    float mix = map(mouseX, 0, width, 0, 1);
    float[] rgb1 = DwUtils.getColor(pallette, mix, null);
    float s1 = 1f/255f;
    float s2 = s1 * 0.25f;
    particles.param.col_A = new float[]{rgb1[0] * s1, rgb1[1] * s1, rgb1[2] * s1, 1.0f};
    particles.param.col_B = new float[]{rgb1[0] * s2, rgb1[1] * s2, rgb1[2] * s2, 0.0f};
  }


  public void draw(){
    updateColor();

    
    if(UPDATE_PHYSICS){
      setTimestep();
      
      spawnParticles();
  
      addImpulse();
      
      // update particle simulation
      particles.update(ff_acc);
    }
    
    
    
    if(!DISPLAY_DIST && !DISPLAY_FLOW){
      // render obstacles and particles
      pg_canvas.beginDraw(); 
      pg_canvas.background(255);
      pg_canvas.image(pg_obstacles, 0, 0);
      pg_canvas.endDraw();
      particles.displayParticles(pg_canvas);
    } else {
      pg_canvas.beginDraw(); 
      pg_canvas.background(0);
      pg_canvas.endDraw();
    }


    if(DISPLAY_DIST){
      int Z = DwGLTexture.SWIZZLE_0;
      int R = DwGLTexture.SWIZZLE_R;
      int G = DwGLTexture.SWIZZLE_G;
      int B = DwGLTexture.SWIZZLE_B;
      int A = DwGLTexture.SWIZZLE_A;
      int[] RGBA = {R,G,B,A};
      
      Merge.TexMad texA = new Merge.TexMad(particles.tex_obs_dist, 0.030f * particles.param.mul_obs, 0.0f);
      Merge.TexMad texB = new Merge.TexMad(particles.tex_col_dist, 0.500f * particles.param.mul_col, 0.0f);
      Merge.TexMad texC = new Merge.TexMad(particles.tex_coh_dist, 0.005f * particles.param.mul_coh, 0.0f);
      
      texA.mul *= 1<<particles.param.wh_scale_obs;
//      texB.mul *= 1<<particles.param.wh_scale_col;
//      texC.mul *= 1<<particles.param.wh_scale_coh;
      
      particles.tex_obs_dist.swizzle(new int[]{R, R, R, Z});
      particles.tex_col_dist.swizzle(new int[]{R, R, R, Z});
      particles.tex_coh_dist.swizzle(new int[]{R, Z, Z, Z});
      
      DwFilter.get(context).merge.apply(pg_canvas, texA, texB, texC);
      
      particles.tex_coh_dist.swizzle(RGBA);
      particles.tex_col_dist.swizzle(RGBA);
      particles.tex_obs_dist.swizzle(RGBA);
    }

    if(DISPLAY_FLOW){
      particles.ff_sum.param.line_spacing = 8;
      particles.ff_sum.param.line_width   = 0.8f;
      particles.ff_sum.param.line_scale   = 1.5f;
      particles.ff_sum.param.line_shading = 0;
      particles.ff_sum.displayPixel(pg_canvas);
      particles.ff_sum.displayLines(pg_canvas);
    }
    
    
    
    
    
    blendMode(REPLACE);
    image(pg_canvas, 0, 0);
    blendMode(BLEND);
    
    String txt_fps = String.format(Locale.ENGLISH, "[%s]   [%7.2f fps]   [particles %,d] ",  getClass().getSimpleName(), frameRate, particles.getCount() );
    surface.setTitle(txt_fps);
    
    fill(0);
    text(txt_fps, 15, 25);
  }
  


  


  public void spawnParticles(){
    
    float px,py,vx,vy,radius;
    int count, vw, vh;
    float vel = 0f;
    
    vw = width;
    vh = height;

    count = 1;
    radius = 10;
    px = vw/2f;
    py = vh/4f;
    vx = 0;
    vy = 4;
    
    DwFlowFieldParticles.SpawnRadial sr = new DwFlowFieldParticles.SpawnRadial();
//    sr.num(count);
//    sr.dim(radius, radius);
//    sr.pos(px, vh-1-py);
//    sr.vel(vx, vy);
//    particles.spawn(vw, vh, sr);

    if(mousePressed && mouseButton == LEFT){     
      count = ceil(particles.getCount() * 0.0025f);
      count = min(max(count, 1), 5000);
      
      float pr = particles.getCollisionSize() * 0.25f;
      radius = ceil(sqrt(count * pr * pr));
      px = mouseX;
      py = mouseY;
      vx = (mouseX-pmouseX) * +vel;
      vy = (mouseY-pmouseY) * -vel;
      
      sr.num(count);
      sr.dim(radius, radius);
      sr.pos(px, vh-1-py);
      sr.vel(vx, vy);
      particles.spawn(vw, vh, sr);
    }
  }
  
  public void reset(){
    particles.reset();
    particles.resizeWorld(width, height);
    particles.createObstacleFlowField(pg_obstacles, new int[]{0,0,0,255}, false);
    float border = 100;
    float dimx = width  - border;
    float dimy = height - border;
    float particle_size = particles.param.size_collision;
    int numx = (int) (dimx / (particle_size+0.1f));
    int numy = (int) (dimy / (particle_size+0.1f));
    
    
    DwFlowFieldParticles.SpawnRect spawn = new DwFlowFieldParticles.SpawnRect();
    spawn.dim(dimx, dimy);
    spawn.pos(width/2-dimx/2, height/2-dimy/2);
    spawn.vel(0, 0);
    
    spawn.num(numx, numy);
    
    particles.spawn(width, height, spawn);
  }
  

  
  public void keyReleased(){
    if(key == 'r') reset();
    if(key == 't') UPDATE_PHYSICS = !UPDATE_PHYSICS;
    if(key == '1') DISPLAY_DIST   = !DISPLAY_DIST;
    if(key == '2') DISPLAY_FLOW   = !DISPLAY_FLOW;
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { FlowFieldParticles_DamBreak.class.getName() });
  }

}