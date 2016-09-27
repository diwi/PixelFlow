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

public class Mix {
  
  public DwPixelFlow context;

  public Mix(DwPixelFlow context){
    this.context = context;
  }

  public void apply(PGraphics2D srcA, PGraphics2D srcB, PGraphics2D dst, float mix_value) {
    Texture tex_srcA = srcA.getTexture(); if(!tex_srcA.available())   return;
    Texture tex_srcB = srcB.getTexture(); if(!tex_srcB.available())   return;
    
    dst.beginDraw();
    context.begin();
    apply(tex_srcA.glName, tex_srcB.glName, dst.width, dst.height, mix_value);
    context.end("Mix.apply");
    dst.endDraw();
  }
  
   public void apply(DwGLTexture srcA, DwGLTexture srcB, DwGLTexture dst, float mix_value) {
    context.begin();
    context.beginDraw(dst);
    apply(srcA.HANDLE[0], srcB.HANDLE[0], dst.w, dst.h, mix_value);
    context.endDraw();
    context.end("Mix.apply");
  }
  
  DwGLSLProgram shader;
  private void apply(int tex_handle_A, int tex_handle_B, int w, int h, float mix_value){
    if(shader == null) shader = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/mix.frag");
    shader.begin();
    shader.uniform2f     ("wh" , w, h);
    shader.uniform1f     ("mix_value", mix_value);
    shader.uniformTexture("texA", tex_handle_A);
    shader.uniformTexture("texB", tex_handle_B);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
}
