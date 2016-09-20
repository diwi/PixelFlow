/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package OpticalFlow_CaptureParticles_Euler;

import com.thomasdiewald.pixelflow.java.CollisionObject;

import processing.core.PMatrix2D;
import processing.core.PShape;

public class Particle implements CollisionObject{
  
  // max radius among all particles, used for normalization, ...
  static public float MAX_RAD; 
  
  // parent system
  public ParticleSystem system;
  
  // index (must be unique)
  public int idx;
  
  // position, radius, velocity
  public float x=0, y=0, rad=0, vx=0, vy=0;
  
  // just experimenting
  private float contact_area_factor;

  // display shape
  private PShape    shp_particle;
  private PMatrix2D shp_transform = new PMatrix2D();

  
  public Particle(ParticleSystem system, int idx) {
    this.system = system;
    this.idx = idx;
  }

  public void setposition(float x, float y){
    this.x = x;
    this.y = y;
  }
  
  public void setRadius(float rad_){
    rad = Math.max(rad_, 0.1f);
    contact_area_factor = (rad * rad) / (MAX_RAD * MAX_RAD);
  }
  
  public void setShape(PShape shape){
    shp_particle = shape;
    updateShapePosition();
  }

  public void setColor(int col_argb){
    shp_particle.setTint(col_argb);
    shp_particle.setFill(col_argb);
  }
  
  
//  public void applyCollision(Particle[] others) {
//    if(system.SPRINGINESS == 0){
//      return;
//    }
//    
//    for (int i = idx + 1; i < others.length; i++) {
//      updateCollisionPair(others[i]);
//    }   
//  }
//  
//  
//  public void updateCollisionPair(Particle othr) {
//    float dx = othr.x - this.x;
//    float dy = othr.y - this.y;
//    float dist_cur = dx*dx + dy*dy; // squared distance!
//    float dist_min = othr.rad + this.rad;
//    
//    if (dist_cur < (dist_min*dist_min)) { 
//      if(dist_cur == 0.0f) return; // problem, particles have same coordinates
//      dist_cur = (float) Math.sqrt(dist_cur);
//      
//      float overlap = (dist_min - dist_cur) * 0.5f;
//      float collision_x = (dx / dist_cur) * overlap * system.SPRINGINESS;
//      float collision_y = (dy / dist_cur) * overlap * system.SPRINGINESS;
//      
//      this.vx -= collision_x;
//      this.vy -= collision_y;
//      othr.vx += collision_x;
//      othr.vy += collision_y;
//    }
//
//  }
  
  
  private Particle tmp = null;

  @Override
  public void resetCollisionPtr(){
    tmp = null;
  }
  
  public void updateCollision(Particle othr) {
    if(this == othr    ) return; // not colliding with myself
    if(this == othr.tmp) return; // already collided with "othr"
    
    othr.tmp = this; // mark as checked
    
    float dx = othr.x - this.x;
    float dy = othr.y - this.y;
    float dist_cur = dx*dx + dy*dy; // squared distance!
    float dist_min = othr.rad + this.rad;
    
    if (dist_cur < (dist_min*dist_min)) { 
      if(dist_cur < 0.001f) return; // problem, particles have same coordinates
      dist_cur = (float) Math.sqrt(dist_cur);
      
      float overlap_scale = othr.rad / dist_min;

      float overlap = (dist_min - dist_cur) * overlap_scale;
      float collision_x = overlap * (dx / dist_cur);
      float collision_y = overlap * (dy / dist_cur);

      vx += -collision_x * system.SPRINGINESS;
      vy += -collision_y * system.SPRINGINESS;
    }
  }
  

  public void applyFLuid(float fluid_vx, float fluid_vy){ 
    // contact_area_factor: smaller objects move slower
    vx += fluid_vx * system.MULT_FLUID * contact_area_factor;
    vy += fluid_vy * system.MULT_FLUID * contact_area_factor;
  }
  
  
  public void applyGravity(){
    if(system.MULT_GRAVITY == 0.0) return;
    // contact_area_factor: smaller objects "fall" faster
    vy += 0.01 * system.MULT_GRAVITY * 1f/contact_area_factor;
  }
  

  public void updatePosition(int xmin, int ymin, int xmax, int ymax) {
    // slow down
    vx *= system.MULT_VELOCITY;
    vy *= system.MULT_VELOCITY;
  
    // update position
    x += vx;
    y += vy;
    
    // boundary conditions
    if ((x - rad) < xmin) { x = xmin + rad; vx *= -system.SPRINGINESS; }
    if ((x + rad) > xmax) { x = xmax - rad; vx *= -system.SPRINGINESS; }
    if ((y - rad) < ymin) { y = ymin + rad; vy *= -system.SPRINGINESS; }
    if ((y + rad) > ymax) { y = ymax - rad; vy *= -system.SPRINGINESS; } 
    
    // apply new position to PShape
    updateShapePosition();
  }
  
  private void updateShapePosition(){
    // build transformation matrix
    shp_transform.reset();
    shp_transform.translate(x, y);
    shp_transform.rotate((float)Math.atan2(vy,vx));

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
  
  public void updateColor(){
    float speed = getSpeed();
    float radn  = 1.1f * rad / MAX_RAD;

    getShading(speed * 0.5f, rgb);
    int a = 255;
    int r = clamp(rgb[0] * radn) & 0xFF;
    int g = clamp(rgb[1] * radn) & 0xFF;
    int b = clamp(rgb[2] * radn) & 0xFF;

    int col = a << 24 | r << 16 | g << 8 | b;
    setColor(col);
  }

  
  public float getSpeed(){
    return (float) Math.sqrt(vx*vx + vy*vy);
  }

  @Override public final float x  () { return x  ; }
  @Override public final float y  () { return y  ; }
  @Override public final float rad() { return rad; }
  
  @Override
  public void update(CollisionObject othr) {
    updateCollision((Particle)othr);
  }

}