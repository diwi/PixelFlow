/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Fluid2D.Fluid_LiquidText;


import java.io.File;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;
import com.thomasdiewald.pixelflow.java.fluid.DwFluidParticleSystem2D;
import com.thomasdiewald.pixelflow.java.utils.DwFrameCapture;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;


public class Fluid_LiquidText extends PApplet {
  
  // Fluid_LiquidText is more or less the same as Fluid_LiquidPainting.
  // 
  // The density source is a PGraphics object, which at program-start got some 
  // text drawn to it.
  //
  // controls:
  //
  // LMB: add Velocity
  // MMB: add Density
  // RMB: add Temperature
  
  
  
  private class MyFluidData implements DwFluid2D.FluidData{
    
    @Override
    // this is called during the fluid-simulation update step.
    public void update(DwFluid2D fluid) {
    
      float px, py, vx, vy, radius, vscale;

      boolean mouse_input = !cp5.isMouseOver() && mousePressed;
      if(mouse_input ){
        
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        
        if(mouseButton == LEFT){
          radius = 15;
          fluid.addVelocity(px, py, radius, vx, vy, 2, 0.5f);
        }
        if(mouseButton == CENTER){
          radius = 30;
          fluid.addDensity (px, py, radius, 1.0f, 0.0f, 0.40f, 1f, 1);
        }
      }
  
      // use the text as input for density and temperature
      addDensityTexture    (fluid, pg_text);
      addTemperatureTexture(fluid, pg_text);
    }
    
    // custom shader, to add density from a texture (PGraphics2D) to the fluid.
    public void addDensityTexture(DwFluid2D fluid, PGraphics2D pg){
      int[] pg_tex_handle = new int[1];
//      pg_tex_handle[0] = pg.getTexture().glName;
      context.begin();
      context.getGLTextureHandle(pg, pg_tex_handle);
      context.beginDraw(fluid.tex_density.dst);

      DwGLSLProgram shader = context.createShader("data/addDensity.frag");
      shader.begin();
      shader.uniform2f     ("wh"        , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode", 6);   
      shader.uniform1f     ("mix_value" , 0.05f);     
      shader.uniform1f     ("multiplier", 1);     
      shader.uniformTexture("tex_ext"   , pg_tex_handle[0]);
      shader.uniformTexture("tex_src"   , fluid.tex_density.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end("app.addDensityTexture");
      fluid.tex_density.swap();
    }
    
    // custom shader, to add temperature from a texture (PGraphics2D) to the fluid.
    public void addTemperatureTexture(DwFluid2D fluid, PGraphics2D pg){
      int[] pg_tex_handle = new int[1];
//      pg_tex_handle[0] = pg.getTexture().glName;
      context.begin();
      context.getGLTextureHandle(pg, pg_tex_handle);
      context.beginDraw(fluid.tex_temperature.dst);
      DwGLSLProgram shader = context.createShader("data/addTemperature.frag");
      shader.begin();
      shader.uniform2f     ("wh"        , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode", 1);   
      shader.uniform1f     ("mix_value" , 0.02f);     
      shader.uniform1f     ("multiplier", 0.015f);     
      shader.uniformTexture("tex_ext"   , pg_tex_handle[0]);
      shader.uniformTexture("tex_src"   , fluid.tex_temperature.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end("app.addTemperatureTexture");
      fluid.tex_temperature.swap();
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
  
  DwPixelFlow context;
  DwFluid2D fluid;
  MyFluidData cb_fluid_data;
  DwFluidParticleSystem2D particle_system;
  

  PGraphics2D pg_fluid;       // render target
  PGraphics2D pg_obstacles;   // texture-buffer, for adding obstacles
  PGraphics2D pg_text;        // texture-buffer, for adding fluid data (density and temperature)
  
  // sprite, can be used for the particle system
  PImage img_sprite;
  
  // processing font
  PFont font;
  
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
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    // fluid simulation
    fluid = new DwFluid2D(context, viewport_w, viewport_h, fluidgrid_scale);
    
    // fuild simulation parameters
    fluid.param.dissipation_density     = 0.90f;
    fluid.param.dissipation_velocity    = 0.90f;
    fluid.param.dissipation_temperature = 0.90f;
    
    // interface for adding data to the fluid simulation
    cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);

    // processing font
    font = createFont("../data/SourceCodePro-Regular.ttf", 128);

    // fluid render target
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(4);

    // particles
    particle_system = new DwFluidParticleSystem2D();
    particle_system.resize(context, viewport_w/3, viewport_h/3);
    
    // obstacles buffer
    pg_obstacles = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_obstacles.noSmooth();
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    // border
    pg_obstacles.strokeWeight(24);
    pg_obstacles.stroke(255);
    pg_obstacles.noFill();
    pg_obstacles.rect(0, 0, pg_obstacles.width, pg_obstacles.height, 24);
    pg_obstacles.endDraw();
    
    // add the obstacles to the simulation
    fluid.addObstacles(pg_obstacles);
 
    // buffer, for fluid data
    pg_text = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
  
//    drawText(pg_obstacles); // use text as obstacle
    drawText(pg_text);

    // sprite for fluid particles
    img_sprite = loadImage("../data/sprite.png");
    
    createGUI();

    background(0);
    frameRate(60);
  }
  

  
  
  public void drawText(PGraphics pg){
    pg.beginDraw();
    pg.clear();
 
    pg.textFont(font);
    pg.textAlign(CENTER, CENTER);
    
    float px, py, line_height = 150;
    px = viewport_w/2;
    py = viewport_h/2 - line_height;
    
    pg.fill(230,130,0);
//    pg.fill(8);
    pg.text("Processing", px, py);
    
    py += line_height;
    pg.fill(0,130,230);
//    pg.fill(8);
    pg.text("Fluid", px, py);
    
    py += line_height;
    pg.fill(0,130,230);
//    pg.fill(8);
    pg.text("Simulation", px, py);
    
    pg.endDraw();
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
      particle_system.render(pg_fluid, img_sprite, 2);
    }
    
    // display
    image(pg_fluid    , 0, 0);
    image(pg_obstacles, 0, 0);
//    image(pg_text     , 0, 0);

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
    if(key == 's') saveScreenshot();
  }
  
  public void saveScreenshot(){
    DwFrameCapture capture = new DwFrameCapture(this, "examples/");
    File file = capture.createFilename();
    save(file.getAbsolutePath());
    System.out.println(file);
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
    PApplet.main(new String[] { Fluid_LiquidText.class.getName() });
  }
}