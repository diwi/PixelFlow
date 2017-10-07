/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody2D.SoftBody2D_GetStarted;


import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import processing.core.*;

public class SoftBody2D_GetStarted extends PApplet {

  
  //
  // Getting started with verlet particles/softbody simulation.
  // 
  // + Collision Detection
  //
  
  
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  
  
  // physics parameters
  DwPhysics.Param param_physics = new DwPhysics.Param();
  
  // physics simulation
  DwPhysics<DwParticle2D> physics;
 
  DwParticle2D[] particles = new DwParticle2D[15];

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
    
    // physics object
    physics = new DwPhysics<DwParticle2D>(param_physics);

    // global physics parameters
    param_physics.GRAVITY = new float[]{ 0, 0.5f };
    param_physics.bounds  = new float[]{ 0, 0, width, height };
    param_physics.iterations_collisions = 4;
    param_physics.iterations_springs    = 4;
    
    // particle parameters
    DwParticle2D.Param param_particle = new DwParticle2D.Param();
    param_particle.DAMP_BOUNDS          = 0.50f;
    param_particle.DAMP_COLLISION       = 0.9990f;
    param_particle.DAMP_VELOCITY        = 0.9999991f; 

    // spring parameters
    DwSpringConstraint.Param param_spring = new DwSpringConstraint.Param();
    param_spring.damp_dec = 0.899999f;
    param_spring.damp_inc = 0.000099999f;

    // create particles + chain them together
    for(int i = 0; i < particles.length; i++){
      float radius = 10;
      float px = width/2;
      float py = 100 + i * radius * 3;
      particles[i] = new DwParticle2D(i, px, py, radius, param_particle);
      
      if(i > 0) DwSpringConstraint2D.addSpring(physics, particles[i-1], particles[i], param_spring);
    }
    
    // add all particles to the physics simulation
    physics.setParticles(particles, particles.length);

    frameRate(60);
  }
  
  

  
  public void draw() {

    updateMouseInteractions();    
    
    // update physics simulation
    physics.update(1);
    
    // render
    background(255);
    
    // render springs: access the springs and use the current force for the line-color
    noFill();
    strokeWeight(1);
    beginShape(LINES);
    ArrayList<DwSpringConstraint> springs = physics.getSprings();
    for(DwSpringConstraint spring : springs){
      if(spring.enabled){
        DwParticle2D pa = particles[spring.idxPa()];
        DwParticle2D pb = particles[spring.idxPb()];
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
    

    // render particles
    noStroke();
    fill(0);
    for(int i = 0; i < particles.length; i++){
      DwParticle2D particle = particles[i];
      ellipse(particle.cx, particle.cy, particle.rad*2, particle.rad*2);
    }
    
    // stats, to the title window
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [frame %d]   [fps %6.2f]", particles.length,frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 
  DwParticle2D particle_mouse = null;
  
  public DwParticle2D findNearestParticle(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    DwParticle2D particle = null;
    for(int i = 0; i < particles.length; i++){
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


  public void updateMouseInteractions(){
    if(particle_mouse != null){
      float[] mouse = {mouseX, mouseY};
      particle_mouse.moveTo(mouse, 0.2f);
    }
  }
  
  public void mousePressed(){
    particle_mouse = findNearestParticle(mouseX, mouseY, 100);
    if(particle_mouse != null){
      particle_mouse.enable(false, false, false);
    }
  }
  
  public void mouseReleased(){
    if(particle_mouse != null){
      if(mouseButton == LEFT  ) particle_mouse.enable(true, true,  true );
      if(mouseButton == CENTER) particle_mouse.enable(true, false, false);
      particle_mouse = null;
    }
  }
  

  public static void main(String args[]) {
    PApplet.main(new String[] { SoftBody2D_GetStarted.class.getName() });
  }
}