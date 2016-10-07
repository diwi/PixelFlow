package com.thomasdiewald.pixelflow.java.geometry;

import java.util.Arrays;

public class DwMeshCleaner{

  DwIndexedFaceSetAble ifs;
  
  // used for merging/welding/remapping
  private DwVertexIndexWrapper[] verts_orig;
  private DwVertexIndexWrapper[] verts_work;
  
  
  public DwMeshCleaner(DwIndexedFaceSetAble ifs){
    this.ifs = ifs;
  }


  public void initVertices(){
    int verts_count = ifs.getVertsCount();
    if(verts_work == null || verts_work.length != verts_count){
      verts_work = new DwVertexIndexWrapper[verts_count];
      verts_orig = new DwVertexIndexWrapper[verts_count];
      for(int i = 0; i < verts_count; i++){
        verts_work[i] = verts_orig[i] = new DwVertexIndexWrapper(ifs, i);
      }
    }
  }
  

  public void mergeVerts(float merge_distance, int max_iterations){
    
    initVertices();
    
    int verts_count = ifs.getVertsCount();
    float dd_merge_sq = merge_distance*merge_distance;

    for(int k = 0; k < max_iterations; k++){
      // sort along x-axis
      Arrays.sort(verts_work);
      
      // reset shift-dir to zero
      for(int i = 0; i < verts_count; i++) verts_work[i].resetShift();
      
      // compute position-shift
      for(int i = 0; i < verts_count; i++){
        boolean search_lo = true; for(int j = i-1; search_lo && j >=          0; j--) search_lo = verts_work[i].merge(verts_work[j], dd_merge_sq);
        boolean search_hi = true; for(int j = i+1; search_hi && j < verts_count; j++) search_hi = verts_work[i].merge(verts_work[j], dd_merge_sq);
      }
      
      // update position (apply shift)
      float shift_new = 0;
      float shift_max = 0;
      float shift_sum = 0;
      for(int i = 0; i < verts_count; i++){
        shift_new  = verts_work[i].applyShift();
        shift_max  = Math.max(shift_max, shift_new);
        shift_sum += shift_new;
      }
      
      // if shift is very low, we are done
      if(shift_sum < 0.0001f){
        break;
      }
    }
 
  }
  
  public void weldVerts(float weld_distance){
    
    initVertices();
    
    int verts_count = ifs.getVertsCount();
    float dd_weld_sq = weld_distance*weld_distance;
    
    // sort along x-axis
    Arrays.sort(verts_work);

    // weld
    // vertices with the "same" position, get the same vertex id
    // during comparison, the one with the lower index dominates the other one
    for(int i = 0; i < verts_count; i++){
      boolean search_lo = true; for(int j = i-1; search_lo && j >=          0; j--) search_lo = verts_work[i].weld(verts_work[j], dd_weld_sq);
      boolean search_hi = true; for(int j = i+1; search_hi && j < verts_count; j++) search_hi = verts_work[i].weld(verts_work[j], dd_weld_sq);
    }
    
    // update face-vertex-indices
    int     faces_count = ifs.getFacesCount();
    int[][] faces       = ifs.getFaces();
    for(int i = 0; i < faces_count; i++){
      int[] face = faces[i];
      for(int j = 0; j < face.length; j++){
        int vert_id_old = face[j];
        face[j] = verts_orig[vert_id_old].vert;
      }
    }
  }
  
  
  public void remapVerts(){
    // only vertices, referenced by faces remain in the list
    // other, unreferenced, vertices are removed
    int       verts_count = ifs.getVertsCount();
    float[][] verts       = ifs.getVerts();
    int  []   verts_map   = new int  [verts_count];
    float[][] verts_new   = new float[verts_count][];
    
    for(int i = 0; i < verts_count; i++) verts_map[i] = -1;
    
    int vert_id_cur = 0;
    int     faces_count = ifs.getFacesCount();
    int[][] faces       = ifs.getFaces();
    for(int i = 0; i < faces_count; i++){
      int[] face = faces[i];
      for(int j = 0; j < face.length; j++){
        int vert_id_old = face[j];
        
        if(verts_map[vert_id_old] == -1){
          verts_new[vert_id_cur] = verts[vert_id_old];
          verts_map[vert_id_old] = vert_id_cur++;
        } 
        
        face[j] = verts_map[vert_id_old];
      }
    }
    
    verts = Arrays.copyOf(verts_new, vert_id_cur);
    ifs.setVerts(verts);
  }
  

  public DwMeshCleaner removeDuplicateVerts(float threshold){
    mergeVerts(threshold, 100);
    weldVerts(threshold);
    remapVerts();
    return this;
  }





}