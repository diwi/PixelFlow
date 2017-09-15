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
  
//  protected String data_path = DwPixelFlow.SHADER_DIR+"flowfield/";
  protected String data_path = "D:/data/__Eclipse/workspace/WORKSPACE_FLUID/PixelFlow/src/com/thomasdiewald/pixelflow/glsl/flowfield/";
  
  public DwGLSLProgram shader_spawn_radial;
  public DwGLSLProgram shader_spawn_rect;
  public DwGLSLProgram shader_update_verletpos;
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
  
    shader_spawn_radial      = context.createShader((Object) (this+"radial"    ), data_path + "particles_spawn.frag");
    shader_spawn_rect        = context.createShader((Object) (this+"rect"      ), data_path + "particles_spawn.frag");
    
    shader_update_verletpos  = context.createShader((Object) (this+"update"    ), data_path + "particles_update.frag");
    shader_update_collision  = context.createShader((Object) (this+"collision" ), data_path + "particles_update.frag");
    
    shader_spawn_radial.frag.setDefine("SPAWN_RADIAL", 1);
    shader_spawn_rect  .frag.setDefine("SPAWN_RECT"  , 1);

    shader_update_verletpos.frag.setDefine("UPDATE_VEL", 1);
    shader_update_verletpos.frag.setDefine("UPDATE_ACC", 0);
    
    shader_update_collision.frag.setDefine("UPDATE_VEL", 0);
    shader_update_collision.frag.setDefine("UPDATE_ACC", 1);
    
    
    
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

    SpawnRect sr = new SpawnRect();
    sr.num(tex_particle.src.w, tex_particle.src.h);
    sr.pos(-2,-2);
    sr.dim( 1, 1);
    
    spawn(1, 1, sr);
    
    spawn_idx = 0;
    spawn_num = 0;
  }
  
  
  public void resize(int num_particles_max){
    int size = (int) Math.ceil(Math.sqrt(num_particles_max));
    resize(size, size);
  }
  
  public void resize(int w, int h){
    boolean resized = tex_particle.resize(context, GL2ES2.GL_RGBA32F, w, h, GL2ES2.GL_RGBA, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 4, 4);
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
    if(tex_collision.HANDLE != null){
      shader_display_lines.uniform1f     ("shader_collision_mult", param.shader_collision_mult);
      shader_display_lines.uniformTexture("tex_collision", tex_collision);
    } else {
      shader_display_lines.uniform1f     ("shader_collision_mult", 0);
    }
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
  

  
  public float[] getAccMinMax(){  
    int collision_radius = getCollisionRadius();
    float acc_min = 0.001f;
    float acc_max = collision_radius * 3.00f; 
    acc_max = Math.min(acc_max, 6);
    return new float[] {acc_min, acc_max};
  }

  
  
  public void updateAcceleration(DwGLTexture tex_velocity, float velocity_mult){
    
    int w_velocity = tex_velocity.w;
    int h_velocity = tex_velocity.h;
    
    int w_particle = tex_particle.dst.w;
    int h_particle = tex_particle.dst.h;

    int viewport_w = w_particle;
    int viewport_h = (spawn_num + w_particle) / w_particle;
    

    float[] acc_minmax = getAccMinMax();

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update_collision.begin();
    shader_update_collision.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update_collision.uniform2f     ("acc_minmax"     , acc_minmax[0], acc_minmax[1]);
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
    
//    int collision_radius = getCollisionRadius();
//    float acc_min = DwUtils.SQRT2 * 0.001f;
//    float acc_max = DwUtils.SQRT2 * collision_radius * 3.00f; acc_max = Math.min(acc_max, 6);
//    float vel_min = DwUtils.SQRT2 * 0.001f;
//    float vel_max = DwUtils.SQRT2 * collision_radius * 3.00f; vel_max = Math.min(vel_max, 6);
    
    float[] acc_minmax = getAccMinMax();
    
    float acc_mult = velocity_mult;
    float vel_mult = param.velocity_damping;

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update_verletpos.begin();
    shader_update_verletpos.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update_verletpos.uniform2f     ("acc_minmax"     , acc_minmax[0], acc_minmax[1]/acc_mult);
    shader_update_verletpos.uniform2f     ("vel_minmax"     , acc_minmax[0], acc_minmax[1]/vel_mult);
    shader_update_verletpos.uniform1f     ("acc_mult"       , acc_mult);
    shader_update_verletpos.uniform1f     ("vel_mult"       , vel_mult);
    shader_update_verletpos.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update_verletpos.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update_verletpos.uniformTexture("tex_position"   , tex_particle.src);
    shader_update_verletpos.uniformTexture("tex_velocity"   , tex_velocity);
    shader_update_verletpos.drawFullScreenQuad(0, 0, viewport_w, viewport_h);
    shader_update_verletpos.end();
    context.endDraw();
    tex_particle.swap();
    context.end("DwFlowFieldParticles.update");
  }
  
  public void update(int w_velocity, int h_velocity){
    
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    
    int viewport_w = w_particle;
    int viewport_h = (spawn_num + w_particle) / w_particle;

    float[] acc_minmax = getAccMinMax();
    float vel_mult = param.velocity_damping;

    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update_verletpos.begin();
    shader_update_verletpos.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update_verletpos.uniform2f     ("vel_minmax"     , acc_minmax[0], acc_minmax[1]/vel_mult);
    shader_update_verletpos.uniform1f     ("vel_mult"       , vel_mult);
    shader_update_verletpos.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update_verletpos.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update_verletpos.uniformTexture("tex_position"   , tex_particle.src);
    shader_update_verletpos.drawFullScreenQuad(0, 0, viewport_w, viewport_h);
    shader_update_verletpos.end();
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
