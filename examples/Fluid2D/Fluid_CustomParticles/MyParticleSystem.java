/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Fluid2D.Fluid_CustomParticles;

import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;

import processing.core.PConstants;
import processing.opengl.PGraphics2D;


public class MyParticleSystem{
  
  public DwGLSLProgram shader_particleSpawn;
  public DwGLSLProgram shader_particleUpdate;
  public DwGLSLProgram shader_particleRender;
  
  public DwGLTexture.TexturePingPong tex_particles = new DwGLTexture.TexturePingPong();
  
  DwPixelFlow context;
  
  public int particles_x;
  public int particles_y;
  
  public int MAX_PARTICLES;
  public int ALIVE_LO = 0;
  public int ALIVE_HI = 0;
  public int ALIVE_PARTICLES = 0;
  
  // a global factor, to comfortably reduce/increase the number of particles to spawn
  public float spwan_scale = 1.0f;
  public float point_size  = 1.5f;
  
  public Param param = new Param();
  
  static public class Param{
    public float dissipation = 0.90f;
    public float inertia     = 0.20f;
  }
  
  public MyParticleSystem(){
  }
  
  public MyParticleSystem(DwPixelFlow context, int MAX_PARTICLES){
    context.papplet.registerMethod("dispose", this);
    this.resize(context, MAX_PARTICLES);
  }
  
  public void dispose(){
    release();
  }
  
  // call this, when the object is not used any longer!
  // OpenGL resources must be released to void memory leaks
  public void release(){
    tex_particles.release();
  }
  
  public void resize(DwPixelFlow context, int MAX_PARTICLES_WANTED){
    particles_x = (int) Math.ceil(Math.sqrt(MAX_PARTICLES_WANTED));
    particles_y = particles_x;
    resize(context, particles_x, particles_y);
  }
  
  public void resize(DwPixelFlow context, int num_particels_x, int num_particels_y){
    this.context = context;
    
    context.begin();
    
    release(); // just in case its not the first resize call
    
    MAX_PARTICLES = particles_x * particles_y;
    System.out.println("ParticelSystem: texture size = "+particles_x+"/"+particles_y +" ("+MAX_PARTICLES+" particles)");
    
    // create shader
    String dir = "data/";
    shader_particleSpawn  = context.createShader(dir + "particleSpawn.frag");
    shader_particleUpdate = context.createShader(dir + "particleUpdate.frag");
    shader_particleRender = context.createShader(dir + "particleRender.glsl", dir + "particleRender.glsl");
    shader_particleRender.vert.setDefine("SHADER_VERT", 1);
    shader_particleRender.frag.setDefine("SHADER_FRAG", 1);

    // allocate texture
    tex_particles.resize(context, GL2ES2.GL_RGBA32F, particles_x, particles_y, GL2ES2.GL_RGBA, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 4, 4);

    context.end("ParticleSystem.resize");
 
    reset();  // initialize particles
  }
  
  
  public void reset(){

    ALIVE_LO = ALIVE_HI = ALIVE_PARTICLES = 0;

    // clear to 0 first, just in case
    tex_particles.src.clear(0);
    tex_particles.dst.clear(0);
    
    // additionally:
    // spawning ALL particles at (-1,-1)
    // this "kind of" clears the texture
    // also, in the render-pass, the vertex gets clipped and no fragment is generated
    // --> speeds up everything
    spawn(null, -1, -1, 0, particles_x *particles_y);
    
    ALIVE_LO = ALIVE_HI = ALIVE_PARTICLES = 0;
  }
  

  /**
   * 
   * @param px_norm normalized x spawn-position [0, 1]
   * @param py_norm normalized y spawn-position [0, 1]
   * @param count
   */
  public void spawn(DwFluid2D fluid, float px, float py, float radius, int count){
    
    count = Math.round(count * spwan_scale);
    
    if(ALIVE_HI == MAX_PARTICLES){
      System.out.println("all particles spawned, respawning from 0");
      ALIVE_HI = 0;
    }

    int spawn_lo = ALIVE_HI; 
    int spawn_hi = Math.min(spawn_lo + count, MAX_PARTICLES); 
    float noise = (float)(Math.random() * Math.PI);

    context.begin();
    context.beginDraw(tex_particles.dst);
    shader_particleSpawn.begin();
    if(fluid != null){
      shader_particleSpawn.uniform2f("wh_viewport", fluid.viewp_w, fluid.viewp_h);
    }
    shader_particleSpawn.uniform1i("spawn_lo", spawn_lo);
    shader_particleSpawn.uniform1i("spawn_hi", spawn_hi);
    shader_particleSpawn.uniform2f("spawn_origin", px, py);
    shader_particleSpawn.uniform1f("spawn_radius", radius);
    shader_particleSpawn.uniform1f("noise", noise);
    shader_particleSpawn.uniform2f("wh_particles" , particles_x, particles_y);
    shader_particleSpawn.uniformTexture("tex_particles" , tex_particles.src);
    shader_particleSpawn.drawFullScreenQuad();
    shader_particleSpawn.end();
    context.endDraw();
    context.end("ParticleSystem.spawn");
    tex_particles.swap();
    
    ALIVE_HI = spawn_hi;
    ALIVE_PARTICLES = Math.max(ALIVE_PARTICLES, ALIVE_HI - ALIVE_LO);
  }
  
  public void update(DwFluid2D fluid){
    context.begin();
    context.beginDraw(tex_particles.dst);
    shader_particleUpdate.begin();
    shader_particleUpdate.uniform2f     ("wh_fluid"     , fluid.fluid_w, fluid.fluid_h);
    shader_particleUpdate.uniform2f     ("wh_particles" , particles_x, particles_y);
    shader_particleUpdate.uniform1f     ("timestep"     , fluid.param.timestep);
    shader_particleUpdate.uniform1f     ("rdx"          , 1.0f / fluid.param.gridscale);
    shader_particleUpdate.uniform1f     ("dissipation"  , param.dissipation);
    shader_particleUpdate.uniform1f     ("inertia"      , param.inertia);
    shader_particleUpdate.uniformTexture("tex_particles", tex_particles.src);
    shader_particleUpdate.uniformTexture("tex_velocity" , fluid.tex_velocity.src);
    shader_particleUpdate.uniformTexture("tex_obstacles", fluid.tex_obstacleC.src);
    shader_particleUpdate.drawFullScreenQuad();
    shader_particleUpdate.end();
    context.endDraw();
    context.end("ParticleSystem.update");
    tex_particles.swap();
  }
  

  public void render(PGraphics2D dst, int background){
    // no need to run the vertex shader for particles that haven't spawned yet
    int num_points_to_render = ALIVE_PARTICLES;
    int w = dst.width;
    int h = dst.height;
    
    dst.beginDraw();
    dst.blendMode(PConstants.BLEND);
    if(background == 0) dst.blendMode(PConstants.ADD); // works nicely on black background
    
    context.begin();
    shader_particleRender.begin();
    shader_particleRender.uniform2f     ("wh_viewport", w, h);
    shader_particleRender.uniform2i     ("num_particles", particles_x, particles_y);
    shader_particleRender.uniform1f     ("point_size"   , point_size);
    shader_particleRender.uniformTexture("tex_particles", tex_particles.src);
    shader_particleRender.drawFullScreenPoints(num_points_to_render);
    shader_particleRender.end();
    context.end("ParticleSystem.render");

    dst.endDraw();
  }

  
}
