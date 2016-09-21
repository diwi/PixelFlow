package VerletPhysics_SoftBodyCollision;

import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.SpringConstraint;

public class SoftBody{
  int body_idx;
  float nodes_radius;
  int nodes_x;
  int nodes_y;
  int num_nodes;
  int nodes_offset;
  
  public int dist_bend = 4;
  
   
  public SoftBody(int body_idx){
    this.body_idx = body_idx;
  }
  
  public VerletParticle2D[] create(VerletParticle2D[] particles, VerletParticle2D.Param param, int nodes_x, int nodes_y, float nodes_radius, float start_x, float start_y){
 
    this.nodes_x = nodes_x;
    this.nodes_y = nodes_y;
    this.num_nodes = nodes_x * nodes_y;
    this.nodes_radius = nodes_radius;
    
    nodes_offset = particles.length;
    particles = Arrays.copyOf(particles, nodes_offset + num_nodes);
    
    int x, y, idx;
    
    for(y = 0; y < nodes_y; y++){
      for(x = 0; x < nodes_x; x++){
        idx = nodes_offset + y * nodes_x + x;
        float px = start_x + x * nodes_radius*2;
        float py = start_y + y * nodes_radius*2;
        particles[idx] = new VerletParticle2D(idx, px, py, nodes_radius);
        particles[idx].collision_group = body_idx;
        particles[idx].setParamByRef(param);
      }
    }
    
  
    for(y = 0; y < nodes_y; y++){
      for(x = 0; x < nodes_x; x++){
        addSpring(particles, x, y, -1, 0, SpringConstraint.TYPE.STRUCT);
        addSpring(particles, x, y,  0,-1, SpringConstraint.TYPE.STRUCT);
        addSpring(particles, x, y, +1, 0, SpringConstraint.TYPE.STRUCT);
        addSpring(particles, x, y,  0,+1, SpringConstraint.TYPE.STRUCT);
                      
        addSpring(particles, x, y, -1,-1, SpringConstraint.TYPE.SHEAR);
        addSpring(particles, x, y, +1,-1, SpringConstraint.TYPE.SHEAR);
        addSpring(particles, x, y, -1,+1, SpringConstraint.TYPE.SHEAR);
        addSpring(particles, x, y, +1,+1, SpringConstraint.TYPE.SHEAR);
        
        dist_bend = 4;
        addSpring(particles, x, y, -dist_bend,-dist_bend, SpringConstraint.TYPE.BEND);
        addSpring(particles, x, y, +dist_bend,-dist_bend, SpringConstraint.TYPE.BEND);
        addSpring(particles, x, y, -dist_bend,+dist_bend, SpringConstraint.TYPE.BEND);
        addSpring(particles, x, y, +dist_bend,+dist_bend, SpringConstraint.TYPE.BEND);
        
//        dist_bend = 8;
//        addSpring(particles, x, y, -dist_bend,-dist_bend, SpringConstraint.TYPE.BEND);
//        addSpring(particles, x, y, +dist_bend,-dist_bend, SpringConstraint.TYPE.BEND);
//        addSpring(particles, x, y, -dist_bend,+dist_bend, SpringConstraint.TYPE.BEND);
//        addSpring(particles, x, y, +dist_bend,+dist_bend, SpringConstraint.TYPE.BEND);
      }
    }
    
    return particles;
  }
  
  public VerletParticle2D getNode(VerletParticle2D[] particles, int x, int y){
    if(x < nodes_x && y < nodes_y){
      int idx = nodes_offset + y *nodes_x + x;
      return particles[idx];
    }
    return null;
  }
  
  
  
  public void addSpring(VerletParticle2D[] particles, int x, int y, int offx, int offy, SpringConstraint.TYPE type){
    int nx = x + offx;
    int ny = y + offy;
    if(nx < 0) nx = 0; else if(nx > nodes_x-1) nx = nodes_x-1;
    if(ny < 0) ny = 0; else if(ny > nodes_y-1) ny = nodes_y-1;
    int ia = nodes_offset +  y * nodes_x +  x;
    int ib = nodes_offset + ny * nodes_x + nx;
    SpringConstraint.addSpring(particles, ia, ib, type);
  }

}
  
  
  
 
  