/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package ParticleCollisionSystem;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;

public class Particle {
  

  static public float MAX_RAD;
  
  // STATIC GLOBAL controlling variables
  
  // gravity
  static public float GRAVITY = 0.005f;
  
  // collisions
  static public float COLLISION_SPRING  = 0.30f; // collision-damping for: particle <-> particle
  static public float COLLISION_DAMPING = 0.50f; // collision-damping for: particle <-> solid
  
  static public float VELOCITY_DISSIPATION = 0.95f; // gobal viscosity
  
  // fluid
  static public float FLUID_INERTIA     = 0.10f;
  static public float FLUID_DISSIPATION = 0.30f;
  static public float FLUID_SCALE       = 0.4f;
  

  
  //position, radius
  public float x, y, rad, mass;
  public int color;

  // final velocity
  public float vx = 0; 
  public float vy = 0;


  // fluid velocity
  public float fluid_vx = 0;
  public float fluid_vy = 0;
  
  // index in particles array
  public int idx;

  PShape shp_particle;

  
  public Particle(int idx) {
    this.idx = idx;
  }

  public void setposition(float x, float y){
    this.x = x;
    this.y = y;
  }
  
  public void setRadius(float rad){
    this.rad = Math.max(rad, 0.1f);
    this.mass = 1f;
    MAX_RAD = Math.max(MAX_RAD, rad);
  }
  
  
  public void initShape(PApplet papplet, PImage sprite_img){
    shp_particle = papplet.createShape();
    shp_particle.beginShape(PConstants.QUAD);
    shp_particle.noStroke();
    shp_particle.texture(sprite_img);
    shp_particle.textureMode(PConstants.NORMAL);
    shp_particle.normal(0, 0, 1);
    shp_particle.vertex(-rad, -rad, 0, 0);
    shp_particle.vertex(+rad, -rad, 1, 0);
    shp_particle.vertex(+rad, +rad, 1, 1);
    shp_particle.vertex(-rad, +rad, 0, 1);
    shp_particle.endShape();    
  }
  
  
  public void applyCollision(Particle[] others) {
    if(COLLISION_SPRING == 0){
      return; // save a lot of processing power
    }
    
    for (int i = idx + 1; i < others.length; i++) {
      updateCollisionPair(others[i]);
    }   
  }
  
  
  public void updateCollisionPair(Particle othr) {
    float dx = othr.x - this.x;
    float dy = othr.y - this.y;
    float dist_cur = dx*dx + dy*dy; // squared distance!
    float dist_min = othr.rad + this.rad;
    
    if (dist_cur < (dist_min*dist_min)) { 
      if(dist_cur == 0.0f) return; // problem, particles have same coordinates
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
  
  public int collision_count = 0;
  public Particle tmp = null;
  
  public void beforeCollisionDetection(){
    tmp = null;
    collision_count = 0;
  }
  
  public void updateCollision(Particle othr) {

    if(this == othr.tmp) return; // already collided with "othr"
    
    othr.tmp = this;
    
    float dx = othr.x - this.x;
    float dy = othr.y - this.y;
    float dist_cur = dx*dx + dy*dy; // squared distance!
    float dist_min = othr.rad + this.rad;
    
    if (dist_cur < (dist_min*dist_min)) { 
      if(dist_cur < 0.0001f) return; // problem, particles have same coordinates
      dist_cur = (float) Math.sqrt(dist_cur);
      
      float overlap = (dist_min - dist_cur) * 0.5f;
      float collision_x = (dx / dist_cur) * overlap * COLLISION_SPRING;
      float collision_y = (dy / dist_cur) * overlap * COLLISION_SPRING;
      
      this.vx -= collision_x;
      this.vy -= collision_y;
       
      collision_count++;
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
   
    // add to particle-velocity
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

    // update shape position
    shp_particle.resetMatrix();
//    shp_particle.rotate((float)Math.atan2(vy,vx));
    shp_particle.translate(x, y);
  }

  public float getSpeed(){
    return (float)Math.sqrt(vx*vx + vy*vy);
  }
  

  public void updateColor(int shading){
    float speed = getSpeed() * 100;
    
    float radn = rad / MAX_RAD;
    shading *= radn;
    
//    float collision = collision_count*20;
//    speed += collision;
    
    int r,g,b,a;

    if(idx == 0){
      r = clamp(shading + speed*10) & 0xFF;
      g = clamp(speed*0.5f        ) & 0xFF;
      b = clamp(0                 ) & 0xFF;
      a = 255;
    } else {
      r = clamp(shading + speed   ) & 0xFF;
      g = clamp(shading-speed     ) & 0xFF;
      b = clamp(shading-speed*0.6f) & 0xFF;
      a = 255;
    }
    
//    speed = 255f * rad / MAX_RAD;
//    r = clamp(speed) & 0xFF;
//    g = clamp(speed) & 0xFF;
//    b = clamp(speed) & 0xFF;
//    a = 255;
 
     
    color = a << 24 | r << 16 | g << 8 | b;
    shp_particle.setTint(color);
  }
  
  
  private int clamp(float v){
    if( v <   0 ) return 0;
    if( v > 255 ) return 255;
    return (int)v;
  }
  
  
  public void display(PGraphics pg){
//    pg.shape(particle);
  }

}