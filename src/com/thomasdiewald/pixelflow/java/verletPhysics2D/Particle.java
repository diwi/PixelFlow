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


public class Particle implements CollisionObject{
  
  // max radius among all particles, used for normalization, ...
  static public float MAX_RAD = 0; 
  
  static public class Param{
    public float COLLISION_DAMPING = 1;
    public float VELOCITY_DAMPING = 1;
    
    
    public float SPRINGCONSTRAINT_increase = 0.99999f;
    public float SPRINGCONSTRAINT_decrease = 0.99999f;
  }
  
  Param param = new Param();

  // index (must be unique)
  public int idx;
  
  // pinned to a position
  public boolean fixed = false;
  
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
  

  
  // display shape
  private PShape    shp_particle  = null;
  private PMatrix2D shp_transform = null;
  

  public Particle( int idx) {
    this.idx = idx;
  }
  public Particle( int idx, float x, float y, float rad) {
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
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // SPRING CONSTRAINT
  //////////////////////////////////////////////////////////////////////////////
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
  
  
  private float tmp_cx = 0;
  private float tmp_cy = 0;
  
  public void beginSpringIteration(){
    tmp_cx = cx;
    tmp_cy = cy;
  }
  
  // swap buffers, simulates pingpong
  public void endSpringIteration(int xmin, int ymin, int xmax, int ymax){
    cx = tmp_cx;
    cy = tmp_cy;
//    tmp_cx = 0;
//    tmp_cy = 0;
    // constrain bounds
    updateBounds(xmin, ymin, xmax, ymax);
  }
  
  
  public void updateSprings(Particle[] particles){
    
    if(fixed) return;
    
    // sum up force of attached springs
    float spring_x = 0;
    float spring_y = 0;
    Particle pa = this;
    for(int i = 0; i < spring_count; i++){
      SpringConstraint spring = springs[i];
      Particle pb = particles[spring.idx];
      
      float dx = pb.cx - pa.cx;
      float dy = pb.cy - pa.cy;
      float dd_curr_sq = dx*dx + dy*dy;
      float dd_rest_sq = spring.dd_rest_sq;
      float force = (dd_rest_sq / (dd_curr_sq + dd_rest_sq) - 0.5f);
      
//      force *= (dd_curr_sq < dd_rest_sq) ? spring.spring_inc : spring.spring_dec;
      
      force *= (dd_curr_sq < dd_rest_sq) ? param.SPRINGCONSTRAINT_increase : param.SPRINGCONSTRAINT_decrease;
      force *= SpringConstraint.SPRING_STABILIZATION;
      
      float pa_mass_factor = 2f * pb.mass / (pa.mass + pb.mass);
      float pb_mass_factor = 2f - pa_mass_factor;
      
      spring_x -= dx * force * pa_mass_factor;
      spring_y -= dy * force * pa_mass_factor;    
    }


    // prevent spring from exploding
    float spring_limit = 1f;
    float dd_sq = spring_x*spring_x + spring_y*spring_y;
    float max_shift = rad * 0.05f; // TODO
    if( dd_sq > max_shift*max_shift){
      spring_limit = max_shift / (float)Math.sqrt(dd_sq);
    }
    
    // add spring force
    tmp_cx += spring_limit * spring_x;
    tmp_cy += spring_limit * spring_y;
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
  
  
  public void updatePosition(int xmin, int ymin, int xmax, int ymax, float timestep) {
    if(fixed) return;
    
    float vx = (cx - px) * param.VELOCITY_DAMPING / mass;
    float vy = (cy - py) * param.VELOCITY_DAMPING / mass;
 
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
  // BOUNDARY COLLISION
  //////////////////////////////////////////////////////////////////////////////
  public void updateBounds(int xmin, int ymin, int xmax, int ymax){
    if(fixed){
      return;
    }
    
//    float damping = COLLISION_DAMPING;
//    if ((cx - rad) < xmin) { float vx = cx - px; cx = xmin + rad; px = cx + vx * damping; }
//    if ((cx + rad) > xmax) { float vx = cx - px; cx = xmax - rad; px = cx + vx * damping; }
//    if ((cy - rad) < ymin) { float vy = cy - py; cy = ymin + rad; py = cy + vy * damping; }
//    if ((cy + rad) > ymax) { float vy = cy - py; cy = ymax - rad; py = cy + vy * damping; }
    
    // friction
//    if ((cx - rad) < xmin) { float vx = cx - px, vy = cy - py; cx = xmin + rad; px = cx + vx * damping; py = cy - vy * damping; }
//    if ((cx + rad) > xmax) { float vx = cx - px, vy = cy - py; cx = xmax - rad; px = cx + vx * damping; py = cy - vy * damping; }
//    if ((cy - rad) < ymin) { float vx = cx - px, vy = cy - py; cy = ymin + rad; px = cx - vx * damping; py = cy + vy * damping; }
//    if ((cy + rad) > ymax) { float vx = cx - px, vy = cy - py; cy = ymax - rad; px = cx - vx * damping; py = cy + vy * damping; }
//    
    
    // causes damping in both axis, friction
    if ((cx - rad) < xmin) { cx = xmin + rad; px = cx; py = cy; }
    if ((cx + rad) > xmax) { cx = xmax - rad; px = cx; py = cy; }
    if ((cy - rad) < ymin) { cy = ymin + rad; px = cx; py = cy; }
    if ((cy + rad) > ymax) { cy = ymax - rad; px = cx; py = cy; }
  }


  
  //////////////////////////////////////////////////////////////////////////////
  // PARTICLE COLLISION
  //////////////////////////////////////////////////////////////////////////////
  public void updateCollision(Particle othr) {
    
    if(othr.collision_group == this.collision_group) return; // particles are of the same group
    if(this == othr    ) return; // not colliding with myself
    if(this == othr.tmp) return; // already collided with "othr"
    
    othr.tmp = this; // mark as checked
      
    float dx        = othr.cx - this.cx;
    float dy        = othr.cy - this.cy;
    float dd_cur_sq = dx*dx + dy*dy; // squared distance!
    float dd_min    = othr.rad + this.rad;
    float dd_min_sq = dd_min*dd_min;
    
    if (dd_cur_sq < dd_min_sq) { 
//      float dd_cur     = (float) Math.sqrt(dd_cur_sq);
//      float force      = 0.5f * (dd_min - dd_cur) / (dd_cur + 0.00001f);
//      float force_x    = dx * force * param.COLLISION_DAMPING;
//      float force_y    = dy * force * param.COLLISION_DAMPING;

      // http://www.gotoandplay.it/_articles/2005/08/advCharPhysics.php
      float force   = dd_min_sq / (dd_cur_sq + dd_min_sq) - 0.5f;
      float force_x = dx * force * param.COLLISION_DAMPING;
      float force_y = dy * force * param.COLLISION_DAMPING;
      
      float this_mass_factor = 2f * othr.mass / (this.mass + othr.mass);
      float othr_mass_factor = 2f - this_mass_factor;
      
      this.cx -= force_x * this_mass_factor;
      this.cy -= force_y * this_mass_factor;
      othr.cx += force_x * othr_mass_factor;
      othr.cy += force_y * othr_mass_factor;
    }
  }
  
  
  private Particle tmp = null;

  @Override
  public void beginCollision() {
    tmp = null;
  }
  
  @Override
  public void update(CollisionObject othr) {
    updateCollision((Particle)othr);
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
    shp_transform.reset();
    shp_transform.translate(cx, cy);
    shp_transform.rotate((float)Math.atan2(cx - px,cy - py));

    // update shape position
    shp_particle.resetMatrix();
    shp_particle.applyMatrix(shp_transform);
  }
  
  private final float[][] PALLETTE = 
    {
    {  50,  80,  130},    
    { 100, 178, 255}, 
    { 255, 120,  50},
//      {   25,    100,    255}, 
//      {   255,    0,    100}, 
  };

  private final void getShading(float val, float[] rgb){
    if(val < 0.0) val = 0.0f; else if(val >= 1.0) val = 0.99999f;
    float lum_steps = val * (PALLETTE.length-1);
    int   idx = (int)(Math.floor(lum_steps));
    float fract = lum_steps - idx;
    
    rgb[0] = PALLETTE[idx][0] * (1-fract) +  PALLETTE[idx+1][0] * fract;
    rgb[1] = PALLETTE[idx][1] * (1-fract) +  PALLETTE[idx+1][1] * fract;
    rgb[2] = PALLETTE[idx][2] * (1-fract) +  PALLETTE[idx+1][2] * fract;
  }

  
  private int clamp(float v){
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