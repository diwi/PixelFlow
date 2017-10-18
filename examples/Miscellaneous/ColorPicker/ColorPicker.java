/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Miscellaneous.ColorPicker;


import com.thomasdiewald.pixelflow.java.utils.DwColorPicker;
import processing.core.*;



public class ColorPicker extends PApplet {
  
  //
  //
  // Simple ColorPicker.
  // 
  // just Extend and Override it to create your own style.
  // e.g. displayHUD();
  // 
  //
  
  public DwColorPicker cp1;
  public DwColorPicker cp2;
  public DwColorPicker cp3;
  public DwColorPicker cp4;
  public DwColorPicker cp5;
  
  PFont font;
  
  public void settings(){
    size(1000, 700, P2D);
  }
  
  public void setup(){
    surface.setLocation(210, 0);
    
    cp1 = new DwColorPicker(this, 10, 10, width-20, 150);
    cp2 = new DwColorPicker(this, 10, 220, 180, 400);
    cp3 = new DwColorPicker(this, 240, 240, 600, 80, 20);
    cp4 = new DwColorPicker(this, 240, 400, 240, 240, 20);
    cp5 = new DwColorPicker(this, 540, 500, 400, 20, 20);
    
    cp1.createPallette(361, 5);
    cp2.createPallette(13, 200);
    cp3.createPallette(8); 
    cp4.createPallette(361,240);
    
    cp1.selectColorByGrid(cp1.getNX()/2, cp1.getNY()/2);
    cp2.selectColorByRGB(255, 255, 125);
    cp3.selectColorByNormalizedCoords(0.5f, 0.5f);
    cp4.selectColorByRGB(128,128,128);
    
    font = createFont("../data/SourceCodePro-Regular.ttf", 12);
    textFont(font);
    
    frameRate(1000);
  }

  public void draw(){
    
    background(0);
    
    if(DwColorPicker.LAST_USED != null){
      noStroke();
      fill(DwColorPicker.LAST_USED.getSelectedColor());
      rect(0, height, width, -10);
    }

    // info
    String txt_fps = String.format(getClass().getName()+ "  [fps %6.2f]", frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { ColorPicker.class.getName() });
  }
  
  
}