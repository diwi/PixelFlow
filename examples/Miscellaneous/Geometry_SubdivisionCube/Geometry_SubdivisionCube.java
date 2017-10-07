/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Miscellaneous.Geometry_SubdivisionCube;

import com.thomasdiewald.pixelflow.java.geometry.DwCube;
import com.thomasdiewald.pixelflow.java.geometry.DwHalfEdge;
import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSetAble;

import peasy.PeasyCam;
import processing.core.*;
import processing.opengl.PGraphics3D;

public class Geometry_SubdivisionCube extends PApplet {

  
  // A demo to create a Subdivision Cube, and either render it by as usual, 
  // or convert it to a HalfEdge representation and use that for rendering and
  // more complex mesh operations/iterations.
  //
  // Controls:
  // key '1'-'7' ... subdivisions
  // key 's        ... toggle stroke display
  
  PeasyCam cam;
  
  DwCube cube;
  DwHalfEdge.Mesh mesh;
  
  float radius = 200;
  boolean DISPLAY_STROKE = false;

  public void settings() {
    size(800, 800, P3D);
    smooth(8);
  }
 
  public void setup() {
    cam = new PeasyCam(this, 1000);
    createMesh(4);
  }
  
  
  public void createMesh(int subdivisions){
    cube = new DwCube(subdivisions);
    mesh = new DwHalfEdge.Mesh(cube);
  }
  
  public void draw() {
    lights();
    directionalLight(128, 96, 64, -500, -500, +1000);
    background(64);
    displayGizmo(300);
    
    scale(radius);
    if(DISPLAY_STROKE){
      strokeWeight(0.5f/radius);
      stroke(0);
    } else {
      noStroke();
    }
    fill(255);

    // display the IFS-mesh (Indexed Face set)
    pushMatrix();
      translate(-1.5f, 0);
      displayMesh(cube);
    popMatrix();
    
    // display the HalfEdge mesh
    pushMatrix();
      translate(+1.5f, 0);
      mesh.display((PGraphics3D) this.g);
    popMatrix();
    

    // info
    int num_faces = mesh.ifs.getFacesCount();
    int num_verts = mesh.ifs.getVertsCount();
    int num_edges = mesh.edges.length;
    String txt_fps = String.format(getClass().getName()+ "   [Verts %d]  [Faces %d]  [HalfEdges %d]  [fps %6.2f]", num_verts, num_faces, num_edges, frameRate);
    surface.setTitle(txt_fps);
  }
  
  // this method can of course be optimized if we know in advance the number of 
  // vertices per face
  public void displayMesh(DwIndexedFaceSetAble ifs){
    int       faces_count = ifs.getFacesCount();
    int  [][] faces       = ifs.getFaces();
    float[][] verts       = ifs.getVerts();
    float[] v;

    for(int i = 0; i < faces_count; i++){
      int[] face = faces[i];
      switch(face.length){
      case 3: 
        beginShape(TRIANGLE);
        v = verts[face[2]];  vertex(v[0], v[1], v[2]);
        v = verts[face[1]];  vertex(v[0], v[1], v[2]);
        v = verts[face[0]];  vertex(v[0], v[1], v[2]);
        endShape();
        break;
      case 4: 
        beginShape(QUAD);
        v = verts[face[3]];  vertex(v[0], v[1], v[2]);
        v = verts[face[2]];  vertex(v[0], v[1], v[2]);
        v = verts[face[1]];  vertex(v[0], v[1], v[2]);
        v = verts[face[0]];  vertex(v[0], v[1], v[2]);
        endShape();
        break;
      default:
        beginShape(); // POLYGON
        for(int j = 0; j < face.length; j++){
          v = verts[face[j]];  vertex(v[0], v[1], v[2]);
        }
        endShape(CLOSE);
        break;
      }
    }
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
    if(key >= '1' && key <= '7') createMesh(key-'1');
    if(key == 's') DISPLAY_STROKE = !DISPLAY_STROKE;
  }

  
  public static void main(String args[]) {
    PApplet.main(new String[] { Geometry_SubdivisionCube.class.getName() });
  }
}