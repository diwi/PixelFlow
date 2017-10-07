/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Fluid2D.Fluid_SlowBuoyancy;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;
import com.thomasdiewald.pixelflow.java.fluid.DwFluidParticleSystem2D;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;

public class Fluid_SlowBuoyancy extends PApplet {
  
  // Experiment to demonstrate Buoyancy.
  // The top emitter adds negative temperature + density, while the bottom
  // emitter adds positive temperature + density.
  //
  // controls:
  //
  // LMB: add Velocity
  // MMB: add Density
  
  
  private class MyFluidData implements DwFluid2D.FluidData{
     
    @Override
    // this is called during the fluid-simulation update step.
    public void update(DwFluid2D fluid) {
    
      float px, py, vx, vy, radius, vscale, temperature;

      boolean mouse_input = !cp5.isMouseOver() && mousePressed;
      if(mouse_input ){
        
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        
        if(mouseButton == LEFT){
          radius = 20;
          fluid.addVelocity(px, py, radius, vx, vy);
        }
        if(mouseButton == CENTER){
          radius = 50;
          fluid.addDensity (px, py, radius, 1, 1, 1, 1f, 1);
        }
        if(mouseButton == RIGHT){
          radius = 15;
          fluid.addTemperature(px, py, radius, 1f);
        }
      }
      
      
      temperature = 0.5f;
      vscale = 15;
      px     = width/2-0;
      py     = 0;
      radius = 100;
      fluid.addDensity (px, py, radius, 1.0f, 0.0f, 0.40f, 1f, 1);
      radius = 100;
      fluid.addTemperature(px, py, radius, temperature);
      
      px     = width/2+0;
      py     = height;
      radius = 100;
      fluid.addDensity (px, py, radius, 0.00f, 0.40f, 1, 1f, 1);
      radius = 100;
      fluid.addTemperature(px, py, radius, -temperature);
    }
    
  }
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = viewport_w-gui_w;
  int gui_y = 0;
  
  int fluidgrid_scale = 1;
  

  DwFluid2D fluid;
  MyFluidData cb_fluid_data;
  
  // default particle system
  DwFluidParticleSystem2D particle_system;
  
  // fluid rendertarget
  PGraphics2D pg_fluid;
  
  //texture-buffer, for adding obstacles
  PGraphics2D pg_obstacles;

  // some state variables for the GUI/display
  int     BACKGROUND_COLOR           = 0;
  boolean UPDATE_FLUID               = true;
  boolean DISPLAY_FLUID_TEXTURES     = true;
  boolean DISPLAY_FLUID_VECTORS      = false;
  int     DISPLAY_fluid_texture_mode = 0;
  boolean DISPLAY_PARTICLES          = false;

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

    fluid.param.dissipation_density     = 0.99f;
    fluid.param.dissipation_velocity    = 0.85f;
    fluid.param.dissipation_temperature = 0.99f;
    fluid.param.vorticity               = 0.00f;
    fluid.param.timestep                = 0.25f;
    fluid.param.num_jacobi_projection   = 80;
    
    // interface for adding data to the fluid simulation
    cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);

    // fluid render target
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(4);

    
    pg_obstacles = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_obstacles.smooth(4);
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    pg_obstacles.rectMode(CENTER);
    pg_obstacles.fill(64);
    pg_obstacles.noStroke();
    pg_obstacles.translate(width/2, height/2);
    
    pg_obstacles.ellipse(  0, -180, 80, 80);
    pg_obstacles.ellipse(-30, +200, 50, 50);
    pg_obstacles.ellipse(+30, +200, 50, 50);

    pg_obstacles.endDraw();
    
 
    // particles
    particle_system = new DwFluidParticleSystem2D();
    particle_system.resize(context, viewport_w/3, viewport_h/3);
    
    createGUI();

    background(0);
    frameRate(60);
  }
  
  
  

  public void draw() {
   

    if(UPDATE_FLUID){
      fluid.addObstacles(pg_obstacles);
      fluid.update();
      particle_system.update(fluid);
    }

    
    pg_fluid.beginDraw();
    pg_fluid.background(BACKGROUND_COLOR);
    pg_fluid.endDraw();
    
    if(DISPLAY_FLUID_TEXTURES){
      fluid.renderFluidTextures(pg_fluid, DISPLAY_fluid_texture_mode);
    }
    
    if(DISPLAY_FLUID_VECTORS){
      fluid.renderFluidVectors(pg_fluid, 10);
    }
    
    if(DISPLAY_PARTICLES){
      particle_system.render(pg_fluid, null, 0);
    }
    
    // display
    image(pg_fluid    , 0, 0);
    image(pg_obstacles, 0, 0);
    
    
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
      group_display.setHeight(20).setSize(gui_w, height)
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
      .open(0, 1);
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Fluid_SlowBuoyancy.class.getName() });
  }
}