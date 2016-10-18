/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.utils;


public class DwAABB_2D{
  
  public float min_x, min_y;
  public float max_x, max_y;
  
  public DwAABB_2D(){
  }
  
  public static DwAABB_2D create(){
    DwAABB_2D aabb = new DwAABB_2D();
    aabb.min_x = aabb.min_y = +Float.MAX_VALUE;
    aabb.max_x = aabb.max_y = -Float.MAX_VALUE;
    return aabb;
  }
  
  public void update(float[] xy){
    update(xy[0], xy[1]);
  }
  public void update(float x, float y){
    if(min_x > x) min_x = x; else if(max_x < x) max_x = x;
    if(min_y > y) min_y = y; else if(max_y < y) max_y = y;
  }
  
  public float area(){ return sizeX()*sizeY(); }
  public float sizeX(){ return max_x - min_x; }
  public float sizeY(){ return max_y - min_y; }
  
  public float maxExtent(){
    float sx = max_x - min_x;
    float sy = max_y - min_y;
    
    return (sx > sy) ? sx : sy;
  }
  
  @Override
  public String toString(){
    return ("aabb ["+min_x+", "+min_y+"] ["+max_x+", "+max_y+"]");
  }
}