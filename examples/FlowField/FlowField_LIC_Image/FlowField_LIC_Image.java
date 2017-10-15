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



package FlowField.FlowField_LIC_Image;

import java.util.Locale;

import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
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


public class FlowField_LIC_Image extends PApplet {
  
  //
  // Line Integral Convolution (LIC) Shader
  //
  // LIC is a  low-pass filter, for sampling and averaging  samples along the 
  // streamlines of a flowfield (vector field).
  //
  // In this demo the flowfield is created by some simply mouse-drawing.
  // The background texture is loaded from an external image by M.C. Escher.
  //
  //
  
  
  boolean START_FULLSCREEN = !true;
  
  int viewport_w = 1680;
  int viewport_h = 1024;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 10;
  int gui_y = 10;
  
  PGraphics2D pg_canvas;
  PGraphics2D pg_source;
  PGraphics2D pg_impulse;
  PGraphics2D pg_noise;

  DwPixelFlow context;
  
  DwFlowField ff_impulse;
  
  PImage img;
  
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
    
    //img = loadImage("data/Mona_Lisa_1024.jpg");
    img = loadImage("data/mc_escher.jpg");
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    ff_impulse = new DwFlowField(context);
    ff_impulse.param.blur_iterations = 1;
    ff_impulse.param.blur_radius     = 1;
    
    ff_impulse.param_lic.iterations = 2;
    ff_impulse.param_lic.num_samples = 40;
    ff_impulse.param_lic.acc_mult = 1f;
    ff_impulse.param_lic.vel_mult = 1f;
    ff_impulse.param_lic.TRACE_BACKWARD = true;
    ff_impulse.param_lic.TRACE_FORWARD  = !true;
    
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
    pg_canvas.beginDraw();
    pg_canvas.endDraw();
    
    pg_source = (PGraphics2D) createGraphics(width, height, P2D);
    pg_source.smooth(0);
    pg_source.beginDraw();
    pg_source.endDraw();
    
    pg_impulse = (PGraphics2D) createGraphics(width, height, P2D);
    pg_impulse.smooth(0);
    
    DwUtils.COL_TL = new float[]{255,128,0, 255};
    DwUtils.COL_TR = new float[]{0,0,0, 255};
    DwUtils.COL_BL = new float[]{  0,0,0, 255};
    DwUtils.COL_BR = new float[]{255, 255, 255, 255};
    DwUtils.COL_CC = new float[]{  0,  0,  0,   0};
    
    pg_noise = DwUtils.createBackgroundNoiseTexture(this, width/2, height/2);
    
    resetScene();
  }
  

  
  
  public void resetScene(){
    
    int dimx = pg_source.width;
    int dimy = pg_source.height;
    
    float ratiox = dimx  / (float) img.width;
    float ratioy = dimy / (float) img.height;
    
    float ratio = min(ratiox, ratioy);
    
    pg_source.beginDraw();
    pg_source.image(pg_noise, 0, 0, dimx, dimy);
    
    pg_source.pushMatrix();
    pg_source.translate(dimx/2, dimy/2);
    pg_source.scale(ratio);
    pg_source.translate(-img.width/2, -img.height/2);
    pg_source.image(img, 0, 0);
    pg_source.popMatrix();
    

//    int num_points = dimx * dimy / 40;
//    for(int i = 0; i < num_points; i++){
//      float x = random(0, dimx-1);
//      float y = random(0, dimy-1);
//      pg_source.fill(0, random(255));
//      pg_source.rect(x, y, 1, 1);
//    }
    
    pg_source.endDraw();
  }
  


  
  //////////////////////////////////////////////////////////////////////////////
  //
  // DRAW
  //
  //////////////////////////////////////////////////////////////////////////////
  boolean APPLY_IMPULSE = false;
  float impulse_max = 256;
  float impulse_mul = 10;
  float impulse_tsmooth = 1f;
  int   impulse_radius = 200;
  
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
  

  

  public int DISPLAY_MODE = 1;
  

  public void draw(){
    
    resizeScene();
    
    addImpulse();
    
    pg_canvas.beginDraw();
    pg_canvas.image(pg_source, 0, 0);
    pg_canvas.endDraw();
    
    if(DISPLAY_MODE == 0){
      ff_impulse.displayPixel(pg_canvas);
      ff_impulse.displayLines(pg_canvas);
    }
    
    if(DISPLAY_MODE == 1){
      ff_impulse.displayLineIntegralConvolution(pg_canvas, pg_source);
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
    resetScene();
  }
  
  
  public void setDisplayType(int val){
    DISPLAY_MODE = val;
  }
  
  public void setLicStates(float[] val){
    ff_impulse.param_lic.TRACE_BACKWARD    = val[0] > 0;
    ff_impulse.param_lic.TRACE_FORWARD     = val[1] > 0;
  }
  
  public void toggleGUI(){
    if(cp5.isVisible()) cp5.hide(); else cp5.show();
  }

  
  ControlP5 cp5;
  
  float mult_fg = 1f;
  float mult_active = 2f;
  float CR = 96;
  float CG = 64;
  float CB = 8;
  int col_bg, col_fg, col_active;
  
  
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
      .setRange(0.5f, 1.5f).setValue(param.intensity_mult).plugTo(param, "intensity_mult");
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
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_lic)
      .open()
      ;
    

  }
  
  
  
 
  public static void main(String args[]) {
    PApplet.main(new String[] { FlowField_LIC_Image.class.getName() });
  }
  
  
}