/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package FlowField.FlowField_LIC_StreamLines;

import java.util.Locale;

import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import controlP5.Accordion;
import controlP5.CColor;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.opengl.PJOGL;



public class FlowField_LIC_StreamLines extends PApplet {
  
  //
  // Line Integral Convolution (LIC) Shader
  //
  // LIC is a  low-pass filter, for sampling and averaging  samples along the 
  // streamlines of a flowfield (vector field).
  //
  //
  // LIC:
  //
  // In this demo the flowfield is created by some simply mouse-drawing.
  // The background texture the sample are taken from should contain 
  // high-frequency noise for the LIC-Shader to work best.
  //
  //
  // StreamLines:
  //
  // Additionally to the LIC filter, the streamlines are computed and rendered.
  // This is quite expensive, since a lot of lines are required and each line
  // needs a reasonable amount of vertices (10-40).
  // All these vertices need to be generated on the fly by an alternating 
  // update and render pass.
  // In the beginning for each line a particle is spawned and during the update
  // passes the particles positions are updated using verlet integration.
  // In the successive render pass, the last particle-step is rendered as a line.
  //
  //
  
  
  
  
  
  boolean START_FULLSCREEN = !true;
  
  int viewport_w = 1680;
  int viewport_h = 1024;
//  int viewport_w = 1280;
//  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 10;
  int gui_y = 10;
  
  PGraphics2D pg_canvas;
  PGraphics2D pg_impulse;
  PGraphics2D pg_noise;
  PGraphics2D pg_tmp;

  DwPixelFlow context;
  
  DwFlowField ff_impulse;
  
  DwFlowFieldParticles particles_stream;
  
  int     STREAMLINE_SAMPLES = 20;
  int     STREAMLINE_RES = 10;
  int     DISPLAY_MODE  = 1;
  
