/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;
import java.util.HashMap;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwStack;
import com.thomasdiewald.pixelflow.java.accelerationstructures.DwPair;

import processing.core.PConstants;
import processing.opengl.PGraphics3D;


public class DwHalfEdge {

  static public class Edge{
    public Edge pair;
    public Edge next;
    public int  vert;
    public int  FLAG; // can be used for anything. e.g. bitmask, pointer, boolean etc...
    public int  indx; // can be used for anything.
    public Edge(int vert){
      this.vert = vert;
    }
  }

  static public class Face{
    public Edge edge;
    public int  FLAG;
    public Face(Edge edge){
      this.edge = edge;
    }
  }

  static public class Vert{
    public Edge edge;
    public int  FLAG;
    public Vert(Edge edge){
      this.edge = edge;
    }
  }
  
  static public class Mesh{
    
    public DwIndexedFaceSetAble ifs;
    public Edge[] edges;
    public Vert[] verts; // not really needed
    public Face[] faces; // not really needed
    
    public int verts_per_face = -1; // 3 = triangle, 4 = quad, -1 = any polygon
    
    public Mesh(DwIndexedFaceSetAble ifs){
      create(ifs);
    }
    
    public void addFaces(ArrayList<Face> faces_list_new){
      int num_faces_new = faces_list_new.size();
      int num_faces_old = faces.length;
      
      Face[] faces_new = faces_list_new.toArray(new Face[num_faces_new]);
      Face[] faces_old = faces;
      
      faces = new Face[num_faces_old + num_faces_new];
      System.arraycopy(faces_old, 0, faces,             0, num_faces_old);
      System.arraycopy(faces_new, 0, faces, num_faces_old, num_faces_new);
    }
    
    private void create(DwIndexedFaceSetAble ifs){
      this.ifs = ifs;
      
      // IFS data
      int[][] ifs_faces   = ifs.getFaces();
      int     faces_count = ifs.getFacesCount();
      int     verts_count = ifs.getVertsCount();
      int     edges_count = 0;
      
      // - count number of required edges
      // - check if all faces have the same amount of vertices
      //   e.g. all triangles, quads, or just polygons
      verts_per_face = ifs_faces[0].length;
      for(int i = 0; i < faces_count; i++){
        edges_count += ifs_faces[i].length;
        if(verts_per_face != ifs_faces[i].length) { 
          verts_per_face = -1; 
        }
      }

      // allocate
      edges = new DwHalfEdge.Edge[edges_count];
      faces = new DwHalfEdge.Face[faces_count];
      verts = new DwHalfEdge.Vert[verts_count];
      
      // edgemap, for finding edge-pairs
      HashMap<DwPair<Integer>, DwHalfEdge.Edge> edgemap = new HashMap<DwPair<Integer>, DwHalfEdge.Edge>();
      
      // setup edges/faces
      for(int i = 0, edge_id = 0; i < faces_count; i++){
        int num_edges = ifs_faces[i].length;
        
        // create face-edges
        for(int j = 0; j < num_edges; j++){
          edges[edge_id + j] = new DwHalfEdge.Edge(ifs_faces[i][j]); 
        }
        
        // create links + fill edgemap
        for(int j = 0; j < num_edges; j++){
          int j0 = edge_id + (j+0) % num_edges;
          int j1 = edge_id + (j+1) % num_edges;
          edges[j0].next = edges[j1]; // next-link
          edgemap.put(DwPair.AB(edges[j0]), edges[j0]); // put edge into map
        }
        
        faces[i] = new DwHalfEdge.Face(edges[edge_id]); // face-link
        edge_id += num_edges;
      }

      // setup edge-pairs
      for(DwHalfEdge.Edge edge : edges){
        edge.pair = edgemap.get(DwPair.BA(edge)); // pair-link
        verts[edge.vert] = new DwHalfEdge.Vert(edge);  // vertex-link
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
      } while(iter.pair != null && (iter = iter.pair.next) != edge);
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
        if(edge != null && getFLAG_display(edge)){
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
        if(edge != null && getFLAG_display(edge)){
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
        if(edge != null && getFLAG_display(edge)){
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
