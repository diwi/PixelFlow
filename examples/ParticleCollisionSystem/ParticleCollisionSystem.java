/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package ParticleCollisionSystem;


import com.thomasdiewald.pixelflow.java.Fluid;
import com.thomasdiewald.pixelflow.java.PixelFlow;

import controlP5.Accordion;
import controlP5.Button;
import controlP5.CheckBox;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;



public class ParticleCollisionSystem extends PApplet {
  
  private class MyFluidData implements Fluid.FluidData{
    
    // update() is called during the fluid-simulation update step.
    @Override
    public void update(Fluid fluid) {
    
      float px, py, vx, vy, radius, vscale, r, g, b, intensity, temperature;
      
      // add impulse: density + temperature
      px = width-80;
      py = 30;
      radius = 60;
      r = 0.1f;
      g = 0.4f;
      b = 1.0f;
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
      r = 1.0f;
      g = 0;
      b = 0.4f;
      vx = 0;
      vy = -50;
      fluid.addDensity(px, py, radius, r, g, b, intensity);
      temperature = -4;
      fluid.addVelocity(px, py, radius, vx, vy);
      
      
      
      // add impulse: density + velocit
      px = width/2+50;
      py = 50;
      r = 1f;
      g = 1f;
      b = 1f;
      vx = -50;
      vy = 10;
      radius = 15;
      fluid.addDensity(px, py, radius, r, g, b, intensity);
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
        radius = 12;
        fluid.addDensity(px, py, radius, 1, 1, 1f, 1.0f);
        radius = 25;
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
  
//  int viewport_w = 800;
//  int viewport_h = 800;
  int fluidgrid_scale = 1;
  
  int BACKGROUND_COLOR = 0;
  
  // Fluid simulation
  public Fluid fluid;

  // render targets
  PGraphics2D pg_fluid;
  //texture-buffer, for adding obstacles
  PGraphics2D pg_obstacles;

  
  
  public ParticleSystem particlesystem;
  
  public boolean COLLISION_DETECTION = true;
  public CollisionGridAccelerator collision_grid;

  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
    context.printGL();
    
    
    // fluid simulation
    fluid = new Fluid(context, viewport_w, viewport_h, fluidgrid_scale);
    
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

    
    particlesystem = new ParticleSystem(this);
    
    // set some parameters
    particlesystem.PARTICLE_SHAPE_IDX = 0;
    particlesystem.PARTICLE_COUNT = 1000;
    particlesystem.PARTICLE_SCREEN_FILL_FACTOR = 0.7f;
    
    Particle.COLLISION_SPRING = 0.80f;
    Particle.COLLISION_DAMPING= 0.80f;
    Particle.GRAVITY = 0.01f;
     
    particlesystem.initParticles();
    
    collision_grid = new CollisionGridAccelerator();

    createGUI();

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
    
    if(DISPLAY_PARTICLES){
    }
    

    // add a force to particle[0] with the middle mousebutton
    if(mousePressed && mouseButton == CENTER){
      Particle particle = particlesystem.particles[0];
      
      float dx = mouseX - particle.x;
      float dy = mouseY - particle.y;
      
      float damping_pos = 0.2f;
      float damping_vel = 0.2f;
      
      particle.x  += dx * damping_pos;
      particle.y  += dy * damping_pos;
      
      particle.vx += dx * damping_vel;
      particle.vy += dy * damping_vel;
    }
    
    
    // Transfer velocity data from the GPU to the host-application
    // This is in general a bad idea because such operations are very slow. So 
    // either do everything in shaders, and avoid memory transfer when possible, 
    // or do it very rarely. however, this is just an example for convenience.
    fluid_velocity = fluid.getVelocity(fluid_velocity);
    
    

    // optinally, collision detection can be applied
    if(COLLISION_DETECTION && Particle.COLLISION_SPRING != 0.0){
      collision_grid.updateCollisions(particlesystem.particles);
    }
 
    // update step: particle motion
    // 1) add fluid velocity to the particles' velocity
    // 2) add gravity
    // 3) update velocity + position + color
    for (Particle particle : particlesystem.particles) {
      int px_view = Math.round(particle.x);
      int py_view = Math.round(height - 1 - particle.y); // invert y
      
      int px_grid = px_view/fluid.grid_scale;
      int py_grid = py_view/fluid.grid_scale;

      int w_grid  = fluid.tex_velocity.src.w;

      int PIDX    = py_grid * w_grid + px_grid;

      float fluid_vx = +fluid_velocity[PIDX * 2 + 0] * 0.05f;
      float fluid_vy = -fluid_velocity[PIDX * 2 + 1] * 0.05f; // invert y
      
     
      particle.applyFLuid(fluid_vx, fluid_vy);
      particle.applyGravity();
      particle.updatePosition(10, 10, width-10, height-10);
      particle.updateColor(particlesystem.PARTICLE_SHADING);
    }
    
    

    // RENDER
    // display textures
    background(0);
    image(pg_fluid    , 0, 0);
    image(pg_obstacles, 0, 0);
    

    // draw particlesystem
    if(DISPLAY_PARTICLES){
      PGraphics pg = this.g;
      pg.blendMode(BLEND);
      pg.hint(DISABLE_DEPTH_MASK);
      particlesystem.display(pg);
    }
    

    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", fluid.fluid_w, fluid.fluid_h, fluid.simulation_step, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  
  
  
  
  boolean UPDATE_FLUID = true;
  
  boolean DISPLAY_FLUID_TEXTURES  = true;
  boolean DISPLAY_FLUID_VECTORS   = !true;
  boolean DISPLAY_PARTICLES       = true;
  
  int     DISPLAY_fluid_texture_mode = 0;
  
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
    if(key == 'e') DISPLAY_PARTICLES      = !DISPLAY_PARTICLES;
  }
  

