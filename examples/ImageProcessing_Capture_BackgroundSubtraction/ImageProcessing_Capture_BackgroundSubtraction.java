/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package ImageProcessing_Capture_BackgroundSubtraction;



import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwHarrisCorner;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.BinomialBlur;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Laplace;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Median;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Sobel;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.SummedAreaTable;

import controlP5.ControlP5;
import controlP5.Group;
import controlP5.Slider;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;


public class ImageProcessing_Capture_BackgroundSubtraction extends PApplet {
  

  
  // Camera
  Capture cam;
 
  // two draw-buffers for swaping
  PGraphics2D pg_src;
  PGraphics2D pg_background;
  PGraphics2D pg_diff;
  PGraphics2D pg_tmp; // just another buffer for temporary results
 

  int cam_w = 640;
  int cam_h = 480;
  
  int view_w = 1200;
  int view_h = (int)(view_w * cam_h/(float)cam_w);
  
  DwPixelFlow context;
  DwFilter filter;

  public void settings() {
    size(cam_w, cam_h, P2D);
    smooth(0);
  }

  public void setup() {

    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    filter = DwFilter.get(context);
    
    pg_src        = initTexture(view_w, view_h);
    pg_background = initTexture(view_w, view_h);
    pg_diff       = initTexture(view_w, view_h);
    pg_tmp        = initTexture(view_w, view_h);
    
    cam = new Capture(this, cam_w, cam_h, 30);
    cam.start();
 
    
    frameRate(60);
//    frameRate(1000);
  }
  
  public PGraphics2D initTexture(int w, int h){
    PGraphics2D pg = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg.smooth(0);
    pg.beginDraw();
    pg.textureWrap(CLAMP);
    pg.clear();
    pg.endDraw();
    return pg;
  }
  
  

  public int DISPLAY = 1;
  
  

  
  public void draw() {
    
    if( cam.available() ){
      cam.read();
      
      pg_src.beginDraw();
      pg_src.image(cam, 0, 0);
      pg_src.endDraw();
      
      if(CAPTURE_BG){
        filter.luminance.apply(pg_src, pg_background);
        filter.gaussblur.apply(pg_background, pg_background, pg_tmp, 6);
        CAPTURE_BG = false;
      }
      
      pg_tmp.beginDraw();
      pg_tmp.clear();
      pg_tmp.endDraw();
    
      filter.luminance.apply(pg_src, pg_diff);
      filter.gaussblur.apply(pg_diff, pg_diff, pg_tmp, 3);
      
//      float mult     = 2f;
//      float shift    = 0.1f;
//      
//      float[] madA = { +mult, shift * 0.5f};
//      float[] madB = { -mult, shift * 0.5f};
//      filter.merge.apply(pg_diff, pg_background, pg_diff, madA, madB);
      
      filter.difference.apply(pg_diff, pg_background, pg_diff);
    }
    
    blendMode(REPLACE);
    clear();
    switch(DISPLAY){
      case 0: image(pg_src, 0, 0); break;
      case 1: image(pg_diff, 0, 0); break;
      case 2: image(pg_background, 0, 0); break;
    }
    
  }
  
  
  public boolean CAPTURE_BG = false;

  
  
  public void keyReleased(){
    if(key >= '1' && key <= '3')DISPLAY = key - '1';
    if(key == 'b') CAPTURE_BG = true;
  }
  
  


  public static void main(String args[]) {
    PApplet.main(new String[] { ImageProcessing_Capture_BackgroundSubtraction.class.getName() });
  }
}