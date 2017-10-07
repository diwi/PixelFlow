/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Sampling.Sampling_Poisson3D;


import com.thomasdiewald.pixelflow.java.geometry.DwIcosahedron;
import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSetAble;
import com.thomasdiewald.pixelflow.java.geometry.DwMeshUtils;
import com.thomasdiewald.pixelflow.java.sampling.PoissonDiscSamping3D;
import com.thomasdiewald.pixelflow.java.sampling.PoissonSample;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PShape;

public class Sampling_Poisson3D extends PApplet {
  
  PeasyCam cam;
  
  PShape shp_samples_spheres;
  PShape shp_samples_points;

  boolean DISPLAY_RADIUS = true;
  boolean GENERATE_SPHERES = true;
  
  public void settings(){
    size(1280, 720, P3D);
    smooth(8);
  }
  
  
  static class MyPoissonSample extends PoissonSample{
    public MyPoissonSample(float x, float y, float z, float r, float r_collision) {
      super(x, y, z, r, r_collision);
    }
  }
  
  public void setup(){
    cam = new PeasyCam(this, 0, 0, 0, 1000);

    generatePoissonSampling();
    
    frameRate(1000);
  }
  
  
  public void generatePoissonSampling(){

    PoissonDiscSamping3D<MyPoissonSample> pds = new PoissonDiscSamping3D<MyPoissonSample>() {
      @Override
      public MyPoissonSample newInstance(float x, float y, float z, float r, float rcollision){
        return new MyPoissonSample(x,y,z,r,rcollision);
      }
    };
    
    long timer;
    float[] bounds = {-200,-200,0, 200, 200, 400};
    float rmin = 10;
    float rmax = 50;
    float roff = 1;
    int new_points = 50;
    
    timer = System.currentTimeMillis();
    
    pds.generatePoissonSampling(bounds, rmin, rmax, roff, new_points);
    
    timer = System.currentTimeMillis() - timer;
    System.out.println("poisson samples 3D generated");
    System.out.println("    time: "+timer+"ms");
    System.out.println("    count: "+pds.samples.size());
    
    
    timer = System.currentTimeMillis();
    shp_samples_spheres = createShape(GROUP);
    shp_samples_points  = createShape(GROUP);
    for(MyPoissonSample sample : pds.samples){
      addShape(sample);
    }
    timer = System.currentTimeMillis() - timer;
    System.out.println("PShapes created");
    System.out.println("    time: "+timer+"ms");
    System.out.println("    count: "+pds.samples.size());
  }
  

  public void draw(){
    lights();
    pointLight(128, 96, 64, -500, -500, -1000);
//    directionalLight(128, 96, 64, -500, -500, +1000);
    
    background(64);
    displayGizmo(500);

    if(DISPLAY_RADIUS){
      shape(shp_samples_spheres);
    } else {
      shape(shp_samples_points);
    }
  }
  
  DwIndexedFaceSetAble ifs;
  
  int verts_per_face = 0;
  
  void addShape(MyPoissonSample sample){
    PShape shp_point = createShape(POINT, sample.x(), sample.y(), sample.z());
    shp_point.setStroke(color(255));
    shp_point.setStrokeWeight(3);
    shp_samples_points.addChild(shp_point);
    

    if(ifs == null){
      ifs = new DwIcosahedron(2); verts_per_face = 3;
//      ifs = new DwCube(2); verts_per_face = 4;
    }
    PShape shp_sphere = createShape(PShape.GEOMETRY);
    shp_sphere.setStroke(false);
    shp_sphere.setFill(color(255));
    shp_sphere.resetMatrix();
    shp_sphere.translate(sample.x(), sample.y(), sample.z());
   
    DwMeshUtils.createPolyhedronShape(shp_sphere, ifs, sample.rad(), verts_per_face, true);
    
    shp_samples_spheres.addChild(shp_sphere);
    
    
//    PShape shp_sphere_normals = createShape(PShape.GEOMETRY);
//    shp_sphere_normals.setStroke(false);
//    shp_sphere_normals.setFill(color(255));
//    shp_sphere_normals.resetMatrix();
//    shp_sphere_normals.translate(sample.x(), sample.y(), sample.z());
//   
//    DwMeshUtils.createPolyhedronShapeNormals(shp_sphere_normals, ifs, sample.rad(), 10);
//    
//    shp_samples_spheres.addChild(shp_sphere_normals);
  }
  
  

  
  PShape shp_gizmo;

  public void displayGizmo(float s){
    if(shp_gizmo == null){
      strokeWeight(1);
      shp_gizmo = createShape();
      shp_gizmo.beginShape(LINES);
      shp_gizmo.stroke(255,0,0); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(s,0,0);
      shp_gizmo.stroke(0,255,0); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(0,s,0); 
      shp_gizmo.stroke(0,0,255); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(0,0,s); 
      shp_gizmo.endShape();
    }
    shape(shp_gizmo);
  }
  
  
  public void keyReleased(){
    if(key == ' ') DISPLAY_RADIUS = !DISPLAY_RADIUS;
    if(key == 'r') generatePoissonSampling();
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Sampling_Poisson3D.class.getName() });
  }
}