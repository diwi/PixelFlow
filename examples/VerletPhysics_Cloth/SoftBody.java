package VerletPhysics_Cloth;

import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.SpringConstraint;

public class SoftBody{
  int body_idx;

  int nodes_x;
  int nodes_y;
  float nodes_radius;
  
  int num_nodes;
  int nodes_offset;
  
  public int dist_bend = 2;
  
  VerletParticle2D[] particles;
  
  public SoftBody(int body_idx){
    this.body_idx = body_idx;
  }
  
  public VerletParticle2D[] create(VerletParticle2D[] particles_world, VerletParticle2D.Param param, int nodes_x, int nodes_y, float nodes_radius, float start_x, float start_y){
 
    this.nodes_x = nodes_x;
    this.nodes_y = nodes_y;
    this.num_nodes = nodes_x * nodes_y;
    this.nodes_radius = nodes_radius;
    this.nodes_offset = particles_world.length;
    
    particles = new VerletParticle2D[num_nodes];
      
    int x, y, idx, idx_world;
    float px, py;
    
    for(y = 0; y < nodes_y; y++){
      for(x = 0; x < nodes_x; x++){
        idx = y * nodes_x + x;
        idx_world = idx + nodes_offset;
        px = start_x + x * nodes_radius*2;
        py = start_y + y * nodes_radius*2;
        particles[idx] = new VerletParticle2D(idx_world, px, py, nodes_radius);
        particles[idx].collision_group = body_idx;
        particles[idx].setParamByRef(param);
      }
    }
    

    
  
    for(y = 0; y < nodes_y; y++){
      for(x = 0; x < nodes_x; x++){
        addSpring(x, y, -1, 0, SpringConstraint.TYPE.STRUCT);
        addSpring(x, y,  0,-1, SpringConstraint.TYPE.STRUCT);
        addSpring(x, y, +1, 0, SpringConstraint.TYPE.STRUCT);
        addSpring(x, y,  0,+1, SpringConstraint.TYPE.STRUCT);
                  
        addSpring(x, y, -1,-1, SpringConstraint.TYPE.SHEAR);
        addSpring(x, y, +1,-1, SpringConstraint.TYPE.SHEAR);
        addSpring(x, y, -1,+1, SpringConstraint.TYPE.SHEAR);
        addSpring(x, y, +1,+1, SpringConstraint.TYPE.SHEAR);
        
        dist_bend = 4;
        addSpring(x, y, -dist_bend,-dist_bend, SpringConstraint.TYPE.BEND);
        addSpring(x, y, +dist_bend,-dist_bend, SpringConstraint.TYPE.BEND);
        addSpring(x, y, -dist_bend,+dist_bend, SpringConstraint.TYPE.BEND);
        addSpring(x, y, +dist_bend,+dist_bend, SpringConstraint.TYPE.BEND);

//        addSpring(x, y, -dist_bend,         0, SpringConstraint.TYPE.BEND);
//        addSpring(x, y, +dist_bend,         0, SpringConstraint.TYPE.BEND);
//        addSpring(x, y,          0,+dist_bend, SpringConstraint.TYPE.BEND);
//        addSpring(x, y,          0,-dist_bend, SpringConstraint.TYPE.BEND);
      }
    }
    
    
    // add new particles
    particles_world = Arrays.copyOf(particles_world, nodes_offset + num_nodes);
    System.arraycopy(particles, 0, particles_world, nodes_offset, num_nodes);
    return particles_world;
  }
  
 

  public VerletParticle2D getNode(int x, int y){
    if(x < nodes_x && y < nodes_y){
      int idx = y *nodes_x + x;
      return particles[idx];
    }
    return null;
  }
  
  
  public void addSpring(int x, int y, int offx, int offy, SpringConstraint.TYPE type){
    int nx = x + offx;
    int ny = y + offy;
    
    if(nx < 0) nx = 0; else if(nx > nodes_x-1) nx = nodes_x-1;
    if(ny < 0) ny = 0; else if(ny > nodes_y-1) ny = nodes_y-1;

    int ia =  y * nodes_x +  x;
    int ib = ny * nodes_x + nx;

    SpringConstraint.addSpring(particles, ia, ib, type);
  }

}
  
  
  
 
  