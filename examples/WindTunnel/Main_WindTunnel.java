/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */





package com.thomasdiewald.pixelflow.examples.WindTunnel;


import java.util.ArrayList;

import com.thomasdiewald.pixelflow.src.Fluid;
import com.thomasdiewald.pixelflow.src.PixelFlow;
import com.thomasdiewald.pixelflow.src.StreamLines;
import com.thomasdiewald.pixelflow.src.dwgl.DwGLSLProgram;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.Numberbox;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;


public class Main_WindTunnel extends PApplet {
  

  private class MyFluidData implements Fluid.FluidData{
    
    @Override
    // this is called during the fluid-simulation update step.
    public void update(Fluid fluid) {
    
      float px, py, vx, vy, radius, vscale, r, g, b, a;

      boolean mouse_input = !cp5.isMouseOver() && mousePressed && !obstacle_painter.isDrawing();
      if(mouse_input ){
        
        vscale = 15;
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
    public void addVelocityTexture(Fluid fluid, PGraphics2D pg, float mix){
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
    public void addDensityTexture(Fluid fluid, PGraphics2D pg, float mix){
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
  
  
  int viewport_w = 1200;
  int viewport_h =  600;
  int fluidgrid_scale = 1;
  
  int BACKGROUND_COLOR = 0;
  
  PFont font;
  
  public PixelFlow context;
  public Fluid fluid;
  public StreamLines streamlines;
  MyFluidData cb_fluid_data;

  PGraphics2D pg_fluid;             // render target
  PGraphics2D pg_density;           // texture-buffer, for adding fluid data
  PGraphics2D pg_velocity;          // texture-buffer, for adding fluid data
  PGraphics2D pg_obstacles;         // texture-buffer, for adding fluid data
  PGraphics2D pg_obstacles_drawing; // texture-buffer, for adding fluid data
  
  ObstaclePainter obstacle_painter;
  
  MorphShape morph; // animated morph shape, used as dynamic obstacle
  
  
  // Streamline states
  public boolean DISPLAY_STREAMLINES = false;
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
    
    streamlines = new StreamLines(context);
    
    // fluid simulation
    fluid = new Fluid(context, viewport_w, viewport_h, fluidgrid_scale);

    // some fluid params
    fluid.param.dissipation_density     = 0.99999f;
    fluid.param.dissipation_velocity    = 0.99999f;
    fluid.param.dissipation_temperature = 0.70f;
    fluid.param.vorticity               = 0.00f;
    
    // interface for adding data to the fluid simulation
    cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);

    // processing font
    font = createFont("SourceCodePro-Regular.ttf", 48);

    // fluid render target
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(4);

    // main obstacle texture
    pg_obstacles = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
//    pg_obstacles.noSmooth();
    pg_obstacles.smooth(4);
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    pg_obstacles.endDraw();
    
    
    // second obstacle texture, used for interactive mouse-driven painting
    pg_obstacles_drawing = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
  //  pg_obstacles_drawing.noSmooth();
    pg_obstacles_drawing.smooth(4);
    pg_obstacles_drawing.beginDraw();
    pg_obstacles_drawing.clear();
    pg_obstacles_drawing.blendMode(REPLACE);
   
    // place some initial obstacles
    randomSeed(6);
    for(int i = 0; i < 50; i++){
      float px = random(150, width-50);
      float py = random(50, height-50);
      
      // keep left center area free
      if(    py > height/2 - 120 
          && py < height/2 + 120
          && px < width/2 + 100
          ){
        continue;
      }
      pg_obstacles_drawing.rectMode(CENTER);
      pg_obstacles_drawing.noStroke();
      pg_obstacles_drawing.fill(64);
      pg_obstacles_drawing.rect(px, py, 20, 20, random(0,10));
    }
    
    pg_obstacles_drawing.translate(200, height/2+50);
    pg_obstacles_drawing.rotate(0.3f);
    pg_obstacles_drawing.fill(200,0,0);
    pg_obstacles_drawing.textFont(font);
    pg_obstacles_drawing.text("fluid simulation", 0, 0);
   
    pg_obstacles_drawing.endDraw();
    
    
    // init the obstacle painter, for mouse interaction
    obstacle_painter = new ObstaclePainter(pg_obstacles_drawing);
    
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
    
    
    // animated morph shape
    morph = new MorphShape(120);

    createGUI();

    frameRate(60);
  }
  

  
  
  public void drawObstacles(){
  
    pg_obstacles.beginDraw();
    pg_obstacles.blendMode(BLEND);
    pg_obstacles.clear();
    
    // add morph-shape as obstacles
    pg_obstacles.pushMatrix();
    {
      pg_obstacles.noFill();
      pg_obstacles.strokeWeight(10);
      pg_obstacles.stroke(64);
      
      pg_obstacles.translate(width/2, height/2);
//      morph.drawAnimated(pg_obstacles, 0.975f);
      morph.draw(pg_obstacles, mouseY/(float)height);
    }
    pg_obstacles.popMatrix();
    
    // add painted obstacles on top of it
    pg_obstacles.image(pg_obstacles_drawing, 0, 0);
    pg_obstacles.endDraw();
  }
  
  
  
  

  
  public void drawVelocity(PGraphics pg, int texture_type){
    
    float vx = 30; // velocity in x direction
    float vy =  0; // velocity in y direction
    
    int argb = Velocity.Polar.encode_vX_vY(vx, vy);
    float[] vam = Velocity.Polar.getArc(vx, vy);
    
    float vA = vam[0]; // velocity direction (angle)
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
      
      int offy = 100;
  
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
    
    if(DISPLAY_STREAMLINES){
      streamlines.render(pg_fluid, fluid, STREAMLINE_DENSITY);
    }
    
    // display
    image(pg_fluid    , 0, 0);
    image(pg_obstacles, 0, 0);

    
    // draw the brush, when obstacles get removed
    obstacle_painter.displayBrush(this.g);

    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", fluid.fluid_w, fluid.fluid_h, fluid.simulation_step, frameRate);
    surface.setTitle(txt_fps);
    
  }
  


  
  
  
  
  
  public class MorphShape{
    
    ArrayList<float[]> shape1 = new ArrayList<float[]>();
    ArrayList<float[]> shape2 = new ArrayList<float[]>();

    public MorphShape(float size){
      createShape1(size);
      createShape2(size*0.8f);
      initAnimator(1, 1);
    }
    
    float O = 2f;
    
    // square
    public void createShape1(float size){
  
      int NUM_SEGS = 10;
      float seg_len = size/NUM_SEGS;
      
      float px = +size/2;
      float py = +size/2;
     
      // BOTTOM
      for(int i = 0; i < NUM_SEGS; i++){
        shape1.add(new float[]{px, py});
        px -= seg_len;
      }

      // LEFT
      for(int i = 0; i < NUM_SEGS; i++){
        shape1.add(new float[]{px, py});
        py -= seg_len;
      }

      // TOP
      for(int i = 0; i < NUM_SEGS; i++){
        shape1.add(new float[]{px, py});
        px += seg_len;
      }
      shape1.add(new float[]{px, py}); 
    }
    
    public void createShape2(float size){
      int NUM_POINTS = shape1.size();
      
      float arc_min = PI/4f;
      float arc_max = TWO_PI-arc_min;
      float arc_range = arc_max - arc_min;
      float arc_step = arc_range/(NUM_POINTS-1);
   
      for (int i = 0; i < NUM_POINTS; i++) {
        float arc = arc_min + i * arc_step;
        float vx = size * cos(arc);
        float vy = size * sin(arc);

        shape2.add(new float[]{vx, vy});
      }
      
    }
    
    
    public void initAnimator(float morph_mix, int morph_state){
      if( morph_mix < 0 ) morph_mix = 0;
      if( morph_mix > 1 ) morph_mix = 1;
      morph_state &= 1;
      
      this.morph_mix = morph_mix;
      this.morph_state = morph_state;
    }

    
    float morph_mix = 1f;
    int   morph_state = 1;
    
    public void drawAnimated(PGraphics2D pg, float ease){
      morph_mix *= ease;
      if(morph_mix < 0.0001f){
        morph_mix = 1f;
        morph_state ^= 1;
      } 
      
      this.draw(pg, morph_state == 0 ? morph_mix : 1-morph_mix);
    }
    
    
    public void draw(PGraphics2D pg, float mix){
      pg.beginShape();
      for (int i = 0; i < shape1.size(); i++) {
        float[] v1 = shape1.get(i);
        float[] v2 = shape2.get(i);
        float vx = v1[0] * (1.0f - mix) + v2[0] * mix;
        float vy = v1[1] * (1.0f - mix) + v2[1] * mix;
        pg.vertex(vx, vy);
      }
      pg.endShape();
    }
     
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
    
    int shading = 64;
    
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
    .setBackgroundHeight(height)
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
    
    cp5.addRadio("setDisplayVelocityVectors").setGroup(group_fluid).setSize(18,18).setPosition(px, py+=(int)(oy*2.5))
    .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
    .addItem("Velocity Vectors",0)
    .addItem("StreamLines"     ,1)
    ;
    

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
        .setPosition(width-180,0)
        .setWidth(180)
        .addItem(group_fluid)
        ;

    accordion.setCollapseMode(Accordion.MULTI);
    accordion.open(0);
    
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_WindTunnel.class.getName() });
  }
}