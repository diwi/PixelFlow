package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;
import java.util.HashMap;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwPair;



public class DwCube implements DwIndexedFaceSetAble{
  
  public int       faces_count;
  public int       verts_count;
  public int  [][] faces;
  public float[][] verts;
  private int      verts_idx;
  private HashMap<DwPair<Integer>, Integer> verts_cache = new HashMap<DwPair<Integer>, Integer>();

  
  public DwCube(int subdivisions){
    create(subdivisions);
  }
  
  
  // Euler formula for closed, 2-manifold meshes:  
  // V - E + F = 2*(1-g)
  // V ... number of vertices
  // E ... number of edges
  // F ... number of faces
  // g ... genus (=number of "holes"/"handles", sphere.genus: 0, torus.genus: 1)
  
  public int getNumFaces(int subdivisions){
    int F = 6 * (int) Math.pow(4, subdivisions);
    return F;
  }
  
  public int getNumVerts(int subdivisions){
    int F = getNumFaces(subdivisions);
    int E = 4 * F / 2; // cube
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
  
  private int addFace(int[][] faces, int face_idx, int a, int b, int c, int d){
    faces[face_idx][0] = a;
    faces[face_idx][1] = b;
    faces[face_idx][2] = c;
    faces[face_idx][3] = d;
    return ++face_idx;
  }

  private int getCenter(int ia, int ib, int ic, int id){
    float mx = (verts[ia][0] + verts[ib][0] + verts[ic][0] + verts[id][0]) * 0.25f;
    float my = (verts[ia][1] + verts[ib][1] + verts[ic][1] + verts[id][1]) * 0.25f;
    float mz = (verts[ia][2] + verts[ib][2] + verts[ic][2] + verts[id][2]) * 0.25f;
    verts_idx = addVertex(verts, verts_idx, mx, my, mz);
    return verts_idx-1;
  }
  
  private int getCenter(int ia, int ib){
    if(ib<ia){int it=ia;ia=ib;ib=it;}
    DwPair<Integer> key = new DwPair<Integer>(ia, ib);
    Integer     val = verts_cache.get(key);
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
    
    float t = (float) Math.sqrt(3f);

    // XY top plane
    verts_idx = addVertex(verts, verts_idx, -t, -t, -t); //  0
    verts_idx = addVertex(verts, verts_idx, -t, +t, -t); //  1
    verts_idx = addVertex(verts, verts_idx, +t, +t, -t); //  2
    verts_idx = addVertex(verts, verts_idx, +t, -t, -t); //  3       
    
    // XY bot plane              
    verts_idx = addVertex(verts, verts_idx,  -t, -t, +t); //  4
    verts_idx = addVertex(verts, verts_idx,  -t, +t, +t); //  5
    verts_idx = addVertex(verts, verts_idx,  +t, +t, +t); //  6
    verts_idx = addVertex(verts, verts_idx,  +t, -t, +t); //  7
                               

    // 2) create initial face set
    int curr_subdivision = 0; 
    
    faces_count = getNumFaces(curr_subdivision);
    faces       = new int[faces_count][4];

    int face_idx = 0;

    face_idx = addFace(faces, face_idx, 3, 2, 1, 0); // XYn
    face_idx = addFace(faces, face_idx, 4, 5, 6, 7); // XYp
    face_idx = addFace(faces, face_idx, 4, 7, 3, 0); // YZn
    face_idx = addFace(faces, face_idx, 6, 5, 1, 2); // YZp
    face_idx = addFace(faces, face_idx, 5, 4, 0, 1); // XZn
    face_idx = addFace(faces, face_idx, 7, 6, 2, 3); // XZp

    // 3) iterative subdivision: quads -> 4 quads
    //
    //       a------ab-----b
    //       |      |      |
    //       |      |      |   
    //      da------e------bc
    //       |      |      |
    //       |      |      |
    //       d------cd-----c
    //
    while(++curr_subdivision <= subdivisions){
      
      int     faces_new_count = getNumFaces(curr_subdivision);
      int[][] faces_new       = new int[faces_new_count][4];
      
      for(int j = 0, face_new_idx = 0; j < faces_count; j++){
        int ia = faces[j][0];
        int ib = faces[j][1];
        int ic = faces[j][2];
        int id = faces[j][3];
        int ie = getCenter(ia, ib, ic, id);
        
        int iab = getCenter(ia, ib);
        int ibc = getCenter(ib, ic);
        int icd = getCenter(ic, id);
        int ida = getCenter(id, ia);

        face_new_idx = addFace(faces_new, face_new_idx, ia, iab, ie, ida);
        face_new_idx = addFace(faces_new, face_new_idx, ib, ibc, ie, iab);
        face_new_idx = addFace(faces_new, face_new_idx, ic, icd, ie, ibc);
        face_new_idx = addFace(faces_new, face_new_idx, id, ida, ie, icd);
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


