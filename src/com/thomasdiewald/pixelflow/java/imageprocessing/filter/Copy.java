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

public class Copy {
  
  public DwPixelFlow context;

  public Copy(DwPixelFlow context){
    this.context = context;
  }

  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst) {
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
       
    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName, dst.width, dst.height);
    context.endDraw();
    context.end("Copy.apply");
  }
  
  public void apply(PGraphicsOpenGL src, DwGLTexture dst) {
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
       
    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName, dst.w, dst.h);
    context.endDraw();
    context.end("Copy.apply");
  }
  
  public void apply(DwGLTexture src, PGraphicsOpenGL dst) {
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    
    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.width, dst.height);
    context.endDraw();
    context.end("Copy.apply");
    
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst) {
    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.w, dst.h);
    context.endDraw();
    context.end("Copy.apply");
  }
  
  DwGLSLProgram shader;
  private void apply(int tex_handle, int w, int h){
    if(shader == null) shader = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/copy.frag");
    shader.begin();
    shader.uniform2f     ("wh_rcp", 1f/w, 1f/h);
    shader.uniformTexture("tex"   , tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
}
