/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.softbodydynamics.constraint;



public abstract class DwSpringConstraint {
  
  static public enum TYPE{
    VIRTUAL,
    BEND,
    SHEAR,
    STRUCT,
    ;
   
  }
  

  
  static public class Param{
    // resistance to compression -> strut 
    // 0.0 ... very squishy
    // 1.0 ... very springy
    public float damp_inc = 1.0f;
    
    // resistance to expansion -> tie  
    // 0.0 ... very loose
    // 1.0 ... very tense
    public float damp_dec = 1.0f;
  }
  
  
  public Param param = new Param();
  public TYPE  type;
  public boolean enabled = true;
  
//  public DwParticle pa;
//  public DwParticle pb;
  
  public float dd_rest_sq;
  public float dd_rest;
  public float force;
  
  
  public abstract void  updateRestlength();
  public abstract float computeForce();
  public abstract void  update();
  public abstract int   idxPa();
  public abstract int   idxPb();
  
  
  public final void enable(boolean enable){
    this.enabled = enable;
  }
  
  public void setRestLength(float dd_rest){
    this.dd_rest    = dd_rest;
    this.dd_rest_sq = dd_rest * dd_rest;
  }
  
  
}
