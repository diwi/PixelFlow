/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package Streamlines;



import com.thomasdiewald.pixelflow.java.Fluid;
import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.StreamLines;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.Numberbox;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;




public class Main_Streamlines extends PApplet {
  
  private class MyFluidData implements Fluid.FluidData{
    
    // update() is called during the fluid-simulation update step.
    @Override
    public void update(Fluid fluid) {
    
      float px, py, vx, vy, radius, vscale, r, g, b, intensity, temperature;
      
      // add impulse: density + temperature
      px = width-80;
      py = 30;
      radius = 60;
      r = 1;
      g = 1;
      b = 1;
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
      r = 1;
      g = 1;
      b = 1;
      vx = 0;
      vy = -50;
      fluid.addDensity(px, py, radius, r, g, b, intensity);
      temperature = -4;
      fluid.addTemperature(px, py, radius, temperature);
      

      boolean mouse_input = !cp5.isMouseOver() && mousePressed;
      
      // add impulse: density + velocity
      if(mouse_input && mouseButton == LEFT){
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        radius = 8;
        fluid.addDensity(px, py, radius, 1, 1, 1f, 1.0f);
        radius = 15;
        fluid.addVelocity(px, py, radius, vx, vy);
      }
      
      // add impulse: density + velocity
      if(mouse_input && mouseButton == RIGHT){
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        radius = 30;
        fluid.addDensity(px, py, radius, 0, 0.4f, 1f, 1f, 1);
        radius = 20;
        temperature = 1;
        fluid.addTemperature(px, py, radius, temperature);
      }
     
    }
  }
  
  
  int viewport_w = 800;
  int viewport_h = 800;
  int fluidgrid_scale = 1;
  
  int BACKGROUND_COLOR = 0;
  
  // library
  PixelFlow context;
  
  // Fluid simulation
  public Fluid fluid;
  
  // streamline visualization
  public StreamLines streamlines;

