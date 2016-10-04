package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;
import java.util.HashMap;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwPair;

public class DwIcosahedron implements DwIndexedFaceSetAble{
  // https://en.wikipedia.org/wiki/Regular_icosahedron
  // golden ratio: 1.618034
  static public final double GOLDEN_RATIO = (1.0 + Math.sqrt(5.0)) / 2.0;

  public int       faces_count;
  public int       verts_count;
  public int  [][] faces;
  public float[][] verts;
  private int      verts_idx;
  private HashMap<DwPair<Integer>, Integer> verts_cache = new HashMap<DwPair<Integer>, Integer>();

  
  public DwIcosahedron(int subdivisions){
    create(subdivisions);
  }
  
  
  // Euler formula for closed, 2-manifold meshes:  
  // V - E + F = 2*(1-g)
  // V ... number of vertices
  // E ... number of edges
  // F ... number of faces
  // g ... genus (=number of "holes"/"handles", sphere.genus: 0, torus.genus: 1)
  
  public int getNumFaces(int subdivisions){
    int F = 20 * (int) Math.pow(4, subdivisions);
    return F;
  }
  
  public int getNumVerts(int subdivisions){
    int F = getNumFaces(subdivisions);
    int E = 3 * F / 2; // isocahedron
    int V = 2 + E - F;
    return V;
  }
  
  private int addVertex(float[][] verts, int verts_idx, float x, float y, float z){
    float dd_sq = x*x + y*y + z*z;
    float dd_inv = 1f / (float) Math.sqrt(dd_sq);
    verts[verts_idx][0] = x * dd_inv;
    verts[verts_idx][1] = y * dd_inv;
    verts[verts_idx][2] = z * dd_inv;
    return ++verts_idx;
  }
  
  private int addFace(int[][] faces, int face_idx, int a, int b, int c){
    faces[face_idx][0] = a;
    faces[face_idx][1] = b;
    faces[face_idx][2] = c;
    return ++face_idx;
  }

  private int getCenter(int ia, int ib){
    if(ib<ia){int it=ia;ia=ib;ib=it;}
    DwPair<Integer> key = new DwPair<Integer>(ia, ib);
    Integer         val = verts_cache.get(key);
    if (val == null) {
      float mx = (verts[ia][0] + verts[ib][0]) * 0.5f;
      float my = (verts[ia][1] + verts[ib][1]) * 0.5f;
      float mz = (verts[ia][2] + verts[ib][2]) * 0.5f;
      verts_cache.put(key, (val = verts_idx));
      verts_idx = addVertex(verts, verts_idx, mx, my, mz);
    }
    return val;
  }

  public void create(int subdivisions){
    
    // 1) create initial vertex set
    verts_count = getNumVerts(subdivisions);
    verts       = new float[verts_count][3];
    verts_idx   = 0;
    verts_cache.clear();
    
    final float t = (float) GOLDEN_RATIO;

    // XY plane
    verts_idx = addVertex(verts, verts_idx, -1, t, 0); //  0
    verts_idx = addVertex(verts, verts_idx,  1, t, 0); //  1
    verts_idx = addVertex(verts, verts_idx, -1,-t, 0); //  2
    verts_idx = addVertex(verts, verts_idx,  1,-t, 0); //  3       
    
    // YZ plane                
    verts_idx = addVertex(verts, verts_idx,  0,-1, t); //  4
    verts_idx = addVertex(verts, verts_idx,  0, 1, t); //  5
    verts_idx = addVertex(verts, verts_idx,  0,-1,-t); //  6
    verts_idx = addVertex(verts, verts_idx,  0, 1,-t); //  7
                               
    // ZX plane                
    verts_idx = addVertex(verts, verts_idx,  t, 0,-1); //  8
    verts_idx = addVertex(verts, verts_idx,  t, 0, 1); //  9
    verts_idx = addVertex(verts, verts_idx, -t, 0,-1); // 10
    verts_idx = addVertex(verts, verts_idx, -t, 0, 1); // 11

    
    // 2) create initial face set
    int curr_subdivision = 0; 
    
    faces_count = getNumFaces(curr_subdivision);
    faces       = new int[faces_count][3];

    int face_idx = 0;
    // http://blog.andreaskahler.com/2009/06/creating-icosphere-mesh-in-code.html
    // 5 faces around point 0
    face_idx = addFace(faces, face_idx, 0, 11,  5);
    face_idx = addFace(faces, face_idx, 0,  5,  1);
    face_idx = addFace(faces, face_idx, 0,  1,  7);
    face_idx = addFace(faces, face_idx, 0,  7, 10);
    face_idx = addFace(faces, face_idx, 0, 10, 11);

    // 5 adjacent faces 
    face_idx = addFace(faces, face_idx,  1,  5, 9);
    face_idx = addFace(faces, face_idx,  5, 11, 4);
    face_idx = addFace(faces, face_idx, 11, 10, 2);
    face_idx = addFace(faces, face_idx, 10,  7, 6);
    face_idx = addFace(faces, face_idx,  7,  1, 8);

    // 5 faces around point 3
    face_idx = addFace(faces, face_idx, 3, 9, 4);
    face_idx = addFace(faces, face_idx, 3, 4, 2);
    face_idx = addFace(faces, face_idx, 3, 2, 6);
    face_idx = addFace(faces, face_idx, 3, 6, 8);
    face_idx = addFace(faces, face_idx, 3, 8, 9);

    // 5 adjacent faces 
    face_idx = addFace(faces, face_idx, 4, 9,  5);
    face_idx = addFace(faces, face_idx, 2, 4, 11);
    face_idx = addFace(faces, face_idx, 6, 2, 10);
    face_idx = addFace(faces, face_idx, 8, 6,  7);
    face_idx = addFace(faces, face_idx, 9, 8,  1);
    

    // 3) iterative subdivision: triangle -> 4 triangles
    //
    //       a
    //       /\
    //   ca /__\ ab
    //     /\  /\ 
    //    /__\/__\
    //   c   bc   b
    //
    while(++curr_subdivision <= subdivisions){
      
      int     faces_new_count = getNumFaces(curr_subdivision);
      int[][] faces_new       = new int[faces_new_count][3];
      
      for(int j = 0, face_new_idx = 0; j < faces_count; j++){
        int ia = faces[j][0];
        int ib = faces[j][1];
        int ic = faces[j][2];
        
        int iab = getCenter(ia, ib);
        int ibc = getCenter(ib, ic);
        int ica = getCenter(ic, ia);

        face_new_idx = addFace(faces_new, face_new_idx, ia , iab, ica);
        face_new_idx = addFace(faces_new, face_new_idx, ib , ibc, iab);
        face_new_idx = addFace(faces_new, face_new_idx, ic , ica, ibc);
        face_new_idx = addFace(faces_new, face_new_idx, iab, ibc, ica);
      }
      
      faces       = faces_new;
      faces_count = faces_new_count;
    }

  }


  @Override
  public int getVertsCount() {
    return verts_count;
  }
  
  @Override
  public int getFacesCount() {
    return faces_count;
  }

  @Override
  public float[][] getVerts() {
    return verts;
  }

  @Override
  public int[][] getFaces() {
    return faces;
  }

  @Override
  public void setVerts(float[][] verts) {
    System.out.println("unimplemented");
  }

  @Override
  public void setFaces(int[][] faces) {
    System.out.println("unimplemented");
  }
  
  @Override
  public void setVerts(ArrayList<float[]> verts_list) {
    System.out.println("unimplemented");
  }

  @Override
  public void setFaces(ArrayList<int[]> faces_list) {
    System.out.println("unimplemented");
  }
  
}


