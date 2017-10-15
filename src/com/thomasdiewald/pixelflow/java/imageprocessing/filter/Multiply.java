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

public class Multiply {
  
  public DwPixelFlow context;
  
  public Multiply(DwPixelFlow context){
    this.context = context;
  }
  
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, float[] multiplier) {
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;

    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName, dst.width, dst.height, multiplier);
    context.endDraw();
    context.end("Multiply.apply");
  }
  
  public void apply(PGraphicsOpenGL src, DwGLTexture dst, float[] multiplier) {
    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
       
    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName, dst.w, dst.h, multiplier);
    context.endDraw();
    context.end("Multiply.apply");
  }
  
  
  public void apply(DwGLTexture src, DwGLTexture dst, float[] multiplier) {
    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.w, dst.h, multiplier);
    context.endDraw();
    context.end("Multiply.apply");
  }
  
  DwGLSLProgram shader_vec;
  public void apply(int tex_handle, int w, int h, float[] multiplier){
    if(shader_vec == null) shader_vec = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/multiply.frag");
    shader_vec.begin();
    shader_vec.uniform2f     ("wh_rcp", 1f/w, 1f/h);
    shader_vec.uniformTexture("tex", tex_handle);
    shader_vec.uniform4fv    ("multiplier", 1, multiplier);
    shader_vec.drawFullScreenQuad();
    shader_vec.end();
  }
  
  
  
  
  
  
  
  public void apply(DwGLTexture src, DwGLTexture dst, DwGLTexture mul) {
    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.w, dst.h, mul.HANDLE[0]);
    context.endDraw();
    context.end("Multiply.apply");
  }
  
  
  
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, PGraphicsOpenGL mul) {
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    Texture tex_mul = mul.getTexture(); if(!tex_mul.available())  return; 
    
    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName, dst.width, dst.height, tex_mul.glName);
    context.endDraw();
    context.end("Multiply.apply");
  }
  
  
  DwGLSLProgram shader_tex;
  public void apply(int tex_handle_src, int w, int h, int tex_handle_mul){
    if(shader_tex == null) shader_tex = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/multiply_tex.frag");
    shader_tex.begin();
    shader_tex.uniform2f     ("wh_rcp" , 1f/w, 1f/h);
    shader_tex.uniformTexture("tex_src", tex_handle_src);
    shader_tex.uniformTexture("tex_mul", tex_handle_mul);
    shader_tex.drawFullScreenQuad();
    shader_tex.end();
  }
  
  
  
  
  
}
