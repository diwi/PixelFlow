package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;

/**
 * Indexed Face Set, Getter methods
 */
public interface DwIndexedFaceSetAble{
  
  int       getVertsCount();
  int       getFacesCount();
  float[][] getVerts();
  int  [][] getFaces(); 
  
  void setVerts(float[][] verts);
  void setFaces(int  [][] faces);
  
  void setVerts(ArrayList<float[]> verts_list);
  void setFaces(ArrayList<int  []> faces_list); 
  
//  TODO:
//  float[][] getNormals();
//  float[][] getTextcoords();
}