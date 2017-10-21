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

package Skylight.Skylight_BulletPhysics_TowerDemolition;

import java.io.File;
import java.util.Locale;


import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;
import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DepthOfField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwScreenSpaceGeometryBuffer;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;
import com.thomasdiewald.pixelflow.java.utils.DwFrameCapture;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import peasy.*;
import bRigid.*;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;

public class Skylight_BulletPhysics_TowerDemolition extends PApplet {
  
  //
  // author: Thomas Diewald
  //
  // video: https://vimeo.com/218351202
  //
  //
  // This Example shows how to combine the PixelFlow Skylight-Renderer and 
  // Bullet-Physics.
  //
  // Features:
  //
  // - Rigid Body Simulation - bRigid, Bullet Physics
  // - Skylight Renderer, Sun + AO
  // - DoF
  // - Bloom
  // - SMAA
  // - shooting
  // - ...
  //
  // required Libraries to run this example (PDE contribution manager):
  //
  // - PeasyCam
  //   library for camera control, by Jonathan Feinberg
  //   http://mrfeinberg.com/peasycam/
  //
  // - bRigid (jBullet-Physics for Processing),
  //   library for rigid body simulation, by Daniel Koehler
  //   http://www.lab-eds.org/bRigid
  //
  // - PixelFlow
  //   library for skylight, post-fx, lots of GLSL, etc..., by Thomas Diewald
  //   https://github.com/diwi/PixelFlow
  //
  //

  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;

  // Camera
  PeasyCam cam;
  
  // Bullet Physics
  BPhysics physics;
  
  // Bullet bodies, group-shape
  PShape group_bulletbodies;
  
  // PixelFlow Context
  DwPixelFlow context;
  
  // PixelFlow Filter, for post fx
  DwFilter filter;
  
  // some render-targets
  PGraphics3D pg_render;
  PGraphics3D pg_aa;
  
  // SkyLight Renderer
  DwSkyLight skylight;
  PMatrix3D mat_scene_view;
  PMatrix3D mat_scene_bounds;
  
  // AntiAliasing - SMAA
  SMAA smaa;

  // Depth of Field - DoF
  DepthOfField dof;
  DwScreenSpaceGeometryBuffer geombuffer;
  PGraphics3D pg_tmp;
  
  PFont font12, font96;
  
  
  DwFrameCapture capture;
  
  // switches
  public boolean UPDATE_PHYSICS  = true;
  public boolean APPLY_DOF       = true;
  public boolean APPLY_BLOOM     = true;
  public boolean DISPLAY_WIREFRAME = false;
  
  
  public int BUILDING = 0;

  public void settings() {
    size(viewport_w, viewport_h, P3D);
    smooth(0);
  }

