/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Sampling.Sampling_Poisson2D;


import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.sampling.PoissonDiscSamping2D;
import com.thomasdiewald.pixelflow.java.sampling.PoissonSample;

import processing.core.PApplet;

public class Sampling_Poisson2D extends PApplet {



  ArrayList<PoissonSample> samples;

  boolean DISPLAY_RADIUS = true;
  
  public void settings(){
    size(1280, 720, P2D);
    smooth(16);
  }
  
  public void setup(){
    generatePoissonSampling();
  }
  

  public void generatePoissonSampling(){
    long timer;
    
    PoissonDiscSamping2D<PoissonSample> pds = new PoissonDiscSamping2D<PoissonSample>() {
      @Override
      public PoissonSample newInstance(float x, float y, float r, float rcollision){
        return new PoissonSample(x,y,r,rcollision);
      }
    };
    
    
    float[] bounds = {0,0,0, width, height, 0};
    float rmin = 2;
    float rmax = 25;
    float roff = 0.5f;
    int new_points = 100;
    
    timer = System.currentTimeMillis();
    
    pds.setRandomSeed((long) random(0,100000));
    pds.generatePoissonSampling2D(bounds, rmin, rmax, roff, new_points);

    timer = System.currentTimeMillis() - timer;
    System.out.println("poisson samples 3D generated");
    System.out.println("    time: "+timer+"ms");
    System.out.println("    count: "+pds.samples.size());
    
    samples = pds.samples;
  }
  

  public void draw(){
    background(64);
    
    for(PoissonSample sample : samples){
      float px = sample.x();
      float py = sample.y();
      float pr = sample.rad();
      if(DISPLAY_RADIUS){
        stroke(255);
        strokeWeight(0.5f);
        fill(255);
        noStroke();
        ellipse(px, py, pr * 2, pr * 2);
      } else {
        stroke(255);
        strokeWeight(2);
        point(px, py);
      }
    }

  }

  
  public void keyReleased(){
    if(key == ' ') DISPLAY_RADIUS = !DISPLAY_RADIUS;
    if(key == 'r') generatePoissonSampling();
  }

  
  public static void main(String args[]) {
    PApplet.main(new String[] { Sampling_Poisson2D.class.getName() });
  }
}