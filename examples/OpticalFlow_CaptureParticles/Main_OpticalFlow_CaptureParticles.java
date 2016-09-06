/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package OpticalFlow_CaptureParticles;



import controlP5.Accordion;
import controlP5.CheckBox;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;
import thomasdiewald.pixelflow.java.OpticalFlow;
import thomasdiewald.pixelflow.java.PixelFlow;
import thomasdiewald.pixelflow.java.filter.Filter;


public class Main_OpticalFlow_CaptureParticles extends PApplet {
 
  
  int cam_w = 640;
  int cam_h = 480;
  
  int view_w = 1200;
  int view_h = (int)(view_w * cam_h/(float)cam_w);
  
  int gui_w = 200;
  
  //main library context
  PixelFlow context;
  
  // optical flow
  OpticalFlow opticalflow;
  
  // buffer for the capture-image
  PGraphics2D pg_cam_a, pg_cam_b; 

  // offscreen render-target
  PGraphics2D pg_oflow;
  
  // camera capture (video library)
  Capture cam;
  
  // some state variables for the GUI/display
  boolean APPLY_GRAYSCALE = true;
  boolean APPLY_BILATERAL = true;
  int     VELOCITY_LINES  = 6;
  

  
  // Airballs
  public int   NUM_BALLS = 500;
  public float BALL_SCREEN_FILL_FACTOR = 0.20f;
  public int   BALL_SHADING = 255;
  
  public Ball[] balls;
  
    
  public void settings() {
    size(view_w + gui_w, view_h, P2D);
    smooth(4);
  }

  public void setup() {

    // main library context
    context = new PixelFlow(this);
    context.print();
    context.printGL();
    
    // optical flow
    opticalflow = new OpticalFlow(context, cam_w, cam_h);
    
    // optical flow parameters
    opticalflow.param.display_mode = 3;
    
    // ball parameters
    NUM_BALLS                 = 3000;
    BALL_SCREEN_FILL_FACTOR   = 0.25f;
    BALL_SHADING              = 200;
    Ball.GRAVITY              = 0;
    Ball.COLLISION_DAMPING    = 0.60f;
    Ball.COLLISION_SPRING     = 0.60f;
    Ball.FLUID_SCALE          = 0.15f;
    Ball.FLUID_INERTIA        = 0.50f;
    Ball.FLUID_DISSIPATION    = 0.60f;
    Ball.VELOCITY_DISSIPATION = 0.95f;
    
//    String[] cameras = Capture.list();
//    printArray(cameras);
//    cam = new Capture(this, cameras[0]);
    
    cam = new Capture(this, cam_w, cam_h, 30);
    cam.start();
    
    pg_cam_a = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_cam_a.noSmooth();
    
    pg_cam_b = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_cam_b.noSmooth();
    
    pg_oflow = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg_oflow.smooth(4);
    
    
    initBalls();

    createGUI();
      
    background(0);
    frameRate(60);
  }
  


  
  public void initBalls(){
    Ball.MAX_RAD = 0;
    balls = new Ball[NUM_BALLS];
    
    float radius = sqrt((view_w * view_h * BALL_SCREEN_FILL_FACTOR) / NUM_BALLS) * 0.5f;
    float r_min = radius * 0.8f;
    float r_max = radius * 1.2f;
    
    randomSeed(0);
    for (int i = 0; i < NUM_BALLS; i++) {
      float rad = random(r_min, r_max);
      float px = random(10 + 2 * rad, view_w - 2 * rad - 10);
      float py = random(10 + 2 * rad, view_h - 2 * rad - 10);
      balls[i] = new Ball(px, py, rad, i);
    }
    
    balls[0].rad *= 2;
  }
  
  
  // float buffer for pixel transfer from OpenGL to the host application
  float[] fluid_velocity = new float[cam_w * cam_h * 2];

