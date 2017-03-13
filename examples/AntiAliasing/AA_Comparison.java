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





package AntiAliasing;

import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.FXAA.FXAA;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureUtils;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.sampling.DwSampling;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;
import com.thomasdiewald.pixelflow.java.utils.DwVertexRecorder;

import peasy.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics3D;


public class AA_Comparison extends PApplet {


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
  
  PGraphics3D pg_render_noaa;
  PGraphics3D pg_render_smaa;
  PGraphics3D pg_render_fxaa;
  PGraphics3D pg_render_msaa;
  
  
  AA_MODE aa_mode = AA_MODE.NOAA;
  
  
  DwPixelFlow context;
  FXAA fxaa;
  SMAA smaa;
  
  PFont font;
  
  enum AA_MODE{
    NOAA,
    MSAA,
    SMAA,
    FXAA
  }
  
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
    
    // NO AA
    pg_render_noaa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_render_noaa.smooth(0);
    pg_render_noaa.textureSampling(5);
    
    // MSAA
    pg_render_msaa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_render_msaa.smooth(8);

    // FXAA
    pg_render_fxaa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_render_fxaa.smooth(0);
    pg_render_fxaa.textureSampling(5);
    pg_render_fxaa.beginDraw();
    pg_render_fxaa.blendMode(PConstants.REPLACE);
    pg_render_fxaa.endDraw();
    
    // SMAA
    pg_render_smaa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_render_smaa.smooth(0);
    pg_render_smaa.textureSampling(5);
    pg_render_smaa.beginDraw();
    pg_render_smaa.blendMode(PConstants.REPLACE);
    pg_render_smaa.endDraw();
    
    context = new DwPixelFlow(this);
    
    fxaa = new FXAA(context);
    smaa = new SMAA(context);
    
    
    // processing font
//    font = createFont("SourceCodePro-Regular.ttf", 32);
    font = createFont("Calibri", 32);
    textFont(font);
    
    frameRate(1000);
  }

  

  public void draw() {

    DwGLTextureUtils.copyMatrices((PGraphics3D)this.g, pg_render_noaa);
    DwGLTextureUtils.copyMatrices((PGraphics3D)this.g, pg_render_msaa);
    
    
    if(aa_mode == AA_MODE.MSAA){
      pg_render_msaa.beginDraw();
      displayScene(pg_render_msaa);
      pg_render_msaa.endDraw();
      
      pg_render_msaa.beginDraw();
      pg_render_msaa.blendMode(PConstants.REPLACE);
      pg_render_msaa.endDraw();
      
      DwFilter.get(context).gamma.apply(pg_render_msaa, pg_render_msaa, gamma);
    }
    
    if(aa_mode == AA_MODE.NOAA || aa_mode == AA_MODE.SMAA || aa_mode == AA_MODE.FXAA){
      pg_render_noaa.beginDraw();
      displayScene(pg_render_noaa);
      pg_render_noaa.endDraw();
      
      pg_render_noaa.beginDraw();
      pg_render_noaa.blendMode(PConstants.REPLACE);
      pg_render_noaa.endDraw();
      
  
      // 1) RGB gamma correction
      DwFilter.get(context).gamma.apply(pg_render_noaa, pg_render_noaa, gamma);
      
      // 2) RGBL ... red, green, blue, luminance, for FXAA
      DwFilter.get(context).rgbl.apply(pg_render_noaa, pg_render_noaa);
      
      if(aa_mode == AA_MODE.SMAA) smaa.apply(pg_render_noaa, pg_render_smaa);
      if(aa_mode == AA_MODE.FXAA) fxaa.apply(pg_render_noaa, pg_render_fxaa);
    }
    
    PGraphics3D display = pg_render_noaa;
    
    switch(aa_mode){
      case NOAA: display = pg_render_noaa; break;
      case MSAA: display = pg_render_msaa; break;
      case SMAA: display = pg_render_smaa; break;
      case FXAA: display = pg_render_fxaa; break;
    }

    

    blendMode(PConstants.REPLACE);
    clear();
    peasycam.beginHUD();
    image(display, 0, 0);
    
    fill(255);
    text(aa_mode+"", 10, height-10);
    peasycam.endHUD();

    // some info, window title
    String txt_fps = String.format(getClass().getName()+ "   [fps %6.2f]", frameRate);
    surface.setTitle(txt_fps);
  }


  public void displayScene(PGraphics canvas){
    float BACKGROUND_COLOR_GAMMA = (float) (Math.pow(BACKGROUND_COLOR/255.0, gamma) * 255.0);

    canvas.blendMode(PConstants.BLEND);
    canvas.background(BACKGROUND_COLOR_GAMMA);
    canvas.pointLight(255, 255, 255, 600,600,600);
    canvas.pointLight(50, 50, 50, -600,-600,20);
    canvas.ambientLight(64, 64, 64);
    canvas.shape(shape);
    
//    float aabb = 400;
////    displayGizmo(canvas, 300);
//    displayGridXY(canvas, 50, 20);
//    displayAABB(canvas, new float[]{-aabb, -aabb, 0, aabb, aabb, aabb});
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
      shp_gizmo.strokeWeight(1f);
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
      shp_gridxy.strokeWeight(1f);
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
    if(key == '1') aa_mode = AA_MODE.NOAA;
    if(key == '2') aa_mode = AA_MODE.MSAA;
    if(key == '3') aa_mode = AA_MODE.SMAA;
    if(key == '4') aa_mode = AA_MODE.FXAA;
  }
  
  public static void main(String args[]) {
    PApplet.main(new String[] { AA_Comparison.class.getName() });
  }
}
