/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.verletPhysics2D;

import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.CollisionGridAccelerator;

public class VerletPhysics2D {
  
  public static class Param{  
    public int iterations_springs    = 4;
    public int iterations_collisions = 4;
    public float[] GRAVITY = {0, 1};
    public float[] bounds = new float[4]; // [xmin, ymin, xmax, ymax]
  }
  
  private int collision_group_id = 0;
  
  public int getNewCollisionGroupId(){
    return collision_group_id++;
  }
  
  public Param param = new Param();

  CollisionGridAccelerator collision_grid = new CollisionGridAccelerator();
  
  public VerletPhysics2D(){
  }
  
  
  
 
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // the particles, used for simulating
  //////////////////////////////////////////////////////////////////////////////
  private int particles_count;
  private VerletParticle2D[] particles = new VerletParticle2D[0];
  
  public VerletParticle2D[] getParticles(){
    return particles;
  }
  public int getParticlesCount(){
    return particles_count;
  }
  
  public boolean CHECK_PARTICLE_INDEX_WHEN_ADDING = !false;
  
  public void addParticles(VerletParticle2D[] particles_add, int particles_add_count){
    particles = Arrays.copyOf(particles, particles_count + particles_add_count);
    System.arraycopy(particles_add, 0, particles, particles_count, particles_add_count);
    particles_count += particles_add_count;
    
    if(CHECK_PARTICLE_INDEX_WHEN_ADDING){
      for(int i = 0; i < particles_count; i++){
        if(particles[i].idx != i){
          System.out.println("Particle.idx not matching array index: "+particles[i].idx+" != "+i);
        }
      }
    }
  }
  
  public void update(float timestep){
    update(particles, particles_count, timestep);
  }
  

  public void update(VerletParticle2D[] particles, int particles_count, float timestep){
    
    float xmin = param.bounds[0];
    float ymin = param.bounds[1];
    float xmax = param.bounds[2];
    float ymax = param.bounds[3];

    // iterative spring refinement
    for(int k = 0; k < param.iterations_springs; k++){
      for(int i = 0; i < particles_count; i++) particles[i].beforeSprings();
      for(int i = 0; i < particles_count; i++) particles[i].updateSprings(particles);
      for(int i = 0; i < particles_count; i++) particles[i].afterSprings(xmin, ymin, xmax, ymax);
    }
    
    // iterative collision refinement
    for(int k = 0; k < param.iterations_collisions; k++){  
      for(int i = 0; i < particles_count; i++) particles[i].beforeCollision();
      collision_grid.updateCollisions(particles, particles_count);
      for(int i = 0; i < particles_count; i++) particles[i].afterCollision(xmin, ymin, xmax, ymax);
    }

    // verlet integration
    for(int i = 0; i < particles_count; i++){
      particles[i].addGravity(param.GRAVITY[0], param.GRAVITY[1]);
      particles[i].updatePosition(xmin, ymin, xmax, ymax, timestep);
      particles[i].updateShape();
    }

  }
  
  
  
  
  
  
}
