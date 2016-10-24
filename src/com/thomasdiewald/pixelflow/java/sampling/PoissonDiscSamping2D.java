/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.sampling;

import java.util.ArrayList;
import java.util.Random;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwCollisionGrid;
import com.thomasdiewald.pixelflow.java.accelerationstructures.DwCollisionObject;
import com.thomasdiewald.pixelflow.java.accelerationstructures.DwStack;


public abstract class PoissonDiscSamping2D<T extends DwCollisionObject> {

  protected Random rand = new Random(0);

  public ArrayList<T> samples = new ArrayList<T>();
  
  protected DwStack<Integer> stack = new  DwStack<Integer>();
  protected DwCollisionGrid grid = new DwCollisionGrid();
  
  public PoissonDiscSamping2D(){
  }
  
  public abstract T newInstance(float x, float y, float r, float rcollision);
//  public abstract T newInstance(float x, float y, float z, float r, float rcollision);
  
  public void setRandomSeed(long seed){
    rand.setSeed(seed);
  }
  
  public void generatePoissonSampling2D(float[] bounds, float radius_min, float radius_max, float offset, int new_points){  
    // fix bounds
    float sx = bounds[3] - bounds[0], mx = (bounds[3] + bounds[0]) * 0.5f;
    float sy = bounds[4] - bounds[1], my = (bounds[4] + bounds[1]) * 0.5f;
    float sz = bounds[5] - bounds[2], mz = (bounds[5] + bounds[2]) * 0.5f;
    float s_min = radius_max + offset;
    
    if(sx < s_min * 2) { bounds[0] = mx - s_min; bounds[3] = mx + s_min; } 
    if(sy < s_min * 2) { bounds[1] = my - s_min; bounds[4] = my + s_min; } 
    if(sz < s_min * 2) { bounds[2] = mz - s_min; bounds[5] = mz + s_min; } 

    // allocate data structures
    samples = new ArrayList<T>();
    stack   = new DwStack<Integer>();
    grid    = new DwCollisionGrid(bounds, radius_max);

    // add first sample
    float pr_new = radius_min + rand.nextFloat() * (radius_max - radius_min);
    T sample_new = newInstance(mx, my, pr_new, pr_new+offset);
    addSample(sample_new);

    // fill space
    while(!stack.isEmpty()){
//      int rand_ptr = (int)(random(1) * stack.size());
//      int sample_old_idx = stack.pop(rand_ptr);
      int sample_old_idx = stack.pop();
      T sample_old = samples.get(sample_old_idx);

      for(int i = 0; i < new_points; i++){
        sample_new = createSample2D(sample_old, radius_min, radius_max, offset);
        addSample(sample_new);
      }
    }
  }
  
  public T createSample2D(T sample_old, float radius_min, float radius_max, float offset){
    float pr_old = sample_old.rad();
    float px_old = sample_old.x();
    float py_old = sample_old.y();
    
    double angle = Math.PI * 2 * rand.nextFloat();
    float x = (float) Math.cos(angle);
    float y = (float) Math.sin(angle);
    
    float pr_new = radius_min + rand.nextFloat() * (radius_max - radius_min);
    float px_new = px_old + x * (pr_old + pr_new + offset*2);
    float py_new = py_old + y * (pr_old + pr_new + offset*2);
    return newInstance(px_new, py_new, pr_new, pr_new+offset);
  }
  
  public void addSample(T sample){
    if(!gotCollision(sample)){
      stack.push(samples.size());
      samples.add(sample);
      grid.insertRealloc(sample);
    }
  }
  
  public boolean gotCollision(DwCollisionObject object){
    float[] bounds = grid.bounds;
    if(object.x() - object.radCollision() < bounds[0]) return true;
    if(object.y() - object.radCollision() < bounds[1]) return true;
    if(object.z() - object.radCollision() < bounds[2]) return true;
    if(object.x() + object.radCollision() > bounds[3]) return true;
    if(object.y() + object.radCollision() > bounds[4]) return true;
    if(object.z() + object.radCollision() > bounds[5]) return true;
    grid.solveCollision(object);
    return (object.getCollisionCount() > 0);
  }
  
  
}
