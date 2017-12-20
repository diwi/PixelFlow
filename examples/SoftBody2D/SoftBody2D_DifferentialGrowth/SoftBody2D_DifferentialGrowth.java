/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody2D.SoftBody2D_DifferentialGrowth;




import java.util.ArrayList;
import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import processing.core.*;

public class SoftBody2D_DifferentialGrowth extends PApplet {

  
  //
  // A simple Verlet Physics Example, that shows how to create a chain by 
  // creating particles and chaining them together with spring-constraints.
  // The springs-color shows the current stress, ... at a range from red to black.
  // 
  // + Collision Detection
  //
  // Controls:
  // LMB: drag particles
  // MMB: drag + fix particles to a location
  // RMB: disable springs, to split the chain
  //
  // key ' ': add particles at the current mouse location
  // key 'p': toggle particle display
  //
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  
  // parameters
  // particle behavior, different presets for different bodies
  DwPhysics.Param          param_physics      = new DwPhysics.Param();
  DwParticle.Param         param_chain        = new DwParticle.Param();
  DwSpringConstraint.Param param_spring_chain = new DwSpringConstraint.Param();
  
  // physics simulation
  DwPhysics<DwParticle2D> physics;
 
  // all we need is an array of particles
  int particles_count = 0;
  DwParticle2D[] particles = new DwParticle2D[particles_count];

  
  boolean DISPLAY_PARTICLES = true;

  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  

  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    DwPixelFlow context = new DwPixelFlow(this);
    context.print();
//    context.printGL();
    
    param_physics.GRAVITY = new float[]{ 0, 0.0f };
    param_physics.bounds  = new float[]{ 0, 0, width, height };
    param_physics.iterations_collisions = 8;
    param_physics.iterations_springs    = 8;
    
    // parameters for chain-particles
    param_chain.DAMP_BOUNDS          = 0.50f;
    param_chain.DAMP_COLLISION       = 0.19999f;
    param_chain.DAMP_VELOCITY        = 0.799999f; 

    param_spring_chain.damp_dec = 0.1999999f;
    param_spring_chain.damp_inc = 0.1999999f;
    
    // physics simulation object
    physics = new DwPhysics<DwParticle2D>(param_physics);
    
    // create 200 particles at start
//    for(int i = 0; i < 2; i++){
//      float spawn_x = width/2 + random(-200, 200);
//      float spawn_y = height/2 + random(-200, 200);
//      createParticle(spawn_x, spawn_y);
//    }
    float x, y;
    
    x = width/2 - radius/2;
    y = height/2;
    createParticle(x, y);
    
    x = width/2 + radius/2;
    y = height/2;
    createParticle(x, y);

