/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.src.filter;


import com.thomasdiewald.pixelflow.src.PixelFlow;
import com.thomasdiewald.pixelflow.src.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.src.dwgl.DwGLTexture;

import processing.opengl.PGraphics2D;
import processing.opengl.Texture;

public class SobelFilter {
  
  
  static public enum DIR{
    HORZ_3x3("sobel3_horz.frag", "sobel3_horz_UBYTE.frag"),
    VERT_3x3("sobel3_vert.frag", "sobel3_vert_UBYTE.frag"),
    TLBR_3x3("sobel3_tlbr.frag", "sobel3_tlbr_UBYTE.frag"),
    BRTL_3x3("sobel3_brtl.frag", "sobel3_brtl_UBYTE.frag");
    
    String dir_shader;
    String dir_shader_ubyte;
    
    DIR(String dir_shader, String dir_shader_ubyte){
      this.dir_shader = dir_shader;
      this.dir_shader_ubyte = dir_shader_ubyte;
    }

  }
  
  DwGLSLProgram[] shader      = new DwGLSLProgram[4];
  DwGLSLProgram[] shaderUbyte = new DwGLSLProgram[4];
  
  public PixelFlow context;

  public SobelFilter(PixelFlow context){
    this.context = context;
  }
  
  
  public void apply(PGraphics2D src, PGraphics2D dst, SobelFilter.DIR dir) {
    if(src == dst){
      System.out.println("SobelFilter error: read-write race");
      return;
    }
 
    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
    
    
       
    dst.beginDraw();
    context.begin();
    if(shaderUbyte[dir.ordinal()] == null) shaderUbyte[dir.ordinal()] = context.createShader(PixelFlow.SHADER_DIR+"Filter/"+dir.dir_shader_ubyte);
    apply(shaderUbyte[dir.ordinal()], tex_src.glName, dst.width, dst.height);
    context.end("SobelFilter.apply");
    dst.endDraw();
  }
  
 
  public void apply(DwGLTexture src, DwGLTexture dst, SobelFilter.DIR dir) {
    if(src == dst){
      System.out.println("SobelFilter error: read-write race");
      return;
    }
    context.begin();
    context.beginDraw(dst);
    if(shader[dir.ordinal()] == null) shader[dir.ordinal()] = context.createShader(PixelFlow.SHADER_DIR+"Filter/"+dir.dir_shader);
    apply(shader[dir.ordinal()], src.HANDLE[0], dst.w, dst.h);
    context.endDraw();
    context.end("SobelFilter.apply");
  }
  
  public void apply(DwGLSLProgram shader, int tex_handle, int w, int h){
    shader.begin();
    shader.uniform2f     ("wh" , w, h);
    shader.uniformTexture("tex", tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
  
  
  
}
