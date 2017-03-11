/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
 * 
 * src  - www.github.com/diwi/PixelFlow
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.antialiasing.FXAA;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * @author Thomas
 *
 */
public class FXAA {

  public DwPixelFlow context;
  
//  public float[] luminance = {0.2989f, 0.5870f, 0.1140f};
//  public float[] luminance = {0.2126f, 0.7152f, 0.0722f};
//  public float[] luminance = {0.3333f, 0.3333f, 0.3333f}; // rgb average
  
  public FXAA(DwPixelFlow context){
    this.context = context;
    
    this.shader = context.createShader(DwPixelFlow.SHADER_DIR+"antialiasing/FXAA/FXAA.frag");
  }
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst) {
    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
       
    dst.beginDraw();
    context.begin();
    apply(tex_src.glName, dst.width, dst.height);
    context.end("FXAA.apply");
    dst.endDraw();
  }
  
  public void apply(PGraphicsOpenGL src, DwGLTexture dst) {
    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
       
    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName, dst.w, dst.h);
    context.endDraw();
    context.end("FXAA.apply");
  }
  
  
  public void apply(DwGLTexture src, DwGLTexture dst) {
    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.w, dst.h);
    context.endDraw();
    context.end("FXAA.apply");
  }
  
  DwGLSLProgram shader;
  public void apply(int tex_handle, int w, int h){
//    if(shader == null) shader = context.createShader(DwPixelFlow.SHADER_DIR+"antialiasing/FXAA/FXAA.frag");
    shader.begin();
    shader.uniform2f     ("wh_rcp" , 1f/w, 1f/h);
    shader.uniformTexture("tex", tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
  
 
}