  boolean APPLY_IMPULSE       = false;
  boolean DISPLAY_STREAMLINES = true;
  boolean DISPLAY_PARTICLES   = !true;
  boolean BLUR_ITERATIONS     = true;
  
  
  float impulse_max = 256;
  float impulse_mul = 10;
  float impulse_tsmooth = 1f;
  int   impulse_radius = 250;

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
    PJOGL.profile = 3;
    smooth(8);
  }
  

  public void setup(){
    surface.setLocation(viewport_x, viewport_y);
    surface.setResizable(true);

    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    ff_impulse = new DwFlowField(context);
    
    ff_impulse.param.blur_iterations = 1;
    ff_impulse.param.blur_radius     = 1;
    
    ff_impulse.param_lic.iterations     = 2;
    ff_impulse.param_lic.num_samples    = 25;
    ff_impulse.param_lic.acc_mult       = 1.00f;
    ff_impulse.param_lic.vel_mult       = 1.00f;
    ff_impulse.param_lic.intensity_mult = 1.00f;
    ff_impulse.param_lic.intensity_exp  = 1.50f;
    ff_impulse.param_lic.TRACE_BACKWARD = true;
    ff_impulse.param_lic.TRACE_FORWARD  = false;
    
    particles_stream = new DwFlowFieldParticles(context);
    
    particles_stream.param.blend_mode = 1; // 1 = additive blending
    particles_stream.param.shader_collision_mult = 0.00f;
    particles_stream.param.display_line_width  = 1f;
    particles_stream.param.display_line_smooth = false;
    particles_stream.param.steps = 1;
    particles_stream.param.mul_obs =  0.0f;
    particles_stream.param.mul_col =  0.0f;
    particles_stream.param.mul_coh =  0.0f;
    particles_stream.param.mul_acc = +5.0f;
    particles_stream.param.velocity_damping = 0.30f;
    
    particles_stream.param.acc_minmax[1] = 120;
    particles_stream.param.vel_minmax[1] = 120;
    
    particles_stream.param.size_display   = 10;
    particles_stream.param.size_collision = 0;
    particles_stream.param.size_cohesion  = 0;
    
    particles_stream.param.mul_coh = 0.00f;
    particles_stream.param.mul_col = 0.00f;
    particles_stream.param.mul_obs = 0.00f;
    
    particles_stream.param.wh_scale_col = 0;
    particles_stream.param.wh_scale_coh = 5;
    particles_stream.param.wh_scale_obs = 0;
    
    resizeScene();
    
    createGUI();

    frameRate(1000);
  }
  

  public void resizeScene(){
    
    if(pg_canvas != null && width == pg_canvas.width && height == pg_canvas.height){
      return;
    }

    pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
    pg_canvas.smooth(0);
    
    
    pg_tmp = (PGraphics2D) createGraphics(width, height, P2D);
    pg_tmp.smooth(0);
    
    DwUtils.changeTextureFormat(pg_canvas, GL3.GL_RGBA16F, GL3.GL_RGBA, GL3.GL_FLOAT, GL3.GL_LINEAR);
    DwUtils.changeTextureFormat(pg_tmp   , GL3.GL_RGBA16F, GL3.GL_RGBA, GL3.GL_FLOAT, GL3.GL_LINEAR);
    
    pg_canvas.beginDraw();
    pg_canvas.endDraw();

    pg_impulse = (PGraphics2D) createGraphics(width, height, P2D);
    pg_impulse.smooth(0);
  
    DwUtils.COL_TL = new float[]{   0,  0,   0, 255};
    DwUtils.COL_TR = new float[]{  32, 64, 128, 255};
    DwUtils.COL_BL = new float[]{ 128, 64,  32, 255};
    DwUtils.COL_BR = new float[]{  0,   0,   0, 255};
   
    pg_noise = DwUtils.createBackgroundNoiseTexture(this, width/2, height/2);
    
    resetScene();
  }
  
  public void resetScene(){
    pg_canvas.beginDraw();
    pg_canvas.clear();
    pg_canvas.endDraw();
  }
  

  
  //////////////////////////////////////////////////////////////////////////////
  //
  // DRAW
  //
  //////////////////////////////////////////////////////////////////////////////

  
  public void addImpulse(){
    
    APPLY_IMPULSE = mousePressed && !cp5.isMouseOver();
    
    final int MID = 127;
    float mx, my, pmx, pmy, vx, vy;
    
    pg_impulse.beginDraw();
    pg_impulse.background(MID, MID, MID);

    
    float mult = 200f / impulse_radius;
    
    // mouse position and velocity
     mx =  mouseX;  my =  mouseY;
    pmx = pmouseX; pmy = pmouseY;
    vx = (mx - pmx) * +impulse_mul * mult;
    vy = (my - pmy) * -impulse_mul * mult; // flip vertically

    // clamp velocity
    float vv_sq = vx*vx + vy*vy;
    float vv_sq_max = impulse_max*impulse_max;
    if(vv_sq > vv_sq_max && vv_sq > 0.0){
      float vv = sqrt(vv_sq);
      vx = impulse_max * vx / vv;
      vy = impulse_max * vy / vv;
    }
    
    // map velocity, to UNSIGNED_BYTE range
    vx = map(vx, -impulse_max, +impulse_max, 0, MID<<1);
    vy = map(vy, -impulse_max, +impulse_max, 0, MID<<1);
    
    // render impulse
    if(APPLY_IMPULSE){
      pg_impulse.noStroke();
      pg_impulse.fill(vx, vy, MID);
      pg_impulse.ellipse(mx, my, impulse_radius, impulse_radius);
    }

    pg_impulse.endDraw();
    
    
    // create impulse texture
    ff_impulse.resize(width, height);
    {
      Merge.TexMad ta = new Merge.TexMad(ff_impulse.tex_vel, impulse_tsmooth, 0);
      Merge.TexMad tb = new Merge.TexMad(pg_impulse,  1, -MID/255f);
      DwFilter.get(context).merge.apply(ff_impulse.tex_vel, ta, tb);
      ff_impulse.blur();
    }
  }
  

 

  public void draw(){
    
    resizeScene();
    
    addImpulse();
    
    resetScene();

    if(DISPLAY_MODE == 0){
      ff_impulse.displayPixel(pg_canvas);
      ff_impulse.displayLines(pg_canvas);
    }
    
    if(DISPLAY_MODE == 1){
      ff_impulse.displayLineIntegralConvolution(pg_canvas, pg_noise);
    }
    
    
    if(DISPLAY_STREAMLINES || DISPLAY_PARTICLES){ 

      int iterations = STREAMLINE_SAMPLES;
      
      int dim_x = width;
      int dim_y = height;
      
      int num_x = ceil(dim_x / STREAMLINE_RES);
      int num_y = ceil(dim_y / STREAMLINE_RES);

      DwFlowFieldParticles.SpawnRect sr = new DwFlowFieldParticles.SpawnRect();
      sr.num = new int[]{num_x, num_y};
      sr.dim = new float[]{dim_x, dim_y};
      sr.pos = new float[]{0,0};
      sr.vel = new float[]{0,0};
      
      particles_stream.resizeParticlesCount(num_x, num_y);
      particles_stream.resizeWorld(dim_x, dim_y);
      particles_stream.reset();
      particles_stream.spawn(dim_x, dim_y, sr);
      
      particles_stream.param.shader_type = 0;
      particles_stream.param.size_display = (int) max(2, (0.55f * min(dim_x/(float)num_x, dim_y/(float)num_y)));

//      float mul_vec = particles_stream.param.velocity_damping;
//      float mul_acc = particles_stream.param.mul_acc;
//      float mul_coh = particles_stream.param.mul_coh;
//      float mul_col = particles_stream.param.mul_col;
//      float mul_obs = particles_stream.param.mul_obs;
//
//      particles_stream.param.mul_coh = 0;
//      particles_stream.param.mul_col = 0;
//      particles_stream.param.mul_obs = 0;
//      
//      if(keyPressed && key =='w')
//      {
//        int warmup = min(5, iterations / 5);
//        for(int i = 0; i < warmup; i++){
//          particles_stream.param.velocity_damping = (1.0f - i / (float)(warmup-1)) * 2.0f;
//          particles_stream.update(ff_impulse);
//        }
//      }
//      
//      particles_stream.param.velocity_damping =  mul_vec;
//      particles_stream.param.mul_acc          = -mul_acc;
//      particles_stream.param.mul_coh          = mul_coh;      
//      particles_stream.param.mul_col          = mul_col;      
//      particles_stream.param.mul_obs          = mul_obs;      
      
      
      float s1 = 0.40f;
      float s2 = 0.40f;
      float[] col_A = {0.25f*s1, 0.50f*s1, 1.00f*s1, 1.00f};
      float[] col_B = {1.00f*s2, 0.10f*s2, 0.50f*s2, 0.50f};
      

      float step = 1f / iterations;
      for(int i = 0; i < iterations; i++){
        if(BLUR_ITERATIONS && i > 0){
          DwFilter.get(context).gaussblur.apply(pg_canvas, pg_canvas, pg_tmp, 1);
//          float mult = 0.95f;
//          DwFilter.get(context).multiply.apply(pg_canvas, pg_canvas, new float[]{mult,mult,mult,mult});
        }
        
        particles_stream.update(ff_impulse);
        
        float line_uv = 1 - i * step;
        line_uv = (float) Math.pow(line_uv, 2);
        DwUtils.mix(col_A, col_B, line_uv, particles_stream.param.col_A);
               
        if(DISPLAY_STREAMLINES){
          particles_stream.displayTrail(pg_canvas);
        }
        
        if(DISPLAY_PARTICLES){
//          particles_stream.displayParticles(pg_canvas);
          float p1 = 2.0f;
          float p2 = 1f;
//          particles_stream.param.col_A = new float[]{0.20f*p1,0.02f*p1,0.00f*p1,0.50f};
//          particles_stream.param.col_B = new float[]{0.00f*p2,0.00f*p2,0.00f*p2,0.00f};
          particles_stream.param.col_A = new float[]{0.20f*p1,0.05f*p1,0.02f*p1,0.50f};
          particles_stream.param.col_B = new float[]{0.00f*p2,0.00f*p2,0.00f*p2,0.00f};
          particles_stream.displayParticles(pg_canvas);
        }
        

      }
      
      
      
//      particles_stream.param.mul_acc          = mul_acc;
//      particles_stream.param.velocity_damping = mul_vec;
    }
    

    
    
    
    
    
       
    blendMode(REPLACE); 
    image(pg_canvas, 0, 0);
    blendMode(BLEND);
    
    if(APPLY_IMPULSE){
      strokeWeight(3);
      stroke(255,64);
      noFill();
      ellipse(mouseX, mouseY, impulse_radius, impulse_radius);
    }
    
    info();
  }
  

  void info(){
    String txt_app = getClass().getSimpleName();
    String txt_device = context.gl.glGetString(GL3.GL_RENDERER).trim().split("/")[0];
    String txt_fps = String.format(Locale.ENGLISH, "[%s]  [%s]  [%d/%d]  [%7.2f fps]", 
                     txt_app, txt_device, pg_canvas.width, pg_canvas.height, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public void keyReleased(){
    if(key == 'r') reset();
    if(key == 'h') toggleGUI();
    if(key >= '1' && key <= '9') DISPLAY_MODE = key - '1';
  }
  
  public void reset(){
    ff_impulse.reset();
    particles_stream.reset();
    resetScene();
  }
  
  public void setDisplayType(int val){
    DISPLAY_MODE = val;
  }
  
  public void setLicStates(float[] val){
    ff_impulse.param_lic.TRACE_BACKWARD    = val[0] > 0;
    ff_impulse.param_lic.TRACE_FORWARD     = val[1] > 0;
  }
  
  public void setDisplayStreamLine(float[] val){
    DISPLAY_STREAMLINES = val[0] > 0;
    DISPLAY_PARTICLES   = val[1] > 0;
  }
  
  public void toggleGUI(){
    if(cp5.isVisible()) cp5.hide(); else cp5.show();
  }

  
  public void setStreamLineStates(float[] val){
    particles_stream.param.display_line_smooth    = val[0] > 0;
    BLUR_ITERATIONS                               = val[1] > 0;
  }
  
  
  
  float mult_fg = 1f;
  float mult_active = 2f;
  float CR = 96;
  float CG = 64;
  float CB = 8;
  int col_bg, col_fg, col_active;
  
  ControlP5 cp5;
  
  public void createGUI(){
    
    col_bg     = color(4, 220);
    col_fg     = color(CR*mult_fg, CG*mult_fg, CB*mult_fg);
    col_active = color(CR*mult_active, CG*mult_active, CB*mult_active);
    
    int col_group = color(8,64);
    
    CColor theme = ControlP5.getColor();
    theme.setForeground(col_fg);
    theme.setBackground(col_bg);
    theme.setActive(col_active);

    cp5 = new ControlP5(this);
    cp5.setAutoDraw(true);
    
    int sx, sy, px, py;
    sx = 100; 
    sy = 14; 

    int dy_group = 20;
    int dy_item = 4;
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - LIC
    ////////////////////////////////////////////////////////////////////////////
    Group group_lic = cp5.addGroup("Line Integral Convolution");
    {
      group_lic.setHeight(20).setSize(gui_w, 330)
      .setBackgroundColor(col_group).setColorBackground(col_group);
      group_lic.getCaptionLabel().align(CENTER, CENTER);
      
      px = 15; py = 15;
      
      cp5.addButton("reset").setGroup(group_lic).plugTo(this, "reset").setSize(80, 18).setPosition(px, py);

      
      {
        py += 1 * sy + dy_group;
        int count = 2;
        sx = (gui_w-30 - 2 * (count-1)) / count;
        RadioButton rb_type = cp5.addRadio("setDisplayType").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(count).plugTo(this, "setDisplayType")
          .setNoneSelectedAllowed(true)
          .addItem("flow field", 0)
          .addItem("LIC"       , 1)
          .activate(DISPLAY_MODE);
  
        for(Toggle toggle : rb_type.getItems()) toggle.getCaptionLabel().alignX(CENTER).alignY(CENTER);
        py += sy + dy_group;
      }
      
      DwFlowField.ParamLIC param = ff_impulse.param_lic;
      

      cp5.addSlider("iterations").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(1, 20).setValue(param.iterations).plugTo(param, "iterations");
      py += sy + dy_item;
      
      cp5.addSlider("samples").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(1, 120).setValue(param.num_samples).plugTo(param, "num_samples");
      py += sy + dy_item;
      
      cp5.addSlider("blur radius").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 10).setValue(ff_impulse.param.blur_radius).plugTo(ff_impulse.param, "blur_radius");
      py += sy + dy_item;
      
      cp5.addSlider("acc_mult").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 5).setValue(param.acc_mult).plugTo(param, "acc_mult");
      py += sy + dy_item;
      
      cp5.addSlider("vel_mult").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 3).setValue(param.vel_mult).plugTo(param, "vel_mult");
      py += sy + dy_item;
      
      cp5.addSlider("intensity_exp").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0.0f, 3f).setValue(param.intensity_exp).plugTo(param, "intensity_exp");
      py += sy + dy_item;
      
      cp5.addSlider("intensity_mult").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0.0f, 2.5f).setValue(param.intensity_mult).plugTo(param, "intensity_mult");
      py += sy + dy_group;
      
      cp5.addCheckBox("setLicStates").setGroup(group_lic).setSize(sy,sy).setPosition(px, py)
      .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
      .addItem("TRACE BACKWARD", 0).activate(param.TRACE_BACKWARD ? 0 : 2)
      .addItem("TRACE FORWARD" , 1).activate(param.TRACE_FORWARD  ? 1 : 2)
      ; 
      
      int count = 2;
      py += sy * count + 2 * (count-1) + dy_group;
      cp5.addSlider("brush").setGroup(group_lic).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 500).setValue(this.impulse_radius).plugTo(this, "impulse_radius");
      py += sy + dy_item;
  
    }
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - STREAMLINES
    ////////////////////////////////////////////////////////////////////////////
    Group group_streamlines = cp5.addGroup("StreamLines");
    {
      group_streamlines.setHeight(20).setSize(gui_w, 200)
      .setBackgroundColor(col_group).setColorBackground(col_group);
      group_streamlines.getCaptionLabel().align(CENTER, CENTER);
      
      px = 15; py = 15;
      
      int count = 1;
      cp5.addCheckBox("setDisplayStreamLine").setGroup(group_streamlines).setSize(sy, sy).setPosition(px, py)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(count).plugTo(this, "setDisplayStreamLine")
        .addItem("STREAMLINE", 0).activate(DISPLAY_STREAMLINES ? 0 : 2)
        .addItem("PARTICLES" , 1).activate(DISPLAY_PARTICLES   ? 1 : 2)
        ;
      
      py += 2 * sy + dy_group;
  
      DwFlowFieldParticles.Param param = particles_stream.param;
      
      cp5.addSlider("StreamLine.samples").setLabel("samples").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
      .setRange(5, 100).setValue(STREAMLINE_SAMPLES).plugTo(this, "STREAMLINE_SAMPLES");
      py += sy + dy_item;
      
      cp5.addSlider("StreamLine.res").setLabel("resolution").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
      .setRange(3, 20).setValue(STREAMLINE_RES).plugTo(this, "STREAMLINE_RES");
      py += sy + dy_item;
        
      cp5.addSlider("StreamLine.vel_mult").setLabel("vel_mult").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 1).setValue(param.velocity_damping).plugTo(param, "velocity_damping");
      py += sy + dy_item;
      
      cp5.addSlider("StreamLine.acc_mult").setLabel("acc_mult").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
      .setRange(-10, 10).setValue(param.mul_acc).plugTo(param, "mul_acc");
      py += sy + dy_group;
      
      
//      cp5.addSlider("StreamLine.size_collision").setLabel("size_collision").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
//      .setRange(0, 20).setValue(param.size_collision).plugTo(param, "size_collision");
//      py += sy + dy_item;
//      
//      cp5.addSlider("StreamLine.size_cohesion").setLabel("size_cohesion").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
//      .setRange(0, 50).setValue(param.size_cohesion).plugTo(param, "size_cohesion");
//      py += sy + dy_group;
      
      
      
      
      cp5.addCheckBox("setStreamLineStates").setGroup(group_streamlines).setSize(sy,sy).setPosition(px, py)
      .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
      .addItem("smooth lines", 0).activate(param.display_line_smooth ? 0 : 2)
      .addItem("blur lines"  , 0).activate(BLUR_ITERATIONS           ? 1 : 2)
      ; 
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_lic)
      .addItem(group_streamlines)
      .open()
      ;
    

  }
  
  
  
  
  
  
  
  
  
 
  public static void main(String args[]) {
    PApplet.main(new String[] { FlowField_LIC_StreamLines.class.getName() });
  }
  
  
}