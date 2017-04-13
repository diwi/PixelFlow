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

public class Sobel {
  
  
  public static enum TYPE{
      _3x3_HORZ  ("SOBEL_3x3_HORZ")
    , _3x3_VERT  ("SOBEL_3x3_VERT")
    , _3x3_TLBR  ("SOBEL_3x3_TLBR")
    , _3x3_BLTR  ("SOBEL_3x3_BLTR")
    ;
    
    protected DwGLSLProgram shader;
    protected String define;
    
    private TYPE(String define){
      this.define = define;
    }
    
    private void buildShader(DwPixelFlow context){
      if(shader != null){
        return; // no need to rebuild
      }
      Object id = this.name();
      shader = context.createShader(id, DwPixelFlow.SHADER_DIR+"Filter/sobel.frag");
      shader.frag.setDefine(define, 1);
    }
  }

  public DwPixelFlow context;

  public Sobel(DwPixelFlow context){
    this.context = context;
  }
  
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, Sobel.TYPE kernel) {
    if(src == dst){
      System.out.println("Sobel error: read-write race");
      return;
    }
    if(kernel == null){
      return; 
    }
    
    kernel.buildShader(context);
 
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    
    float[] mad = new float[]{0.5f,0.5f};
    
//    dst.beginDraw();
    context.begin();
    context.beginDraw(dst);
    apply(kernel.shader, tex_src.glName, dst.width, dst.height, mad);
    context.endDraw();
    context.end("Sobel.apply");
//    dst.endDraw();
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst, Sobel.TYPE kernel) {
    if(src == dst){
      System.out.println("Sobel error: read-write race");
      return;
    }
    if(kernel == null){
      return; 
    }
    
    kernel.buildShader(context);
    float[] mad = new float[]{1,0};
    
    context.begin();
    context.beginDraw(dst);
    apply(kernel.shader, src.HANDLE[0], dst.w, dst.h, mad);
    context.endDraw();
    context.end("Sobel.apply");
  }
  
  public void apply(DwGLSLProgram shader, int tex_handle, int w, int h, float[] mad){
    shader.begin();
    shader.uniform2f     ("wh_rcp", 1f/w, 1f/h);
    shader.uniform2f     ("mad", mad[0], mad[1]);
    shader.uniformTexture("tex", tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
}
