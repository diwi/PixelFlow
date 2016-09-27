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

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwStack;

import processing.core.PConstants;
import processing.opengl.PGraphics3D;


public class DwHalfEdge {

  static public class Edge{
    public int  FLAG; // can be used for anything. e.g. bitmask, pointer, boolean etc...
    public Edge pair;
    public Edge next;
    public int  vert;

    public Edge(int vert){
      this.vert = vert;
    }
       
    public int keyThis(){ return this.vert << 16 | next.vert; }
    public int keyPair(){ return next.vert << 16 | this.vert; }
    
    
    
    public void put(HashMap<Integer, Edge> edgemap){
      edgemap.put(this.keyThis(), this);
    }
    
    public void getPair(HashMap<Integer, Edge> edgemap){
      pair = edgemap.get(this.keyPair());
    }
  }
  
  

  static public class Face{
    public int  FLAG;
    public Edge edge;
    public Face(Edge edge){
      this.edge = edge;
    }
  }


  static public class Vert{
    public int  FLAG;
    public Edge edge;
    public Vert(Edge edge){
      this.edge = edge;
    }
  }
  
  
  
  static public class Mesh{
    
    public DwIndexedFaceSetAble ifs;
    public Edge[] edges;
    public Vert[] verts; // not really needed
    public Face[] faces; // not really needed
    
    public int verts_per_face;
    
    public Mesh(DwIndexedFaceSetAble ifs){
      int     faces_count = ifs.getFacesCount();
      int[][] faces       = ifs.getFaces();
      
      int verts_per_face = faces[0].length;
      
      for(int i = 0; i < faces_count; i++){
        if(verts_per_face != faces[i].length) { 
          verts_per_face = -1; 
          break;
        }
      }
      if(verts_per_face == -1){
        // mixed: faces have all a different vertexcount
      }
      if(verts_per_face == 3){
        createFromTriangleMesh(ifs, verts_per_face);
      }
      if(verts_per_face == 4){
        createFromQuadMesh(ifs, verts_per_face);
      }
    }
    
    private void createFromQuadMesh(DwIndexedFaceSetAble ifs, int verts_per_face){
      this.ifs = ifs;
      this.verts_per_face = verts_per_face;
      
      int[][] faces       = ifs.getFaces();
      int     faces_count = ifs.getFacesCount();
      int     verts_count = ifs.getVertsCount();
      int     edges_count = ifs.getFacesCount() * verts_per_face;
      
      this.edges = new DwHalfEdge.Edge[edges_count];
      this.faces = new DwHalfEdge.Face[faces_count];
      this.verts = new DwHalfEdge.Vert[verts_count];
      
      HashMap<Integer, DwHalfEdge.Edge> edgemap = new HashMap<Integer, DwHalfEdge.Edge>();
      
      // setup edges/faces
      for(int i = 0, edge_id = 0; i < faces_count; i++){
        // create face-edges
        DwHalfEdge.Edge e01 = new DwHalfEdge.Edge(faces[i][0]); 
        DwHalfEdge.Edge e12 = new DwHalfEdge.Edge(faces[i][1]);
        DwHalfEdge.Edge e23 = new DwHalfEdge.Edge(faces[i][2]);
        DwHalfEdge.Edge e30 = new DwHalfEdge.Edge(faces[i][3]);
        
        // link face-edges
        e01.next = e12;
        e12.next = e23;
        e23.next = e30;
        e30.next = e01;
        
        // put face-edges into map
        e01.put(edgemap);
        e12.put(edgemap);
        e23.put(edgemap);
        e30.put(edgemap);
        
        // add to list
        edges[edge_id++] = e01;
        edges[edge_id++] = e12;
        edges[edge_id++] = e23;
        edges[edge_id++] = e30;
        
        // link: edge <-> face
        this.faces[i] = new DwHalfEdge.Face(e01); 
      }

      // setup edge-pairs
      for(DwHalfEdge.Edge edge : edges){
        // link: edge <-> edge
        edge.getPair(edgemap);
        // link: edge <-> vertex
        verts[edge.vert] = new DwHalfEdge.Vert(edge);
      }
      
      
    }
    
    private void createFromTriangleMesh(DwIndexedFaceSetAble ifs, int verts_per_face){
      this.ifs = ifs;
      this.verts_per_face = verts_per_face;
      int[][] faces       = ifs.getFaces();
      int     faces_count = ifs.getFacesCount();
      int     verts_count = ifs.getVertsCount();
      int     edges_count = ifs.getFacesCount() * verts_per_face;
      
      this.edges = new DwHalfEdge.Edge[edges_count];
      this.faces = new DwHalfEdge.Face[faces_count];
      this.verts = new DwHalfEdge.Vert[verts_count];

      HashMap<Integer, DwHalfEdge.Edge> edgemap = new HashMap<Integer, DwHalfEdge.Edge>();
      
      // setup edges/faces
      for(int i = 0, edge_id = 0; i < faces_count; i++){
        
        // create face-edges
        DwHalfEdge.Edge e01 = new DwHalfEdge.Edge(faces[i][0]); 
        DwHalfEdge.Edge e12 = new DwHalfEdge.Edge(faces[i][1]);
        DwHalfEdge.Edge e20 = new DwHalfEdge.Edge(faces[i][2]);
        
        // link face-edges
        e01.next = e12;
        e12.next = e20;
        e20.next = e01;
        
        // put face-edges into map
        e01.put(edgemap);
        e12.put(edgemap);
        e20.put(edgemap);
        
        // add to list
        edges[edge_id++] = e01;
        edges[edge_id++] = e12;
        edges[edge_id++] = e20;
        
        // link: edge <-> face
        this.faces[i] = new DwHalfEdge.Face(e01); 
      }

      // setup edge-pairs
      for(DwHalfEdge.Edge edge : edges){
        // link: edge <-> edge
        edge.getPair(edgemap);
        // link: edge <-> vertex
        verts[edge.vert] = new DwHalfEdge.Vert(edge);
      }
    }
    
    
   
