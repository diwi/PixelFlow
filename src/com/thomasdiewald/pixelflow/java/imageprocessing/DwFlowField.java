/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.imageprocessing;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;


/**
 * 
 * Builds a gradient image from a given input.
 * The gradient is stored in a two-channel float texture.
 * 
 * @author Thomas Diewald
 *
 */
public class DwFlowField {
  
  public static class Param {
    
    public float   line_spacing = 15;
    public float   line_width   = 1.0f;
    public float   line_scale   = 1.5f;
    public boolean line_smooth  = true;
    
    public int     line_mode    = 0; // 0 or 1, in velocity direction, or normal to it
    public int     line_shading = 0; // 0 =  col_A/col_B, 1 = velocity
    
    public float[] line_col_A        = {1,1,1,1.0f};
    public float[] line_col_B        = {1,1,1,0.1f};
    
    public int     blend_mode   = 0; // BLEND=0; ADD=1
    
    public int     blur_radius     = 2;
    public int     blur_iterations = 1; 
  }
  
  
  public DwPixelFlow context;
  
  public Param param = new Param();
  
  public DwGLSLProgram shader_create;
  public DwGLSLProgram shader_display_lines;
  public DwGLSLProgram shader_display_pixel;
  
  public DwGLTexture tex_vel = new DwGLTexture();
  public DwGLTexture tex_tmp = new DwGLTexture();
  
  public DwFlowField(DwPixelFlow context){
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    String data_path = DwPixelFlow.SHADER_DIR+"Filter/";
    
    shader_create        = context.createShader(data_path+"flowfield_create.frag");
    shader_display_pixel = context.createShader(data_path+"flowfield_display_pixel.frag");
    shader_display_lines = context.createShader(data_path+"flowfield_display_lines.glsl", data_path+"flowfield_display_lines.glsl");
    shader_display_lines.frag.setDefine("SHADER_FRAG", 1);
    shader_display_lines.vert.setDefine("SHADER_VERT", 1);
  }
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_vel.release();
    tex_tmp.release();
  }
  
  public void reset(){
    tex_vel.clear(0);
  }

  public void resize(int w, int h){
    boolean resized = tex_vel.resize(context, GL2.GL_RG32F, w, h, GL2.GL_RG, GL.GL_FLOAT, GL2.GL_LINEAR, 2, 4);
    if(resized){
      tex_vel.setParam_WRAP_S_T(GL2.GL_CLAMP_TO_EDGE);
      tex_vel.clear(0);
    }
  }
  
  public void create(PGraphicsOpenGL pg_src){
    Texture tex_src = pg_src.getTexture(); if(!tex_src.available())  return;
    create(tex_src.glName, tex_src.glWidth, tex_src.glHeight);
  }
  
  public void create(DwGLTexture tex_src){
    create(tex_src.HANDLE[0], tex_src.w, tex_src.h);
  }
  
  public void create(int tex_src, int w_src, int h_src){
    context.begin();

    resize(w_src, h_src);
    
    int w_dst = tex_vel.w;
    int h_dst = tex_vel.h;
    
    context.beginDraw(tex_vel);
    shader_create.begin();
    shader_create.uniform2f     ("wh_rcp" , 1f/w_dst, 1f/h_dst);
    shader_create.uniformTexture("tex_src", tex_src);
    shader_create.drawFullScreenQuad();
    shader_create.end();
    context.endDraw("FlowField.create");

    blur(param.blur_iterations, param.blur_radius);

    context.end();
  }
  
  

  
  public void blur(){
    blur(param.blur_iterations, param.blur_radius);
  }

  public void blur(int iterations, int radius){
    if(!tex_vel.isTexture() || iterations <= 0 || radius <= 0){
      return;
    }
    
    tex_tmp.resize(context, tex_vel);
    tex_tmp.setParam_WRAP_S_T(GL2.GL_CLAMP_TO_EDGE);
    
    for(int i = 0; i < iterations; i++){
      DwFilter.get(context).gaussblur.apply(tex_vel, tex_vel, tex_tmp, radius);
    }
    context.errorCheck("FlowField.blur()");
  }
  
  

  
  public void displayLines(PGraphicsOpenGL dst){
    int   w = dst.width;
    int   h = dst.height;
    int   lines_x   = (int) Math.ceil(w/param.line_spacing);
    int   lines_y   = (int) Math.ceil(h/param.line_spacing);
    int   num_lines = lines_x * lines_y;
    float scale     = param.line_scale;

    context.begin();
    context.beginDraw(dst);
    blendMode();
    shader_display_lines.vert.setDefine("LINE_MODE"   , param.line_mode);
    shader_display_lines.vert.setDefine("LINE_SHADING", param.line_shading);
    shader_display_lines.begin();
    shader_display_lines.uniform4fv    ("col_A"         , 1, param.line_col_A);
    shader_display_lines.uniform4fv    ("col_B"         , 1, param.line_col_B);
    shader_display_lines.uniform2i     ("wh_lines"      ,    lines_x,    lines_y);
    shader_display_lines.uniform2f     ("wh_lines_rcp"  , 1f/lines_x, 1f/lines_y);
    shader_display_lines.uniform1f     ("vel_scale"     , scale);
    shader_display_lines.uniformTexture("tex_velocity"  , tex_vel);
    shader_display_lines.drawFullScreenLines(0, 0, w, h, num_lines, param.line_width, param.line_smooth);
    shader_display_lines.end();
    context.endDraw("FlowField.displayLines");
    context.end();
  }
  
  public void displayPixel(PGraphicsOpenGL dst){
    int w = dst.width;
    int h = dst.height;
    context.begin();
    context.beginDraw(dst);
    blendMode();
    shader_display_pixel.begin();
    shader_display_pixel.uniform2f     ("wh_rcp"      ,   1f/w, 1f/h);
    shader_display_pixel.uniformTexture("tex_velocity"  , tex_vel);
    shader_display_pixel.drawFullScreenQuad();
    shader_display_pixel.end();
    context.endDraw();
    context.end("FlowField.displayPixel");
  }
  
  protected void blendMode(){
    context.gl.glEnable(GL.GL_BLEND);
    context.gl.glBlendEquation(GL.GL_FUNC_ADD);
    switch(param.blend_mode){
      case 0:  context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE); break; // BLEND
      case 1:  context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE                , GL.GL_ONE, GL.GL_ONE); break; // ADD
      default: context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE); break; // BLEND
    }
  }

}
