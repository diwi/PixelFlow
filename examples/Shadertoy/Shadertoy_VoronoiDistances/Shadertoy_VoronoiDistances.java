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


package Shadertoy.Shadertoy_VoronoiDistances;



import java.nio.ByteBuffer;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;

import processing.core.PApplet;


public class Shadertoy_VoronoiDistances extends PApplet {
  
  //
  // Shadertoy Demo:   https://www.shadertoy.com/view/ldl3W8
  // Shadertoy Author: https://www.shadertoy.com/user/iq
  //
  
  DwPixelFlow context;
  DwShadertoy toy;
  DwGLTexture tex0 = new DwGLTexture();

  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toy = new DwShadertoy(context, "data/VoronoiDistances.frag");
    
    // create noise texture
    int wh = 256;
    byte[] bdata = new byte[wh * wh * 4];
    ByteBuffer bbuffer = ByteBuffer.wrap(bdata);
    for(int i = 0; i < bdata.length;){
      bdata[i++] = (byte) random(0, 255);
      bdata[i++] = (byte) random(0, 255);
      bdata[i++] = (byte) random(0, 255);
      bdata[i++] = (byte) 255;
    }
    tex0.resize(context, GL2.GL_RGBA8, wh, wh, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4, 1, bbuffer);
    
    frameRate(60);
  }

  
  public void draw() {

    toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    toy.set_iChannel(0, tex0);
    toy.apply(this.g);
    
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_VoronoiDistances.class.getName() });
  }
}