package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;
import java.util.HashMap;


public class IcosahedronBuilder{

  private int index;
  public ArrayList<int[]>   faces = new ArrayList<int[]>();
  public ArrayList<float[]> verts = new ArrayList<float[]>();
  
  private HashMap<Integer, Integer> vtx_cache = new HashMap<Integer, Integer>();

  // add vertex to mesh, fix position to be on unit sphere, return index
  private int addVertex(float x, float y, float z){
    float dd_sq = x*x + y*y + z*z;
    float dd_inv = 1f / (float) Math.sqrt(dd_sq);
    float[] vtx = {x * dd_inv, y * dd_inv, z * dd_inv};
    verts.add(vtx);
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

    float[] va = verts.get(ia);
    float[] vb = verts.get(ib);

    float mx = (va[0] + vb[0]) * 0.5f;
    float my = (va[1] + vb[1]) * 0.5f;
    float mz = (va[2] + vb[2]) * 0.5f;

    int im = addVertex(mx, my, mz); 
    vtx_cache.put(key, im);
    return im;
  }
  
  public void create(int subdivisions){

    index = 0;
    verts.clear();
    faces.clear();
    vtx_cache.clear();
    
    // https://en.wikipedia.org/wiki/Regular_icosahedron
    
    // golden ratio: 1.618034
    float t = (float) (1.0f + Math.sqrt(5.0)) / 2.0f;

    
    // XY plane
    addVertex(-1,  t,  0); //  0
    addVertex( 1,  t,  0); //  1
    addVertex(-1, -t,  0); //  2
    addVertex( 1, -t,  0); //  3
                               
    // YZ plane                
    addVertex( 0, -1,  t); //  4
    addVertex( 0,  1,  t); //  5
    addVertex( 0, -1, -t); //  6
    addVertex( 0,  1, -t); //  7
                               
    // ZX plane                
    addVertex( t,  0, -1); //  8
    addVertex( t,  0,  1); //  9
    addVertex(-t,  0, -1); // 10
    addVertex(-t,  0,  1); // 11


    // http://blog.andreaskahler.com/2009/06/creating-icosphere-mesh-in-code.html
    // 5 faces around point 0
    addFace(faces, 0, 11,  5);
    addFace(faces, 0,  5,  1);
    addFace(faces, 0,  1,  7);
    addFace(faces, 0,  7, 10);
    addFace(faces, 0, 10, 11);

    // 5 adjacent faces 
    addFace(faces,  1,  5, 9);
    addFace(faces,  5, 11, 4);
    addFace(faces, 11, 10, 2);
    addFace(faces, 10,  7, 6);
    addFace(faces,  7,  1, 8);

    // 5 faces around point 3
    addFace(faces, 3, 9, 4);
    addFace(faces, 3, 4, 2);
    addFace(faces, 3, 2, 6);
    addFace(faces, 3, 6, 8);
    addFace(faces, 3, 8, 9);

    // 5 adjacent faces 
    addFace(faces, 4, 9,  5);
    addFace(faces, 2, 4, 11);
    addFace(faces, 6, 2, 10);
    addFace(faces, 8, 6,  7);
    addFace(faces, 9, 8,  1);
    
    // refine triangles
    for (int i = 0; i < subdivisions; i++){
      ArrayList<int[]> faces_tmp = new ArrayList<int[]>();
   
      // subdivide: triangle -> 4 triangles
      //
      //           ia
      //           / \
      //          /   \
      //         /     \
      //     ic /_______\ iab
      //       / \      /\ 
      //      /   \    /  \
      //     /     \  /    \
      //    /_______\/______\
      //  ic        ibc       ib
      //
      for (int[] face : faces){  
        int ia = face[0];
        int ib = face[1];
        int ic = face[2];
        
        int iab = getCenter(ia, ib);
        int ibc = getCenter(ib, ic);
        int ica = getCenter(ic, ia);

        addFace(faces_tmp, ia , iab, ica);
        addFace(faces_tmp, ib , ibc, iab);
        addFace(faces_tmp, ic , ica, ibc);
        addFace(faces_tmp, iab, ibc, ica);
      }

      faces = faces_tmp;
    }
  }
}