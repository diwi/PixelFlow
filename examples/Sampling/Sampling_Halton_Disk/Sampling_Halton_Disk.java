/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Sampling.Sampling_Halton_Disk;



import com.thomasdiewald.pixelflow.java.sampling.DwSampling;

import processing.core.PApplet;

public class Sampling_Halton_Disk extends PApplet {


  
  public void settings(){
    size(800,800, P2D);
    smooth(8);
  }
  
  public void setup(){
    frameRate(1000);
    background(64);
  }


  int index = 0;
  public void draw(){
 
    index++;
    
    float[] xy = DwSampling.sampleDisk_Halton(index, 1);
    float radius = width/2;
    float ox = width/2;
    float oy = height/2;
    float sample_x = ox + xy[0] * radius;
    float sample_y = oy + xy[1]  * radius;
   
    noStroke();
    fill(255);
    ellipse(sample_x, sample_y, 2, 2);

  }
  

  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Sampling_Halton_Disk.class.getName() });
  }
}