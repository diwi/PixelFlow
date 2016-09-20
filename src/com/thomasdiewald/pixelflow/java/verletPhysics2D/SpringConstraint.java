/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.verletPhysics2D;

public class SpringConstraint {
    
    static public enum TYPE{
      STRUCT,
      SHEAR,
      BEND
    }
    
    static public float SPRING_STABILIZATION = 0.45f;
    
    public TYPE  type;
//    public float spring_inc; // TODO
//    public float spring_dec; // TODO

    public int idx;
    public float dd_rest_sq;
    public float dd_rest;
    
    public SpringConstraint(int idx, float spring_len_sq){
      this(idx, spring_len_sq, TYPE.STRUCT);
    }

    public SpringConstraint(int idx, float spring_len_sq, TYPE type){
      this.idx = idx;
//      this.spring_inc = spring_increase;
//      this.spring_dec = spring_decrease;
      this.type = type;
      
      this.dd_rest_sq = spring_len_sq;
      this.dd_rest = (float) Math.sqrt(dd_rest_sq);
    }
    
//    public SpringConstraint(int idx, float spring_increase, float spring_decrease, float spring_len_sq, TYPE type){
//      this.idx = idx;
////      this.spring_inc = spring_increase;
////      this.spring_dec = spring_decrease;
//      this.type = type;
//      
//      this.dd_rest_sq = spring_len_sq;
//      this.dd_rest = (float) Math.sqrt(dd_rest_sq);
//    }
  }
  