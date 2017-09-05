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
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.opengl.PGraphicsOpenGL;


public class DwFlowFieldParticles{
  
  public static class Param {
    
    // particle size rendering/collision
    public float   size_display   = 10f;
    public float   size_collision = 10f;
    
    // velocity damping
    public float   velocity_damping  = 0.98f;
    
    // update
    public float   collision_mult = 1;
    public int     collision_steps = 1;
    
    // display stuff
    public float   shader_collision_mult = 0.1f;
    
    public float[] col_A        = {1.0f, 1.0f, 1.0f, 10.0f};
    public float[] col_B        = {0.0f, 0.0f, 0.0f,  0.0f};

    public int     blend_mode   = 0; // BLEND=0; ADD=1
    

    public float   display_line_width = 0.5f;
    public boolean display_line_smooth = true;
    

    public DwGLTexture tex_sprite = new DwGLTexture();
  }
  
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
//  protected String data_path = DwPixelFlow.SHADER_DIR+"flowfield/";
  protected String data_path = "D:/data/__Eclipse/workspace/WORKSPACE_FLUID/PixelFlow/src/com/thomasdiewald/pixelflow/glsl/flowfield/";
  
  public DwGLSLProgram shader_init;
  public DwGLSLProgram shader_init_grid;
  public DwGLSLProgram shader_update;
  public DwGLSLProgram shader_update_collision;
  public DwGLSLProgram shader_display;
  public DwGLSLProgram shader_display_collision;
  public DwGLSLProgram shader_display_lines;
  
  public DwGLSLProgram shader_create_sprite;

  public DwGLTexture.TexturePingPong tex_particle = new DwGLTexture.TexturePingPong();
  
  public DwGLTexture tex_collision = new DwGLTexture();
  
  protected int spawn_idx = 0;
  protected int spawn_num = 0;
  
  public DwFlowFieldParticles(DwPixelFlow context, int num_particles){    
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    String filename;
    
    shader_create_sprite     = context.createShader(data_path + "create_sprite_texture.frag");
  
    shader_init              = context.createShader(data_path + "particles_spawn.frag");
    shader_init_grid         = context.createShader(data_path + "particles_spawn_grid.frag");
    shader_update            = context.createShader(data_path + "particles_update.frag");
    shader_update_collision  = context.createShader(data_path + "particles_update_collision.frag");
    
    filename = data_path + "particles_display_lines.glsl";
    shader_display_lines     = context.createShader((Object) (this+"LINES"   ), filename, filename);
    shader_display_lines.frag.setDefine("SHADER_FRAG", 1);
    shader_display_lines.vert.setDefine("SHADER_VERT", 1);
    
    // filename = data_path + "particles_display_quads.glsl";
    filename = data_path + "particles_display_points.glsl";
    shader_display           = context.createShader((Object) (this+"SPRITE"   ), filename, filename);
    shader_display_collision = context.createShader((Object) (this+"COLLISION"), filename, filename);

    shader_display.frag.setDefine("SHADER_FRAG_DISPLAY", 1);
    shader_display.vert.setDefine("SHADER_VERT"        , 1);
    
    shader_display_collision.frag.setDefine("SHADER_FRAG_COLLISION", 1);
    shader_display_collision.vert.setDefine("SHADER_VERT"          , 1);

    createSpriteTexture(32, 2, 1, 1);
    
    resize(num_particles);
  }
  
  
  public void createSpriteTexture(int size, float e1, float e2, float mult){
    context.begin();
    param.tex_sprite.resize(context, GL.GL_RGBA8, size, size, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, GL.GL_LINEAR, 1, 4);
    context.beginDraw(param.tex_sprite);
    shader_create_sprite.begin();
    shader_create_sprite.uniform2f("wh_rcp", 1f/size, 1f/size);
    shader_create_sprite.uniform1f("e1"    , e1);
    shader_create_sprite.uniform1f("e2"    , e2);
    shader_create_sprite.uniform1f("mult"  , mult);
    shader_create_sprite.drawFullScreenQuad();
    shader_create_sprite.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.createSpriteTexture");
  }
  
  
  public void dispose(){
    release();
  }
  
  public void release(){
    param.tex_sprite.release();
    tex_particle.release();
  }
  
  
  public int getCount(){
    return spawn_num;
  }
  
