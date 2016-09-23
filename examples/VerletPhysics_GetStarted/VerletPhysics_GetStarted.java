/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package VerletPhysics_GetStarted;


import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics2D;
import processing.core.*;

public class VerletPhysics_GetStarted extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  // physics simulation
  VerletPhysics2D physics;
 
  VerletParticle2D[] particles = new VerletParticle2D[15];

  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
//    context.printGL();
    
    // physics object
    physics = new VerletPhysics2D();

    // global physics parameters
    physics.param.GRAVITY = new float[]{ 0, 0.5f };
    physics.param.bounds  = new float[]{ 0, 0, width, height };
    physics.param.iterations_collisions = 4;
    physics.param.iterations_springs    = 4;
    
    // particle parameters
    VerletParticle2D.Param param_particle = new VerletParticle2D.Param();
    param_particle.DAMP_BOUNDS          = 0.50f;
    param_particle.DAMP_COLLISION       = 0.9990f;
    param_particle.DAMP_VELOCITY        = 0.9999991f; 

    // spring parameters
    SpringConstraint2D.Param param_spring = new SpringConstraint2D.Param();
    param_spring.damp_dec = 0.899999f;
    param_spring.damp_inc = 0.000099999f;

    // create particles + chain them together
    for(int i = 0; i < particles.length; i++){
      float radius = 10;
      float px = width/2;
      float py = 100 + i * radius * 3;
      particles[i] = new VerletParticle2D(i, px, py, radius, param_particle);
      
      if(i > 0) SpringConstraint2D.addSpring(particles[i-1], particles[i], param_spring);
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
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint2D spring = pa.springs[j];
        if(spring.is_the_good_one){
          VerletParticle2D pb = spring.pb;
          float force = Math.abs(spring.force);
          float r = force*5000f;
          float g = r/10;
          float b = 0;
          stroke(r,g,b);
          vertex(pa.cx, pa.cy);
          vertex(pb.cx, pb.cy);
        }
      }
    }
    endShape();
    
    // render particles
    noStroke();
    fill(0);
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D particle = particles[i];
      ellipse(particle.cx, particle.cy, particle.rad*2, particle.rad*2);
    }
    
    // stats, to the title window
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [frame %d]   [fps %6.2f]", particles.length,frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 
  VerletParticle2D particle_mouse = null;
  
  public VerletParticle2D findNearestParticle(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    VerletParticle2D particle = null;
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
      particle_mouse.moveTo(mouseX, mouseY, 0.2f);
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
    PApplet.main(new String[] { VerletPhysics_GetStarted.class.getName() });
  }
}