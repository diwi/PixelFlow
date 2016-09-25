/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.geometry;

import java.util.HashMap;
import java.util.Stack;

import processing.core.PConstants;
import processing.opengl.PGraphics3D;

/**
 * @author Thomas
 *
 */
public class IcosahedronMesh {

  public int num_faces;
  public int num_edges;
  public int num_verts;
  public int  [][] faces;
  public float[][] verts;
  
  
  public HalfEdge.Edge[] he_edges;
//  public HalfEdge.Face[] he_faces;
//  public HalfEdge.Vert[] he_verts;
  

  public IcosahedronMesh(){
  }
  
  public void create(int subdivisions){
    IcosahedronBuilder icosahedron_builder = new IcosahedronBuilder();
    icosahedron_builder.create(subdivisions);
    
    num_faces = icosahedron_builder.faces.size();
    num_verts = icosahedron_builder.verts.size();
    num_edges = num_faces * 3;
    
    faces = icosahedron_builder.faces.toArray(new int  [num_faces][3]);
    verts = icosahedron_builder.verts.toArray(new float[num_verts][3]);
    
    he_edges = new HalfEdge.Edge[num_edges];
//    he_faces = new HalfEdge.Face[num_faces];
//    he_verts = new HalfEdge.Vert[num_verts];
    
    HashMap<Integer, HalfEdge.Edge> edgemap = new HashMap<Integer, HalfEdge.Edge>();
    
    // setup edges/faces
    for(int i = 0, edge_id = 0; i < num_faces; i++){
      // create face-edges
      HalfEdge.Edge e01 = new HalfEdge.Edge(i, faces[i][0]); 
      HalfEdge.Edge e12 = new HalfEdge.Edge(i, faces[i][1]);
      HalfEdge.Edge e20 = new HalfEdge.Edge(i, faces[i][2]);
      
      // link face-edges
      e01.next = e12;
      e12.next = e20;
      e20.next = e01;
      
      // put face-edges into map
      e01.put(edgemap);
      e12.put(edgemap);
      e20.put(edgemap);
      
      he_edges[edge_id++] = e01;
      he_edges[edge_id++] = e12;
      he_edges[edge_id++] = e20;
      
//      he_faces[i] = new HalfEdge.Face(e01); // TODO
    }
    
    // setup edge-pairs
    for(int i = 0; i < num_edges; i++){
      he_edges[i].getPair(edgemap);
    }
    
//    // setup verts
//    for(int i = 0; i < num_faces; i++){
//      HalfEdge.Face face = he_faces[i];
//      he_verts[face.edge.vert] = new HalfEdge.Vert(face.edge); // TODO
//    }
    
  }
  
  
  
  
  
  
  
  
  public float SCALE = 100f;
  private int COUNTER = 0;
  
  public void display(PGraphics3D pg){
    COUNTER++;
    displayIterative(pg, he_edges[0]);
//    displayRecursive(pg, he_edges[0]); // too dangerous
  }
  
  private void displayIterative(PGraphics3D pg, HalfEdge.Edge edge){
    Stack<HalfEdge.Edge> stack = new Stack<HalfEdge.Edge>();
    stack.push(edge);
    
    while(!stack.isEmpty()){
      edge = stack.pop();
      if(edge.FLAG < COUNTER){
        pg.beginShape(PConstants.TRIANGLE);
                  float[] v = verts[edge.vert]; edge.FLAG++; pg.vertex(v[0]*SCALE, v[1]*SCALE, v[2]*SCALE);
        edge = edge.next; v = verts[edge.vert]; edge.FLAG++; pg.vertex(v[0]*SCALE, v[1]*SCALE, v[2]*SCALE);
        edge = edge.next; v = verts[edge.vert]; edge.FLAG++; pg.vertex(v[0]*SCALE, v[1]*SCALE, v[2]*SCALE);
        pg.endShape();
          
        // recursively draw neighbors
        stack.push((edge = edge.next).pair);
        stack.push((edge = edge.next).pair);
        stack.push((edge = edge.next).pair);
      }
    }
  }
  

  private void displayRecursive(PGraphics3D pg, HalfEdge.Edge edge){
    if(edge.FLAG < COUNTER){
      pg.beginShape(PConstants.TRIANGLE);
                float[] v = verts[edge.vert]; edge.FLAG++; pg.vertex(v[0]*SCALE, v[1]*SCALE, v[2]*SCALE);
      edge = edge.next; v = verts[edge.vert]; edge.FLAG++; pg.vertex(v[0]*SCALE, v[1]*SCALE, v[2]*SCALE);
      edge = edge.next; v = verts[edge.vert]; edge.FLAG++; pg.vertex(v[0]*SCALE, v[1]*SCALE, v[2]*SCALE);
      pg.endShape();
        
      // recursively draw neighbors
      displayRecursive(pg, (edge = edge.next).pair);
      displayRecursive(pg, (edge = edge.next).pair);
      displayRecursive(pg, (edge = edge.next).pair);
    }
  }
  
  
  
  
  
  
  
  
  
}