  public void draw() {
    
    if( cam.available() ){
      cam.read();
      
      // render to offscreenbuffer
      pg_cam_a.beginDraw();
      pg_cam_a.image(cam, 0, 0);
      pg_cam_a.endDraw();

      
      // apply filters (not necessary)
      if(APPLY_GRAYSCALE){
        Filter.get(context).luminance.apply(pg_cam_a, pg_cam_a);
      }
      if(APPLY_BILATERAL){
        Filter.get(context).bilateral.apply(pg_cam_a, pg_cam_b, 5, 0.10f, 4);
        swapCamBuffer();
      }
      
      // update Optical Flow
      opticalflow.update(pg_cam_a);
      
      
      // render Optical Flow
      pg_oflow.beginDraw();
      pg_oflow.clear();
      pg_oflow.image(pg_cam_a, 0, 0, view_w, view_h);
      pg_oflow.endDraw();

      // add flow-vectors to the image
      if(opticalflow.param.display_mode == 2){
        opticalflow.renderVelocityShading(pg_oflow);
      }
      opticalflow.renderVelocityStreams(pg_oflow, VELOCITY_LINES);
      
      
      // Transfer velocity data from the GPU to the host-application
      // This is in general a bad idea because such operations are very slow. So 
      // either do everything in shaders, and avoid memory transfer when possible, 
      // or do it very rarely. however, this is just an example for convenience.
      fluid_velocity = opticalflow.getVelocity(fluid_velocity);
       
    }
    
    
    
    // add a force to ball[0] with the middle mousebutton
    if(mousePressed && mouseButton == CENTER){
      Ball ball = balls[0];
      
      float dx = mouseX - ball.x;
      float dy = mouseY - ball.y;
      
      float damping_pos = 0.2f;
      float damping_vel = 0.2f;
      
      ball.x  += dx * damping_pos;
      ball.y  += dy * damping_pos;
      
      ball.vx += dx * damping_vel;
      ball.vy += dy * damping_vel;
    }
    
     
    
    // update step: ball motion
    // 1) solve collisions between all balls
    // 2) add fluid velocity to the balls' velocity
    // 3) add gravity
    // 4) update final velocity + position
    for (Ball ball : balls) {

      int px_view = Math.round(ball.x);
      int py_view = Math.round(height - 1 - ball.y); // invert y
      
      float scale_X = view_w / (float) cam_w;
      float scale_Y = view_h / (float) cam_h;
      
      int px_grid = (int)(px_view / scale_X);
      int py_grid = (int)(py_view / scale_Y);

      int w_grid  = opticalflow.frameCurr.velocity.w;

      int PIDX    = py_grid * w_grid + px_grid;

      float fluid_vx = +fluid_velocity[PIDX * 2 + 0];
      float fluid_vy = -fluid_velocity[PIDX * 2 + 1]; // invert y
      
      ball.applyCollisions(balls);
      ball.applyGravity();
      ball.applyFLuid(fluid_vx, fluid_vy);
      ball.updatePosition(0, 0, view_w, view_h);
    }
    

    // display result
    background(0);
    image(pg_oflow, 0, 0);
    
    
    // draw Balls
    PGraphics pg = this.g;
    pg.blendMode(BLEND);
    for (int i = 0; i < balls.length; i++) {
      
      Ball ball = balls[i];
   
      float vlen = sqrt(ball.vx*ball.vx + ball.vy*ball.vy);
      float dx = (ball.rad-2) * ball.vx / vlen;
      float dy = (ball.rad-2) * ball.vy / vlen;
       
      // draw velocity
      pg.stroke(255);
      pg.strokeWeight(1);
      pg.line(ball.x, ball.y, ball.x - dx, ball.y - dy);
      
      // draw ball
      pg.noStroke();
      if(i == 0){
        pg.fill(BALL_SHADING + vlen*100, vlen, 0, 200);
      } else {
        pg.fill(vlen*100, BALL_SHADING*0.5f, BALL_SHADING, 200);
      }
      ball.display(pg);  
    }
    
    
   
   
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", cam_w, cam_h, opticalflow.UPDATE_STEP, frameRate);
    surface.setTitle(txt_fps);
   
  }
  
  
 
  void swapCamBuffer(){
    PGraphics2D tmp = pg_cam_a;
    pg_cam_a = pg_cam_b;
    pg_cam_b = tmp;
  }
  
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    Group group_oflow = cp5.addGroup("OpticalFlow")
    .setPosition(view_w, 20).setHeight(20).setWidth(gui_w)
    .setBackgroundHeight(380).setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_oflow.getCaptionLabel().align(LEFT, CENTER);
    
    int sx = 100, sy = 14;
    int px = 10, py = 20, oy = (int)(sy*1.5f);
    
    cp5.addSlider("blur input").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 30).setValue(opticalflow.param.blur_input)
    .plugTo(opticalflow.param, "blur_input").linebreak();
    
    cp5.addSlider("blur flow").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 10).setValue(opticalflow.param.blur_flow)
    .plugTo(opticalflow.param, "blur_flow").linebreak();
    
    cp5.addSlider("temporal smooth").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1).setValue(opticalflow.param.temporal_smoothing)
    .plugTo(opticalflow.param, "temporal_smoothing").linebreak();
    
    cp5.addSlider("flow scale").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 200f).setValue(opticalflow.param.flow_scale)
    .plugTo(opticalflow.param, "flow_scale").linebreak();

    cp5.addSlider("threshold").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 2.0f).setValue(opticalflow.param.threshold)
    .plugTo(opticalflow.param, "threshold").linebreak();
    
    cp5.addSpacer("display").setGroup(group_oflow).setPosition(px, py+=oy);

    CheckBox cb = cp5.addCheckBox("activeFilters").setGroup(group_oflow).setSize(18, 18).setPosition(px, py+=oy)
    .setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
    .addItem("grayscale"       , 0)
    .addItem("bilateral filter", 0)
    ;
    
    if(APPLY_GRAYSCALE) cb.activate(0);
    if(APPLY_BILATERAL) cb.activate(1);
    
    cp5.addSlider("line density").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=(int)(oy*2.5))
    .setRange(1, 10).setValue(VELOCITY_LINES)
    .plugTo(this, "VELOCITY_LINES").linebreak();

    cp5.addRadio("setDisplayMode").setGroup(group_oflow).setSize(18, 18).setPosition(px, py+=oy)
        .setSpacingColumn(40).setSpacingRow(2).setItemsPerRow(3)
        .addItem("dir", 0)
        .addItem("normal", 1)
        .addItem("Shading", 2)
        .activate(opticalflow.param.display_mode);

    group_oflow.open();
    
    

    
    
    

    
    Group group_balls = cp5.addGroup("ball controls")
