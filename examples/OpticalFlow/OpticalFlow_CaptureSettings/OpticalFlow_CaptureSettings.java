/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package OpticalFlow.OpticalFlow_CaptureSettings;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;


public class OpticalFlow_CaptureSettings extends PApplet {
  
  //Example, Optical Flow for Webcam capture + GUI, to find good parameters.
  
  int cam_w = 640;
  int cam_h = 480;
  
  int view_w = 1200;
  int view_h = (int)(view_w * cam_h/(float)cam_w);
  int view_x = 230;
  int view_y = 0;
  
  int gui_w = 200;
  int gui_x = view_w;
  int gui_y = 0;
  
  //main library context
  DwPixelFlow context;
  
  // optical flow
  DwOpticalFlow opticalflow;
  
  // buffer for the capture-image
  PGraphics2D pg_cam_a, pg_cam_b; 

  // offscreen render-target
  PGraphics2D pg_oflow;
  
  // camera capture (video library)
  Capture cam;
  

  // some state variables for the GUI/display
  int     BACKGROUND_COLOR  = 0;
  boolean DISPLAY_SOURCE   = true;
  boolean APPLY_GRAYSCALE = false;
  boolean APPLY_BILATERAL = true;
  int     VELOCITY_LINES  = 6;

  
  public void settings() {
    size(view_w + gui_w, view_h, P2D);
    smooth(4);
  }

  public void setup() {
    
    surface.setLocation(view_x, view_y);
    
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
      
    //optical flow object
    opticalflow = new DwOpticalFlow(context, cam_w, cam_h);

    // webcam capture
    cam = new Capture(this, cam_w, cam_h, 30);
    cam.start();
    
    // renderbuffers
    pg_cam_a = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_cam_a.smooth(0);
    
    pg_cam_b = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_cam_b.smooth(0);
    
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
        DwFilter.get(context).luminance.apply(pg_cam_a, pg_cam_a);
      }
      if(APPLY_BILATERAL){
        DwFilter.get(context).bilateral.apply(pg_cam_a, pg_cam_b, 5, 0.10f, 4);
        swapCamBuffer();
      }
      
      // update Optical Flow
      opticalflow.update(pg_cam_a);
      
    }

    // render Optical Flow
    pg_oflow.beginDraw();
    pg_oflow.background(BACKGROUND_COLOR);
    if(DISPLAY_SOURCE){
      pg_oflow.image(pg_cam_a, 0, 0, view_w, view_h);
    }
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
  
  
  

  public void opticalFlow_setDisplayMode(int val){
    opticalflow.param.display_mode = val;
  }
  public void activeFilters(float[] val){
    APPLY_GRAYSCALE = (val[0] > 0);
    APPLY_BILATERAL = (val[1] > 0);
  }
  public void setOptionsGeneral(float[] val){
    DISPLAY_SOURCE = (val[0] > 0);
  }
 

  ControlP5 cp5;
  
  public void createGUI(){
    
    cp5 = new ControlP5(this);
    
    int sx, sy, px, py, oy;
    
    sx = 100; sy = 14;
    oy = (int)(sy*1.5f);
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - OPTICAL FLOW
    ////////////////////////////////////////////////////////////////////////////
    Group group_oflow = cp5.addGroup("Optical Flow");
    {
      group_oflow.setSize(gui_w, 165).setHeight(20)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_oflow.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      cp5.addSlider("blur input").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py)
        .setRange(0, 30).setValue(opticalflow.param.blur_input).plugTo(opticalflow.param, "blur_input");
      
      cp5.addSlider("blur flow").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
        .setRange(0, 10).setValue(opticalflow.param.blur_flow).plugTo(opticalflow.param, "blur_flow");
      
      cp5.addSlider("temporal smooth").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
        .setRange(0, 1).setValue(opticalflow.param.temporal_smoothing).plugTo(opticalflow.param, "temporal_smoothing");
      
      cp5.addSlider("flow scale").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
        .setRange(0, 200f).setValue(opticalflow.param.flow_scale).plugTo(opticalflow.param, "flow_scale");
  
      cp5.addSlider("threshold").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
        .setRange(0, 3.0f).setValue(opticalflow.param.threshold).plugTo(opticalflow.param, "threshold");
      
      cp5.addRadio("opticalFlow_setDisplayMode").setGroup(group_oflow).setSize(18, 18).setPosition(px, py+=oy)
        .setSpacingColumn(40).setSpacingRow(2).setItemsPerRow(3)
        .addItem("dir"    , 0)
        .addItem("normal" , 1)
        .addItem("Shading", 2)
        .activate(opticalflow.param.display_mode);
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - DISPLAY
    ////////////////////////////////////////////////////////////////////////////
    Group group_display = cp5.addGroup("display");
    {
      group_display.setHeight(20).setSize(gui_w, 125)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_display.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      cp5.addSlider("BACKGROUND").setGroup(group_display).setSize(sx,sy).setPosition(px, py)
          .setRange(0, 255).setValue(BACKGROUND_COLOR).plugTo(this, "BACKGROUND_COLOR");
  
      cp5.addCheckBox("setOptionsGeneral").setGroup(group_display).setSize(38, 18).setPosition(px, py+=oy)
          .setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
          .addItem("display source", 0).activate(DISPLAY_SOURCE ? 0 : 100);
  
      cp5.addCheckBox("activeFilters").setGroup(group_display).setSize(18, 18).setPosition(px, py+=(int)(oy*1.5f))
          .setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
          .addItem("grayscale"       , 0).activate(APPLY_GRAYSCALE ? 0 : 100)
          .addItem("bilateral filter", 1).activate(APPLY_BILATERAL ? 1 : 100);
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_oflow)
      .addItem(group_display)
      .open(0, 1);
  }
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { OpticalFlow_CaptureSettings.class.getName() });
  }
}