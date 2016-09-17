/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package OpticalFlow_CaptureParticles_Verlet;

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
  
  // verlet integration
  public float x  = 0, y  = 0; // position
  public float px = 0, py = 0; // previous position
  public float ax = 0, ay = 0; // acceleration
  public float rad= 0; ;       // radius
  
  public boolean pinned = false;
  
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
    this.px = x;
    this.py = y;
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
  
  private Particle tmp = null;

  @Override
  public void beginCollision(){
    tmp = null;
  }
  
  public void updateCollision(Particle othr) {
    if(this == othr    ) return; // not colliding with myself
    if(this == othr.tmp) return; // already collided with "othr"
    
    othr.tmp = this; // mark as checked
    
    float dx        = othr.x - this.x;
    float dy        = othr.y - this.y;
    float dd_cur_sq = dx*dx + dy*dy; // squared distance!
    float dd_min    = othr.rad + this.rad;
    float dd_min_sq = dd_min*dd_min;
    
    if (dd_cur_sq < dd_min_sq) { 
      
//      if(dd_cur_sq < 0.00001f) return; // problem, particles have same coordinates
//      float dd_cur = (float) Math.sqrt(dd_cur_sq);
//      float overlap_scale = othr.rad / dd_min;
//      float overlap = (dd_min - dd_cur) * overlap_scale;
//      float collision_x = overlap * (dx / dd_cur) * system.SPRINGINESS;
//      float collision_y = overlap * (dy / dd_cur) * system.SPRINGINESS;

      // http://www.gotoandplay.it/_articles/2005/08/advCharPhysics.php
      float delta = dd_min_sq / (dd_cur_sq + dd_min_sq) - 0.5f;
      float collision_x = dx * delta *  system.SPRINGINESS;
      float collision_y = dy * delta *  system.SPRINGINESS;
      
      this.x -= collision_x;
      this.y -= collision_y;
      othr.x += collision_x;
      othr.y += collision_y;
    }
  }


  public void applyFLuid(float fluid_vx, float fluid_vy){ 
    // contact_area_factor: smaller objects move slower
    ax += fluid_vx * system.MULT_FLUID * contact_area_factor;
    ay += fluid_vy * system.MULT_FLUID * contact_area_factor;
    
//    x += fluid_vx * system.MULT_FLUID * contact_area_factor;
//    y += fluid_vy * system.MULT_FLUID * contact_area_factor;
  }
  
  
  public void applyGravity(){
    if(system.MULT_GRAVITY == 0.0) return;
    // contact_area_factor: smaller objects "fall" faster
    ay += 0.05f * system.MULT_GRAVITY * 1f/contact_area_factor;
  }
  

  public void updatePosition(float time_step) {
    
    // slow down
    float vx = (x - px) * system.MULT_VELOCITY;
    float vy = (y - py) * system.MULT_VELOCITY;
    
    px = x;
    py = y;
    
    // new position, verlet integration
    x += vx + ax * 0.5f * time_step * time_step;
    y += vy + ay * 0.5f * time_step * time_step;
    
    ax = 0;
    ay = 0;
  }
  
  public void fixboundaries(int xmin, int ymin, int xmax, int ymax){    
    if ((x - rad) < xmin) { float vx = x - px; x = xmin + rad; px = x + vx * system.SPRINGINESS; }
    if ((x + rad) > xmax) { float vx = x - px; x = xmax - rad; px = x + vx * system.SPRINGINESS; }
    if ((y - rad) < ymin) { float vy = y - py; y = ymin + rad; py = y + vy * system.SPRINGINESS; }
    if ((y + rad) > ymax) { float vy = y - py; y = ymax - rad; py = y + vy * system.SPRINGINESS; }
  }
  
  
  public void updateShape(){
    updateShapePosition();
    updateShapeColor();
  }
  
  public void updateShapePosition(){
    float vx = x - px;
    float vy = y - py;
    
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
  
  public void updateShapeColor(){
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
    float vx = x-px;
    float vy = y-py;
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