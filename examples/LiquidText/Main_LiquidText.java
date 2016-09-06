/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package LiquidText;



import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;
import src.Fluid;
import src.ParticleSystem;
import src.PixelFlow;
import src.dwgl.DwGLSLProgram;


public class Main_LiquidText extends PApplet {
  


  private class MyFluidData implements Fluid.FluidData{
    
    
    @Override
    // this is called during the fluid-simulation update step.
    public void update(Fluid fluid) {
    
      float px, py, vx, vy, radius, vscale, r, g, b, a;

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
    public void addDensityTexture(Fluid fluid, PGraphics2D pg){
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
    public void addTemperatureTexture(Fluid fluid, PGraphics2D pg){
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
  
  
  int viewport_w = 1200;
  int viewport_h =  900;
  int fluidgrid_scale = 1;
  
  int BACKGROUND_COLOR = 0;
  
  public PixelFlow context;
  public Fluid fluid;
  MyFluidData cb_fluid_data;
  ParticleSystem particle_system;
  

  PGraphics2D pg_fluid;       // render target
  PGraphics2D pg_obstacles;   // texture-buffer, for adding obstacles
  PGraphics2D pg_text;        // texture-buffer, for adding fluid data (density and temperature)
  
  // sprite, can be used for the particle system
  PImage img_sprite;
  
  // processing font
  PFont font;

 
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  }
  

  
  
  
  public void setup() {
    
    // main library context
    context = new PixelFlow(this);
    context.print();
    context.printGL();

    // fluid simulation
    fluid = new Fluid(context, viewport_w, viewport_h, fluidgrid_scale);
    
    // fuild simulation parameters
    fluid.param.dissipation_density     = 0.90f;
    fluid.param.dissipation_velocity    = 0.90f;
    fluid.param.dissipation_temperature = 0.90f;
    
    // interface for adding data to the fluid simulation
    cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);

    // processing font
    font = createFont("SourceCodePro-Regular.ttf", 128);

    // fluid render target
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(4);

    // particles
    particle_system = new ParticleSystem();
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
    img_sprite = loadImage("sprite.png");
    
    createGUI();

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
    particle_system.reset();
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
    
    RadioButton rb_setDisplayVelocityVectors = cp5.addRadio("setDisplayVelocityVectors").setGroup(group_fluid)
        .setPosition(10, 255).setSize(18,18)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
        .addItem("Velocity Vectors",0)
//        .activate(0)
        ;

    RadioButton rb_setDisplayParticles = cp5.addRadio("setDisplayParticles").setGroup(group_fluid)
        .setPosition(10, 280).setSize(18,18)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
        .addItem("Particles",0)
//        .activate(0)
        ;

    cp5.addNumberbox("BACKGROUND_COLOR").setGroup(group_fluid)
    .setPosition(10,310).setSize(80,18)
    .setMin(0).setMax(255)
    .setScrollSensitivity(1) .setValue(BACKGROUND_COLOR);
    
    group_fluid.close();
  }
  
  
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_LiquidText.class.getName() });
  }
}