package com.thomasdiewald.pixelflow.java.geometry;

public class DwVertexIndexWrapper implements Comparable<DwVertexIndexWrapper>{
  
  final DwIndexedFaceSetAble ifs;
  public int vert;

  public DwVertexIndexWrapper(DwIndexedFaceSetAble ifs, int vert){
    this.vert = vert;
    this.ifs = ifs;
  }

  private int     shift_counts = 0;
  private float[] shift = new float[3];
  public void resetShift(){
    shift[0] = shift[1] = shift[2] = 0;
    shift_counts = 0;
  }

  public float applyShift(){
    if(shift_counts > 0){
      float[] this_vert = ifs.getVerts()[this.vert];
      float norm = 1f/shift_counts;
      this_vert[0] += shift[0] * norm;
      this_vert[1] += shift[1] * norm;
      this_vert[2] += shift[2] * norm;
    }
    float shift_mag = shift[0]*shift[0] + shift[1]*shift[1] + shift[2]*shift[2];

    resetShift();
    return shift_mag;
  }


  public boolean merge(DwVertexIndexWrapper othr, float dd_merge_sq) {
    if(othr == this) return true;

    float[][] verts = ifs.getVerts();
    float[] this_vert = verts[this.vert];
    float[] othr_vert = verts[othr.vert];

    float dx = othr_vert[0] - this_vert[0]; 
    if(dx*dx > dd_merge_sq) return false; // sorted in x
    float dy = othr_vert[1] - this_vert[1]; 
    float dz = othr_vert[2] - this_vert[2];
    float dd_sq = dx*dx + dy*dy + dz*dz;
    if(dd_sq <= dd_merge_sq){
      float shift_mag = 0.5f;
      shift[0] += dx * shift_mag;
      shift[1] += dy * shift_mag;
      shift[2] += dz * shift_mag;
      shift_counts++;
    }
    return true;
  }



  public boolean weld(DwVertexIndexWrapper othr, float dd_weld_sq) {
    if(othr == this) return true;

    float[][] verts = ifs.getVerts();
    float[] this_vert = verts[this.vert];
    float[] othr_vert = verts[othr.vert];

    float dx = othr_vert[0] - this_vert[0]; 
    if(dx*dx > dd_weld_sq) return false; // sorted in x
    float dy = othr_vert[1] - this_vert[1]; 
    float dz = othr_vert[2] - this_vert[2];
    float dd_sq = dx*dx + dy*dy + dz*dz;
    if(dd_sq <= dd_weld_sq){
      if(this.vert > othr.vert){
        this.vert = othr.vert;
      }
    }
    return true;
  }


  @Override
  // sort along x-axis
  public int compareTo(DwVertexIndexWrapper othr) {
    float[][] verts = ifs.getVerts();
    final float this_vert = verts[this.vert][0];
    final float othr_vert = verts[othr.vert][0];
    if(this_vert > othr_vert) return +1;
    if(this_vert < othr_vert) return -1;
    return 0;
  }


}