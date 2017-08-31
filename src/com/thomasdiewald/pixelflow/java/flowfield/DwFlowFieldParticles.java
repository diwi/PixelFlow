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
import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import processing.opengl.PGraphicsOpenGL;


public class DwFlowFieldParticles{
  
  public static class Param {
    
    public float   dissipation  = 0.990f;
    public float   inertia      = 0.900f;
    
    public float   timestep     = 1;
    
    public float   point_size   = 10f;
    public float   spwan_scale  = 1.0f;
    
//    public float[] col_A        = {1.0f, 0.35f, 0.125f, 1.0f};
//    public float[] col_A        = {0.3f, 0.70f, 1.4f, 1.2f};
    public float[] col_A        = {1.0f, 1.0f, 1.0f, 20f};
//    public float[] col_A        = {0.6f, 1.00f, 0.2f, 0.8f};
    public float[] col_B        = {0.0f, 0.0f, 0.0f, 0.0f};

    public int     blend_mode   = 0; // BLEND=0; ADD=1
    public boolean smooth       = true;
  }
  
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
  protected String data_path = DwPixelFlow.SHADER_DIR+"flowfield/";
//  protected String data_path = "D:/data/__Eclipse/workspace/WORKSPACE_FLUID/PixelFlow/src/com/thomasdiewald/pixelflow/glsl/flowfield/";
  
  public DwGLSLProgram shader_init;
  public DwGLSLProgram shader_update;
  public DwGLSLProgram shader_display;
  
  public DwGLTexture.TexturePingPong tex_position = new DwGLTexture.TexturePingPong();
  
  protected int spawn_idx = 0;

  public DwFlowFieldParticles(DwPixelFlow context, int num_particles){    
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    shader_init    = context.createShader(data_path + "flowfieldparticles_spawn.frag");
    shader_update  = context.createShader(data_path + "flowfieldparticles_update.frag");
    shader_display = context.createShader(data_path + "flowfieldparticles_display.glsl", data_path + "flowfieldparticles_display.glsl");
    shader_display.frag.setDefine("SHADER_FRAG", 1);
    shader_display.vert.setDefine("SHADER_VERT", 1);
    
    this.resize(num_particles);
  }
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_position.release();
  }
  
  public void reset(){
    spawn_idx = 0;
    tex_position.clear(0);
    
    spawn(-10, -10, 1, 1, 0, tex_position.src.w*tex_position.src.h);
  }
  
  public void resize(int num_particles_max){
    int size = (int) Math.ceil(Math.sqrt(num_particles_max));
    resize(size, size);
  }
  
  public void resize(int w, int h){
    tex_position.resize(context, GL2ES2.GL_RGBA32F, w, h, GL2ES2.GL_RGBA, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 4, 4);
    reset();
  }
  


  /**
   * 
   * @param px      x-pos (unnormalized)
   * @param py      y-pos (unnormalized)
   * @param radius  radius (unnormalized)
   * @param count   number of particles to spawn
   */
  public void spawn(float px, float py, int viewport_w, int viewport_h, float radius, int count){
    
    int w_position = tex_position.src.w;
    int h_position = tex_position.src.h;
    int spawn_max = w_position * h_position;
    
    count = Math.round(count * param.spwan_scale);
    
    if(spawn_idx >= spawn_max){
      spawn_idx = 0;
    }

    int spawn_lo = spawn_idx; 
    int spawn_hi = Math.min(spawn_lo + count, spawn_max); 
    float noise = (float)(Math.random() * Math.PI * 2);
    
    context.begin();
    context.beginDraw(tex_position.dst);
    shader_init.begin();
    shader_init.uniform2f     ("wh_viewport_rcp", 1f/viewport_w, 1f/viewport_h);
    shader_init.uniform1i     ("spawn_lo"    , spawn_lo);
    shader_init.uniform1i     ("spawn_hi"    , spawn_hi);
    shader_init.uniform2f     ("spawn_pos"   , px, py);
    shader_init.uniform1f     ("spawn_rad"   , radius);
    shader_init.uniform1f     ("noise"       , noise);
    shader_init.uniform2i     ("wh_position" , w_position, h_position);
    shader_init.uniformTexture("tex_position", tex_position.src);
    shader_init.drawFullScreenQuad();
    shader_init.end();
    context.endDraw();
    context.end("ParticelSystem.spawn");
    tex_position.swap();
    
    spawn_idx = spawn_hi;
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
    context.end("ParticelSystem.update");
  }
  
  
  public void display(PGraphicsOpenGL canvas){
    int w = canvas.width;
    int h = canvas.height;
    
    int w_position = tex_position.src.w;
    int h_position = tex_position.src.h;
    int spawn_max = w_position * h_position;
    
    context.begin();
    context.beginDraw(canvas);
    blendMode();
    shader_display.begin();
    shader_display.uniform1f     ("point_size"   , param.point_size);
    shader_display.uniform2i     ("wh_position"  , w_position, h_position);
    shader_display.uniform4fv    ("col_A"        , 1, param.col_A);
    shader_display.uniform4fv    ("col_B"        , 1, param.col_B);
    shader_display.uniformTexture("tex_position" , tex_position.src);
    shader_display.drawFullScreenPoints(0, 0, w, h, spawn_max, param.smooth);
    shader_display.end();
    context.endDraw();
    context.end("ParticelSystem.render");
  }
  
  protected void blendMode(){
    context.gl.glEnable(GL.GL_BLEND);
    switch(param.blend_mode){
      case 0:  context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); break; // BLEND
      case 1:  context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE)                ; break; // ADD
      default: context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); break; // BLEND
    }
  }
  
  
  public void display(PGraphicsOpenGL canvas, DwGLTexture tex_velocity){
    update(tex_velocity);
    display(canvas);
  }

  
}
