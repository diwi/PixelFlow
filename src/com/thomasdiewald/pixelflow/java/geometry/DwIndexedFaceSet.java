package com.thomasdiewald.pixelflow.java.geometry;

import java.util.ArrayList;

import processing.core.PConstants;
import processing.core.PGraphics;

public class DwIndexedFaceSet implements DwIndexedFaceSetAble{
  public float[][] verts;
  public int  [][] faces;

  public DwIndexedFaceSet(){ 
  }
  
  public DwIndexedFaceSet(int verts_count, int faces_count){
    this.verts = new float[verts_count][];
    this.faces = new int  [faces_count][];
  }
  
  public DwIndexedFaceSet(float[][] verts, int[][] faces){
    this.verts = verts;
    this.faces = faces;
  }
  
  public DwIndexedFaceSet(ArrayList<float[]> verts_list, ArrayList<int[]> faces_list){
    setVerts(verts_list);
    setFaces(faces_list);
  }
  
  
  public DwIndexedFaceSet translate(float tx, float ty, float tz){
    for(int i = 0; i < verts.length; i++){
      verts[i][0] += tx;
      verts[i][1] += ty;
      verts[i][2] += tz;
    }
    return this;
  }
  
  public DwIndexedFaceSet scale(float sx, float sy, float sz){
    for(int i = 0; i < verts.length; i++){
      verts[i][0] *= sx;
      verts[i][1] *= sy;
      verts[i][2] *= sz;
    }
    return this;
  }
  
  public DwIndexedFaceSet scale(float xyz){
    for(int i = 0; i < verts.length; i++){
      verts[i][0] *= xyz;
      verts[i][1] *= xyz;
      verts[i][2] *= xyz;
    }
    return this;
  }
  
  public float[] computeBounds(){
    float[] bounds = {  +Float.MAX_VALUE, +Float.MAX_VALUE, +Float.MAX_VALUE, 
                        -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };

    for(int i = 0; i < verts.length; i++){
      float[] v = verts[i];
      if(bounds[0] > v[0]) bounds[0] = v[0];
      if(bounds[1] > v[1]) bounds[1] = v[1];
      if(bounds[2] > v[2]) bounds[2] = v[2];
      if(bounds[3] < v[0]) bounds[3] = v[0];
      if(bounds[4] < v[1]) bounds[4] = v[1];
      if(bounds[5] < v[2]) bounds[5] = v[2];
    }
    return bounds;
  }
  
  
  public void alignMin(float minx, float miny, float minz){
    float[] bounds = computeBounds();
    minx -= bounds[0];
    miny -= bounds[1];
    minz -= bounds[2];
    translate(minx, miny, minz);
  }
  
  public void fitBounds(float[] dst_bounds){
    float[] src_bounds = computeBounds();
    
    float src_sx = src_bounds[3] - src_bounds[0];
    float src_sy = src_bounds[4] - src_bounds[1];
    float src_sz = src_bounds[5] - src_bounds[2];
    
    float src_cx = src_bounds[0] + src_sx * 0.5f;
    float src_cy = src_bounds[1] + src_sy * 0.5f;
    float src_cz = src_bounds[2] + src_sz * 0.5f;
    
    float dst_sx = dst_bounds[3] - dst_bounds[0];
    float dst_sy = dst_bounds[4] - dst_bounds[1];
    float dst_sz = dst_bounds[5] - dst_bounds[2];
    
    float dst_cx = dst_bounds[0] + dst_sx * 0.5f;
    float dst_cy = dst_bounds[1] + dst_sy * 0.5f;
    float dst_cz = dst_bounds[2] + dst_sz * 0.5f;
    
    float sx = dst_sx / src_sx; if(Float.isNaN(sx)) sx = 0;
    float sy = dst_sy / src_sy; if(Float.isNaN(sy)) sy = 0;
    float sz = dst_sz / src_sz; if(Float.isNaN(sz)) sz = 0;
    
    float sxyz = Math.max(Math.max(sx, sy), sz);

    if(sx != 0) sxyz = Math.min(sxyz, sx);
    if(sy != 0) sxyz = Math.min(sxyz, sy);
    if(sz != 0) sxyz = Math.min(sxyz, sz);
    
    for(int i = 0; i < verts.length; i++){
      float[] v = verts[i];
      v[0] = (v[0] - src_cx) * sxyz + dst_cx;
      v[1] = (v[1] - src_cy) * sxyz + dst_cy;
      v[2] = (v[2] - src_cz) * sxyz + dst_cz;
    }

  }
  
  public void fitSize(float fit_xyz){
    fitSize(fit_xyz, fit_xyz, fit_xyz);
  }
  
  public void fitSize(float fit_x, float fit_y, float fit_z){
    float[] bounds = computeBounds();

    float src_sx = bounds[3] - bounds[0];
    float src_sy = bounds[4] - bounds[1];
    float src_sz = bounds[5] - bounds[2];
    
    float sx = fit_x / src_sx;  if(Float.isNaN(sx)) sx = 0;
    float sy = fit_y / src_sy;  if(Float.isNaN(sy)) sy = 0;
    float sz = fit_z / src_sz;  if(Float.isNaN(sz)) sz = 0;

    float sxyz = Math.max(Math.max(sx, sy), sz);
    if(sx != 0) sxyz = Math.min(sxyz, sx);
    if(sy != 0) sxyz = Math.min(sxyz, sy);
    if(sz != 0) sxyz = Math.min(sxyz, sz);
    scale(sxyz);

  }
  
  
  public void display(PGraphics pg){
    for(int i = 0; i < faces.length; i++){
      int[] face = faces[i];
      pg.beginShape();
      for(int j = 0; j < face.length; j++){
        vertex(pg, verts[face[j]]);
      }
      pg.endShape(PConstants.CLOSE);
    }
  }
  
  public void display(PGraphics pg, int filter_num_verts){
    if(filter_num_verts == 3){
      pg.beginShape(PConstants.TRIANGLES);
      for(int i = 0; i < faces.length; i++){
        int[] face = faces[i];
        if(face.length == 3){
          vertex(pg, verts[face[0]]);
          vertex(pg, verts[face[1]]);
          vertex(pg, verts[face[2]]);
        }
      }
      pg.endShape(PConstants.CLOSE);
    }
    
    if(filter_num_verts == 4){
      pg.beginShape(PConstants.QUADS);
      for(int i = 0; i < faces.length; i++){
        int[] face = faces[i];
        if(face.length == 4){
          vertex(pg, verts[face[0]]);
          vertex(pg, verts[face[1]]);
          vertex(pg, verts[face[2]]);
          vertex(pg, verts[face[3]]);
        }
      }
      pg.endShape(PConstants.CLOSE);
    }
  }
  
  public void vertex(PGraphics pg, float[] v){
    if(pg.is2D()){
      pg.vertex(v[0], v[1]);
    } else {
      pg.vertex(v[0], v[1], v[2]);
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