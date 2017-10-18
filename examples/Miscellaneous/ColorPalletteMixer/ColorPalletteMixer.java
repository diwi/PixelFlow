/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Miscellaneous.ColorPalletteMixer;


import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.core.*;


public class ColorPalletteMixer extends PApplet {
  
  // Example for getting a smooth color from a discrete color pallette.

  public void settings(){
    size(1000, 100, P2D);
  }
  
  public void setup(){
    surface.setLocation(210, 0);

    float[][] pallette = {
        {   0,   0,   0},
        { 255,   0,   0},  
        {   0,   0, 255},  
        {   0, 255,   0},
        {   0, 255, 128},  
        { 255,   0, 255},
        { 255, 255,   0},
        { 255, 128,  32},
        {  32, 128, 255},
        {   0,   0,   0},
    };
    
    float[] rgb = new float[3];

    
    int count = width/5;
    for(int i = 0; i <= count; i++){
      
      float sx = width / (float)count;
      float px = sx * i;
      
      float val_norm = px / width;

      rgb = DwUtils.getColor(pallette, val_norm, rgb);
      
      rectMode(CENTER);
      noStroke();
      fill(rgb[0], rgb[1], rgb[2]);
      rect(px, height/2, sx, height);
    }

  }
  

  public void draw(){
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { ColorPalletteMixer.class.getName() });
  }
  
  
}