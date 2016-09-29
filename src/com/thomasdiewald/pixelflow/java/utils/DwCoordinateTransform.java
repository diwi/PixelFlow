/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package com.thomasdiewald.pixelflow.java.utils;

import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;

import processing.core.PMatrix3D;
import processing.opengl.PGraphics3D;

public class DwCoordinateTransform{
  public PGraphics3D pg;
  
  public float[] world  = new float[4];
  public float[] screen = new float[4];
  
  public PMatrix3D mat_projmodelview     = new PMatrix3D();
  public PMatrix3D mat_projmodelview_inv = new PMatrix3D();

  public void useCurrentTransformationMatrix(PGraphics3D pg){
    this.pg = pg;
    this.mat_projmodelview    .set(pg.projmodelview);
    this.mat_projmodelview_inv.set(pg.projmodelview);
    this.mat_projmodelview_inv.invert();
  }
  
  // this transforms a coordinate (vec4) from model-space to screen-space
  public void transformToScreen(DwParticle3D particle, float[] dst_screen){
    world[0] = particle.cx;
    world[1] = particle.cy;
    world[2] = particle.cz;
    world[3] = 1;
    worldToScreen(world, dst_screen);
  }
  
  public void worldToScreen(float x, float y, float z, float[] dst_screen){
    world[0] = x;
    world[1] = y;
    world[2] = z;
    world[3] = 1;
    worldToScreen(world, dst_screen);
  }

  public void worldToScreen(float[] src_world, float[] dst_screen){
    src_world[3] = 1;
    mat_projmodelview.mult(src_world, dst_screen);
    float w_inv = 1f/dst_screen[3];
    dst_screen[0] = ((dst_screen[0] * w_inv) * +0.5f + 0.5f) * pg.width;
    dst_screen[1] = ((dst_screen[1] * w_inv) * -0.5f + 0.5f) * pg.height;
    dst_screen[2] = ((dst_screen[2] * w_inv) * +0.5f + 0.5f);
  }
  
  
  
  
  // this transforms a coordinate (vec4) from screen-space to model-space
  public void screenToWorld(float[] src_screen, float[] dst_world){
    src_screen[0] = ((src_screen[0]/(float) pg.width ) * 2 - 1) * +1;
    src_screen[1] = ((src_screen[1]/(float) pg.height) * 2 - 1) * -1;
    src_screen[2] = ((src_screen[2]              ) * 2 - 1) * +1;
    src_screen[3] = 1;
    mat_projmodelview_inv.mult(src_screen, dst_world);
    float w_inv = 1f/dst_world[3];
    dst_world[0] *= w_inv;
    dst_world[1] *= w_inv;
    dst_world[2] *= w_inv;
  }
  
  public void screenToWorld(float x, float y, float z, float[] dst_world){
    screen[0] = ((x/(float) pg.width ) * 2 - 1) * +1;
    screen[1] = ((y/(float) pg.height) * 2 - 1) * -1;
    screen[2] = ((z              ) * 2 - 1) * +1;
    screen[3] = 1;
    mat_projmodelview_inv.mult(screen, dst_world);
    float w_inv = 1f/dst_world[3];
    dst_world[0] *= w_inv;
    dst_world[1] *= w_inv;
    dst_world[2] *= w_inv;
  }
  
  
}