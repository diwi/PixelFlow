/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Miscellaneous.PickAndMove;



import com.thomasdiewald.pixelflow.java.utils.DwCoordinateTransform;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics3D;


public class PickAndMove extends PApplet {

  //
  // Demo: Picking, Coordinate Transform (screen <-> world), Object Transform (screen-aligned)
  //
  // controls:
  //
  // 'w' + mouse: Move objects (screen-aligned)
  //
  // LMB: orbit
  // MMB: pan
  // RMB: zoom
  //

 
  PeasyCam cam;

  PGraphics3D pg_pick;
  
  DwCoordinateTransform transform = new DwCoordinateTransform();
  
  SceneObject[] scene_objects;
  
  PShape shp_gizmo;
  PShape shp_grid;
  PShape group_render;
  PShape group_pick;

  public void settings(){
    size(1280, 720, P3D); 
    smooth(8);
  }
  
  
  public void setup() {
    
    cam = new PeasyCam(this, 1000);
    
    perspective(60 * PI/180f, width/(float)height, 1, 200000);
    
    // picking buffer
    pg_pick = (PGraphics3D) createGraphics(width, height, P3D);
    pg_pick.smooth(0);
    
    createScene();
  }
  

  
  public void draw(){
    
    // disable peasycam when objects are moved
    cam.setActive(!MOVE_OBJECT);
    
    // primary graphics
    PGraphics3D pg_canvas = (PGraphics3D) this.g;

    // render scene to the picking buffer
    pg_pick.beginDraw();
    pg_pick.modelview .set(pg_canvas.modelview);   // copy current modelview
    pg_pick.projection.set(pg_canvas.projection);  // copy current projection
    pg_pick.updateProjmodelview();
    displayScene(pg_pick);
    pg_pick.endDraw();
    pg_pick.loadPixels();
    
    // update moving objects, mouse over, etc...
    updateMouseAction();
    
    // display final scene
    displayScene(pg_canvas);
    
  
    // picking buffer
//    cam.beginHUD();
//    noLights();
//    background(0);
//    image(pg_pick, 0,0);
//    cam.endHUD();
    
    String txt_fps = String.format(getClass().getName()+ "   [objects %d]  [fps %6.2f]", scene_objects.length, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  // create scene objects, gizmo, grid, etc...
  public void createScene(){
    
    createGizmo(600);
    createGrid(15, 1200);

    group_render = createShape(GROUP);
    group_pick   = createShape(GROUP);
    
    scene_objects = new SceneObject[5000];
    
    colorMode(HSB, 360, 100, 100);
    
    for(int id = 0; id < scene_objects.length; id++){
      float pr = 300;
      float posx = random(-pr,pr);
      float posy = random(-pr,pr);
      float posz = random(-pr,pr);
      
      float dmin = 20;
      float dmax = 40;
      float dimx = random(dmin, dmax);
      float dimy = random(dmin, dmax);
      float dimz = random(dmin, dmax * 5);
      
      float colr = random(40, 240);
      float colg = random(30,70);
      float colb = 100;
      
      SceneObject obj = new SceneObject();
      obj.shp_render = createShape(BOX, dimx, dimy, dimz);
      obj.shp_pick   = createShape(BOX, dimx, dimy, dimz);
        
      // render shape style
      obj.shp_render.setStroke(true);
      obj.shp_render.setStroke(color(0));
      obj.shp_render.setStrokeWeight(1f);
      obj.shp_render.setFill(true);
      obj.shp_render.setFill(color(colr, colg, colb));

      // picking shape style
      obj.shp_pick.setStroke(false);
      obj.shp_pick.setFill(true);
      obj.shp_pick.setFill(0xFF000000 | id);

      // apply some local transformations
      obj.mat.reset();
      obj.mat.translate(posx, posy, posz);
      obj.mat.rotateZ(random(PI));
      obj.mat.rotateX(random(PI));
      obj.mat.rotateY(random(PI));
      obj.udpateShapesTransform();
      
      // add shapes to global group (for fast rendering)
      group_render.addChild(obj.shp_render);
      group_pick  .addChild(obj.shp_pick);
      
      scene_objects[id] = obj;
    }
    
    colorMode(RGB, 255, 255, 255);
  }
  

  public void createGizmo(float s){
    shp_gizmo = createShape();
    shp_gizmo.beginShape(LINES);
    shp_gizmo.strokeWeight(1.5f);
    shp_gizmo.stroke(255,0,0); shp_gizmo.vertex(0, 0, 0); shp_gizmo.vertex(s, 0, 0);
    shp_gizmo.stroke(0,255,0); shp_gizmo.vertex(0, 0, 0); shp_gizmo.vertex(0, s, 0);
    shp_gizmo.stroke(0,0,255); shp_gizmo.vertex(0, 0, 0); shp_gizmo.vertex(0, 0, s);
    shp_gizmo.endShape();
  }

  
  public void createGrid(int n, float s){
    shp_grid = createShape();
    shp_grid.beginShape(LINES);
    shp_grid.strokeWeight(0.5f);
    shp_grid.stroke(128);
    shp_grid.translate(-s/2, -s/2);
    float grid_d = s/(n-1);
    for(int y = 0; y < n; y++){
      for(int x = 0; x < n; x++){
        float sx = x * grid_d;
        float sy = y * grid_d;
        shp_grid.vertex(sx, 0, 0); shp_grid.vertex(sx, s, 0);
        shp_grid.vertex(0, sy, 0); shp_grid.vertex(s, sy, 0);    
      }
    }
    shp_grid.endShape();
  }
  
  
 
  public void displayScene(PGraphics3D canvas){
    if(canvas == pg_pick){
      canvas.blendMode(REPLACE);
      canvas.clear();
      canvas.noLights();
      canvas.shape(group_pick);
    } else {
      canvas.blendMode(BLEND);
      canvas.background(32);
      canvas.lights();
      canvas.shape(shp_gizmo);
      canvas.shape(shp_grid);
      canvas.shape(group_render);
    }
  }
  
  

  SceneObject obj_sel = null;
  PMatrix3D   obj_mat = new PMatrix3D();
  float[]     obj_off = null;
  
  int col_prev = 0;
  int idx_prev = 0;
  

  
  public void updateMouseAction(){
    
    if(obj_sel == null){
      if(mouseX >= 0 && mouseX <  pg_pick.width && mouseY >= 0 && mouseY <  pg_pick.height) {
        int idx_curr = pg_pick.pixels[mouseY * pg_pick.width + mouseX];
        
        if(idx_prev != -1){
          scene_objects[idx_prev].shp_render.setFill(col_prev);
          idx_prev = -1;
        }
        
        // no mouse-over, just return
        if(idx_curr == 0){
          return;
        }
  
        // mouse-over, change fill color
        idx_curr &= 0x00FFFFFF;
        col_prev =  scene_objects[idx_curr].shp_render.getFill(0);
        scene_objects[idx_curr].shp_render.setFill(color(255, 64, 0));
        idx_prev = idx_curr;
        
        // mouse-over AND selection mode is active -> keep the object, and
        // keep a backup of its transformation matrix
        if(SELECT_OBJECT){
          obj_sel = scene_objects[idx_curr];
          obj_mat.set(obj_sel.mat);
          obj_off = null;
        }
      } 
    }
    
    // object selected and ready to be moved
    if(obj_sel != null){
      PGraphics3D pg_canvas = (PGraphics3D) this.g;
      
      // build object matrix
      PMatrix3D mvp = pg_canvas.projmodelview.get();
      mvp.apply(obj_mat);
      
      transform.useCurrentTransformationMatrix(pg_canvas, mvp);

      // transform object to screen-coords
      float[] screen = new float[4];
      float[] world  = new float[4];
      transform.worldToScreen(world, screen);
      
      // respect mouse-offset (to object center)
      if(obj_off == null){
        obj_off = new float[2];
        obj_off[0] = mouseX - screen[0];
        obj_off[1] = mouseY - screen[1];
      }
      
      // transform object (new screen-coords!) back to world-coords
      screen[0] = mouseX - obj_off[0];
      screen[1] = mouseY - obj_off[1];
      transform.screenToWorld(screen, world);

      // modify object matrix
      obj_sel.mat.set(obj_mat);
      obj_sel.mat.translate(world[0], world[1], world[2]);
      obj_sel.udpateShapesTransform();
    }
  }
  
  
  
 
  boolean MOVE_OBJECT = false;
  boolean SELECT_OBJECT = false;

  public void keyPressed(){
    if(key == 'w'){
      MOVE_OBJECT = true;
    }
  }
  
  public void keyReleased(){
    MOVE_OBJECT = false;
  }
  
  public void mousePressed(){
    SELECT_OBJECT = MOVE_OBJECT;
  }
  
  public void mouseReleased(){
    SELECT_OBJECT = false;
    obj_sel = null;
  }
  
  
  
  static class SceneObject{
    
    PShape shp_render;
    PShape shp_pick;
    PMatrix3D mat = new PMatrix3D();

    public void udpateShapesTransform(){
      shp_render.resetMatrix();
      shp_render.applyMatrix(mat);  
      shp_pick.resetMatrix();
      shp_pick.applyMatrix(mat);
    }
  }
  
  

  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { PickAndMove.class.getName() });
  }
}