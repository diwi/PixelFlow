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
    
//    static public float SPRING_STABILIZATION = 0.45f;
    
    public TYPE  type;
    public int   idx;
    public float dd_rest_sq;
    public float dd_rest;
    
    private SpringConstraint(VerletParticle2D particle, float spring_len_sq){
      this(particle, spring_len_sq, TYPE.STRUCT);
    }

    private SpringConstraint(VerletParticle2D particle, float spring_len_sq, TYPE type){
      this.idx = particle.idx;
      this.type = type;
      this.dd_rest_sq = spring_len_sq;
      this.dd_rest = (float) Math.sqrt(dd_rest_sq);
    }
      
    static public void addSpringPair(VerletParticle2D[] particles, int ia, int ib, float rest_len_sq, TYPE type){
      if(ia == ib) return;
      particles[ia].addSpring(new SpringConstraint(particles[ib], rest_len_sq, type));
      particles[ib].addSpring(new SpringConstraint(particles[ia], rest_len_sq, type));
    }
    
    static public void addSpringPair(VerletParticle2D[] particles, int ia, int ib, float rest_len_sq){
      addSpringPair(particles, ia, ib, rest_len_sq, TYPE.STRUCT);
    }
    
    static public void addSpringPair(VerletParticle2D[] particles, int ia, int ib){
      addSpringPair(particles, ia, ib, TYPE.STRUCT);
    }
    
    static public void addSpringPair(VerletParticle2D[] particles, int ia, int ib, TYPE type){
      
      VerletParticle2D pa = particles[ia];
      VerletParticle2D pb = particles[ib];
      
      // compute rest distance (length of spring)
      float dx = pb.cx - pa.cx;
      float dy = pb.cy - pa.cy;
      float rest_len_sq = dx*dx + dy*dy;
      
      addSpringPair(particles, ia, ib, rest_len_sq, type);
    }
  }
  