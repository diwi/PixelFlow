/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Miscellaneous.BloomSphere;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.geometry.DwCube;
import com.thomasdiewald.pixelflow.java.geometry.DwHalfEdge;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import peasy.PeasyCam;
import processing.core.*;
import processing.opengl.PGraphics3D;

public class BloomSphere extends PApplet {

  
  //
  // Bloom Demo in a 3D scene.
  
  // The faces of a subdivision cube are shaded by their normal distance to two
  // rotating planes. The closer faces are to a plane the brighter their color.
  // The Bloom Shader takes care of the glow.
  //
  // the "normal distance" is the shortest distance between the face-center and
  // the plane
  //
  //
  
  boolean START_FULLSCREEN = !true;
  

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  PeasyCam cam;
  
  float radius = 200;
  boolean DISPLAY_STROKE = false;
  
  DwPixelFlow context;

  PGraphics3D pg_dst;
  PGraphics3D pg_luminance;

  DwCube cube;
  DwHalfEdge.Mesh mesh;
  PShape shp_mesh;
  
  float[] plane_rot = new float[3];

  enum RENDER_PASS{
    COLOR,
    LUMINANCE
  }


  public void settings() {
    if(START_FULLSCREEN){
      fullScreen(P3D);
      viewport_w = width;
      viewport_h = height;
    } else {
      size(viewport_w, viewport_h, P3D);
    }
    smooth(8);
  }
 
  public void setup() {
    background(0);
    if(!START_FULLSCREEN){
      surface.setLocation(viewport_x, viewport_y);
    }
    
    cam = new PeasyCam(this, 600);
    
    createMesh(5);

    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    pg_dst       = (PGraphics3D) createGraphics(width, height, P3D);
    pg_luminance = (PGraphics3D) createGraphics(width, height, P3D);
    
    randomSeed(1);
    frameRate(60);
  }
  
  
  public void createMesh(int subdivisions){
    cube = new DwCube(subdivisions);
    mesh = new DwHalfEdge.Mesh(cube);
    shp_mesh = createShapeMesh();
  }
  
  public PShape createShapeMesh(){
    float[][] verts = mesh.ifs.getVerts();
    int  [][] faces = mesh.ifs.getFaces();
  
    PShape shp = createShape();
    shp.beginShape(QUADS);
    shp.noStroke();
    shp.fill(0);
    for(int i = 0; i < faces.length; i++){
      int[] face = faces[i];
      // face normal is also the face center
      float[] face_center = {0,0,0};
      for(int j = 0; j < 4; j++){
        float[] vi = verts[face[j]];
        face_center[0] += vi[0] * 0.25;
        face_center[1] += vi[1] * 0.25;
        face_center[2] += vi[2] * 0.25;
      }
      
      for(int j = 0; j < 4; j++){
        float[] vi = verts[face[j]];
        float[] vn = face_center;
        shp.normal(vn[0], vn[1], vn[2]);
        shp.vertex(vi[0], vi[1], vi[2]);
      }
    }
    shp.endShape();
    return shp;
  }
  

