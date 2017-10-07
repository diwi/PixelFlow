/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package OpticalFlow.OpticalFlow_Capture;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;

public class OpticalFlow_Capture extends PApplet {
  
  // Example, Optical Flow for Webcam capture.
  
  DwPixelFlow context;
  
  DwOpticalFlow opticalflow;
  
  PGraphics2D pg_cam;
  PGraphics2D pg_oflow;
  
  int cam_w = 640;
  int cam_h = 480;
  
  int view_w = 1000;
  int view_h = (int)(view_w * cam_h/(float)cam_w);
  
  Capture cam;
  
  public void settings() {
    size(view_w, view_h, P2D);
    smooth(4);
  }

  public void setup() {
   
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // optical flow
    opticalflow = new DwOpticalFlow(context, cam_w, cam_h);

//    String[] cameras = Capture.list();
//    printArray(cameras);
//    cam = new Capture(this, cameras[0]);
    
    // Capture, video library
    cam = new Capture(this, cam_w, cam_h, 30);
    cam.start();
    
    pg_cam = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_cam.noSmooth();
    
    pg_oflow = (PGraphics2D) createGraphics(width, height, P2D);
    pg_oflow.smooth(4);
        
    background(0);
    frameRate(60);
  }
  

  public void draw() {
    
    if( cam.available() ){
      cam.read();
      
      // render to offscreenbuffer
      pg_cam.beginDraw();
      pg_cam.image(cam, 0, 0);
      pg_cam.endDraw();
      
      // update Optical Flow
      opticalflow.update(pg_cam); 
    }
    
    // rgba -> luminance (just for display)
    DwFilter.get(context).luminance.apply(pg_cam, pg_cam);
    
    // render Optical Flow
    pg_oflow.beginDraw();
    pg_oflow.clear();
    pg_oflow.image(pg_cam, 0, 0, width, height);
    pg_oflow.endDraw();
    
    // flow visualizations
    opticalflow.param.display_mode = 0;
    opticalflow.renderVelocityShading(pg_oflow);
    opticalflow.renderVelocityStreams(pg_oflow, 5);
    
    // display result
    background(0);
    image(pg_oflow, 0, 0);
 
  }
  

  public static void main(String args[]) {
    PApplet.main(new String[] { OpticalFlow_Capture.class.getName() });
  }
}