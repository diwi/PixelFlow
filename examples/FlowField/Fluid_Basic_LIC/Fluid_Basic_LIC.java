/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package FlowField.Fluid_Basic_LIC;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;

public class Fluid_Basic_LIC extends PApplet {
  
  //
  //
  // Basic Fluid Demo: LIC filter on top of fluid velocity.
  //
  // controls:
  //
  // LMB: add Density + Velocity
  // MMB: draw obstacles
  // RMB: clear obstacles
  //
  
  private class MyFluidData implements DwFluid2D.FluidData{
    
    // update() is called during the fluid-simulation update step.
    @Override
    public void update(DwFluid2D fluid) {
    
      float px, py, vx, vy, radius, vscale, r, g, b, intensity, temperature;
      
      // add impulse: density + temperature
      intensity = 1.0f;
      px = 1*width/3;
      py = 0;
      radius = 100;
      r = 0.0f;
      g = 0.3f;
      b = 1.0f;
      fluid.addDensity(px, py, radius, r, g, b, intensity);

      if((fluid.simulation_step) % 200 == 0){
        temperature = 50f;
        fluid.addTemperature(px, py, radius, temperature);
      }
      
      // add impulse: density + temperature
      float animator = sin(fluid.simulation_step*0.01f);
 
      intensity = 1.0f;
      px = 2*width/3f;
      py = 150;
      radius = 50;
      r = 1.0f;
      g = 0.0f;
      b = 0.3f;
      fluid.addDensity(px, py, radius, r, g, b, intensity);
      
      temperature = animator * 20f;
      fluid.addTemperature(px, py, radius, temperature);
      
      
      // add impulse: density 
      px = 1*width/3f;
      py = height-2*height/3f;
      radius = 50.5f;
      r = g = b = 64/255f;
      intensity = 1.0f;
      fluid.addDensity(px, py, radius, r, g, b, intensity, 3);

      
      boolean mouse_input = !cp5.isMouseOver() && mousePressed && !obstacle_painter.isDrawing();
      
      // add impulse: density + velocity
      if(mouse_input && mouseButton == LEFT){
        radius = 15;
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        
        fluid.addDensity(px, py, radius, 0.25f, 0.0f, 0.1f, 1.0f);
        fluid.addVelocity(px, py, radius, vx, vy);
      }
     
    }
  }
  
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  int fluidgrid_scale = 4;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  DwPixelFlow context;
  
  DwFluid2D fluid;
  ObstaclePainter obstacle_painter;
 
  // render targets
  PGraphics2D pg_fluid;
  
  //texture-buffer, for adding obstacles
  PGraphics2D pg_obstacles;
  
  // some state variables for the GUI/display
  int     BACKGROUND_COLOR           = 0;
  boolean UPDATE_FLUID               = true;
  boolean DISPLAY_FLUID_TEXTURES     = true;
  boolean DISPLAY_FLUID_VECTORS      = false;
  int     DISPLAY_fluid_texture_mode = 0;
  
  
  DwFlowField ff_fluid;

  PGraphics2D pg_noise;
  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(8);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    ff_fluid = new DwFlowField(context);
    
    ff_fluid.param.blur_iterations = 1;
    ff_fluid.param.blur_radius     = 1;
    
    ff_fluid.param_lic.iterations     = 2;
    ff_fluid.param_lic.num_samples    = 30;
    ff_fluid.param_lic.acc_mult       = 0.35f;
    ff_fluid.param_lic.vel_mult       = 0.35f;
    ff_fluid.param_lic.intensity_mult = 1.10f;
    ff_fluid.param_lic.intensity_exp  = 1.20f;
    ff_fluid.param_lic.TRACE_BACKWARD = true;
    ff_fluid.param_lic.TRACE_FORWARD  = false;

    
    pg_noise = DwUtils.createBackgroundNoiseTexture(this, width/2, height/2);
    

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
    pg_fluid.smooth(8);
    pg_fluid.beginDraw();
    pg_fluid.background(BACKGROUND_COLOR);
    pg_fluid.endDraw();
    
