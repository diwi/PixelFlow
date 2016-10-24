/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.softbodydynamics.particle;


import com.thomasdiewald.pixelflow.java.accelerationstructures.DwCollisionObject;

import processing.core.PMatrix2D;
import processing.core.PShape;


public class DwParticle2D extends DwParticle{

  public float cx = 0, cy = 0; // current position
  public float px = 0, py = 0; // previous position
  public float ax = 0, ay = 0; // acceleration

  public DwParticle2D(int idx) {
    super(idx);
  }
  
  public DwParticle2D(int idx, float x, float y, float rad) {
    super(idx);
    setPosition(x, y);
    setRadius(rad);
  }
  
  public DwParticle2D(int idx, float x, float y, float rad, Param param) {
    super(idx);
    setPosition(x, y);
    setRadius(rad);
    setParamByRef(param);
  }
  
  public void setPosition(float x, float y){
    this.cx = this.px = x;
    this.cy = this.py = y;
  }
  
  
 
  
  



  
  
  //////////////////////////////////////////////////////////////////////////////
  // VERLET INTEGRATION
  //////////////////////////////////////////////////////////////////////////////


  @Override
  public void moveTo(float[] cnew, float damping){
    px  = cx;
    py  = cy;
    cx += (cnew[0] - cx) * damping;
    cy += (cnew[1] - cy) * damping;
  }
  
  @Override
  public void addForce(float[] anew){
    this.ax += anew[0] / mass;
    this.ay += anew[1] / mass;
  }

  @Override
  public void addGravity(float[] gravity){
    this.ax += gravity[0];
    this.ay += gravity[1];
  }
  
  @Override
  public void updatePosition(float timestep) {
    if(enable_forces){
      // velocity
      float vx = (cx - px) * param.DAMP_VELOCITY;
      float vy = (cy - py) * param.DAMP_VELOCITY;
      
      px = cx;
      py = cy;
  
      // verlet integration
      cx += vx + ax * 0.5 * timestep * timestep;
      cy += vy + ay * 0.5 * timestep * timestep;
    }
    ax = ay = 0;
  }
  
  
  //////////////////////////////////////////////////////////////////////////////
  // PARTICLE COLLISION
  //////////////////////////////////////////////////////////////////////////////
  public void updateCollision(DwParticle2D othr) {

    if(!enable_collisions) return;
    if(othr.collision_group == this.collision_group) return; // particles are of the same group
    if(this == othr              ) return; // not colliding with myself
    if(this == othr.collision_ptr) return; // already collided with "othr"
    
    othr.collision_ptr = this; // mark as checked
      
    float dx        = othr.cx - this.cx;
    float dy        = othr.cy - this.cy;
    float dd_cur_sq = dx*dx + dy*dy;
    float dd_min    = othr.rad_collision + this.rad_collision;
    float dd_min_sq = dd_min*dd_min;
    
    if (dd_cur_sq < dd_min_sq) { 
      float this_mass_factor = 2f * othr.mass / (this.mass + othr.mass);
      // http://www.gotoandplay.it/_articles/2005/08/advCharPhysics.php
      float force = (dd_min_sq / (dd_cur_sq + dd_min_sq) - 0.5f) * param.DAMP_COLLISION;

      this.collision_x -= dx * force * this_mass_factor;
      this.collision_y -= dy * force * this_mass_factor;
      this.collision_count++;
    }
  }
  

  // collision force
  private float collision_x;
  private float collision_y;
  
  @Override
  public void beforeCollision(){
    collision_x = collision_y = 0;
    collision_count = 0;
  }
  
  @Override
  public void afterCollision(){
    float limit = 1f;
    cx += collision_x * limit;
    cy += collision_y * limit;
  }
  
  

  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // BOUNDARY COLLISION
  //////////////////////////////////////////////////////////////////////////////
  @Override
  public void updateBounds(float[] bounds){
    if(!enable_collisions) return;
    final float[] bd = bounds;
    float vx, vy;
    float damping = param.DAMP_BOUNDS;
    float r = rad;    
    if ((cx - r) < bd[0]) {vx=cx-px;vy=cy-py; cx=bd[0]+r;px=cx+vx*damping;py=cy-vy*damping;}
    if ((cx + r) > bd[2]) {vx=cx-px;vy=cy-py; cx=bd[2]-r;px=cx+vx*damping;py=cy-vy*damping;}
    if ((cy - r) < bd[1]) {vx=cx-px;vy=cy-py; cy=bd[1]+r;px=cx-vx*damping;py=cy+vy*damping;}
    if ((cy + r) > bd[3]) {vx=cx-px;vy=cy-py; cy=bd[3]-r;px=cx-vx*damping;py=cy+vy*damping;}
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  @Override
  public final float x() {
    return cx;
  }

  @Override
  public final float y() {
    return cy;
  }
  
  @Override
  public final float z() {
    return 0;
  }

  @Override
  public final void update(DwCollisionObject othr) {
    updateCollision((DwParticle2D)othr);
  }
  

  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // DISPLAY
  ////////////////////////////////////////////////////////////////////////////// 
  
  @Override
  public void setShape(PShape shape){
    shp_particle = shape;
    shp_transform = new PMatrix2D();
    updateShapePosition();
  }
  
  
  @Override
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

  @Override
  public float getVelocity(){
    float vx = cx - px;
    float vy = cy - py;
    return (float) Math.sqrt(vx*vx + vy*vy);
  }
  
  
  
  

}