  public int getCollisionRadius(){
    // double and odd
    int collision_radius = (int) Math.ceil(param.size_collision * 2);
    if((collision_radius & 1) == 0){
      collision_radius += 1;
    }
    return collision_radius;
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
    
    int fsq_w = w_particle;
    int fsq_h = (spawn_hi + w_particle) / w_particle;
    
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
    shader_init.drawFullScreenQuad(0, 0, fsq_w, fsq_h);
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
    
    int fsq_w = w_particle;
    int fsq_h = (spawn_hi + w_particle) / w_particle;

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_init_grid.begin();
    shader_init_grid.uniform1i     ("spawn_lo"    , spawn_lo);
    shader_init_grid.uniform1i     ("spawn_hi"    , spawn_hi);
    shader_init_grid.uniform2i     ("num_xy"      , nx, ny);
    shader_init_grid.uniform2i     ("wh_position" , w_particle, h_particle);
    shader_init_grid.uniformTexture("tex_position", tex_particle.src);
    shader_init.drawFullScreenQuad(0, 0, fsq_w, fsq_h);
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
    shader_display.begin();
    shader_display.uniform1f     ("shader_collision_mult", param.shader_collision_mult);
    shader_display.uniform1f     ("point_size"   , param.size_display);
    shader_display.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display.uniform4fv    ("col_A"        , 1, param.col_A);
    shader_display.uniform4fv    ("col_B"        , 1, param.col_B);
    shader_display.uniformTexture("tex_collision", tex_collision);
    shader_display.uniformTexture("tex_position" , tex_particle.src);
    shader_display.uniformTexture("tex_sprite"   , param.tex_sprite);
    shader_display.drawFullScreenPoints(0, 0, w, h, spawn_num, false);
    shader_display.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.display");
  }
  
  public void displayTrail(PGraphicsOpenGL canvas){
    int w = canvas.width;
    int h = canvas.height;
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    
    float   lwidth  = param.display_line_width;
    boolean lsmooth = param.display_line_smooth;
    
    context.begin();
    context.beginDraw(canvas);
    blendMode();
    shader_display_lines.begin();
    shader_display_lines.uniform1f     ("shader_collision_mult", param.shader_collision_mult);
    shader_display_lines.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display_lines.uniform4fv    ("col_A"        , 1, param.col_A);
    shader_display_lines.uniformTexture("tex_collision", tex_collision);
    shader_display_lines.uniformTexture("tex_position" , tex_particle.src);
    shader_display_lines.drawFullScreenLines(0, 0, w, h, spawn_num, lwidth, lsmooth);
    shader_display_lines.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.display");
  }
  

  

  public void createCollisionMap(int w, int h){
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;

    int collision_radius = getCollisionRadius();

    context.begin();
    
    tex_collision.resize(context, GL2ES2.GL_R32F, w, h, GL2ES2.GL_RED , GL2ES2.GL_FLOAT, GL2ES2.GL_LINEAR, 1, 4);

    context.beginDraw(tex_collision);
    context.gl.glColorMask(true, false, false, false);
    context.gl.glClearColor(0,0,0,0);
    context.gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT);
    context.gl.glEnable(GL.GL_BLEND);
    context.gl.glBlendEquation(GL.GL_FUNC_ADD);
    context.gl.glBlendFunc(GL.GL_SRC_COLOR, GL.GL_ONE);
    shader_display_collision.begin();
    shader_display_collision.uniform1f     ("point_size"   , collision_radius);
    shader_display_collision.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display_collision.uniformTexture("tex_position" , tex_particle.src);
    shader_display_collision.drawFullScreenPoints(0, 0, w, h, spawn_num, false);
    shader_display_collision.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.createCollisionMap");
  }
  

  

  
  
  public void updateCollision(DwGLTexture tex_velocity, float velocity_mult){
    
    int w_velocity = tex_velocity.w;
    int h_velocity = tex_velocity.h;
    
    int w_particle = tex_particle.dst.w;
    int h_particle = tex_particle.dst.h;

    int viewport_w = w_particle;
    int viewport_h = (spawn_num + w_particle) / w_particle;
    
    int collision_radius = getCollisionRadius();
    float acc_min = DwUtils.SQRT2 * 0.005f;
    float acc_max = DwUtils.SQRT2 * collision_radius * 3.00f; 

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update_collision.begin();
    shader_update_collision.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update_collision.uniform2f     ("acc_minmax"     , acc_min, acc_max);
    shader_update_collision.uniform1f     ("acc_mult"       , velocity_mult);
    shader_update_collision.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update_collision.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update_collision.uniformTexture("tex_position"   , tex_particle.src);
    shader_update_collision.uniformTexture("tex_velocity"   , tex_velocity);
    shader_update_collision.drawFullScreenQuad(0, 0, viewport_w, viewport_h);
    shader_update_collision.end();
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
    
    int collision_radius = getCollisionRadius();
    float acc_min = DwUtils.SQRT2 * 0.00f;
    float acc_max = DwUtils.SQRT2 * collision_radius * 3.00f; acc_max = Math.min(acc_max, 6);
    float vel_min = DwUtils.SQRT2 * 0.00f;
    float vel_max = DwUtils.SQRT2 * collision_radius * 3.00f; vel_max = Math.min(vel_max, 6);

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update.begin();
    shader_update.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update.uniform2f     ("acc_minmax"     , acc_min, acc_max);
    shader_update.uniform2f     ("vel_minmax"     , vel_min, vel_max);
    shader_update.uniform1f     ("acc_mult"       , velocity_mult);
    shader_update.uniform1f     ("vel_mult"       , param.velocity_damping);
    shader_update.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
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
