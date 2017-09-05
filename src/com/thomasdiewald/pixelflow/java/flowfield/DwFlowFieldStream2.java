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
import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import processing.opengl.PGraphicsOpenGL;


public class DwFlowFieldStream2{
  
  public static class Param {
    
    // particle size rendering/collision
    public float   size_display   = 10f;
    public float   size_collision = 10f;
    
    // velocity damping
    public float   damping  = 0.98f;
    
    // display stuff
    public float   shader_collision_mult = 0.1f;
    
    public float[] col_A        = {1.0f, 1.0f, 1.0f, 10.0f};
    public float[] col_B        = {0.0f, 0.0f, 0.0f,  0.0f};

    public int     blend_mode   = 0; // BLEND=0; ADD=1

    public boolean smooth = true;
  }
  
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
//  protected String data_path = DwPixelFlow.SHADER_DIR+"flowfield/";
  protected String data_path = "D:/data/__Eclipse/workspace/WORKSPACE_FLUID/PixelFlow/src/com/thomasdiewald/pixelflow/glsl/flowfield/";
  
  public DwGLSLProgram shader_init;
  public DwGLSLProgram shader_init_grid;
  public DwGLSLProgram shader_update;
  public DwGLSLProgram shader_collision;
  public DwGLSLProgram shader_display_collision;
  public DwGLSLProgram shader_display_sprite;
  
  public DwGLSLProgram shader_create_sprite;

  public DwGLTexture.TexturePingPong tex_particle = new DwGLTexture.TexturePingPong();

  protected int spawn_idx = 0;
  protected int spawn_num = 0;
  
