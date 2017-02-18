/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.softbodydynamics.particle;


import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwCollisionObject;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;

import processing.core.PMatrix;
import processing.core.PShape;


public abstract class DwParticle implements DwCollisionObject{
  
  // max radius among all particles, can be used for normalization, ...
  static public float MAX_RAD = 0; 
  
  static public class Param{
    public float DAMP_BOUNDS    = 1;
    public float DAMP_COLLISION = 1;
    public float DAMP_VELOCITY  = 1;
  }
  
  Param param = new Param();

  
  // index (must be unique)
  // must match the position of the array, for indexing
  public int idx;
  
  
  // pinned to a position
  public boolean enable_collisions = true;
  public boolean enable_springs    = true;
  public boolean enable_forces     = true;
  

  public float rad  = 0;           // radius
  public float rad_collision  = 0; // collision radius
  public float mass = 1f;          // mass

  
  // Spring Constraints
  public int spring_count = 0;
  public DwSpringConstraint[] springs = null;
  public boolean all_springs_deactivated = false;
  
  
  // don'd apply collision on particles within the same group
  public int collision_group;
  public int collision_count;

  
  // display shape
  protected PShape  shp_particle  = null;
  protected PMatrix shp_transform = null;
  

  public DwParticle(int idx) {
    this.idx = idx;
    this.collision_group = idx;
  }
  
  public DwParticle(int idx, float rad) {
    this.idx = idx;
    this.collision_group = idx;
    setRadius(rad);
  }
  
  public DwParticle(int idx, float rad, Param param) {
    this.idx = idx;
    this.collision_group = idx;
    setRadius(rad);
    setParamByRef(param);
  }
  
  public void setRadius(float rad_){
    rad = Math.max(rad_, 0.1f);
    rad_collision = rad;
  }
  public void setRadiusCollision(float rad_collision_){
    rad_collision = Math.max(rad_collision_, 0.1f);
  }
  
  public void setMass(float mass){
    this.mass = mass;
  }
  
  public void setParamByRef(Param param){
    if(param != null){
      this.param = param;
    }
  }
  
  public void setCollisionGroup(int id){
    collision_group = id;
  }
  
  public void enableCollisions(boolean enable_collisions){
    this.enable_collisions = enable_collisions;
  }
  
  public void enableSprings(boolean enable_springs){
    this.enable_springs = enable_springs;
  }
  
  public void enableForces(boolean enable_forces){
    this.enable_forces = enable_forces;
  }
  
  public void enable(boolean enable_collisions, boolean enable_springs, boolean enable_forces){
    this.enable_collisions = enable_collisions;
    this.enable_springs = enable_springs;
    this.enable_forces = enable_forces;
  }
  
  


  public void addSpring(DwSpringConstraint spring){
    
    // TODO, sorted by id, etc...

//    // make sure we don't have multiple springs to the same vertex.
//    int pos = 0;
//    while(pos < spring_count && springs[pos].pb.idx <= spring.pb.idx) pos++;
//    
//    // already in the list, so return
//    if(pos > 0 && springs[pos-1].pb == spring.pb) return;
    
    // realloc if necessary
    if(springs == null || spring_count >= springs.length){
      int new_len = (int) Math.max(2, Math.ceil(spring_count*1.5f) );
      if( springs == null){
        springs = new DwSpringConstraint[new_len];
      } else {
        springs = Arrays.copyOf(springs, new_len);
      }
    }
    
    // shift data to the right, by one
//    System.arraycopy(springs, pos, springs, pos+1, spring_count-pos);
//    springs[pos] = spring;
//    spring_count++;
    
    springs[spring_count++] = spring;

    // check correct sorting
//    for(int i = 1; i < spring_count; i++){
//      if( springs[i].pb.idx <  springs[i-1].pb.idx) System.out.println("ERROR");
//    }
  }
  
//  protected DwSpringConstraint removeSpring(DwParticle3D pb){
//    DwSpringConstraint removed = null;
//    int pos = 0;
//    for(pos = 0; pos < spring_count; pos++){
//      if(springs[pos].pb == pb){
//        removed = springs[pos];
//        break;
//      }
//    }
//    if(removed != null){
//      System.arraycopy(springs, pos+1, springs, pos, spring_count-(pos+1));
//      spring_count--;
//    }
//    return removed;
//  }
  
  

  public void enableAllSprings(boolean enable){
    all_springs_deactivated = !enable;
    
    for(int i = 0; i < spring_count; i++){
      springs[i].enable(enable);
    }
  }
  



  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // VERLET INTEGRATION
  //////////////////////////////////////////////////////////////////////////////
  public abstract void moveTo        (float[] cnew, float damping);
  public abstract void addForce      (float[] anew); 
  public abstract void addGravity    (float[] gravity);
  public abstract void updatePosition(float timestep);


  

  
  
  
  
  
  
  
  

  
  
  //////////////////////////////////////////////////////////////////////////////
  // PARTICLE COLLISION
  //////////////////////////////////////////////////////////////////////////////
  public abstract void beforeCollision();
  public abstract void afterCollision();
  
  
  
  
  //////////////////////////////////////////////////////////////////////
  // BOUNDARY COLLISION
  ////////////////////////////////////////////////////////////////////////////// 
  public abstract void updateBounds(float[] bounds);
  
  
  
  protected DwParticle collision_ptr = null;

  @Override
  public final void resetCollisionPtr() {
    collision_ptr = null;
  }
  
  @Override
  public final float rad() {
    return rad;
  }
  
  @Override
  public final float radCollision() {
    return rad_collision;
  }

  
  @Override
  public int getCollisionCount(){
    return collision_count;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // DISPLAY
  //////////////////////////////////////////////////////////////////////////////
  public abstract void setShape(PShape shape);

  public void setColor(int col_argb){
    if(shp_particle != null){
      shp_particle.setTint(col_argb);
      shp_particle.setFill(col_argb);
    }
  }
  
  
  public void updateShape(){
    updateShapePosition();
    updateShapeColor();
  }
  
  public abstract void updateShapePosition();
  
  protected final float[][] PALLETTE = 
    {
    {  50,  80,  130},    
    { 100, 178, 255}, 
    { 255, 120,  50},
//      {   25,    100,    255}, 
//      {   255,    0,    100}, 
  };

  protected final void getShading(float val, float[] rgb){
    if(val < 0.0) val = 0.0f; else if(val >= 1.0) val = 0.99999f;
    float lum_steps = val * (PALLETTE.length-1);
    int   idx = (int)(Math.floor(lum_steps));
    float fract = lum_steps - idx;
    
    rgb[0] = PALLETTE[idx][0] * (1-fract) +  PALLETTE[idx+1][0] * fract;
    rgb[1] = PALLETTE[idx][1] * (1-fract) +  PALLETTE[idx+1][1] * fract;
    rgb[2] = PALLETTE[idx][2] * (1-fract) +  PALLETTE[idx+1][2] * fract;
  }

  
  protected int clamp(float v){
    if( v <   0 ) return 0;
    if( v > 255 ) return 255;
    return (int)v;
  }
  
  private final float[] rgb = new float[3];
  
  public void updateShapeColor(){
    float vel  = getVelocity();
    float radn = 1.1f * rad / MAX_RAD;

    getShading(vel, rgb);
    int a = 255;
    int r = clamp(rgb[0] * radn) & 0xFF;
    int g = clamp(rgb[1] * radn) & 0xFF;
    int b = clamp(rgb[2] * radn) & 0xFF;
    
    int col = a << 24 | r << 16 | g << 8 | b;
    setColor(col);
  }
  
  public abstract float getVelocity();

}