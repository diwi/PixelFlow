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

public class BilateralFilter {
  
  public DwPixelFlow context;

  public BilateralFilter(DwPixelFlow context){
    this.context = context;
  }
  
  public int     BILATERAL_RADIUS      = 5;
  public float   BILATERAL_SIGMA_COLOR = 0.3f;
  public float   BILATERAL_SIGMA_SPACE = 5;
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst) {
    apply(src, dst, BILATERAL_RADIUS, BILATERAL_SIGMA_COLOR, BILATERAL_SIGMA_SPACE);
  }

  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, int radius, float sigma_color, float sigma_space) {
    if(src == dst){
      System.out.println("BilateralFilter error: read-write race");
      return;
    }
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
       
//    dst.beginDraw();
    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName, dst.width, dst.height, radius, sigma_color, sigma_space);
    context.endDraw();
    context.end("BilateralFilter.apply");
//    dst.endDraw();
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst, int radius, float sigma_color, float sigma_space) {
    if(src == dst){
      System.out.println("BilateralFilter error: read-write race");
      return;
    }
    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.w, dst.h, radius, sigma_color, sigma_space);
    context.endDraw();
    context.end("BilateralFilter.apply");
  }
  
  DwGLSLProgram shader;
  private void apply(int tex_handle, int w, int h, int radius, float sigma_color, float sigma_space){
    if(shader == null) shader = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/bilateral.frag");
    shader.begin();
    shader.uniform2f     ("wh_rcp"     , 1f/w, 1f/h);
    shader.uniform1i     ("radius"     , radius);
    shader.uniform1f     ("sigma_color", sigma_color);
    shader.uniform1f     ("sigma_space", sigma_space);
    shader.uniformTexture("tex"        , tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
}
