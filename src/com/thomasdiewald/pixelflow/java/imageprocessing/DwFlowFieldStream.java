/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.imageprocessing;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.opengl.PGraphicsOpenGL;


public class DwFlowFieldStream{
  
  public static class Param {
    
    public float   dissipation  = 0.97f;
    public float   inertia      = 0.80f;
    
    public float   timestep     = 10f;
    
    public int     line_length  = 30;
    public float   line_width   = 1f;
    public float   line_spacing = 10;
 
    public float[] col_A        = {0.25f, 0.50f, 1.0f, 1.00f};
//    public float[] col_B        = {0.9f, 0.95f, 1.0f, 1.0f};
    
//    public float[] col_B        = {0.1f, 0.05f, 0.01f, 1.0f};
       
    float s = 0.05f;
    public float[] col_B        = {1.0f*s, 0.3f*s, 0.1f*s, 1.0f};
//    public float[] col_B        = {0.0f, 0.0f, 0.0f, 1.0f};
    public int     blend_mode   = 1; // BLEND=0; ADD=1
  }
  
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
  protected String data_path = DwPixelFlow.SHADER_DIR+"Filter/";

  public DwGLSLProgram shader_init  ;
  public DwGLSLProgram shader_update;
  public DwGLSLProgram shader_display;
  
  public DwGLTexture.TexturePingPong tex_position = new DwGLTexture.TexturePingPong();

  public DwFlowFieldStream(DwPixelFlow context){
    this.context = context;
    
    shader_init    = context.createShader(data_path+"flowfieldstream_init.frag");
    shader_update  = context.createShader(data_path+"flowfieldstream_update.frag");
    shader_display = context.createShader(data_path+"flowfieldstream_display.glsl", data_path+"flowfieldstream_display.glsl");
    shader_display.frag.setDefine("SHADER_FRAG", 1);
    shader_display.vert.setDefine("SHADER_VERT", 1);
  }
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_position.release();
  }
  
  public void resize(int w, int h){
    tex_position.resize(context, GL2ES2.GL_RGBA32F, w, h, GL2ES2.GL_RGBA, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 4, 4);
  }
  
  public void reset(){
    int w = tex_position.dst.w;
    int h = tex_position.dst.h;
    context.begin();
    context.beginDraw(tex_position.dst);
    shader_init.begin();
    shader_init.uniform2f("wh_rcp", 1f/w, 1f/h);
    shader_init.drawFullScreenQuad();
    shader_init.end();
    context.endDraw();
    tex_position.swap();
    context.end("FlowFieldStream.init");

  }
  
  public void update(DwGLTexture tex_velocity){
    
    int w_velocity = tex_velocity.w;
    int h_velocity = tex_velocity.h;

    context.begin();
    context.beginDraw(tex_position.dst);
    shader_update.begin();
    shader_update.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update.uniform1f     ("timestep"       , param.timestep);
    shader_update.uniform1f     ("rdx"            , 1f);
    shader_update.uniform1f     ("dissipation"    , param.dissipation);
    shader_update.uniform1f     ("inertia"        , param.inertia);
    shader_update.uniformTexture("tex_position"   , tex_position.src);
    shader_update.uniformTexture("tex_velocity"   , tex_velocity);
    shader_update.drawFullScreenQuad();
    shader_update.end();
    context.endDraw();
    tex_position.swap();
    context.end("FlowFieldStream.update");
  }
  
  
  
//  DwGLTexture tex_tmp = new DwGLTexture();
  
  public void display(PGraphicsOpenGL canvas, DwGLTexture tex_velocity){
    
    int w_dst = canvas.width;
    int h_dst = canvas.height;
    
    {
      int w_stream = (int) Math.ceil(w_dst / param.line_spacing);
      int h_stream = (int) Math.ceil(h_dst / param.line_spacing);
      resize(w_stream, h_stream);
    }
    
    int w_position = tex_position.src.w;
    int h_position = tex_position.src.h;
    
//    tex_tmp.resize(context, GL.GL_RGBA, w_dst, h_dst, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, GL.GL_NEAREST, 4, 1);
//    DwFilter.get(context).copy.apply(canvas, tex_tmp);
    
    context.begin();
    
    reset();

    for(int i = 0; i < param.line_length; i++){
      update(tex_velocity);
      
      float line_uv = i/(float)(param.line_length-1);
      line_uv = (float) Math.pow(line_uv, 0.25f);
      
      context.beginDraw(canvas);  
      blendMode();
      shader_display.begin();
      shader_display.uniform2i     ("wh_position"    , w_position, h_position);
      shader_display.uniform1f     ("line_uv"        , line_uv);
      shader_display.uniform4fv    ("col_A"          , 1, param.col_A);
      shader_display.uniform4fv    ("col_B"          , 1, param.col_B);
      shader_display.uniformTexture("tex_position_A" , tex_position.src);
      shader_display.uniformTexture("tex_position_B" , tex_position.dst);
      shader_display.drawFullScreenLines(0, 0, w_dst, h_dst, w_position * h_position, param.line_width);
      shader_display.end();
      context.endDraw();
    }

//    context.end("FlowFieldStream.display");
//    DwFilter.get(context).copy.apply(tex_tmp, canvas);
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