    frameRate(60);
  }
  
  float radius = 5; 
  
  
  // creates a new particle, and links it with the previous one
  public void createParticle(float spawn_x, float spawn_y){
    // just in case, to avoid position conflicts
    spawn_x += random(-0.01f, +0.01f);
    spawn_y += random(-0.01f, +0.01f);
    
    int   idx_curr = particles_count;
    int   idx_prev = idx_curr - 1;
    float radius_collision_scale = 1.1f;

    float rest_len = radius * 2 * radius_collision_scale;
    
    DwParticle2D pa = new DwParticle2D(idx_curr);
    pa.setMass(1);
    pa.setParamByRef(param_chain);
    pa.setPosition(spawn_x, spawn_y);
    pa.setRadius(radius);
    pa.setRadiusCollision(radius * radius_collision_scale);
    pa.setCollisionGroup(idx_curr); // every particle has a different collision-ID
    addParticleToList(pa);

    if(idx_prev >= 0){
      DwParticle2D pb = particles[idx_prev];
      pa.px = pb.cx;
      pa.py = pb.cy;
      DwSpringConstraint2D.addSpring(physics, pb, pa, rest_len, param_spring_chain);
    }
  }
  
  
  public void createParticle(float spawn_x, float spawn_y, DwParticle2D p_prev, DwParticle2D p_next){
    spawn_x += random(-0.01f, +0.01f);
    spawn_y += random(-0.01f, +0.01f);
    
    int   idx_curr = particles_count;
    float radius_collision_scale = 1.1f;

    DwParticle2D pa = new DwParticle2D(idx_curr);
    pa.setMass(1);
    pa.setParamByRef(param_chain);
    pa.setPosition(spawn_x, spawn_y);
    pa.setRadius(radius);
    pa.setRadiusCollision(radius * radius_collision_scale);
    pa.setCollisionGroup(idx_curr); // every particle has a different collision-ID
    addParticleToList(pa);

    if(p_prev != null){
      DwSpringConstraint2D.addSpring(physics, pa, p_prev, radius, param_spring_chain);
      DwSpringConstraint2D.addSpring(physics, pa, p_next, radius, param_spring_chain);
    }
  }
  
  
  
  
  // kind of the same what an ArrayList<VerletParticle2D> would do.
  public void addParticleToList(DwParticle2D particle){
    if(particles_count >= particles.length){
      int new_len = (int) Math.max(2, Math.ceil(particles_count*1.5f) );
      if(particles == null){
        particles = new DwParticle2D[new_len];
      } else {
        particles = Arrays.copyOf(particles, new_len);
      }
    }
    particles[particles_count++] = particle;
    physics.setParticles(particles, particles_count);
  }
  
  
  public void draw() {

    if(keyPressed && key == ' '){
      createParticle(mouseX, mouseY);
    }
    

    
    ArrayList<DwSpringConstraint> springs = physics.getSprings();

    for(int i = springs.size()-1; i >= 0; i--){
      DwSpringConstraint2D spring = (DwSpringConstraint2D) springs.get(i);
      DwParticle2D pa = spring.pa;
      DwParticle2D pb = spring.pb;
      spring.dd_rest += random(0.2f);
      
      float dd_rest_max = sqrt(spring.dd_rest_sq) * 2;
      
      int count = pa.spring_count + pb.spring_count;


      if(spring.enabled && spring.dd_rest > dd_rest_max){
        spring.enable(false);
        float px = (pa.cx + pb.cx) * 0.5f;
        float py = (pa.cy + pb.cy) * 0.5f;
        
        createParticle(px, py, pa, pb);
      }
//      spring.dd_rest_sq = spring.dd_rest * spring.dd_rest;
//      spring.pa.springs
//      s2d.dd_rest
//      spring.idxPa();
    }
    
    updateMouseInteractions();    
    
    // update physics simulation
    physics.update(1);
      
    // render
    background(255);
    
    noFill();
    strokeWeight(1);
    beginShape(LINES);
    for(int i = 0; i < particles_count; i++){
      DwParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        DwSpringConstraint2D spring = (DwSpringConstraint2D) pa.springs[j];
        if(spring.pa != pa) continue;
        if(!spring.enabled) continue;
        
        DwParticle2D pb = spring.pb;
        float force = Math.abs(spring.force);
        float r = force*5000f;
        float g = r/10;
        float b = 0;
        stroke(r,g,b);
        vertex(pa.cx, pa.cy);
        vertex(pb.cx, pb.cy);
        
      }
    }
    endShape();
    
    if(DISPLAY_PARTICLES){
      noStroke();
      fill(0);
      for(int i = 0; i < particles_count; i++){
        DwParticle2D particle = particles[i];
        ellipse(particle.cx, particle.cy, particle.rad*2, particle.rad*2);
      }
    }
    
    // interaction stuff
    if(DELETE_SPRINGS){
      fill(255,64);
      stroke(0);
      strokeWeight(1);
      ellipse(mouseX, mouseY, DELETE_RADIUS*2, DELETE_RADIUS*2);
    }

    // info
    int NUM_SPRINGS   = physics.getSpringCount();
    int NUM_PARTICLES = physics.getParticlesCount();
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 
  DwParticle2D particle_mouse = null;
  
  public DwParticle2D findNearestParticle(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    DwParticle2D particle = null;
    for(int i = 0; i < particles_count; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq =  dx*dx + dy*dy;
      if( dd_sq < dd_min_sq){
        dd_min_sq = dd_sq;
        particle = particles[i];
      }
    }
    return particle;
  }
  
  public ArrayList<DwParticle> findParticlesWithinRadius(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    ArrayList<DwParticle> list = new ArrayList<DwParticle>();
    for(int i = 0; i < particles_count; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq =  dx*dx + dy*dy;
      if(dd_sq < dd_min_sq){
        list.add(particles[i]);
      }
    }
    return list;
  }
  
  
  public void updateMouseInteractions(){
    // deleting springs/constraints between particles
    if(DELETE_SPRINGS){
      ArrayList<DwParticle> list = findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
      for(DwParticle tmp : list){
        tmp.enableAllSprings(false);
        tmp.collision_group = physics.getNewCollisionGroupId();
        tmp.rad_collision = tmp.rad;
      }
    } else {
      if(particle_mouse != null){
        float[] mouse = {mouseX, mouseY};
        particle_mouse.moveTo(mouse, 0.2f);
      }
    }
  }
  
  
  boolean DELETE_SPRINGS = false;
  float   DELETE_RADIUS  = 10;

  public void mousePressed(){
    if(mouseButton == RIGHT ) DELETE_SPRINGS = true;
    
    if(!DELETE_SPRINGS){
      particle_mouse = findNearestParticle(mouseX, mouseY, 100);
      if(particle_mouse != null) particle_mouse.enable(false, false, false);
    }
  }
  
  public void mouseReleased(){
    if(particle_mouse != null && !DELETE_SPRINGS){
      if(mouseButton == LEFT  ) particle_mouse.enable(true, true,  true );
      if(mouseButton == CENTER) particle_mouse.enable(true, false, false);
      particle_mouse = null;
    }
    if(mouseButton == RIGHT ) DELETE_SPRINGS = false;
  }
  
  public void keyReleased(){
    if(key == 'p') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
  }

  public static void main(String args[]) {
    PApplet.main(new String[] { SoftBody2D_DifferentialGrowth.class.getName() });
  }
}