  public void fluid_resizeUp(){
    fluid.resize(width, height, fluidgrid_scale = max(1, --fluidgrid_scale));
  }
  public void fluid_resizeDown(){
    fluid.resize(width, height, ++fluidgrid_scale);
  }
  public void fluid_reset(){
//    particle_system.reset();
    fluid.reset();
  }
  public void fluid_togglePause(){
    UPDATE_FLUID = !UPDATE_FLUID;
  }
  public void setDisplayMode(int val){
    DISPLAY_fluid_texture_mode = val;
    DISPLAY_FLUID_TEXTURES = DISPLAY_fluid_texture_mode != -1;
  }
  public void setDisplayVelocityVectors(int val){
    DISPLAY_FLUID_VECTORS = val != -1;
  }
  public void setDisplayParticles(int val){
    DISPLAY_PARTICLES = val != -1;
  }
  
  
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    Group group_fluid = cp5.addGroup("fluid controls")
//    .setPosition(20, 40)
    .setHeight(20).setWidth(180)
    .setBackgroundHeight(330)
    .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_fluid.getCaptionLabel().align(LEFT, CENTER);
  
    Button breset = cp5.addButton("reset").setGroup(group_fluid).plugTo(this, "fluid_reset").setWidth(75);
    Button bplus  = cp5.addButton("+"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeUp").setWidth(25);
    Button bminus = cp5.addButton("-"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeDown").setWidth(25).linebreak();
    
    float[] pxy = breset.getPosition();
    bplus .setPosition(pxy[0] + 75 + 10, pxy[1]);
    bminus.setPosition(pxy[0] + 75 + 25 + 20, pxy[1]);
    
    int px, py, oy;
    int sx = 100, sy = 14;
    
    cp5.addSlider("velocity").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.dissipation_velocity)
    .plugTo(fluid.param, "dissipation_velocity").linebreak();
    
    cp5.addSlider("density").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.dissipation_density)
    .plugTo(fluid.param, "dissipation_density").linebreak();
    
    cp5 .addSlider("temperature").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.dissipation_temperature)
    .plugTo(fluid.param, "dissipation_temperature").linebreak();
  
    cp5 .addSlider("vorticity").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.vorticity)
    .plugTo(fluid.param, "vorticity").linebreak();
        
    cp5.addSlider("iterations").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 80).setValue(fluid.param.num_jacobi_projection)
    .plugTo(fluid.param, "num_jacobi_projection").linebreak();
          
    cp5.addSlider("timestep").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.timestep)
    .plugTo(fluid.param, "timestep").linebreak();
        
    cp5.addSlider("gridscale").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 50).setValue(fluid.param.gridscale)
    .plugTo(fluid.param, "gridscale").linebreak();
    
    RadioButton rb_setDisplayMode = cp5.addRadio("setDisplayMode").setGroup(group_fluid)
        .setPosition(10, 210).setSize(80,18)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(2)
        .addItem("Density"    ,0)
        .addItem("Temperature",1)
        .addItem("Pressure"   ,2)
        .addItem("Velocity"   ,3)
        .activate(0);
    for(Toggle toggle : rb_setDisplayMode.getItems()) toggle.getCaptionLabel().alignX(CENTER);
    
    cp5.addRadio("setDisplayVelocityVectors").setGroup(group_fluid)
        .setPosition(10, 255).setSize(18,18)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
        .addItem("Velocity Vectors",0)
        ;

    cp5.addNumberbox("BACKGROUND_COLOR").setGroup(group_fluid)
    .setPosition(10,280).setSize(80,18)
    .setMin(0).setMax(255)
    .setScrollSensitivity(1) .setValue(BACKGROUND_COLOR);
    
   
    group_fluid.close();
    
    
    
    
    Group group_particles = cp5.addGroup("Particle Controls")
