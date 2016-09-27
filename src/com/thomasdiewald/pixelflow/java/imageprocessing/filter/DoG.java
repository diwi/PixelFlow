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

import processing.opengl.PGraphics2D;
import processing.opengl.Texture;

public class DoG {
  
  public DwPixelFlow context;

  public DoG(DwPixelFlow context){
    this.context = context;
  }
  
  DwGLSLProgram shader_DoG;
  DwGLSLProgram shader_DoG_UBYTE;
  
  //multiplier = {1,-1}
  public void apply(PGraphics2D srcA, PGraphics2D srcB, PGraphics2D dst, float[] multiplier) {
    Texture tex_srcA = srcA.getTexture(); if(!tex_srcA.available())   return;
    Texture tex_srcB = srcB.getTexture(); if(!tex_srcB.available())   return;
    
    dst.beginDraw();
    context.begin();
    if(shader_DoG_UBYTE == null) shader_DoG_UBYTE = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/DoG_UBYTE.frag");
    apply(shader_DoG_UBYTE, tex_srcA.glName, tex_srcB.glName, dst.width, dst.height, multiplier);
    context.end("DoG.apply");
    dst.endDraw();
  }
  
  
  // multiplier = {1,-1}
  public void apply(DwGLTexture srcA, DwGLTexture srcB, DwGLTexture dst, float[] multiplier) {
    context.begin();
    context.beginDraw(dst);
    if(shader_DoG == null) shader_DoG = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/DoG.frag");
    apply(shader_DoG, srcA.HANDLE[0], srcB.HANDLE[0], dst.w, dst.h, multiplier);
    context.endDraw();
    context.end("DoG.apply");
  }
  
  private void apply(DwGLSLProgram shader, int tex_handle_A, int tex_handle_B, int w, int h, float[] multiplier){
    shader.begin();
    shader.uniform2f     ("wh" , w, h);
    shader.uniform2fv    ("multiplier", 1, multiplier);
    shader.uniformTexture("texA", tex_handle_A);
    shader.uniformTexture("texB", tex_handle_B);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
}
