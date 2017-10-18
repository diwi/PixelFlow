/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Miscellaneous.ColorPicker_ControlP5;



import com.thomasdiewald.pixelflow.java.utils.DwColorPicker;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ControllerView;
import controlP5.Group;
import controlP5.Pointer;

import processing.core.*;



public class ColorPicker_ControlP5 extends PApplet {
  
  //
  // DwColorPicker as a ControlP5 controller
  // 

  PFont font;
  
  int[] rgb0 =  {255,125,0};
  int[] rgb1 =  {0,125,255};
  
  int circle0_radius = 150;
  int circle1_radius = 200;
  
  public void settings(){
//    size(600, 600, JAVA2D);
    size(600, 600, P2D);
    smooth(8);
  }
  
  public void setup(){
    frameRate(1000);
    
    createGUI();
  }

  public void draw(){
    background(64);
    
    strokeWeight(10);

    
    pushMatrix();
    translate(width/2, height/2);
    stroke(0);
    fill(rgb0[0], rgb0[1], rgb0[2]);
    ellipse(0, -100, circle0_radius, circle0_radius);
    
    stroke(255);
    fill(rgb1[0], rgb1[1], rgb1[2]);
    ellipse(0, +100, circle1_radius, circle1_radius);
    popMatrix();
  }
  

  int gui_w = 160;
  int hui_h = 350;
  ControlP5 cp5;
  
  public void createGUI(){
    
    cp5 = new ControlP5(this);

    int col_group = color(16,220);
    
    // GROUP
    Group group = cp5.addGroup("ColorPicker");
    group.setHeight(20).setSize(gui_w, hui_h).setBackgroundColor(col_group).setColorBackground(col_group);
    group.getCaptionLabel().align(CENTER, CENTER);
    
    int sx = 100;
    int sy = 14;
    int px = 10;
    int py = 25;
    int dy = 2;
    
    cp5.addSlider("radius 0").setGroup(group).setSize(sx, sy).setPosition(px, py)
      .setRange(50, 500).setValue(circle0_radius).plugTo(this, "circle0_radius");
    py += sy + dy;
        
    cp5.addSlider("radius 1").setGroup(group).setSize(sx, sy).setPosition(px, py)
      .setRange(50, 500).setValue(circle1_radius).plugTo(this, "circle1_radius");
    py += sy + 30;
      
    sx = gui_w - 20;
    sy = 60;
    ColorPicker cp1 = new ColorPicker(cp5, "colorpicker1", sx, sy, 100, rgb0).setGroup(group).setPosition(px, py);
    py += sy + 30;
    
    sy = sx;
    ColorPicker cp2 = new ColorPicker(cp5, "colorpicker2", sx, sy, 5, rgb1).setGroup(group).setPosition(px, py);
    py += sy + dy;
    

    cp2.createPallette(20);

    // ACCORDION
    cp5.addAccordion("acc").setPosition(20, 20).setSize(gui_w, hui_h)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group)
      .open(0, 1);
   
  }
  


  
  /**
   * Creating a new cp5-controller for PixelFlows colorpicker.
   */
  static class ColorPicker extends Controller<ColorPicker> {
    ControlP5 cp5;
    DwColorPicker colorpicker;
    Pointer ptr = getPointer();
    int[] rgb;
    
    int hud_sy = 16;

    ColorPicker(ControlP5 cp5, String theName, int dim_x, int dim_y, int ny, int[] rgb) {
      super(cp5, theName);

      setSize(dim_x, dim_y);
      this.cp5 = cp5;
      this.rgb = rgb;
      this.colorpicker = new DwColorPicker(cp5.papplet, 0, 0, dim_x, dim_y-hud_sy);
      this.colorpicker.setAutoDraw(false);
      this.colorpicker.setAutoMouse(false);
      createPallette(ny);
      
      setView(new ControllerView<ColorPicker>() {
        public void display(PGraphics pg, ColorPicker cp) {
          
          int dim_x = getWidth();
          int dim_y = getHeight();
          
          int    cp_col = colorpicker.getSelectedColor();
          String cp_rgb = colorpicker.getSelectedRGBasString();
          // String cp_hsb = colorpicker.getSelectedHSBasString();

          int sy = hud_sy;
          int px = 0;
          int py = colorpicker.h()+1;
          
          pg.noStroke();
          pg.fill(200, 50);
          pg.rect(px-1, py, dim_x+2, sy+1);
          pg.fill(cp_col);
          pg.rect(px, py, sy, sy);
          
          pg.fill(255);
          pg.text(cp_rgb, px + sy * 2, py+8);
//          pg.text(cp_hsb, px + sy * 2, py+8);

          colorpicker.display();
        }
      });
    }
    
    public ColorPicker createPallette(int shadesY){
      colorpicker.createPallette(shadesY);
      colorpicker.selectColorByRGB(rgb[0], rgb[1], rgb[2]);
      return this;
    }
    
    public ColorPicker createPallette(int shadesX, int shadesY){
      colorpicker.createPallette(shadesX, shadesY);
      colorpicker.selectColorByRGB(rgb[0], rgb[1], rgb[2]);
      return this;
    }

    
    boolean STATE_SELECT = false;
    public void selectColor(){
      if(STATE_SELECT){
        colorpicker.selectColorByCoords(ptr.x(), ptr.y());
        int[] selected = colorpicker.getSelectedRGBColor();
        rgb[0] = selected[0];
        rgb[1] = selected[1];
        rgb[2] = selected[2];
      }
    }

    protected void onPress() {
      STATE_SELECT = colorpicker.inside(ptr.x(), ptr.y());
      selectColor();
    }
    protected void onDrag() {
      selectColor();
    }
    protected void onRelease( ) {
      selectColor();
      STATE_SELECT = false;
    }

  }
    
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { ColorPicker_ControlP5.class.getName() });
  }
  
  
}