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


package com.thomasdiewald.pixelflow.java.flowfieldparticles;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DistanceTransform;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge.TexMad;

import processing.opengl.PGraphicsOpenGL;

/**
 * 
 *
 * <h1> GPU FlowFieldParticle DevDemo.</h1>
 * <br>
 * Verlet Particle Simulation based on FlowFields.<br>
 * 
 * <h2> ___ FLOW FIELDS / SDF (SIGNED DISTANCE FIELDS) ___  </h2>
 * A FlowField is simply a velocity texture which is used for iteratively updating 
 * particle-positions.
 * E.g. Applying a Sobel-filter (x/y gradients) onto a SDF results in a flowfield.
 * 
 * <h2> ___ ACCELERATION ___ </h2>
 * This is probably the most common use for velocity textures in particle
 * simulations, ... using the velocity for the update step.
 * 
 * <h2> ___ COLLISION ___ </h2>
 * Particle-Collisions can be solved in a very elegant way, by rendering particles
 * into a R32F texture using a distance-to-center shading function and additive
 * blending. In a next step a flowfield from this texture is used for updating 
 * particles position in the next update step.
 * This is key to simulate millions of particles on the GPU.
 * 
 * <h2> ___ COHESION ___ </h2>
 * Same as for the collisions I was a bit surprised how well it works for
 * simulation particle-to-particle attraction.
 * 
 * <h2> ___ OBSTACLES ___ </h2>
 * Handling obstacles is more or less the same as particle-to-particle interaction
 * is handled.
 * A distance transform step is applied in the scene obstacles (edges) to get 
 * a local distance field from which a flow field can be generated.
 * The resulting velocity texture is used for collision detection.
 *
 * <br>
 * @author Thomas Diewald, (C) 2017<br>
 * 
 * 
 */
public class DwFlowFieldParticles{
  
  public static class Param {
    
    // physics timestep, ... milliseconds per frame ... 1 / frameRate
    public float timestep = 1 / 120f;
    
    // particle size rendering/collision
    public int   size_display   = 10;
    public int   size_collision = 10;
    public int   size_cohesion  =  5;
    
    // buffer size scaling ... 1 << scale ... pow(2, scale)
    public int wh_scale_col = 0; // 1 << wh_scale_col ... 1 << 0 ...  1
    public int wh_scale_coh = 4; // 1 << wh_scale_coh ... 1 << 4 ... 16
    public int wh_scale_obs = 0; // 1 << wh_scale_obs ... 1 << 0 ...  2
    public int wh_scale_sum = 0; // auto
    
    
    public float[] acc_minmax = {0.01f, 24f};
    public float[] vel_minmax = {0.00f, 24f};
    
    // velocity damping
    public float   velocity_damping  = 0.9999f;
    
    // update
    public int     steps = 1;
    
    public float   mul_acc = 1;
    public float   mul_col = 1;
    public float   mul_coh = 1;
    public float   mul_obs = 1;
    
    // display stuff
    public float   shader_collision_mult = 0.1f;
    public int     shader_type = 0; // 0 ... point based, 1 ...velocity based
    
    public float[] col_A = {1.0f, 1.0f, 1.0f, 10.0f};
    public float[] col_B = {0.0f, 0.0f, 0.0f,  0.0f};

    public int     blend_mode   = 0; // BLEND=0; ADD=1
    
    // particle trails
    public float   display_line_width = 0.5f;
    public boolean display_line_smooth = true;
    

    // point sprite texture
    public DwGLTexture tex_sprite = new DwGLTexture();
  }
  
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
  public DwGLSLProgram shader_create_sprite;
  
  public DwGLSLProgram shader_spawn_radial;
  public DwGLSLProgram shader_spawn_rect;
  public DwGLSLProgram shader_update_vel;
  public DwGLSLProgram shader_update_acc;

  public DwGLSLProgram shader_display_particles;
  public DwGLSLProgram shader_display_trails;
  
  public DwGLSLProgram shader_particles_dist;
  public DwGLSLProgram shader_obstacles_dist;

  public DwGLTexture.TexturePingPong tex_particle = new DwGLTexture.TexturePingPong();
  
