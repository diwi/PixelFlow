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


package Shadertoy_ExpansiveReactionDiffusion;



import java.nio.ByteBuffer;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;


public class Shadertoy_ExpansiveReactionDiffusion extends PApplet {

  DwPixelFlow context;
  
  DwShadertoy toyA;
  DwShadertoy toyB;
  DwShadertoy toyC;
  DwShadertoy toyD;
  DwShadertoy toy;
  PGraphics2D pg_canvas;
  
  DwGLTexture tex_noise = new DwGLTexture();
 
  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toyA = new DwShadertoy(context, "data/ExpansiveReactionDiffusion_BufA.frag");
    toyB = new DwShadertoy(context, "data/ExpansiveReactionDiffusion_BufB.frag");
    toyC = new DwShadertoy(context, "data/ExpansiveReactionDiffusion_BufC.frag");
    toyD = new DwShadertoy(context, "data/ExpansiveReactionDiffusion_BufD.frag");
    toy  = new DwShadertoy(context, "data/ExpansiveReactionDiffusion_Image.frag");
    
    // create noise texture
    int wh = 256;

    byte[] bdata = new byte[wh * wh * 4];
    ByteBuffer bbuffer = ByteBuffer.wrap(bdata);
    for(int i = 0; i < bdata.length; i++){
      bdata[i] = (byte) random(0, 255);
    }
    tex_noise.resize(context, GL2.GL_RGBA8, wh, wh, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4, 1, bbuffer);
    
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

    if(mousePressed){
      toyA.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
      toyB.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
      toyC.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
      toyD.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
      toy .set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }
    
    toyA.set_iChannel(0, toyA);
    toyA.set_iChannel(1, toyC);
    toyA.set_iChannel(2, toyD);
    toyA.set_iChannel(3, tex_noise);
    toyA.apply();
    
    toyB.set_iChannel(0, toyA);
    toyB.apply();
    
    toyC.set_iChannel(0, toyB);
    toyC.apply();
    
    toyD.set_iChannel(0, toyA);
    toyD.apply();
    
    toy.set_iChannel(0, toyA);
    toy.set_iChannel(2, toyC);
    toy.set_iChannel(3, tex_noise);
    toy.apply(pg_canvas);
    

    blendMode(REPLACE);
    image(pg_canvas, 0, 0);
        
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_ExpansiveReactionDiffusion.class.getName() });
  }
}