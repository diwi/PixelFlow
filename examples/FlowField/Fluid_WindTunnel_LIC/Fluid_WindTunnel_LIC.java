/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */





package FlowField.Fluid_WindTunnel_LIC;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
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



public class Fluid_WindTunnel_LIC extends PApplet {
  
  //
  //
  // Windtunnel Demo: LIC filter on top of fluid velocity.
  //
  //

  private class MyFluidData implements DwFluid2D.FluidData{
    
    @Override
    // this is called during the fluid-simulation update step.
    public void update(DwFluid2D fluid) {
    
      float px, py, vx, vy, radius, vscale;

      boolean mouse_input = !cp5.isMouseOver() && mousePressed && !obstacle_painter.isDrawing();
      if(mouse_input ){
        
        vscale = 10;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        
        if(mouseButton == LEFT){
          radius = 20;
          fluid.addVelocity(px, py, radius, vx, vy);
          fluid.addDensity (px, py, radius, 1.0f, 0.0f, 0.40f, 1f, 1);
        }
      }
  
      // use the text as input for density
      float mix_density  = fluid.simulation_step == 0 ? 1.0f : 0.05f;
      float mix_velocity = fluid.simulation_step == 0 ? 1.0f : 0.5f;
      
      addDensityTexture (fluid, pg_density , mix_density);
      addVelocityTexture(fluid, pg_velocity, mix_velocity);
    }
    
    
    // custom shader, to add velocity from a texture (PGraphics2D) to the fluid.
    public void addVelocityTexture(DwFluid2D fluid, PGraphics2D pg, float mix){
      int[] pg_tex_handle = new int[1]; 
//      pg_tex_handle[0] = pg.getTexture().glName
      context.begin();
      context.getGLTextureHandle(pg, pg_tex_handle);
      context.beginDraw(fluid.tex_velocity.dst);
      DwGLSLProgram shader = context.createShader("data/addVelocity.frag");
      shader.begin();
      shader.uniform2f     ("wh"        , fluid.fluid_w, fluid.fluid_h);                                                                   
      shader.uniform1i     ("blend_mode", 6);   
      shader.uniform1f     ("mix_value" , mix);     
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
      shader.uniform1i     ("blend_mode", 2);   
      shader.uniform1f     ("mix_value" , mix);     
      shader.uniform1f     ("multiplier", 1);     
      shader.uniformTexture("tex_ext"   , pg_tex_handle[0]);
      shader.uniformTexture("tex_src"   , fluid.tex_density.src);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
      context.end();
      fluid.tex_density.swap();
    }
 
  }
  
  boolean START_FULLSCREEN = !true;
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 0;
  int gui_y = 0;
      
  int fluidgrid_scale = 4;

  DwPixelFlow context;
  DwFluid2D fluid;

  MyFluidData cb_fluid_data;

  PGraphics2D pg_fluid;             // render target
  PGraphics2D pg_density;           // texture-buffer, for adding fluid data
  PGraphics2D pg_velocity;          // texture-buffer, for adding fluid data
  PGraphics2D pg_obstacles;         // texture-buffer, for adding fluid data
  PGraphics2D pg_obstacles_drawing; // texture-buffer, for adding fluid data
  
  ObstaclePainter obstacle_painter;
  
  DwFlowField ff_fluid;

  PGraphics2D pg_noise;
  
  // some state variables for the GUI/display
  int     BACKGROUND_COLOR           = 0;
  boolean UPDATE_FLUID               = true;
  boolean DISPLAY_FLUID_TEXTURES     = true;
  boolean DISPLAY_FLUID_VECTORS      = false;
  int     DISPLAY_fluid_texture_mode = 0;

  public void settings() {
    if(START_FULLSCREEN){
      viewport_w = displayWidth;
      viewport_h = displayHeight;
      viewport_x = 0;
      viewport_y = 0;
      fullScreen(P2D);
    } else {
      viewport_w = (int) min(viewport_w, displayWidth  * 0.9f);
      viewport_h = (int) min(viewport_h, displayHeight * 0.9f);
      size(viewport_w, viewport_h, P2D);
    }
    
    gui_x = viewport_w-gui_w;
    smooth(0);
  }
  
  
  public void setup() {
    
    surface.setLocation(viewport_x, viewport_y);
    surface.setResizable(true);
    
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
    
    // fluid simulation
    fluid = new DwFluid2D(context);

    // some fluid params
    fluid.param.dissipation_density     = 0.99999f;
    fluid.param.dissipation_velocity    = 0.99999f;
    fluid.param.dissipation_temperature = 0.70f;
    fluid.param.vorticity               = 0.00f;
    
    // interface for adding data to the fluid simulation
    cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);
    
