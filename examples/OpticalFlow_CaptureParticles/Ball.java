/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package OpticalFlow_CaptureParticles;

import processing.core.PGraphics;

public class Ball {
  

  static float MAX_RAD;
  
  // STATIC GLOBAL controlling variables
  
  // gravity
  static public float GRAVITY = 0.005f;
  
  // collisions
  static public float COLLISION_SPRING  = 0.05f; // collision-damping for: ball <-> ball
  static public float COLLISION_DAMPING = 0.80f; // collision-damping for: ball <-> solid
  
  static public float VELOCITY_DISSIPATION = 0.991f; // gobal viscosity
  
  // fluid
  static public float FLUID_INERTIA     = 0.40f;
  static public float FLUID_DISSIPATION = 0.90f;
  static public float FLUID_SCALE       = 0.002f;


  //position, radius
  public float x, y, rad, mass;

  // final velocity
  public float vx = 0; 
  public float vy = 0;


  // fluid velocity
  public float fluid_vx = 0;
  public float fluid_vy = 0;
  
  // index in balls array
  public int idx;

 
  public Ball(float x, float y, float rad, int idx) {
    this.x = x;
    this.y = y;
    this.rad = rad;
    this.mass = 1f;
    this.idx = idx;
    
    MAX_RAD = Math.max(MAX_RAD, rad);
  } 
  
  
  public void applyCollisions(Ball[] others) {
    if(COLLISION_SPRING == 0){
      return; // save a lot of processing power
    }
    
    for (int i = idx + 1; i < others.length; i++) {
      Ball othr = others[i];
      
      float dx = othr.x - this.x;
      float dy = othr.y - this.y;
      float dist_cur = dx*dx + dy*dy; // squared distance!
      float dist_min = othr.rad + this.rad;
      
      if (dist_cur < (dist_min*dist_min)) { 
        if(dist_cur == 0.0f) continue; // problem, balls have same coordinates
        dist_cur = (float) Math.sqrt(dist_cur);
        
        float overlap = (dist_min - dist_cur) * 0.5f;
        float collision_x = (dx / dist_cur) * overlap * COLLISION_SPRING;
        float collision_y = (dy / dist_cur) * overlap * COLLISION_SPRING;
        
        this.vx -= collision_x;
        this.vy -= collision_y;
        othr.vx += collision_x;
        othr.vy += collision_y;
      }
      
    }   
  }
  

  public void applyFLuid(float fluid_vx_new, float fluid_vy_new){
 
    fluid_vx_new *= FLUID_SCALE;
    fluid_vy_new *= FLUID_SCALE;
    
    // fluid affects bigger objects more
    float contact_area_factor = (rad * rad) / (MAX_RAD * MAX_RAD);
    fluid_vx_new *= contact_area_factor;
    fluid_vy_new *= contact_area_factor;
  
    // smooth
    fluid_vx = fluid_vx * FLUID_INERTIA + fluid_vx_new * (1-FLUID_INERTIA);
    fluid_vy = fluid_vy * FLUID_INERTIA + fluid_vy_new * (1-FLUID_INERTIA);
    
    // dissipate
    fluid_vx *= FLUID_DISSIPATION;
    fluid_vy *= FLUID_DISSIPATION;
   
    // add to ball-velocity
    vx += fluid_vx;
    vy += fluid_vy;
  }
  
  
  public void applyGravity(){
    // smaller objects "fall" faster in this demo
    float contact_area_factor = (rad * rad) / (MAX_RAD * MAX_RAD);
    float mass_volume = 1f / (float)Math.sqrt(contact_area_factor);
    vy += GRAVITY * mass_volume;
  }
  

  public void updatePosition(int xmin, int ymin, int xmax, int ymax) {
    // slow down a bit
    vx *= VELOCITY_DISSIPATION;
    vy *= VELOCITY_DISSIPATION;
    
    // update position
    x += vx;
    y += vy;
    
    // boundary conditions
    if ((x - rad) < xmin) { x = xmin + rad; vx *= -COLLISION_DAMPING; }
    if ((x + rad) > xmax) { x = xmax - rad; vx *= -COLLISION_DAMPING; }
    if ((y - rad) < ymin) { y = ymin + rad; vy *= -COLLISION_DAMPING; }
    if ((y + rad) > ymax) { y = ymax - rad; vy *= -COLLISION_DAMPING; } 
  }
  
  
  public void display(PGraphics pg) {
    pg.ellipse(x, y, rad*2, rad*2);
  }
  
}