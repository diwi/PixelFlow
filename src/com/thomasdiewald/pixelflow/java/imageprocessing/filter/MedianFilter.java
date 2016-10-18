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

public class MedianFilter {
  
  static public enum TYPE{
    _3x3_ ("median3.frag" ),
    _5x5_ ("median5.frag");

    String shader;

    TYPE(String shader){
      this.shader = shader;
    }
  }
  
  
  public DwPixelFlow context;

  public MedianFilter(DwPixelFlow context){
    this.context = context;
  }

  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, MedianFilter.TYPE size) {
    if(src == dst){
      System.out.println("MedianFilter error: read-write race");
      return;
    }

    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
    
    dst.beginDraw();
    context.begin();
    apply(tex_src.glName, dst.width, dst.height, size);
    context.end("MedianFilter.apply");
    dst.endDraw();
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst, MedianFilter.TYPE size) {
    if(src == dst){
      System.out.println("MedianFilter error: read-write race");
      return;
    }

    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.w, dst.h, size);
    context.endDraw();
    context.end("MedianFilter.apply");
  }
  
  DwGLSLProgram[] shader = new DwGLSLProgram[TYPE.values().length];
  private void apply(int tex_handle, int w, int h, MedianFilter.TYPE size){
    if(shader[size.ordinal()] == null) shader[size.ordinal()] = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/"+size.shader);
    
    DwGLSLProgram shader_use = shader[size.ordinal()];
    shader_use.begin();
    shader_use.uniform2f     ("wh" , w, h);
    shader_use.uniformTexture("tex", tex_handle);
    shader_use.drawFullScreenQuad();
    shader_use.end();
  }
  
}
