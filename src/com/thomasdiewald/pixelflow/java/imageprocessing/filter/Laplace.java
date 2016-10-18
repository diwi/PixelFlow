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

public class Laplace {
  
  
  static public enum TYPE{
    W4_3x3 ("laplace4.frag" , "laplace4_UBYTE.frag" ),
    W8_3x3 ("laplace8.frag" , "laplace8_UBYTE.frag" ),
    W12_3x3("laplace12.frag", "laplace12_UBYTE.frag");

    String shader;
    String shader_ubyte;
    
    TYPE(String shader, String shader_ubyte){
      this.shader = shader;
      this.shader_ubyte = shader_ubyte;
    }
  }
  
  public DwPixelFlow context;

  public Laplace(DwPixelFlow context){
    this.context = context;
  }
  
  DwGLSLProgram[] shader       = new DwGLSLProgram[TYPE.values().length];
  DwGLSLProgram[] shader_ubyte = new DwGLSLProgram[TYPE.values().length];

  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, TYPE type) {
    if(src == dst){
      System.out.println("Laplace error: read-write race");
      return;
    }
    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
       
    dst.beginDraw();
    context.begin();
    if(shader_ubyte[type.ordinal()] == null) shader_ubyte[type.ordinal()] = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/"+type.shader_ubyte);
    apply(shader_ubyte[type.ordinal()], tex_src.glName, dst.width, dst.height);
    context.end("Laplace.apply");
    dst.endDraw();
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst, TYPE type) {
    if(src == dst){
      System.out.println("Laplace error: read-write race");
      return;
    }
    context.begin();
    context.beginDraw(dst);
    if(shader[type.ordinal()] == null) shader[type.ordinal()] = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/"+type.shader);
    apply(shader[type.ordinal()], src.HANDLE[0], dst.w, dst.h);
    context.endDraw();
    context.end("Laplace.apply");
  }
  
  public void apply(DwGLSLProgram shader, int tex_handle, int w, int h){
    shader.begin();
    shader.uniform2f     ("wh" , w, h);
    shader.uniformTexture("tex", tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }

}
