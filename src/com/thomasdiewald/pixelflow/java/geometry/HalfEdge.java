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


public class HalfEdge {

  static public class Edge{
 
    public Edge pair;
    public Edge next;
    public int  face;
    public int  vert;
    
    // can be used for anything. 
    // e.g. bitmask, pointer, boolean etc...
    public int  FLAG;

    public Edge(int face, int vert){
      this.face = face;
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
  
  
  
  
//  static public class Face{
//    public int  FLAG;
//    public Edge edge;
//   
//    public Face(Edge edge){
//      this.edge = edge;
//    }
//  }
//  
//  static public class Vert{
//    public int  FLAG;
//    public Edge edge;
//   
//    public Vert(Edge edge){
//      this.edge = edge;
//    }
//  }
 

}
