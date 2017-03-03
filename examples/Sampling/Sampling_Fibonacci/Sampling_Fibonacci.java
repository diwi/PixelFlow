/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Sampling.Sampling_Fibonacci;


import com.thomasdiewald.pixelflow.java.sampling.DwSampling;

import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;

public class Sampling_Fibonacci extends PApplet {

 
  PShape shp_samples;
  int sample_idx = 1;
  
  public void settings(){
    size(800,800, P2D);
    smooth(8);
  }
  
  public void setup(){
    shp_samples = createShape(GROUP);
    frameRate(1000);
  }

  public void draw(){
    background(64);

    float r = 0.01f * (float) Math.pow(sample_idx, 0.5f);
    float angle = sample_idx * (float) DwSampling.GOLDEN_ANGLE_R;
    float x = r * cos(angle);
    float y = r * sin(angle);
    float[] sample = {x, y};
    
    addShape(sample, 500);
    
    translate(width/2, height/2);
    shape(shp_samples);
    sample_idx++;
  }
  
  void addShape(float[] position, float scale){
    PVector pos = new PVector().set(position).mult(scale);
    PShape shp_point = createShape(POINT, pos.x, pos.y);
    shp_point.setStroke(color(255));
    shp_point.setStrokeWeight(3);
    shp_samples.addChild(shp_point);
  }
  

  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Sampling_Fibonacci.class.getName() });
  }
}