/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package FirstBlood;



import com.thomasdiewald.pixelflow.java.Fluid;
import com.thomasdiewald.pixelflow.java.ParticleSystem;
import com.thomasdiewald.pixelflow.java.PixelFlow;

import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;

public class Main_FirstBlood extends PApplet {
  
  private class MyFluidData implements Fluid.FluidData{
    
    // update() is called during the fluid-simulation update step.
    @Override
    public void update(Fluid fluid) {
    
      float px, py, vx, vy, radius, vscale, r, g, b, intensity, temperature;
      
      boolean mouse_input = !cp5.isMouseOver() && mousePressed;
 
      if(mouse_input){
        
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        intensity = 2.0f;
        temperature = 5f;
        
        // add impulse: density + velocity
        if(mouseButton == LEFT){
          radius = 10;
          fluid.addDensity(px, py, radius, 1,0,0,intensity);
          radius = 3;
          fluid.addDensity(px, py, radius, 1,0.8f,0.6f,intensity);
          radius = 15;
          fluid.addVelocity(px, py, radius, vx, vy);
        }
        
        // add impulse: velocity
        if(mouseButton == CENTER){
          radius = 10;
          fluid.addVelocity(px, py, radius, vx, vy);
        }
        
        // add impulse: density + temperature
        if(mouseButton == RIGHT){
          radius = 15;
          fluid.addDensity(px, py, radius, 0.9f,0.9f,0.9f,intensity);
          radius = 10;
          fluid.addTemperature(px, py, radius, temperature);
        }
      }
     
    }
  }
  
  
  
  int viewport_w = 600;
  int viewport_h = 600;
  int fluidgrid_scale = 1;
  
  int BACKGROUND_COLOR = 255;

  
  // fluid simulation
  public Fluid fluid;
  
  // particle system
  ParticleSystem particle_system;

  // render targets
  PGraphics2D pg_fluid;
  
  //texture-buffer, for adding obstacles
  PGraphics2D pg_obstacles;

  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  }
  
  public void setup() {
    
    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
    context.printGL();

    // fluid simulation
    fluid = new Fluid(context, viewport_w, viewport_h, fluidgrid_scale);

    // particle
    particle_system = new ParticleSystem(context, viewport_w/3, viewport_h/3);
    
    // set some simulation parameters
    fluid.param.dissipation_density     = 0.99f;
    fluid.param.dissipation_velocity    = 0.51f;
    fluid.param.dissipation_temperature = 0.50f;
    fluid.param.vorticity               = 0.00f;
    
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
    // some rectangle
//    pg_obstacles.rectMode(CENTER);
//    pg_obstacles.translate(width/2, height/2f);
//    pg_obstacles.rotate(PI*0.1f);
//    pg_obstacles.stroke(192);
//    pg_obstacles.strokeWeight(10);
//    pg_obstacles.noFill();
//    pg_obstacles.rect(0, 0, 200, 200);
    // border-obstacle
    pg_obstacles.resetMatrix();
    pg_obstacles.rectMode(CORNER);
    pg_obstacles.strokeWeight(20);
    pg_obstacles.stroke(192);
    pg_obstacles.noFill();
    pg_obstacles.rect(0, 0, pg_obstacles.width, pg_obstacles.height);
    pg_obstacles.endDraw();
    
    createGUI();
    
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
      // render: density (0), temperature (1), pressure (2), velocity (3)
      fluid.renderFluidTextures(pg_fluid, DISPLAY_fluid_texture_mode);
    }
    
    if(DISPLAY_FLUID_VECTORS){
      // render: velocity vector field
      fluid.renderFluidVectors(pg_fluid, 10);
    }
    
    if(DISPLAY_PARTICLES){
      // render: particles; 0 ... points, 1 ...sprite texture, 2 ... dynamic points
      particle_system.render(pg_fluid, null, 2);
    }
    

    // display
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
    fluid.reset();
    particle_system.reset();
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
    .setPosition(20, 40).setHeight(20).setWidth(180)
    .setBackgroundHeight(350).setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_fluid.getCaptionLabel().align(LEFT, CENTER);
  
    Button breset = cp5.addButton("reset").setGroup(group_fluid).plugTo(this, "fluid_reset").setWidth(75);
    Button bplus  = cp5.addButton("+"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeUp").setWidth(25);
    Button bminus = cp5.addButton("-"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeDown").setWidth(25).linebreak();
    
    float[] pxy = breset.getPosition();
    bplus .setPosition(pxy[0] + 75 + 10, pxy[1]);
    bminus.setPosition(pxy[0] + 75 + 25 + 20, pxy[1]);
    
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
        .addItem("Velocity Vectors",0);

    cp5.addRadio("setDisplayParticles").setGroup(group_fluid)
        .setPosition(10, 280).setSize(18,18)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
        .addItem("Particles",0);

    cp5.addNumberbox("BACKGROUND_COLOR").setGroup(group_fluid)
    .setPosition(10,310).setSize(80,18)
    .setMin(0).setMax(255)
    .setScrollSensitivity(1) .setValue(BACKGROUND_COLOR);
    
    group_fluid.close();
  }
  
  
  
  
 
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_FirstBlood.class.getName() });
  }
}