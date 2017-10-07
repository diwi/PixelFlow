/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Fluid2D.Fluid_VerletParticleCollisionSystem;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;



public class Fluid_VerletParticleCollisionSystem extends PApplet {
  
  // VerletParticles, motion driven by the fluid-velocity.
  //
  // The particle positions are updated by the physics object, which also
  // handles the collisions.
  // To add fluid-velocity (simulated on the GPU) to particles (simulated on the
  // CPU) the velocity data needs to be transfered, which is a rather expensive 
  // thing to do.
  //
  //
  // controls:
  //
  // LMB: add Velocity
  // RMB: add Density
  
  
  
  private class MyFluidData implements DwFluid2D.FluidData{
    
    // update() is called during the fluid-simulation update step.
    @Override
    public void update(DwFluid2D fluid) {
    
      float px, py, vx, vy, radius, vscale, r, g, b, intensity, temperature;
      
      // add impulse: density + temperature
      px = width-80;
      py = 30;
      radius = 60;
      r = 0.1f; g = 0.4f; b = 1.0f;
      r = 1f; g = 1f; b = 1f;
      vx = 0;
      vy = 50;
      intensity = 1.0f;
      fluid.addDensity(px, py, radius, r, g, b, intensity);
      temperature = 4;
      fluid.addTemperature(px, py, radius, temperature);
      
      // add impulse: density + velocit
      px = width/2;
      py = height/2;
      radius = 15;
      r = 1.0f; g = 0; b = 0.4f;
      r = 1f; g = 1f; b = 1f;
      vx = 0;
      vy = -50;
      fluid.addDensity(px, py, radius, r, g, b, intensity);
      temperature = -4;
      fluid.addVelocity(px, py, radius, vx, vy);
      

      // add impulse: density + velocit
      px = width/2+50;
      py = 50;
      vx = -50;
      vy = 10;
      radius = 15;
      fluid.addDensity(px, py, radius, 1, 1, 1, intensity);
      temperature = -4;
      radius = 35;
      fluid.addVelocity(px, py, radius, vx, vy);
      

      boolean mouse_input = !cp5.isMouseOver() && mousePressed;
      
      // add impulse: density + velocity
      if(mouse_input && mouseButton == LEFT){
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        radius = 15;
        fluid.addDensity(px, py, radius, 1, 1, 1f, 1.0f);
        radius = 20;
        fluid.addVelocity(px, py, radius, vx, vy);
      }
      
      // add impulse: density + velocity
      if(mouse_input && mouseButton == RIGHT){
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        radius = 60;
        fluid.addDensity(px, py, radius, 1, 0.4f, 0f, 1f, 1);
        radius = 60;
        temperature = 5;
        fluid.addTemperature(px, py, radius, temperature);
      }
     
    }
  }
  
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  int fluidgrid_scale = 1;

  
  // Fluid simulation
  DwFluid2D fluid;

  // render targets
  PGraphics2D pg_fluid;
  //texture-buffer, for adding obstacles
  PGraphics2D pg_obstacles;

  // particle system, cpu
  ParticleSystem particlesystem;
  
  DwPhysics.Param param_physics = new DwPhysics.Param();

  // verlet physics, handles the update-step
  DwPhysics<DwParticle2D> physics;
  
  
  // some state variables for the GUI/display
  int     BACKGROUND_COLOR           = 0;
  boolean UPDATE_FLUID               = true;
  boolean DISPLAY_FLUID_TEXTURES     = true;
  boolean DISPLAY_FLUID_VECTORS      = !true;
  int     DISPLAY_fluid_texture_mode = 0;
  boolean COLLISION_DETECTION        = true;
  

  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    DwPixelFlow context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // fluid simulation
    fluid = new DwFluid2D(context, viewport_w, viewport_h, fluidgrid_scale);
    
    // set some simulation parameters
    fluid.param.dissipation_density     = 0.999f;
    fluid.param.dissipation_velocity    = 0.99f;
    fluid.param.dissipation_temperature = 0.50f;
    fluid.param.vorticity               = 0.10f;
    
