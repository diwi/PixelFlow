/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.geometry;

import processing.core.PConstants;
import processing.core.PShape;

/**
 * @author Thomas
 *
 */
public class DwMeshUtils {
  
  
  static public void createPolyhedronShape(PShape shape, DwIndexedFaceSetAble ifs, float scale, int verts_per_face, boolean smooth){
    
    int type = -1;
    
    switch(verts_per_face){
      case 3: type = PConstants.TRIANGLES; break;
      case 4: type = PConstants.QUADS; break;
      default: return;
    }
   
    shape.setStroke(false);
    shape.beginShape(type);
//    shape.noStroke();
    
    int  [][] faces = ifs.getFaces();
    float[][] verts = ifs.getVerts();
    
    for(int[] face : faces){
      float nx = 0, ny = 0, nz = 0;

      int num_verts = face.length;
      
      // compute face normal
      if(!smooth){
        for(int i = 0; i < num_verts; i++){
          int vi = face[i];
          nx += verts[vi][0];
          ny += verts[vi][1];
          nz += verts[vi][2];
        }
        nx /= num_verts;
        ny /= num_verts;
        nz /= num_verts;
        shape.normal(nx, ny, nz); 
      }
      
      for(int i = 0; i < num_verts; i++){
//      for(int i = num_verts-1; i >= 0; i--){
        float[] v = verts[face[i]];
        if(smooth){
          shape.normal(v[0], v[1], v[2]); 
        }
        shape.vertex(v[0]*scale, v[1]*scale, v[2]*scale);
      }
    }
    shape.endShape();
  }
  
  
}
