/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.verletPhysics2D;

import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.CollisionObject;

import processing.core.PMatrix2D;
import processing.core.PShape;


public class VerletParticle2D implements CollisionObject{
  
  // max radius among all particles, can be used for normalization, ...
  static public float MAX_RAD = 0; 
  
  static public class Param{
    public float DAMP_BOUNDS    = 1;
    public float DAMP_COLLISION = 1;
    public float DAMP_VELOCITY  = 1;
    public float DAMP_SPRING_increase = 0.99999f;
    public float DAMP_SPRING_decrease = 0.99999f;
  }
  
  Param param = new Param();

  // index (must be unique)
  public int idx;
  
  // pinned to a position
  public boolean enable_collisions = true;
  public boolean enable_springs = true;
  public boolean enable_forces  = true;
  

  
  // verlet integration
  public float cx = 0, cy = 0; // current position
  public float px = 0, py = 0; // previous position
  public float ax = 0, ay = 0; // acceleration
  public float rad  = 0;       // radius
  public float mass = 1f;      // mass

  // Spring Constraints
  public int spring_count = 0;
  public SpringConstraint[] springs = null;
  
  
  // don'd apply collision on particles within the same group
  public int collision_group;
  public int collision_count;

  
  // display shape
  private PShape    shp_particle  = null;
  private PMatrix2D shp_transform = null;
  

  public VerletParticle2D( int idx) {
    this.idx = idx;
  }
  public VerletParticle2D( int idx, float x, float y, float rad) {
    this.idx = idx;
    setPosition(x, y);
    setRadius(rad);
  }
  public void setPosition(float x, float y){
    this.cx = x;
    this.cy = y;
    this.px = x;
    this.py = y;
  }
  
  public void setRadius(float rad_){
    rad = Math.max(rad_, 0.1f);
  }
  
  public void setMass(float mass){
    this.mass = mass;
  }
  