  public void setup() {
    
    surface.setLocation(viewport_x, viewport_y);
    
    float SCENE_SCALE = 1000;
    
    capture = new DwFrameCapture(this, "examples/");
    
    font12 = createFont("../data/SourceCodePro-Regular.ttf", 12);
    font96 = createFont("../data/SourceCodePro-Regular.ttf", 32);
    
    cam = new PeasyCam(this, 0, 0, 0, SCENE_SCALE);
    perspective(60 * DEG_TO_RAD, width/(float)height, 2, SCENE_SCALE * 250);

    group_bulletbodies = createShape(GROUP);
    
//    Vector3f min = new Vector3f(-300, -300,    0);
//    Vector3f max = new Vector3f(+300, +300, +1000);
//    physics = new BPhysics(min, max);
    physics = new BPhysics(); // no bounding box
    physics.world.setGravity(new Vector3f(0, 0, -30));
   
    pg_render = (PGraphics3D) createGraphics(width, height, P3D);
    pg_render.smooth(0);
    pg_render.beginDraw();
    pg_render.endDraw();
    
    
    // compute scene bounding-sphere
    DwBoundingSphere scene_bs = new DwBoundingSphere();
    scene_bs.set(0, 0, 200, 450);
    PMatrix3D mat_bs = scene_bs.getUnitSphereMatrix();

    // matrix, to place (centering, scaling) the scene in the viewport
    mat_scene_view = new PMatrix3D();
    mat_scene_view.scale(SCENE_SCALE);
    mat_scene_view.apply(mat_bs);

    // matrix, to place the scene in the skylight renderer
    mat_scene_bounds = mat_scene_view.get();
    mat_scene_bounds.invert();
    mat_scene_bounds.preApply(mat_bs);

    // callback for rendering the scene
    DwSceneDisplay scene_display = new DwSceneDisplay(){
      @Override
      public void display(PGraphics3D canvas) {
        displayScene(canvas);  
      }
    };
    
    // library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // postprocessing filters
    filter = DwFilter.get(context);
    
    // init skylight renderer
    skylight = new DwSkyLight(context, scene_display, mat_scene_bounds);
    
    // parameters for sky-light
    skylight.sky.param.iterations     = 50;
    skylight.sky.param.solar_azimuth  = 0;
    skylight.sky.param.solar_zenith   = 0;
    skylight.sky.param.sample_focus   = 1; // full sphere sampling
    skylight.sky.param.intensity      = 1.0f;
    skylight.sky.param.rgb            = new float[]{1,1,1};
    skylight.sky.param.shadowmap_size = 512; // quality vs. performance
    
    // parameters for sun-light
    skylight.sun.param.iterations     = 50;
    skylight.sun.param.solar_azimuth  = 35;
    skylight.sun.param.solar_zenith   = 65;
    skylight.sun.param.sample_focus   = 0.1f;
    skylight.sun.param.intensity      = 1.0f;
    skylight.sun.param.rgb            = new float[]{1,1,1};
    skylight.sun.param.shadowmap_size = 512;
    
    // postprocessing AA
    smaa = new SMAA(context);
    pg_aa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_aa.smooth(0);
    pg_aa.textureSampling(5);
    
    
    dof = new DepthOfField(context);
    geombuffer = new DwScreenSpaceGeometryBuffer(context, scene_display);
    
    pg_tmp = (PGraphics3D) createGraphics(width, height, P3D);
    pg_tmp.smooth(0);
    DwUtils.changeTextureFormat(pg_tmp, GL2.GL_RGBA16F, GL2.GL_RGBA, GL2.GL_FLOAT);
    
    // fresh start
    reset();
    createBuildings(BUILDING);
    frameRate(60);
  }
  


  public void draw() {
    
    // handle bullet physics update, etc...
    if(UPDATE_PHYSICS){
      physics.update();
      
      removeLostBodies();
      
      for (BObject body : physics.rigidBodies) {
        updateShapes( body);
      }
    }
    
   
    // when the camera moves, the renderer restarts
    updateCamActiveStatus();
    if(CAM_ACTIVE || UPDATE_PHYSICS){
      skylight.reset();
    }

    // update renderer
    skylight.update();
    

    // apply AntiAliasing
    smaa.apply(skylight.renderer.pg_render, pg_aa);
    
    // apply bloom
    if(APPLY_BLOOM){
      filter.bloom.param.mult   = 0.15f; //map(mouseX, 0, width, 0, 1);
      filter.bloom.param.radius = 0.5f; // map(mouseY, 0, height, 0, 1);
      filter.bloom.apply(pg_aa, null, pg_aa);
    }  
    
    // apply DoF
    if(APPLY_DOF){
      int mult_blur = 5;
      
      geombuffer.update(skylight.renderer.pg_render);
      
      filter.gaussblur.apply(geombuffer.pg_geom, geombuffer.pg_geom, pg_tmp, mult_blur);

      dof.param.focus_pos = new float[]{0.5f, 0.5f};
//      dof.param.focus_pos[0] =   map(mouseX, 0, width , 0, 1);
//      dof.param.focus_pos[1] = 1-map(mouseY, 0, height, 0, 1);
      dof.param.mult_blur = mult_blur;
      dof.apply(pg_aa, pg_render, geombuffer);
      filter.copy.apply(pg_render, pg_aa);
    }
    
    // display result
    cam.beginHUD();
    {
      background(255);
      noLights();
      image(pg_aa, 0, 0);
      
      displayCross();
      
      displayHUD();
      
      displayModelChange();
    }
    cam.endHUD();
    
    // info
    String txt_fps = String.format(getClass().getName()+ "  [fps %6.2f]  [bodies %d]", frameRate, physics.rigidBodies.size());
    surface.setTitle(txt_fps);
  }
  