//    .setPosition(20, 40)
    .setHeight(20).setWidth(gui_w)
    .setBackgroundHeight(view_h)
    .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_balls.getCaptionLabel().align(LEFT, CENTER);
    
    sx = 90;
    py = 10;
    oy = (int)(sy*1.5f);
    
    cp5.addButton("reset balls").setGroup(group_balls).plugTo(this, "initBalls").setWidth(160).setPosition(px, 10);
    
    py+=10;
    
    cp5.addSlider("NUM BALLS").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(10, 5000).setValue(NUM_BALLS)
    .plugTo(this, "setBallCount").linebreak();
    
    cp5.addSlider("Fill Factor").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1).setValue(BALL_SCREEN_FILL_FACTOR)
    .plugTo(this, "setBallsFillFactor").linebreak();

    cp5.addSlider("GRAVITY").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 0.05f).setValue(Ball.GRAVITY)
    .plugTo(this, "setBall_GRAVITY").linebreak();
    
    cp5.addSlider("COLL SPRING").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1f).setValue(Ball.COLLISION_SPRING)
    .plugTo(this, "setBall_COLLISION_SPRING").linebreak();
    
    cp5.addSlider("COLL DAMPING").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1f).setValue(Ball.COLLISION_DAMPING)
    .plugTo(this, "setBall_COLLISION_DAMPING").linebreak();
    
    cp5.addSlider("VEL DISSIPATION").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0.85f, 1.0f).setValue(Ball.VELOCITY_DISSIPATION)
    .plugTo(this, "setBall_VELOCITY_DISSIPATION").linebreak();
    
    py+=10;
    
    cp5.addSlider("FLUID DISSIPATION").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1f).setValue(Ball.FLUID_DISSIPATION)
    .plugTo(this, "setBall_FLUID_DISSIPATION").linebreak();
    
    cp5.addSlider("FLUID INERTIA").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1f).setValue(Ball.FLUID_INERTIA)
    .plugTo(this, "setBall_FLUID_INERTIA").linebreak();
    
    cp5.addSlider("FLUID SCALE").setGroup(group_balls).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1).setValue(Ball.FLUID_SCALE)
    .plugTo(this, "setBall_FLUID_SCALE").linebreak();

    
    py+=10;
    
    cp5.addSlider("BALL SHADING").setGroup(group_balls).setSize(sx, sy).setPosition(10, py+=oy)
    .setRange(0, 255).setValue(BALL_SHADING)
    .plugTo(this, "BALL_SHADING").linebreak();
    
    
    Accordion accordion = cp5.addAccordion("acc")
        .setPosition(view_w,0)
        .setWidth(gui_w)
        .addItem(group_oflow)
        .addItem(group_balls)
        ;

    accordion.setCollapseMode(Accordion.MULTI);
    accordion.open(0);
    accordion.open(1);
    
    
   
    group_oflow.open();
  }
  

  
  public void setDisplayMode(int val){
    opticalflow.param.display_mode = val;
  }

  public void activeFilters(float[] val){
    APPLY_GRAYSCALE = (val[0] > 0);
    APPLY_BILATERAL = (val[1] > 0);
  }
  
  
  
  
  
  public void setBallCount(int count){
    if(count == NUM_BALLS && balls != null && balls.length == NUM_BALLS){
      return;
    }
    NUM_BALLS = count;
    initBalls();
  }
  public void setBallsFillFactor(float screen_fill_factor){
    if(screen_fill_factor == BALL_SCREEN_FILL_FACTOR){
      return;
    }
    BALL_SCREEN_FILL_FACTOR = screen_fill_factor;
    initBalls();
  }
  public void setBall_GRAVITY(float v){
    Ball.GRAVITY = v;
  }
  public void setBall_COLLISION_SPRING(float v){
    Ball.COLLISION_SPRING = v;
  }
  public void setBall_COLLISION_DAMPING(float v){
    Ball.COLLISION_DAMPING = v;
  }
  public void setBall_VELOCITY_DISSIPATION(float v){
    Ball.VELOCITY_DISSIPATION = v;
  }
  public void setBall_FLUID_DISSIPATION(float v){
    Ball.FLUID_DISSIPATION = v;
  }
  public void setBall_FLUID_INERTIA(float v){
    Ball.FLUID_INERTIA = v;
  }
  public void setBall_FLUID_SCALE(float v){
    Ball.FLUID_SCALE = v;
  }
  
  
  
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { Main_OpticalFlow_CaptureParticles.class.getName() });
  }
}