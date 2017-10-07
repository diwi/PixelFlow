/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package OpticalFlow.OpticalFlow_MovieFluid;



import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Movie;


public class OpticalFlow_MovieFluid extends PApplet {
  
  //
  // This Demo-App combines Optical Flow (based on Movie frames) and Fluid 
  // simulation.
  // The resulting velocity vectors of the Optical Flow are used to change the
  // velocity of the Fluid. The Movie Frames are the source for the Fluid_density.
  // 
  // This effect works best, when the movie background is not chaanging too much,
  // e.g. when the camera is not moving.
  // 
  
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
          radius = 50;
          fluid.addDensity (px, py, radius, 1.0f, 0.0f, 0.40f, 1f, 1);
        }
        if(mouseButton == RIGHT){
          radius = 15;
          fluid.addTemperature(px, py, radius, 15f);
        }
        
      }

      addDensityTexture_cam(fluid, opticalflow);
      addVelocityTexture   (fluid, opticalflow);
//      addTemperatureTexture(fluid, opticalflow);
    }
    

    public void addDensityTexture_cam(DwFluid2D fluid, DwOpticalFlow opticalflow){
      int[] pg_tex_handle = new int[1];
      
      if( !pg_movie.getTexture().available() ) {
        System.out.println("no tex");
        return;
      }
      
      float mix = opticalflow.UPDATE_STEP > 1 ? 0.05f : 1.0f;
      
      context.begin();
      context.getGLTextureHandle(pg_movie, pg_tex_handle);
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
    public void addTemperatureTexture(DwFluid2D fluid, DwOpticalFlow opticalflow){
      context.begin();
      context.beginDraw(fluid.tex_temperature.dst);
      DwGLSLProgram shader = context.createShader("examples/OpticalFlow_MovieFluid/data/addTemperature.frag");
      shader.begin();
      shader.uniform2f     ("wh"        , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode", 1);   
      shader.uniform1f     ("mix_value" , 0.1f);     
      shader.uniform1f     ("multiplier", 0.01f);     
      shader.uniformTexture("tex_ext"   , opticalflow.frameCurr.velocity);
      shader.uniformTexture("tex_src"   , fluid.tex_temperature.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end("app.addTemperatureTexture");
      fluid.tex_temperature.swap();
    }
    
    
    // custom shader, to add density from a texture (PGraphics2D) to the fluid.
    public void addVelocityTexture(DwFluid2D fluid, DwOpticalFlow opticalflow){
      context.begin();
      context.beginDraw(fluid.tex_velocity.dst);
      DwGLSLProgram shader = context.createShader("data/addVelocity.frag");
      shader.begin();
      shader.uniform2f     ("wh"             , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode"     , 2);    
      shader.uniform1f     ("multiplier"     , 0.5f);   
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
  
 
 
 
 

 
  // dimensions
  int view_w = 1280;
  int view_h = 720;
  int view_x = 230;
  int view_y = 0;
   
  int gui_w = 200;
  int gui_x = view_w - gui_w;
  int gui_y = 0;
  
  int pg_movie_w = view_w - gui_w;
  int pg_movie_h = view_h;
  

  //main library context
  DwPixelFlow context;
  
  // optical flow
  DwOpticalFlow opticalflow;
  
  
  // fluid stuff
  int fluidgrid_scale = 1;
  DwFluid2D fluid;

  
  // render targets
  PGraphics2D pg_movie; 
  PGraphics2D pg_temp; 
  PGraphics2D pg_oflow;
  
  // Movie
  Movie movie;
  TimeLine timeline;
  PFont font;


  // some state variables for the GUI/display
  int     BACKGROUND_COLOR = 0;
  boolean DISPLAY_SOURCE   = true;
  boolean APPLY_GRAYSCALE  = false;
  boolean APPLY_BILATERAL  = true;
  int     VELOCITY_LINES   = 6;
  
  boolean UPDATE_FLUID = true;
  
  boolean DISPLAY_FLUID_TEXTURES  = true;
  boolean DISPLAY_FLUID_VECTORS   = !true;
  boolean DISPLAY_PARTICLES       = !true;
  
  int     DISPLAY_fluid_texture_mode = 0;
  

  
  
  public void settings() {
    size(view_w, view_h, P2D);
    smooth(8);
  }

  public void setup() {
    
    surface.setLocation(view_x, view_y);
    
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
      
    // optical flow object
    opticalflow = new DwOpticalFlow(context, pg_movie_w, pg_movie_h);
    opticalflow.param.display_mode = 1;
    
    // fluid object
    fluid = new DwFluid2D(context, pg_movie_w, pg_movie_h, fluidgrid_scale);
    // initial fluid parameters
    fluid.param.dissipation_density     = 0.95f;
    fluid.param.dissipation_velocity    = 0.90f;
    fluid.param.dissipation_temperature = 0.70f;
    fluid.param.vorticity               = 0.30f;
    // callback for adding fluid data
    fluid.addCallback_FluiData(new MyFluidData());
   
    // init render targets
    pg_movie = (PGraphics2D) createGraphics(pg_movie_w, pg_movie_h, P2D);
    pg_movie.smooth(0);
    pg_movie.beginDraw();
    pg_movie.background(0);
    pg_movie.endDraw();
    
    pg_temp = (PGraphics2D) createGraphics(pg_movie_w, pg_movie_h, P2D);
    pg_temp.smooth(0);
    
    pg_oflow = (PGraphics2D) createGraphics(pg_movie_w, pg_movie_h, P2D);
    pg_oflow.smooth(0);
    
    
    // movie file is not contained in the library release
    // to keep the file size small. please use one of your own videos instead.
    movie = new Movie(this, "examples/data/Pulp_Fiction_Dance_Scene.mp4");
    movie.loop();
    
    // font for timeline
    font = createFont("../data/SourceCodePro-Regular.ttf", 12);

    timeline = new TimeLine(movie, 0, height-20, pg_movie_w, 20);

    createGUI();
    
    background(0);
    frameRate(60);
  }
  

  

  

  public void draw() {
    
    if( movie.available() ){
      movie.read();
      
      // compute movie display size to fit the best
      int movie_w = movie.width;
      int movie_h = movie.height;
      
      float mov_w_fit = pg_movie_w;
      float mov_h_fit = (pg_movie_w/(float)movie_w) * movie_h;
      
      if(mov_h_fit > pg_movie_h){
        mov_h_fit = pg_movie_h;
        mov_w_fit = (pg_movie_h/(float)movie_h) * movie_w;
      }
      
      // render to offscreenbuffer
      pg_movie.beginDraw();
      pg_movie.background(0);
      pg_movie.imageMode(CENTER);
      pg_movie.pushMatrix();
      pg_movie.translate(pg_movie_w/2f, pg_movie_h/2f);
      pg_movie.scale(0.95f);
      pg_movie.image(movie, 0, 0, mov_w_fit, mov_h_fit);
      pg_movie.popMatrix();
      pg_movie.endDraw();
      
      // apply filters (not necessary)
      if(APPLY_GRAYSCALE){
        DwFilter.get(context).luminance.apply(pg_movie, pg_movie);
      }
      if(APPLY_BILATERAL){
        DwFilter.get(context).bilateral.apply(pg_movie, pg_temp, 5, 0.10f, 4);
        swapCamBuffer();
      }
      
      // update Optical Flow
      opticalflow.update(pg_movie);
    }
    
    if(UPDATE_FLUID){
      fluid.update();
    }
  
    // render Optical Flow
    pg_oflow.beginDraw();
    pg_oflow.background(BACKGROUND_COLOR);
    if(DISPLAY_SOURCE){
      pg_oflow.image(pg_movie, 0, 0);
    }
    pg_oflow.endDraw();
    
    // add fluid stuff to rendering
    if(DISPLAY_FLUID_TEXTURES){
      fluid.renderFluidTextures(pg_oflow, DISPLAY_fluid_texture_mode);
    }
    
    if(DISPLAY_FLUID_VECTORS){
      fluid.renderFluidVectors(pg_oflow, 10);
    }
    
    // add flow-vectors to the image
    if(opticalflow.param.display_mode == 2){
      opticalflow.renderVelocityShading(pg_oflow);
    }
    opticalflow.renderVelocityStreams(pg_oflow, VELOCITY_LINES);
    
    // display result
    background(0);
    image(pg_oflow, 0, 0);
    
    timeline.draw(mouseX, mouseY);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", view_w, view_h, opticalflow.UPDATE_STEP, frameRate);
    surface.setTitle(txt_fps);
   
  }
  
  

  void swapCamBuffer(){
    PGraphics2D tmp = pg_movie;
    pg_movie = pg_temp;
    pg_temp = tmp;
  }
  
  

  
  
  
  

  // Movie Timeline
  class TimeLine{
    float x, y, w, h;
    Movie movie;
    
    public TimeLine(Movie movie, float x, float y, float w, float h){
      this.movie = movie;
      setPosition(x,y,w,h);
    }
    
    public void setPosition(float x, float y, float w, float h){
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }
    
    public boolean inside(float mx, float my){
      return mx >= x && mx <= (x+w) && my >= y && my <= (y+h);
    }
    
    public void jumpToMoviePos(){
      if(inside(mouseX, mouseY)){
        float movie_pos = map(mouseX-x, 0, w, 0, movie.duration());
        movie.jump(movie_pos);
      }
    }
    
    public void draw(float mx, float my){
      float time      = movie.time();
      float duration  = movie.duration();
      float movie_pos = w * time / duration;
      String time_str = String.format(Locale.ENGLISH, "%1.2f", time);
      
      // timeline
      fill(64, 200);
      noStroke();
      rect(x, y, w, h);
      
      // time handle
      fill(200, 200);
      rect(x+movie_pos-25, y, 50, 20, 8);
      
      // time, as text in seconds
      fill(0);
      textFont(font);
      textAlign(CENTER, CENTER);
      text(time_str, x + movie_pos, y + h/2 - 2);
      
      if(inside(mx, my)){
        float hoover_pos = duration * (mx - x) / w;
        String hoover_str = String.format(Locale.ENGLISH, "%1.2f", hoover_pos);
        
        // time handle
        fill(200, 50);
        rect(mx-25, y, 50, 20, 8);
        
        // time, as text in seconds
        fill(200, 100);
        textFont(font);
        textAlign(CENTER, CENTER);
        text(hoover_str, mx, y + h/2 - 2);
      }  
    }
  }
  
  

  

  
  public void fluid_resizeUp(){
    fluid.resize(width, height, fluidgrid_scale = max(1, --fluidgrid_scale));
  }
  public void fluid_resizeDown(){
    fluid.resize(width, height, ++fluidgrid_scale);
  }
  public void fluid_reset(){
    fluid.reset();
    opticalflow.reset();
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
  public void opticalFlow_setDisplayMode(int val){
    opticalflow.param.display_mode = val;
  }
  public void activeFilters(float[] val){
    APPLY_GRAYSCALE = (val[0] > 0);
    APPLY_BILATERAL = (val[1] > 0);
  }
  public void setOptionsGeneral(float[] val){
    DISPLAY_SOURCE = (val[0] > 0);
  }
 
  
  public void mouseReleased(){
    if(timeline.inside(mouseX, mouseY)){
      timeline.jumpToMoviePos();
      opticalflow.reset();
    }
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
    if(key == 'e') DISPLAY_PARTICLES      = !DISPLAY_PARTICLES;
  }
  


  

  ControlP5 cp5;
  
  public void createGUI(){
    
    cp5 = new ControlP5(this);
    
    int sx, sy, px, py, oy;
    
    sx = 100; sy = 14;
    oy = (int)(sy*1.5f);
    

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
    // GUI - OPTICAL FLOW
    ////////////////////////////////////////////////////////////////////////////
    Group group_oflow = cp5.addGroup("Optical Flow");
    {
      group_oflow.setSize(gui_w, 165).setHeight(20)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_oflow.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      cp5.addSlider("blur input").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py)
        .setRange(0, 30).setValue(opticalflow.param.blur_input).plugTo(opticalflow.param, "blur_input");
      
      cp5.addSlider("blur flow").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
        .setRange(0, 10).setValue(opticalflow.param.blur_flow).plugTo(opticalflow.param, "blur_flow");
      
      cp5.addSlider("temporal smooth").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
        .setRange(0, 1).setValue(opticalflow.param.temporal_smoothing).plugTo(opticalflow.param, "temporal_smoothing");
      
      cp5.addSlider("flow scale").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
        .setRange(0, 200f).setValue(opticalflow.param.flow_scale).plugTo(opticalflow.param, "flow_scale");
  
      cp5.addSlider("threshold").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
        .setRange(0, 3.0f).setValue(opticalflow.param.threshold).plugTo(opticalflow.param, "threshold");
      
      cp5.addRadio("opticalFlow_setDisplayMode").setGroup(group_oflow).setSize(18, 18).setPosition(px, py+=oy)
        .setSpacingColumn(40).setSpacingRow(2).setItemsPerRow(3)
        .addItem("dir"    , 0)
        .addItem("normal" , 1)
        .addItem("Shading", 2)
        .activate(opticalflow.param.display_mode);
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - DISPLAY
    ////////////////////////////////////////////////////////////////////////////
    Group group_display = cp5.addGroup("display");
    {
      group_display.setHeight(20).setSize(gui_w, 125)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_display.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      cp5.addSlider("BACKGROUND").setGroup(group_display).setSize(sx,sy).setPosition(px, py)
          .setRange(0, 255).setValue(BACKGROUND_COLOR).plugTo(this, "BACKGROUND_COLOR");
  
      cp5.addCheckBox("setOptionsGeneral").setGroup(group_display).setSize(38, 18).setPosition(px, py+=oy)
          .setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
          .addItem("display source", 0).activate(DISPLAY_SOURCE ? 0 : 100);
  
      cp5.addCheckBox("activeFilters").setGroup(group_display).setSize(18, 18).setPosition(px, py+=(int)(oy*1.5f))
          .setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
          .addItem("grayscale"       , 0).activate(APPLY_GRAYSCALE ? 0 : 100)
          .addItem("bilateral filter", 1).activate(APPLY_BILATERAL ? 1 : 100);
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_fluid)
      .addItem(group_oflow)
      .addItem(group_display)
      .open(0, 1, 2);
  }
  
  

  

  public static void main(String args[]) {
    PApplet.main(new String[] { OpticalFlow_MovieFluid.class.getName() });
  }
}