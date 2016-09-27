package com.thomasdiewald.pixelflow.java.particlephysics.softbodies3D;

import java.util.Random;

import com.thomasdiewald.pixelflow.java.geometry.DwHalfEdge;
import com.thomasdiewald.pixelflow.java.geometry.DwHalfEdge.Edge;
import com.thomasdiewald.pixelflow.java.particlephysics.DwParticle3D;
import com.thomasdiewald.pixelflow.java.particlephysics.DwPhysics3D;
import com.thomasdiewald.pixelflow.java.particlephysics.DwSpringConstraint3D;
import com.thomasdiewald.pixelflow.java.geometry.DwIcosahedron;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.opengl.PGraphics2D;


public class DwSoftBall extends DwSoftBody3D{
  
  DwHalfEdge.Mesh mesh;
  
  // specific attributes for this body
  public float nodes_r;
  public int subdivisions;
  public float radius;

  public int bend_spring_mode = 0;
  public int bend_spring_dist = 3; // try other values, it affects the objects stiffness
  
  Random rand;
  
  public DwSoftBall(){
  }

  public void create(DwPhysics3D physics, int subdivisions, float radius,float start_x, float start_y, float start_z){
    
    DwIcosahedron icosahedron = new DwIcosahedron(subdivisions);
    mesh = new DwHalfEdge.Mesh(icosahedron);

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
    DwHalfEdge.Edge[] edges = new DwHalfEdge.Edge[6];
    for(int ia = 0; ia < num_nodes; ia++){
      int edge_count = mesh.getVertexEdges(ia, edges);
      
      for(int j = 0; j < edge_count; j++){
        int ib = edges[j].pair.vert;     
        addSpring(ia, ib, DwSpringConstraint3D.TYPE.STRUCT);
      }
    }

    // 3) compute best nodes radius
    float nodes_r_tmp = Float.MAX_VALUE;
    for(int ia = 0; ia < num_nodes; ia++){
      DwParticle3D pa = particles[ia];
      for(int j = 0; j < pa.spring_count; j++){
        if(pa.springs[j].dd_rest < nodes_r_tmp){
          nodes_r_tmp = pa.springs[j].dd_rest;
        }
      }   
    }
    
    // 4) update nodes radius
    nodes_r = nodes_r_tmp * 0.5f;
    for(int ia = 0; ia < num_nodes; ia++){
      particles[ia].setRadius(nodes_r);
      particles[ia].setRadiusCollision(nodes_r * collision_radius_scale);
    }
    
    DwParticle3D.MAX_RAD = Math.max(DwParticle3D.MAX_RAD, nodes_r);

    
    // 2) create BEND springs
    for(int ia = 0, ib = 0; ia < num_nodes; ia++){
//      ib = (int) (rand.nextFloat() * num_nodes);
//      addSpring(ia, ib, SpringConstraint3D.TYPE.BEND);
//      
//      ib = (int) (rand.nextFloat() * num_nodes);
//      addSpring(ia, ib, SpringConstraint3D.TYPE.BEND);
      
      
      
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
        addSpring(ia, ib, DwSpringConstraint3D.TYPE.BEND);
      } 
    }
    

    
    // add new particles to the physics-world
    physics.addParticles(particles, num_nodes);
  }
  
 
  public DwParticle3D getNode(int idx){
    if(idx < 0 || idx > num_nodes) return null;
    return particles[idx];
  }
  
  
  public void addSpring(int ia, int ib, DwSpringConstraint3D.TYPE type){
    DwSpringConstraint3D.addSpring(particles[ia], particles[ib], param_spring, type);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // NORMALS
  //////////////////////////////////////////////////////////////////////////////
  

  public float[][] normals;
  public float normal_dir = -1f;
  private float[][] cross = new float[4][3];
  
  private void computeNormals(float[] normal, DwParticle3D pC, 
                                              DwParticle3D pT,
                                              DwParticle3D pB,
                                              DwParticle3D pL,
                                              DwParticle3D pR)
  {
    int count = 0;
//    count  = VerletParticle3D.cross(pC, pT, pR, cross[0]);
//    count += VerletParticle3D.cross(pC, pR, pB, cross[count]);
//    count += VerletParticle3D.cross(pC, pB, pL, cross[count]);
//    count += VerletParticle3D.cross(pC, pL, pT, cross[count]);

    int nx = 0, ny = 0, nz = 0;
    for(int k = 0; k < count; k++){
      nx += cross[k][0];
      ny += cross[k][1];
      nz += cross[k][2];
    }
    
    float dd_sq  = nx*nx + ny*ny + nz*nz;
    float dd_inv = normal_dir/(float)(Math.sqrt(dd_sq)+0.000001f);
    
    normal[0] = nx * dd_inv;
    normal[1] = ny * dd_inv;
    normal[2] = nz * dd_inv;  
  }
  
  

  @Override
  public void computeNormals(){
    DwHalfEdge.Edge[] edges = new DwHalfEdge.Edge[6]; // octrahedron, so max=6, min=5 (edges/vertex) 
    for(int ia = 0; ia < num_nodes; ia++){
      
      DwParticle3D pC = particles[ia];
      float[]n = normals[ia]; n[0] = n[1] = n[2] = 0;
      int edge_count = mesh.getVertexEdges(ia, edges);
      for(int j = 0; j < edge_count-1; j++){
        int ib0 = edges[j+0].pair.vert;
        int ib1 = edges[j+1].pair.vert;
        DwParticle3D.crossAccum(pC, particles[ib0], particles[ib1], n);
      }
      int ib0 = edges[0].pair.vert;
      int ib1 = edges[edge_count-1].pair.vert;
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

  @Override
  public void displayMesh(PGraphics pg){
    int     faces_count = mesh.ifs.getFacesCount();
    int[][] faces       = mesh.ifs.getFaces();
    
    float[] n = new float[3]; // normal buffer
    
    pg.fill(material_color);
    pg.beginShape(PConstants.TRIANGLES);
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
        pg.normal(n[0], n[1], n[2]); 
        pg.vertex(p0.cx, p0.cy, p0.cz);
        pg.vertex(p1.cx, p1.cy, p1.cz);
        pg.vertex(p2.cx, p2.cy, p2.cz);
      } else {
        n = normals[v0];  pg.normal(n[0], n[1], n[2]);  pg.vertex(p0.cx, p0.cy, p0.cz);
        n = normals[v1];  pg.normal(n[0], n[1], n[2]);  pg.vertex(p1.cx, p1.cy, p1.cz);
        n = normals[v2];  pg.normal(n[0], n[1], n[2]);  pg.vertex(p2.cx, p2.cy, p2.cz);
      }
    }
    pg.endShape();
    
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
  
  
  
 
  