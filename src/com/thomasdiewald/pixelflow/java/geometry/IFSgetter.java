package com.thomasdiewald.pixelflow.java.geometry;


/**
 * Indexed Face Set, Getter methods
 */
public interface IFSgetter{
  
  int       getVertsCount();
  int       getFacesCount();
  float[][] getVerts();
  int  [][] getFaces(); 
  
//  TODO:
//  float[][] getNormals();
//  float[][] getTextcoords();
}