  // render targets
  PGraphics2D pg_fluid;
  //texture-buffer, for adding obstacles
  PGraphics2D pg_obstacles;
  
  
  // Streamline states
  public boolean DISPLAY_STREAMLINES = true;
  public int     STREAMLINE_DENSITY  = 10;

  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  }
  
  public void setup() {
    
    // main library context
    context = new PixelFlow(this);
    context.print();
    context.printGL();
    
    // visualization of the velocity field
    streamlines = new StreamLines(context);
    
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
    
    //
    pg_obstacles.noStroke();
    pg_obstacles.fill(64);
    pg_obstacles.rect(250, 500, 50, 200);
    
    pg_obstacles.noStroke();
    pg_obstacles.fill(64);
    pg_obstacles.rect(400, 100, 300, 10);
    
    pg_obstacles.endDraw();
    

    createGUI();
    
    frameRate(60);
//    frameRate(1000);
  }
  
  
  
  // float buffer for pixel transfer from OpenGL to the host application
  float[] fluid_velocity;

 
  public void draw() {    

    // update simulation
    if(UPDATE_FLUID){
      fluid.addObstacles(pg_obstacles);
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

    if(DISPLAY_STREAMLINES){
      streamlines.render(pg_fluid, fluid, STREAMLINE_DENSITY);
    }

    // RENDER
    // display textures
    image(pg_fluid    , 0, 0);
    image(pg_obstacles, 0, 0);
    

    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", fluid.fluid_w, fluid.fluid_h, fluid.simulation_step, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  boolean UPDATE_FLUID = true;
  
  boolean DISPLAY_FLUID_TEXTURES  = true;
  boolean DISPLAY_FLUID_VECTORS   = !true;
  boolean DISPLAY_PARTICLES       = !true;
  
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
    DISPLAY_FLUID_VECTORS = val == 0;
    DISPLAY_STREAMLINES   = val == 1;
  }
  public void setDisplayParticles(int val){
    DISPLAY_PARTICLES = val != -1;
  }
  
  
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    
    int px, py, oy;
    int sx = 100, sy = 14;
    
    sx = 90;
    px = 10;
    py = 10;
    oy = (int)(sy*1.5f);
    
    
    Group group_fluid = cp5.addGroup("fluid controls")
//    .setPosition(20, 40)
    .setHeight(20).setWidth(180)
    .setBackgroundHeight(430)
    .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_fluid.getCaptionLabel().align(LEFT, CENTER);
  
    cp5.addButton("reset").setGroup(group_fluid).plugTo(this, "fluid_reset").setWidth(75).setPosition(px, py);
    cp5.addButton("+"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeUp").setWidth(25).setPosition(px+=85, py);
    cp5.addButton("-"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeDown").setWidth(25).setPosition(px+=30, py);
    
    px = 10;
     
    cp5.addSlider("velocity").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=(int)(oy*1.5))
    .setRange(0, 1).setValue(fluid.param.dissipation_velocity)
    .plugTo(fluid.param, "dissipation_velocity").linebreak();
    
    cp5.addSlider("density").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1).setValue(fluid.param.dissipation_density)
    .plugTo(fluid.param, "dissipation_density").linebreak();
    
    cp5 .addSlider("temperature").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1).setValue(fluid.param.dissipation_temperature)
    .plugTo(fluid.param, "dissipation_temperature").linebreak();
  
    cp5 .addSlider("vorticity").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1).setValue(fluid.param.vorticity)
    .plugTo(fluid.param, "vorticity").linebreak();
        
    cp5.addSlider("iterations").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 80).setValue(fluid.param.num_jacobi_projection)
    .plugTo(fluid.param, "num_jacobi_projection").linebreak();
          
    cp5.addSlider("timestep").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1).setValue(fluid.param.timestep)
    .plugTo(fluid.param, "timestep").linebreak();
        
    cp5.addSlider("gridscale").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 50).setValue(fluid.param.gridscale)
    .plugTo(fluid.param, "gridscale").linebreak();
    
    RadioButton rb_setDisplayMode = cp5.addRadio("setDisplayMode").setGroup(group_fluid).setSize(80,18).setPosition(px, py+=(int)(oy*1.5))
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(2)
        .addItem("Density"    ,0)
        .addItem("Temperature",1)
        .addItem("Pressure"   ,2)
        .addItem("Velocity"   ,3)
        .activate(0);
    for(Toggle toggle : rb_setDisplayMode.getItems()) toggle.getCaptionLabel().alignX(CENTER);
    
    
    RadioButton rb = cp5.addRadio("setDisplayVelocityVectors").setGroup(group_fluid).setSize(18,18).setPosition(px, py+=(int)(oy*2.5))
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
        .addItem("Velocity Vectors",0)
        .addItem("StreamLines"     ,1)
        ;
    if(DISPLAY_STREAMLINES) rb.activate(1);
    
    
    cp5.addSlider("line density").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=(int)(oy*2.5))
    .setRange(5, 20).setValue(STREAMLINE_DENSITY)
    .plugTo(this, "STREAMLINE_DENSITY").linebreak();
    
    cp5.addSlider("line length").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(5, 300).setValue(streamlines.param.line_length)
    .plugTo(streamlines.param, "line_length").linebreak();
    
    cp5.addSlider("Velocity scale").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(1, 50).setValue(streamlines.param.velocity_scale)
    .plugTo(streamlines.param, "velocity_scale").linebreak();
    
    cp5.addSlider("Velocity min").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(1, 200).setValue(streamlines.param.velocity_min)
    .plugTo(streamlines.param, "velocity_min").linebreak();
    

    Numberbox bg = cp5.addNumberbox("BACKGROUND_COLOR").setGroup(group_fluid).setSize(80,sy).setPosition(px, py+=(int)(oy*1.5f))
    .setMin(0).setMax(255).setScrollSensitivity(1) .setValue(BACKGROUND_COLOR);
    bg.getCaptionLabel().align(LEFT, CENTER).getStyle().setMarginLeft(85);
   
   
    group_fluid.close();
   
    
    Accordion accordion = cp5.addAccordion("acc")
        .setPosition(20,20)
        .setWidth(180)
        .addItem(group_fluid)
        ;

    accordion.setCollapseMode(Accordion.MULTI);
    accordion.open(0);
    
  }
  
  

  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_Streamlines.class.getName() });
  }
}