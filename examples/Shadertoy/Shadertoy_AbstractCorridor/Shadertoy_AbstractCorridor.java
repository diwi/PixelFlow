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
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.PApplet;
import processing.core.PImage;



public class Shadertoy_AbstractCorridor extends PApplet {
  
  //
  // Shadertoy Demo:   https://www.shadertoy.com/view/MlXSWX
  // Shadertoy Author: https://www.shadertoy.com/user/Shane
  //
  
  DwPixelFlow context;
  DwShadertoy toy;

  DwGLTexture tex_0 = new DwGLTexture();
  DwGLTexture tex_1 = new DwGLTexture();

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
    
    
    // load assets
    PImage img0 = loadImage("../Shadertoy/Shadertoy_AbstractCorridor/data/Abstract 2.jpg");
    PImage img1 = loadImage("../Shadertoy/Shadertoy_AbstractCorridor/data/Wood.jpg");
    
    // create textures
    tex_0.resize(context, GL2.GL_RGBA8, img0.width, img0.height, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4,1);
    tex_1.resize(context, GL2.GL_RGBA8, img1.width, img1.height, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4,1);
    
    // copy images to textures
    DwFilter.get(context).copy.apply(img0, tex_0);
    DwFilter.get(context).copy.apply(img1, tex_1);
    
    // mipmap
    DwShadertoy.setTextureFilter(tex_0, DwShadertoy.TexFilter.MIPMAP);
    DwShadertoy.setTextureFilter(tex_1, DwShadertoy.TexFilter.MIPMAP);
    
    frameRate(60);
  }


  public void draw() {
    
    if(mousePressed){
      toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }
    toy.set_iChannel(0, tex_0);
    toy.set_iChannel(1, tex_1);
    toy.apply(this.g);

    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_AbstractCorridor.class.getName() });
  }
}