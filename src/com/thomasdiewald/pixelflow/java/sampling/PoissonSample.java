package com.thomasdiewald.pixelflow.java.sampling;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwCollisionObject;




/**
 * 
 * this class just serves as a quick example.
 * 
 * @author Thomas
 *
 */
public class PoissonSample implements DwCollisionObject{
  
  int collision_count = 0;
  float x, y, z, r, r_collision;
  
  public PoissonSample(float x, float y, float r){
    this(x,y,0,r,r);
  }
  public PoissonSample(float x, float y, float r, float r_collision){
    this(x,y,0,r,r_collision);
  }
  public PoissonSample(float x, float y, float z, float r, float r_collision){
    this.x = x;
    this.y = y;
    this.z = z;
    this.r = r;
    this.r_collision = r_collision;
  }

  @Override
  public void resetCollisionPtr() {
    collision_count = 0;
  }

  @Override
  public void update(DwCollisionObject othr) {
    if(this == othr) return;
    float rr = othr.radCollision() + this.radCollision();
    float dx = othr.x() - this.x();
    float dy = othr.y() - this.y();
    float dz = othr.z() - this.z();
    float dd_cur_sq = dx*dx + dy*dy + dz*dz;
    float dd_min_sq = rr * rr;
    
    collision_count += dd_cur_sq < dd_min_sq ? 1 : 0;
  }

  @Override public float x() { return x; }
  @Override public float y() { return y; }
  @Override public float z() { return z; }
  @Override public float rad() { return r;}
  @Override public float radCollision() { return r_collision; }
  @Override public int getCollisionCount(){ return collision_count; }
}