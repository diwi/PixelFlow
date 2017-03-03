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
import java.util.Random;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwStack;


import processing.core.PApplet;

public class Sampling_Poisson2D_devAnim0 extends PApplet {

  int sample_idx = 1;
  
  int n = 2;
  float sample_radius = 3;
  
  float size_x = 800;
  float size_y = 400;
  
  Random rand = new Random(0);
  

  ArrayList<float[]> samples = new ArrayList<float[]>();
  DwStack<Integer> stack = new  DwStack<Integer>();
  
  public void settings(){
    size((int)size_x, (int)size_y, P2D);
    smooth(8);
    
  }
  

  public void setup(){
    float[] sample_new = {width/2, height/2, sample_radius};
    addSample(sample_new);
    frameRate(1000);
  }
  
  public void addSample(float[] sample){
    stack.push(samples.size());
    samples.add(sample);
  }

  float empty_distance = 1f;
  public void draw(){
    background(64);
    
    for(int m = 0; m < 1; m++)
    if(!stack.isEmpty()){
      int rand_ptr = (int)(rand.nextFloat() * stack.size());
      int sample_idx = stack.pop(rand_ptr);
      float[] sample = samples.get(sample_idx);
      
      float curr_x = sample[0];
      float curr_y = sample[1];
      float curr_r = sample[2];
      
      float[] sample_new = null;
      boolean GOT_NEW_SAMPLE = false;
      
      int tests = 10;
      float angle_step  = (float)Math.PI * 2 / tests;
      float angle_start = (float)Math.PI * 2 * rand.nextFloat();
      for(int i = 0; !GOT_NEW_SAMPLE && i < tests; i++){
        double angle = angle_start + i * angle_step;
//        double angle = (float)Math.PI * 2 * rand.nextFloat();
        float x = (float) Math.cos(angle);
        float y = (float) Math.sin(angle);
  
//        float rr = 0;
//        float pr = (rand.nextFloat() * rr - rr/2 + 1.0f) *sample_radius;
        float pr = random(2,10);
        float px = curr_x + x * (curr_r + pr + empty_distance);
        float py = curr_y + y * (curr_r + pr + empty_distance);
        sample_new = new float[]{px, py, pr};
        
        GOT_NEW_SAMPLE = true;
        
        if(sample_new[0] <      0) GOT_NEW_SAMPLE = false;
        if(sample_new[1] <      0) GOT_NEW_SAMPLE = false;
        if(sample_new[0] > size_x) GOT_NEW_SAMPLE = false;
        if(sample_new[1] > size_y) GOT_NEW_SAMPLE = false;
        
        for(int j = 0; GOT_NEW_SAMPLE && j < samples.size(); j++){
          float[] sample_check = samples.get(j);
          float dr = sample_check[2] + sample_new[2];
          float dx = sample_check[0] - sample_new[0];
          float dy = sample_check[1] - sample_new[1];
          float dd_sq = dx*dx + dy*dy;
          if(dd_sq < dr*dr){
            GOT_NEW_SAMPLE = false;
          }
        }
        
      }
      
      if(GOT_NEW_SAMPLE){
        stack.push(sample_idx);
        addSample(sample_new);
      }
    }
    
    
 
  
    for(float[] sample : samples){
      float px = sample[0];
      float py = sample[1];
      float pr = sample[2];
      if(DISPLAY_RADIUS){
        stroke(255);
        strokeWeight(0.5f);
        fill(255);
        noStroke();
        ellipse(px, py, pr * 2, pr * 2);
      }
      
      stroke(255);
      strokeWeight(2);
      point(px, py);
    }

  }

  boolean DISPLAY_RADIUS = true;
  
  public void keyReleased(){
    if(key == ' ') DISPLAY_RADIUS = !DISPLAY_RADIUS;
  }

  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Sampling_Poisson2D_devAnim0.class.getName() });
  }
}