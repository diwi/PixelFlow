/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
 * 
 * src  - www.github.com/diwi/PixelFlow
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.imageprocessing.filter;


import com.jogamp.opengl.GLES3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;


// Fast Summed-Area Table Generation and its Applications
// http://www.shaderwrangler.com/publications/sat/SAT_EG2005.pdf

public class SummedAreaTable{
  
  protected DwPixelFlow context;
  protected DwGLSLProgram shader_create;
  protected DwGLSLProgram shader_blur;
  
  protected DwGLTexture sat_src  = new DwGLTexture();
  protected DwGLTexture sat_dst  = new DwGLTexture();
  
  protected InternalFormat format = InternalFormat.RGB32F;
  protected final int SAMPLES = 8;
  
  public SummedAreaTable(DwPixelFlow context){
    this.context  = context;
    this.shader_create = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/SummedAreaTable_Create.frag");
    this.shader_blur   = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/SummedAreaTable_Blur.frag");
  }
  
  private void swap(){
    DwGLTexture sat_tmp = sat_src;
    sat_src = sat_dst;
    sat_dst = sat_tmp;
  }
  
  static public enum InternalFormat{
    R32F    (GLES3.GL_R32F   , GLES3.GL_RED , 1),
    RG32F   (GLES3.GL_RG32F  , GLES3.GL_RG  , 2),
    RGB32F  (GLES3.GL_RGB32F , GLES3.GL_RGB , 3),
    RGBA32F (GLES3.GL_RGBA32F, GLES3.GL_RGBA, 4);
    
    final public int gl_internalformat;
    final public int gl_format;
    final public int num_channels;
    private InternalFormat(int gl_internalformat, int gl_format, int num_channels){
      this.gl_internalformat = gl_internalformat;
      this.gl_format = gl_format;
      this.num_channels = num_channels;
    }
  }

  public void setFormat(InternalFormat format){
    this.format = format;
  }
  
  public InternalFormat getFormat(){
    return this.format;
  }
  
  public DwGLTexture getSummedAreaTableTexture(){
    return sat_src;
  }
  
  public void resize(int w, int h){
    sat_src.resize(context, format.gl_internalformat, w, h, format.gl_format, GLES3.GL_FLOAT, GLES3.GL_NEAREST, format.num_channels, 4);
    sat_dst.resize(context, format.gl_internalformat, w, h, format.gl_format, GLES3.GL_FLOAT, GLES3.GL_NEAREST, format.num_channels, 4);
  }
  
  public void release(){
    sat_src.release();
    sat_dst.release();
  }
  
  public void clear(){
    sat_src.clear(0);
    sat_dst.clear(0);
  }

  public void create(PGraphicsOpenGL src){
    Texture tex_src = src.getTexture();  if(!tex_src.available())  return;
    
    int w = tex_src.glWidth;
    int h = tex_src.glHeight;
    int HANDLE_src = tex_src.glName;
    
    resize(w,h);
    
    DwGLSLProgram shader = shader_create;
    context.begin();
    shader.begin();

    // (0) horizontal pass on src image
    {
      int pass = 0;
      int jump = (int) Math.pow(SAMPLES, pass);
      context.beginDraw(sat_dst);
      shader.uniformTexture("tex" , HANDLE_src);
      shader.uniform2i     ("jump", jump, 0);
      shader.drawFullScreenQuad();
      context.endDraw();
      swap();
    }
    
    // (1) remaining horizontal passes
    int pass_count_h = (int)(Math.ceil(Math.log(w) / Math.log(SAMPLES)));
    for(int pass = 1; pass < pass_count_h; pass++){
      int jump = (int) Math.pow(SAMPLES, pass);
      context.beginDraw(sat_dst);
      shader.uniformTexture("tex" , sat_src);
      shader.uniform2i     ("jump", jump, 0);
      shader.drawFullScreenQuad();
      context.endDraw();
      swap();
    }
    
    // (2) vertical passes
    int pass_count_v = (int)(Math.ceil(Math.log(h) / Math.log(SAMPLES)));
    for(int pass = 0; pass < pass_count_v; pass++){
      int jump = (int) Math.pow(SAMPLES, pass);
      context.beginDraw(sat_dst);
      shader.uniformTexture("tex" , sat_src);
      shader.uniform2i     ("jump", 0, jump);
      shader.drawFullScreenQuad();
      context.endDraw();
      swap();
    }
    shader.end();
    context.end("SummedAreaTable.create");
  }
  
  /**
   * applies a box-blur using the SAT-Textures (needs to be created in previous step)
   * 
   * @param dst
   * @param radius
   */
  public void apply(PGraphicsOpenGL dst, int radius){
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    
    int w_sat = sat_src.w;
    int h_sat = sat_src.h;
    int w_dst = tex_dst.glWidth;
    int h_dst = tex_dst.glHeight;

    DwGLSLProgram shader = shader_blur;
    context.begin();
    context.beginDraw(dst);
    shader.begin();
    shader.uniform2f     ("wh_dst" , w_dst, h_dst);
    shader.uniform2f     ("wh_sat" , w_sat, h_sat);
    shader.uniformTexture("tex_sat", sat_src);
    shader.uniform1i     ("radius", radius);
    shader.drawFullScreenQuad();
    shader.end();
    context.endDraw();
    context.end("SummedAreaTable.apply");
  }

}
