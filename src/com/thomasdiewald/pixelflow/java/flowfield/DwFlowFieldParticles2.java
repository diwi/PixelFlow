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

import processing.core.PConstants;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;


public class DwFlowFieldParticles2{
  
  public static class Param {
    
    public float   damping  = 0.990f;
//    public float   inertia      = 0.900f;
    
    public float   timestep     = 1;
    public float   velocity_mult = 1f;
    
    public float   point_exp    = 0.3f;
    
    public float   point_size_display   = 10f;
    public float   point_size_collision = 10f;
    
    public float   spwan_scale  = 1.0f;

    
    //    public float[] col_A        = {1.0f, 0.35f, 0.125f, 1.0f};
//    public float[] col_A        = {0.3f, 0.70f, 1.4f, 1.2f};
    public float[] col_A        = {1.0f, 1.0f, 1.0f, 20f};
//    public float[] col_A        = {0.6f, 1.00f, 0.2f, 0.8f};
    public float[] col_B        = {0.0f, 0.0f, 0.0f, 0.0f};

    public int     blend_mode   = 0; // BLEND=0; ADD=1

    public DwGLTexture tex_sprite = null;
  }
  
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
//  protected String data_path = DwPixelFlow.SHADER_DIR+"flowfield/";
  protected String data_path = "D:/data/__Eclipse/workspace/WORKSPACE_FLUID/PixelFlow/src/com/thomasdiewald/pixelflow/glsl/flowfield/";
  
  public DwGLSLProgram shader_init;
  public DwGLSLProgram shader_update;
  public DwGLSLProgram shader_collision;
  public DwGLSLProgram shader_display;
  public DwGLSLProgram shader_display_sprite;
  
  
  public DwGLTexture.TexturePingPong tex_particle = new DwGLTexture.TexturePingPong();
  
  public DwGLTexture tex_pos_cur = new DwGLTexture();
  public DwGLTexture tex_pos_old = new DwGLTexture();
  
  public DwGLTexture tex_collision = new DwGLTexture();
  
  protected int spawn_idx = 0;
  protected int spawn_num = 0;
  
