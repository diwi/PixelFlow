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

public class Add {
  
  public DwPixelFlow context;

  public Add(DwPixelFlow context){
    this.context = context;
  }

  public void apply(PGraphics2D srcA, PGraphics2D srcB, PGraphics2D dst, float[] multiplier) {
    Texture tex_srcA = srcA.getTexture(); if(!tex_srcA.available())   return;
    Texture tex_srcB = srcB.getTexture(); if(!tex_srcB.available())   return;
    
    dst.beginDraw();
    context.begin();
    apply(tex_srcA.glName, tex_srcB.glName, dst.width, dst.height, multiplier);
    context.end("Add.apply");
    dst.endDraw();
  }
  
 
  public void apply(DwGLTexture srcA, DwGLTexture srcB, DwGLTexture dst, float[] multiplier) {
    context.begin();
    context.beginDraw(dst);
    apply(srcA.HANDLE[0], srcB.HANDLE[0], dst.w, dst.h, multiplier);
    context.endDraw();
    context.end("Add.apply");
  }
  
  DwGLSLProgram shader;
  private void apply(int tex_handle_A, int tex_handle_B, int w, int h, float[] multiplier){
    if(shader == null) shader = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/add.frag");
    shader.begin();
    shader.uniform2f     ("wh" , w, h);
    shader.uniform2fv    ("multiplier", 1, multiplier);
    shader.uniformTexture("texA", tex_handle_A);
    shader.uniformTexture("texB", tex_handle_B);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
}
