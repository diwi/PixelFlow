/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
 * 
 * src  - www.github.com/diwi/PixelFlow
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */





package AntiAliasing_SMAA;

import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureUtils;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import peasy.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.opengl.PGraphics3D;


public class AntiAliasing_SMAA extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;

  // camera control
  PeasyCam peasycam;
  
  float gamma = 2.2f;
  float BACKGROUND_COLOR = 32;
  

  // scene to render
  PShape shape;
  
  PGraphics3D pg_render;
  PGraphics3D pg_smaa;
  
  DwPixelFlow context;
  SMAA smaa;

  public void settings() {
    size(viewport_w, viewport_h, P3D);
    smooth(0);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);

    // camera
    peasycam = new PeasyCam(this, -4.083,  -6.096,   7.000, 500);
    peasycam.setRotations(  1.085,  -0.477,   2.910);

    // projection
    perspective(60 * DEG_TO_RAD, width/(float)height, 2, 5000);

    // load obj file into shape-object
    shape = loadShape("examples/data/skylight_demo_scene.obj");
    shape.scale(10);
    
    // main rendertarget
    pg_render = (PGraphics3D) createGraphics(width, height, P3D);
    pg_render.smooth(0);
    pg_render.textureSampling(5);

    // postprocessing AA
    pg_smaa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_smaa.smooth(0);
    pg_smaa.textureSampling(5);
    pg_smaa.beginDraw();
    pg_smaa.blendMode(PConstants.REPLACE);
    pg_smaa.endDraw();
    
    context = new DwPixelFlow(this);
    smaa = new SMAA(context);
    
    frameRate(1000);
  }

  

  public void draw() {
    float BACKGROUND_COLOR_GAMMA = (float) (Math.pow(BACKGROUND_COLOR/255.0, gamma) * 255.0);

    DwGLTextureUtils.copyMatrices((PGraphics3D)this.g, pg_render);

    boolean APPLY_SMAA = !(keyPressed && key == ' ');
    
    pg_render.beginDraw();
    pg_render.blendMode(PConstants.BLEND);
    pg_render.background(BACKGROUND_COLOR_GAMMA);
    pg_render.pointLight(255, 255, 255, 600,600,600);
    pg_render.pointLight(50, 50, 50, -600,-600,20);
    pg_render.ambientLight(64, 64, 64);
    displayScene(pg_render);
    pg_render.endDraw();
    
    // 1) RGB gamma correction
    // 2) RGBL ... red, green, blue, luminance
    pg_render.beginDraw();
    pg_render.blendMode(PConstants.REPLACE);
    pg_render.endDraw();
    DwFilter.get(context).gamma.apply(pg_render, pg_render, 2.2f);
    
    // AntiAliasing ... SMAA
    smaa.apply(pg_render, pg_smaa);
    
    blendMode(PConstants.REPLACE);
    clear();
    peasycam.beginHUD();
    if(APPLY_SMAA) {
      image(pg_smaa, 0, 0);
    } else {
      image(pg_render, 0, 0);
    }
    peasycam.endHUD();

    // some info, window title
    String txt_fps = String.format(getClass().getName()+ "   [fps %6.2f]", frameRate);
    surface.setTitle(txt_fps);
  }


  public void displayScene(PGraphics canvas){
    canvas.shape(shape);
//    displayGizmo(canvas, 300);
//    displayGridXY(canvas, 50, 10);
  }
  
  
  
  
  
  
  

  //////////////////////////////////////////////////////////////////////////////
  // Scene Display Utilities
  //////////////////////////////////////////////////////////////////////////////
  
  PShape shp_gizmo;
  PShape shp_gridxy;
  PShape shp_aabb;
  
  public void displayGizmo(PGraphics canvas, float s){
    if(shp_gizmo == null){
      shp_gizmo = createShape();
      shp_gizmo.beginShape(LINES);
      shp_gizmo.strokeWeight(1);
      shp_gizmo.stroke(255,0,0); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(s,0,0);
      shp_gizmo.stroke(0,255,0); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(0,s,0); 
      shp_gizmo.stroke(0,0,255); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(0,0,s); 
      shp_gizmo.endShape();
    }
    canvas.shape(shp_gizmo);
  }
  
  public void displayGridXY(PGraphics canvas, int lines, float s){
    if(shp_gridxy == null){
      shp_gridxy = createShape();
      shp_gridxy.beginShape(LINES);
      shp_gridxy.stroke(0);
      shp_gridxy.strokeWeight(1);
      float d = lines*s;
      for(int i = 0; i <= lines; i++){
        shp_gridxy.vertex(-d,-i*s,0); shp_gridxy.vertex(d,-i*s,0);
        shp_gridxy.vertex(-d,+i*s,0); shp_gridxy.vertex(d,+i*s,0);
        
        shp_gridxy.vertex(-i*s,-d,0); shp_gridxy.vertex(-i*s,d,0);
        shp_gridxy.vertex(+i*s,-d,0); shp_gridxy.vertex(+i*s,d,0);
      }
      shp_gridxy.endShape();
    }
    canvas.shape(shp_gridxy);
  }
  
  
  public void displayAABB(PGraphics canvas, float[] aabb){
    if(shp_aabb == null){
      float xmin = aabb[0], xmax = aabb[3];
      float ymin = aabb[1], ymax = aabb[4];
      float zmin = aabb[2], zmax = aabb[5];
      
      shp_aabb = createShape(GROUP);
      
      PShape plane_zmin = createShape();
      plane_zmin.beginShape(QUAD);
      plane_zmin.stroke(0);
      plane_zmin.strokeWeight(1);
      plane_zmin.fill(32,128,192);
      plane_zmin.normal(0, 0, 1); plane_zmin.vertex(xmin, ymin, zmin);
      plane_zmin.normal(0, 0, 1); plane_zmin.vertex(xmax, ymin, zmin);
      plane_zmin.normal(0, 0, 1); plane_zmin.vertex(xmax, ymax, zmin);
      plane_zmin.normal(0, 0, 1); plane_zmin.vertex(xmin, ymax, zmin);
      plane_zmin.endShape(CLOSE);
      shp_aabb.addChild(plane_zmin);
      
      PShape plane_zmax = createShape();
      plane_zmax.beginShape(QUAD);
      plane_zmax.noFill();
      plane_zmax.stroke(0);
      plane_zmax.strokeWeight(1);
      plane_zmax.vertex(xmin, ymin, zmax);
      plane_zmax.vertex(xmax, ymin, zmax);
      plane_zmax.vertex(xmax, ymax, zmax);
      plane_zmax.vertex(xmin, ymax, zmax);
      plane_zmax.endShape(CLOSE);
      shp_aabb.addChild(plane_zmax);
      
      PShape vert_lines = createShape();
      vert_lines.beginShape(LINES);
      vert_lines.stroke(0);
      vert_lines.strokeWeight(1);
      vert_lines.vertex(xmin, ymin, zmin);  vert_lines.vertex(xmin, ymin, zmax);
      vert_lines.vertex(xmax, ymin, zmin);  vert_lines.vertex(xmax, ymin, zmax);
      vert_lines.vertex(xmax, ymax, zmin);  vert_lines.vertex(xmax, ymax, zmax);
      vert_lines.vertex(xmin, ymax, zmin);  vert_lines.vertex(xmin, ymax, zmax);
      vert_lines.endShape();
      shp_aabb.addChild(vert_lines);
      
      PShape corners = createShape();
      corners.beginShape(POINTS);
      corners.stroke(0);
      corners.strokeWeight(7);
      corners.vertex(xmin, ymin, zmin);  corners.vertex(xmin, ymin, zmax);
      corners.vertex(xmax, ymin, zmin);  corners.vertex(xmax, ymin, zmax);
      corners.vertex(xmax, ymax, zmin);  corners.vertex(xmax, ymax, zmax);
      corners.vertex(xmin, ymax, zmin);  corners.vertex(xmin, ymax, zmax);
      corners.endShape();
      shp_aabb.addChild(corners);
    }
    canvas.shape(shp_aabb);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  public void printCam(){
    float[] pos = peasycam.getPosition();
    float[] rot = peasycam.getRotations();
    float[] lat = peasycam.getLookAt();
    float   dis = (float) peasycam.getDistance();
    
    System.out.printf(Locale.ENGLISH, "position: (%7.3f, %7.3f, %7.3f)\n", pos[0], pos[1], pos[2]);
    System.out.printf(Locale.ENGLISH, "rotation: (%7.3f, %7.3f, %7.3f)\n", rot[0], rot[1], rot[2]);
    System.out.printf(Locale.ENGLISH, "look-at:  (%7.3f, %7.3f, %7.3f)\n", lat[0], lat[1], lat[2]);
    System.out.printf(Locale.ENGLISH, "distance: (%7.3f)\n", dis);
  }
  
  
  public void keyReleased(){
//    printCam();
  }
  
  public static void main(String args[]) {
    PApplet.main(new String[] { AntiAliasing_SMAA.class.getName() });
  }
}
