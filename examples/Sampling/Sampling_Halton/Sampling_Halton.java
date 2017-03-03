/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Sampling.Sampling_Halton;



import com.thomasdiewald.pixelflow.java.sampling.DwSampling;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PVector;

public class Sampling_Halton extends PApplet {

 
  PeasyCam cam;
  PShape shp_samples;
  int sample_idx = 1;
  
  public void settings(){
    size(800,800, P3D);
    smooth(8);
  }
  
  public void setup(){
    cam = new PeasyCam(this, 0, 0, 0, 1000);
    shp_samples = createShape(GROUP);
    frameRate(1000);
  }

  public void draw(){
    background(64);
    displayGizmo(500);

//    float[] sample = DwSampling.cosineSampleHemisphere_Halton(sample_idx);
    float[] sample = DwSampling.uniformSampleHemisphere_Halton(sample_idx);
//    float[] sample = DwSampling.uniformSampleSphere_Halton(sample_idx);
    addShape(sample, 500);
    
    shape(shp_samples);
    sample_idx++;
  }
  
  void addShape(float[] position, float scale){
    PVector pos = new PVector().set(position).mult(scale);
    PShape shp_point = createShape(POINT, pos.x, pos.y, pos.z);
    shp_point.setStroke(color(255));
    shp_point.setStrokeWeight(3);
    shp_samples.addChild(shp_point);
  }
  
  PShape shp_gizmo;

  public void displayGizmo(float s){
    if(shp_gizmo == null){
      strokeWeight(1);
      shp_gizmo = createShape();
      shp_gizmo.beginShape(LINES);
      shp_gizmo.stroke(255,0,0); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(s,0,0);
      shp_gizmo.stroke(0,255,0); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(0,s,0); 
      shp_gizmo.stroke(0,0,255); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(0,0,s); 
      shp_gizmo.endShape();
    }
    shape(shp_gizmo);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Sampling_Halton.class.getName() });
  }
}