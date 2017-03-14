/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.fluid;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;

import processing.opengl.PGraphics2D;


public class DwFluidStreamLines2D{
  
  public Param param = new Param();
  
  static public class Param{
    public int   line_length    = 20;
    public float velocity_scale = 10f;
    public float velocity_min   = 10f;
  }

  public DwGLSLProgram shader_streamlineInit  ;
  public DwGLSLProgram shader_streamlineUpdate;
  public DwGLSLProgram shader_streamlineRender;
  
  public DwGLTexture.TexturePingPong tex_vertices = new DwGLTexture.TexturePingPong();

  public DwPixelFlow context;
  
  public int lines_x;
  public int lines_y;


  
  public DwFluidStreamLines2D(DwPixelFlow context){
    this.context = context;
    this.context.papplet.registerMethod("dispose", this);
  }
  
//  public Streamlines(PixelFlow context, int num_lines_x, int num_lines_y){
//    context.papplet.registerMethod("dispose", this);
//    this.resize(context, num_lines_x, num_lines_y);
//  }
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_vertices.release();
  }
  
  public void resize(int num_lines_x_, int num_lines_y_){
    
    if(this.lines_x  == num_lines_x_ && 
       this.lines_y  == num_lines_y_ ){
      return;
    }
    
    context.begin();
       
    this.lines_x      = num_lines_x_;
    this.lines_y      = num_lines_y_;

    // create shader
    shader_streamlineInit   = context.createShader(DwPixelFlow.SHADER_DIR+"Streamlines/streamlineInit.frag");
    shader_streamlineUpdate = context.createShader(DwPixelFlow.SHADER_DIR+"Streamlines/streamlineUpdate.frag");
    shader_streamlineRender = context.createShader(DwPixelFlow.SHADER_DIR+"Streamlines/streamlineRender.vert", DwPixelFlow.SHADER_DIR+"Streamlines/streamlineRender.frag");
    
    // allocate texture
    tex_vertices.resize(context, GL2ES2.GL_RG32F, lines_x, lines_y, GL2ES2.GL_RG, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 4, 4);
    tex_vertices.src.clear(0);
    tex_vertices.dst.clear(0);
    
    context.end("Streamlines.resize");
  }
  

  public void init(){
    context.begin();
    context.beginDraw(tex_vertices.dst);
    shader_streamlineInit.begin();
    shader_streamlineInit.uniform2f("wh", lines_x, lines_y);
    shader_streamlineInit.drawFullScreenQuad();
    shader_streamlineInit.end();
    context.endDraw();
    context.end("Streamlines.init");
    tex_vertices.swap();
  }
  
  public void update(DwFluid2D fluid){
    context.begin();
    context.beginDraw(tex_vertices.dst);
    shader_streamlineUpdate.begin();
    shader_streamlineUpdate.uniform2f     ("wh_fluid"      , fluid.fluid_w, fluid.fluid_h);
    shader_streamlineUpdate.uniform2f     ("wh_particles"  , lines_x, lines_y);
    shader_streamlineUpdate.uniform1f     ("timestep"      , fluid.param.timestep);
    shader_streamlineUpdate.uniform1f     ("rdx"           , 1.0f / fluid.param.gridscale);
    shader_streamlineUpdate.uniform1f     ("velocity_scale", param.velocity_scale);
    shader_streamlineUpdate.uniform1f     ("velocity_min"  , param.velocity_min/(float)fluid.fluid_w);
    shader_streamlineUpdate.uniformTexture("tex_particles" , tex_vertices.src);
    shader_streamlineUpdate.uniformTexture("tex_velocity"  , fluid.tex_velocity.src);
    shader_streamlineUpdate.drawFullScreenQuad();
    shader_streamlineUpdate.end();
    context.endDraw();
    context.end("Streamlines.update");
    tex_vertices.swap();
  }
  
  
  public void render(PGraphics2D dst, DwFluid2D fluid, int spacing){
    
    int w = dst.width;
    int h = dst.height;
    
    {
      int   lines_x    = Math.round(w / spacing);
      int   lines_y    = Math.round(h / spacing);
//      float space_x    = w / (float) lines_x;
//      float space_y    = h / (float) lines_y;
      resize(lines_x, lines_y);
    }
    
//    dst.blendMode(PConstants.BLEND);
//    dst.blendMode(PConstants.ADD);
    
    context.begin();
    
    init();
    
    for(int segment_idx = 0; segment_idx < param.line_length; segment_idx++){
      update(fluid);
      
//      dst.beginDraw();
//      dst.blendMode(PConstants.BLEND);
//      dst.blendMode(PConstants.ADD);

      context.begin();
      context.beginDraw(dst);
      context.gl.glEnable(GL.GL_BLEND);
      context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); // BLEND
      context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);                 // ADD
      
      shader_streamlineRender.begin();
      shader_streamlineRender.uniform2i     ("num_lines"      , lines_x, lines_y);
      shader_streamlineRender.uniform1f     ("line_uv"        , segment_idx/(float)(param.line_length-1));
      shader_streamlineRender.uniformTexture("tex_vertices_V0", tex_vertices.src);
      shader_streamlineRender.uniformTexture("tex_vertices_V1", tex_vertices.dst);
      shader_streamlineRender.drawFullScreenLines(0, 0, w, h, lines_x * lines_y, 1f);
      shader_streamlineRender.end();
      
      context.endDraw();
      context.end("Streamlines.render");
      
//      dst.endDraw();
    }
    
    context.end("Streamlines.update");

//    dst.loadTexture();
  }
    

  
}
