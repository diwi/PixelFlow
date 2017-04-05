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

public class Gamma {
  
  float gamma = 2.2f;
  
  public DwPixelFlow context;

  public Gamma(DwPixelFlow context){
    this.context = context;
  }
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, float gamma) {
    Texture tex_src = src.getTexture(); if(!tex_src.available()) return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available()) return;
    
//    dst.beginDraw();
    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName,dst.width, dst.height, gamma);
    context.endDraw();
    context.end("GammaCorrection.apply");
//    dst.endDraw();
  }
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst) {
    apply(src, dst, gamma);
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst, float gamma) {
    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.w, dst.h, gamma);
    context.endDraw();
    context.end("GammaCorrection.apply");
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst) {
    apply(src, dst, gamma);
  }
  
  DwGLSLProgram shader;
  private void apply(int tex_handle, int w, int h, float gamma){
    if(shader == null) shader = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/gamma.frag");
    shader.begin();
    shader.uniform2f     ("wh_rcp" , 1f/w, 1f/h);
    shader.uniform1f     ("GAMMA_CORRECTION", gamma);
    shader.uniformTexture("tex", tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
//    System.out.println("H");
  }
  
}
