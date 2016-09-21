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
  
  public SpringConstraint other;
  public boolean parent = true;

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

  static public void addSpring(VerletParticle2D[] particles, int ia, int ib, float rest_len_sq, TYPE type){
    if(ia == ib) return;
    
    SpringConstraint spring_pa_pb = new SpringConstraint(particles[ib], rest_len_sq, type);
    SpringConstraint spring_pb_pa = new SpringConstraint(particles[ia], rest_len_sq, type);
    
    spring_pa_pb.other = spring_pb_pa;
    spring_pb_pa.other = spring_pa_pb;
    
    // both springs exist, but only one of them is used for the simulation, drawing etc...
    spring_pa_pb.other.parent = !spring_pa_pb.parent; 
    
    particles[ia].addSpring(spring_pa_pb);
    particles[ib].addSpring(spring_pb_pa);
  }

  static public void addSpring(VerletParticle2D[] particles, int ia, int ib, float rest_len_sq){
    addSpring(particles, ia, ib, rest_len_sq, TYPE.STRUCT);
  }

  static public void addSpring(VerletParticle2D[] particles, int ia, int ib){
    addSpring(particles, ia, ib, TYPE.STRUCT);
  }

  static public void addSpring(VerletParticle2D[] particles, int ia, int ib, TYPE type){

    VerletParticle2D pa = particles[ia];
    VerletParticle2D pb = particles[ib];

    // compute rest distance (length of spring)
    float dx = pb.cx - pa.cx;
    float dy = pb.cy - pa.cy;
    float rest_len_sq = dx*dx + dy*dy;

    addSpring(particles, ia, ib, rest_len_sq, type);
  }
  
  static public int getSpringCount(VerletParticle2D[] particles){
    int spring_count = 0;
    for(int i = 0; i < particles.length; i++){
      spring_count += particles[i].spring_count;
    }
    return spring_count;
  }

  static public void makeAllSpringsUnidirectional(VerletParticle2D[] particles){
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint pa_spring = pa.springs[j];
        pa_spring.other.parent = !pa_spring.parent;
      }
    }
  }
  
  
  static public void makeAllSpringsBidirectional(VerletParticle2D[] particles){
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint pa_spring = pa.springs[j];
        pa_spring.other.parent = pa_spring.parent = true;
      }
    }
  }
  
  static public void deleteSpring(VerletParticle2D[] particles, int ia, int ib){
    VerletParticle2D pa = particles[ia];
    VerletParticle2D pb = particles[ib];
    
    pa.removeSpring(ib);
    pb.removeSpring(ia);
  }
  
  static public void deleteSprings(VerletParticle2D[] particles, int ia){
    VerletParticle2D pa = particles[ia];

    while(pa.spring_count > 0){
      int ib = pa.springs[0].idx;
      deleteSpring(particles, ia, ib);
    }
  }
  
  
  
}














