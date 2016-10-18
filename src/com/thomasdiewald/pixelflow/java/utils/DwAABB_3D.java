/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.utils;


public class DwAABB_3D{
  public float min_x, min_y, min_z;
  public float max_x, max_y,max_z;
  public DwAABB_3D(){
    
  }
  
  public static DwAABB_3D create(){
    DwAABB_3D aabb = new DwAABB_3D();
    aabb.min_x = aabb.min_y = aabb.min_z = +Float.MAX_VALUE;
    aabb.max_x = aabb.max_y = aabb.max_z = -Float.MAX_VALUE;
    return aabb;
  }

  public void update(float[][] points, int num_points){
    for(int i = 0; i < num_points; i++){
      update(points[i]);
    }
  }
  
  public void update(float[] xyz){
    update(xyz[0], xyz[1], xyz[2]);
  }
  
  public void update(float x, float y, float z){
    if(min_x > x) min_x = x; else if(max_x < x) max_x = x;
    if(min_y > y) min_y = y; else if(max_y < y) max_y = y;
    if(min_z > z) min_z = z; else if(max_z < z) max_z = z;
  }
  
  public float sizeX(){ return max_x - min_x; }
  public float sizeY(){ return max_y - min_y; }
  public float sizeZ(){ return max_z - min_z; }

  
  public float maxExtent(){
    float sx = max_x - min_x;
    float sy = max_y - min_y;
    float sz = max_z - min_z;
    
    float smax = sx > sy ? sx : sy;
    return smax > sz ? smax : sz;
  }
  
  @Override
  public String toString(){
    return ("aabb ["+min_x+", "+min_y+", "+min_z+"] ["+max_x+", "+max_y+", "+max_z+"]");
  }
}