package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSetAble;

public class DwIFSGrid implements DwIndexedFaceSetAble{

  public int nx; 
  public int ny;
  
  public int       faces_count;
  public int       verts_count;
  public int  [][] faces;
  public float[][] verts;


  public DwIFSGrid(int nx, int ny){
    create(nx, ny);
  }
  
  public void create(int nx, int ny){
    this.nx = nx;
    this.ny = ny;
    
    int vnx = nx;
    int vny = ny;
    
    int fnx = nx - 1;
    int fny = ny - 1;
    
    float dimx = nx - 1;
    float dimy = ny - 1;
    
    verts_count = vnx * vny;
    faces_count = fnx * fny;
    
    verts = new float[verts_count][3];
    faces = new int  [faces_count][4];
    
    // create vertices
    for(int y = 0; y < vny; y++){
      for(int x = 0; x < vnx; x++){
        
        float px = 2 * x / dimx - 1;
        float py = 2 * y / dimy - 1;
        float pz = 0;
        
        int vid = y * vnx + x;
        verts[vid][0] = px;
        verts[vid][1] = py;
        verts[vid][2] = pz;
      }
    }
    
    
    // create faces, 4 verts, CCW
    for(int y = 0; y < fny; y++){
      for(int x = 0; x < fnx; x++){
        int vid = y * vnx + x;
        int fid = y * fnx + x;
        faces[fid][0] = vid;
        faces[fid][1] = vid + vnx;
        faces[fid][2] = vid + vnx + 1;
        faces[fid][3] = vid + 1;
      }
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
