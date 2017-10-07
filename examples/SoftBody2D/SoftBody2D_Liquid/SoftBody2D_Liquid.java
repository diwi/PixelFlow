/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package SoftBody2D.SoftBody2D_Liquid;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwLiquidFX;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;
import processing.opengl.PGraphics2D;



public class SoftBody2D_Liquid extends PApplet {
  //
  // 2D Verlet Particle System.
  // 
  // + Collision Detection
  //
  //
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  
  DwPixelFlow context;
  
  // particle system
  ParticleSystem particlesystem;
  
  DwLiquidFX particle_fluid_fx;
  
  PGraphics2D pg_particles;
  PGraphics2D pg_particles2;
  
  // physics parameters
  DwPhysics.Param param_physics = new DwPhysics.Param();
  
  // verlet physics, handles the update-step
  DwPhysics<DwParticle2D> physics;
  
  // some state variables for the GUI/display
  int     BACKGROUND_COLOR    = 0;
  boolean COLLISION_DETECTION = true;
  
  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    pg_particles = (PGraphics2D) createGraphics(width, height, P2D);
    pg_particles.smooth(0);
    
    pg_particles2 = (PGraphics2D) createGraphics(width, height, P2D);
    pg_particles2.smooth(0);
    
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    particle_fluid_fx = new DwLiquidFX(context);
    
    // particle system object
    particlesystem = new ParticleSystem(this, width, height);
    
    // set some parameters
    particlesystem.PARTICLE_COUNT              = 1000;
    particlesystem.PARTICLE_SCREEN_FILL_FACTOR = 0.50f;

    particlesystem.MULT_GRAVITY                  = 1.00f;
    particlesystem.particle_param.DAMP_BOUNDS    = 0.89999f;
    particlesystem.particle_param.DAMP_COLLISION = 0.89999f;
    particlesystem.particle_param.DAMP_VELOCITY  = 0.9999f;
    
    particlesystem.initParticles();
    
    physics = new DwPhysics<DwParticle2D>(param_physics);
    param_physics.GRAVITY = new float[]{0, 0.1f};
    param_physics.bounds  = new float[]{0, 0, width, height};
    param_physics.iterations_collisions = 8;
    param_physics.iterations_springs    = 0; // no springs in this demo
   
    createGUI();

    background(0);
    frameRate(600);
  }
  
 
  public void draw() {    
    
    DwParticle2D mparticle = particlesystem.getMouseParticle();

    //  add force: Middle Mouse Button (MMB) -> particle[0]
    if(mousePressed){
      float[] mouse = {mouseX, mouseY};
      mparticle.moveTo(mouse, 0.3f);
      mparticle.enableCollisions(false);
    } else {
      mparticle.enableCollisions(true);
    }
    
    // update physics step
    boolean collision_detection = COLLISION_DETECTION && particlesystem.particle_param.DAMP_COLLISION != 0.0;
    
    physics.param.GRAVITY[1] = 0.05f * particlesystem.MULT_GRAVITY;
    physics.param.iterations_collisions = collision_detection ? 4 : 0;

    physics.setParticles(particlesystem.particles, particlesystem.particles.length);
    physics.update(1);

    // RENDER
    pg_particles.beginDraw();
    pg_particles.blendMode(REPLACE);
    pg_particles.background(BACKGROUND_COLOR, 0);
    pg_particles.blendMode(BLEND);
    pg_particles.shape(particlesystem.shp_particlesystem);
    pg_particles.endDraw();

    pg_particles2.beginDraw();
    pg_particles2.blendMode(REPLACE);
    pg_particles2.background(BACKGROUND_COLOR, 0);
    pg_particles2.blendMode(BLEND);
    pg_particles2.shape(mparticle.getShape());
    pg_particles2.endDraw();
    

    // apply liquid filter
    if(!keyPressed){
      particle_fluid_fx.param.base_LoD = 2;
      particle_fluid_fx.param.base_blur_radius = 1;
      
      particle_fluid_fx.apply(pg_particles);
      particle_fluid_fx.apply(pg_particles2);
    }
    
    DwFilter.get(context).gamma.apply(pg_particles, pg_particles);
    DwFilter.get(context).gamma.apply(pg_particles2, pg_particles2);
    
    background(BACKGROUND_COLOR);
    blendMode(BLEND);
    image(pg_particles, 0, 0);
    image(pg_particles2, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  


  public void activateCollisionDetection(float[] val){
    COLLISION_DETECTION = (val[0] > 0);
  }


  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    int sx, sy, px, py, oy;
    
    sx = 100; sy = 14; oy = (int)(sy*1.5f);
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - PARTICLES
    ////////////////////////////////////////////////////////////////////////////
    Group group_particles = cp5.addGroup("Particles");
    {
      
      group_particles.setHeight(20).setSize(gui_w, 200)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_particles.getCaptionLabel().align(CENTER, CENTER);
      
      sx = 100; px = 10; py = 10;oy = (int)(sy*1.4f);
      
      cp5.addButton("reset particles").setGroup(group_particles).setWidth(160).setPosition(10, 10).plugTo(particlesystem, "initParticles");

      cp5.addSlider("Particle count").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy+10)
          .setRange(10, 10000).setValue(particlesystem.PARTICLE_COUNT).plugTo(particlesystem, "setParticleCount");
      
      cp5.addSlider("Fill Factor").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
      .setRange(0.2f, 1.5f).setValue(particlesystem.PARTICLE_SCREEN_FILL_FACTOR).plugTo(particlesystem, "setFillFactor");
      
      cp5.addSlider("VELOCITY").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy+10)
          .setRange(0.85f, 1.0f).setValue(particlesystem.particle_param.DAMP_VELOCITY).plugTo(particlesystem.particle_param, "DAMP_VELOCITY");
      
      cp5.addSlider("GRAVITY").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 10f).setValue(particlesystem.MULT_GRAVITY).plugTo(particlesystem, "MULT_GRAVITY");

      cp5.addSlider("SPRINGINESS").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1f).setValue(particlesystem.particle_param.DAMP_COLLISION).plugTo(particlesystem.particle_param, "DAMP_COLLISION");
      
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_particles)
      .open(0);
  }
  
   


  public static void main(String args[]) {
    PApplet.main(new String[] { SoftBody2D_Liquid.class.getName() });
  }
}