package com.thomasdiewald.pixelflow.java.softbodydynamics.softbody;

import java.util.Random;

import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody2D;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import processing.core.PGraphics;
import processing.core.PShape;

public class DwSoftBall2D extends DwSoftBody2D{
  
  // specific attributes for this body
  float circle_x;
  float circle_y;
  float cirlce_r;
  float nodes_r;
  
  public int bend_spring_mode = 0;
  public int bend_spring_dist = 3; // try other values, it affects the objects stiffness
  
  Random rand;
  
  
  public DwSoftBall2D(){
  }
  
  public void create(DwPhysics<DwParticle2D> physics, float circle_x, float circle_y, float cirlce_r, float nodes_r){
    
    
    // compute number of circle vertices
    float threshold1 = 10;  // radius shortening for arc segments
    float threshold2 = 100; // arc between segments
    
    double arc1 = Math.acos(Math.max((cirlce_r-threshold1), 0) / cirlce_r);
    double arc2 = (180 - threshold2) * Math.PI / 180;
    double arc = Math.min(arc1, arc2);

    int num_vtx = (int)Math.ceil(2*Math.PI/arc);
    num_vtx += (num_vtx*0.5f)*2;
    
    // set fields
    this.physics            = physics;
    this.rand               = new Random(0);
    this.collision_group_id = physics.getNewCollisionGroupId();
    this.nodes_offset       = physics.getParticlesCount();
    this.circle_x           = circle_x;
    this.circle_y           = circle_y;
    this.cirlce_r           = cirlce_r;
    this.nodes_r            = nodes_r;
    this.num_nodes          = num_vtx;
    this.particles          = new DwParticle2D[num_nodes];
    
    DwParticle2D.MAX_RAD = Math.max(DwParticle2D.MAX_RAD, nodes_r);

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
      particles[idx] = new CustomParticle2D(idx_world, px, py, nodes_r);
      particles[idx].setParamByRef(param_particle);
      particles[idx].setRadiusCollision(nodes_r * collision_radius_scale);
      particles[idx].collision_group = collision_group_id;
      if(self_collisions){
        particles[idx].collision_group = physics.getNewCollisionGroupId();
      }
    }
    
 
    // 2) create springs
    for(int i = 0; i < num_nodes; i++){
      addSprings(i, 1, DwSpringConstraint.TYPE.STRUCT);
      
      if(bend_spring_mode == 0){
        addSprings(i, bend_spring_dist, DwSpringConstraint.TYPE.BEND);
      }
      // random, 'kind of' anisotropic
      if(bend_spring_mode == 1){
        addSprings(i, (int)(num_nodes/4 + rand.nextFloat() * (num_nodes/2-num_nodes/4)), DwSpringConstraint.TYPE.BEND);
      }
    }
    
    // add new particles to the physics-world
    physics.addParticles(particles, num_nodes);
  }
  
 
  public DwParticle2D getNode(int idx){
    return particles[idx];
  }
  
  public void addSprings(int ia, int off, DwSpringConstraint.TYPE type){
    int ib_L = (num_nodes + ia + off) % num_nodes;
    int ib_R = (num_nodes + ia - off) % num_nodes;
    addSpring(ia, ib_L, type);
    addSpring(ia, ib_R, type);
  }
  
  
  public void addSpring(int ia, int ib, DwSpringConstraint.TYPE type){
    DwSpringConstraint2D.addSpring(physics, particles[ia], particles[ib], param_spring, type);
  }


  
  private PShape createShape(PGraphics pg) {
    PShape shp = pg.createShape();

    shp.beginShape();
    shp.fill(material_color);
    shp.noStroke();
    for(int i = 0; i < num_nodes; i++){
      DwParticle2D pa = particles[i];
      if(pa.all_springs_deactivated) continue;
      shp.vertex(pa.cx, pa.cy);
    }
    shp.endShape();
    return shp;
  }
  
  
  
  @Override
  public void createShapeMesh(PGraphics pg) {
    PShape shp = createShape(pg);
    shp.setStroke(false);
    setShapeMesh(pg.parent, shp);
  }
  
  @Override
  public void createShapeWireframe(PGraphics pg, DwStrokeStyle style){
    PShape shp = createShape(pg);
    
    shp.setTexture(null);
    shp.setFill(false);
    shp.setStroke(true);
    shp.setStroke(style.stroke_color);
    shp.setStrokeWeight(style.stroke_weight);
    setShapeWireframe(pg.parent, shp);
  }



}
  
  
  
 
  