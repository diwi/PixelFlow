/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.softbodydynamics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwCollisionGrid;
import com.thomasdiewald.pixelflow.java.accelerationstructures.DwPair;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;

public class DwPhysics<T extends DwParticle> {
  
  public static class Param{  
    
    public int iterations_springs    = 4;
    public int iterations_collisions = 4;
    // 2D {x, y}
    // 3D {x, y, z}
    public float[] GRAVITY = new float[3];
    
    // 2D [xmin, ymin, xmax, ymax] 
    // 3D [xmin, ymin, zmin, xmax, ymax, zmax] 
    public float[] bounds = new float[6];
    
  }
  
  // 0 ... no collision (or not?)
  private int collision_group_id = 1; 
  public DwPhysics.Param param;

  private DwCollisionGrid collision_grid = new DwCollisionGrid();
  private int particles_count;
  private T[] particles;
  
  
  public ArrayList<DwSpringConstraint> springs = new ArrayList<DwSpringConstraint>();
  public HashMap<DwPair<DwParticle>, DwSpringConstraint> spring_map = new HashMap<DwPair<DwParticle>, DwSpringConstraint>();
  
  public DwPhysics(DwPhysics.Param param){
    this.param = param;
    reset();
  }
  
  public T[] getParticles(){
    return particles;
  }
  public int getParticlesCount(){
    return particles_count;
  }
  
  public ArrayList<DwSpringConstraint> getSprings(){
    return springs;
  }
  public int getSpringCount(){
    return springs.size();
  }
  public int getNewCollisionGroupId(){
    return collision_group_id++;
  }
  
  

  
  public boolean CHECK_PARTICLE_INDEX_WHEN_ADDING = !false;
  
  public void addParticles(T[] particles_add, int particles_add_count){
    
    if(particles == null){
      setParticles(particles_add, particles_add_count);
    } else {
      particles = Arrays.copyOf(particles, particles_count + particles_add_count);
      System.arraycopy(particles_add, 0, particles, particles_count, particles_add_count);
      particles_count += particles_add_count;
    }
    
    if(CHECK_PARTICLE_INDEX_WHEN_ADDING){
      for(int i = 0; i < particles_count; i++){
        if(particles[i].idx != i){
          System.out.println("Particle.idx not matching array index: "+particles[i].idx+" != "+i);
        }
      }
    }
  }
  
  public void setParticles(T[] particles_set, int particles_set_count){
    this.particles = particles_set;
    this.particles_count = particles_set_count;
  }
  
  

  public void reset(){
    collision_group_id = 1; 
    particles_count = 0;
    particles = null;
    springs.clear();
    spring_map.clear();
  }
  
  
  public boolean update_particle_shapes = true;
  

  public void update(float timestep){
    
    if(particles == null){
      return;
    }
    
    // iterative spring refinement
    for(int k = 0; k < param.iterations_springs; k++){
      for(DwSpringConstraint spring : springs) spring.update();
      for(int i = 0; i < particles_count; i++){
        particles[i].updateBounds(param.bounds);
      }
    }
      
    // iterative collision refinement
    for(int k = 0; k < param.iterations_collisions; k++){  
      
      for(int i = 0; i < particles_count; i++){
        particles[i].beforeCollision();
      }
      
      collision_grid.updateCollisions(particles, particles_count);
      
      for(int i = 0; i < particles_count; i++) {
        particles[i].afterCollision();
        particles[i].updateBounds(param.bounds);
      }
    }

    // verlet integration
    for(int i = 0; i < particles_count; i++){
      particles[i].addGravity(param.GRAVITY);
      particles[i].updatePosition(timestep);
      particles[i].updateBounds(param.bounds);
      
      if(update_particle_shapes){
        particles[i].updateShape();
      }
    }

  }
  
  
  
  
  
  
  
  
}