  public void setParamByRef(Param param){
    this.param = param;
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
  
  public void addSpring(SpringConstraint spring){
    if(springs == null || spring_count >= springs.length){
      int new_len = (int) Math.max(2, Math.ceil(spring_count*1.5f) );
      if( springs == null){
        springs = new SpringConstraint[new_len];
      } else {
        springs = Arrays.copyOf(springs, new_len);
      }
    }
    springs[spring_count++] = spring;
  }
  
  
  
  
  // spring force
  private float spring_x = 0;
  private float spring_y = 0;
  
  public void beforeSprings(){
    spring_x = spring_y = 0;
  }
  public void afterSprings(float xmin, float ymin, float xmax, float ymax){
    
    // prevent spring from exploding
    float limit = 1f;
    float dd_sq = spring_x*spring_x + spring_y*spring_y;
    float dd_max =  rad/(float)(spring_count);
    if( dd_sq > dd_max*dd_max){
      limit = dd_max / (float)Math.sqrt(dd_sq);
    }
    
    cx += spring_x * limit;
    cy += spring_y * limit;
    updateBounds(xmin, ymin, xmax, ymax);
  }
  
  
  // collision force
  private float collision_x;
  private float collision_y;
  
  public void beforeCollision(){
    collision_x = collision_y = 0;
    collision_count = 0;
  }
  public void afterCollision(float xmin, float ymin, float xmax, float ymax){
    // prevent explosions
    float limit = 1f;
    float dd_sq = collision_x*collision_x + collision_y*collision_y;
    float dd_max = rad/(float)(collision_count);

    if( dd_sq > dd_max*dd_max){
      limit = dd_max / (float)Math.sqrt(dd_sq);
    }
    
    cx += collision_x * limit;
    cy += collision_y * limit;
    
    updateBounds(xmin, ymin, xmax, ymax);
  }
  



  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // VERLET INTEGRATION
  //////////////////////////////////////////////////////////////////////////////
  public void addForce(float ax, float ay){
    this.ax += ax / mass;
    this.ay += ay / mass;
  }
  
  public void addGravity(float gx, float gy){
    this.ax += gx;
    this.ay += gy;
  }
  
  
  public void updatePosition(float xmin, float ymin, float xmax, float ymax, float timestep) {
    if(!enable_forces) return;
    
    float vx = (cx - px) * param.DAMP_VELOCITY;
    float vy = (cy - py) * param.DAMP_VELOCITY;
 
    px = cx;
    py = cy;

    // verlet integration
    cx += vx + ax * 0.5 * timestep * timestep;
    cy += vy + ay * 0.5 * timestep * timestep;
    
    ax = ay = 0;
    
    
    // constrain bounds
    updateBounds(xmin, ymin, xmax, ymax);
  }
  

  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // SPRING CONSTRAINT
  //////////////////////////////////////////////////////////////////////////////

  
  public void updateSprings(VerletParticle2D[] particles){
    
    if(!enable_springs) return;
    
    // sum up force of attached springs
    VerletParticle2D pa = this;

    for(int i = 0; i < spring_count; i++){
      SpringConstraint spring = springs[i];
      VerletParticle2D pb = particles[spring.idx];
      
      float dx = pb.cx - pa.cx;
      float dy = pb.cy - pa.cy;
      float dd_curr_sq = dx*dx + dy*dy;
      float dd_rest_sq = spring.dd_rest_sq;
      float force = (dd_rest_sq / (dd_curr_sq + dd_rest_sq) - 0.5f);
      
      float pa_mass_factor = 2f * pb.mass / (pa.mass + pb.mass);
//    float pb_mass_factor = 2f - pa_mass_factor;
      
//      force *= (dd_curr_sq < dd_rest_sq) ? spring.spring_inc : spring.spring_dec;
      force *= (dd_curr_sq < dd_rest_sq) ? param.DAMP_SPRING_increase : param.DAMP_SPRING_decrease;
      force *= SpringConstraint.SPRING_STABILIZATION;
      
      spring_x -= dx * force * pa_mass_factor;
      spring_y -= dy * force * pa_mass_factor;    
    }
  }
  


  
  
  //////////////////////////////////////////////////////////////////////////////
  // PARTICLE COLLISION
  //////////////////////////////////////////////////////////////////////////////
  public void updateCollision(VerletParticle2D othr) {

    if(!enable_collisions) return;
    if(othr.collision_group == this.collision_group) return; // particles are of the same group
    if(this == othr              ) return; // not colliding with myself
    if(this == othr.collision_ptr) return; // already collided with "othr"
    
    othr.collision_ptr = this; // mark as checked
      
    float dx        = othr.cx - this.cx;
    float dy        = othr.cy - this.cy;
    float dd_cur_sq = dx*dx + dy*dy; // squared distance!
    float dd_min    = othr.rad + this.rad;
    float dd_min_sq = dd_min*dd_min;
    
    if (dd_cur_sq < dd_min_sq) { 
      float this_mass_factor = 2f * othr.mass / (this.mass + othr.mass);
//      float othr_mass_factor = 2f - this_mass_factor;
      
//      float dd_cur     = (float) Math.sqrt(dd_cur_sq);
//      float force      = (0.5f * (dd_min - dd_cur) / (dd_cur + 0.00001f)) * param.COLLISION_DAMPING;
//      float force_x    = dx * force * param.COLLISION_DAMPING;
//      float force_y    = dy * force * param.COLLISION_DAMPING;

      // http://www.gotoandplay.it/_articles/2005/08/advCharPhysics.php
      float force   = (dd_min_sq / (dd_cur_sq + dd_min_sq) - 0.5f) * param.DAMP_COLLISION;

      this.collision_x -= dx * force * this_mass_factor;
      this.collision_y -= dy * force * this_mass_factor;

      collision_count++;
//      if(this.enable_collisions)
//      {
//        this.cx -= dx * force * this_mass_factor;
//        this.cy -= dy * force * this_mass_factor;
//      }
//      if(othr.enable_collisions)
//      {
//        othr.cx += dx * force * othr_mass_factor;
//        othr.cy += dy * force * othr_mass_factor;
//      }
    }
  }
  

  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // BOUNDARY COLLISION
  //////////////////////////////////////////////////////////////////////////////
  public void updateBounds(float xmin, float ymin, float xmax, float ymax){
    if(!enable_collisions){
      return;
    }
    
    float damping = param.DAMP_BOUNDS;
//    if ((cx - rad) < xmin) { float vx = cx - px; cx = xmin + rad; px = cx + vx * damping; }
//    if ((cx + rad) > xmax) { float vx = cx - px; cx = xmax - rad; px = cx + vx * damping; }
//    if ((cy - rad) < ymin) { float vy = cy - py; cy = ymin + rad; py = cy + vy * damping; }
//    if ((cy + rad) > ymax) { float vy = cy - py; cy = ymax - rad; py = cy + vy * damping; }
    
    // friction
    
    
    if ((cx - rad) < xmin) { float vx = cx - px, vy = cy - py; cx = xmin + rad; px = cx + vx * damping; py = cy - vy * damping; }
    if ((cx + rad) > xmax) { float vx = cx - px, vy = cy - py; cx = xmax - rad; px = cx + vx * damping; py = cy - vy * damping; }
    if ((cy - rad) < ymin) { float vx = cx - px, vy = cy - py; cy = ymin + rad; px = cx - vx * damping; py = cy + vy * damping; }
    if ((cy + rad) > ymax) { float vx = cx - px, vy = cy - py; cy = ymax - rad; px = cx - vx * damping; py = cy + vy * damping; }
    

    // causes damping in both axis, friction
//    if ((cx - rad) < xmin) { cx = xmin + rad; px = cx; py = cy; }
//    if ((cx + rad) > xmax) { cx = xmax - rad; px = cx; py = cy; }
//    if ((cy - rad) < ymin) { cy = ymin + rad; px = cx; py = cy; }
//    if ((cy + rad) > ymax) { cy = ymax - rad; px = cx; py = cy; }
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  private VerletParticle2D collision_ptr = null;

  @Override
  public void resetCollisionPtr() {
    collision_ptr = null;
  }
  
  @Override
  public void update(CollisionObject othr) {
    updateCollision((VerletParticle2D)othr);
  }

  @Override
  public float x() {
    return cx;
  }

  @Override
  public float y() {
    return cy;
  }

  @Override
  public float rad() {
    return rad;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // DISPLAY
  //////////////////////////////////////////////////////////////////////////////
  public void setShape(PShape shape){
    shp_particle = shape;
    shp_transform = new PMatrix2D();
    updateShapePosition();
  }

  public void setColor(int col_argb){
    shp_particle.setTint(col_argb);
    shp_particle.setFill(col_argb);
  }
  
  public void updateShape(){
    updateShapePosition();
    updateShapeColor();
  }
  
  public void updateShapePosition(){
    // build transformation matrix
    if(shp_transform != null){
      shp_transform.reset();
      shp_transform.translate(cx, cy);
      shp_transform.rotate((float)Math.atan2(cy-py, cx-px));
    }

    // update shape position
    if(shp_particle != null){
      shp_particle.resetMatrix();
      shp_particle.applyMatrix(shp_transform);
    }
  }
  
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
    float vel  = getVelcoity();
    float radn = 1.1f * rad / MAX_RAD;

    getShading(vel * 0.5f, rgb);
    int a = 255;
    int r = clamp(rgb[0] * radn) & 0xFF;
    int g = clamp(rgb[1] * radn) & 0xFF;
    int b = clamp(rgb[2] * radn) & 0xFF;

    int col = a << 24 | r << 16 | g << 8 | b;
    setColor(col);
  }
  
  public float getVelcoity(){
    float vx = cx-px;
    float vy = cy-py;
    return (float) Math.sqrt(vx*vx + vy*vy);
  }
  
  
  
  

}