    // init the obstacle painter, for mouse interaction
    obstacle_painter = new ObstaclePainter(pg_obstacles_drawing);
    
    
//    PFont font = createDefaultFont(12);
    PFont font = createFont("Arial", 12, false);
    textFont(font);

    resize();
    
    createGUI();

    frameRate(60);
  }
  

  public void resize(){
   
    if(!fluid.resize(width, height, fluidgrid_scale)){
      return;
    }
    
    viewport_w = width;
    viewport_h = height;
    
    float[][] PALLETTE_L = {
        {    0,    0,   0, 255},
        {  255,  160,   0, 255},
        {    0,   96, 255, 255},
        {  255,  160,   0, 255},
        {    0,    0,   0, 255},
    };
    float[][] PALLETTE_R = {
        {    0,    0,   0, 255},
        {   64,   0,   0, 255},
        {   255, 192,   0, 255},
        {   64,   0,   0, 255},
        {   255, 255,  255, 255},
        {   128,  64,   0, 255},

        {   255,  64,   0, 255},
        {   255, 192,   0, 255},
        {    32,   128, 255, 255},
        {   64,   0,   0, 255},
        {    0,    0,   0, 255},
    };
    
    float[] COL_CVL = new float[4];
    float[] COL_CVR = new float[4];
    float[] COL_CC  = new float[4];
  
    
    int dimx = width/2;
    int dimy = height/2;
    
    PGraphics2D pg = (PGraphics2D) createGraphics(dimx, dimy, PConstants.P2D);
    pg.smooth(0);
    pg.beginDraw();
    pg.noStroke();
    
    for(int y = 0; y < dimy; y++){
      for(int x = 0; x < dimx; x++){
        float nx = x / (float) pg.width;
        float ny = y / (float) pg.height;
        float nval = noise(x * 0.025f, y * 0.025f) * 1.7f  + 0.3f;
        DwUtils.getColor(PALLETTE_L, ny, COL_CVL);
        DwUtils.getColor(PALLETTE_R, ny, COL_CVR);
        DwUtils.mix(COL_CVL, COL_CVR, nx, COL_CC);
        DwUtils.mult(COL_CC, nval);
        DwUtils.clamp(COL_CC, 0, 255);
        pg.fill(COL_CC[0], COL_CC[1], COL_CC[2], 255);
        pg.rect(x, y, 1, 1);
      }
    }
    
    // add some random noise
    int num_points = dimx * dimy / 4;
    for(int i = 0; i < num_points; i++){
      float x = random(0, dimx-1);
      float y = random(0, dimy-1);
      pg.fill(0, random(255));
      pg.rect(x, y, 1, 1);
    }
  
    pg.endDraw();
    
    pg_noise = pg;
    
      

    // fluid render target
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(8);

    // main obstacle texture
    pg_obstacles = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_obstacles.smooth(8);
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    pg_obstacles.endDraw();
    
    // second obstacle texture, used for interactive mouse-driven painting
    pg_obstacles_drawing = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_obstacles_drawing.smooth(8);

    
    resetObstacles();
    
    // image/buffer that will be used as density input
    pg_density = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_density.noSmooth();
    pg_density.beginDraw();
    pg_density.clear();
    pg_density.endDraw();
    
    // image/buffer that will be used as velocity input
    pg_velocity = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_velocity.noSmooth();
    pg_velocity.beginDraw();
    pg_velocity.clear();
    pg_velocity.endDraw();
    
    obstacle_painter.pg = pg_obstacles_drawing;
    
    if(cp5 != null){
      cp5.getGroup("Line Integral Convolution").setSize(gui_w, height);
      cp5.setPosition(width-gui_w, 0);
    }
    

    
  }
  
  
  
  public void drawObstacles(){
    pg_obstacles.beginDraw();
    pg_obstacles.blendMode(REPLACE);
    pg_obstacles.image(pg_obstacles_drawing, 0, 0);
    pg_obstacles.endDraw();
  }
  
  
  
  

  
  public void drawVelocity(PGraphics pg, int texture_type){
    
    float vx = 50; // velocity in x direction
    float vy =  0; // velocity in y direction
    
    int argb = Velocity.Polar.encode_vX_vY(vx, vy);
    float[] vam = Velocity.Polar.getArc(vx, vy);
    
//    float vA = vam[0]; // velocity direction (angle)
    float vM = vam[1]; // velocity magnitude
    
    if(vM == 0){
      // no velocity, so just return
      return;
    }
    
    pg.beginDraw();
    pg.blendMode(REPLACE); // important
    pg.clear();
    pg.noStroke();
    
    if(vM > 0){
      
      int offy = height/3;
  
      // add density
      if(texture_type == 1){
        float size_h = height-2*offy;
        pg.noStroke();
//        pg.fill(50, 155, 255);
//        pg.rect(0, offy, 10, size_h/2);
//        pg.fill(255, 155, 50);
//        pg.rect(0, offy+size_h/2, 10, size_h/2);
//        
        
        int num_segs = 30;
        float seg_len = size_h / num_segs;
        for(int i = 0; i < num_segs; i++){
          float py = offy + i * seg_len;
          if(i%2 == 0){
            if(frameCount % 50 == 0){
              pg.fill(255,150,50);
              pg.rect(5, py, seg_len*2, seg_len);
            }
          } else {
            pg.fill(50, 155, 255);
//            if(frameCount % 70 == 0){
//              pg.fill(255, 0, 0);
//              pg.rect(10, py, 11, seg_len);
//            }
            pg.noStroke();
            pg.rect(5, py, seg_len, seg_len);
          }

        }
      }
      
      // add encoded velocity
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
        
        pg.noStroke();
        pg.rect(0, offy, 10, height-2*offy);
      }
    }
    pg.endDraw();
  }
  
  
  

  

  public void draw() {
    
    resize();
   

    if(UPDATE_FLUID){
      
      drawVelocity(pg_velocity, 0);
      drawVelocity(pg_density , 1);
      
      drawObstacles();
      
      fluid.addObstacles(pg_obstacles);
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

    // draw the brush, when obstacles get removed
    obstacle_painter.displayBrush(this.g);

    // info
    String txt_fps  = String.format(getClass().getSimpleName()+ "   [view %d/%d]  [fluid %d/%d]  [fps %6.2f]", width, height, fluid.fluid_w, fluid.fluid_h, frameRate);
           txt_fps += String.format("   [LMB=impulse   MMB=draw   RMB=clear]");
    
    surface.setTitle(txt_fps);
    
    fill(255);
//    textSize(12);
    text(txt_fps, 5, height-6);
    
    // draw gui
    cp5.draw();
    
  }
  


  
  
  
  
  
  public class ObstaclePainter{
    
    // 0 ... not drawing
    // 1 ... adding obstacles
    // 2 ... removing obstacles
    public int draw_mode = 0;
    PGraphics pg;
    
    float size_paint = 20;
    float size_clear = size_paint * 3.5f;
    
    float paint_x, paint_y;
    float clear_x, clear_y;
    
    int shading = 255;
    
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
  public void resetObstacles(){
    pg_obstacles_drawing.beginDraw();
    pg_obstacles_drawing.beginDraw();
    pg_obstacles_drawing.clear();
    
    float dimxy = 2 * height/3f;
    
    float th = 20;
    
    pg_obstacles_drawing.noStroke();
    pg_obstacles_drawing.fill(255);
    pg_obstacles_drawing.rectMode(CENTER);
    pg_obstacles_drawing.rect(width/2, height/2, dimxy, dimxy, 2*th);
    pg_obstacles_drawing.blendMode(REPLACE);
    pg_obstacles_drawing.fill(0,0);
    pg_obstacles_drawing.rect(width/2, height/2, dimxy-2*th, dimxy-2*th, th);
    pg_obstacles_drawing.rect(width/2, height/2, dimxy*2, 50);
    pg_obstacles_drawing.endDraw();
    pg_obstacles_drawing.endDraw();
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
    cp5.setAutoDraw(false);
    cp5.setPosition(width-gui_w, 0);
    
    int sx, sy, px, py, oy;
    
    sx = 100; sy = 14; oy = (int)(sy*1.5f);
    
    int col_group = color(8,64);
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - FLUID
    ////////////////////////////////////////////////////////////////////////////
    Group group_fluid = cp5.addGroup("fluid");
    {
      group_fluid.setHeight(20).setSize(gui_w, 320)
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
      
      
      cp5.addButton("reset obstacles").setGroup(group_fluid).plugTo(this, "resetObstacles" ).setSize(80, 18).setPosition(px, py+= (int)(oy*1.5f));
      

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
      group_lic.setHeight(20).setSize(gui_w, height)
      .setBackgroundColor(col_group).setColorBackground(col_group);
      group_lic.getCaptionLabel().align(CENTER, CENTER);
      
      DwFlowField.ParamLIC param = ff_fluid.param_lic;
      
      px = 15; py = 15;
      int count = 2;
      sx = (gui_w-30 - 2 * (count-1)) / count;
      RadioButton rb_type = cp5.addRadio("setDisplayType").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(count).plugTo(this, "setDisplayType")
        .setNoneSelectedAllowed(true)
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
      .setRange(0, 2).setValue(param.acc_mult).plugTo(param, "acc_mult");
      py += sy + dy_item;
      
      cp5.addSlider("vel_mult").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 2).setValue(param.vel_mult).plugTo(param, "vel_mult");
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
    cp5.addAccordion("acc").setPosition(0, 0).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_fluid)
      .addItem(group_lic)
      .open();
    
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Fluid_WindTunnel_LIC.class.getName() });
  }
}