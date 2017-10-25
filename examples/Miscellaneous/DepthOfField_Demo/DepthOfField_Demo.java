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


package Miscellaneous.DepthOfField_Demo;

import java.util.Locale;
import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.geometry.DwCube;
import com.thomasdiewald.pixelflow.java.geometry.DwMeshUtils;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DepthOfField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwScreenSpaceGeometryBuffer;
import com.thomasdiewald.pixelflow.java.utils.DwMagnifier;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import peasy.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;


public class DepthOfField_Demo extends PApplet {
  
  //
  // Depth of Field (DoF)
  //
  // WIP, I am still experimenting here ... so better do not use it yet.
  // 
  //

  boolean START_FULLSCREEN = !true;

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;

  // camera control
  PeasyCam peasycam;
  
  // library context
  DwPixelFlow context;

  PGraphics3D pg_render;
  PGraphics3D pg_dof;
  PGraphics3D pg_tmp;

  DepthOfField dof;
  DwScreenSpaceGeometryBuffer geombuffer;
  
  DwMagnifier magnifier;
  
  boolean APPLY_DOF = true;
  
  
  public void settings() {
    if(START_FULLSCREEN){
      viewport_w = displayWidth;
      viewport_h = displayHeight;
      viewport_x = 0;
      viewport_y = 0;
      fullScreen(P3D);
    } else {
      size(viewport_w, viewport_h, P3D);
    }
    smooth(0);
  }
  
 
  public void setup() {
    surface.setResizable(true);
    surface.setLocation(viewport_x, viewport_y);

    // camera
    peasycam = new PeasyCam(this, -4.083,  -6.096,   7.000, 1300);
    peasycam.setRotations(  1.085,  -0.477,   2.910);
    
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // callback for scene display (used in GBAA)
    DwSceneDisplay scene_display = new DwSceneDisplay() {  
      @Override
      public void display(PGraphics3D canvas) {
        displayScene(canvas); 
      }
    };
    
    geombuffer = new DwScreenSpaceGeometryBuffer(context, scene_display);
    
    dof = new DepthOfField(context);
    
    int mag_h = (int) (height/3f);
    magnifier = new DwMagnifier(this, 4, 0, height-mag_h, mag_h, mag_h);
    
    frameRate(1000);
  }


  
  // dynamically resize render-targets
  public boolean resizeScreen(){

    boolean[] RESIZED = {false};
    
    pg_render = DwUtils.changeTextureSize(this, pg_render, width, height, 8, RESIZED);
    pg_dof    = DwUtils.changeTextureSize(this, pg_dof   , width, height, 0, RESIZED);
    pg_tmp    = DwUtils.changeTextureSize(this, pg_tmp   , width, height, 0, RESIZED, GL2.GL_RGBA16F, GL2.GL_RGBA, GL2.GL_FLOAT);

//    geombuffer.resize(width, height)
    
    if(RESIZED[0]){
      // nothing here
    }
    
    peasycam.feed();
    perspective(60 * DEG_TO_RAD, width/(float)height, 2, 6000);
    
    return RESIZED[0];
  }
  


  public void draw() {
    
    resizeScreen();
    
    displaySceneWrap(pg_render);
    
    DwFilter.get(context).gamma.apply(pg_render, pg_render);
    
    int mult_blur = 30;

    if(APPLY_DOF){
      geombuffer.update(pg_render);

      DwFilter.get(context).gaussblur.apply(geombuffer.pg_geom, geombuffer.pg_geom, pg_tmp, (int) Math.pow(mult_blur, 0.33f));

//      dof.param.focus     = map(mouseX, 0, width, 0, 1);
      dof.param.focus_pos = new float[]{0.5f, 0.5f};
      dof.param.focus_pos[0] = map(mouseX+0.5f, 0, width , 0, 1);
      dof.param.focus_pos[1] = map(mouseY+0.5f, 0, height, 1, 0);
      dof.param.mult_blur = mult_blur;
      dof.apply(pg_render, pg_dof, geombuffer);
      
      DwFilter.get(context).copy.apply(pg_dof, pg_render);
    }
    
    magnifier.apply(pg_render, mouseX, mouseY);
    magnifier.displayTool();

    DwUtils.beginScreen2D(g);
//    peasycam.beginHUD();
    {
      blendMode(REPLACE);
      clear();
      image(pg_render, 0, 0);
//      image(geombuffer.pg_geom, 0, 0);
      
      magnifier.display(g, 0, height-magnifier.h);
      
      blendMode(BLEND);
      
      pushMatrix();
      float cursor_s = 10;
      
      float fpx = (       dof.param.focus_pos[0]) * width;
      float fpy = (1.0f - dof.param.focus_pos[1]) * height;
      
      blendMode(EXCLUSION);
      translate(fpx, fpy);
      strokeWeight(1);
      stroke(255,200);
      line(-cursor_s, 0, +cursor_s, 0);
      line(0, -cursor_s, 0, +cursor_s);
      blendMode(BLEND);
      popMatrix();
    }
//    peasycam.endHUD();
    DwUtils.endScreen2D(g);
    
    // some info, window title
    String txt_fps = String.format(getClass().getName()+ "   [fps %6.2f]", frameRate);
    surface.setTitle(txt_fps);
  }


  
  public void displaySceneWrap(PGraphics3D canvas){
    canvas.beginDraw();
    DwUtils.copyMatrices((PGraphics3D) this.g, canvas);
    // background
    canvas.blendMode(PConstants.BLEND);
    canvas.background(2);
    displayScene(canvas);
    canvas.endDraw();
  }
  
  

