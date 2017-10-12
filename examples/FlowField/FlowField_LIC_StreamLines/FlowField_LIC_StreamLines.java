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
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles.SpawnRect;
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
  
  boolean START_FULLSCREEN = !true;
  
  int viewport_w = 1680;
  int viewport_h = 1024;
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
  
  int     STREAMLINE_SAMPLES = 15;
  int     DISPLAY_MODE  = 1;
  boolean APPLY_IMPULSE = false;
  boolean DISPLAY_FLOWFIELD_STREAM = true;
  
  float impulse_max = 256;
  float impulse_mul = 10;
  float impulse_tsmooth = 1f;
  int   impulse_radius = 180;

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
    
    ff_impulse.param_lic.iterations     = 4;
    ff_impulse.param_lic.num_samples    = 30;
    ff_impulse.param_lic.acc_mult       = 1.00f;
    ff_impulse.param_lic.vel_mult       = 0.8999f;
    ff_impulse.param_lic.intensity_mult = 1.10f;
    ff_impulse.param_lic.intensity_exp  = 1.20f;
    ff_impulse.param_lic.TRACE_BACKWARD = true;
    ff_impulse.param_lic.TRACE_FORWARD  = false;
    
    particles_stream = new DwFlowFieldParticles(context);
    
    particles_stream.param.blend_mode = 1;
    particles_stream.param.shader_collision_mult = 0.0f;
    particles_stream.param.display_line_width  = 0.5f;
    particles_stream.param.display_line_smooth = !true;
    particles_stream.param.steps = 1;
    particles_stream.param.mul_obs =  0.0f;
    particles_stream.param.mul_col =  0.0f;
    particles_stream.param.mul_coh =  0.0f;
    particles_stream.param.mul_acc = -10.0f;
    particles_stream.param.velocity_damping = 0.10f;
    particles_stream.param.acc_minmax[1] = 120;
    particles_stream.param.vel_minmax[1] = 120;
    
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

    pg_canvas.beginDraw();
    pg_canvas.endDraw();

    pg_impulse = (PGraphics2D) createGraphics(width, height, P2D);
    pg_impulse.smooth(0);
  
    DwUtils.COL_TL = new float[]{  0,  0,  0, 255};
    DwUtils.COL_TR = new float[]{255,128,  0, 255};
    DwUtils.COL_BL = new float[]{  0,128,255, 255};
    DwUtils.COL_BR = new float[]{255,255,255, 255};
   
    pg_noise = DwUtils.createBackgroundNoiseTexture(this, width/2, height/2);
    
    pg_noise = (PGraphics2D) createGraphics(width, height, P2D);
    int dimx = pg_noise.width; 
    int dimy = pg_noise.height; 
    pg_noise.beginDraw();
    pg_noise.clear();
    pg_noise.blendMode(BLEND);
    
    int num_points = dimx * dimy / (3*3);
    for(int i = 0; i < num_points; i++){
      float x = random(0, dimx-1);
      float y = random(0, dimy-1);
      
      float r = random(4);
      r = 1;
      
      pg_noise.noStroke();
      pg_noise.fill(128 * r  , 128 * r, 128 * r);
      pg_noise.rect(x, y, 2, 2);
      
    }
    pg_noise.endDraw();

    resetScene();
  }
  
  public void resetScene(){
    pg_canvas.beginDraw();
    pg_canvas.image(pg_noise, 0, 0, pg_canvas.width, pg_canvas.height);
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

    // mouse position and velocity
     mx =  mouseX;  my =  mouseY;
    pmx = pmouseX; pmy = pmouseY;
    vx = (mx - pmx) * +impulse_mul;
    vy = (my - pmy) * -impulse_mul; // flip vertically

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
    

    
    if(DISPLAY_FLOWFIELD_STREAM){ 

      int iterations = STREAMLINE_SAMPLES;
      
      int dim_x = width;
      int dim_y = height;
      
      int num_x = ceil(dim_x / (iterations * 0.5f));
      int num_y = ceil(dim_y / (iterations * 0.5f));

      SpawnRect sr = new SpawnRect();
      sr.num = new int[]{num_x, num_y};
      sr.dim = new float[]{dim_x, dim_y};
      sr.pos = new float[]{0,0};
      sr.vel = new float[]{0,0};
      
      particles_stream.resizeParticlesCount(num_x, num_y);
      particles_stream.resizeWorld(dim_x, dim_y);
      particles_stream.spawn(dim_x, dim_y, sr);
      
      float mul_vec = particles_stream.param.velocity_damping;
      float mul_acc = particles_stream.param.mul_acc;
    
      particles_stream.param.mul_acc = +mul_acc;
      int warmup = min(5, iterations / 5);
      for(int i = 0; i < warmup; i++){
        particles_stream.param.velocity_damping = 1.0f - i / (float)(warmup);
        particles_stream.update(ff_impulse);
      }
      particles_stream.param.velocity_damping = mul_vec;
      particles_stream.param.mul_acc = -mul_acc;
        
      float s = 0.05f;
      float[] col_A = {0.25f  , 0.50f  , 1.00f  , 0.00f};
      float[] col_B = {1.00f*s, 0.30f*s, 0.10f*s, 1.00f};

      float step = 1f / iterations;
      for(int i = 0; i < iterations; i++){
        float line_uv = i * step;
        line_uv = (float) Math.pow(line_uv, 2);
        DwUtils.mix(col_A, col_B, line_uv, particles_stream.param.col_A);

        particles_stream.update(ff_impulse);
        particles_stream.displayTrail(pg_canvas);
      }
      
      
      particles_stream.param.mul_acc          = mul_acc;
      particles_stream.param.velocity_damping = mul_vec;
    }
    
    DwFilter.get(context).gaussblur.apply(pg_canvas, pg_canvas, pg_tmp, 1);
    
    
    
    
    
       
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
    resetScene();
  }
  
  public void setDisplayType(int val){
    DISPLAY_MODE = val;
  }
  
  public void setLicStates(float[] val){
    ff_impulse.param_lic.TRACE_BACKWARD    = val[0] > 0;
    ff_impulse.param_lic.TRACE_FORWARD     = val[1] > 0;
  }
  
  public void setDisplayStreamLine(int val){
    DISPLAY_FLOWFIELD_STREAM = val != -1;
  }
  
  public void toggleGUI(){
    if(cp5.isVisible()) cp5.hide(); else cp5.show();
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
      .setRange(0.5f, 2.5f).setValue(param.intensity_mult).plugTo(param, "intensity_mult");
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
      group_streamlines.setHeight(20).setSize(gui_w, 130)
      .setBackgroundColor(col_group).setColorBackground(col_group);
      group_streamlines.getCaptionLabel().align(CENTER, CENTER);
      
      px = 15; py = 15;
      
      int count = 1;
      cp5.addRadio("setDisplayStreamLine").setGroup(group_streamlines).setSize(sy, sy).setPosition(px, py)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(count).plugTo(this, "setDisplayStreamLine")
        .setNoneSelectedAllowed(true)
        .addItem("STREAMLINE", 1)
        .activate(DISPLAY_FLOWFIELD_STREAM ? 0 : 1);
      
      py += 1 * sy + dy_group;
  
      DwFlowFieldParticles.Param param = particles_stream.param;
      
      cp5.addSlider("StreamLine.samples").setLabel("samples").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
      .setRange(5, 50).setValue(STREAMLINE_SAMPLES).plugTo(this, "STREAMLINE_SAMPLES");
      py += sy + dy_item;
        
      cp5.addSlider("StreamLine.vel_mult").setLabel("vel_mult").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 1).setValue(param.velocity_damping).plugTo(param, "velocity_damping");
      py += sy + dy_item;
      
      cp5.addSlider("StreamLine.acc_mult").setLabel("acc_mult").setGroup(group_streamlines).setSize(sx, sy).setPosition(px, py)
      .setRange(-20, 20).setValue(param.mul_acc).plugTo(param, "mul_acc");
      py += sy + dy_item;
      
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