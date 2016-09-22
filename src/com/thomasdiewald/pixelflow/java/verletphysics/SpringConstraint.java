/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.verletphysics;

import java.util.ArrayList;

public class SpringConstraint {

  static public enum TYPE{
    STRUCT,
    SHEAR,
    BEND
  }
  

  
  // if true, then this spring is used for rendering/constraints relaxation etc ...
  // if false, then its sibling *must* be true
  public boolean is_the_good_one = true;

  // the exact mirror of this one.
  // other.pb returns the origin of this spring
  // this  spring: pa -> pb
  // other spring: pa <- pb
  public SpringConstraint other;
  
  // other end of this spring. pa -> pb
  public VerletParticle2D pb;
  public TYPE  type;
  public float dd_rest_sq;
  public float dd_rest;
  public float force;
  
  private SpringConstraint(VerletParticle2D particle, float spring_len_sq){
    this(particle, spring_len_sq, TYPE.STRUCT);
  }

  private SpringConstraint(VerletParticle2D particle, float spring_len_sq, TYPE type){
    this.pb = particle;
    this.type = type;
    this.dd_rest_sq = spring_len_sq;
    this.dd_rest = (float) Math.sqrt(dd_rest_sq);
  }
  
  public float computeForce(){
    float dx = pb.cx - other.pb.cx;
    float dy = pb.cy - other.pb.cy;
    float dd_curr_sq = dx*dx + dy*dy;
    float force = (dd_rest_sq / (dd_curr_sq + dd_rest_sq) - 0.5f);
        
//    float dd_rest    = (float) Math.sqrt(dd_rest_sq);
//    float dd_curr    = (float) Math.sqrt(dd_curr_sq);
//    float force      = (0.5f * (dd_rest - dd_curr) / (dd_curr + 0.00001f));

    return force;
  }
  
  static public void addSpring(VerletParticle2D pa, VerletParticle2D pb, float rest_len_sq, TYPE type){
    if(pa == pb) return;
    
    SpringConstraint spring_pa_pb = new SpringConstraint(pb, rest_len_sq, type);
    SpringConstraint spring_pb_pa = new SpringConstraint(pa, rest_len_sq, type);
    
    spring_pa_pb.other = spring_pb_pa;
    spring_pb_pa.other = spring_pa_pb;
    
    // both springs exist, but only one of them is used for the simulation, drawing etc...
    spring_pa_pb.other.is_the_good_one = !spring_pa_pb.is_the_good_one; 
    
    pa.addSpring(spring_pa_pb);
    pb.addSpring(spring_pb_pa);
  }

  static public void addSpring(VerletParticle2D pa, VerletParticle2D pb, float rest_len_sq){
    addSpring(pa, pb, rest_len_sq, TYPE.STRUCT);
  }

  static public void addSpring(VerletParticle2D pa, VerletParticle2D pb){
    addSpring(pa, pb, TYPE.STRUCT);
  }

  static public void addSpring(VerletParticle2D pa, VerletParticle2D pb, TYPE type){
    float dx = pb.cx - pa.cx;
    float dy = pb.cy - pa.cy;
    float rest_len_sq = dx*dx + dy*dy;
    addSpring(pa, pb, rest_len_sq, type);
  }
  
  static public int getSpringCount(VerletParticle2D[] particles, boolean only_good_ones){
    int spring_count = 0;

    if(only_good_ones){
      for(int i = 0; i < particles.length; i++){
        VerletParticle2D pa = particles[i];
        for(int j = 0; j < pa.spring_count; j++){
          SpringConstraint pa_spring = pa.springs[j];
          if(pa_spring.is_the_good_one) spring_count++;
        }
      }
    } else {
      for(int i = 0; i < particles.length; i++){
        spring_count += particles[i].spring_count;
      }
    }
    
    return spring_count;
  }

  static public void makeAllSpringsUnidirectional(VerletParticle2D[] particles){
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint pa_spring = pa.springs[j];
        pa_spring.other.is_the_good_one = !pa_spring.is_the_good_one;
      }
    }
  }
  
  
  static public void makeAllSpringsBidirectional(VerletParticle2D[] particles){
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint pa_spring = pa.springs[j];
        pa_spring.other.is_the_good_one = pa_spring.is_the_good_one = true;
      }
    }
  }
  
  static public SpringConstraint deleteSpring(VerletParticle2D pa, VerletParticle2D pb){
    SpringConstraint sa = pa.removeSpring(pb);
    SpringConstraint sb = pb.removeSpring(pa);
    
    if(sa.other != sb && sb.other != sa){
      System.out.println("error: SpringConstraints not Linked to each other!!");
    }
    return sa;
  }
  
  static public ArrayList<SpringConstraint> deleteSprings(VerletParticle2D pa){
    return deleteSprings(pa, (ArrayList<SpringConstraint>) null);
  }
  
  static public ArrayList<SpringConstraint> deleteSprings(VerletParticle2D pa, ArrayList<SpringConstraint> removed_springs){
    if(removed_springs == null) removed_springs = new ArrayList<SpringConstraint>();
    while(pa.spring_count > 0){
      VerletParticle2D pb = pa.springs[0].pb;
      removed_springs.add(deleteSpring(pa, pb));
    }
    return removed_springs;
  }
  
  
  static public ArrayList<SpringConstraint> deactivateSprings(VerletParticle2D pa, ArrayList<SpringConstraint> deactivated_springs){
    if(deactivated_springs == null) deactivated_springs = new ArrayList<SpringConstraint>();
    for(int j = 0; j < pa.spring_count; j++){
      SpringConstraint pa_spring = pa.springs[j];
      pa_spring.is_the_good_one       = false;
      pa_spring.other.is_the_good_one = false;
      deactivated_springs.add(pa_spring);
    }
    
    return deactivated_springs;
    
  }
  
}














