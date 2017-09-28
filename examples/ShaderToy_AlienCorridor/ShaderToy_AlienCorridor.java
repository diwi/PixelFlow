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


package Shadertoy_AlienCorridor;


import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;


public class Shadertoy_AlienCorridor extends PApplet {
  
  DwPixelFlow context;
  
  DwShadertoy toyA;
  DwShadertoy toyB;
  DwShadertoy toyC;
  DwShadertoy toyD;
  DwShadertoy toy;
  
  DwGLTexture tex_noise = new DwGLTexture();
  
  PGraphics2D pg_canvas;
  PGraphics2D pg_noise;
  
  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toyA  = new DwShadertoy(context, "data/AlienCorridor_BufA.frag");
    toyB  = new DwShadertoy(context, "data/AlienCorridor_BufB.frag");
    toyC  = new DwShadertoy(context, "data/AlienCorridor_BufC.frag");
    toyD  = new DwShadertoy(context, "data/AlienCorridor_BufD.frag");
    toy   = new DwShadertoy(context, "data/AlienCorridor_Image.frag");
    
    pg_noise = (PGraphics2D) createGraphics(512, 512, P2D);
    pg_noise.smooth(0);
    pg_noise.beginDraw();
    pg_noise.noStroke();
    for(int y = 0; y < pg_noise.height; y++){
      for(int x = 0; x < pg_noise.width; x++){
        float scale = 0.02f;
        int gray = (int)(noise(x * scale, y * scale) * 255);
        int argb = 0xFF000000 | gray<<16 | gray<<8 | gray;
        pg_noise.fill(argb);
        pg_noise.rect(x,y,1,1);
      }
    }
    pg_noise.endDraw();
    
    tex_noise.resize(context, GL2.GL_RGBA8, pg_noise.width, pg_noise.height, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4, 1);
    DwFilter.get(context).copy.apply(pg_noise, tex_noise);
    
    frameRate(60);
  }
  

  public void resizeScene(){
    if(pg_canvas == null || width != pg_canvas.width || height != pg_canvas.height){
      pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
      toy.reset();
    }
    
    toyA.resize(width, height);
    toyB.resize(width, height);
    toyC.resize(width, height);
    toyD.resize(width, height);
  }
  

  public void draw() {
    
    resizeScene();
    
    toyA.set_iChannel(0, tex_noise);
    toyA.apply();
    
    toyB.set_iChannel(0, toyA);
    toyB.apply();
    
    toyC.set_iChannel(0, toyB);
    toyC.apply();
    
    toyD.set_iChannel(0, toyC);
    toyD.apply();
    
    toy.set_iChannel(0, toyD);
    toy.set_iChannel(1, toyA);
    toy.apply(pg_canvas);
    
    image(pg_canvas, 0, 0);

    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

 
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_AlienCorridor.class.getName() });
  }
}