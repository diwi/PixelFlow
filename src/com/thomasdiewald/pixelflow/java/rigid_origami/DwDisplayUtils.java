/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.rigid_origami;

import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;

import processing.core.PGraphics;
import processing.core.PShape;


public class DwDisplayUtils {
  
  static public final void line(PShape pg, DwParticle3D v0, DwParticle3D v1){
    vertex(pg, v0);
    vertex(pg, v1);
  }

  static public final void line(PShape pg, float[] v0, float[] v1){
    vertex(pg, v0);
    vertex(pg, v1);
  }
  
  static public final void line(PGraphics pg, DwParticle3D v0, DwParticle3D v1){
    vertex(pg, v0);
    vertex(pg, v1);
  }
  
  static public final void line(PGraphics pg, float[] v0, float[] v1){
    vertex(pg, v0);
    vertex(pg, v1);
  }
  
  static public final void normal(PGraphics pg, DwParticle3D v0, float[] n, float len){
    if(pg.is2D()){
      pg.vertex(v0.cx           , v0.cy           ); 
      pg.vertex(v0.cx + n[0]*len, v0.cy + n[1]*len); 
    } else {
      pg.vertex(v0.cx           , v0.cy           , v0.cz           ); 
      pg.vertex(v0.cx + n[0]*len, v0.cy + n[1]*len, v0.cz + n[2]*len); 
    }
  }
  
  
  static public final void normal(PGraphics pg,float[] v0, float[] n, float len){
    if(pg.is2D()){
      pg.vertex(v0[0]           , v0[1]           ); 
      pg.vertex(v0[0] + n[0]*len, v0[1] + n[1]*len); 
    } else {
      pg.vertex(v0[0]           , v0[1]           , v0[2]           ); 
      pg.vertex(v0[0] + n[0]*len, v0[1] + n[1]*len, v0[2] + n[2]*len); 
    }
  }
  
  
  static public final void vertex(PGraphics pg, DwParticle3D v0){
    if(pg.is2D()){
      pg.vertex(v0.cx, v0.cy); 
    } else {
      pg.vertex(v0.cx, v0.cy, v0.cz); 
    }
  }
  
  static public final void vertex(PGraphics pg, float[] v0){
    if(pg.is2D()){
      pg.vertex(v0[0], v0[1]); 
    } else {
      pg.vertex(v0[0], v0[1], v0[2]); 
    }
  }
  
  static public final void vertex(PGraphics pg, DwParticle3D v0, float[] t0){
    if(pg.is2D()){
      pg.vertex(v0.cx, v0.cy       , t0[0], t0[1]); 
    } else {
      pg.vertex(v0.cx, v0.cy, v0.cz, t0[0], t0[1]); 
    }
  }
  
  static public final void vertex(PGraphics pg, float[] v0, float[] t0){
    if(pg.is2D()){
      pg.vertex(v0[0], v0[1]       , t0[0], t0[1]); 
    } else {
      pg.vertex(v0[0], v0[1], v0[2], t0[0], t0[1]); 
    }
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  static public final void vertex(PShape pg, DwParticle3D v0){
    if(pg.is2D()){
      pg.vertex(v0.cx, v0.cy); 
    } else {
      pg.vertex(v0.cx, v0.cy, v0.cz); 
    }
  }
  
  static public final void vertex(PShape pg, float[] v0){
    if(pg.is2D()){
      pg.vertex(v0[0], v0[1]); 
    } else {
      pg.vertex(v0[0], v0[1], v0[2]); 
    }
  }
  
  static public final void vertex(PShape pg, DwParticle3D v0, float[] t0){
    if(pg.is2D()){
      pg.vertex(v0.cx, v0.cy       , t0[0], t0[1]); 
    } else {
      pg.vertex(v0.cx, v0.cy, v0.cz, t0[0], t0[1]); 
    }
  }
  
  static public final void vertex(PShape pg, float[] v0, float[] t0){
    if(pg.is2D()){
      pg.vertex(v0[0], v0[1]       , t0[0], t0[1]); 
    } else {
      pg.vertex(v0[0], v0[1], v0[2], t0[0], t0[1]); 
    }
  }
  
  
  
  
  
  
  
}
