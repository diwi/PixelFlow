/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.particlephysics;

import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwCollisionGrid;

public class DwPhysics3D {
  
  public static class Param{  
    public int iterations_springs    = 4;
    public int iterations_collisions = 4;
    public float[] GRAVITY = {0, 0, 1};
    public float[] bounds = new float[6]; // [xmin, ymin, zmin, xmax, ymax, zmax]
  }
  
  // 0 ... no collision (or not?)
  private int collision_group_id = 1; 
  public Param param = new Param();

  private DwCollisionGrid collision_grid = new DwCollisionGrid();
  private int particles_count;
  private DwParticle3D[] particles;
  
  
  public DwPhysics3D(){
    reset();
  }
  
  public DwParticle3D[] getParticles(){
    return particles;
  }
  public int getParticlesCount(){
    return particles_count;
  }
   
  public boolean CHECK_PARTICLE_INDEX_WHEN_ADDING = !false;
  
  public void addParticles(DwParticle3D[] particles_add, int particles_add_count){
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
  
  public void setParticles(DwParticle3D[] particles_set, int particles_set_count){
    this.particles = particles_set;
    this.particles_count = particles_set_count;
  }
  
  
  public void reset(){
    collision_group_id = 1; 
    particles_count = 0;
    particles = new DwParticle3D[particles_count];
  }
  
  public void update(float timestep){
    update(particles, particles_count, timestep);
  }
  
  public int getNewCollisionGroupId(){
    return collision_group_id++;
  }
  

  private void update(DwParticle3D[] particles, int particles_count, float timestep){
    
    float xmin = param.bounds[0];
    float ymin = param.bounds[1];
    float zmin = param.bounds[2];
    float xmax = param.bounds[3];
    float ymax = param.bounds[4];
    float zmax = param.bounds[5];

    // iterative spring refinement
    for(int k = 0; k < param.iterations_springs; k++){
      for(int i = 0; i < particles_count; i++) particles[i].beforeSprings();
      for(int i = 0; i < particles_count; i++) particles[i].updateSprings(particles);
      for(int i = 0; i < particles_count; i++) particles[i].afterSprings(xmin, ymin, zmin, xmax, ymax, zmax);
    }
    
    // iterative collision refinement
    for(int k = 0; k < param.iterations_collisions; k++){  
      for(int i = 0; i < particles_count; i++) particles[i].beforeCollision();
      collision_grid.updateCollisions(particles, particles_count);
      for(int i = 0; i < particles_count; i++) particles[i].afterCollision(xmin, ymin, zmin, xmax, ymax, zmax);
    }

    // verlet integration
    for(int i = 0; i < particles_count; i++){
      particles[i].addGravity(param.GRAVITY[0], param.GRAVITY[1], param.GRAVITY[2]);
      particles[i].updatePosition(xmin, ymin, zmin, xmax, ymax, zmax, timestep);
      particles[i].updateShape();
    }

  }
  
  
  
  
  
  
}