  // render something
  public void displayScene(PGraphics3D canvas){
    // lights
    canvas.directionalLight(255, 255, 255, 200,600,400);
    canvas.directionalLight(255, 255, 255, -200,-600,-400);
    canvas.ambientLight(64, 64, 64);
    
    if(canvas == geombuffer.pg_geom){
      canvas.background(255, 255);
      canvas.pgl.clearColor(1, 1, 1, 6000);
      canvas.pgl.clear(PGL.COLOR_BUFFER_BIT);
    }
    

    sceneShape(canvas);
  }
  
  
  
  

  PShape shp_scene;
  
  public void sceneShape(PGraphics3D canvas){
    if(shp_scene != null){
      canvas.shape(shp_scene);
      return;
    }
    
    shp_scene = createShape(GROUP);
    
    int num_boxes = 50;
    int num_spheres = 50;
    float bb_size = 800;
    float xmin = -bb_size;
    float xmax = +bb_size;
    float ymin = -bb_size;
    float ymax = +bb_size;
    float zmin =  0;
    float zmax = +bb_size;

    colorMode(HSB, 360, 1, 1);
    randomSeed(0);

    for(int i = 0; i < num_boxes; i++){
      float px = random(xmin, xmax);
      float py = random(ymin, ymax);
      float sx = random(200) + 10;
      float sy = random(200) + 10;
      float sz = random(zmin, zmax);

      
      float off = 45;
      float base = 0;
      float hsb_h = base + random(-off,off);
      float hsb_s = 1;
      float hsb_b = random(0.1f,1.0f);
      int shading = color(hsb_h, hsb_s, hsb_b);
      
      PShape shp_box = createShape(BOX, sx, sy, sz);
      shp_box.setFill(true);
      shp_box.setStroke(false);
      shp_box.setFill(shading);
      shp_box.translate(px, py, sz/2);
      shp_scene.addChild(shp_box);
    }
    

    DwCube cube_smooth = new DwCube(4);
    DwCube cube_facets = new DwCube(2);

    for(int i = 0; i < num_spheres; i++){
      float px = random(xmin, xmax);
      float py = random(ymin, ymax);
      float pz = random(zmin, zmax);
      float rr = random(50) + 50;
      boolean facets = true;//(i%2 == 0);

      float off = 20;
      float base = 225;
      float hsb_h = base + random(-off,off);
      float hsb_s = 1;
      float hsb_b = random(0.1f,1.0f);
      int shading = color(hsb_h, hsb_s, hsb_b);
      
      PShape shp_sphere = createShape(PShape.GEOMETRY);
      if(facets){
        DwMeshUtils.createPolyhedronShape(shp_sphere, cube_facets, 1, 4, false);
      } else {
        DwMeshUtils.createPolyhedronShape(shp_sphere, cube_smooth, 1, 4, true);
      }

      shp_sphere.setStroke(!true);
      shp_sphere.setStroke(color(0));
      shp_sphere.setStrokeWeight(0.01f / rr);
      shp_sphere.setFill(true);
      shp_sphere.setFill(shading);
      shp_sphere.resetMatrix();
      shp_sphere.scale(rr);
      shp_sphere.translate(px, py, pz);
      shp_scene.addChild(shp_sphere);
    }
    colorMode(RGB, 255, 255, 255);
    
    PShape shp_rect = createShape(RECT, -1000, -1000, 2000, 2000);
    shp_rect.setStroke(false);
    shp_rect.setFill(true);
    shp_rect.setFill(color(255));
    
    shp_scene.addChild(shp_rect);
  }
  
  
  

  
  
  
  public PShape createGridXY(int lines, float s){
    PShape shp_gridxy = createShape();
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
    return shp_gridxy;
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
    if(key == 'c') printCam();
    if(key == ' ') APPLY_DOF = !APPLY_DOF;
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { DepthOfField_Demo.class.getName() });
  }
}
