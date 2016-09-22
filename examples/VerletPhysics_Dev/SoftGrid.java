package VerletPhysics_Dev;

import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics2D;

public class SoftGrid extends SoftBody{
  
  // specific attributes for this body
  int   nodes_x;
  int   nodes_y;
  float nodes_r;
  
  public int bend_spring_mode = 0;
  public int bend_spring_dist = 3; // try other values, it affects the objects stiffness
  
  public SoftGrid(){
  }
  
  public void create(VerletPhysics2D physics, VerletParticle2D.Param param, int nx, int ny, float nr, float start_x, float start_y){
 
    this.collision_group_id = physics.getNewCollisionGroupId();
    this.nodes_offset       = physics.getParticlesCount();
    this.nodes_x            = nx;
    this.nodes_y            = ny;
    this.nodes_r            = nr;
    this.num_nodes          = nodes_x * nodes_y;
    this.particles          = new VerletParticle2D[num_nodes];
    
    VerletParticle2D.MAX_RAD = Math.max(VerletParticle2D.MAX_RAD, nr);

    // temp variables
    int idx, idx_world;
    int x, y, ox, oy;
    float px, py;
    
    // 1) init particles
    for(y = 0; y < nodes_y; y++){
      for(x = 0; x < nodes_x; x++){
        idx            = y * nodes_x + x;
        idx_world      = idx + nodes_offset;
        px             = start_x + x * nodes_r * 2;
        py             = start_y + y * nodes_r * 2;
        particles[idx] = new CustomVerletParticle2D(idx_world, px, py, nodes_r);
        particles[idx].collision_group = collision_group_id;
        particles[idx].setParamByRef(param);
      }
    }
    
    
    ox = bend_spring_dist;
    oy = bend_spring_dist;
 
    // 2) create springs
    for(y = 0; y < nodes_y; y++){
      for(x = 0; x < nodes_x; x++){
        
        if(CREATE_STRUCT_SPRINGS){
          addSpring(x, y, -1, 0, SpringConstraint.TYPE.STRUCT);
          addSpring(x, y,  0,-1, SpringConstraint.TYPE.STRUCT);
          addSpring(x, y, +1, 0, SpringConstraint.TYPE.STRUCT);
          addSpring(x, y,  0,+1, SpringConstraint.TYPE.STRUCT);
        }
                  
        if(CREATE_SHEAR_SPRINGS){
          addSpring(x, y, -1,-1, SpringConstraint.TYPE.SHEAR);
          addSpring(x, y, +1,-1, SpringConstraint.TYPE.SHEAR);
          addSpring(x, y, -1,+1, SpringConstraint.TYPE.SHEAR);
          addSpring(x, y, +1,+1, SpringConstraint.TYPE.SHEAR);
        }
        
        if(CREATE_BEND_SPRINGS){
          // diagonal
          if(bend_spring_mode == 0){
            addSpring(x, y, -ox, -oy, SpringConstraint.TYPE.BEND);
            addSpring(x, y, +ox, -oy, SpringConstraint.TYPE.BEND);
            addSpring(x, y, -ox, +oy, SpringConstraint.TYPE.BEND);
            addSpring(x, y, +ox, +oy, SpringConstraint.TYPE.BEND);
          }
          
          // orthogonal
          if(bend_spring_mode == 1){
            addSpring(x, y, -ox,   0, SpringConstraint.TYPE.BEND);
            addSpring(x, y, +ox,   0, SpringConstraint.TYPE.BEND);
            addSpring(x, y,   0, +oy, SpringConstraint.TYPE.BEND);
            addSpring(x, y,   0, -oy, SpringConstraint.TYPE.BEND);
          }
          
          // random, 'kind of' anisotropic
          if(bend_spring_mode == 2){
            for(int i = 0; i < 4; i++){
              ox = (int) ((Math.random()*2-1)*6);
              oy = (int) ((Math.random()*2-1)*4);
              addSpring(x, y, ox, oy, SpringConstraint.TYPE.BEND);
            }
          }
        }
        
      }
    }
    
    
    // add new particles to the physics-world
    physics.addParticles(particles, num_nodes);
  }
  
 
  public VerletParticle2D getNode(int x, int y){
    if(x < nodes_x && y < nodes_y){
      int idx = y *nodes_x + x;
      return particles[idx];
    } else {
      return null;
    }
  }
  
  
  public void addSpring(int x, int y, int offx, int offy, SpringConstraint.TYPE type){
    int nx = x + offx;
    int ny = y + offy;
    
    // clamp offset to grid-bounds
    if(nx < 0) nx = 0; else if(nx > nodes_x-1) nx = nodes_x-1;
    if(ny < 0) ny = 0; else if(ny > nodes_y-1) ny = nodes_y-1;

    int ia =  y * nodes_x +  x;
    int ib = ny * nodes_x + nx;

    SpringConstraint.addSpring(particles[ia], particles[ib], type);
  }
  

}
  
  
  
 
  