  public DwGLTexture tex_col_dist = new DwGLTexture();
  public DwGLTexture tex_obs_dist = new DwGLTexture();
  public DwGLTexture tex_coh_dist = new DwGLTexture();
  public DwGLTexture tex_tmp_dist = new DwGLTexture();
  
  public DwFlowField ff_col;
  public DwFlowField ff_obs;
  public DwFlowField ff_coh;
  public DwFlowField ff_sum;

  public DwGLSLProgram shader_obstacles_FG;
  public DwGLTexture tex_obs_FG = new DwGLTexture();
  public DwGLTexture tex_obs  = new DwGLTexture();
  
  public DistanceTransform distancetransform;
  
  public int spawn_idx = 0;
  protected int spawn_num = 0;
  
  public DwFlowFieldParticles(DwPixelFlow context){
    this(context, 0);
  }
  
  public DwFlowFieldParticles(DwPixelFlow context, int num_particles){    
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    String filename;
    
    String data_path = DwPixelFlow.SHADER_DIR+"FlowFieldParticles/";
    
    shader_create_sprite = context.createShader(data_path + "create_sprite_texture.frag");

    shader_spawn_radial = context.createShader((Object) (this+"radial"), data_path + "particles_spawn.frag");
    shader_spawn_radial.frag.setDefine("SPAWN_RADIAL", 1);
    shader_spawn_rect   = context.createShader((Object) (this+"rect"  ), data_path + "particles_spawn.frag");
    shader_spawn_rect  .frag.setDefine("SPAWN_RECT"  , 1);
    
    shader_update_vel = context.createShader((Object) (this+"update_vel"), data_path + "particles_update.frag");
    shader_update_vel.frag.setDefine("UPDATE_VEL", 1);
    shader_update_acc = context.createShader((Object) (this+"update_acc"), data_path + "particles_update.frag");
    shader_update_acc.frag.setDefine("UPDATE_ACC", 1);

    filename = data_path + "particles_display_points.glsl"; // "particles_display_quads.glsl"
    shader_display_particles = context.createShader((Object) (this+"SPRITE"), filename, filename);
    shader_display_particles.vert.setDefine("SHADER_VERT", 1);
    shader_display_particles.frag.setDefine("SHADER_FRAG", 1);

    filename = data_path + "particles_display_lines.glsl";
    shader_display_trails = context.createShader((Object) (this+"LINES"), filename, filename);
    shader_display_trails.vert.setDefine("SHADER_VERT", 1);
    shader_display_trails.frag.setDefine("SHADER_FRAG", 1);

    shader_particles_dist = context.createShader(data_path+"particles_dist.glsl", data_path+"particles_dist.glsl");
    shader_particles_dist.vert.setDefine("SHADER_VERT", 1);
    shader_particles_dist.frag.setDefine("SHADER_FRAG", 1);
    
    shader_obstacles_dist = context.createShader(data_path+"obstacles_dist.frag");
    shader_obstacles_FG   = context.createShader(data_path+"obstacles_FG.frag");
    

    ff_col = new DwFlowField(context);
    ff_col.param.blur_iterations = 1;
    ff_col.param.blur_radius     = 2;
    
    ff_obs = new DwFlowField(context);
    ff_obs.param.blur_iterations = 1;
    ff_obs.param.blur_radius     = 1;
    
    ff_coh = new DwFlowField(context);
    ff_coh.param.blur_iterations = 1;
    ff_coh.param.blur_radius     = 1;

    ff_sum = new DwFlowField(context);
    ff_sum.param.blur_iterations = 1;
    ff_sum.param.blur_radius     = 3;
    
    distancetransform = new DistanceTransform(context);

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
  
  

  
  public int getCount(){
    return spawn_num;
  }


  public float getTimestep(){
    return Math.min(1, 120 * param.timestep) * 0.5f;
  }

  
  public int getCollisionSize(){
    float scale = 1 << Math.abs(param.wh_scale_col);
    if(param.wh_scale_col < 0) scale = 1f / scale;
    
    int radius = (int) Math.ceil(param.size_collision * 2f / scale);
    radius += 1 - (radius & 1); // make radius an odd number
    return radius;
  }
  
  public int getCohesionSize(){
    float scale = 1 << Math.abs(param.wh_scale_coh);
    if(param.wh_scale_coh < 0) scale = 1f / scale;

    int radius = (int) Math.ceil(param.size_cohesion * 16 / scale);
    radius += 1 - (radius & 1); // make radius an odd number
    return radius;
  }
  
  public int getObstacleSize(){
    float scale = 1 << Math.abs(param.wh_scale_obs);
    if(param.wh_scale_obs < 0) scale = 1f / scale;
    
    int radius = (int) Math.ceil(param.size_collision / (2f * scale));
    radius += 1 - (radius & 1); // make radius an odd number
    return radius;
  }
  
  
  public void reset(){
    tex_particle.clear(0);
    
    tex_obs_dist.clear(0);
    tex_col_dist.clear(0);
    tex_coh_dist.clear(0);
    tex_tmp_dist.clear(0);
    
    ff_obs.reset();  
    ff_col.reset();   
    ff_coh.reset(); 
    ff_sum.reset(); 
    
    spawn_idx = 0;

    SpawnRect sr = new SpawnRect();
    sr.num(tex_particle.src.w, tex_particle.src.h);
    sr.pos(-2,-2);
    sr.dim( 1, 1);
    
    spawn(1, 1, sr);
    
    spawn_idx = 0;
    spawn_num = 0;
  }
  
  
  public void dispose(){
    release();
  }

  public void release(){
    distancetransform.release();
    tex_obs_FG.release();
    tex_obs.release();
    
    tex_obs_dist.release();
    tex_col_dist.release();
    tex_coh_dist.release();
    tex_tmp_dist.release();
    
    ff_col.release();
    ff_obs.release();
    ff_coh.release();
    ff_sum.release();

    param.tex_sprite.release();
    tex_particle.release();
  }
  
  

  
  public boolean resizeWorld(int w, int h){
    
    param.wh_scale_sum = Math.min(Math.min(param.wh_scale_col, param.wh_scale_coh), param.wh_scale_obs);
    
    float scale_obs = 1 << Math.abs(param.wh_scale_obs);
    float scale_col = 1 << Math.abs(param.wh_scale_col);
    float scale_coh = 1 << Math.abs(param.wh_scale_coh);
    float scale_sum = 1 << Math.abs(param.wh_scale_sum);
    
    if(param.wh_scale_obs < 0) scale_obs = 1f / scale_obs;
    if(param.wh_scale_col < 0) scale_col = 1f / scale_col;
    if(param.wh_scale_coh < 0) scale_coh = 1f / scale_coh;
    if(param.wh_scale_sum < 0) scale_sum = 1f / scale_sum;

//    int w_sum = w,                            h_sum = h;
    int w_sum = (int) Math.ceil(w/scale_sum), h_sum = (int) Math.ceil(h/scale_sum);
    int w_obs = (int) Math.ceil(w/scale_obs), h_obs = (int) Math.ceil(h/scale_obs);
    int w_col = (int) Math.ceil(w/scale_col), h_col = (int) Math.ceil(h/scale_col);
    int w_coh = (int) Math.ceil(w/scale_coh), h_coh = (int) Math.ceil(h/scale_coh);
    
    boolean resized = false;
    
    resized |= ff_obs.resize(w_obs, h_obs);
    resized |= ff_col.resize(w_col, h_col);
    resized |= ff_coh.resize(w_coh, h_coh);
    resized |= ff_sum.resize(w_sum, h_sum);
    
    resized |= tex_obs_FG.resize(context, GL2.GL_RGBA, w_obs, h_obs, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, 4, 1);
    resized |= tex_obs   .resize(context, GL2.GL_RGBA, w_obs, h_obs, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, 4, 1);
    
    boolean HIGHP_FLOAT = true;
    int format  = GL2.GL_RED;
    int filter  = GL2.GL_LINEAR;
    int wrap    = GL2.GL_CLAMP_TO_EDGE;
    int iformat = HIGHP_FLOAT ? GL2.GL_R32F : GL2.GL_R16F;
    int type    = HIGHP_FLOAT ? GL2.GL_FLOAT : GL2.GL_HALF_FLOAT;
    int bpc     = HIGHP_FLOAT ? 4 : 2;
    resized |= tex_obs_dist.resize(context, iformat, w_obs, h_obs, format, type, filter, wrap, 1, bpc);
    resized |= tex_col_dist.resize(context, iformat, w_col, h_col, format, type, filter, wrap, 1, bpc);
    resized |= tex_coh_dist.resize(context, iformat, w_coh, h_coh, format, type, filter, wrap, 1, bpc);
    resized |= tex_tmp_dist.resize(context, iformat, w_col, h_col, format, type, filter, wrap, 1, bpc);
    
    if(resized){
      tex_obs_dist.clear(0);
      tex_col_dist.clear(0);
      tex_coh_dist.clear(0);
      tex_tmp_dist.clear(0);
    }

    return resized;
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
    
    int row_lo = 0; // need to write from 0 to copy those, because of tex_particle.swap();
    // int row_lo = (lo             ) / w_particle;
    int row_hi = (hi + w_particle) / w_particle;
    int scx = 0;
    int scy = row_lo;
    int scw = w_particle;
    int sch = row_hi - row_lo;
    
    float ts = getTimestep();
    
    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_spawn_radial.begin();
    shader_spawn_radial.uniform1i     ("spawn.num"      , type.num);
    shader_spawn_radial.uniform2f     ("spawn.pos"      , type.pos[0], type.pos[1]);
    shader_spawn_radial.uniform2f     ("spawn.dim"      , type.dim[0], type.dim[1]);
    shader_spawn_radial.uniform2f     ("spawn.vel"      , type.vel[0]*ts, type.vel[1]*ts);
    shader_spawn_radial.uniform1f     ("spawn.off"      , off);
    shader_spawn_radial.uniform2i     ("lo_hi"          , lo, hi);
    shader_spawn_radial.uniform2f     ("wh_viewport_rcp", 1f/w_viewport, 1f/h_viewport);
    shader_spawn_radial.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_spawn_radial.uniformTexture("tex_position"   , tex_particle.src);
    shader_spawn_radial.scissors(scx, scy, scw, sch);
    shader_spawn_radial.drawFullScreenQuad();
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
    
    int row_lo = 0; // need to write from 0 to copy those, because of tex_particle.swap();
    // int row_lo = (lo             ) / w_particle;
    int row_hi = (hi + w_particle) / w_particle;
    int scx = 0;
    int scy = row_lo;
    int scw = w_particle;
    int sch = row_hi - row_lo;

    float ts = getTimestep();
    
    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_spawn_rect.begin();
    shader_spawn_rect.uniform2i     ("spawn.num"      , type.num[0], type.num[1]);
    shader_spawn_rect.uniform2f     ("spawn.pos"      , type.pos[0], type.pos[1]);
    shader_spawn_rect.uniform2f     ("spawn.dim"      , type.dim[0], type.dim[1]);
    shader_spawn_rect.uniform2f     ("spawn.vel"      , type.vel[0]*ts, type.vel[1]*ts);
    shader_spawn_rect.uniform2i     ("lo_hi"          , lo, hi);
    shader_spawn_rect.uniform2f     ("wh_viewport_rcp", 1f/w_viewport, 1f/h_viewport);
    shader_spawn_rect.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_spawn_rect.uniformTexture("tex_position"   , tex_particle.src);
    shader_spawn_rect.scissors(scx, scy, scw, sch);
    shader_spawn_rect.drawFullScreenQuad();
    shader_spawn_rect.end();
    context.endDraw();
    context.end("DwFlowFieldParticles.spawnRect");
    tex_particle.swap();
    
    spawn_idx = hi;
    spawn_num += hi - lo;
    spawn_num = Math.min(spawn_num, spawn_max);
  }
  

  
  
  public void displayParticles(PGraphicsOpenGL canvas){
    if(param.size_display <= 0) return;
    context.begin();
    context.beginDraw(canvas);
    displayParticles(canvas.width, canvas.height);
    context.endDraw();
    context.end("DwFlowFieldParticles.displayParticles PGraphicsOpenGL");
  }
  
  public void displayParticles(DwGLTexture canvas){
    if(param.size_display <= 0) return;
    context.begin();
    context.beginDraw(canvas);
    displayParticles(canvas.w, canvas.h);
    context.endDraw();
    context.end("DwFlowFieldParticles.displayParticles DwGLTexture");
  }
  
  public void displayTrail(PGraphicsOpenGL canvas){
    if(param.display_line_width <= 0) return;
    context.begin();
    context.beginDraw(canvas);
    displayTrail(canvas.width, canvas.height);
    context.endDraw();
    context.end("DwFlowFieldParticles.displayTrail PGraphicsOpenGL");
  }
  
  public void displayTrail(DwGLTexture canvas){
    if(param.display_line_width <= 0) return;
    context.begin();
    context.beginDraw(canvas);
    displayTrail(canvas.w, canvas.h);
    context.endDraw();
    context.end("DwFlowFieldParticles.displayTrail DwGLTexture");
  }
  
  
  protected void displayParticles(int w_viewport, int h_viewport){
    if(param.size_display <= 0) return;
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    int point_size = param.size_display;
    blendMode();
    shader_display_particles.frag.setDefine("SHADING_TYPE", param.shader_type);
    shader_display_particles.begin();
    shader_display_particles.uniform1f     ("shader_collision_mult", param.shader_collision_mult);
    shader_display_particles.uniform1f     ("point_size"   , point_size);
    shader_display_particles.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display_particles.uniform2f     ("wh_viewport"  , w_viewport, h_viewport);
    shader_display_particles.uniform4fv    ("col_A"        , 1, param.col_A);
    shader_display_particles.uniform4fv    ("col_B"        , 1, param.col_B);
    shader_display_particles.uniformTexture("tex_collision", tex_col_dist);
    shader_display_particles.uniformTexture("tex_position" , tex_particle.src);
    shader_display_particles.uniformTexture("tex_sprite"   , param.tex_sprite);
    shader_display_particles.drawFullScreenPoints(spawn_num);
    shader_display_particles.end();
  }
  
  
  protected void displayTrail(int w_viewport, int h_viewport){
    if(param.display_line_width <= 0) return;
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    blendMode();
    shader_display_trails.begin();
    shader_display_trails.uniform1f     ("shader_collision_mult", param.shader_collision_mult);
    shader_display_trails.uniform2i     ("wh_position"  , w_particle, h_particle);
    shader_display_trails.uniform2f     ("wh_viewport"  , w_viewport, h_viewport);
    shader_display_trails.uniform4fv    ("col_A"        , 1, param.col_A);
    shader_display_trails.uniformTexture("tex_collision", tex_col_dist);
    shader_display_trails.uniformTexture("tex_position" , tex_particle.src);
    shader_display_trails.drawFullScreenLines(spawn_num, param.display_line_width, param.display_line_smooth);
    shader_display_trails.end();
  }
  
  
  

  public void createCollisionFlowField(){
    if(param.mul_col <= 0 || param.size_collision <= 0) return;
    
    int w_viewport = tex_col_dist.w;
    int h_viewport = tex_col_dist.h;
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    int point_size = getCollisionSize();
    
    context.begin();
    {
      // 1) create distance field
      context.beginDraw(tex_col_dist);
      context.gl.glColorMask(true, false, false, false);
      context.gl.glClearColor(0,0,0,0);
      context.gl.glClear(GL.GL_COLOR_BUFFER_BIT);
      context.gl.glEnable(GL.GL_BLEND);
      context.gl.glBlendEquation(GL.GL_FUNC_ADD);
      context.gl.glBlendFunc(GL.GL_SRC_COLOR, GL.GL_ONE); // ADD
      shader_particles_dist.begin();
      shader_particles_dist.uniform1f     ("point_size"  , point_size);
      shader_particles_dist.uniform2i     ("wh_position" , w_particle, h_particle);
      shader_particles_dist.uniform2f     ("wh_viewport" , w_viewport, h_viewport);
      shader_particles_dist.uniformTexture("tex_position", tex_particle.src);
      shader_particles_dist.drawFullScreenPoints(spawn_num);
      shader_particles_dist.end();
      context.endDraw("DwFlowFieldParticles.createCollisionFlowField");
      
      // .) smooth distance field
      DwFilter.get(context).gaussblur.apply(tex_col_dist, tex_col_dist, tex_tmp_dist, 1);
      
      // 2) create flow field
      ff_col.create(tex_col_dist);
      
      // .) smooth distance field
      //  DwFilter.get(context).gaussblur.apply(tex_col_dist, tex_col_dist, tex_tmp_dist, 1);
    }
    context.end();
  }
  
  public void createCohesionFlowField(){
    if(param.mul_coh <= 0 || param.size_cohesion <= 0) return;
    
    int w_viewport = tex_coh_dist.w;
    int h_viewport = tex_coh_dist.h;
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    int point_size = getCohesionSize();

    context.begin();
    {
      // 1) create distance field
      context.beginDraw(tex_coh_dist);
      context.gl.glColorMask(true, false, false, false);
      context.gl.glClearColor(0,0,0,0);
      context.gl.glClear(GL.GL_COLOR_BUFFER_BIT);
      context.gl.glEnable(GL.GL_BLEND);
      context.gl.glBlendEquation(GL.GL_FUNC_ADD);
      context.gl.glBlendFunc(GL.GL_SRC_COLOR, GL.GL_ONE); // ADD
      shader_particles_dist.begin();
      shader_particles_dist.uniform1f     ("point_size"  , point_size);
      shader_particles_dist.uniform2i     ("wh_position" , w_particle, h_particle);
      shader_particles_dist.uniform2f     ("wh_viewport" , w_viewport, h_viewport);
      shader_particles_dist.uniformTexture("tex_position", tex_particle.src);
      shader_particles_dist.drawFullScreenPoints(spawn_num);
      shader_particles_dist.end();
      context.endDraw("DwFlowFieldParticles.createCohesionFlowField");
       
      // 2) create flow field
      ff_coh.create(tex_coh_dist);
    }
    context.end();
  }
  
  
  
  
  
  
  
  
  
 
  

  public void createObstacleFlowField(PGraphicsOpenGL pg_scene, int[] FG, boolean FG_invert){
    if(param.mul_obs <= 0.0) return;
    
    float[] FG_mask = {FG[0]/255f, FG[1]/255f, FG[2]/255f, FG[3]/255f};
    
    float FG_offset = getObstacleSize();
    FG_offset -= (ff_sum.param.blur_radius - ff_obs.param.blur_radius) << param.wh_scale_obs;
    FG_offset = Math.max(FG_offset, 0);
    
    DwFilter.get(context).copy.apply(pg_scene, tex_obs);
    
    context.begin();
    {
      // 1) create FG mask
      context.beginDraw(tex_obs_FG);
      shader_obstacles_FG.begin();
      shader_obstacles_FG.uniform4fv    ("FG_mask"  , 1, FG_mask);
      shader_obstacles_FG.uniform1i     ("FG_invert", FG_invert ? 1 : 0);
      shader_obstacles_FG.uniformTexture("tex_scene", tex_obs);
      shader_obstacles_FG.drawFullScreenQuad();
      shader_obstacles_FG.end();
      context.endDraw("DwFlowFieldObstacles.create() create FG mask");
      
      // 2) apply distance transform
      distancetransform.param.FG_mask = new float[]{1,1,0,1}; // only obstacle EDGES
      distancetransform.param.FG_invert = false;
      distancetransform.create(tex_obs_FG);
      
      // 3) create distance field
      context.beginDraw(tex_obs_dist);
      shader_obstacles_dist.begin();
      shader_obstacles_dist.uniform2f     ("mad"     , 1, FG_offset);
      shader_obstacles_dist.uniformTexture("tex_FG"  , tex_obs_FG);
      shader_obstacles_dist.uniformTexture("tex_dtnn", distancetransform.tex_dtnn.src);
      shader_obstacles_dist.drawFullScreenQuad();
      shader_obstacles_dist.end();
      context.endDraw("DwFlowFieldObstacles.create() distance field");
      
      // 4) create flow field
      ff_obs.create(tex_obs_dist);
    }
    context.end("DwFlowFieldParticles.createObstacleFlowField");
  }
  
  

  
  public void updateAcceleration(DwGLTexture tex_velocity, float acc_mult){
    
    int w_velocity = tex_velocity.w;
    int h_velocity = tex_velocity.h;
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;

    int scw = w_particle;
    int sch = (spawn_num + w_particle) / w_particle;
    
    float timestep = getTimestep();
    float acc_min = param.acc_minmax[0] * timestep;
    float acc_max = param.acc_minmax[1] * timestep;
    
    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update_acc.begin();
    shader_update_acc.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update_acc.uniform2f     ("acc_minmax"     , acc_min, acc_max);
    shader_update_acc.uniform1f     ("acc_mult"       , acc_mult);
    shader_update_acc.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update_acc.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update_acc.uniformTexture("tex_position"   , tex_particle.src);
    shader_update_acc.uniformTexture("tex_velocity"   , tex_velocity);
    shader_update_acc.uniformTexture("tex_collision"  , tex_col_dist);
    shader_update_acc.scissors(0, 0, scw, sch);
    shader_update_acc.drawFullScreenQuad();
    shader_update_acc.end();
    context.endDraw("DwFlowFieldParticles.updateAcceleration");
    context.end();
    tex_particle.swap();
  }
  
  
 
  public void updateVelocity(){

    int w_velocity = ff_sum.tex_vel.w;
    int h_velocity = ff_sum.tex_vel.h;
    
    int w_particle = tex_particle.src.w;
    int h_particle = tex_particle.src.h;
    
    int scw = w_particle;
    int sch = (spawn_num + w_particle) / w_particle;

    float timestep = getTimestep();
    float vel_mult = param.velocity_damping;
    float vel_min = param.vel_minmax[0] * timestep;
    float vel_max = param.vel_minmax[1] * timestep;
    
    context.begin();
    context.beginDraw(tex_particle.dst);
    shader_update_vel.begin();
    shader_update_vel.uniform1i     ("spawn_hi"       , spawn_num);
    shader_update_vel.uniform2f     ("vel_minmax"     , vel_min, vel_max);
    shader_update_vel.uniform1f     ("vel_mult"       , vel_mult);
    shader_update_vel.uniform2i     ("wh_position"    ,    w_particle,    h_particle);
    shader_update_vel.uniform2f     ("wh_velocity_rcp", 1f/w_velocity, 1f/h_velocity);
    shader_update_vel.uniformTexture("tex_position"   , tex_particle.src);
    shader_update_vel.uniformTexture("tex_collision"  , tex_col_dist);
    shader_update_vel.scissors(0, 0, scw, sch);
    shader_update_vel.drawFullScreenQuad();
    shader_update_vel.end("DwFlowFieldParticles.updateVelocity");
    context.endDraw();
    context.end();
    tex_particle.swap();
  }
  

  public final TexMad tm_acc = new TexMad();
  public final TexMad tm_col = new TexMad();
  public final TexMad tm_coh = new TexMad();
  public final TexMad tm_obs = new TexMad();
  

  public void update(DwFlowField ff_acc){
    update(ff_acc.tex_vel);
  }
  
  public void update(DwGLTexture tex_acc){
    
    float timestep = getTimestep() / param.steps;
    
    updateVelocity();

    for(int i = 0; i < param.steps; i++){
      
      createCollisionFlowField();
      createCohesionFlowField();
      
      tm_acc.set(       tex_acc,  1.000f * param.mul_acc * timestep, 0);
      tm_col.set(ff_col.tex_vel,  1.000f * param.mul_col * timestep, 0);
      tm_coh.set(ff_coh.tex_vel, -0.025f * param.mul_coh * timestep, 0);
      tm_obs.set(ff_obs.tex_vel,  3.000f * param.mul_obs * timestep, 0);
      
      DwFilter.get(context).merge.apply(ff_sum.tex_vel, tm_acc, tm_col, tm_coh, tm_obs);
      ff_sum.blur();
      
      updateAcceleration(ff_sum.tex_vel, 1.0f);
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
