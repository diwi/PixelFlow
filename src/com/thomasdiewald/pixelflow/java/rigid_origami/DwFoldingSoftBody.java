package com.thomasdiewald.pixelflow.java.rigid_origami;

import com.thomasdiewald.pixelflow.java.rigid_origami.DwDisplayUtils;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody.StrokeStyle;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;


public class DwFoldingSoftBody extends DwSoftBody3D{
  
  public DwFoldingModel foldingmodel;
 
  public DwFoldingSoftBody(){
  }

  public void create(DwPhysics<DwParticle3D> physics, DwFoldingModel pattern_, DwSpringConstraint.Param rigid, DwSpringConstraint.Param soft){
    
    this.foldingmodel = pattern_;

    int       verts_count = foldingmodel.ifs.getVertsCount();
    float[][] verts       = foldingmodel.ifs.getVerts();
    
    this.physics            = physics;
    this.nodes_offset       = physics.getParticlesCount();
    this.collision_group_id = physics.getNewCollisionGroupId();
    this.self_collisions    = true;
    this.num_nodes          = verts_count;
    this.particles          = new CustomParticle3D[num_nodes];
    
    fold_value_cur = 1;
    fold_value_dst = 1;

    for(int i = 0; i < num_nodes; i++){
      particles[i] = new CustomParticle3D(i + nodes_offset, verts[i], 1);
      particles[i].setCollisionGroup(collision_group_id);
      particles[i].setParamByRef(param_particle);
    }
    
    foldingmodel.createSprings(physics, particles, rigid, soft);
    
    foldingmodel.createNodeRadius(physics, particles);
    
    // add new particles to the physics-world
    physics.addParticles(particles, num_nodes);
    
    updateFoldValue();
  }
  
  
  public float fold_value_stp = 0.001f;
  float fold_value_cur = 1;
  float fold_value_dst = 1;
  
  public void addFoldValue(float fold_value){
    fold_value_dst += fold_value;
    fold_value_dst = Math.min(fold_value_dst, 1);
    fold_value_dst = Math.max(fold_value_dst, 0);
    if(fold_value_cur < fold_value_dst){
      fold_value_stp = +Math.abs(fold_value_stp);
    } else {
      fold_value_stp = -Math.abs(fold_value_stp);
    }
  }
  
  public void setFoldValue(float fold_value){
    this.fold_value_dst = fold_value;
    fold_value_dst = Math.min(fold_value_dst, 1);
    fold_value_dst = Math.max(fold_value_dst, 0);
    if(fold_value_cur < fold_value_dst){
      fold_value_stp = +Math.abs(fold_value_stp);
    } else {
      fold_value_stp = -Math.abs(fold_value_stp);
    }
  }
  
  public void updateFoldValue(){
    fold_value_cur += fold_value_stp;
    float dval = fold_value_dst - fold_value_cur;
    if(dval * fold_value_stp < 0){
      fold_value_cur = fold_value_dst;
    }
    if(foldingmodel != null){
      foldingmodel.manipSprings(fold_value_cur);
    }
  }
  
  public void manipSprings(float scale){
    if(foldingmodel != null){
      foldingmodel.manipSprings(scale);
    }
  }
  
  
  
  public DwParticle3D getNode(int idx){
    if(idx < 0 || idx > num_nodes) return null;
    return particles[idx];
  }
  
  

  public float[][] normals_face;

  @Override
  public void computeNormals(){
    normals_face = foldingmodel.computeFaceNormals(normals_face, particles);
  }
  

  
  private PShape createShape(PGraphics pg){
    PShape shp = pg.createShape();
    shp.beginShape(PConstants.TRIANGLES);
    foldingmodel.display(shp, this);
    shp.endShape();
    return shp;
  }
  
  
  @Override
  public void createShapeMesh(PGraphics pg){
    PShape shp = createShape(pg);
    setShapeMesh(pg.parent, shp);
  }
  
  
  @Override
  public void createShapeWireframe(PGraphics pg, StrokeStyle style){
    PShape shp = createShape(pg);
    shp.setTexture(null);
    shp.setFill(false);
    shp.setStroke(true);
    shp.setStroke(style.stroke_color);
    shp.setStrokeWeight(style.stroke_weight);
    
    setShapeWireframe(pg.parent, shp);
  }
  

  
  
  
  @Override
  public void displayNormals(PGraphics pg){
    pg.beginShape(PConstants.LINES);
    int     faces_count = foldingmodel.ifs.getFacesCount();
    int[][] faces       = foldingmodel.ifs.getFaces();
    for(int i = 0; i < faces_count; i++){
      int ia = faces[i][0];
      DwDisplayUtils.normal(pg, particles[ia], normals_face[i], display_normal_length);
    }
    pg.endShape();
  }



}
  
  
  
 
  