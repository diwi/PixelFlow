/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package Fluid2D.Fluid_CustomParticles;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.opengl.PJOGL;

public class Fluid_CustomParticles extends PApplet {
  
  // Fluid_CustomParticles show how to setup a completely customized particle
  // system that is interacting with the fluid simulation.
  // The particle data (positions) is stored in an OpenGL texture (GL_RGBA32F) 
  // and gets updated each frame using GLSL shaders.
  // No Data transfer (application <-> device), is required.
  //
  //
  // controls:
  //
  // LMB: add Particles + Velocity
  // MMB: add Particles
  // RMB: add Particles
  
  
  
  private class MyFluidData implements DwFluid2D.FluidData{
    
    // update() is called during the fluid-simulation update step.
    @Override
    public void update(DwFluid2D fluid) {
    
      float px, py, vx, vy, radius, vscale, temperature;
 
      radius = 15;
      vscale = 10;
      px     = width/2;
      py     = 50;
      vx     = 1 * +vscale;
      vy     = 1 *  vscale;
      radius = 40;
      temperature = 1f;
      fluid.addDensity(px, py, radius, 0.2f, 0.3f, 0.5f, 1.0f);
      fluid.addTemperature(px, py, radius, temperature);
      particles.spawn(fluid, px, py, radius, 100);
      

      boolean mouse_input = !cp5.isMouseOver() && mousePressed;
      
      // add impulse: density + velocity, particles
      if(mouse_input && mouseButton == LEFT){
        radius = 15;
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        fluid.addDensity (px, py, radius, 0.25f, 0.0f, 0.1f, 1.0f);
        fluid.addVelocity(px, py, radius, vx, vy);
        particles.spawn(fluid, px, py, radius*2, 300);
      }
      
      // add impulse: density + temperature, particles
      if(mouse_input && mouseButton == CENTER){
        radius = 15;
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        temperature = 2f;
        fluid.addDensity(px, py, radius, 0.25f, 0.0f, 0.1f, 1.0f);
        fluid.addTemperature(px, py, radius, temperature);
        particles.spawn(fluid, px, py, radius, 100);
      }
      
      // particles
      if(mouse_input && mouseButton == RIGHT){
        px     = mouseX;
        py     = height - 1 - mouseY; // invert
        radius = 50;
        particles.spawn(fluid, px, py, radius, 300);
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
  
  int fluidgrid_scale = 3;
  
  DwFluid2D fluid;

  // render targets
  PGraphics2D pg_fluid;
  //texture-buffer, for adding obstacles
  PGraphics2D pg_obstacles;
  
  // custom particle system
  MyParticleSystem particles;
  
  // some state variables for the GUI/display
  int     BACKGROUND_COLOR           = 0;
  boolean UPDATE_FLUID               = true;
  boolean DISPLAY_FLUID_TEXTURES     = false;
  boolean DISPLAY_FLUID_VECTORS      = false;
  int     DISPLAY_fluid_texture_mode = 0;
  boolean DISPLAY_PARTICLES          = true;
  
  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
    PJOGL.profile = 3;
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
    fluid.param.dissipation_temperature = 0.80f;
    fluid.param.vorticity               = 0.10f;
    
    // interface for adding data to the fluid simulation
    MyFluidData cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);
   
    // pgraphics for fluid
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(4);
    pg_fluid.beginDraw();
    pg_fluid.background(BACKGROUND_COLOR);
    pg_fluid.endDraw();
    
        // pgraphics for obstacles
    pg_obstacles = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_obstacles.smooth(4);
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    float radius;
    radius = 200;
    pg_obstacles.stroke(64);
    pg_obstacles.strokeWeight(10);
    pg_obstacles.noFill();
    pg_obstacles.rect(1*width/2f,  1*height/4f, radius, radius, 20);
    // border-obstacle
    pg_obstacles.strokeWeight(20);
    pg_obstacles.stroke(64);
    pg_obstacles.noFill();
    pg_obstacles.rect(0, 0, pg_obstacles.width, pg_obstacles.height);
    pg_obstacles.endDraw();
    
    fluid.addObstacles(pg_obstacles);
    
    // custom particle object
    particles = new MyParticleSystem(context, 1000 * 1000);

    createGUI();
    
    background(0);
    frameRate(60);
  }
  



  public void draw() {    
    
 
    // update simulation
    if(UPDATE_FLUID){
      fluid.addObstacles(pg_obstacles);
      fluid.update();
      particles.update(fluid);
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
    
    if( DISPLAY_PARTICLES){
      // render: particles; 0 ... points, 1 ...sprite texture, 2 ... dynamic points
      particles.render(pg_fluid, BACKGROUND_COLOR);
    }
    

    // display
    image(pg_fluid    , 0, 0);
    image(pg_obstacles, 0, 0);

    
    // display number of particles as text
    String txt_num_particles = String.format("Particles  %,d", particles.ALIVE_PARTICLES);
    fill(0, 0, 0, 220);
    noStroke();
    rect(10, height-10, 160, -30);
    fill(255,128,0);
    text(txt_num_particles, 20, height-20);
 
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

  public void fluid_displayParticles(int val){
    DISPLAY_PARTICLES = val != -1;
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
    // GUI - DISPLAY
    ////////////////////////////////////////////////////////////////////////////
    Group group_display = cp5.addGroup("display");
    {
      group_display.setHeight(20).setSize(gui_w, 50)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_display.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      cp5.addSlider("BACKGROUND").setGroup(group_display).setSize(sx,sy).setPosition(px, py)
          .setRange(0, 255).setValue(BACKGROUND_COLOR).plugTo(this, "BACKGROUND_COLOR");
      
      cp5.addRadio("fluid_displayParticles").setGroup(group_display).setSize(18,18).setPosition(px, py+=(int)(oy*1.5f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("display particles", 0)
          .activate(DISPLAY_PARTICLES ? 0 : 2);
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_fluid)
      .addItem(group_display)
      .open(4);
  }
  


  

  
  public static void main(String args[]) {
    PApplet.main(new String[] { Fluid_CustomParticles.class.getName() });
  }
}