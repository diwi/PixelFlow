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


package Shadertoy.Shadertoy_Elevated;



import java.nio.ByteBuffer;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;

import processing.core.PApplet;


public class Shadertoy_Elevated extends PApplet {
  
  //
  // Shadertoy Demo:   https://www.shadertoy.com/view/MdX3Rr
  // Shadertoy Author: https://www.shadertoy.com/user/iq
  //
  
  DwPixelFlow context;
  DwShadertoy toy, toyA;
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
    
    toyA = new DwShadertoy(context, "data/Elevated_BufA.frag");
    toy  = new DwShadertoy(context, "data/Elevated.frag");
    
    // create noise texture
    int wh = 256;
    byte[] bdata = new byte[wh * wh];
    ByteBuffer bbuffer = ByteBuffer.wrap(bdata);
    for(int i = 0; i < bdata.length; i++){
      bdata[i] = (byte) random(0, 256);
    }
    tex_noise.resize(context, GL2.GL_R8, wh, wh, GL2.GL_RED, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 1, 1, bbuffer);
    frameRate(60);
  }
  
  public void draw() {

    if(mousePressed){
      toyA.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }
    toyA.set_iChannel(0, tex_noise);
    toyA.apply(width, height);
    
    toy.set_iChannel(0, toyA);
    toy.apply(this.g);
    
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_Elevated.class.getName() });
  }
}