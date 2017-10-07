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



package Miscellaneous.ImageProcessing_Capture_BackgroundSubtraction;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwBackgroundSubtraction;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;


public class ImageProcessing_Capture_BackgroundSubtraction extends PApplet {
  

  // 
  // Simple example for background-subtraction.
  // 
  // controls:
  // 'b'   capture background
  // '1'   display source
  // '2'   display result     ...  difference x source
  // '3'   display difference ... |background - source|
  // '4'   display background
  
  
  
  // Camera
  Capture cam;
 
  PGraphics2D pg_src;
  PGraphics2D pg_dst;
  
  int cam_w = 640;
  int cam_h = 480;
  
  int DISPLAY = 1;
  
  DwBackgroundSubtraction bg_subtration;

  public void settings() {
    size(cam_w, cam_h, P2D);
    smooth(0);
  }

  public void setup() {

    // main library context
    DwPixelFlow context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    cam = new Capture(this, cam_w, cam_h, 30);
    cam.start();
    
    bg_subtration = new DwBackgroundSubtraction(context, cam_w, cam_h);
    
    pg_src = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_dst = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    
    frameRate(60);
  }
  

  public void draw() {
    
    if( cam.available() ){
      cam.read();
      
      pg_src.beginDraw();
      pg_src.blendMode(REPLACE);
      pg_src.image(cam, 0, 0);
      pg_src.endDraw();
      
      bg_subtration.apply(pg_src, pg_dst);
    }
    
    // display results
    blendMode(REPLACE);
    switch(DISPLAY){
      case 0: image(pg_src, 0, 0); break;
      case 1: image(pg_dst, 0, 0); break;
      case 2: image(bg_subtration.pg_diff, 0, 0); break;
      case 3: image(bg_subtration.pg_background, 0, 0); break;
    }
    
  }

  public void keyReleased(){
    if(key >= '1' && key <= '9') DISPLAY = key - '1';
    if(key == 'b') bg_subtration.reset();
  }
  

  public static void main(String args[]) {
    PApplet.main(new String[] { ImageProcessing_Capture_BackgroundSubtraction.class.getName() });
  }
}