/**
 * 
 * Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com - MIT License
 * 
 * ___PixelFlow___
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * 
 */
package com.thomasdiewald.pixelflow.java;

/**
 * @author Thomas
 *
 */
public interface CollisionObject {

  public void beginCollision();
  public void update    (CollisionObject othr);

  public float x();
  public float y();
  public float rad();
}
