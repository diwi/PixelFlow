/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package OpticalFlow.OpticalFlow_Basic;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;

import processing.core.*;
import processing.opengl.PGraphics2D;

public class OpticalFlow_Basic extends PApplet {
  
  // A GetStarted-example for using Optical Flow in Applications
  //
  // based on the direction an object is moving (dx, dy, dt), velocity vectors
  // are generated as output. 
  // To simulate a Movie or Webcam capture, this example simply draws some moving
  // stuff.
  //
  // LMB: (default)
  // MMB: velocity vectors are display normal to their direction
  // RMB: velocity is displayed as pixelshading
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;

  DwOpticalFlow opticalflow;
  PGraphics2D pg_oflow;
  PGraphics2D pg_src;
  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(8);
  }

  public void setup() {
    surface.setLocation(viewport_x, viewport_y);

    // main library context
    DwPixelFlow context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // opticalflow
    opticalflow = new DwOpticalFlow(context);
    
    // some flow parameters
    opticalflow.param.flow_scale         = 100;
    opticalflow.param.temporal_smoothing = 0.8f;
    opticalflow.param.display_mode       = 0;
    opticalflow.param.grayscale          = true;
    
    // render target
    pg_oflow = (PGraphics2D) createGraphics(width, height, P2D);
    pg_oflow.smooth(8);

    // drawing canvas, used as input for the optical flow
    pg_src = (PGraphics2D) createGraphics(width, height, P2D);
    pg_src.smooth(8);
  
    frameRate(60);
//    frameRate(1000);
  }
  

  // animated rectangle data
  float rs = 80;
  float rx = 100;
  float ry = 100;
  float dx = 3;
  float dy = 2.4f;
  
  public void draw() {

    // update rectangle position
    rx += dx;
    ry += dy;
    // keep inside viewport
    if(rx <        rs/2) {rx =        rs/2; dx = -dx; }
    if(rx > width -rs/2) {rx = width -rs/2; dx = -dx; }
    if(ry <        rs/2) {ry =        rs/2; dy = -dy; }
    if(ry > height-rs/2) {ry = height-rs/2; dy = -dy; }
    
    // update input image
    pg_src.beginDraw();
    pg_src.clear();
    pg_src.background(0);
    
    pg_src.rectMode(CENTER);
    pg_src.fill(150, 200, 255);
    pg_src.rect(rx, ry, rs, rs, rs/3f);
    
    pg_src.fill(200, 150, 255);
    pg_src.noStroke();
    pg_src.ellipse(mouseX, mouseY, 100, 100);
    pg_src.endDraw();
    

    // update Optical Flow
    opticalflow.update(pg_src);
    
    // render Optical Flow
    pg_oflow.beginDraw();
    pg_oflow.clear();
    pg_oflow.endDraw();
    
    // opticalflow visualizations
    // 1) velocity is displayed as dense, colored shading
    if(mousePressed && mouseButton == RIGHT) opticalflow.renderVelocityShading(pg_oflow);
    
    // 2) velocity is displayed as vectors
    //    display_mode = 0 --> lines, along the velocity direction
    //    display_mode = 1 --> lines, normal to the velocity direction
    opticalflow.param.display_mode = (mousePressed && mouseButton == CENTER) ? 1 : 0;
    opticalflow.renderVelocityStreams(pg_oflow, 10);
    
    // display result
    background(0);
    image(pg_src, 0, 0);
    image(pg_oflow, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", pg_oflow.width, pg_oflow.height, opticalflow.UPDATE_STEP, frameRate);
    surface.setTitle(txt_fps);
  }
  

  public static void main(String args[]) {
    PApplet.main(new String[] { OpticalFlow_Basic.class.getName() });
  }
}