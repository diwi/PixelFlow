/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package ColorPicker;



import java.util.Locale;

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
  PFont font;
  
  public void settings(){
    size(800, 500, P2D);
  }
  
  public void setup(){
    surface.setLocation(210, 0);
    
    cp1 = new DwColorPicker(this, 10, 10, width-20, 150, 5);
    cp2 = new DwColorPicker(this, 10, height/2+10, 180, 80, 100);
    cp3 = new DwColorPicker(this, width/2, height/2+10, 180, 80, 2);
    
    cp1.selectColorByCoords(cp1.getNumColorsX()/2, cp1.getNumColorsY()/2);
    cp2.selectColorByRGB(255, 255, 125);
    cp3.selectColorByNormalizedCoords(0.5f, 0.5f);
    
    font = createFont("SourceCodePro-Regular.ttf", 12);
    textFont(font);
    
    frameRate(1000);
  }

  public void draw(){
    
    background(0);
    
    noStroke();
    fill(DwColorPicker.LAST_USED.getSelectedColor());
    rect(0, height, width, -10);

    // info
    String txt_fps = String.format(getClass().getName()+ "  [fps %6.2f]", frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { ColorPicker.class.getName() });
  }
  
  
}