  public DwFlowFieldStream2(DwPixelFlow context, int num_particles){    
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    shader_init              = context.createShader(data_path + "particles_spawn.frag");
    shader_init_grid         = context.createShader(data_path + "particles_spawn_grid.frag");
    shader_update            = context.createShader(data_path + "particles_update.frag");
    shader_collision         = context.createShader(data_path + "particles_collision.frag");
    
//    String filename = data_path + "particles_display_quads.glsl";
    String filename = data_path + "particles_display_points.glsl";
    shader_display_sprite    = context.createShader((Object) (this+"SPRITE"   ), filename, filename);
    shader_display_collision = context.createShader((Object) (this+"COLLISION"), filename, filename);

    shader_display_sprite.frag.setDefine("SHADER_FRAG_DISPLAY", 1);
    shader_display_sprite.vert.setDefine("SHADER_VERT"        , 1);
    
    shader_display_collision.frag.setDefine("SHADER_FRAG_COLLISION", 1);
    shader_display_collision.vert.setDefine("SHADER_VERT"          , 1);

    resize(num_particles);
  }
  
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_particle.release();
  }
  
  public int getCount(){
    return spawn_num;
  }
  
  public void reset(){
    tex_particle.clear(0);
    spawn_idx = 0;
    spawn(-2, -2, 1, 1, 1, tex_particle.src.w * tex_particle.src.h);
    spawn_idx = 0;
    spawn_num = 0;
  }
  
  
  public void resize(int num_particles_max){
    int size = (int) Math.ceil(Math.sqrt(num_particles_max));
    resize(size, size);
  }
  
  public void resize(int w, int h){
    tex_particle.resize(context, GL2ES2.GL_RGBA32F, w, h, GL2ES2.GL_RGBA, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 4, 4);
    reset();
  }
  


  public void spawn(float px, float py, int viewport_w, int viewport_h, float radius, int count){
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    int spawn_max = w_particle * h_particle;

    if(spawn_idx >= spawn_max){
      spawn_idx = 0;
    }

    int spawn_lo = spawn_idx; 
    int spawn_hi = Math.min(spawn_lo + count, spawn_max); 
    float noise = (float)(Math.random() * Math.PI * 2);
    
    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_init.begin();
    shader_init.uniform2f     ("wh_viewport_rcp", 1f/viewport_w, 1f/viewport_h);
    shader_init.uniform1i     ("spawn_lo"    , spawn_lo);
    shader_init.uniform1i     ("spawn_hi"    , spawn_hi);
    shader_init.uniform2f     ("spawn_pos"   , px, py);
    shader_init.uniform1f     ("spawn_rad"   , radius);
    shader_init.uniform1f     ("noise"       , noise);
    shader_init.uniform2i     ("wh_position" , w_particle, h_particle);
    shader_init.uniformTexture("tex_position", tex_particle.src);
    shader_init.drawFullScreenQuad();
    shader_init.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.spawn");
    tex_particle.swap();
    
    spawn_idx = spawn_hi;
    spawn_num += spawn_hi-spawn_lo;
    spawn_num = Math.min(spawn_num, spawn_max); 
  }
  
  
  
  public void spawnGrid(int nx, int ny){
    
    int count = nx * ny;
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    int spawn_max = w_particle * h_particle;

    if(spawn_idx >= spawn_max){
      spawn_idx = 0;
    }

    int spawn_lo = spawn_idx; 
    int spawn_hi = Math.min(spawn_lo + count, spawn_max); 

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_init_grid.begin();
    shader_init_grid.uniform1i     ("spawn_lo"    , spawn_lo);
    shader_init_grid.uniform1i     ("spawn_hi"    , spawn_hi);
    shader_init_grid.uniform2i     ("num_xy"      , nx, ny);
    shader_init_grid.uniform2i     ("wh_position" , w_particle, h_particle);
    shader_init_grid.uniformTexture("tex_position", tex_particle.src);
    shader_init_grid.drawFullScreenQuad();
    shader_init_grid.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.spawnGrid");
    tex_particle.swap();
    
    spawn_idx = spawn_hi;
    spawn_num += spawn_hi-spawn_lo;
    spawn_num = Math.min(spawn_num, spawn_max); 
  }
  
  
  
  
  
  
  
  
  
  


  public void display(PGraphicsOpenGL canvas){
    int w = canvas.width;
    int h = canvas.height;
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    
    context.begin();
    context.beginDraw(canvas);
    blendMode();
    shader_display_sprite.begin();
    shader_display_sprite.uniform1f     ("point_size"   , param.size_display);
    shader_display_sprite.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display_sprite.uniform4fv    ("col_A"        , 1, param.col_A);
    shader_display_sprite.uniform4fv    ("col_B"        , 1, param.col_B);
    shader_display_sprite.uniformTexture("tex_position" , tex_particle.src);
    shader_display_sprite.drawFullScreenPoints(0, 0, w, h, spawn_num, false);
//    shader_display_sprite.drawFullScreenQuads(0, 0, w, h, spawn_num);
    shader_display_sprite.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.display");
  }
  

  public void updateCollision(DwGLTexture tex_velocity, float velocity_mult){
    
    int w_velocity = tex_velocity.w;
    int h_velocity = tex_velocity.h;
    
    int w_particle = tex_particle.dst.w;
    int h_particle = tex_particle.dst.h;

    int viewport_w = w_particle;
    int viewport_h = (spawn_num + w_particle) / w_particle;
    
    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_collision.begin();
    shader_collision.uniform1i     ("spawn_hi"       , spawn_num);
    shader_collision.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_collision.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_collision.uniform1f     ("velocity_mult"  , velocity_mult);
    shader_collision.uniformTexture("tex_position"   , tex_particle.src);
    shader_collision.uniformTexture("tex_velocity"   , tex_velocity);
    shader_collision.drawFullScreenQuad(0, 0, viewport_w, viewport_h);
    shader_collision.end();
    context.endDraw();
    tex_particle.swap();
    context.end("DwFlowFieldParticles.updateCollision");
  }
  
 
  public void update(DwGLTexture tex_velocity, float velocity_mult){
    
    int w_velocity = tex_velocity.w;
    int h_velocity = tex_velocity.h;
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    
    int viewport_w = w_particle;
    int viewport_h = (spawn_num + w_particle) / w_particle;

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update.begin();
    shader_update.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update.uniform1f     ("velocity_mult"  , velocity_mult);
    shader_update.uniform1f     ("damping"        , param.damping);
    shader_update.uniformTexture("tex_position"   , tex_particle.src);
    shader_update.uniformTexture("tex_velocity"   , tex_velocity);
    shader_update.drawFullScreenQuad(0, 0, viewport_w, viewport_h);
    shader_update.end();
    context.endDraw();
    tex_particle.swap();
    context.end("DwFlowFieldParticles.update");
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
