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

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwCollisionGrid;
import com.thomasdiewald.pixelflow.java.accelerationstructures.DwStack;
import com.thomasdiewald.pixelflow.java.sampling.PoissonSample;

import processing.core.PApplet;

public class Sampling_Poisson2D_devAnim1 extends PApplet {

  int sample_idx = 1;
  
  int n = 2;
  float sample_radius = 3;
  
  float size_x = 800;
  float size_y = 400;
  
  Random rand = new Random(0);
  

  ArrayList<PoissonSample> samples = new ArrayList<PoissonSample>();
  DwStack<Integer> stack = new  DwStack<Integer>();
  
  DwCollisionGrid grid = new DwCollisionGrid();
  
  public void settings(){
    size((int)size_x, (int)size_y, P2D);
    smooth(8);
  }
  

  public void setup(){
    
    float[] bounds = {0,0,0, width, height, 0};
    grid.init(bounds, 10);
    
    PoissonSample sample_new = new PoissonSample(width/2, height/2, sample_radius, sample_radius+offset);
    addSample(sample_new);
    
    
    grid.solveCollision(sample_new);
    if(sample_new.getCollisionCount() == 0){
      grid.insertRealloc(sample_new);
    }

    frameRate(1000);
  }
  
  public void addSample(PoissonSample sample){
    stack.push(samples.size());
    samples.add(sample);
  }

  float offset = 1f;
  public void draw(){
    background(64);
    
    for(int m = 0; m < 2; m++)
    if(!stack.isEmpty()){
      int rand_ptr = (int)(rand.nextFloat() * stack.size());
      int sample_idx = stack.pop(rand_ptr);
      PoissonSample sample = samples.get(sample_idx);
      
      float curr_x = sample.x();
      float curr_y = sample.y();
      float curr_r = sample.rad();
      
      PoissonSample sample_new = null;
      boolean GOT_NEW_SAMPLE = false;
      
      int tests = 1000;
//      float angle_step  = (float)Math.PI * 2 / tests;
//      float angle_start = (float)Math.PI * 2 * rand.nextFloat();
      for(int i = 0; !GOT_NEW_SAMPLE && i < tests; i++){
//        double angle = angle_start + i * angle_step;
        double angle = (float)Math.PI * 2 * rand.nextFloat();
        float x = (float) Math.cos(angle);
        float y = (float) Math.sin(angle);
  
//        float rr = 0;
//        float pr = (rand.nextFloat() * rr - rr/2 + 1.0f) *sample_radius;
        float pr = random(2,15);
        float px = curr_x + x * (curr_r + pr + offset * 2);
        float py = curr_y + y * (curr_r + pr + offset * 2);
        sample_new = new PoissonSample(px, py, pr, pr);
        
        GOT_NEW_SAMPLE = true;
       
        if(sample_new.x() <      0) GOT_NEW_SAMPLE = false;
        if(sample_new.y() <      0) GOT_NEW_SAMPLE = false;
        if(sample_new.x() > size_x) GOT_NEW_SAMPLE = false;
        if(sample_new.y() > size_y) GOT_NEW_SAMPLE = false;
        
        grid.solveCollision(sample_new);
        if(sample_new.getCollisionCount() > 0){
          GOT_NEW_SAMPLE = false;
        }
        
        if(GOT_NEW_SAMPLE){
          addSample(sample_new);
          grid.insertRealloc(sample_new);
          GOT_NEW_SAMPLE = false;
        }
        
      }
      
//      if(GOT_NEW_SAMPLE){
//        stack.push(sample_idx);
//        addSample(sample_new);
//        grid.insertRealloc(sample_new);
//      }
    }
    
    
 
  
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
    PApplet.main(new String[] { Sampling_Poisson2D_devAnim1.class.getName() });
  }
}