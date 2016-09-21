package VerletPhysics_Cloth;

import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletPhysics2D;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.SpringConstraint;

public class SoftBody{
  
  int   collision_group_id; // particles that share the same id, are ignored during collision tests
  int   nodes_x;
  int   nodes_y;
  float nodes_r;
  
  int   num_nodes;    // number of particles for this object
  int   nodes_offset; // offset in the global array, used for creating a unique id
  
  public int dist_bend_spring = 4; // try other values, it affects the objects stiffness
  
  VerletParticle2D[] particles;
  
  
  public SoftBody(){
  }
  
  public void create(VerletPhysics2D physics, VerletParticle2D.Param param, int nx, int ny, float nr, float start_x, float start_y){
 
    this.collision_group_id = physics.getNewCollisionGroupId();
    this.nodes_offset       = physics.getParticlesCount();
    this.nodes_x            = nx;
    this.nodes_y            = ny;
    this.nodes_r            = nr;
    this.num_nodes          = nodes_x * nodes_y;

    // generate new particles array for this body
    this.particles = new VerletParticle2D[num_nodes];

    // temp variables
    int x, y, ox, oy, idx, idx_world;
    float px, py;
    
    // 1) init particles
    for(y = 0; y < nodes_y; y++){
      for(x = 0; x < nodes_x; x++){
        idx            = y * nodes_x + x;
        idx_world      = idx + nodes_offset;
        px             = start_x + x * nodes_r * 2;
        py             = start_y + y * nodes_r * 2;
        particles[idx] = new VerletParticle2D(idx_world, px, py, nodes_r);
        particles[idx].collision_group = collision_group_id;
        particles[idx].setParamByRef(param);
      }
    }
    
    
    ox = dist_bend_spring;
    oy = dist_bend_spring;
 
    // 2) create springs
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
        
        addSpring(x, y, -ox, -oy, SpringConstraint.TYPE.BEND);
        addSpring(x, y, +ox, -oy, SpringConstraint.TYPE.BEND);
        addSpring(x, y, -ox, +oy, SpringConstraint.TYPE.BEND);
        addSpring(x, y, +ox, +oy, SpringConstraint.TYPE.BEND);

//        addSpring(x, y, -ox,   0, SpringConstraint.TYPE.BEND);
//        addSpring(x, y, +ox,   0, SpringConstraint.TYPE.BEND);
//        addSpring(x, y,   0, +oy, SpringConstraint.TYPE.BEND);
//        addSpring(x, y,   0, -oy, SpringConstraint.TYPE.BEND);
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

    SpringConstraint.addSpring(particles, ia, ib, type);
  }

}
  
  
  
 
  