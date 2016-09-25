package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;
import java.util.HashMap;


public class TetrahedronBuilder{

  private int index;
  public ArrayList<int[]>   faces    = new ArrayList<int[]>();
  public ArrayList<float[]> vertices = new ArrayList<float[]>();
  
  private HashMap<Integer, Integer> vtx_cache = new HashMap<Integer, Integer>();

  // add vertex to mesh, fix position to be on unit sphere, return index
  private int addVertex(float x, float y, float z){
    float dd_sq = x*x + y*y + z*z;
    float dd_inv = 1f / (float) Math.sqrt(dd_sq);
    float[] vtx = {x * dd_inv, y * dd_inv, z * dd_inv};
    vertices.add(vtx);
    return index++;
  }
  
  public void addFace(ArrayList<int[]> faces, int a, int b, int c){
    faces.add(new int[]{a,b,c});
  }


  // return index of point in the middle of p1 and p2
  private int getCenter(int ia, int ib){
    int key = (ia<ib) ? (ib<<16)|ia : (ia<<16)|ib;
    Integer val = vtx_cache.get(key);
    if (val != null) {
      return val.intValue();
    }

    float[] va = vertices.get(ia);
    float[] vb = vertices.get(ib);

    float mx = (va[0] + vb[0]) * 0.5f;
    float my = (va[1] + vb[1]) * 0.5f;
    float mz = (va[2] + vb[2]) * 0.5f;

    int im = addVertex(mx, my, mz); 
    vtx_cache.put(key, im);
    return im;
  }
  
  public void create(int subdivisions){

    index = 0;
    vertices.clear();
    faces.clear();
    vtx_cache.clear();
    
    // https://en.wikipedia.org/wiki/Tetrahedron
    float t = (float) (1f/Math.sqrt(2.0));
    
    addVertex(+1,  0, -t); //  0
    addVertex(-1,  0, -t); //  1
    addVertex( 0, +1,  t); //  2
    addVertex( 0, -1,  t); //  3
                               
    addFace(faces, 0,  1,  2);
    addFace(faces, 0,  2,  3);
    addFace(faces, 1,  2,  3);
    addFace(faces, 0,  1,  3);

   
//    System.out.println("tetrahedron, level: "+0+", "+faces.size()+", "+vertices.size());


    // refine triangles
    for (int i = 0; i < subdivisions; i++){
      ArrayList<int[]> faces_tmp = new ArrayList<int[]>();
   
      
      // subdivide: triangle -> 4 triangles
      for (int[] face : faces){
        
        int iaf = face[0];
        int ibf = face[1];
        int icf = face[2];
        
        int ia = getCenter(iaf, ibf);
        int ib = getCenter(ibf, icf);
        int ic = getCenter(icf, iaf);

        addFace(faces_tmp, iaf, ia, ic);
        addFace(faces_tmp, ibf, ib, ia);
        addFace(faces_tmp, icf, ic, ib);
        addFace(faces_tmp, ia , ib, ic);
      }
      
      faces = faces_tmp;
//      System.out.println("tetrahedron, level: "+i+", "+faces.size()+", "+vertices.size());
    }
  }
}