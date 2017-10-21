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


package Shadertoy.Shadertoy_AbstractCorridor;


import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;



public class Shadertoy_AbstractCorridor1 extends PApplet {
  
  //
  // Shadertoy Demo:   https://www.shadertoy.com/view/MlXSWX
  // Shadertoy Author: https://www.shadertoy.com/user/Shane
  //
  
  DwPixelFlow context;
  DwShadertoy toy;

  PGraphics2D pg_0;
  PGraphics2D pg_1;

  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toy = new DwShadertoy(context, "data/AbstractCorridor.frag");
    
    int w = 512;
    int h = 512;
    
    pg_0 = (PGraphics2D) createGraphics(w, h, P2D);
    pg_1 = (PGraphics2D) createGraphics(w, h, P2D);
    
    DwUtils.changeTextureWrap  (pg_0, GL2.GL_MIRRORED_REPEAT);
    DwUtils.changeTextureFilter(pg_0, GL2.GL_LINEAR, GL2.GL_LINEAR);
    
    DwUtils.changeTextureWrap  (pg_1, GL2.GL_MIRRORED_REPEAT);
    DwUtils.changeTextureFilter(pg_1, GL2.GL_LINEAR, GL2.GL_LINEAR);
    
    pg_0.beginDraw();
    pg_0.background(100,200,0);
//    pg_0.noStroke();
//    int lines = 5;
//    for(int y = 0; y < lines; y++){
//      for(int x = 0; x < lines; x++){
//        int px = x * pg_0.width / (lines-1);
//        int py = y * pg_0.width / (lines-1);
//        pg_0.fill(0);
//        pg_0.rect(px-1,0,2,height);
//        pg_0.fill(0);
//        pg_0.rect(0,py-1,width,2);
//      }
//    }
    pg_0.endDraw();
    
    pg_1.beginDraw();
    pg_1.background(200,100,0);
    pg_1.noStroke();
    for(int y = 0; y < pg_1.height; y++){
      for(int x = 0; x < pg_1.width; x++){
        float scale = 0.02f;
        int gray = (int)(noise(x * scale, y * scale) * 255);
        int argb = gray<<24 | gray<<16 | gray<<8 | gray;
        pg_1.fill(argb);
        pg_1.rect(x,y,1,1);
      }
    }
    pg_1.endDraw();

    frameRate(60);
  }


  public void draw() {
    
    if(mousePressed){
      toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }
    toy.set_iChannel(0, pg_0);
    toy.set_iChannel(1, pg_1);
    toy.apply(this.g);
    
//    image(pg_0, 0, 0);
//    image(pg_1, 0, 0);

    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_AbstractCorridor1.class.getName() });
  }
}