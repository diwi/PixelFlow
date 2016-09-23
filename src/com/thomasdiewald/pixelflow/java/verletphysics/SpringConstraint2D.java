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

public class SpringConstraint2D {
  
  

  static public enum TYPE{
    STRUCT,
    SHEAR,
    BEND
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
  
  
  Param param = new Param();

  
  // if true, then this spring is used for rendering/constraints relaxation etc ...
  // if false, then its sibling *must* be true
  public boolean is_the_good_one = true;

  // the exact mirror of this one.
  // other.pb returns the origin of this spring
  // this  spring: pa -> pb
  // other spring: pa <- pb
  public SpringConstraint2D other;
  
  // other end of this spring. pa -> pb
  public VerletParticle2D pb;
  public TYPE  type;
  public float dd_rest_sq;
  public float dd_rest;
  public float force;
  
  private SpringConstraint2D(VerletParticle2D particle, float spring_len_sq, Param param){
    this(particle, spring_len_sq, param, TYPE.STRUCT);
  }

  private SpringConstraint2D(VerletParticle2D particle, float spring_len_sq, Param param, TYPE type){
    this.pb         = particle;
    this.param      = param;
    this.type       = type;
    this.dd_rest_sq = spring_len_sq;
    this.dd_rest    = (float) Math.sqrt(dd_rest_sq);
  }
  
  public void updateRestlength(){
    float dx        = pb.cx - other.pb.cx;
    float dy        = pb.cy - other.pb.cy;
    this.dd_rest_sq = dx*dx + dy*dy;
    this.dd_rest    = (float) Math.sqrt(dd_rest_sq);
  }
  
  
  public float updateForce(){
    if(is_the_good_one){
      float dx    = pb.cx - other.pb.cx;
      float dy    = pb.cy - other.pb.cy;
      float dd_sq = dx*dx + dy*dy;
//      float dd    = (float) Math.sqrt(dd_sq);
//      force       = (0.5f * (dd_rest - dd) / (dd + 0.00001f));
      force       = (dd_rest_sq / (dd_sq + dd_rest_sq) - 0.5f);
      force      *= (dd_sq < dd_rest_sq) ? param.damp_inc: param.damp_dec; 
      other.force = force;
    }
    return force;
  }
  
  
  public float computeForce(){
    float dx    = pb.cx - other.pb.cx;
    float dy    = pb.cy - other.pb.cy;
    float dd_sq = dx*dx + dy*dy;
//    float dd    = (float) Math.sqrt(dd_sq);
//    float force = (0.5f * (dd_rest - dd) / (dd + 0.00001f));
    float force = (dd_rest_sq / (dd_sq + dd_rest_sq) - 0.5f);
    return force;
  }
  
  static public void addSpring(VerletParticle2D pa, VerletParticle2D pb, float rest_len_sq, Param param, TYPE type){
    if(pa == pb) return;
    
    SpringConstraint2D spring_pa_pb = new SpringConstraint2D(pb, rest_len_sq, param, type);
    SpringConstraint2D spring_pb_pa = new SpringConstraint2D(pa, rest_len_sq, param, type);
    
    spring_pa_pb.other = spring_pb_pa;
    spring_pb_pa.other = spring_pa_pb;
    
    // both springs exist, but only one of them is used for the simulation, drawing etc...
    spring_pa_pb.other.is_the_good_one = !spring_pa_pb.is_the_good_one; 
    
    pa.addSpring(spring_pa_pb);
    pb.addSpring(spring_pb_pa);
  }

  // TODO error checking
  static public void addSpring(VerletParticle2D pa, VerletParticle2D pb, float rest_len_sq, Param param){
    addSpring(pa, pb, rest_len_sq, param, TYPE.STRUCT);
  }

  static public void addSpring(VerletParticle2D pa, VerletParticle2D pb, Param param){
    addSpring(pa, pb, param, TYPE.STRUCT);
  }

  static public void addSpring(VerletParticle2D pa, VerletParticle2D pb, Param param, TYPE type){
    float dx = pb.cx - pa.cx;
    float dy = pb.cy - pa.cy;
    float rest_len_sq = dx*dx + dy*dy;
    addSpring(pa, pb, rest_len_sq, param, type);
  }
  
  static public int getSpringCount(VerletParticle2D[] particles, boolean only_good_ones){
    int spring_count = 0;

    if(only_good_ones){
      for(int i = 0; i < particles.length; i++){
        VerletParticle2D pa = particles[i];
        for(int j = 0; j < pa.spring_count; j++){
          SpringConstraint2D pa_spring = pa.springs[j];
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
        SpringConstraint2D pa_spring = pa.springs[j];
        pa_spring.other.is_the_good_one = !pa_spring.is_the_good_one;
      }
    }
  }
  
  
  static public void makeAllSpringsBidirectional(VerletParticle2D[] particles){
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint2D pa_spring = pa.springs[j];
        pa_spring.other.is_the_good_one = pa_spring.is_the_good_one = true;
      }
    }
  }
  
  static public SpringConstraint2D deleteSpring(VerletParticle2D pa, VerletParticle2D pb){
    SpringConstraint2D sa = pa.removeSpring(pb);
    SpringConstraint2D sb = pb.removeSpring(pa);
    
    if(sa.other != sb && sb.other != sa){
      System.out.println("error: SpringConstraints not Linked to each other!!");
    }
    return sa;
  }
  
  static public ArrayList<SpringConstraint2D> deleteSprings(VerletParticle2D pa){
    return deleteSprings(pa, (ArrayList<SpringConstraint2D>) null);
  }
  
  static public ArrayList<SpringConstraint2D> deleteSprings(VerletParticle2D pa, ArrayList<SpringConstraint2D> removed_springs){
    if(removed_springs == null) removed_springs = new ArrayList<SpringConstraint2D>();
    while(pa.spring_count > 0){
      VerletParticle2D pb = pa.springs[0].pb;
      removed_springs.add(deleteSpring(pa, pb));
    }
    return removed_springs;
  }
  
  static public ArrayList<SpringConstraint2D> deactivateSprings(VerletParticle2D pa){
    return deactivateSprings(pa, (ArrayList<SpringConstraint2D>) null);
  }
  static public ArrayList<SpringConstraint2D> deactivateSprings(VerletParticle2D pa, ArrayList<SpringConstraint2D> deactivated_springs){
    if(deactivated_springs == null) deactivated_springs = new ArrayList<SpringConstraint2D>();
    for(int j = 0; j < pa.spring_count; j++){
      SpringConstraint2D pa_spring = pa.springs[j];
      pa_spring.is_the_good_one       = false;
      pa_spring.other.is_the_good_one = false;
      deactivated_springs.add(pa_spring);
    }
    
    return deactivated_springs;
    
  }
  
}