    // pgraphics for obstacles
    pg_obstacles = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_obstacles.smooth(8);
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    // circle-obstacles
    pg_obstacles.strokeWeight(10);
    pg_obstacles.noFill();
    pg_obstacles.noStroke();
    pg_obstacles.fill(32);
    float radius;
    radius = 100;
    pg_obstacles.ellipse(1*width/3f,  2*height/3f, radius, radius);
    radius = 150;
    pg_obstacles.ellipse(2*width/3f,  2*height/4f, radius, radius);
    radius = 200;
    pg_obstacles.stroke(32);
    pg_obstacles.strokeWeight(10);
    pg_obstacles.noFill();
    pg_obstacles.ellipse(1*width/2f,  1*height/4f, radius, radius);
    // border-obstacle
    pg_obstacles.strokeWeight(20);
    pg_obstacles.stroke(32);
    pg_obstacles.noFill();
    pg_obstacles.rect(0, 0, pg_obstacles.width, pg_obstacles.height);
    pg_obstacles.endDraw();
    
    // class, that manages interactive drawing (adding/removing) of obstacles
    obstacle_painter = new ObstaclePainter(pg_obstacles);
    
    createGUI();
    
    frameRate(60);
  }
  


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
    

    if(LIC_DISPLAY_MODE >= 0){
      
      ff_fluid.resize(fluid.tex_velocity.src.w, fluid.tex_velocity.src.h);
      DwFilter.get(context).copy.apply(fluid.tex_velocity.src, ff_fluid.tex_vel);
      ff_fluid.blur();
      
      if(LIC_DISPLAY_MODE == 0){
        ff_fluid.displayLineIntegralConvolution(pg_fluid, pg_noise);
      }
      if(LIC_DISPLAY_MODE == 1){
        ff_fluid.displayPixel(pg_fluid);
        ff_fluid.displayLines(pg_fluid);
      }
    }
    



    // display
    blendMode(REPLACE);
    image(pg_fluid    , 0, 0);
    blendMode(BLEND);
    image(pg_obstacles, 0, 0);