  public void displayCross(){
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
  
  
  int building_prev = -1;
  float alpha = 255;
  public void displayModelChange(){
    alpha *= 1.08;
    if(building_prev != BUILDING){
      building_prev = BUILDING;
      alpha = 1;
    }
      
      if(alpha < 255){
      textFont(font96);
  //    textMode(SCREEN);
      
      fill(0, 255-alpha);
      textAlign(CENTER, CENTER);
      text("Model "+BUILDING, width/2+2, height - 50+2);
      
      fill(255, 255-alpha);
      textAlign(CENTER, CENTER);
      text("Model "+BUILDING, width/2, height - 50);
    }
  }

  
  public void displayHUD(){
    
    String txt_fps            = String.format(Locale.ENGLISH, "fps: %6.2f", frameRate);
    String txt_num_bodies     = String.format(Locale.ENGLISH, "rigid bodies: %d", physics.rigidBodies.size());
    String txt_samples_sky    = String.format(Locale.ENGLISH, "sky/sun: %d/%d (samples)", skylight.sky.param.iterations,  skylight.sun.param.iterations);
 
    String txt_model          = String.format(Locale.ENGLISH, "[1-9] model: %d", BUILDING);
    String txt_reset          = String.format(Locale.ENGLISH, "[r] reset");
    String txt_update_physics = String.format(Locale.ENGLISH, "[p] physics:   %b", UPDATE_PHYSICS);
    String txt_apply_bloom    = String.format(Locale.ENGLISH, "[q] bloom:     %b", APPLY_BLOOM);
    String txt_apply_dof      = String.format(Locale.ENGLISH, "[w] DoF:       %b", APPLY_DOF);
    String txt_wireframe      = String.format(Locale.ENGLISH, "[e] wireframe: %b", DISPLAY_WIREFRAME);
    String txt_shoot          = String.format(Locale.ENGLISH, "[ ] shoot");

    int tx, ty, sy;
    tx = 10;
    ty = 10;
    sy = 13;
    
    fill(0, 100);
    noStroke();
    stroke(0, 200);
    rectMode(CORNER);
    rect(5, 5, 200, 170);
    
    textAlign(LEFT, BASELINE );
    textFont(font12);
//    textMode(SCREEN);
    fill(220);
    text(txt_fps            , tx, ty+=sy);
    text(txt_num_bodies     , tx, ty+=sy);
    text(txt_samples_sky    , tx, ty+=sy);
    ty+=sy;
    text(txt_model          , tx, ty+=sy);
    text(txt_reset          , tx, ty+=sy);
    text(txt_update_physics , tx, ty+=sy);
    text(txt_apply_bloom    , tx, ty+=sy);
    text(txt_apply_dof      , tx, ty+=sy);
    text(txt_wireframe      , tx, ty+=sy);
    text(txt_shoot          , tx, ty+=sy);

  }
  
  
  
  // reset scene
  public void reset(){
    
    // remove bodies
    for(int i = physics.rigidBodies.size() - 1; i >= 0; i--){
      BObject body = physics.rigidBodies.get(i);
      physics.removeBody(body);
    }
    
    // just in case, i am actually not not sure if PShape really needs this to 
    // avoid memoryleaks.
    for(int i = group_bulletbodies.getChildCount() - 1; i >= 0; i--){
      group_bulletbodies.removeChild(i);
    }

    
    addGround();
  }
  
  
  
  // bodies that have fallen outside of the scene can be removed
  public void removeLostBodies(){
    for(int i = physics.rigidBodies.size() - 1; i >= 0; i--){
      BObject body = physics.rigidBodies.get(i);
      Vector3f pos = body.getPosition();
      
      if(pos.z < -1000){
        int idx = group_bulletbodies.getChildIndex(body.displayShape);
        if(idx >= 0){
          group_bulletbodies.removeChild(idx);
        }
        physics.removeBody(body);
      }
    }
  }
  
  
  
  // shoot body into the scene
  int shooter_count = 0;
  PMatrix3D mat_mvp     = new PMatrix3D();
  PMatrix3D mat_mvp_inv = new PMatrix3D();
  
  public void addShootingBody(){

    PGraphics3D pg = (PGraphics3D) skylight.renderer.pg_render;
    mat_mvp.set(pg.modelview);
    mat_mvp.apply(mat_scene_view);
    mat_mvp_inv.set(mat_mvp);
    mat_mvp_inv.invert();
    
    float[] cam_start = {0, 0, -0, 1};
    float[] cam_aim   = {0, 0, -400, 1};
    float[] world_start = new float[4];
    float[] world_aim   = new float[4];
    mat_mvp_inv.mult(cam_start, world_start);
    mat_mvp_inv.mult(cam_aim, world_aim);
    
    Vector3f pos = new Vector3f(world_start[0], world_start[1], world_start[2]);
    Vector3f aim = new Vector3f(world_aim[0], world_aim[1], world_aim[2]);
    Vector3f dir = new Vector3f(aim);
    dir.sub(pos);
    dir.normalize();
    dir.scale(2000);

    float mass = 20000;
    float dimr = 80;
    
    BObject obj;
    
//    if((shooter_count % 2) == 0){
      obj = new BSphere(this, mass, 0, 0, 0, dimr*0.5f);
//    } else {
//      obj = new BBox(this, mass, dimr, dimr, dimr);
//    }
    BObject body = new BObject(this, mass, obj, pos, true);
    
    body.setPosition(pos);
    body.setVelocity(dir);
    body.setRotation(new Vector3f(random(-1, 1),random(-1, 1),random(-1, 1)), random(PI));

    body.rigidBody.setRestitution(0.9f);
    body.rigidBody.setFriction(1);
//    body.rigidBody.setHitFraction(1);
    body.rigidBody.setDamping(0.1f, 0.1f);
    
    body.displayShape.setStroke(false);
    body.displayShape.setFill(true);
    body.displayShape.setFill(color(255,8,0));
    body.displayShape.setStrokeWeight(1);
    body.displayShape.setStroke(color(0));
    if(obj instanceof BBox){
      fixBoxNormals(body.displayShape);
    }

    physics.addBody(body);
    group_bulletbodies.addChild(body.displayShape);
    
    body.displayShape.setName("[shooter_"+shooter_count+"] [wire]");
    shooter_count++;
  }
  

  // bRigid-bug: face 1 and 3, vertex order -> inverse normal
  private void fixBoxNormals(PShape box){
    PShape face;
    face = box.getChild(1);
    for(int i = 0; i < 4; i++){
      face.setNormal(i, -1, 0, 0);
    }
    face = box.getChild(3);
    for(int i = 0; i < 4; i++){
      face.setNormal(i, +1, 0, 0);
    }
  }
  



  
  public void createBuildings(int idx){
    
    BUILDING = min(max(idx, 0), 9);

    reset();
    
    float pos_z = 20f;
    
    int num_z = 30;
    int num_segs = 10;
    
    float radius = 120;
   

    float box_x = 35;
    float box_y = 0;
    float box_z = 20;
    
    if(BUILDING == 5){
      box_x = 3;
      box_z = 10;
      num_z = 60;
      radius = 50;
    }
    
    if(BUILDING == 6){
      box_x = 5;
      box_z = 25;
      num_z = 40;
      num_segs = 2;
      radius = 150;
    }
    
    
    float arc = TWO_PI / num_segs;
    float arc_len = 2 * radius * PI / num_segs;
    box_y = arc_len;
    
    float box_mass = box_x * box_y * box_z;

    float angle_start = 0;
    float posz_start = pos_z + box_z/2f;
    
    for(int id_z = 0; id_z < num_z; id_z++){
      for(int i = 0; i < num_segs; i++){
//        int id = id_z*num_segs + i;
        angle_start = (id_z % 2) * arc / 2;


        PMatrix3D mat_p5 = new PMatrix3D();
        switch(BUILDING){
        default:
        case(0):
        case(1):
        {
          mat_p5.rotate(angle_start + i * arc);
          mat_p5.translate(radius + box_x/2, 0, posz_start + box_z * id_z);
          break;
        }       
        case(2):
        {
          mat_p5.rotate(angle_start + i * arc);
          mat_p5.translate(radius + box_x/2, 0, posz_start + box_z * id_z);
          mat_p5.translate((sin(id_z*0.25f)+1)*40, 0, 0);
          break;
        }
        case(3):
        {
          mat_p5.translate(sin(id_z*0.24f)*50, sin(id_z*0.40f)*30, 0);
          mat_p5.rotate(angle_start + i * arc);
          mat_p5.translate(radius + box_x/2, 0, posz_start + box_z * id_z);
          break;
        }
        case(4):
        {
          mat_p5.rotate(angle_start + i * arc);
          mat_p5.translate(radius + box_x/2, 0, posz_start + box_z * id_z);
          if(id_z%2 == 0){
            mat_p5.rotate(PI/2);
          }
          break;
        }
 
        }
        
        Matrix4f mat = new Matrix4f();
        mat.setRow(0, mat_p5.m00, mat_p5.m01, mat_p5.m02, mat_p5.m03);
        mat.setRow(1, mat_p5.m10, mat_p5.m11, mat_p5.m12, mat_p5.m13);
        mat.setRow(2, mat_p5.m20, mat_p5.m21, mat_p5.m22, mat_p5.m23);
        mat.setRow(3, mat_p5.m30, mat_p5.m31, mat_p5.m32, mat_p5.m33);
        
        Transform transform = new Transform(mat);
 
        BObject obj = new BBox(this, box_mass, box_x, box_y, box_z);
        BObject body = new BObject(this, box_mass, obj, new Vector3f(0, 0, 0), true);
        
        body.rigidBody.setWorldTransform(transform);
//        body.rigidBody.setRestitution(.01f);
        body.rigidBody.setFriction(0.93f);
//        body.rigidBody.setDamping(0.2f, 0.2f);
        
        body.displayShape.setStroke(false);
        body.displayShape.setFill(true);
        body.displayShape.setFill(color(255));
        body.displayShape.setStrokeWeight(1);
        body.displayShape.setStroke(color(0));
        if(obj instanceof BBox){
          fixBoxNormals(body.displayShape);
        }
        physics.addBody(body);
        group_bulletbodies.addChild(body.displayShape);
        
        String wire = (id_z % 2) == 0 ? "[wire]" : "";
        body.displayShape.setName("[block"+id_z+"_"+i+"] "+wire);
      }
    }
  }

  // add ground bodies
  public void addGround(){
    {
      Vector3f pos = new Vector3f(0,0,10);
      BObject obj = new BBox(this, 0, 400, 400, 20);
      BObject body = new BObject(this, 0, obj, pos, true);
      
      body.setPosition(pos);
  
      body.displayShape.setStroke(false);
      body.displayShape.setFill(true);
      body.displayShape.setFill(color(200, 96, 16));
      body.displayShape.setStrokeWeight(1);
      body.displayShape.setStroke(color(0));
      if(obj instanceof BBox){
        fixBoxNormals(body.displayShape);
      }
      physics.addBody(body);
      group_bulletbodies.addChild(body.displayShape);
      body.displayShape.setName("ground_box");
    }
    
    {
      Vector3f pos = new Vector3f(0,0,0);
      BObject obj = new BBox(this, 0, 650, 650, 5);
      BObject body = new BObject(this, 0, obj, pos, true);
      
      body.setPosition(pos);
  
      body.displayShape.setStroke(false);
      body.displayShape.setFill(true);
      body.displayShape.setFill(color(96));
      body.displayShape.setStrokeWeight(1);
      body.displayShape.setStroke(color(0));
      if(obj instanceof BBox){
        fixBoxNormals(body.displayShape);
      }
      physics.addBody(body);
      group_bulletbodies.addChild(body.displayShape);
      body.displayShape.setName("ground_plane");
    }
    
  }
  
  
  
  // toggle shading/wireframe display
  public void toggleDisplayWireFrame(){
    DISPLAY_WIREFRAME = !DISPLAY_WIREFRAME;
    for (BObject body : physics.rigidBodies) {
      PShape shp = body.displayShape;
      String name = shp.getName();
      if(name != null && name.contains("[wire]")){
        shp.setFill(!DISPLAY_WIREFRAME);
        shp.setStroke(DISPLAY_WIREFRAME);
      }
    }
    skylight.reset();
  }
  
  

  // render scene
  public void displayScene(PGraphics3D pg){
    if(pg == skylight.renderer.pg_render){
      pg.background(16);
    }
    
    if(pg == geombuffer.pg_geom){
      pg.background(255, 255);
      pg.pgl.clearColor(1, 1, 1, 6000);
      pg.pgl.clear(PGL.COLOR_BUFFER_BIT);
    }
    
    pg.pushMatrix();
    pg.applyMatrix(mat_scene_view);
    pg.shape(group_bulletbodies);
    pg.popMatrix();
  }
  
  
  // update PShape matrices
  Transform transform = new Transform();
  Matrix4f out = new Matrix4f();
  
  public void updateShapes(BObject body){
    if (body.displayShape != null) {
      body.displayShape.resetMatrix();
      if (body.getMass() < 0) {
        transform = body.rigidBody.getMotionState().getWorldTransform(transform);
        out = transform.getMatrix(out);
        body.transform.set(transform);
        body.displayShape.applyMatrix(out.m00, out.m01, out.m02, out.m03, out.m10, out.m11, out.m12, out.m13, out.m20, out.m21, out.m22, out.m23, out.m30, out.m31, out.m32, out.m33);
      } else {
        transform = body.rigidBody.getWorldTransform(transform);
//        body.transform.set(transform);
        body.displayShape.translate(transform.origin.x, transform.origin.y, transform.origin.z);
      }
    }
  }
  
  
  
  // check if camera is moving
  float[] cam_pos = new float[3];
  boolean CAM_ACTIVE = false;
  public void updateCamActiveStatus(){
    float[] cam_pos_curr = cam.getPosition();
    CAM_ACTIVE = false;
    CAM_ACTIVE |= cam_pos_curr[0] != cam_pos[0];
    CAM_ACTIVE |= cam_pos_curr[1] != cam_pos[1];
    CAM_ACTIVE |= cam_pos_curr[2] != cam_pos[2];
    cam_pos = cam_pos_curr;
  }
  
  
  
  public void keyReleased(){
    if(key == 'p') UPDATE_PHYSICS = !UPDATE_PHYSICS;
    if(key == 'q') APPLY_BLOOM = !APPLY_BLOOM;
    if(key == 'w') APPLY_DOF = !APPLY_DOF;
    if(key == 'e') toggleDisplayWireFrame();
    if(key == 'r') createBuildings(BUILDING);
    if(key == ' ') addShootingBody();
    if(key == 's') saveScreenshot();
    if(key >= '0' && key <= '9') createBuildings(key-'0');
  }
  
  

  
  public void saveScreenshot(){
    File file = capture.createFilename();
//    pg_aa.save(file.getAbsolutePath());
    save(file.getAbsolutePath());
    System.out.println(file.getAbsolutePath());
  }


  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_BulletPhysics_TowerDemolition.class.getName() });
  }
}
