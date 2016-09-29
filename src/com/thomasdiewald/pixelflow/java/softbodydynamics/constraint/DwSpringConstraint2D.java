/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.softbodydynamics.constraint;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwPair;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;


public class DwSpringConstraint2D extends DwSpringConstraint{
  
  public DwParticle2D pa, pb;

  private DwSpringConstraint2D(DwParticle2D pa, DwParticle2D pb, Param param, TYPE type){
    this(pa, pb, 0, param, type);
    updateRestlength();
  }
  
  private DwSpringConstraint2D(DwParticle2D pa, DwParticle2D pb, float spring_len_sq, Param param){
    this(pa, pb, spring_len_sq, param, TYPE.STRUCT);
  }

  private DwSpringConstraint2D(DwParticle2D pa, DwParticle2D pb, float spring_len_sq, Param param, TYPE type){
    if(pa.idx < pb.idx){ DwParticle2D pt = pa; pa = pb; pb = pt; } // swap
    this.pa         = pa;
    this.pb         = pb;
    this.param      = param;
    this.type       = type;
    this.dd_rest_sq = spring_len_sq;
    this.dd_rest    = (float) Math.sqrt(dd_rest_sq);
  }
  
  @Override
  public void updateRestlength(){
    float dx        = pb.cx - pa.cx;
    float dy        = pb.cy - pa.cy;
    this.dd_rest_sq = dx*dx + dy*dy;
    this.dd_rest    = (float) Math.sqrt(dd_rest_sq);
  }
  
  @Override
  public float computeForce(){
    float dx    = pb.cx - pa.cx;
    float dy    = pb.cy - pa.cy;
    float dd_sq = dx*dx + dy*dy;
//    float dd    = (float) Math.sqrt(dd_sq);
//    float force = (0.5f * (dd_rest - dd) / (dd + 0.00001f));
    float force = (dd_rest_sq / (dd_sq + dd_rest_sq) - 0.5f);
    return force;
  }
  
  @Override
  public void update(){
    if(!enabled) return;
    float dx    = pb.cx - pa.cx;
    float dy    = pb.cy - pa.cy;
    float dd_sq = dx*dx + dy*dy;
//    float dd    = (float) Math.sqrt(dd_sq);
//    force       = (0.5f * (dd_rest - dd) / (dd + 0.00001f));
    force       = (dd_rest_sq / (dd_sq + dd_rest_sq) - 0.5f);
    force      *= (dd_sq < dd_rest_sq) ? param.damp_inc: param.damp_dec; 
    
    float pa_mass_factor = 2f * pb.mass / (pa.mass + pb.mass);
    float pb_mass_factor = 2f - pa_mass_factor;

    if(pa.enable_springs){
      pa.cx -= dx * force * pa_mass_factor;
      pa.cy -= dy * force * pa_mass_factor;  
    } 
    if(pb.enable_springs){
      pb.cx += dx * force * pb_mass_factor;
      pb.cy += dy * force * pb_mass_factor; 
    }
  }
  
  
  static public DwSpringConstraint addSpring(DwPhysics<DwParticle2D> physics, DwParticle2D pa, DwParticle2D pb, float rest_length, Param param){
    DwSpringConstraint spring = addSpring(physics, pa, pb, param, TYPE.STRUCT); 
    spring.dd_rest = rest_length;
    spring.dd_rest_sq = rest_length*rest_length;
    return spring;
  }
  
  static public DwSpringConstraint addSpring(DwPhysics<DwParticle2D> physics, DwParticle2D pa, DwParticle2D pb, Param param){
    return addSpring(physics, pa, pb, param, TYPE.STRUCT);
  }
  static public DwSpringConstraint addSpring(DwPhysics<? extends DwParticle> physics, DwParticle2D pa, DwParticle2D pb, Param param, TYPE type){
    if(pa == pb) return null;
    
    if(pa.idx < pb.idx){ DwParticle2D pt = pa; pa = pb; pb = pt; } // swap
 
    DwPair<DwParticle> pair = new DwPair<DwParticle>(pa, pb);
    DwSpringConstraint spring = physics.spring_map.get(pair);
    if(spring == null){
      spring = new DwSpringConstraint2D(pa, pb, param, type);
      physics.spring_map.put(pair, spring);
      physics.springs.add(spring);
      
      pa.addSpring(spring);
      pb.addSpring(spring);
    } 
    return spring;
  }


  @Override
  public int idxPa() {
    return pa.idx;  
  }
  @Override
  public int idxPb() {
    return pb.idx;  
  }

}