    public int getNumberOfVertexEdges(int vertex_id){
      DwHalfEdge.Vert vert = verts[vertex_id];
      DwHalfEdge.Edge edge = vert.edge;  
      DwHalfEdge.Edge iter = edge;
      int count = 0;
      do {
        count++;
      } while((iter = iter.pair.next) != edge);
      return count;
    }
    
    /**
     * returns the number of edges attached to this vertex.
     * edges[] is filled up to that number.
     * 
     * @param vertex_id
     * @param edges
     * @return int, number of edges attached to this vertex
     */
    public int getVertexEdges(int vertex_id, DwHalfEdge.Edge[] edges){
      DwHalfEdge.Vert vert = verts[vertex_id];
      DwHalfEdge.Edge edge = vert.edge;  
      DwHalfEdge.Edge iter = edge;
      int count = 0;
      do {
        if(count < edges.length){
          edges[count] = iter;
        }
        count++;
      } while((iter = iter.pair.next) != edge);
      return count;
    }
    
    

    // display stuff
    private int DISPLAY_BIT        = 0;
    private int DISPLAY_BIT_MASK   = 1 << 10;

    public void display(PGraphics3D pg){
      DISPLAY_BIT ^= DISPLAY_BIT_MASK; // toggle
      switch(verts_per_face){
        case -1: displayPolygons (pg, edges[0]); break;
        case  3: displayTriangles(pg, edges[0]); break;
        case  4: displayQuads    (pg, edges[0]); break;
      }
    }

    private void displayPolygons(PGraphics3D pg, DwHalfEdge.Edge edge){
      DwStack<DwHalfEdge.Edge> stack = new DwStack<DwHalfEdge.Edge>();
      stack.push(edge);
      float[][] verts = ifs.getVerts();
      float[] v;
      while(!stack.isEmpty()){
        edge = stack.pop();
        if(getFLAG_display(edge)){
          DwHalfEdge.Edge iter = edge;
          pg.beginShape();
          do {
            v = verts[iter.vert]; pg.vertex(v[0], v[1], v[2]); 
          } while((iter = iter.next) != edge);
          pg.endShape(PConstants.CLOSE);
          
          // recursively draw neighbors
          do {
            setFLAG_display(iter);
            stack.push(iter.pair); 
          } while((iter = iter.next) != edge);
        }
      }
    }
     
    private void displayQuads(PGraphics3D pg, DwHalfEdge.Edge edge){
      DwStack<DwHalfEdge.Edge> stack = new DwStack<DwHalfEdge.Edge>();
      stack.push(edge);
      float[][] verts = ifs.getVerts();
      float[] v;
      pg.beginShape(PConstants.QUADS);
      while(!stack.isEmpty()){
        edge = stack.pop();
        if(getFLAG_display(edge)){
          // draw quad
          v = verts[edge.vert]; edge = edge.next; pg.vertex(v[0], v[1], v[2]); 
          v = verts[edge.vert]; edge = edge.next; pg.vertex(v[0], v[1], v[2]); 
          v = verts[edge.vert]; edge = edge.next; pg.vertex(v[0], v[1], v[2]); 
          v = verts[edge.vert]; edge = edge.next; pg.vertex(v[0], v[1], v[2]); 
          // recursively draw neighbors
          stack.push((edge = edge.next).pair); setFLAG_display(edge); 
          stack.push((edge = edge.next).pair); setFLAG_display(edge); 
          stack.push((edge = edge.next).pair); setFLAG_display(edge);
          stack.push((edge = edge.next).pair); setFLAG_display(edge); 
        }
      }
      pg.endShape();
    }
    
    private void displayTriangles(PGraphics3D pg, DwHalfEdge.Edge edge){
      DwStack<DwHalfEdge.Edge> stack = new DwStack<DwHalfEdge.Edge>();
      stack.push(edge);  
      float[][] verts = ifs.getVerts();
      float[] v;
      pg.beginShape(PConstants.TRIANGLES);
      while(!stack.isEmpty()){
        edge = stack.pop();
        if(getFLAG_display(edge)){
          // draw triangle
          v = verts[edge.vert]; edge = edge.next; pg.vertex(v[0], v[1], v[2]); 
          v = verts[edge.vert]; edge = edge.next; pg.vertex(v[0], v[1], v[2]); 
          v = verts[edge.vert]; edge = edge.next; pg.vertex(v[0], v[1], v[2]); 
          // recursively draw neighbors
          stack.push((edge = edge.next).pair); setFLAG_display(edge); 
          stack.push((edge = edge.next).pair); setFLAG_display(edge); 
          stack.push((edge = edge.next).pair); setFLAG_display(edge);
        }
      }
      pg.endShape();
    }
    
    
    
    

    
    private boolean getFLAG_display(DwHalfEdge.Edge edge){
      return ((edge.FLAG ^ DISPLAY_BIT) & DISPLAY_BIT_MASK) != 0;
    }
    
    private void setFLAG_display(DwHalfEdge.Edge edge){
      edge.FLAG = ((edge.FLAG & ~DISPLAY_BIT_MASK) | DISPLAY_BIT);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
     
  }
  
  
 

}
