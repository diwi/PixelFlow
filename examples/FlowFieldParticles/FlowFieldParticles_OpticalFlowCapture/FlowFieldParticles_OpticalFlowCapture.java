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



package FlowFieldParticles.FlowFieldParticles_OpticalFlowCapture;

import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;



public class FlowFieldParticles_OpticalFlowCapture extends PApplet {
  
  //
  // Particle Simulation + Optical Flow.
  // The optical flow of a webcam capture is used as acceleration for the particles.
  //
  
  int cam_w = 640;
  int cam_h = 480;
  
  int viewp_w = 1280;
  int viewp_h = (int) (viewp_w * cam_h/(float)cam_w);

  Capture cam;

  PGraphics2D pg_canvas;
  PGraphics2D pg_obstacles;
  PGraphics2D pg_cam; 
  
  DwPixelFlow context;
  DwOpticalFlow opticalflow;
  DwFlowFieldParticles particles;

  DwFlowFieldParticles.SpawnRect spawn = new DwFlowFieldParticles.SpawnRect();

  public void settings() {
    size(viewp_w, viewp_h, P2D);
    smooth(0);
  }
  
  public void setup(){
    surface.setLocation(230, 0);
    
    // capturing
    cam = new Capture(this, cam_w, cam_h, 30);
    cam.start();
    
    pg_cam = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_cam.smooth(0);
    pg_cam.beginDraw();
    pg_cam.background(0);
    pg_cam.endDraw();
    
    pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
    pg_canvas.smooth(0);
    
    int border = 20;
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
    pg_obstacles.rect(border/2, border/2, width-border, height-border);
    pg_obstacles.endDraw();
    
    
    // library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // optical flow 
    opticalflow = new DwOpticalFlow(context, cam_w, cam_h);
    opticalflow.param.grayscale = true;
    
//    border = 120;
    float dimx = width  - border;
    float dimy = height - border;
    
    int particle_size = 6;
    int numx = (int) (dimx / (0.9f*particle_size));
    int numy = (int) (dimy / (0.9f*particle_size));

    // particle spawn-def, rectangular shape
    spawn.num(numx, numy);
    spawn.dim(dimx, dimy);
    spawn.pos(width/2-dimx/2, height/2-dimy/2);
    spawn.vel(0, 0);
    
    // partcle simulation
    particles = new DwFlowFieldParticles(context, numx * numy);
    // particles.param.col_A = new float[]{0.40f, 0.80f, 0.10f, 3};
    // particles.param.col_B = new float[]{0.20f, 0.40f, 0.05f, 0};
    // particles.param.col_B = new float[]{0.80f, 0.40f, 0.80f, 0};
    particles.param.col_A = new float[]{0.25f, 0.50f, 1.00f, 3};
    particles.param.col_B = new float[]{0.25f, 0.10f, 0.00f, 0};
    particles.param.shader_type = 1;
    particles.param.shader_collision_mult = 0.4f;
    particles.param.steps = 1;
    particles.param.velocity_damping  = 0.999f;
    particles.param.size_display   = ceil(particle_size * 1.5f);
    particles.param.size_collision = particle_size;
    particles.param.size_cohesion  = particle_size;
    particles.param.mul_coh = 0.20f;
    particles.param.mul_col = 1.00f;
    particles.param.mul_obs = 2.00f;
    particles.param.mul_acc = 0.10f; // optical flow multiplier
    particles.param.wh_scale_obs = 0;
    particles.param.wh_scale_coh = 5;
    particles.param.wh_scale_col = 0;
    
    // init stuff that doesn't change
    particles.resizeWorld(width, height); 
    particles.spawn(width, height, spawn);
    particles.createObstacleFlowField(pg_obstacles, new int[]{0,0,0,255}, false);
    
    frameRate(1000);
  }



  public void draw(){
    
    // capture camera frame + optical flow
    if(cam.available()){
      cam.read();
      
      pg_cam.beginDraw();
      pg_cam.image(cam, 0, 0);
      pg_cam.endDraw();
      
      // apply any filters
      DwFilter.get(context).luminance.apply(pg_cam, pg_cam);

      // compute Optical Flow
      opticalflow.update(pg_cam);
    }
    
    particles.param.timestep = 1f/frameRate;
    
    // update particles, using the opticalflow for acceleration
    particles.update(opticalflow.frameCurr.velocity);
    
    // render obstacles + particles
    pg_canvas.beginDraw(); 
    pg_canvas.image(pg_cam, 0, 0, width, height);
    pg_canvas.image(pg_obstacles, 0, 0);
    pg_canvas.endDraw();
    particles.displayParticles(pg_canvas);

    // display result
    image(pg_canvas, 0, 0);

    String txt_fps = String.format(Locale.ENGLISH, "[%s]   [%7.2f fps]   [particles %,d] ",  getClass().getSimpleName(), frameRate, particles.getCount() );
    surface.setTitle(txt_fps);
  }
  
  public void keyReleased(){
    particles.spawn(width, height, spawn);
  }
  

  public static void main(String args[]) {
    PApplet.main(new String[] { FlowFieldParticles_OpticalFlowCapture.class.getName() });
  }

}