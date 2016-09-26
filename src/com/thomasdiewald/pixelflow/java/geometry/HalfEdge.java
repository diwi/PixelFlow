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
import processing.core.PConstants;
import processing.opengl.PGraphics3D;


public class HalfEdge {

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
    
    public IFSgetter ifs;
    public Edge[] edges;
    public Vert[] verts; // not really needed
    public Face[] faces; // not really needed
    
    public Mesh(IFSgetter ifs){
      
      int[][] faces       = ifs.getFaces();
      int     faces_count = ifs.getFacesCount();
      int     verts_count = ifs.getVertsCount();
      int     edges_count = ifs.getFacesCount() * 3;
      
      this.ifs   = ifs;
      this.edges = new HalfEdge.Edge[edges_count];
      this.faces = new HalfEdge.Face[faces_count];
      this.verts = new HalfEdge.Vert[verts_count];

      HashMap<Integer, HalfEdge.Edge> edgemap = new HashMap<Integer, HalfEdge.Edge>();
      
      // setup edges/faces
      for(int i = 0, edge_id = 0; i < faces_count; i++){
        
        // create face-edges
        HalfEdge.Edge e01 = new HalfEdge.Edge(faces[i][0]); 
        HalfEdge.Edge e12 = new HalfEdge.Edge(faces[i][1]);
        HalfEdge.Edge e20 = new HalfEdge.Edge(faces[i][2]);
        
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
        this.faces[i] = new HalfEdge.Face(e01); 
      }

      // setup edge-pairs
      for(HalfEdge.Edge edge : edges){
        // link: edge <-> edge
        edge.getPair(edgemap);
        // link: edge <-> vertex
        verts[edge.vert] = new HalfEdge.Vert(edge);
      }
      
      
//      for(int i = 0; i < verts.length; i++){
//        int count = getNumberOfVertexEdges(i);
//        if(count < 5){ 
//          System.out.println(count);
//        }
//      }
      
    }
    
    
   
    public int getNumberOfVertexEdges(int vertex_id){
      HalfEdge.Vert vert = verts[vertex_id];
      HalfEdge.Edge edge_orig = vert.edge;  
      HalfEdge.Edge edge_iter = edge_orig;
      int count = 0;
      do {
        count++;
      } while((edge_iter = edge_iter.pair.next) != edge_orig);
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
    public int getVertexEdges(int vertex_id, HalfEdge.Edge[] edges){
      HalfEdge.Vert vert = verts[vertex_id];
      HalfEdge.Edge edge_orig = vert.edge;  
      HalfEdge.Edge edge_iter = edge_orig;
      int count = 0;
      do {
        if(count < edges.length){
          edges[count] = edge_iter;
        }
        count++;
      } while((edge_iter = edge_iter.pair.next) != edge_orig);
      return count;
    }
    
    

    // display stuff
    private int DISPLAY_BIT        = 0;
    private int DISPLAY_BIT_MASK   = 1 << 10;

    public void display(PGraphics3D pg, float radius){
      DISPLAY_BIT ^= DISPLAY_BIT_MASK; // toggle
      displayIterative(pg, edges[0], radius);
    }
    
    
    private void displayIterative(PGraphics3D pg, HalfEdge.Edge edge, float radius){
      DwStack<HalfEdge.Edge> stack = new DwStack<HalfEdge.Edge>();
      stack.push(edge);
      
      float[][] verts = ifs.getVerts();
     
      pg.beginShape(PConstants.TRIANGLES);
      while(!stack.isEmpty()){
        edge = stack.pop();
        if(getFLAG_display(edge)){
                    float[] v = verts[edge.vert]; pg.vertex(v[0]*radius, v[1]*radius, v[2]*radius); 
          edge = edge.next; v = verts[edge.vert]; pg.vertex(v[0]*radius, v[1]*radius, v[2]*radius);   
          edge = edge.next; v = verts[edge.vert]; pg.vertex(v[0]*radius, v[1]*radius, v[2]*radius); 
         
          // recursively draw neighbors
          stack.push((edge = edge.next).pair); setFLAG_display(edge); 
          stack.push((edge = edge.next).pair); setFLAG_display(edge); 
          stack.push((edge = edge.next).pair); setFLAG_display(edge); 
        }
      }
      pg.endShape();
    }
    
    
    private boolean getFLAG_display(HalfEdge.Edge edge){
      return ((edge.FLAG ^ DISPLAY_BIT) & DISPLAY_BIT_MASK) != 0;
    }
    
    private void setFLAG_display(HalfEdge.Edge edge){
      edge.FLAG = ((edge.FLAG & ~DISPLAY_BIT_MASK) | DISPLAY_BIT);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
     
  }
  
  
 

}