  public DwFlowFieldParticles2(DwPixelFlow context, int num_particles){    
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    shader_init    = context.createShader(data_path + "particles_spawn.frag");
    shader_update  = context.createShader(data_path + "particles_update.frag");
    shader_collision  = context.createShader(data_path + "particles_collision.frag");
    shader_display = context.createShader(data_path + "particles_display.glsl", data_path + "particles_display.glsl");
    shader_display.frag.setDefine("SHADER_FRAG", 1);
    shader_display.vert.setDefine("SHADER_VERT", 1);
    
    
    
    
    shader_display_sprite = context.createShader((Object) (this+"SPRITE"), data_path + "particles_display.glsl", data_path + "particles_display.glsl");
    shader_display_sprite.frag.setDefine("SHADER_FRAG_DISPLAY", 1);
    shader_display_sprite.vert.setDefine("SHADER_VERT", 1);

    this.resize(num_particles);
  }
  
  
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_particle.release();
    tex_pos_cur.release();
    tex_pos_old.release();
  }
  
  
  public int getCount(){
    return spawn_num;
  }
  
  public void reset(){
    tex_particle.clear(0);
    tex_pos_cur.clear(0);
    tex_pos_old.clear(0);
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
    tex_pos_cur.resize(context, GL2ES2.GL_RG16F, w, h, GL2ES2.GL_RG, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 2, 4);
    tex_pos_old.resize(context, GL2ES2.GL_RG32F, w, h, GL2ES2.GL_RG, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 4, 4);
    reset();
  }
  


  public void spawn(float px, float py, int viewport_w, int viewport_h, float radius, int count){
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    int spawn_max = w_particle * h_particle;
    
    count = Math.round(count * param.spwan_scale);
    
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
  

  
  
  public void display(PGraphicsOpenGL canvas){
    int w = canvas.width;
    int h = canvas.height;
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    
    if(param.tex_sprite == null){
     //TODO
  
      context.begin();
      context.beginDraw(canvas);
      blendMode();
      shader_display.begin();
      shader_display.uniform1f     ("dist_exp"     , param.point_exp);
      shader_display.uniform1f     ("point_size"   , param.point_size_display);
      shader_display.uniform2i     ("wh_position"  , w_particle, h_particle);
      shader_display.uniform4fv    ("col_A"        , 1, param.col_A);
      shader_display.uniform4fv    ("col_B"        , 1, param.col_B);
      shader_display.uniformTexture("tex_position" , tex_particle.src);
      shader_display.drawFullScreenPoints(0, 0, w, h, spawn_num);
//      shader_display.drawFullScreenQuads(0, 0, w, h, spawn_num); // TODO
      shader_display.end();
      context.endDraw();
      context.end("DwFlowFieldParticles.display");
    }  else {
    
//    canvas.beginDraw();
//    canvas.blendMode(PConstants.BLEND);
    
    context.begin();
    context.beginDraw(canvas);
    blendMode();
    shader_display_sprite.begin();
    shader_display_sprite.uniform1f     ("point_size"   , param.point_size_display);
    shader_display_sprite.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display_sprite.uniform4fv    ("col_A"        , 1, param.col_A);
    shader_display_sprite.uniform4fv    ("col_B"        , 1, param.col_B);
    shader_display_sprite.uniformTexture("tex_position" , tex_particle.src);
    shader_display_sprite.uniformTexture("tex_alpha"    , param.tex_sprite);
    shader_display_sprite.drawFullScreenPoints(0, 0, w, h, spawn_num);
//    shader_display_sprite.drawFullScreenQuads(0, 0, w, h, spawn_numh); // TODO
    shader_display_sprite.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.display");
    
    
//    canvas.endDraw();
    
    }
    
    
    
    
  }
  

  public void createCollisionMap(int w, int h){

    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;

    float[] col_A = {1,1,1,1};
    float[] col_B = {0,0,0,0};

    // double and odd
    int collision_radius = (int) Math.ceil(param.point_size_collision * 2);
    if((collision_radius & 1) == 0){
      collision_radius += 1;
    }

    context.begin();
    
    tex_collision.resize(context, GL2ES2.GL_R32F, w, h, GL2ES2.GL_RED , GL2ES2.GL_FLOAT, GL2ES2.GL_LINEAR, 1, 4);
    tex_collision.clear(0);
    
    context.beginDraw(tex_collision);
    context.gl.glEnable(GL.GL_BLEND);
    context.gl.glBlendEquation(GL.GL_FUNC_ADD);
    context.gl.glBlendFunc(GL.GL_SRC_COLOR, GL.GL_ONE);
    shader_display.begin();
    shader_display.uniform1f     ("dist_exp"     , 0.90f);
    shader_display.uniform1f     ("point_size"   , collision_radius);
    shader_display.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display.uniform4fv    ("col_A"        , 1, col_A);
    shader_display.uniform4fv    ("col_B"        , 1, col_B);
    shader_display.uniformTexture("tex_position" , tex_particle.src);
    shader_display.drawFullScreenPoints(0, 0, w, h, spawn_num, false);
    shader_display.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.createCollisionMap");
  }
  
  
  public void updateCollision(DwGLTexture tex_velocity){
    updateCollision(tex_velocity, 1.0f);
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
    shader_collision.uniform1f     ("timestep"       , param.timestep);
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
    shader_update.uniform1f     ("timestep"       , param.timestep);
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
//    switch(param.blend_mode){
//      case 0:  context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); break; // BLEND
//      case 1:  context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE                ); break; // ADD
//      default: context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); break; // BLEND
//    }
    
    switch(param.blend_mode){
    case 0:  context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE); break; // BLEND
    case 1:  context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE                , GL.GL_ONE, GL.GL_ONE); break; // ADD
    default: context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE); break; // BLEND
  }

  }

  
  
}
