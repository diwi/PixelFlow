/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package OpticalFlow_CaptureSettings;



import controlP5.CheckBox;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;
import thomasdiewald.pixelflow.java.OpticalFlow;
import thomasdiewald.pixelflow.java.PixelFlow;
import thomasdiewald.pixelflow.java.filter.Filter;


public class Main_OpticalFlow_CaptureSettings extends PApplet {
 
  
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
  
  
  public void settings() {
    size(view_w + gui_w, view_h, P2D);
    smooth(4);
  }

  public void setup() {
    
    // main library context
    context = new PixelFlow(this);
    context.print();
    context.printGL();
      
    // OF
    opticalflow = new OpticalFlow(context, cam_w, cam_h);
    
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
      
    createGUI();
    
    background(0);
    frameRate(60);
  }
  

  

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
      
    }


    
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
    
    // display result
    background(0);
    image(pg_oflow, 0, 0);
    
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
    
    int sx = 100, sy = 14;
    int px = 10, py = 20, oy = (int)(sy*1.5f);
    
    Group group_oflow = cp5.addGroup("OpticalFlow")
    .setPosition(view_w, 20).setHeight(20).setWidth(gui_w)
    .setBackgroundHeight(view_h).setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_oflow.getCaptionLabel().align(LEFT, CENTER);
    
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
  }
  
  
  public void setDisplayMode(int val){
    opticalflow.param.display_mode = val;
  }

  public void activeFilters(float[] val){
    APPLY_GRAYSCALE = (val[0] > 0);
    APPLY_BILATERAL = (val[1] > 0);
  }
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { Main_OpticalFlow_CaptureSettings.class.getName() });
  }
}