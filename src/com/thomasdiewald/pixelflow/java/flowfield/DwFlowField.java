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




package com.thomasdiewald.pixelflow.java.flowfield;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.GaussianBlur;

import processing.opengl.PGraphicsOpenGL;


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
    
    public float   line_width   = 1.0f;
    public float   line_spacing = 15;
    public float   line_scale   = 1.5f;
    public int     line_mode    = 0; // 0 or 1, in velocity direction, or normal to it
    
    public int     blur_radius     = 5;
    public int     blur_iterations = 1; 
    
    public float[] col_A        = {1,1,1,1.0f};
    public float[] col_B        = {1,1,1,0.1f};
    
    public int     blend_mode   = 0; // BLEND=0; ADD=1
    public boolean smooth       = true;
    
  }
  
  public DwPixelFlow context;
  
  public Param param = new Param();
  
  public DwGLSLProgram shader_create;
  public DwGLSLProgram shader_display;
  
  public DwGLTexture tex_vel = new DwGLTexture();
  public DwGLTexture tex_tmp = new DwGLTexture();
  public GaussianBlur gaussblur;
  
//  protected String data_path = DwPixelFlow.SHADER_DIR+"flowfield/";
  protected String data_path = "D:/data/__Eclipse/workspace/WORKSPACE_FLUID/PixelFlow/src/com/thomasdiewald/pixelflow/glsl/flowfield/";
  
  public DwFlowField(DwPixelFlow context){
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    shader_create  = context.createShader(data_path+"flowfield_create.frag");
    shader_display = context.createShader(data_path+"flowfield_display.glsl", data_path+"flowfield_display.glsl");
    shader_display.frag.setDefine("SHADER_FRAG", 1);
    shader_display.vert.setDefine("SHADER_VERT", 1);
    
    gaussblur = new GaussianBlur(context);
  }
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_vel.release();
  }
  public void reset(){
    tex_vel.clear(0);
  }

  public void resize(int w, int h){
    boolean resized = tex_vel.resize(context, GL2.GL_RG32F, w, h, GL2.GL_RG, GL.GL_FLOAT, GL2.GL_LINEAR, 2, 4);
    tex_vel.setParam_WRAP_S_T(GL2.GL_CLAMP_TO_EDGE);
    
    tex_tmp.resize(context, tex_vel);
    tex_tmp.setParam_WRAP_S_T(GL2.GL_CLAMP_TO_EDGE);
    
    if(resized){
      reset();
    }
  }
  

  
  public void create(DwGLTexture tex_src){
    context.begin();
    
    int w_src = tex_src.w;
    int h_src = tex_src.h;
    
    resize(w_src, h_src);
    
    int w_dst = tex_vel.w;
    int h_dst = tex_vel.h;

    context.beginDraw(tex_vel);
    shader_create.begin();
    shader_create.uniform2f     ("wh_rcp" , 1f/w_dst, 1f/h_dst);
    shader_create.uniformTexture("tex_src", tex_src);
    shader_create.drawFullScreenQuad();
    shader_create.end();
    context.endDraw();

    for(int i = 0; i < param.blur_iterations; i++){
      blur(param.blur_radius);
    }
    
    context.end("FlowField.create()");
  }
  
  
  public void blur(int radius){
    gaussblur.apply(tex_vel, tex_vel, tex_tmp, radius);
  }
  
  
  public void display(PGraphicsOpenGL dst){

    int w = dst.width;
    int h = dst.height;
    
    int   lines_x   = (int) Math.ceil(w/param.line_spacing);
    int   lines_y   = (int) Math.ceil(h/param.line_spacing);
    int   num_lines = lines_x * lines_y;
    float scale     = param.line_scale;

    shader_display.vert.setDefine("LINE_MODE", param.line_mode);
    
    context.begin();
    context.beginDraw(dst);
    blendMode();
    shader_display.begin();
    shader_display.uniform4fv    ("col_A"         , 1, param.col_A);
    shader_display.uniform4fv    ("col_B"         , 1, param.col_B);
    shader_display.uniform2i     ("wh_lines"      ,    lines_x,    lines_y);
    shader_display.uniform2f     ("wh_lines_rcp"  , 1f/lines_x, 1f/lines_y);
    shader_display.uniform1f     ("vel_scale"     , scale);
    shader_display.uniformTexture("tex_velocity"  , tex_vel);
    shader_display.drawFullScreenLines(0, 0, w, h, num_lines, param.line_width, param.smooth);
    shader_display.end();
    context.endDraw();
    context.end("FlowField.display");
  }
  
  protected void blendMode(){
    context.gl.glEnable(GL.GL_BLEND);
    switch(param.blend_mode){
      case 0:  context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); break; // BLEND
      case 1:  context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE)                ; break; // ADD
      default: context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); break; // BLEND
    }
  }
  

}
