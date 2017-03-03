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
//        if(type == PConstants.QUADS){
          shape.normal(-nx, -ny, -nz);  // TODO: processing bug i guess
//        } else {
          shape.normal(nx, ny, nz); 
//        }
      }
      
      for(int i = 0; i < num_verts; i++){
//      for(int i = num_verts-1; i >= 0; i--){
        float[] v = verts[face[i]];
        if(smooth){
//          if(type == PConstants.QUADS){
//            shape.normal(-v[0], -v[1], -v[2]);  // TODO: processing bug i guess
//          } else {
            shape.normal(v[0], v[1], v[2]); 
//          }
        }
        shape.vertex(v[0]*scale, v[1]*scale, v[2]*scale);
      }
    }
    shape.endShape();
  }
  
  
  
  
  static public void createPolyhedronShapeNormals(PShape shape, DwIndexedFaceSetAble ifs, float scale, float normal_len){

    shape.beginShape(PConstants.LINES);
    shape.stroke(0);
    
    int  [][] faces = ifs.getFaces();
    float[][] verts = ifs.getVerts();
    
    for(int[] face : faces){
      float nx = 0, ny = 0, nz = 0;

      int num_verts = face.length;
      
      // compute face normal
      for(int i = 0; i < num_verts; i++){
        int vi = face[i];
        nx += verts[vi][0];
        ny += verts[vi][1];
        nz += verts[vi][2];
      }
      nx /= num_verts;
      ny /= num_verts;
      nz /= num_verts;
      
      float ax = nx*scale;
      float ay = ny*scale;
      float az = nz*scale;
      float bx = ax + nx*normal_len;
      float by = ay + ny*normal_len;
      float bz = az + nz*normal_len;
      shape.vertex(ax, ay, az);
      shape.vertex(bx, by, bz);
    }
    shape.endShape();
  }
  
  
  
//  static public void createPolyhedronShapeNormals(PShape shape, DwIndexedFaceSetAble ifs, float scale, float normal_len){
//
//    shape.beginShape(PConstants.LINES);
//    shape.stroke(0);
//    
//    int  [][] faces = ifs.getFaces();
//    float[][] verts = ifs.getVerts();
//    
//    for(int[] face : faces){
//
//      float[] va = verts[face[0]];
//      float[] vb = verts[face[1]];
//      float[] vc = verts[face[2]];
//  
//      
//      float dxA = vb[0] - va[0];
//      float dyA = vb[1] - va[1];
//      float dzA = vb[2] - va[2];
//      
//      float dxB = vc[0] - va[0];
//      float dyB = vc[1] - va[1];
//      float dzB = vc[2] - va[2];
//      
//      float nx = (dyA * dzB) - (dyB * dzA);
//      float ny = (dzA * dxB) - (dzB * dxA);
//      float nz = (dxA * dyB) - (dxB * dyA);
//      
//      float dd_sq = nx*nx + ny*ny + nz*nz;
//      if(dd_sq == 0.0f) continue;
//      
//      float dd_inv = 1f / (float) Math.sqrt(dd_sq);
//      nx *= dd_inv;
//      ny *= dd_inv;
//      nz *= dd_inv;
//       
//      float ax = nx*scale;
//      float ay = ny*scale;
//      float az = nz*scale;
//      float bx = ax + nx*normal_len;
//      float by = ay + ny*normal_len;
//      float bz = az + nz*normal_len;
//      shape.vertex(ax, ay, az);
//      shape.vertex(bx, by, bz);
//    }
//    shape.endShape();
//  }
  
  
  
}