//    .setPosition(20, 40)
    .setHeight(20).setWidth(180)
    .setBackgroundHeight(400)
    .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_particles.getCaptionLabel().align(LEFT, CENTER);
    
    sx = 90;
    py = 10;
    px = 10;
    oy = (int) (sy * 1.4f);
    
    cp5.addButton("reset particles").setGroup(group_particles).setWidth(160).setPosition(10, 10)
    .plugTo(this, "initParticles").linebreak();
    
    py+=10;
    
    cp5.addSlider("Particle count").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(10, 15000).setValue(particlesystem.PARTICLE_COUNT)
    .plugTo(this, "setParticleCount").linebreak();
    
    cp5.addSlider("Fill Factor").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 2).setValue(particlesystem.PARTICLE_SCREEN_FILL_FACTOR)
    .plugTo(this, "setParticlesFillFactor").linebreak();
    
    cp5.addSlider("GRAVITY").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 0.05f).setValue(Particle.GRAVITY)
    .plugTo(this, "setParticle_GRAVITY").linebreak();

    
    cp5.addSlider("VEL DISSIPATION").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0.85f, 1.0f).setValue(Particle.VELOCITY_DISSIPATION)
    .plugTo(this, "seParticle_VELOCITY_DISSIPATION").linebreak();
    
    py+=10;
    
    cp5.addSlider("FLUID DISSIPATION").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1f).setValue(Particle.FLUID_DISSIPATION)
    .plugTo(this, "setParticle_FLUID_DISSIPATION").linebreak();
    
    cp5.addSlider("FLUID INERTIA").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1f).setValue(Particle.FLUID_INERTIA)
    .plugTo(this, "setParticle_FLUID_INERTIA").linebreak();
    
    cp5.addSlider("FLUID SCALE").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 2f).setValue(Particle.FLUID_SCALE)
    .plugTo(this, "setParticle_FLUID_SCALE").linebreak();
    
    
    
    
    cp5.addSlider("COLL SPRING").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=(int)(oy*1.5f))
    .setRange(0, 1f).setValue(Particle.COLLISION_SPRING)
    .plugTo(this, "setParticle_COLLISION_SPRING").linebreak();
    
    cp5.addSlider("COLL DAMPING").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1f).setValue(Particle.COLLISION_DAMPING)
    .plugTo(this, "setParticle_COLLISION_DAMPING").linebreak();

    
//    COLLISION_DETECTION
    CheckBox cb = cp5.addCheckBox("activateCollisions").setGroup(group_particles).setSize(18, 18).setPosition(px, py+=(int)(oy*1.5f))
    .setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
    .addItem("collision detection", 0)
    ;
    
    if(COLLISION_DETECTION) cb.activate(0);

    
    py+=10;
    
    cp5.addSlider("Particle SHADING").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 255).setValue(particlesystem.PARTICLE_SHADING)
    .plugTo(particlesystem, "PARTICLE_SHADING").linebreak();
    

    cp5.addRadio("setParticleShape").setGroup(group_particles).setSize(18, 18).setPosition(px, py+=oy)
    .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
    .addItem("disk"      , 0)
    .addItem("hightlight", 1)
    .addItem("donut"     , 2)
    .addItem("rectangle" , 3)
    .addItem("circle"    , 4)
    .activate(particlesystem.PARTICLE_SHAPE_IDX);

    
    Accordion accordion = cp5.addAccordion("acc")
        .setPosition(20,20)
        .setWidth(180)
        .addItem(group_fluid)
        .addItem(group_particles)
        ;
    
    
    accordion.setCollapseMode(Accordion.MULTI);
    accordion.open(1);
    
  }
  
  
  public void setParticleShape(int val){
    if( val == -1){
      DISPLAY_PARTICLES = false;
    } else {
      DISPLAY_PARTICLES = true;
      particlesystem.PARTICLE_SHAPE_IDX = val;
      particlesystem.initParticleShapes();
    }
  }
  
  public void activateCollisions(float[] val){
    COLLISION_DETECTION = (val[0] > 0);
  }
  
  public void initParticles(){
    particlesystem.initParticles();
  }
  
  public void setParticleCount(int count){
    if(count == particlesystem.PARTICLE_COUNT && particlesystem.particles != null && particlesystem.particles.length == particlesystem.PARTICLE_COUNT){
      return;
    }
    particlesystem.PARTICLE_COUNT = count;
    initParticles();
  }
  public void setParticlesFillFactor(float screen_fill_factor){
    if(screen_fill_factor == particlesystem.PARTICLE_SCREEN_FILL_FACTOR){
      return;
    }
    particlesystem.PARTICLE_SCREEN_FILL_FACTOR = screen_fill_factor;
    particlesystem.initParticlesSize();
    particlesystem.initParticleShapes();
  }
  
  public void setParticle_GRAVITY(float v){
    Particle.GRAVITY = v;
  }
  public void setParticle_COLLISION_SPRING(float v){
    Particle.COLLISION_SPRING = v;
  }
  public void setParticle_COLLISION_DAMPING(float v){
    Particle.COLLISION_DAMPING = v;
  }
  public void setParticle_VELOCITY_DISSIPATION(float v){
    Particle.VELOCITY_DISSIPATION = v;
  }
  public void setParticle_FLUID_DISSIPATION(float v){
    Particle.FLUID_DISSIPATION = v;
  }
  public void setParticle_FLUID_INERTIA(float v){
    Particle.FLUID_INERTIA = v;
  }
  public void setParticle_FLUID_SCALE(float v){
    Particle.FLUID_SCALE = v;
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { ParticleCollisionSystem.class.getName() });
  }
}