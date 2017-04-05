package com.thomasdiewald.pixelflow.java.softbodydynamics.softbody;

import java.util.Random;

import com.thomasdiewald.pixelflow.java.geometry.DwHalfEdge;
import com.thomasdiewald.pixelflow.java.geometry.DwHalfEdge.Edge;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;
import com.thomasdiewald.pixelflow.java.geometry.DwIcosahedron;
import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSetAble;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;


public class DwSoftBall3D extends DwSoftBody3D{
  

  public DwHalfEdge.Mesh mesh;
  
  // specific attributes for this body
  public float nodes_r;
  public int subdivisions;
  public float radius;

  public int bend_spring_mode = 0;
  public int bend_spring_dist = 3; // try other values, it affects the objects stiffness
  
  Random rand;
  
  public DwSoftBall3D(){
  }

  public void create(DwPhysics<DwParticle3D> physics, int subdivisions, float radius,float start_x, float start_y, float start_z){
    
    DwIndexedFaceSetAble ifs = new DwIcosahedron(subdivisions);
    
    this.mesh               = new DwHalfEdge.Mesh(ifs);
    this.physics            = physics;
    this.rand               = new Random(0);
    this.collision_group_id = physics.getNewCollisionGroupId();
    this.nodes_offset       = physics.getParticlesCount();
    this.subdivisions       = subdivisions;
    this.radius             = radius;
    this.nodes_r            = 1; // computed automatically, bellow
    this.num_nodes          = mesh.verts.length;
    this.particles          = new DwParticle3D[num_nodes];
    this.normals            = new float[num_nodes][3];

   
    // temp variables
    int idx, idx_world;
    float px, py, pz;
    
    float[][] verts = mesh.ifs.getVerts();
  
    // 1) init particles
    for(int i = 0; i < num_nodes; i++){
      idx            = i;
      idx_world      = idx + nodes_offset;
      px             = start_x + verts[i][0] * radius;
      py             = start_y + verts[i][1] * radius;
      pz             = start_z + verts[i][2] * radius;
      particles[idx] = new CustomParticle3D(idx_world, px, py, pz, nodes_r);
      particles[idx].setParamByRef(param_particle);
      particles[idx].setRadiusCollision(nodes_r * collision_radius_scale);
      particles[idx].collision_group = collision_group_id;
      if(self_collisions){
        particles[idx].collision_group = physics.getNewCollisionGroupId();
      }
    }
    


    // 2) create STRUCT springs
    // an icosahedron has a t most 6 edges per vertex
    float nodes_r_tmp = Float.MAX_VALUE;
    DwHalfEdge.Edge[] edges = new DwHalfEdge.Edge[6];
    for(int ia = 0; ia < num_nodes; ia++){
      int edge_count = mesh.getVertexEdges(ia, edges);
      
      for(int j = 0; j < edge_count; j++){
        int ib = edges[j].pair.vert;     
        DwSpringConstraint spring = addSpring(ia, ib, DwSpringConstraint.TYPE.STRUCT);
        if(spring != null && spring.dd_rest < nodes_r_tmp){
          nodes_r_tmp = spring.dd_rest;
        }
        
      }
    }

    // 3) compute best nodes radius
//    float nodes_r_tmp = Float.MAX_VALUE;
//    for(int ia = 0; ia < num_nodes; ia++){
//      DwParticle3D pa = particles[ia];
//      for(int j = 0; j < pa.spring_count; j++){
//        if(pa.springs[j].dd_rest < nodes_r_tmp){
//          nodes_r_tmp = pa.springs[j].dd_rest;
//        }
//      }   
//    }
    
    // 4) update nodes radius
    nodes_r = nodes_r_tmp * 0.5f;
    for(int ia = 0; ia < num_nodes; ia++){
      particles[ia].setRadius(nodes_r);
      particles[ia].setRadiusCollision(nodes_r * collision_radius_scale);
    }
    
    DwParticle3D.MAX_RAD = Math.max(DwParticle3D.MAX_RAD, nodes_r);

    
    // 2) create BEND springs
    for(int ia = 0, ib = 0; ia < num_nodes; ia++){

      int edge_count = mesh.getVertexEdges(ia, edges);
      
      for(int j = 0; j < edge_count; j++){
        Edge edge = edges[j];
        
        //  iterate edges, to get bend_spring_dist-edges offset
        for(int k = 0; k < bend_spring_dist; k++){
          edge = edge.next;
          edge = edge.pair.next;
          edge = edge.pair.next;
        }

        ib = edge.vert;     
        addSpring(ia, ib, DwSpringConstraint.TYPE.BEND);
      } 
    }
    

    
    // add new particles to the physics-world
    physics.addParticles(particles, num_nodes);
  }
  
 
  public DwParticle3D getNode(int idx){
    if(idx < 0 || idx > num_nodes) return null;
    return particles[idx];
  }
  
  
  public DwSpringConstraint addSpring(int ia, int ib, DwSpringConstraint.TYPE type){
//    DwSpringConstraint3D.addSpring(particles[ia], particles[ib], param_spring, type);
//    return null;
    return DwSpringConstraint3D.addSpring(physics, particles[ia], particles[ib], param_spring, type);
  }

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // NORMALS
  //////////////////////////////////////////////////////////////////////////////
  

