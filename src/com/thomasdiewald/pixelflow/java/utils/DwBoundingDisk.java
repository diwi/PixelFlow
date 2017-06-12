/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package com.thomasdiewald.pixelflow.java.utils;



import java.util.Locale;

import processing.core.PMatrix2D;


/**
 * 
 * computes the bounding-dist of a given list of points.
 * algorithm: bouncing bubble
 * 
 * @author Thomas Diewald, http://thomasdiewald.com/blog/
 * 
 * @date 23.05.2017
 * 
 */
public class DwBoundingDisk {

  public final float MOVE_FAC = 0.95f;
  public final float SCALE_FAC = 1.005f;
  public int DEB_COUNTER_ITERATIONS;

  public float[] pos = new float[2];
  public float   rad;
  
  public DwBoundingDisk(){
    set(0,0,0);
  }
  
  public void set(float x, float y, float r){
    pos[0] = x;
    pos[1] = y;
    rad    = r;
  }
  public float[] pos(){
    return pos;
  }
  
  public float rad(){
    return rad;
  }
  
  public void print(){
    System.out.printf(Locale.ENGLISH, "BoundingDisk: xyz[%+3.3f, %+3.3f],  rad[%3.3f]\n", pos[0], pos[1], rad );
  }
  
  public PMatrix2D getUnitDiskMatrix(){
    PMatrix2D mat = new PMatrix2D();
    mat.scale(1.0f/rad);
    mat.translate(-pos[0], -pos[1]);
    return mat;
  }
  

  public void compute(float[][] p, int num_p){
    
    set(0,0,0);

    if( p == null | p.length < 2 ){
      return;
    }

    // init: 
    // place sphere in the middle of the first two points
    float[] pnt_0 = p[0];
    float[] pnt_1 = p[1];
    
    float dx = (pnt_1[0]-pnt_0[0]) * 0.5f;
    float dy = (pnt_1[1]-pnt_0[1]) * 0.5f;
    float dr = 0;

    float sphere_x    = pnt_0[0] + dx;
    float sphere_y    = pnt_0[1] + dy;
    float sphere_r_sq = dx*dx + dy*dy;
    float sphere_r    = (float)Math.sqrt(sphere_r_sq);
    
    if(sphere_r_sq == 0){
      System.out.println("error computing boundingsphere. first two points have same coordinates.");
      return;
    }
    
    final float rscale = (1f - MOVE_FAC); // scale for new radius, constant
    int last = 1;
    int idx = 1;

    // check the other points
    // bad cases i tested took about 2*n passes, e.g. spiral
    DEB_COUNTER_ITERATIONS = 0;
    
    while((idx = ++idx%num_p) != last){
      
      DEB_COUNTER_ITERATIONS++;

      // check if current point is outside current sphere
      dx = p[idx][0] - sphere_x;
      dy = p[idx][1] - sphere_y;

      float r_tmp = dx*dx + dy*dy; // keep it squared for comparing
      
      if(r_tmp > sphere_r_sq){

        r_tmp = (float)Math.sqrt(r_tmp);
        dr = r_tmp - sphere_r;
        float dscale = MOVE_FAC * dr / r_tmp;
       
        // update sphere: move sphere a bit to pnt_i and increase radius to pnt_i
        sphere_x += dscale * dx;
        sphere_y += dscale * dy;
        sphere_r += rscale * dr; // maybe not so good, because of float-precision?
        sphere_r *= SCALE_FAC; // add/mult a small epsilon
        sphere_r_sq = sphere_r * sphere_r; // update squared radius too
        
        last = idx; 
      }
    }

    set(sphere_x, sphere_y, sphere_r);
    
//    System.out.println("DEB_COUNTER_ITERATIONS = "+DEB_COUNTER_ITERATIONS);
  }
  
  
}
