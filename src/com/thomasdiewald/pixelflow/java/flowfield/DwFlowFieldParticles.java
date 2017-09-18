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
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DistanceTransform;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;


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
    
    // particle trails
    public float   display_line_width = 0.5f;
    public boolean display_line_smooth = true;
    
    // point sprite texture
    public DwGLTexture tex_sprite = new DwGLTexture();
  }
  
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
  public DwGLSLProgram shader_spawn_radial;
  public DwGLSLProgram shader_spawn_rect;
  public DwGLSLProgram shader_update_vel;
  public DwGLSLProgram shader_update_acc;
  
  public DwGLSLProgram shader_display_particles;
  public DwGLSLProgram shader_display_trails;
  
  public DwGLSLProgram shader_collision_dist;
  public DwGLSLProgram shader_obstacles_dist;
  
  public DwGLSLProgram shader_create_sprite;

  public DwGLTexture.TexturePingPong tex_particle = new DwGLTexture.TexturePingPong();
  
  public DwGLTexture tex_collision_dist = new DwGLTexture();
  public DwGLTexture tex_obstacles_dist = new DwGLTexture();
  
  public DwFlowField ff_col;
  public DwFlowField ff_obs;
  public DwFlowField ff_acc;
  public DwFlowField ff_sum;
  
  
  public DwGLSLProgram shader_obstacles_FG;
  public DwGLTexture tex_obstacles_FG = new DwGLTexture();
  public DistanceTransform dt_obstacles;

  public Merge merge;
  
  protected int spawn_idx = 0;
  protected int spawn_num = 0;
  
  public DwFlowFieldParticles(DwPixelFlow context, int num_particles){    
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    String filename;
    
    String data_path = DwPixelFlow.SHADER_DIR+"flowfield/";
    
    shader_create_sprite     = context.createShader(data_path + "create_sprite_texture.frag");
  
    
    shader_spawn_radial      = context.createShader((Object) (this+"radial"    ), data_path + "particles_spawn.frag");
    shader_spawn_rect        = context.createShader((Object) (this+"rect"      ), data_path + "particles_spawn.frag");
    
    shader_spawn_radial.frag.setDefine("SPAWN_RADIAL", 1);
    shader_spawn_rect  .frag.setDefine("SPAWN_RECT"  , 1);
    
    
    shader_update_vel  = context.createShader((Object) (this+"update"    ), data_path + "particles_update.frag");
    shader_update_acc  = context.createShader((Object) (this+"collision" ), data_path + "particles_update.frag");
    
    shader_update_vel.frag.setDefine("UPDATE_VEL", 1);
    shader_update_acc.frag.setDefine("UPDATE_ACC", 1);
    
    
    
    filename = data_path + "particles_display_lines.glsl";
    shader_display_trails     = context.createShader((Object) (this+"LINES"   ), filename, filename);
    shader_display_trails.frag.setDefine("SHADER_FRAG", 1);
    shader_display_trails.vert.setDefine("SHADER_VERT", 1);
    
    // filename = data_path + "particles_display_quads.glsl";
    filename = data_path + "particles_display_points.glsl";
    shader_display_particles = context.createShader((Object) (this+"SPRITE"   ), filename, filename);
    shader_collision_dist    = context.createShader((Object) (this+"COLLISION"), filename, filename);

    shader_display_particles.frag.setDefine("SHADER_FRAG_DISPLAY", 1);
    shader_display_particles.vert.setDefine("SHADER_VERT"        , 1);
    
    shader_collision_dist.frag.setDefine("SHADER_FRAG_COLLISION", 1);
    shader_collision_dist.vert.setDefine("SHADER_VERT"          , 1);
    
    
    shader_obstacles_dist = context.createShader(data_path+"obstacles_dist.frag");
    shader_obstacles_FG  = context.createShader(data_path+"obstacles_FG.frag");
    
    dt_obstacles = new DistanceTransform(context);
    
    
//    obstacles = new DwObstacles(context);

    ff_col = new DwFlowField(context);
    ff_col.param.blur_iterations = 1;
    ff_col.param.blur_radius     = 2;
    
    ff_obs = new DwFlowField(context);
    ff_obs.param.blur_iterations = 1;
    ff_obs.param.blur_radius     = 2;

    ff_sum = new DwFlowField(context);
    ff_sum.param.blur_iterations = 1;
    ff_sum.param.blur_radius     = 4;
    
    ff_acc = new DwFlowField(context);
    ff_acc.param.blur_iterations = 0;
    ff_acc.param.blur_radius     = 2;
    
    
    merge = new Merge(context);
    
    createSpriteTexture(32, 2, 1, 1);
    
    resizeParticlesCount(num_particles);
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

    SpawnRect sr = new SpawnRect();
    sr.num(tex_particle.src.w, tex_particle.src.h);
    sr.pos(-2,-2);
    sr.dim( 1, 1);
    
    spawn(1, 1, sr);
    
    spawn_idx = 0;
    spawn_num = 0;
  }
  

  
  
  public void release(){
    dt_obstacles.release();
    tex_obstacles_FG.release();
    
    ff_acc.release();
    ff_col.release();
    ff_obs.release();
    ff_sum.release();
    
    param.tex_sprite.release();
    tex_particle.release();
    
    tex_collision_dist.release();
    tex_obstacles_dist.release();
  }
  

  public void resizeWorld(int w, int h){
    ff_col.resize(w, h);
    ff_obs.resize(w, h);
    ff_acc.resize(w, h);
    ff_sum.resize(w, h);
    
    tex_obstacles_FG.resize(context, GL2.GL_RGBA, w, h, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, 4, 1);
    
    tex_collision_dist.resize(context, GL2.GL_R32F, w, h, GL2.GL_RED, GL2.GL_FLOAT, GL2.GL_LINEAR, 1, 4);
    tex_obstacles_dist.resize(context, GL2.GL_R32F, w, h, GL2.GL_RED, GL2.GL_FLOAT, GL2.GL_LINEAR, 1, 4);
    
    tex_collision_dist.setParam_WRAP_S_T(GL2.GL_CLAMP_TO_EDGE);
    tex_obstacles_dist.setParam_WRAP_S_T(GL2.GL_CLAMP_TO_EDGE);
  }
  
  
  public void resizeParticlesCount(int num_particles_max){
    int size = (int) Math.ceil(Math.sqrt(num_particles_max));
    resizeParticlesCount(size, size);
  }
  
  
  public void resizeParticlesCount(int w, int h){
    boolean resized = tex_particle.resize(context, GL2.GL_RGBA32F, w, h, GL2.GL_RGBA, GL2.GL_FLOAT, GL2.GL_NEAREST, 4, 4);
    if(resized){
      reset();
    }
  }
  
  
  
  
  
  
  
  
  static public abstract class SpawnForm{
    
    public float[] pos = new float[2];
    public float[] dim = new float[2];
    public float[] vel = new float[2];
    
    public void pos(float x, float y){
      pos[0] = x;
      pos[1] = y;
    }
    public void dim(float x, float y){
      dim[0] = x;
      dim[1] = y;
    }
    public void vel(float x, float y){
      vel[0] = x;
      vel[1] = y;
    }
  }

  static public class SpawnRadial extends SpawnForm{
    public int num;
    public void num(int num){
      this.num = num;
    }
  }
  
  static public class SpawnRect extends SpawnForm{
    public int[] num = new int[2];
    public void num(int x, int y){
      num[0] = x;
      num[1] = y;
    }
  }

  
  public void spawn(int w_viewport, int h_viewport, SpawnRadial type){

    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    int spawn_max = w_particle * h_particle;

    if(spawn_idx >= spawn_max){
      spawn_idx = 0;
    }

    int lo = spawn_idx; 
    int hi = Math.min(lo + type.num, spawn_max); 
    float off = (float)(Math.random() * Math.PI * 2); // TODO
    
    int fsq_w = w_particle;
    int fsq_h = (hi + w_particle) / w_particle;
    
    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_spawn_radial.begin();
    shader_spawn_radial.uniform1i     ("spawn.num"     , type.num);
    shader_spawn_radial.uniform2f     ("spawn.pos"     , type.pos[0], type.pos[1]);
    shader_spawn_radial.uniform2f     ("spawn.dim"     , type.dim[0], type.dim[1]);
    shader_spawn_radial.uniform2f     ("spawn.vel"     , type.vel[0], type.vel[1]);
    shader_spawn_radial.uniform1f     ("spawn.off"     , off);
    shader_spawn_radial.uniform2i     ("lo_hi"          , lo, hi);
    shader_spawn_radial.uniform2f     ("wh_viewport_rcp", 1f/w_viewport, 1f/h_viewport);
    shader_spawn_radial.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_spawn_radial.uniformTexture("tex_position"   , tex_particle.src);
    shader_spawn_radial.drawFullScreenQuad(0, 0, fsq_w, fsq_h);
    shader_spawn_radial.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.spawnRadial");
    tex_particle.swap();
    
    spawn_idx = hi;
    spawn_num += hi - lo;
    spawn_num = Math.min(spawn_num, spawn_max); 
  }
  
  
  
  public void spawn(int w_viewport, int h_viewport, SpawnRect type){
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    int spawn_max = w_particle * h_particle;

    if(spawn_idx >= spawn_max){
      spawn_idx = 0;
    }
    
    int lo = spawn_idx; 
    int hi = Math.min(lo + type.num[0] * type.num[1], spawn_max); 
    
    int fsq_w = w_particle;
    int fsq_h = (hi + w_particle) / w_particle;

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_spawn_rect.begin();
    shader_spawn_rect.uniform2i     ("spawn.num"       , type.num[0], type.num[1]);
    shader_spawn_rect.uniform2f     ("spawn.pos"       , type.pos[0], type.pos[1]);
    shader_spawn_rect.uniform2f     ("spawn.dim"       , type.dim[0], type.dim[1]);
    shader_spawn_rect.uniform2f     ("spawn.vel"       , type.vel[0], type.vel[1]);
    shader_spawn_rect.uniform2i     ("lo_hi"          , lo, hi);
    shader_spawn_rect.uniform2f     ("wh_viewport_rcp", 1f/w_viewport, 1f/h_viewport);
    shader_spawn_rect.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_spawn_rect.uniformTexture("tex_position"   , tex_particle.src);
    shader_spawn_rect.drawFullScreenQuad(0, 0, fsq_w, fsq_h);
    shader_spawn_rect.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.spawnRect");
    tex_particle.swap();
    
    spawn_idx = hi;
    spawn_num += hi - lo;
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
    shader_display_particles.begin();
    shader_display_particles.uniform1f     ("shader_collision_mult", param.shader_collision_mult);
    shader_display_particles.uniform1f     ("point_size"   , param.size_display);
    shader_display_particles.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display_particles.uniform4fv    ("col_A"        , 1, param.col_A);
    shader_display_particles.uniform4fv    ("col_B"        , 1, param.col_B);
    shader_display_particles.uniformTexture("tex_collision", tex_collision_dist);
    shader_display_particles.uniformTexture("tex_position" , tex_particle.src);
    shader_display_particles.uniformTexture("tex_sprite"   , param.tex_sprite);
    shader_display_particles.drawFullScreenPoints(0, 0, w, h, spawn_num, false);
    shader_display_particles.end();
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
    shader_display_trails.begin();
    shader_display_trails.uniform1f     ("shader_collision_mult", param.shader_collision_mult);
    shader_display_trails.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display_trails.uniform4fv    ("col_A"        , 1, param.col_A);
    if(tex_collision_dist.HANDLE != null){
      shader_display_trails.uniform1f     ("shader_collision_mult", param.shader_collision_mult);
      shader_display_trails.uniformTexture("tex_collision", tex_collision_dist);
    } else {
      shader_display_trails.uniform1f     ("shader_collision_mult", 0);
    }
    shader_display_trails.uniformTexture("tex_position" , tex_particle.src);
    shader_display_trails.drawFullScreenLines(0, 0, w, h, spawn_num, lwidth, lsmooth);
    shader_display_trails.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.display");
  }
  

  

  public void createCollisionFlowField(){
    
    int w = tex_collision_dist.w;
    int h = tex_collision_dist.h;
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;

    int collision_radius = getCollisionRadius();

    context.begin();
    
    context.beginDraw(tex_collision_dist);
    context.gl.glColorMask(true, false, false, false);
    context.gl.glClearColor(0,0,0,0);
    context.gl.glClear(GL.GL_COLOR_BUFFER_BIT);
    context.gl.glEnable(GL.GL_BLEND);
    context.gl.glBlendEquation(GL.GL_FUNC_ADD);
    context.gl.glBlendFunc(GL.GL_SRC_COLOR, GL.GL_ONE);
    shader_collision_dist.begin();
    shader_collision_dist.uniform1f     ("point_size"   , collision_radius);
    shader_collision_dist.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_collision_dist.uniformTexture("tex_position" , tex_particle.src);
    shader_collision_dist.drawFullScreenPoints(0, 0, w, h, spawn_num, false);
    shader_collision_dist.end();
    context.endDraw();
    
    ff_col.create(tex_collision_dist);
    
    context.end("DwFlowFieldParticles.createCollisionFlowField");
  }
  
  
  
  public void createObstacleFlowField(PGraphicsOpenGL pg_scene, int[] FG, boolean FG_invert){
    
    Texture tex_scene = pg_scene.getTexture(); if(!tex_scene.available())  return;

    float[] FG_mask = {FG[0]/255f, FG[1]/255f, FG[2]/255f, FG[3]/255f};
    float FG_offset = getCollisionRadius() / 4;
    
    FG_offset -= ff_sum.param.blur_radius - ff_obs.param.blur_radius;
    FG_offset = Math.max(FG_offset, 0);
    
    context.begin();

    // 1) create FG mask
    context.beginDraw(tex_obstacles_FG);
    shader_obstacles_FG.begin();
    shader_obstacles_FG.uniform4fv    ("FG_mask"  , 1, FG_mask);
    shader_obstacles_FG.uniform1i     ("FG_invert", FG_invert ? 1 : 0);
    shader_obstacles_FG.uniformTexture("tex_scene", tex_scene.glName);
    shader_obstacles_FG.drawFullScreenQuad();
    shader_obstacles_FG.end();
    context.endDraw("DwFlowFieldObstacles.create() create FG mask");
    
    // 2) apply distance transform
    dt_obstacles.param.FG_mask = new float[]{1,1,0,1}; // only obstacle EDGES
    dt_obstacles.create(tex_obstacles_FG);
    
    // 3) create distance field
    context.beginDraw(tex_obstacles_dist);
    shader_obstacles_dist.begin();
    shader_obstacles_dist.uniform2f     ("mad", 1, FG_offset);
    shader_obstacles_dist.uniformTexture("tex_FG"  , tex_obstacles_FG);
    shader_obstacles_dist.uniformTexture("tex_dtnn", dt_obstacles.tex_dtnn.src);
    shader_obstacles_dist.drawFullScreenQuad();
    shader_obstacles_dist.end();
    context.endDraw("DwFlowFieldObstacles.create() distance field");
    
    // 4) create obstacle flowfield
    ff_obs.create(tex_obstacles_dist);
    
    context.end("DwFlowFieldParticles.createObstacleFlowField");
  }
  
  
  
  
  
  
  
  
  


  public void updateAcceleration(DwGLTexture tex_velocity, float acc_mult){
    
    int w_velocity = tex_velocity.w;
    int h_velocity = tex_velocity.h;
    
    int w_particle = tex_particle.dst.w;
    int h_particle = tex_particle.dst.h;

    int viewport_w = w_particle;
    int viewport_h = (spawn_num + w_particle) / w_particle;
    
    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update_acc.begin();
    shader_update_acc.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update_acc.uniform2f     ("acc_minmax"     , 0.01f, 10);
    shader_update_acc.uniform1f     ("acc_mult"       , acc_mult);
    shader_update_acc.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update_acc.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update_acc.uniformTexture("tex_position"   , tex_particle.src);
    shader_update_acc.uniformTexture("tex_velocity"   , tex_velocity);
    shader_update_acc.drawFullScreenQuad(0, 0, viewport_w, viewport_h);
    shader_update_acc.end();
    context.endDraw();
    tex_particle.swap();
    context.end("DwFlowFieldParticles.updateCollision");
  }
  
 
  public void updateVelocity(){

    int w_velocity = tex_collision_dist.w;
    int h_velocity = tex_collision_dist.h;
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    
    int viewport_w = w_particle;
    int viewport_h = (spawn_num + w_particle) / w_particle;

    float vel_mult = param.velocity_damping;

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update_vel.begin();
    shader_update_vel.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update_vel.uniform2f     ("vel_minmax"     , 0.0f, 10);
    shader_update_vel.uniform1f     ("vel_mult"       , vel_mult);
    shader_update_vel.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update_vel.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update_vel.uniformTexture("tex_position"   , tex_particle.src);
    shader_update_vel.drawFullScreenQuad(0, 0, viewport_w, viewport_h);
    shader_update_vel.end();
    context.endDraw();
    tex_particle.swap();
    context.end("DwFlowFieldParticles.update");
  }
  
  

  

  public void update(){

    float mult_acc = 1.0f                        / param.collision_steps;
    float mult_col = 1.0f * param.collision_mult / param.collision_steps;
    float mult_obs = 2.0f * param.collision_mult / param.collision_steps;
    
    float[] mad = { mult_acc,0,   mult_col,0,   mult_obs,0};
    DwGLTexture[] tex = {
        ff_acc.tex_vel,
        ff_col.tex_vel,
        ff_obs.tex_vel,
      };
      
    updateVelocity();
 
    for(int i = 0; i < param.collision_steps; i++){
      
      createCollisionFlowField();

      merge.apply(ff_sum.tex_vel, tex, mad);
      ff_sum.blur();
      
      updateAcceleration(ff_sum.tex_vel, 1f);
    }
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