    // interface for adding data to the fluid simulation
    MyFluidData cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);
   
    // pgraphics for fluid
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(4);
    
    // pgraphics for obstacles
    pg_obstacles = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_obstacles.noSmooth();
    pg_obstacles.beginDraw();
    pg_obstacles.clear();

    // border-obstacle
    pg_obstacles.strokeWeight(20);
    pg_obstacles.stroke(64);
    pg_obstacles.noFill();
    pg_obstacles.rect(0, 0, pg_obstacles.width, pg_obstacles.height);
    pg_obstacles.endDraw();
    
    fluid.addObstacles(pg_obstacles);
    
    
    
    
    
    
    param_physics.GRAVITY = new float[]{0, 0.1f};
    param_physics.bounds  = new float[]{10, 10, width-10, height-10};
    param_physics.iterations_collisions = 4;
    param_physics.iterations_springs    = 0; // no springs in this demo
    
    physics = new DwPhysics<DwParticle2D>(param_physics);
    
    
   
    // particle system object
    particlesystem = new ParticleSystem(this, width, height);
    
    // set some parameters
    particlesystem.PARTICLE_COUNT              = 1000;
    particlesystem.PARTICLE_SCREEN_FILL_FACTOR = 0.60f;
    particlesystem.PARTICLE_SHAPE_IDX          = 0;

    particlesystem.MULT_FLUID                  = 0.80f;
    particlesystem.MULT_GRAVITY                = 0.50f;

    particlesystem.particle_param.DAMP_BOUNDS    = 1f;
    particlesystem.particle_param.DAMP_COLLISION = 0.80f;
    particlesystem.particle_param.DAMP_VELOCITY  = 0.97f;
    
    
    particlesystem.initParticles();
   
    createGUI();

    background(0);
    frameRate(60);
  }
  
  

  
  // float buffer for pixel transfer from OpenGL to the host application
  float[] fluid_velocity;

  public void draw() {    

    // update simulation
    if(UPDATE_FLUID){
      fluid.update();
    }
    
    // clear render target
    pg_fluid.beginDraw();
    pg_fluid.background(BACKGROUND_COLOR);
    pg_fluid.endDraw();
    
    // render fluid stuff
    if(DISPLAY_FLUID_TEXTURES){
      // render: density (0), temperature (1), pressure (2), velocity (3)
      fluid.renderFluidTextures(pg_fluid, DISPLAY_fluid_texture_mode);
    }
    
    if(DISPLAY_FLUID_VECTORS){
      // render: velocity vector field
      fluid.renderFluidVectors(pg_fluid, 10);
    }
    
    


    // Transfer velocity data from the GPU to the host-application
    // This is in general a bad idea because such operations are very slow. So 
    // either do everything in shaders, and avoid memory transfer when possible, 
    // or do it very rarely. however, this is just an example for convenience.
    fluid_velocity = fluid.getVelocity(fluid_velocity);
    
    // add force: FLuid Velocity
    float[] fluid_vxy = new float[2];
    for (DwParticle2D particle : particlesystem.particles) {

      int px_view = Math.round(particle.cx);
      int py_view = Math.round(height - 1 - particle.cy); // invert y
      
      int px_grid = px_view/fluid.grid_scale;
      int py_grid = py_view/fluid.grid_scale;

      int w_grid  = fluid.tex_velocity.src.w;
      int h_grid  = fluid.tex_velocity.src.h;
      
      // clamp coordinates, just in case
      if(px_grid < 0) px_grid = 0; else if(px_grid >= w_grid) px_grid = w_grid;
      if(py_grid < 0) py_grid = 0; else if(py_grid >= h_grid) py_grid = h_grid;
      
      int PIDX = py_grid * w_grid + px_grid;

      fluid_vxy[0] = +fluid_velocity[PIDX * 2 + 0] * 0.05f * particlesystem.MULT_FLUID;
      fluid_vxy[1] = -fluid_velocity[PIDX * 2 + 1] * 0.05f * particlesystem.MULT_FLUID; // invert y
      
      particle.addForce(fluid_vxy);
    }
    
    
    //  add force: Middle Mouse Button (MMB) -> particle[0]
    if(mousePressed && mouseButton == CENTER){
      float[] mouse = {mouseX, mouseY};
      particlesystem.particles[0].moveTo(mouse, 0.3f);
      particlesystem.particles[0].enableCollisions(false);
    } else {
      particlesystem.particles[0].enableCollisions(true);
    }
    
    
    

    // update physics step
    boolean collision_detection = COLLISION_DETECTION && particlesystem.particle_param.DAMP_COLLISION != 0.0;
    
    physics.param.GRAVITY[1] = 0.05f * particlesystem.MULT_GRAVITY;
    physics.param.iterations_collisions = collision_detection ? 4 : 0;
    
    physics.setParticles(particlesystem.particles, particlesystem.particles.length);
    physics.update(1);
    
   
    

    // RENDER
    // display textures
    background(0);
    image(pg_fluid    , 0, 0);
    image(pg_obstacles, 0, 0);
    

    // draw particlesystem
    PGraphics pg = this.g;
    pg.hint(DISABLE_DEPTH_MASK);
    pg.blendMode(BLEND);
//    pg.blendMode(ADD);
    particlesystem.display(pg);
    pg.blendMode(BLEND);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", fluid.fluid_w, fluid.fluid_h, fluid.simulation_step, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  public void fluid_resizeUp(){
    fluid.resize(width, height, fluidgrid_scale = max(1, --fluidgrid_scale));
  }
  public void fluid_resizeDown(){
    fluid.resize(width, height, ++fluidgrid_scale);
  }
  public void fluid_reset(){
    fluid.reset();
  }
  public void fluid_togglePause(){
    UPDATE_FLUID = !UPDATE_FLUID;
  }
  public void fluid_displayMode(int val){
    DISPLAY_fluid_texture_mode = val;
    DISPLAY_FLUID_TEXTURES = DISPLAY_fluid_texture_mode != -1;
  }
  public void fluid_displayVelocityVectors(int val){
    DISPLAY_FLUID_VECTORS = val != -1;
  }
  public void activateCollisionDetection(float[] val){
    COLLISION_DETECTION = (val[0] > 0);
  }

  public void keyReleased(){
    if(key == 'p') fluid_togglePause(); // pause / unpause simulation
    if(key == '+') fluid_resizeUp();    // increase fluid-grid resolution
    if(key == '-') fluid_resizeDown();  // decrease fluid-grid resolution
    if(key == 'r') fluid_reset();       // restart simulation
    
    if(key == '1') DISPLAY_fluid_texture_mode = 0; // density
    if(key == '2') DISPLAY_fluid_texture_mode = 1; // temperature
    if(key == '3') DISPLAY_fluid_texture_mode = 2; // pressure
    if(key == '4') DISPLAY_fluid_texture_mode = 3; // velocity
    
    if(key == 'q') DISPLAY_FLUID_TEXTURES = !DISPLAY_FLUID_TEXTURES;
    if(key == 'w') DISPLAY_FLUID_VECTORS  = !DISPLAY_FLUID_VECTORS;
  }
  
 
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    int sx, sy, px, py, oy;
    
    sx = 100; sy = 14; oy = (int)(sy*1.5f);
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - FLUID
    ////////////////////////////////////////////////////////////////////////////
    Group group_fluid = cp5.addGroup("fluid");
    {
      group_fluid.setHeight(20).setSize(gui_w, 300)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_fluid.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      cp5.addButton("reset").setGroup(group_fluid).plugTo(this, "fluid_reset"     ).setSize(80, 18).setPosition(px    , py);
      cp5.addButton("+"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeUp"  ).setSize(39, 18).setPosition(px+=82, py);
      cp5.addButton("-"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeDown").setSize(39, 18).setPosition(px+=41, py);
      
      px = 10;
     
      cp5.addSlider("velocity").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=(int)(oy*1.5f))
          .setRange(0, 1).setValue(fluid.param.dissipation_velocity).plugTo(fluid.param, "dissipation_velocity");
      
      cp5.addSlider("density").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(fluid.param.dissipation_density).plugTo(fluid.param, "dissipation_density");
      
      cp5.addSlider("temperature").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(fluid.param.dissipation_temperature).plugTo(fluid.param, "dissipation_temperature");
      
      cp5.addSlider("vorticity").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(fluid.param.vorticity).plugTo(fluid.param, "vorticity");
          
      cp5.addSlider("iterations").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 80).setValue(fluid.param.num_jacobi_projection).plugTo(fluid.param, "num_jacobi_projection");
            
      cp5.addSlider("timestep").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(fluid.param.timestep).plugTo(fluid.param, "timestep");
          
      cp5.addSlider("gridscale").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 50).setValue(fluid.param.gridscale).plugTo(fluid.param, "gridscale");
      
      RadioButton rb_setFluid_DisplayMode = cp5.addRadio("fluid_displayMode").setGroup(group_fluid).setSize(80,18).setPosition(px, py+=(int)(oy*1.5f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(2)
          .addItem("Density"    ,0)
          .addItem("Temperature",1)
          .addItem("Pressure"   ,2)
          .addItem("Velocity"   ,3)
          .activate(DISPLAY_fluid_texture_mode);
      for(Toggle toggle : rb_setFluid_DisplayMode.getItems()) toggle.getCaptionLabel().alignX(CENTER);
      
      cp5.addRadio("fluid_displayVelocityVectors").setGroup(group_fluid).setSize(18,18).setPosition(px, py+=(int)(oy*2.5f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("Velocity Vectors", 0)
          .activate(DISPLAY_FLUID_VECTORS ? 0 : 2);
    }
    

    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - PARTICLES
    ////////////////////////////////////////////////////////////////////////////
    Group group_particles = cp5.addGroup("Particles");
    {
      
      group_particles.setHeight(20).setSize(gui_w, 260)
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

      cp5.addSlider("FLUID").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1f).setValue(particlesystem.MULT_FLUID).plugTo(particlesystem, "MULT_FLUID");
      
      cp5.addSlider("SPRINGINESS").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1f).setValue(particlesystem.particle_param.DAMP_COLLISION).plugTo(particlesystem.particle_param, "DAMP_COLLISION");
      
      cp5.addCheckBox("activateCollisionDetection").setGroup(group_particles).setSize(40, 18).setPosition(px, py+=(int)(oy*1.5f))
          .setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
          .addItem("collision detection", 0)
          .activate(COLLISION_DETECTION ? 0 : 2);
      
      RadioButton rgb_shape = cp5.addRadio("setParticleShape").setGroup(group_particles).setSize(50, 18).setPosition(px, py+=(int)(oy*1.5f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(3).plugTo(particlesystem, "setParticleShape")
          .addItem("disk"  , 0)
          .addItem("spot"  , 1)
          .addItem("donut" , 2)
          .addItem("rect"  , 3)
          .addItem("circle", 4)
          .activate(particlesystem.PARTICLE_SHAPE_IDX);
      for(Toggle toggle : rgb_shape.getItems()) toggle.getCaptionLabel().alignX(CENTER);
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - DISPLAY
    ////////////////////////////////////////////////////////////////////////////
    Group group_display = cp5.addGroup("display");
    {
      group_display.setHeight(20).setSize(gui_w, 25)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_display.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      cp5.addSlider("BACKGROUND").setGroup(group_display).setSize(sx,sy).setPosition(px, py)
          .setRange(0, 255).setValue(BACKGROUND_COLOR).plugTo(this, "BACKGROUND_COLOR");
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_fluid)
      .addItem(group_particles)
      .addItem(group_display)
      .open(1);
  }
  
   


  public static void main(String args[]) {
    PApplet.main(new String[] { Fluid_VerletParticleCollisionSystem.class.getName() });
  }
}