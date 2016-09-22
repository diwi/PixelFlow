package VerletPhysics_Dev;

import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics2D;

public class SoftBall extends SoftBody{
  
  // specific attributes for this body
  float circle_x;
  float circle_y;
  float cirlce_r;
  float nodes_r;
  
  public int bend_spring_mode = 0;
  
  public SoftBall(){
  }
  
  public void create(VerletPhysics2D physics, VerletParticle2D.Param param, float circle_x, float circle_y, float cirlce_r, float nodes_r){
    
    // compute number of circle vertices
    float threshold1 = 10;  // radius shortening for arc segments
    float threshold2 = 100; // arc between segments
    
    double arc1 = Math.acos(Math.max((cirlce_r-threshold1), 0) / cirlce_r);
    double arc2 = (180 - threshold2) * Math.PI / 180;
    double arc = Math.min(arc1, arc2);

    int num_vtx = (int)Math.ceil(2*Math.PI/arc);
    num_vtx += (num_vtx*0.5f)*2;
    
    // set fields
    this.collision_group_id = physics.getNewCollisionGroupId();
    this.nodes_offset       = physics.getParticlesCount();
    this.circle_x           = circle_x;
    this.circle_y           = circle_y;
    this.cirlce_r           = cirlce_r;
    this.nodes_r            = nodes_r;
    this.num_nodes          = num_vtx;
    this.particles          = new VerletParticle2D[num_nodes];
    
    VerletParticle2D.MAX_RAD = Math.max(VerletParticle2D.MAX_RAD, nodes_r);

    // temp variables
    int idx, idx_world;
    float x, y, px, py;
    
    // 1) init particles
    for(idx = 0; idx < num_nodes; idx++){
      idx_world = idx + nodes_offset;
      x  = (float) Math.cos(idx * 2*Math.PI/num_nodes) ;
      y  = (float) Math.sin(idx * 2*Math.PI/num_nodes) ;
      px = circle_x + x * cirlce_r;
      py = circle_y + y * cirlce_r;
      particles[idx] = new CustomVerletParticle2D(idx_world, px, py, nodes_r);
      particles[idx].collision_group = collision_group_id;
      particles[idx].setParamByRef(param);
    }
    
 
    // 2) create springs
    for(int i = 0; i < num_nodes; i++){
      addSprings(i, 1, SpringConstraint.TYPE.STRUCT);
      
      if(bend_spring_mode == 0){
        addSprings(i, 4, SpringConstraint.TYPE.BEND);
      }
      if(bend_spring_mode == 1){
        addSprings(i, num_nodes / 2, SpringConstraint.TYPE.BEND);
      }
      if(bend_spring_mode == 2){
        addSprings(i, num_nodes / 3, SpringConstraint.TYPE.BEND);
      }
      // random, 'kind of' anisotropic
      if(bend_spring_mode == 3){
        addSprings(i, (int)(1 + Math.random() * (num_nodes-1)) , SpringConstraint.TYPE.BEND);
      }
    }
    
    // add new particles to the physics-world
    physics.addParticles(particles, num_nodes);
  }
  
 
  public VerletParticle2D getNode(VerletParticle2D[] particles, int idx){
    return particles[idx];
  }
  
  public void addSprings(int ia, int off, SpringConstraint.TYPE type){
    int ib_L = (num_nodes + ia + off) % num_nodes;
    int ib_R = (num_nodes + ia - off) % num_nodes;
    addSpring(ia, ib_L, type);
    addSpring(ia, ib_R, type);
  }
  
  
  public void addSpring(int ia, int ib, SpringConstraint.TYPE type){
    SpringConstraint.addSpring(particles[ia], particles[ib], type);
  }
  

}
  
  
  
 
  