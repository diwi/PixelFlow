/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.fluid;

import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;

import processing.core.PConstants;
import processing.core.PImage;
import processing.opengl.PGraphics2D;


public class DwFluidParticleSystem2D{

  public DwGLSLProgram shader_particleInit  ;
  public DwGLSLProgram shader_particleUpdate;
  public DwGLSLProgram shader_particleRender;
  
  public DwGLTexture.TexturePingPong tex_particles = new DwGLTexture.TexturePingPong();

  public DwPixelFlow context;
  
  public int particles_x;
  public int particles_y;
  
  public Param param = new Param();
  
  static public class Param{
    public float dissipation = 0.8f;
    public float inertia     = 0.2f;
  }
  
  public DwFluidParticleSystem2D(){
  }
  
  public DwFluidParticleSystem2D(DwPixelFlow context, int num_particels_x, int num_particels_y){
    context.papplet.registerMethod("dispose", this);
    this.resize(context, num_particels_x, num_particels_y);
  }
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_particles.release();
  }
  
  public void resize(DwPixelFlow context_, int num_particels_x, int num_particels_y){
    context = context_;
    
    context.begin();
    
    release();
   
    this.particles_x = num_particels_x;
    this.particles_y = num_particels_y;
//    System.out.println("ParticelSystem: size = "+particles_x+"/"+particles_y +" ("+particles_x*particles_y+" objects)");
    
    // create shader
    shader_particleInit   = context.createShader(DwPixelFlow.SHADER_DIR+"ParticleSystem/particleInit.frag");
    shader_particleUpdate = context.createShader(DwPixelFlow.SHADER_DIR+"ParticleSystem/particleUpdate.frag");
    shader_particleRender = context.createShader(DwPixelFlow.SHADER_DIR+"ParticleSystem/particleRender.vert", DwPixelFlow.SHADER_DIR+"ParticleSystem/particleRender.frag");
    
    // allocate texture
    tex_particles.resize(context, GL2ES2.GL_RGBA32F, particles_x, particles_y, GL2ES2.GL_RGBA, GL2ES2.GL_FLOAT, GL2ES2.GL_NEAREST, 4, 4);
    tex_particles.src.clear(0);
    tex_particles.dst.clear(0);
    
    init();
    
    context.end("ParticelSystem.resize");
  }
  
  public void reset(){
    init();
  }
  
  public void init(){
    context.begin();
    context.beginDraw(tex_particles.dst);
    shader_particleInit.begin();
    shader_particleInit.uniform2f("wh", particles_x, particles_y);
    shader_particleInit.drawFullScreenQuad();
    shader_particleInit.end();
    context.endDraw();
    context.end("ParticelSystem.init");
    tex_particles.swap();
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
    context.end("ParticelSystem.update");
    tex_particles.swap();
  }
  
  
  public void render(PGraphics2D dst, PImage sprite, int display_mode){
    int[] sprite_tex_handle = new int[1];
    if(sprite != null){
      sprite_tex_handle[0] = dst.getTexture(sprite).glName;
    }

    int w = dst.width;
    int h = dst.height;

    dst.beginDraw();
//    dst.blendMode(PConstants.BLEND);
    dst.blendMode(PConstants.ADD);

    context.begin();
    shader_particleRender.begin();
    shader_particleRender.uniform2i     ("num_particles", particles_x, particles_y);
    shader_particleRender.uniformTexture("tex_particles", tex_particles.src);
    shader_particleRender.uniformTexture("tex_sprite"   , sprite_tex_handle[0]);
    shader_particleRender.uniform1i     ("display_mode" , display_mode); // 0 ... 1px points, 1 = sprite texture,  2 ... falloff points
    shader_particleRender.drawFullScreenPoints(0, 0, w, h, particles_x * particles_y);
    shader_particleRender.end();
    context.end("ParticelSystem.render");
    
    dst.endDraw();
  }

  
}
