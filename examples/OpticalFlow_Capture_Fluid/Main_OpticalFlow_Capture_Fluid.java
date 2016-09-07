/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package OpticalFlow_Capture_Fluid;



import com.thomasdiewald.pixelflow.java.Fluid;
import com.thomasdiewald.pixelflow.java.OpticalFlow;
import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.filter.Filter;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.Numberbox;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;


public class Main_OpticalFlow_Capture_Fluid extends PApplet {
 
  
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
          radius = 20;
          fluid.addVelocity(px, py, radius, vx, vy);
        }
        if(mouseButton == CENTER){
          radius = 50;
          fluid.addDensity (px, py, radius, 1.0f, 0.0f, 0.40f, 1f, 1);
        }
        if(mouseButton == RIGHT){
          radius = 15;
          fluid.addTemperature(px, py, radius, 15f);
        }
        
      }
      
      
//      px = view_w/2;
//      py = 50;
//      radius = 50;
//      fluid.addDensity (px, py, radius, 1.0f, 0.0f, 0.40f, 1f, 1);
//      fluid.addTemperature(px, py, radius, 5f);
  
      // use the text as input for density
      if(ADD_DENSITY_MODE == 0) addDensityTexture (fluid, opticalflow);
      if(ADD_DENSITY_MODE == 1) addDensityTexture_cam(fluid, opticalflow);
      
      addTemperatureTexture(fluid, opticalflow);
      addVelocityTexture(fluid, opticalflow);
      
    }
    
    // custom shader, to add density from a texture (PGraphics2D) to the fluid.
    public void addDensityTexture(Fluid fluid, OpticalFlow opticalflow){
      context.begin();
      context.beginDraw(fluid.tex_density.dst);
      DwGLSLProgram shader = context.createShader("data/addDensity.frag");
      shader.begin();
      shader.uniform2f     ("wh"             , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode"     , 6);    
      shader.uniform1f     ("multiplier"     , 3);    
      shader.uniform1f     ("mix_value"      , 0.1f);
      shader.uniformTexture("tex_opticalflow", opticalflow.frameCurr.velocity);
      shader.uniformTexture("tex_density_old", fluid.tex_density.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end("app.addDensityTexture");
      fluid.tex_density.swap();
    }
    
 
    public void addDensityTexture_cam(Fluid fluid, OpticalFlow opticalflow){
      int[] pg_tex_handle = new int[1];
      
      if( !pg_cam_a.getTexture().available() ) return;
      
      float mix = opticalflow.UPDATE_STEP > 1 ? 0.01f : 1.0f;
      
      context.begin();
      context.getGLTextureHandle(pg_cam_a, pg_tex_handle);
      context.beginDraw(fluid.tex_density.dst);
      DwGLSLProgram shader = context.createShader("data/addDensityCam.frag");
      shader.begin();
      shader.uniform2f     ("wh"        , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode", 6);   
      shader.uniform1f     ("mix_value" , mix);     
      shader.uniform1f     ("multiplier", 1f);     
//      shader.uniformTexture("tex_ext"   , opticalflow.tex_frames.src);
      shader.uniformTexture("tex_ext"   , pg_tex_handle[0]);
      shader.uniformTexture("tex_src"   , fluid.tex_density.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end("app.addDensityTexture");
      fluid.tex_density.swap();
    }
    
    
    
    // custom shader, to add temperature from a texture (PGraphics2D) to the fluid.
    public void addTemperatureTexture(Fluid fluid, OpticalFlow opticalflow){

      context.begin();
      context.beginDraw(fluid.tex_temperature.dst);
      DwGLSLProgram shader = context.createShader("data/addTemperature.frag");
      shader.begin();
      shader.uniform2f     ("wh"        , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode", 1);   
      shader.uniform1f     ("mix_value" , 0.1f);     
      shader.uniform1f     ("multiplier", 0.05f);     
      shader.uniformTexture("tex_ext"   , opticalflow.frameCurr.velocity);
      shader.uniformTexture("tex_src"   , fluid.tex_temperature.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end("app.addTemperatureTexture");
      fluid.tex_temperature.swap();
    }
    
    // custom shader, to add density from a texture (PGraphics2D) to the fluid.
    public void addVelocityTexture(Fluid fluid, OpticalFlow opticalflow){
      context.begin();
      context.beginDraw(fluid.tex_velocity.dst);
      DwGLSLProgram shader = context.createShader("data/addVelocity.frag");
      shader.begin();
      shader.uniform2f     ("wh"             , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode"     , 2);    
      shader.uniform1f     ("multiplier"     , 1.0f);   
      shader.uniform1f     ("mix_value"      , 0.1f);
      shader.uniformTexture("tex_opticalflow", opticalflow.frameCurr.velocity);
      shader.uniformTexture("tex_velocity_old", fluid.tex_velocity.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end("app.addDensityTexture");
      fluid.tex_velocity.swap();
    }
 
  }
  
  
  
  
  
  int cam_w = 640;
  int cam_h = 480;
  
  int view_w = 1200;
  int view_h = (int)(view_w * cam_h/(float)cam_w);
  
  int gui_w = 200;
  
  int fluidgrid_scale = 1;
  
  
  // main library context
  PixelFlow context;
  
  // collection of imageprocessing filters
  Filter filter;
  
  // fluid solver
  Fluid fluid;
  
  MyFluidData cb_fluid_data;
  
  // optical flow
  OpticalFlow opticalflow;
  
  // buffer for the capture-image
  PGraphics2D pg_cam_a, pg_cam_b; 

  // offscreen render-target for fluid
  PGraphics2D pg_fluid;
  
  // camera capture (video library)
  Capture cam;
  

  public boolean DISPLAY_CAM       = !true;
  public boolean DISPLAY_GRAYSCALE = true;
  public int     ADD_DENSITY_MODE  = 1;
  public int     BACKGROUND_COLOR  = 0;
  
  // some state variables for the GUI
  boolean APPLY_EDGE_FILTER = true;
  int NUM_LINES = 10;
  
  
  public void settings() {
    size(view_w + gui_w, view_h, P2D);
    smooth(4);
  }

  public void setup() {
    

    // main library context
    context = new PixelFlow(this);
    context.print();
    context.printGL();
    
    filter = new Filter(context);
    
    // fluid solver
    fluid = new Fluid(context, view_w, view_h, fluidgrid_scale);
    
    // some fluid parameters
    fluid.param.dissipation_density     = 0.90f;
    fluid.param.dissipation_velocity    = 0.80f;
    fluid.param.dissipation_temperature = 0.70f;
    fluid.param.vorticity               = 0.30f;

    // calback for adding fluid data
    cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);
    
    // OF
    opticalflow = new OpticalFlow(context, cam_w, cam_h);
    
    // optical flow parameters    
    opticalflow.param.display_mode = 3;

    
    
    
//    String[] cameras = Capture.list();
//    printArray(cameras);
//    cam = new Capture(this, cameras[0]);
    
    cam = new Capture(this, cam_w, cam_h, 30);
    cam.start();
    
    pg_cam_a = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_cam_a.noSmooth();
    
    pg_cam_b = (PGraphics2D) createGraphics(cam_w, cam_h, P2D);
    pg_cam_b.noSmooth();
    
    pg_fluid = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg_fluid.smooth(4);

  
    createGUI();
    
    background(0);
    frameRate(60);
  }
  

  

  public void draw() {
    
    if( cam.available() ){
      cam.read();
      
      // render to offscreenbuffer
      pg_cam_b.beginDraw();
      pg_cam_b.image(cam, 0, 0);
      pg_cam_b.endDraw();
      swapCamBuffer(); // "pg_cam_a" has the image now
      
      filter.bilateral.apply(pg_cam_a, pg_cam_b, 5, 0.10f, 4);
      swapCamBuffer();
      
      // update Optical Flow
      opticalflow.update(pg_cam_a);
      
      if(DISPLAY_GRAYSCALE){
        // make the capture image grayscale (for better contrast)
        filter.luminance.apply(pg_cam_a, pg_cam_b); swapCamBuffer(); 
      }
    }
    

    if(UPDATE_FLUID){
      fluid.update();
    }
    

    // render everything
    pg_fluid.beginDraw();
    pg_fluid.background(BACKGROUND_COLOR);
    if(DISPLAY_CAM && ADD_DENSITY_MODE == 0){
      pg_fluid.image(pg_cam_a, 0, 0, view_w, view_h);
    }
    pg_fluid.endDraw();
    
    // add fluid stuff to rendering
    if(DISPLAY_FLUID_TEXTURES){
      fluid.renderFluidTextures(pg_fluid, DISPLAY_fluid_texture_mode);
    }
    
    if(DISPLAY_FLUID_VECTORS){
      fluid.renderFluidVectors(pg_fluid, 10);
    }
    
    // add optical flow stuff to rendering
    if(opticalflow.param.display_mode == 2){
      opticalflow.renderVelocityShading(pg_fluid);
    }
    opticalflow.renderVelocityStreams(pg_fluid, NUM_LINES);


    // display result
    background(0);
    image(pg_fluid, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", cam_w, cam_h, opticalflow.UPDATE_STEP, frameRate);
    surface.setTitle(txt_fps);
    
  }
  
  
  void swapCamBuffer(){
    PGraphics2D tmp = pg_cam_a;
    pg_cam_a = pg_cam_b;
    pg_cam_b = tmp;
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
  public void setAddDensityMode(int val){
    ADD_DENSITY_MODE = val;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    
    
    int px, py, oy;
    int sx = 100, sy = 14;
    
    px = 10;
    py = sy;
    oy = (int)(sy * 1.5);
    
    Group group_fluid = cp5.addGroup("fluid controls")
//      .setPosition(20, 40)
      .setHeight(20).setWidth(180)
      .setBackgroundHeight(400)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_fluid.getCaptionLabel().align(LEFT, CENTER);
    
      cp5.addButton("reset").setGroup(group_fluid).plugTo(this, "fluid_reset").setWidth(75).setPosition(px, py);
      cp5.addButton("+"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeUp").setWidth(25).setPosition(px+=85, py);
      cp5.addButton("-"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeDown").setWidth(25).setPosition(px+=30, py);
      
      px = 10;
      
      cp5.addSlider("velocity").setGroup(group_fluid).setSize(sx, sy).setPosition(px, py+=oy *2)
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
      
      RadioButton rb_setDisplayMode = cp5.addRadio("setDisplayMode").setGroup(group_fluid).setSize(80,18).setPosition(px, py+=oy)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(2)
          .addItem("Density"    ,0)
          .addItem("Temperature",1)
          .addItem("Pressure"   ,2)
          .addItem("Velocity"   ,3)
          .activate(0);
      for(Toggle toggle : rb_setDisplayMode.getItems()) toggle.getCaptionLabel().alignX(CENTER);
      
      cp5.addRadio("setDisplayVelocityVectors").setGroup(group_fluid).setPosition(px, py+=oy)
          .setPosition(10, 255).setSize(18,18)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("Velocity Vectors",0)
          ;
      
      Numberbox bg = cp5.addNumberbox("BACKGROUND_COLOR").setGroup(group_fluid).setSize(80,sy).setPosition(px, py+=(int)(oy*3.5f))
      .setMin(0).setMax(255).setScrollSensitivity(1) .setValue(BACKGROUND_COLOR);
      bg.getCaptionLabel().align(LEFT, CENTER).getStyle().setMarginLeft(85);
      
      
      
      Toggle cam = cp5.addToggle("display cam").setGroup(group_fluid).setSize(80, sy).setPosition(px, py+=oy)
      .plugTo(this, "DISPLAY_CAM").setValue(DISPLAY_CAM).linebreak();
      cam.getCaptionLabel().align(CENTER, CENTER);
      
      Toggle grayscale = cp5.addToggle("grayscale").setGroup(group_fluid).setSize(80, sy).setPosition(px + 85, py)
      .plugTo(this, "DISPLAY_GRAYSCALE").setValue(DISPLAY_GRAYSCALE).linebreak(); 
      grayscale.getCaptionLabel().align(CENTER, CENTER);

     
      cp5.addRadio("setAddDensityMode").setGroup(group_fluid).setSize(18,18).setPosition(px, py+=(int)(oy*1.5f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("color"    ,0)
          .addItem("camera"   ,1)
          .activate(ADD_DENSITY_MODE);
      
      
      
      group_fluid.close();
      
      
      
      
      
      
 
      
      py = 10;
      
      
      Group group_oflow = cp5.addGroup("OpticalFlow")
          .setPosition(view_w, 20).setHeight(20).setWidth(gui_w)
          .setBackgroundHeight(view_h).setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
          group_oflow.getCaptionLabel().align(LEFT, CENTER);
          
          cp5.addSlider("blur input").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 30).setValue(opticalflow.param.blur_input)
          .plugTo(opticalflow.param, "blur_input").linebreak();
          
          cp5.addSlider("blur flow").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 10).setValue(opticalflow.param.blur_flow)
          .plugTo(opticalflow.param, "blur_flow").linebreak();
          
          cp5.addSlider("temporal smooth").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(opticalflow.param.temporal_smoothing)
          .plugTo(opticalflow.param, "temporal_smoothing").linebreak();
          
          cp5.addSlider("flow scale").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 200f).setValue(opticalflow.param.flow_scale)
          .plugTo(opticalflow.param, "flow_scale").linebreak();

          cp5.addSlider("threshold").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 60.0f).setValue(opticalflow.param.threshold)
          .plugTo(opticalflow.param, "threshold").linebreak();
          
          cp5.addRadio("setDisplayModeOpticalFlow").setGroup(group_oflow).setSize(18, 18).setPosition(px, py+=(int)(oy*1.5f))
              .setSpacingColumn(40).setSpacingRow(2).setItemsPerRow(3)
              .addItem("dir", 0)
              .addItem("normal", 1)
              .addItem("Shading", 2)
              .activate(opticalflow.param.display_mode);

          group_oflow.open();
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      Accordion accordion = cp5.addAccordion("acc")
          .setPosition(view_w, 0)
          .setWidth(gui_w)
          .addItem(group_fluid)
          .addItem(group_oflow)
          ;

      accordion.setCollapseMode(Accordion.MULTI);
      accordion.open(0);
      accordion.open(1);
  }
  
  public void setDisplayModeOpticalFlow(int val){
    opticalflow.param.display_mode = val;
  }

  
  
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { Main_OpticalFlow_Capture_Fluid.class.getName() });
  }
}