package com.thomasdiewald.pixelflow.java.verletphysics.softbodies3D;

import java.util.Random;

import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint3D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle3D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics3D;


public class SoftCube extends SoftBody3D{
  
  // specific attributes for this body
  public int   nodes_x;
  public int   nodes_y;
  public int   nodes_z;
  public float nodes_r;
  
  public int bend_spring_mode = 0;
  public int bend_spring_dist = 3; // try other values, it affects the objects stiffness
  
  Random rand;
  
  public SoftCube(){
  }

  public void create(VerletPhysics3D physics, int nx, int ny, int nz, float nr, float start_x, float start_y, float start_z){
 
    this.rand               = new Random(0);
    this.collision_group_id = physics.getNewCollisionGroupId();
    this.nodes_offset       = physics.getParticlesCount();
    this.nodes_x            = nx;
    this.nodes_y            = ny;
    this.nodes_z            = nz;
    this.nodes_r            = nr;
    this.num_nodes          = nodes_x * nodes_y * nodes_z;
    this.particles          = new VerletParticle3D[num_nodes];
    
    VerletParticle3D.MAX_RAD = Math.max(VerletParticle3D.MAX_RAD, nr);

    // temp variables
    int idx, idx_world;
    int x, y, z, ox, oy, oz;
    float px, py, pz;
    float rand_scale = 1f;
  
    // 1) init particles
    for(z = 0; z < nodes_z; z++){
      for(y = 0; y < nodes_y; y++){
        for(x = 0; x < nodes_x; x++){
          idx            = (z * nodes_x * nodes_y) + (y * nodes_x) + x;
          idx_world      = idx + nodes_offset;
          px             = start_x + x * nodes_r * 2 + (rand.nextFloat()*2-1) * rand_scale;
          py             = start_y + y * nodes_r * 2 + (rand.nextFloat()*2-1) * rand_scale;
          pz             = start_z + z * nodes_r * 2 + (rand.nextFloat()*2-1) * rand_scale;
          particles[idx] = new CustomVerletParticle3D(idx_world, px, py, pz, nodes_r);
          particles[idx].setParamByRef(param_particle);
          particles[idx].setRadiusCollision(nodes_r * collision_radius_scale);
          particles[idx].collision_group = collision_group_id;
          if(self_collisions){
            particles[idx].collision_group = physics.getNewCollisionGroupId();
          }
        }
      }
    }
    
    
    ox = bend_spring_dist;
    oy = bend_spring_dist;
    oz = bend_spring_dist;
    
    // 2) create springs
    for(z = 0; z < nodes_z; z++){
      for(y = 0; y < nodes_y; y++){
        for(x = 0; x < nodes_x; x++){
          if(CREATE_STRUCT_SPRINGS){
            addSpring(x, y, z, -1, 0, 0, SpringConstraint3D.TYPE.STRUCT);
            addSpring(x, y, z, +1, 0, 0, SpringConstraint3D.TYPE.STRUCT);
            addSpring(x, y, z,  0,-1, 0, SpringConstraint3D.TYPE.STRUCT);
            addSpring(x, y, z,  0,+1, 0, SpringConstraint3D.TYPE.STRUCT);
            addSpring(x, y, z,  0, 0,-1, SpringConstraint3D.TYPE.STRUCT);
            addSpring(x, y, z,  0, 0,+1, SpringConstraint3D.TYPE.STRUCT);
          }
                    
          if(CREATE_SHEAR_SPRINGS){
            addSpring(x, y, z, -1,-1, -1, SpringConstraint3D.TYPE.SHEAR);
            addSpring(x, y, z, +1,-1, -1, SpringConstraint3D.TYPE.SHEAR);
            addSpring(x, y, z, -1,+1, -1, SpringConstraint3D.TYPE.SHEAR);
            addSpring(x, y, z, +1,+1, -1, SpringConstraint3D.TYPE.SHEAR);
            
            addSpring(x, y, z, -1,-1, +1, SpringConstraint3D.TYPE.SHEAR);
            addSpring(x, y, z, +1,-1, +1, SpringConstraint3D.TYPE.SHEAR);
            addSpring(x, y, z, -1,+1, +1, SpringConstraint3D.TYPE.SHEAR);
            addSpring(x, y, z, +1,+1, +1, SpringConstraint3D.TYPE.SHEAR);
          }
          
          if(CREATE_BEND_SPRINGS && bend_spring_dist > 0){
            // diagonal
            if(bend_spring_mode == 0){
              addSpring(x, y, z, -ox, -oy, -oz, SpringConstraint3D.TYPE.BEND);
              addSpring(x, y, z, +ox, -oy, -oz, SpringConstraint3D.TYPE.BEND);
              addSpring(x, y, z, -ox, +oy, -oz, SpringConstraint3D.TYPE.BEND);
              addSpring(x, y, z, +ox, +oy, -oz, SpringConstraint3D.TYPE.BEND);
              
              addSpring(x, y, z, -ox, -oy, +oz, SpringConstraint3D.TYPE.BEND);
              addSpring(x, y, z, +ox, -oy, +oz, SpringConstraint3D.TYPE.BEND);
              addSpring(x, y, z, -ox, +oy, +oz, SpringConstraint3D.TYPE.BEND);
              addSpring(x, y, z, +ox, +oy, +oz, SpringConstraint3D.TYPE.BEND);
            }
            
//            // orthogonal
//            if(bend_spring_mode == 1){
//              addSpring(x, y, z, -ox,   0, SpringConstraint3D.TYPE.BEND);
//              addSpring(x, y, z, +ox,   0, SpringConstraint3D.TYPE.BEND);
//              addSpring(x, y, z,   0, +oy, SpringConstraint3D.TYPE.BEND);
//              addSpring(x, y, z,   0, -oy, SpringConstraint3D.TYPE.BEND);
//            }
//            
//            // random, 'kind of' anisotropic
//            if(bend_spring_mode == 2){
//              for(int i = 0; i < 8; i++){
//                ox = (int) Math.round((rand.nextFloat()*2-1) * bend_spring_dist);
//                oy = (int) Math.round((rand.nextFloat()*2-1) * bend_spring_dist);
//
//                addSpring(x, y, z, ox, oy, SpringConstraint3D.TYPE.BEND);
//              }
//            }
          }
          
        }
      }
    }
    
    
    // add new particles to the physics-world
    physics.addParticles(particles, num_nodes);
  }
  
 
  public VerletParticle3D getNode(int x, int y, int z){
    if(x <        0 || y <        0 || z <        0) return null;
    if(x >= nodes_x || y >= nodes_y || z >= nodes_z) return null;

    int idx = (z * nodes_x * nodes_y) + (y * nodes_x) + x;
    return particles[idx];
  }
  
  
  public void addSpring(int ax, int ay, int az, int offx, int offy, int offz, SpringConstraint3D.TYPE type){
    int bx = ax + offx;
    int by = ay + offy;
    int bz = az + offz;
    
    // clamp offset to grid-bounds
    if(bx < 0) bx = 0; else if(bx > nodes_x-1) bx = nodes_x-1;
    if(by < 0) by = 0; else if(by > nodes_y-1) by = nodes_y-1;
    if(bz < 0) bz = 0; else if(bz > nodes_z-1) bz = nodes_z-1;
    
    int ia = (az * nodes_x * nodes_y) + (ay * nodes_x) + ax;
    int ib = (bz * nodes_x * nodes_y) + (by * nodes_x) + bx;

    SpringConstraint3D.addSpring(particles[ia], particles[ib], param_spring, type);
  }
  

}
  
  
  
 
  