/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.utils;


import java.util.Arrays;
import java.util.Locale;

import com.thomasdiewald.pixelflow.java.accelerationstructures.DwStack;
import processing.core.PApplet;
import processing.core.PShape;
import processing.opengl.PGraphics3D;


public class DwVertexRecorder extends PGraphics3D{

  public int       verts_count = 0;
  public float[][] verts = new float[10][3];
  
  public DwVertexRecorder(PApplet papplet){
    setParent(papplet);
    setPrimary(false);
    setSize(papplet.width, papplet.height);
  }
  
  public DwVertexRecorder(PApplet papplet, PShape ... shapes){
    setParent(papplet);
    setPrimary(false);
    setSize(papplet.width, papplet.height);
    
    record(shapes);
  }
  
  public void record(PShape ... shapes){
    parent.beginRecord(this);
    for(PShape shp : shapes){
      parent.shape(shp);
    }
    parent.endRecord();
  }
    
  public void beginDraw(){
    super.beginDraw();
//    System.out.println("SceneRecorder.beginDraw");
    verts_count = 0;
  }
  
  public void endDraw() {
   super.endDraw();
//   System.out.println("SceneRecorder.endraw");
//   update();
  }
  
  public void perspective(float fov, float aspect, float zNear, float zFar){
//    System.out.println("  endShape");
  }
  
  public void beginShape() {
//    super.beginShape();
//    System.out.println("  beginShape");
  }

  public void beginShape(int kind) {
//    super.beginShape(kind);
//    System.out.println("  beginShape");
  }
  
  public void endShape(int mode){
//    super.endShape(mode);
//    System.out.println("  endShape");
  }
  public void endShape(){
//    System.out.println("  endShape");
  }
  
  public void vertex(float x, float y, float z) {
    System.out.printf(Locale.ENGLISH, "  vertex %6.2f,%6.2f,%6.2f\n",x,y,z);
    addVertex(x,y,z);
  }
  
  public void vertex(float x, float y, float z, float u, float v) {
    System.out.printf(Locale.ENGLISH, "  vertex %6.2f,%6.2f,%6.2f  ,%6.2f,%6.2f\n",x,y,z,u,v);
    addVertex(x,y,z);
  }
  
  public void shape(PShape shape){
    DwStack<PShape> stack_shapes = new DwStack<PShape>();
    stack_shapes.push(shape);
    
    while(!stack_shapes.isEmpty()){
      shape = stack_shapes.pop();

      int vertex_count = shape.getVertexCount();
      for(int i = 0; i < vertex_count; i++){ 
        float x = shape.getVertexX(i);
        float y = shape.getVertexY(i);
        float z = shape.getVertexZ(i);
        addVertex(x,y,z);
      }
      
      int child_count = shape.getChildCount();
      if(child_count > 0){
        PShape[] children = shape.getChildren();
        for(int i = 0; i < child_count; i++){ 
          stack_shapes.push(children[i]);
        }
      }
    }
    
//    PShape tess = shape.getTessellation();
//    System.out.println( tess.getChildCount());
//    System.out.println( tess.getVertexCount());
  }
  
  
  
  
  public void addVertex(float x, float y, float z){
    realloc();
    float[] vert = verts[verts_count];
    if(vert == null){
      verts[verts_count] = vert = new float[3];
    }
    vert[0] = x;
    vert[1] = y;
    vert[2] = z;
    
    verts_count++;
  }
  
  public void realloc(){
    int size =  verts.length;
    if(verts_count >= size){
      size  = (int) Math.ceil(size * 1.3333f);
      verts = Arrays.copyOf(verts, size);
    }
  }
  
  
  
//  public DwBoundingSphere bounding_sphere = new DwBoundingSphere();
//  float[] bounding_box = new float[6];
//  
//  public void update(){
//    computeBoundingBox(verts, verts_count);
//    bounding_sphere.compute(verts, verts_count);
//  }
//  
//  public void computeBoundingBox(float[][] verts, int verts_count){
//    // compute AABB
//    float xmin, ymin, zmin;
//    float xmax, ymax, zmax;
//    
//    xmin = ymin = zmin = +Float.MAX_VALUE;
//    xmax = ymax = zmax = -Float.MAX_VALUE;
//    
//    for(int i = 0; i < verts_count; i++){
//      float[] vert = verts[i];
//      float x = vert[0];
//      float y = vert[1];
//      float z = vert[2];
//      
//      if(xmin > x) xmin = x;
//      if(ymin > y) ymin = y;
//      if(zmin > z) zmin = z;
//      if(xmax < x) xmax = x;
//      if(ymax < y) ymax = y;
//      if(zmax < z) zmax = z;
//    }
//    
//    bounding_box[0] = xmin; bounding_box[1] = ymin; bounding_box[2] = zmin;
//    bounding_box[3] = xmax; bounding_box[4] = ymax; bounding_box[5] = zmax;
//  }
  
  
}
