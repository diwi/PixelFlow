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

public class BoxBlur {
  
  // separated blur kernel
  static private final int[] HORZ = new int[]{1,0};
  static private final int[] VERT = new int[]{0,1};
  
  public DwPixelFlow context;

  public BoxBlur(DwPixelFlow context){
    this.context = context;
  }

  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, PGraphicsOpenGL tmp, int radius) {
    if(src == tmp || dst == tmp){
      System.out.println("BoxBlur error: read-write race");
      return;
    }
    if(radius <= 0){
      return; 
    }
    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
    
    tmp.beginDraw();
    context.begin();
    pass(tex_src.glName, tmp.width, tmp.height, radius, HORZ);
    context.end("BoxBlur.apply - HORZ");
    tmp.endDraw();
    
    Texture tex_tmp = tmp.getTexture();
    
    dst.beginDraw();
    context.begin();
    pass(tex_tmp.glName, dst.width, dst.height, radius, VERT);
    context.end("BoxBlur.apply - VERT");
    dst.endDraw(); 
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst, DwGLTexture tmp, int radius) {
    if(src == tmp || dst == tmp){
      System.out.println("BoxBlur error: read-write race");
      return;
    }
    if(radius <= 0){
      return; 
    }
    
    context.begin();
    context.beginDraw(tmp);
    pass(src.HANDLE[0], tmp.w, tmp.h, radius, HORZ);
    context.endDraw();
    context.end("BoxBlur.apply - HORZ");
    
    context.begin();
    context.beginDraw(dst);
    pass(tmp.HANDLE[0], dst.w, dst.h, radius, VERT);
    context.endDraw();
    context.end("BoxBlur.apply - VERT");
  }
  
  DwGLSLProgram shader;
  private void pass(int tex_handle, int w, int h, int radius, int[] dir ){
    if(shader == null) shader = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/boxblur.frag");
    shader.begin();
    shader.uniform2f     ("wh"    , w, h);
    shader.uniform1i     ("radius", radius);
    shader.uniform2i     ("dir"   , dir[0], dir[1]);
    shader.uniformTexture("tex"   , tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }

 
}
