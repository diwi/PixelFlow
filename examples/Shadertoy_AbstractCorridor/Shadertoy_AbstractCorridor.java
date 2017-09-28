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


package Shadertoy_AbstractCorridor;



import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;



public class Shadertoy_AbstractCorridor extends PApplet {
  
  DwPixelFlow context;
  DwShadertoy toy;
  
  PGraphics2D pg_canvas;
  
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
    
    toy = new DwShadertoy(context, "data/AbstractCorridor_Image.frag");
    
    int w = 512;
    int h = 512;
    
    pg_0 = (PGraphics2D) createGraphics(w, h, P2D);
    pg_1 = (PGraphics2D) createGraphics(w, h, P2D);

    pg_0.beginDraw();
    pg_0.background(100,200,0);
    pg_0.endDraw();
    
    pg_1.beginDraw();
    pg_1.background(200,100,0);
    pg_1.endDraw();

    frameRate(60);
  }

  public void resizeScene(){
    if(pg_canvas == null || width != pg_canvas.width || height != pg_canvas.height){
      pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
      toy.reset();
    }
  }
  
  public void draw() {
    
    resizeScene();
    
    if(mousePressed){
      toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }
    toy.set_iChannel(0, pg_0);
    toy.set_iChannel(1, pg_1);
    toy.apply(pg_canvas);
    
    // put it on the screen
    blendMode(REPLACE);
    image(pg_canvas, 0, 0);
        
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_AbstractCorridor.class.getName() });
  }
}