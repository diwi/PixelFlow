/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.imageprocessing.filter;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;


/**
 * 
 * ... can be used for edge detection, frame differencing
 * 
 * @author Thomas Diewald
 *
 */
public class Difference {
  

  
  public DwPixelFlow context;
  
  public Difference(DwPixelFlow context){
    this.context = context;
  }
  
  public void apply(PGraphicsOpenGL dst, PGraphicsOpenGL texA, PGraphicsOpenGL texB) {
    Texture tex_dst  = dst .getTexture(); if(!tex_dst .available())  return;
    Texture tex_texA = texA.getTexture(); if(!tex_texA.available())  return;
    Texture tex_texB = texB.getTexture(); if(!tex_texB.available())  return; 

    context.begin();
    context.beginDraw(dst);
    apply(tex_texA.glName, tex_texB.glName, dst.width, dst.height);
    context.endDraw();
    context.end("Difference.apply");
  }
  

  public void apply(DwGLTexture dst, DwGLTexture texA, DwGLTexture texB) {
    context.begin();
    context.beginDraw(dst);
    apply(texA.HANDLE[0], texB.HANDLE[0], dst.w, dst.h);
    context.endDraw();
    context.end("Difference.apply");
  }
  
  DwGLSLProgram shader;
  public void apply(int texA_handle, int texB_handle, int w, int h){
    if(shader == null) shader = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/difference.frag");
    shader.begin();
    shader.uniform2f     ("wh_rcp", 1f/w, 1f/h);
    shader.uniformTexture("texA", texA_handle);
    shader.uniformTexture("texB", texB_handle);
    shader.drawFullScreenQuad();
    shader.end();
  }

  
}
