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


package Shadertoy.Shadertoy_Repelling;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;


public class Shadertoy_Repelling extends PApplet {
  
  //
  // demo:   https://www.shadertoy.com/view/XdjXWK
  // author: https://www.shadertoy.com/user/iq
  //
  
  DwPixelFlow context;
  DwShadertoy toy;

  PGraphics2D pg_src;
  
  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toy = new DwShadertoy(context, "examples/Shadertoy/Shadertoy_Repelling/data/Repelling.frag");
    
    pg_src = (PGraphics2D) createGraphics(width, height, P2D);
    pg_src.smooth(0);
    
    pg_src.beginDraw();
    pg_src.background(255,160,64);
    pg_src.endDraw();
    
    frameRate(60);
  }
  
  public void draw() {
    
//    pg_src.beginDraw();
//    pg_src.background(255);
//    pg_src.strokeWeight(10);
//    pg_src.stroke(0);
//    pg_src.fill(0,127,255);
//    pg_src.ellipse(mouseX, mouseY, 200, 200);
//    pg_src.endDraw();
 

    blendMode(REPLACE);
    toy.set_iMouse(mouseX, mouseY, mouseX, mouseY);
    toy.set_iChannel(0, pg_src);
    toy.apply(this.g);
    
//    image(pg_src, 0, 0);
    
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_Repelling.class.getName() });
  }
}