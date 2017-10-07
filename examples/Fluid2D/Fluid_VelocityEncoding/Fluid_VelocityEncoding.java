/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package Fluid2D.Fluid_VelocityEncoding;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;

import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;


public class Fluid_VelocityEncoding extends PApplet {
  
  // A more abstract example that shows how to add velocity to the fluid-solver
  // by encoding it in a PGraphics.
  //
  // Similar to the density-examples (Fluid_LiquidPainting), where the density
  // source comes from an image, it is possible to add velocity from a texture.
  //
  // The challenge is to interpret + encode the PGraphics drawing as a 
  // 2d-direction (velocity-vector).
  //
  // As a result we can draw to a PGraphics canvas and use that drawing as any
  // kind of source, density, temperature, velocity.
  //
  //
  // controls:
  //
  // LMB: add Velocity
  // RMB: add Density


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
          radius = 20;
          fluid.addVelocity(px, py, radius, vx, vy);
        }
        if(mouseButton == CENTER){
          radius = 25;
          fluid.addDensity (px, py, radius, 1.0f, 0.0f, 0.40f, 1f, 1);
        }
      }
  
      // use the text as input for density
      float mix_density  = fluid.simulation_step == 0 ? 1.0f : 0.1f;
      float mix_velocity = fluid.simulation_step == 0 ? 1.0f : 0.5f;
      addDensityTexture (fluid, pg_density , mix_density);
      addVelocityTexture(fluid, pg_velocity, mix_velocity);
    }
    
    
    // custom shader, to add velocity from a texture (PGraphics2D) to the fluid.
    public void addVelocityTexture(DwFluid2D fluid, PGraphics2D pg, float mix){
      int[] pg_tex_handle = new int[1]; 
//    pg_tex_handle[0] = pg.getTexture().glName
      context.begin();
      context.getGLTextureHandle(pg, pg_tex_handle);
      context.beginDraw(fluid.tex_velocity.dst);
      DwGLSLProgram shader = context.createShader("data/addVelocity.frag");
      shader.begin();
      shader.uniform2f     ("wh"        , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode", 7);   
      shader.uniform1f     ("mix_value" , 0.015f);     
      shader.uniform1f     ("multiplier", 1);     
      shader.uniformTexture("tex_ext"   , pg_tex_handle[0]);
      shader.uniformTexture("tex_src"   , fluid.tex_velocity.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end();
      fluid.tex_velocity.swap();
    }
    
    // custom shader, to add density from a texture (PGraphics2D) to the fluid.
    public void addDensityTexture(DwFluid2D fluid, PGraphics2D pg, float mix){
      int[] pg_tex_handle = new int[1]; 
//      pg_tex_handle[0] = pg.getTexture().glName
      context.begin();
      context.getGLTextureHandle(pg, pg_tex_handle);
      context.beginDraw(fluid.tex_density.dst);
      DwGLSLProgram shader = context.createShader("data/addDensity.frag");
      shader.begin();
      shader.uniform2f     ("wh"        , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode", 6);   
      shader.uniform1f     ("mix_value" , mix);     
      shader.uniform1f     ("multiplier", 1);     
      shader.uniformTexture("tex_ext"   , pg_tex_handle[0]);
      shader.uniformTexture("tex_src"   , fluid.tex_density.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end("app.addDensityTexture");
      fluid.tex_density.swap();
    }
 
  }
  
  
  int viewport_w = 800;
  int viewport_h = 800;
  int fluidgrid_scale = 1;
  
  int BACKGROUND_COLOR = 255;
  
  public DwPixelFlow context;
  public DwFluid2D fluid;
  MyFluidData cb_fluid_data;

  PGraphics2D pg_fluid;       // render target
  PGraphics2D pg_density;     // texture-buffer, for adding fluid data
  PGraphics2D pg_velocity;    // texture-buffer, for adding fluid data

  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  
  }
  

  public void setup() {
    
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // fluid simulation
    fluid = new DwFluid2D(context, viewport_w, viewport_h, fluidgrid_scale);
    
    // some fluid parameters
    fluid.param.dissipation_density     = 0.95f;
    fluid.param.dissipation_velocity    = 0.80f;
    fluid.param.dissipation_temperature = 0.70f;
    fluid.param.vorticity               = 0.10f;
    
    // interface for adding data to the fluid simulation
    cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);


    // fluid render target
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(4);


    // image/buffer that will be used as density input
    pg_density = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_density.noSmooth();
    pg_density.beginDraw();
    pg_density.clear();
    pg_density.endDraw();
    
    // image/buffer that will be used as density input
    pg_velocity = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_velocity.noSmooth();
    pg_velocity.beginDraw();
    pg_velocity.clear();
    pg_velocity.endDraw();
    

    createGUI();

    frameRate(60);
  }
  
  
  int argb;
  float mx = 0, mx_prev = 0, vx= 0;
  float my = 0, my_prev = 0, vy= 0;
  
  
  public void updateVelocity(){
    float speed = 10;
    float smooth = 0.025f;
    
    mx_prev = mx;
    my_prev = my;
    
    mx += (mouseX - mx) * smooth;
    my += (mouseY - my) * smooth;
    
    vx = +(mx - mx_prev) * speed;
    vy = -(my - my_prev) * speed;  
  }
  
  
  
  public void drawVelocity(PGraphics pg, int texture_type){
    argb = Velocity.Polar.encode_vX_vY(vx, vy);
    float[] vam = Velocity.Polar.getArc(vx, vy);
    
    float vA = vam[0];
    float vM = vam[1];
    
    if(vM == 0){
      return;
    }
    
    pg.beginDraw();
    pg.blendMode(REPLACE); // important
    pg.clear();
    pg.noStroke();
    
    if(vM > 0){
  
      if(texture_type == 1){
        
        float w = width;
        float h = height;
        
        pg.stroke(255,102,0);
        pg.strokeWeight(1);
        
        int count = 10;
        float sx = (w-1)/(float)(count-1);
        float sy = (h-1)/(float)(count-1);
        for(int i = 0; i < count; i++){
          pg.line(i*sx, 0, i*sx, h);
          pg.line(0, i*sy, w, i*sy);
        }
        
//        pg.line(w/2, 0, w/2, h);
//        pg.line(0, h/2, w, h/2);
        
        pg.fill  (64);
        pg.stroke(64);
        pg.fill  (0, 150, 255);
        pg.stroke(0, 150, 255);
      }
      
      if(texture_type == 0){
        // if the the M bits are zero (no magnitude), then processings fill() method 
        // builds a different color than zero: 0x00000000 becomes 0xFF000000
        // this fucks up the encoding/decoding process in the shader.
        // (argb & 0xFFFF0000) == 0
        // pg.fill(argb); // this fails if argb == 0
        
        // so, a workaround is, to pass 4 components separately
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b = (argb >>  0) & 0xFF;
        pg.fill  (r,g,b,a);   
        pg.stroke(r,g,b,a);
      }
      
      pg.rectMode(CENTER);
      pg.pushMatrix();
      pg.translate(mx, my);
      pg.rotate(-vA);
      
      pg.strokeWeight(15);
      pg.noFill();
      
      pg.ellipse(0, 0, 150, 150 - vM);

      pg.popMatrix();
    }
    
    pg.endDraw();
  }
  
  
  

  public void draw() {
   
    if(UPDATE_FLUID){
      
      updateVelocity();
      drawVelocity(pg_velocity, 0);
      drawVelocity(pg_density, 1);
      
      fluid.update();
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
    }
    
    // display
    image(pg_fluid, 0, 0);

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
    .setPosition(0, 20).setHeight(20).setWidth(180)
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
        .addItem("Velocity Vectors",0)
        ;

    cp5.addNumberbox("BACKGROUND_COLOR").setGroup(group_fluid)
    .setPosition(10,310).setSize(80,18)
    .setMin(0).setMax(255)
    .setScrollSensitivity(1) .setValue(BACKGROUND_COLOR);
    
    group_fluid.close();
  }
  
  
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Fluid_VelocityEncoding.class.getName() });
  }
}