  public void draw() {

    // main color pass, rendered as usual
    pg_dst.beginDraw();
    displayScene(pg_dst, RENDER_PASS.COLOR);
    pg_dst.endDraw();
    
    // additional pass where only bloom-relevant stuff is rendered
    pg_luminance.beginDraw();
    displayScene(pg_luminance, RENDER_PASS.LUMINANCE);
    pg_luminance.endDraw();
    
    // apply bloom
    DwFilter filter = DwFilter.get(context);
    filter.bloom.param.blur_radius = 2;
    filter.bloom.param.mult        = 5;    //map(mouseX, 0, width, 0, 5);
    filter.bloom.param.radius      = 0.5f; //map(mouseY, 0, height, 0, 1);
    filter.bloom.apply(pg_luminance, null, pg_dst);

    
    // display result
    cam.beginHUD();
    image(pg_dst, 0, 0);
    cam.endHUD();
    
    // info
    int num_faces = mesh.ifs.getFacesCount();
    int num_verts = mesh.ifs.getVertsCount();
    int num_edges = mesh.edges.length;
    String txt_fps = String.format(getClass().getName()+ "   [Verts %d]  [Faces %d]  [HalfEdges %d]  [fps %6.2f]", num_verts, num_faces, num_edges, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  public void displayScene(PGraphics3D canvas, RENDER_PASS pass){
    cam.getState().apply(canvas);

    canvas.background(0);
    canvas.noLights();

    // for the luminance pass we don't want to fill the colorbuffer, just 
    // the depth-test matters.
    if(RENDER_PASS.LUMINANCE == pass){
      canvas.pgl.colorMask(false, false, false, false);
    }
    
    canvas.pushMatrix();
    canvas.scale(radius * 0.99f); // smaller, tot ackel z-fighting
    canvas.shape(shp_mesh);
    canvas.popMatrix();

    if(RENDER_PASS.LUMINANCE == pass){
      canvas.pgl.colorMask(true, true, true, true);
    }
      
    canvas.scale(radius);
    
    float rot_speed = 1f;
    plane_rot[0] += rot_speed * 0.010f;
    plane_rot[1] += rot_speed * 0.001f;
    plane_rot[2] += rot_speed * 0.005f;
    
    float rx = sin(plane_rot[0]) * PI;
    float ry = cos(plane_rot[1]) * PI;
    float rz = sin(plane_rot[2]) * PI;
    
    PMatrix3D mat_rot_plane_1 = new PMatrix3D();
    mat_rot_plane_1.rotateX(rx);
    mat_rot_plane_1.rotateY(ry);
    mat_rot_plane_1.rotateZ(rz);
    
    PMatrix3D mat_rot_plane_2 = new PMatrix3D();
    mat_rot_plane_2.rotateX(rz * 0.5f);
    mat_rot_plane_2.rotateY(rx * 0.2f);
    mat_rot_plane_2.rotateZ(ry * 0.2f);
    
    float[] plane1 = {mat_rot_plane_1.m02, mat_rot_plane_1.m12, mat_rot_plane_1.m22, 0};
    float[] plane2 = {mat_rot_plane_2.m02, mat_rot_plane_2.m12, mat_rot_plane_2.m22, 0};
    
    int  [][] faces = mesh.ifs.getFaces();
    float[][] verts = mesh.ifs.getVerts();

    float face_offset = 0.6f;
    
    canvas.beginShape(QUADS);
    canvas.noStroke();
    for(int i = 0; i < faces.length; i++){
      int[] face = faces[i];
      
      // face normal is also the face center
      float[] face_center = {0,0,0};
      for(int j = 0; j < 4; j++){
        float[] vi = verts[face[j]];
        face_center[0] += vi[0] * 0.25;
        face_center[1] += vi[1] * 0.25;
        face_center[2] += vi[2] * 0.25;
      }
      
      // normal distances of face-center to planes
      float dist1 = abs(dot(face_center, plane1));
      float dist2 = abs(dot(face_center, plane2));
      
      dist1 = (float) Math.pow(dist1, 0.1f) * 1.0f; dist1 = (1 - dist1) * 255;
      dist2 = (float) Math.pow(dist2, 0.1f) * 1.0f; dist2 = (1 - dist2) * 255;
      
      float rgb_r = dist1;
      float rgb_g = max(dist1, dist2) * 0.5f;
      float rgb_b = dist2;
      float rgb_a = 255;
      
      // generated faces with normal-distance-shading
      canvas.fill(rgb_r, rgb_g, rgb_b, rgb_a); 
      for(int j = 0; j < 4; j++){
        float[] vi = verts[face[j]];
        
        float dx = vi[0] - face_center[0];
        float dy = vi[1] - face_center[1];
        float dz = vi[2] - face_center[2];

        float vx = face_center[0] + dx * face_offset;
        float vy = face_center[1] + dy * face_offset;
        float vz = face_center[2] + dz * face_offset;
        float[] vn = face_center;
        canvas.normal(vn[0], vn[1], vn[2]);
        canvas.vertex(vx, vy, vz);
      }
    }
    canvas.endShape();
    

    // draw planes as circles
    canvas.noFill();
    canvas.strokeWeight(4/radius);
    
    canvas.pushMatrix();
    canvas.applyMatrix(mat_rot_plane_1);
    canvas.stroke(255,128,0);
    canvas.ellipse(0, 0, 3f, 3f);
    canvas.popMatrix();
    
    
    canvas.pushMatrix();
    canvas.applyMatrix(mat_rot_plane_2);
    canvas.stroke(0,128,255);
    canvas.ellipse(0, 0, 3f, 3f);
    canvas.popMatrix();
  }
  
  
 
  float dot(float[] va, float[] vb){
    return va[0] * vb[0] + va[1] * vb[1] + va[2] * vb[2];
  }
  

  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { BloomSphere.class.getName() });
  }
}