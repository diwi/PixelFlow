package VerletPhysics_SoftBodyCollision;

import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.SpringConstraint;

public class SoftCircle{
  int body_idx;
  float nodes_radius;
  int nodes_x;
  int nodes_y;
  int num_nodes;
  int nodes_offset;
  
//  public float SPRING_dec = 0.9999f;
//  public float SPRING_inc = 0.0009999f;
  public int dist_bend = 4;
  
   
  public SoftCircle(int body_idx, float nodes_radius){
    this.body_idx = body_idx;
    this.nodes_radius = nodes_radius;
  }
  
  public VerletParticle2D[] create(VerletParticle2D[] particles, VerletParticle2D.Param param, int center_x, int center_y, float radius){
    nodes_offset = particles.length;

    float threshold1 = 20;  // radius shortening for arc segments
    float threshold2 = 100; // arc between segments
    
    double arc1 = Math.acos(Math.max((radius-threshold1), 0) / radius);
    double arc2 = (180 - threshold2) * Math.PI / 180;
    double arc = Math.min(arc1, arc2);
    
    int num_vtx = (int)Math.ceil(2*Math.PI/arc);
    
    num_vtx += (num_vtx*0.5f)*2;
 
    num_nodes = num_vtx;
    particles = Arrays.copyOf(particles, nodes_offset + num_nodes);
    
    
    int idx = nodes_offset;
    
    // center
//    particles[idx] = new Particle(idx, center_x, center_y, nodes_radius);
//    particles[idx].collision_group = body_idx;
//    idx++;

    // circle vertices
    for(int i = 0; i < num_vtx; i++, idx++){
      float x = (float) Math.cos(i * 2*Math.PI/num_vtx) ;
      float y = (float) Math.sin(i * 2*Math.PI/num_vtx) ;
      float px = center_x + x * radius;
      float py = center_y + y * radius;
      
      particles[idx] = new VerletParticle2D(idx, px, py, nodes_radius);
      particles[idx].collision_group = body_idx;
      particles[idx].setParamByRef(param);
    }
    
    
    int off = 0;
    int ia = 0, ib_L = 0, ib_R = 0;
    for(int i = 0; i < num_vtx; i++){
      ia  = i;


      off = 1;
      ib_L = (num_vtx + ia + off) % num_vtx;
      ib_R = (num_vtx + ia - off) % num_vtx;
      
      addSpring(particles, ia, ib_L, SpringConstraint.TYPE.STRUCT);
      addSpring(particles, ia, ib_R, SpringConstraint.TYPE.STRUCT);

      off = 2;
      ib_L = (num_vtx + ia + off) % num_vtx;
      ib_R = (num_vtx + ia - off) % num_vtx;
      
      addSpring(particles, ia, ib_L, SpringConstraint.TYPE.SHEAR);
      addSpring(particles, ia, ib_R, SpringConstraint.TYPE.SHEAR);

      off = 4;
      ib_L = (num_vtx + ia + off) % num_vtx;
      ib_R = (num_vtx + ia - off) % num_vtx;
      
      addSpring(particles, ia, ib_L, SpringConstraint.TYPE.SHEAR);
      addSpring(particles, ia, ib_R, SpringConstraint.TYPE.SHEAR);

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
  
  
  public void addSpring(VerletParticle2D[] particles, int ia, int ib, SpringConstraint.TYPE type){
    if(ia != ib){
      ia += nodes_offset;
      ib += nodes_offset;
      
      VerletParticle2D pa = particles[ia];
      VerletParticle2D pb = particles[ib];
      
      // compute rest distance (length of spring)
      float dx = pb.cx - pa.cx;
      float dy = pb.cy - pa.cy;
      float dd_rest_sq = dx*dx + dy*dy;
//      SpringConstraint constraint = new SpringConstraint(ib, SPRING_inc, SPRING_dec, dd_rest_sq, type);
      SpringConstraint constraint = new SpringConstraint(ib, dd_rest_sq, type);
      pa.addSpring(constraint);
    }
  }

}
  
  
  
 
  