  public float[][] normals;
  public float normal_dir = -1f;

  @Override
  public void computeNormals(){
    DwHalfEdge.Edge[] edges = new DwHalfEdge.Edge[6]; // octrahedron, so max=6, min=5 (edges/vertex) 
    for(int ia = 0; ia < num_nodes; ia++){
      
      DwParticle3D pC = particles[ia];
      float[]n = normals[ia]; n[0] = n[1] = n[2] = 0;
      int edge_count = mesh.getVertexEdges(ia, edges);
      for(int j = 0; j < edge_count-1; j++){
        int ib0 = edges[j+0].next.vert;
        int ib1 = edges[j+1].next.vert;
        DwParticle3D.crossAccum(pC, particles[ib0], particles[ib1], n);
      }
      int ib0 = edges[0].next.vert;
      int ib1 = edges[edge_count-1].next.vert;
      DwParticle3D.crossAccum(pC, particles[ib0], particles[ib1], n);
      
      float dd_sq  = n[0]*n[0] +  n[1]*n[1] +  n[2]*n[2];
      float dd_inv = normal_dir/(float)(Math.sqrt(dd_sq)+0.000001f);
      
      n[0] *= dd_inv;
      n[1] *= dd_inv;
      n[2] *= dd_inv;  
    }

  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // DISPLAY
  //////////////////////////////////////////////////////////////////////////////


  boolean FLAT_SHADING = false;
  
  
  private PShape createShape(PGraphics pg){
    int     faces_count = mesh.ifs.getFacesCount();
    int[][] faces       = mesh.ifs.getFaces();
    
    float[] n = new float[3]; // normal buffer
    
    PShape shp = pg.createShape();
    shp.beginShape(PConstants.TRIANGLES);
    shp.noStroke();
    shp.fill(material_color);
    for(int i = 0; i < faces_count; i++){
      int v0 = faces[i][0];
      int v1 = faces[i][1];
      int v2 = faces[i][2];
      DwParticle3D p0 = particles[v0]; if(p0.all_springs_deactivated) continue;
      DwParticle3D p1 = particles[v1]; if(p1.all_springs_deactivated) continue;
      DwParticle3D p2 = particles[v2]; if(p2.all_springs_deactivated) continue;
      
      if(FLAT_SHADING){
        n[0] = n[1] = n[2] = 0;
        DwParticle3D.crossAccum(p0, p1, p2, n);
        shp.normal(n[0], n[1], n[2]); 
        shp.vertex(p0.cx, p0.cy, p0.cz);
        shp.vertex(p1.cx, p1.cy, p1.cz);
        shp.vertex(p2.cx, p2.cy, p2.cz);
      } else {
        n = normals[v0];  shp.normal(n[0], n[1], n[2]);  shp.vertex(p0.cx, p0.cy, p0.cz);
        n = normals[v1];  shp.normal(n[0], n[1], n[2]);  shp.vertex(p1.cx, p1.cy, p1.cz);
        n = normals[v2];  shp.normal(n[0], n[1], n[2]);  shp.vertex(p2.cx, p2.cy, p2.cz);
      }
    }
    shp.endShape();
    return shp;
  }
  


  @Override
  public void createShapeMesh(PGraphics pg){
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


  private final void normal(PGraphics pg, DwParticle3D p, float[] n, float nlen){
    if(p.all_springs_deactivated) return;
    pg.vertex(p.cx          , p.cy          , p.cz          );
    pg.vertex(p.cx+n[0]*nlen, p.cy+n[1]*nlen, p.cz+n[2]*nlen);
  }
  
  
  @Override
  public void displayNormals(PGraphics pg){
    pg.beginShape(PConstants.LINES);
    for(int ia = 0; ia < num_nodes; ia++){
      normal(pg, particles[ia], normals[ia], display_normal_length);
    }
    pg.endShape();
  }
  


}
  
  
  
 
  