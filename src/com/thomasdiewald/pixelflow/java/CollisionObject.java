/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java;



public interface CollisionObject {

  public void beginCollision();
  public void update    (CollisionObject othr);

  public float x();
  public float y();
  public float rad();
}
