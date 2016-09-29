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

import processing.core.PMatrix3D;
import processing.core.PShape;

public class DwParticle3D extends DwParticle{
  
  public float cx = 0, cy = 0, cz = 0; // current position
  public float px = 0, py = 0, pz = 0; // previous position
  public float ax = 0, ay = 0, az = 0; // acceleration

  public DwParticle3D(int idx, float x, float y, float z, float rad) {
    super(idx);
    setPosition(x, y, z);
    setRadius(rad);
  }
  
  public DwParticle3D(int idx, float x, float y, float z, float rad, Param param) {
    super(idx);
    setPosition(x, y, z);
    setRadius(rad);
    setParamByRef(param);
  }
  
  public void setPosition(float x, float y, float z){
    this.cx = this.px = x;
    this.cy = this.py = y;
    this.cz = this.pz = z;
  }
  

  //////////////////////////////////////////////////////////////////////////////
  // VERLET INTEGRATION
  //////////////////////////////////////////////////////////////////////////////


  @Override
  public void moveTo(float[] cnew, float damping){
    px  = cx;
    py  = cy;
    pz  = cz;
    cx += (cnew[0] - cx) * damping;
    cy += (cnew[1] - cy) * damping;
    cz += (cnew[2] - cz) * damping;
  }
  
  @Override
  public void addForce(float[] anew){
    this.ax += anew[0] / mass;
    this.ay += anew[1] / mass;
    this.az += anew[2] / mass;
  }

  @Override
  public void addGravity(float[] gravity){
    this.ax += gravity[0];
    this.ay += gravity[1];
    this.az += gravity[2];
  }
  
  @Override
  public void updatePosition(float timestep) {
    if(enable_forces){
      // velocity
      float vx = (cx - px) * param.DAMP_VELOCITY;
      float vy = (cy - py) * param.DAMP_VELOCITY;
      float vz = (cz - pz) * param.DAMP_VELOCITY;
      
      px = cx;
      py = cy;
      pz = cz;
  
      // verlet integration
      cx += vx + ax * 0.5 * timestep * timestep;
      cy += vy + ay * 0.5 * timestep * timestep;
      cz += vz + az * 0.5 * timestep * timestep;
    }
    ax = ay = az = 0;
  }

  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // PARTICLE COLLISION
  //////////////////////////////////////////////////////////////////////////////
  public void updateCollision(DwParticle3D othr) {

    if(!enable_collisions) return;
    if(othr.collision_group == this.collision_group) return; // particles are of the same group
    if(this == othr              ) return; // not colliding with myself
    if(this == othr.collision_ptr) return; // already collided with "othr"
    
    othr.collision_ptr = this; // mark as checked
      
    float dx        = othr.cx - this.cx;
    float dy        = othr.cy - this.cy;
    float dz        = othr.cz - this.cz;
    float dd_cur_sq = dx*dx + dy*dy + dz*dz;
    float dd_min    = othr.rad_collision + this.rad_collision;
    float dd_min_sq = dd_min*dd_min;
    
    if (dd_cur_sq < dd_min_sq) { 
      float this_mass_factor = 2f * othr.mass / (this.mass + othr.mass);
      // http://www.gotoandplay.it/_articles/2005/08/advCharPhysics.php
      float force = (dd_min_sq / (dd_cur_sq + dd_min_sq) - 0.5f) * param.DAMP_COLLISION;

      this.collision_x -= dx * force * this_mass_factor;
      this.collision_y -= dy * force * this_mass_factor;
      this.collision_z -= dz * force * this_mass_factor;
      this.collision_count++;
    }
  }
  

  // collision force
  private float collision_x;
  private float collision_y;
  private float collision_z;
  
  @Override
  public void beforeCollision(){
    collision_x = collision_y = collision_z = 0;
    collision_count = 0;
  }
  
  @Override
  public void afterCollision(){
    float limit = 1f;
    cx += collision_x * limit;
    cy += collision_y * limit;
    cz += collision_z * limit;
  }
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // BOUNDARY COLLISION
  //////////////////////////////////////////////////////////////////////////////
  @Override
  public void updateBounds(float[] bounds){
    if(!enable_collisions) return;
    final float[] bd = bounds;
    float vx, vy, vz;
    float damping = param.DAMP_BOUNDS;
    if ((cx - rad) < bd[0]) {vx=cx-px;vy=cy-py;vz=cz-pz; cx=bd[0]+rad;px=cx+vx*damping;py=cy-vy*damping;pz=cz-vz*damping;}
    if ((cx + rad) > bd[3]) {vx=cx-px;vy=cy-py;vz=cz-pz; cx=bd[3]-rad;px=cx+vx*damping;py=cy-vy*damping;pz=cz-vz*damping;}
    if ((cy - rad) < bd[1]) {vx=cx-px;vy=cy-py;vz=cz-pz; cy=bd[1]+rad;px=cx-vx*damping;py=cy+vy*damping;pz=cz-vz*damping;}
    if ((cy + rad) > bd[4]) {vx=cx-px;vy=cy-py;vz=cz-pz; cy=bd[4]-rad;px=cx-vx*damping;py=cy+vy*damping;pz=cz-vz*damping;}
    if ((cz - rad) < bd[2]) {vx=cx-px;vy=cy-py;vz=cz-pz; cz=bd[2]+rad;px=cx-vx*damping;py=cy-vy*damping;pz=cz+vz*damping;}
    if ((cz + rad) > bd[5]) {vx=cx-px;vy=cy-py;vz=cz-pz; cz=bd[5]-rad;px=cx-vx*damping;py=cy-vy*damping;pz=cz+vz*damping;}
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
    return cz;
  }

  @Override
  public final void update(DwCollisionObject othr) {
    updateCollision((DwParticle3D)othr);
  }
  
  

  
  //////////////////////////////////////////////////////////////////////////////
  // DISPLAY
  //////////////////////////////////////////////////////////////////////////////
  @Override
  public void setShape(PShape shape){
    shp_particle = shape;
    shp_transform = new PMatrix3D();
    updateShapePosition();
  }
  
  @Override
  public void updateShapePosition(){
    // build transformation matrix
    if(shp_transform != null){
      shp_transform.reset();
      shp_transform.translate(cx, cy, cz);
//      shp_transform.rotate((float)Math.atan2(cy-py, cx-px));
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
    float vz = cz - pz;
    return (float) Math.sqrt(vx*vx + vy*vy + vz*vz);
  }
  
  
  
  // TODO, move to some Utils class
  static public int crossAccum(DwParticle3D p, DwParticle3D pA, DwParticle3D pB, float[] cross){
    if(pA == null || pA.all_springs_deactivated ||
       pB == null || pB.all_springs_deactivated)
    {
      return 0;
    } else {
      float dxA = pA.cx - p.cx;
      float dyA = pA.cy - p.cy;
      float dzA = pA.cz - p.cz;
      
      float dxB = pB.cx - p.cx;
      float dyB = pB.cy - p.cy;
      float dzB = pB.cz - p.cz;
      
      cross[0] += (dyA * dzB) - (dyB * dzA);
      cross[1] += (dzA * dxB) - (dzB * dxA);
      cross[2] += (dxA * dyB) - (dxB * dyA);
      return 1;
    }
  }

  
}