//    blendMode(BLEND);
    
    
    obstacle_painter.displayBrush(this.g);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", fluid.fluid_w, fluid.fluid_h, fluid.simulation_step, frameRate);
    surface.setTitle(txt_fps);
  }
  


  public void mousePressed(){
    if(mouseButton == CENTER ) obstacle_painter.beginDraw(1); // add obstacles
    if(mouseButton == RIGHT  ) obstacle_painter.beginDraw(2); // remove obstacles
  }
  
  public void mouseDragged(){
    obstacle_painter.draw();
  }
  
  public void mouseReleased(){
    obstacle_painter.endDraw();
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
    
    if(key == 'h') toggleGUI();
  }
 
  
  

  
  public void toggleGUI(){
    if(cp5.isVisible()) cp5.hide(); else cp5.show();
  }

  
  public void resetLic(){
    ff_fluid.reset();
  }
  
  int LIC_DISPLAY_MODE = 0;
  public void setDisplayType(int val){
    LIC_DISPLAY_MODE = val;
  }
  
  public void setLicStates(float[] val){
    ff_fluid.param_lic.TRACE_BACKWARD    = val[0] > 0;
    ff_fluid.param_lic.TRACE_FORWARD     = val[1] > 0;
  }
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    int sx, sy, px, py, oy;
    
    sx = 100; sy = 14; oy = (int)(sy*1.5f);
    
    int col_group = color(8,64);
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - FLUID
    ////////////////////////////////////////////////////////////////////////////
    Group group_fluid = cp5.addGroup("fluid");
    {
      group_fluid.setHeight(20).setSize(gui_w, 300)
      .setBackgroundColor(col_group).setColorBackground(col_group);
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
    
    
    
    sx = 100; 
    sy = 14; 

    int dy_group = 20;
    int dy_item = 4;
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - LIC
    ////////////////////////////////////////////////////////////////////////////
    Group group_lic = cp5.addGroup("Line Integral Convolution");
    {
      group_lic.setHeight(20).setSize(gui_w, 260)
      .setBackgroundColor(col_group).setColorBackground(col_group);
      group_lic.getCaptionLabel().align(CENTER, CENTER);
      
      DwFlowField.ParamLIC param = ff_fluid.param_lic;
      
      px = 15; py = 15;
      int count = 2;
      sx = (gui_w-30 - 2 * (count-1)) / count;
      RadioButton rb_type = cp5.addRadio("setDisplayType").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(count).plugTo(this, "setDisplayType")
        .setNoneSelectedAllowed(!false)
        .addItem("LIC", 0)
        .addItem("FF" , 1)
        .activate(LIC_DISPLAY_MODE);
  
      for(Toggle toggle : rb_type.getItems()) toggle.getCaptionLabel().alignX(CENTER).alignY(CENTER);
      py += sy + dy_group;
   
      cp5.addSlider("lic iterations").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(1, 40).setValue(param.iterations).plugTo(param, "iterations");
      py += sy + dy_item;
      
      cp5.addSlider("samples").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(1, 120).setValue(param.num_samples).plugTo(param, "num_samples");
      py += sy + dy_item;
      
      cp5.addSlider("blur radius").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 10).setValue(ff_fluid.param.blur_radius).plugTo(ff_fluid.param, "blur_radius");
      py += sy + dy_item;
      
      cp5.addSlider("acc_mult").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 5).setValue(param.acc_mult).plugTo(param, "acc_mult");
      py += sy + dy_item;
      
      cp5.addSlider("vel_mult").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 3).setValue(param.vel_mult).plugTo(param, "vel_mult");
      py += sy + dy_item;
      
      cp5.addSlider("intensity_exp").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0.00f, 2.5f).setValue(param.intensity_exp).plugTo(param, "intensity_exp");
      py += sy + dy_item;
      
      cp5.addSlider("intensity_mult").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0.5f, 2.5f).setValue(param.intensity_mult).plugTo(param, "intensity_mult");
      py += sy + dy_group;
      
      cp5.addCheckBox("setLicStates").setGroup(group_lic).setSize(sy,sy).setPosition(px, py)
      .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
      .addItem("TRACE BACKWARD"    , 0).activate(param.TRACE_BACKWARD ? 0 : 4)
      .addItem("TRACE FORWARD"     , 1).activate(param.TRACE_FORWARD  ? 1 : 4)
      ; 

    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_fluid)
      .addItem(group_lic)
      .open();
  }
  
  

  
  public class ObstaclePainter{
    
    // 0 ... not drawing
    // 1 ... adding obstacles
    // 2 ... removing obstacles
    public int draw_mode = 0;
    PGraphics pg;
    
    float size_paint = 15;
    float size_clear = size_paint * 2.5f;
    
    float paint_x, paint_y;
    float clear_x, clear_y;
    
    int shading = 32;
    
    public ObstaclePainter(PGraphics pg){
      this.pg = pg;
    }
    
    public void beginDraw(int mode){
      paint_x = mouseX;
      paint_y = mouseY;
      this.draw_mode = mode;
      if(mode == 1){
        pg.beginDraw();
        pg.blendMode(REPLACE);
        pg.noStroke();
        pg.fill(shading);
        pg.ellipse(mouseX, mouseY, size_paint, size_paint);
        pg.endDraw();
      }
      if(mode == 2){
        clear(mouseX, mouseY);
      }
    }
    
    public boolean isDrawing(){
      return draw_mode != 0;
    }
    
    public void draw(){
      paint_x = mouseX;
      paint_y = mouseY;
      if(draw_mode == 1){
        pg.beginDraw();
        pg.blendMode(REPLACE);
        pg.strokeWeight(size_paint);
        pg.stroke(shading);
        pg.line(mouseX, mouseY, pmouseX, pmouseY);
        pg.endDraw();
      }
      if(draw_mode == 2){
        clear(mouseX, mouseY);
      }
    }

    public void endDraw(){
      this.draw_mode = 0;
    }
    
    public void clear(float x, float y){
      clear_x = x;
      clear_y = y;
      pg.beginDraw();
      pg.blendMode(REPLACE);
      pg.noStroke();
      pg.fill(0, 0);
      pg.ellipse(x, y, size_clear, size_clear);
      pg.endDraw();
    }
    
    public void displayBrush(PGraphics dst){
      if(draw_mode == 1){
        dst.strokeWeight(1);
        dst.stroke(0);
        dst.fill(200,50);
        dst.ellipse(paint_x, paint_y, size_paint, size_paint);
      }
      if(draw_mode == 2){
        dst.strokeWeight(1);
        dst.stroke(200);
        dst.fill(200,100);
        dst.ellipse(clear_x, clear_y, size_clear, size_clear);
      }
    }
    

  }
  
  
  
  
  
  
  
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Fluid_Basic_LIC.class.getName() });
  }
}