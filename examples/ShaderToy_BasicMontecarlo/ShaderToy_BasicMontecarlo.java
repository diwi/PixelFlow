/**
 * 
 * PixelFlow | Copyright (C]) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Shadertoy_BasicMontecarlo;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;

public class Shadertoy_BasicMontecarlo extends PApplet {
  
  DwPixelFlow context;
  DwShadertoy toyA;
  DwShadertoy toy;
  
  PGraphics2D pg_canvas;
  
  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toyA  = new DwShadertoy(context, "data/BasicMontecarlo_BufA.frag");
    toy   = new DwShadertoy(context, "data/BasicMontecarlo_Image.frag");
    
    frameRate(60);
  }

  
  public void resizeScene(){
    if(pg_canvas == null || width != pg_canvas.width || height != pg_canvas.height){
      pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
    }
    toyA.resize(width, height);
    toy .resize(width, height);
  }
  
  public void draw() {
    
    resizeScene();
    
    toyA.set_iChannel(0, toyA);
    toyA.apply();
    
    toy.set_iChannel(0, toyA);
    toy.apply(pg_canvas);
    
    image(pg_canvas, 0, 0);
        
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_BasicMontecarlo.class.getName() });
  }
}