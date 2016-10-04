package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;

import processing.core.PConstants;
import processing.core.PGraphics;

public class DwIndexedFaceSet implements DwIndexedFaceSetAble{

  public float[][] verts;
  public int  [][] faces;

  public DwIndexedFaceSet(){ 
  }
  
  public DwIndexedFaceSet(ArrayList<float[]> verts_list, ArrayList<int[]> faces_list){
    setVerts(verts_list);
    setFaces(faces_list);
  }
  
  

  public void display(PGraphics pg){
    for(int i = 0; i < faces.length; i++){
      int[] face = faces[i];

      pg.beginShape();
      for(int j = 0; j < face.length; j++){
        float[] v = verts[face[j]];
         pg.vertex(v[0], v[1], v[2]);
      }
      pg.endShape(PConstants.CLOSE);
    }
  }
  
  public void display2D(PGraphics pg){
    for(int i = 0; i < faces.length; i++){
      int[] face = faces[i];

      pg.beginShape();
      for(int j = 0; j < face.length; j++){
        float[] v = verts[face[j]];
         pg.vertex(v[0], v[1]);
      }
      pg.endShape(PConstants.CLOSE);
    }
  }
  
  public void display(PGraphics pg, int filter_num_verts){
    float[] v;
    if(filter_num_verts == 3){
      pg.beginShape(PConstants.TRIANGLES);
      for(int i = 0; i < faces.length; i++){
        int[] face = faces[i];
        if(face.length != 3) continue;
        v = verts[face[0]];  pg.vertex(v[0], v[1], v[2]);
        v = verts[face[1]];  pg.vertex(v[0], v[1], v[2]);
        v = verts[face[2]];  pg.vertex(v[0], v[1], v[2]);
      }
      pg.endShape(PConstants.CLOSE);
    }
    
    if(filter_num_verts == 4){
      pg.beginShape(PConstants.QUADS);
      for(int i = 0; i < faces.length; i++){
        int[] face = faces[i];
        if(face.length != 4) continue;
        v = verts[face[0]];  pg.vertex(v[0], v[1], v[2]);
        v = verts[face[1]];  pg.vertex(v[0], v[1], v[2]);
        v = verts[face[2]];  pg.vertex(v[0], v[1], v[2]);
        v = verts[face[3]];  pg.vertex(v[0], v[1], v[2]);
      }
      pg.endShape(PConstants.CLOSE);
    }
  }
  
  
  public void display2D(PGraphics pg, int filter_num_verts){
    float[] v;
    if(filter_num_verts == 3){
      pg.beginShape(PConstants.TRIANGLES);
      for(int i = 0; i < faces.length; i++){
        int[] face = faces[i];
        if(face.length != 3) continue;
        v = verts[face[0]];  pg.vertex(v[0], v[1]);
        v = verts[face[1]];  pg.vertex(v[0], v[1]);
        v = verts[face[2]];  pg.vertex(v[0], v[1]);
      }
      pg.endShape(PConstants.CLOSE);
    }
    
    if(filter_num_verts == 4){
      pg.beginShape(PConstants.QUADS);
      for(int i = 0; i < faces.length; i++){
        int[] face = faces[i];
        if(face.length != 4) continue;
        v = verts[face[0]];  pg.vertex(v[0], v[1]);
        v = verts[face[1]];  pg.vertex(v[0], v[1]);
        v = verts[face[2]];  pg.vertex(v[0], v[1]);
        v = verts[face[3]];  pg.vertex(v[0], v[1]);
      }
      pg.endShape(PConstants.CLOSE);
    }
  }
  

  
  
  
  
  
  
  
  
  
  public void addFaces(ArrayList<int[]> faces_list_new){
    int num_faces_new = faces_list_new.size();
    int num_faces_old = faces.length;
   
    int[][] faces_new = faces_list_new.toArray(new int[num_faces_new][]);
    int[][] faces_old = faces;
    
    faces = new int[num_faces_old + num_faces_new][];
    System.arraycopy(faces_old, 0, faces,             0, num_faces_old);
    System.arraycopy(faces_new, 0, faces, num_faces_old, num_faces_new);
  }
  
  

  @Override
  public int getVertsCount() {
    return verts.length;
  }

  @Override
  public int getFacesCount() {
    return faces.length;
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
    this.verts = verts;
  }

  @Override
  public void setFaces(int[][] faces) {
    this.faces = faces;
  }

  @Override
  public void setVerts(ArrayList<float[]> verts_list) {
    this.verts = verts_list.toArray(new float[verts_list.size()][]);
  }

  @Override
  public void setFaces(ArrayList<int[]> faces_list) {
    this.faces = faces_list.toArray(new int  [faces_list.size()][]);
  }
  

}