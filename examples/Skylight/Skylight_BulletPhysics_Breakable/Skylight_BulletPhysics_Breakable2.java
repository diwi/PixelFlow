///**
// * 
// * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
// * 
// * src  - www.github.com/diwi/PixelFlow
// * 
// * A Processing/Java library for high performance GPU-Computing.
// * MIT License: https://opensource.org/licenses/MIT
// * 
// */
//
//package Skylight.Skylight_BulletPhysics_Breakable;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//
//
//import javax.vecmath.Matrix4f;
//import javax.vecmath.Vector3f;
//
//import com.bulletphysics.collision.broadphase.Dispatcher;
//import com.bulletphysics.collision.narrowphase.ManifoldPoint;
//import com.bulletphysics.collision.narrowphase.PersistentManifold;
//import com.bulletphysics.dynamics.RigidBody;
//import com.bulletphysics.linearmath.Transform;
//import com.bulletphysics.util.ObjectArrayList;
//import com.jogamp.opengl.GL2;
//
//import peasy.*;
//import bRigid.*;
//import wblut.geom.WB_Coord;
//import wblut.geom.WB_Point;
//import wblut.geom.WB_Polygon;
//import wblut.geom.WB_Voronoi;
//import wblut.geom.WB_VoronoiCell2D;
//import com.thomasdiewald.pixelflow.java.DwPixelFlow;
//import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
//import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DepthOfField;
//import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
//import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
//import com.thomasdiewald.pixelflow.java.render.skylight.DwScreenSpaceGeometryBuffer;
//import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
//import com.thomasdiewald.pixelflow.java.sampling.DwSampling;
//import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;
//import com.thomasdiewald.pixelflow.java.utils.DwFrameCapture;
//import com.thomasdiewald.pixelflow.java.utils.DwUtils;
//
//import processing.core.PApplet;
//import processing.core.PFont;
//import processing.core.PMatrix3D;
//import processing.core.PShape;
//import processing.opengl.PGL;
//import processing.opengl.PGraphics3D;
//
//
//
//public class Skylight_BulletPhysics_Breakable2 extends PApplet {
//  
//  //
//  // author: Thomas Diewald
//  //
//  //
//  // This Example shows how to combine the PixelFlow Skylight-Renderer and 
//  // Bullet-Physics. Rigid Bodies are created from a Voronoi-Tesselation.
//  //
//  // Features:
//  //
//  // - CellFracture from Voronoi-Tesselation - HE_Mesh
//  // - Rigid Body Simulation - bRigid, Bullet Physics
//  // - Skylight Renderer, Sun + AO
//  // - DoF
//  // - Bloom
//  // - SMAA
//  // - shooting
//  // - ...
//  //
//  // required Libraries to run this example (PDE contribution manager):
//  //
//  // - PeasyCam
//  //   library for camera control, by Jonathan Feinberg
//  //   http://mrfeinberg.com/peasycam/
//  //
//  // - bRigid (jBullet-Physics for Processing),
//  //   library for rigid body simulation, by Daniel Koehler
//  //   http://www.lab-eds.org/bRigid
//  //
//  // - HE_Mesh
//  //   library for creating and manipulating polygonal meshes, by Frederik Vanhoutte
//  //   https://github.com/wblut/HE_Mesh (manual installation)
//  //
//  // - PixelFlow
//  //   library for skylight, post-fx, lots of GLSL, etc..., by Thomas Diewald
//  //   https://github.com/diwi/PixelFlow
//  //
//  //
//
//
//  
//  int viewport_w = 1280;
//  int viewport_h = 720;
//  int viewport_x = 230;
//  int viewport_y = 0;
//
//  // Camera
//  PeasyCam cam;
//  
//  // Bullet Physics
//  MyBPhysics physics;
//  
//  // Bullet bodies, group-shape
//  PShape group_bulletbodies;
//  
//  // PixelFlow Context
//  DwPixelFlow context;
//  
//  // PixelFlow Filter, for post fx
//  DwFilter filter;
//  
//  // some render-targets
//  PGraphics3D pg_render;
//  PGraphics3D pg_aa;
//  
//  // SkyLight Renderer
//  DwSkyLight skylight;
//  PMatrix3D mat_scene_view;
//  PMatrix3D mat_scene_bounds;
//  
//  // AntiAliasing - SMAA
//  SMAA smaa;
//
//  // Depth of Field - DoF
//  DepthOfField dof;
//  DwScreenSpaceGeometryBuffer geombuffer;
//  PGraphics3D pg_tmp;
//  
//  PFont font12;
//  
//  
//  DwFrameCapture capture;
//  
//  // switches
//  public boolean UPDATE_PHYSICS    = true;
//  public boolean APPLY_DOF         = true;
//  public boolean APPLY_BLOOM       = true;
//  public boolean DISPLAY_WIREFRAME = false;
//  
//  public void settings() {
//    size(viewport_w, viewport_h, P3D);
//    smooth(0);
//  }
//
//  public void setup() {
//    
//    surface.setLocation(viewport_x, viewport_y);
//    
//    float SCENE_SCALE = 1000;
//    
//    // for screenshot
//    capture = new DwFrameCapture(this, "examples/");
//    
//    font12 = createFont("../data/SourceCodePro-Regular.ttf", 12);
//
//    cam = new PeasyCam(this, 0, 0, 0, SCENE_SCALE);
//    perspective(60 * DEG_TO_RAD, width/(float)height, 2, SCENE_SCALE * 250);
//
//    group_bulletbodies = createShape(GROUP);
//    
//    physics = new MyBPhysics(); // no bounding box
//    physics.world.setGravity(new Vector3f(0, 0, -100));
//   
//    pg_render = (PGraphics3D) createGraphics(width, height, P3D);
//    pg_render.smooth(0);
//    pg_render.beginDraw();
//    pg_render.endDraw();
//    
//    
//    // compute scene bounding-sphere
//    DwBoundingSphere scene_bs = new DwBoundingSphere();
//    scene_bs.set(0, 0, 200, 450);
//    PMatrix3D mat_bs = scene_bs.getUnitSphereMatrix();
//
//    // matrix, to place (centering, scaling) the scene in the viewport
//    mat_scene_view = new PMatrix3D();
//    mat_scene_view.scale(SCENE_SCALE);
//    mat_scene_view.apply(mat_bs);
//
//    // matrix, to place the scene in the skylight renderer
//    mat_scene_bounds = mat_scene_view.get();
//    mat_scene_bounds.invert();
//    mat_scene_bounds.preApply(mat_bs);
//
//    // callback for rendering the scene
//    DwSceneDisplay scene_display = new DwSceneDisplay(){
//      @Override
//      public void display(PGraphics3D canvas) {
//        displayScene(canvas);  
//      }
//    };
//    
//    // library context
//    context = new DwPixelFlow(this);
//    context.print();
//    context.printGL();
//    
//    // postprocessing filters
//    filter = DwFilter.get(context);
//    
//    // init skylight renderer
//    skylight = new DwSkyLight(context, scene_display, mat_scene_bounds);
//    
//    // parameters for sky-light
//    skylight.sky.param.iterations     = 50;
//    skylight.sky.param.solar_azimuth  = 0;
//    skylight.sky.param.solar_zenith   = 0;
//    skylight.sky.param.sample_focus   = 1; // full sphere sampling
//    skylight.sky.param.intensity      = 1.0f;
//    skylight.sky.param.rgb            = new float[]{1,1,1};
//    skylight.sky.param.shadowmap_size = 512; // quality vs. performance
//    
//    // parameters for sun-light
//    skylight.sun.param.iterations     = 50;
//    skylight.sun.param.solar_azimuth  = 35;
//    skylight.sun.param.solar_zenith   = 65;
//    skylight.sun.param.sample_focus   = 0.1f;
//    skylight.sun.param.intensity      = 1.0f;
//    skylight.sun.param.rgb            = new float[]{1,1,1};
//    skylight.sun.param.shadowmap_size = 512;
//    
//    // postprocessing AA
//    smaa = new SMAA(context);
//    pg_aa = (PGraphics3D) createGraphics(width, height, P3D);
//    pg_aa.smooth(0);
//    pg_aa.textureSampling(5);
//    
//    
//    dof = new DepthOfField(context);
//    geombuffer = new DwScreenSpaceGeometryBuffer(context, scene_display);
//    
//    pg_tmp = (PGraphics3D) createGraphics(width, height, P3D);
//    pg_tmp.smooth(0);
//    DwUtils.changeTextureFormat(pg_tmp, GL2.GL_RGBA16F, GL2.GL_RGBA, GL2.GL_FLOAT);
//
//    // fresh start
//    reset();
//    
//    createFractureShape();
//    
//    frameRate(60);
//  }
//  
//
//
//  
//  boolean slow = true;
//  
//  
//  PShape group_collisions;
//  
//  
//  public void getCollisionPoints() {
//    
//    if(group_collisions == null){
//      group_collisions = createShape(GROUP);
//    }
//    
//    
//    Dispatcher dispatcher = physics.world.getDispatcher();
//
//    int numManifolds = dispatcher.getNumManifolds();
//
//    for (int i = 0; i < numManifolds; i++) {
//      PersistentManifold contactManifold = dispatcher.getManifoldByIndexInternal(i);
//      int numCon = contactManifold.getNumContacts();
//      
//      RigidBody rA = (RigidBody) contactManifold.getBody0();
//      RigidBody rB = (RigidBody) contactManifold.getBody1();
//
//      BObject bodyA = (BObject) rA.getUserPointer();
//      if(bodyA != null){
//        System.out.println(bodyA.displayShape.getName());
//      }
//      
//      BObject bodyB = (BObject)rB.getUserPointer();
//      if(bodyB != null){
//        System.out.println(bodyB.displayShape.getName());
//      }
//      
//
//      for (int j = 0; j < numCon; j++) {
//        Vector3f pos0 = new Vector3f();
//        ManifoldPoint p0 = contactManifold.getContactPoint(j);
//        
//        p0.getPositionWorldOnA(pos0);        
////        point(pos0.x, pos0.y, pos0.z);
//        
//        PShape shp_coll = createShape(SPHERE, 2);
//        shp_coll.setStroke(false);
//        shp_coll.setFill(true);
//        shp_coll.setFill(color(255,0,0));
//        shp_coll.translate(pos0.x, pos0.y, pos0.z);
//        group_collisions.addChild(shp_coll);
//        
//      }
//    }
//  }
//  
//
//  public void draw() {
//    
//    // handle bullet physics update, etc...
//    if(UPDATE_PHYSICS){
//
//      physics.update();
////      physics.update(30);
//      
//      getCollisionPoints();
//      
//      
//      
//      removeLostBodies();
//      
//      for (BObject body : physics.rigidBodies) {
//        updateShapes(body);
//      }
//      
//     
//      
//    }
//    
// 
//    // when the camera moves, the renderer restarts
//    updateCamActiveStatus();
//    if(CAM_ACTIVE || UPDATE_PHYSICS){
//      skylight.reset();
//    }
//
//    // update renderer
//    skylight.update();
//    
//
//    // apply AntiAliasing
//    smaa.apply(skylight.renderer.pg_render, pg_aa);
//    
//    // apply bloom
//    if(APPLY_BLOOM){
//      filter.bloom.param.mult   = 0.15f; //map(mouseX, 0, width, 0, 1);
//      filter.bloom.param.radius = 0.5f; // map(mouseY, 0, height, 0, 1);
//      filter.bloom.apply(pg_aa, null, pg_aa);
//    }  
//    
//    // apply DoF
//    if(APPLY_DOF){
//      int mult_blur = 5;
//      
//      geombuffer.update(skylight.renderer.pg_render);
//      
//      filter.gaussblur.apply(geombuffer.pg_geom, geombuffer.pg_geom, pg_tmp, mult_blur);
//
//      dof.param.focus_pos = new float[]{0.5f, 0.5f};
////      dof.param.focus_pos[0] =   map(mouseX, 0, width , 0, 1);
////      dof.param.focus_pos[1] = 1-map(mouseY, 0, height, 0, 1);
//      dof.param.mult_blur = mult_blur;
//      dof.apply(pg_aa, pg_render, geombuffer);
//      filter.copy.apply(pg_render, pg_aa);
//    }
//    
//    // display result
//    cam.beginHUD();
//    {
//      background(255);
//      noLights();
//      image(pg_aa, 0, 0);
//      
//      displayCross();
//      
//      displayHUD();
//    }
//    cam.endHUD();
//    
//    // info
//    String txt_fps = String.format(getClass().getName()+ "  [fps %6.2f]  [bodies %d]", frameRate, physics.rigidBodies.size());
//    surface.setTitle(txt_fps);
//  }
//  
//  
//  
//  public void displayCross(){
//    pushMatrix();
//    float cursor_s = 10;
//    float fpx = (       dof.param.focus_pos[0]) * width;
//    float fpy = (1.0f - dof.param.focus_pos[1]) * height;
//    blendMode(EXCLUSION);
//    translate(fpx, fpy);
//    strokeWeight(1);
//    stroke(255,200);
//    line(-cursor_s, 0, +cursor_s, 0);
//    line(0, -cursor_s, 0, +cursor_s);
//    blendMode(BLEND);
//    popMatrix();
//  }
//
//  
//  
//  
//  public void displayHUD(){
//    
//    String txt_fps            = String.format(Locale.ENGLISH, "fps: %6.2f", frameRate);
//    String txt_num_bodies     = String.format(Locale.ENGLISH, "rigid bodies: %d", physics.rigidBodies.size());
//    String txt_samples_sky    = String.format(Locale.ENGLISH, "sky/sun: %d/%d (samples)", skylight.sky.param.iterations,  skylight.sun.param.iterations);
// 
////    String txt_model          = String.format(Locale.ENGLISH, "[1-9] model: %d", BUILDING);
//    String txt_reset          = String.format(Locale.ENGLISH, "[r] reset");
//    String txt_update_physics = String.format(Locale.ENGLISH, "[t] physics:   %b", UPDATE_PHYSICS);
//    String txt_apply_bloom    = String.format(Locale.ENGLISH, "[q] bloom:     %b", APPLY_BLOOM);
//    String txt_apply_dof      = String.format(Locale.ENGLISH, "[w] DoF:       %b", APPLY_DOF);
//    String txt_wireframe      = String.format(Locale.ENGLISH, "[e] wireframe: %b", DISPLAY_WIREFRAME);
//    String txt_shoot          = String.format(Locale.ENGLISH, "[ ] shoot");
//
//    int tx, ty, sy;
//    tx = 10;
//    ty = 10;
//    sy = 13;
//    
//    fill(0, 100);
//    noStroke();
//    stroke(0, 200);
//    rectMode(CORNER);
//    rect(5, 5, 200, 170);
//    
//    textFont(font12);
////    textMode(SCREEN);
//    fill(220);
//    text(txt_fps            , tx, ty+=sy);
//    text(txt_num_bodies     , tx, ty+=sy);
//    text(txt_samples_sky    , tx, ty+=sy);
//    ty+=sy;
////    text(txt_model          , tx, ty+=sy);
//    text(txt_reset          , tx, ty+=sy);
//    text(txt_update_physics , tx, ty+=sy);
//    text(txt_apply_bloom    , tx, ty+=sy);
//    text(txt_apply_dof      , tx, ty+=sy);
//    text(txt_wireframe      , tx, ty+=sy);
//    text(txt_shoot          , tx, ty+=sy);
//
//  }
//  
//  
//  
//  // reset scene
//  public void reset(){
//    // remove bodies
//    for(int i = physics.rigidBodies.size() - 1; i >= 0; i--){
//      BObject body = physics.rigidBodies.get(i);
//      physics.removeBody(body);
//    }
//    
//    // just in case, i am actually not not sure if PShape really needs this to 
//    // avoid memoryleaks.
//    for(int i = group_bulletbodies.getChildCount() - 1; i >= 0; i--){
//      group_bulletbodies.removeChild(i);
//    }
//
//    addGround();
//  }
//  
//
//  // bodies that have fallen outside of the scene can be removed
//  public void removeLostBodies(){
//    for(int i = physics.rigidBodies.size() - 1; i >= 0; i--){
//      BObject body = physics.rigidBodies.get(i);
//      Vector3f pos = body.getPosition();
//      
//      if(pos.z < -1000){
//        int idx = group_bulletbodies.getChildIndex(body.displayShape);
//        if(idx >= 0){
//          group_bulletbodies.removeChild(idx);
//        }
//        physics.removeBody(body);
//      }
//    }
//  }
//  
//  
//  // toggle shading/wireframe display
//  public void toggleDisplayWireFrame(){
//    DISPLAY_WIREFRAME = !DISPLAY_WIREFRAME;
//    for (BObject body : physics.rigidBodies) {
//      PShape shp = body.displayShape;
//      String name = shp.getName();
//      if(name != null && name.contains("[wire]")){
//        shp.setFill(!DISPLAY_WIREFRAME);
//        shp.setStroke(DISPLAY_WIREFRAME);
//      }
//    }
//    skylight.reset();
//  }
//  
//  
//  // check if camera is moving
//  float[] cam_pos = new float[3];
//  boolean CAM_ACTIVE = false;
//  public void updateCamActiveStatus(){
//    float[] cam_pos_curr = cam.getPosition();
//    CAM_ACTIVE = false;
//    CAM_ACTIVE |= cam_pos_curr[0] != cam_pos[0];
//    CAM_ACTIVE |= cam_pos_curr[1] != cam_pos[1];
//    CAM_ACTIVE |= cam_pos_curr[2] != cam_pos[2];
//    cam_pos = cam_pos_curr;
//  }
//  
//  
//
//  public void keyReleased(){
//    if(key == 't') UPDATE_PHYSICS = !UPDATE_PHYSICS;
//    if(key == 'q') APPLY_BLOOM = !APPLY_BLOOM;
//    if(key == 'w') APPLY_DOF = !APPLY_DOF;
//    if(key == 'e') toggleDisplayWireFrame();
//    if(key == 'r') createFractureShape();
//    if(key == ' ') addShootingBody();
//    if(key == 's') saveScreenshot();
//  }
//  
//
//  
//  
//
//  
//  // shoot body into the scene
//  int shooter_count = 0;
//  PMatrix3D mat_mvp     = new PMatrix3D();
//  PMatrix3D mat_mvp_inv = new PMatrix3D();
//  
//  public void addShootingBody(){
//    
//
//    float vel = 1000;
//    float mass = 100000;
//    float dimr = 30;
//    
//
//    PGraphics3D pg = (PGraphics3D) skylight.renderer.pg_render;
//    mat_mvp.set(pg.modelview);
//    mat_mvp.apply(mat_scene_view);
//    mat_mvp_inv.set(mat_mvp);
//    mat_mvp_inv.invert();
//    
//    float[] cam_start = {0, 0, -0, 1};
//    float[] cam_aim   = {0, 0, -400, 1};
//    float[] world_start = new float[4];
//    float[] world_aim   = new float[4];
//    mat_mvp_inv.mult(cam_start, world_start);
//    mat_mvp_inv.mult(cam_aim, world_aim);
//    
//    Vector3f pos = new Vector3f(world_start[0], world_start[1], world_start[2]);
//    Vector3f aim = new Vector3f(world_aim[0], world_aim[1], world_aim[2]);
//    Vector3f dir = new Vector3f(aim);
//    dir.sub(pos);
//    dir.normalize();
//    dir.scale(vel);
//
//    BObject obj;
//    
////    if((shooter_count % 2) == 0){
//      obj = new BSphere(this, mass, 0, 0, 0, dimr*0.5f);
////    } else {
////      obj = new BBox(this, mass, dimr, dimr, dimr);
////    }
//    BObject body = new BObject(this, mass, obj, pos, true);
//    
//    body.setPosition(pos);
//    body.setVelocity(dir);
//    body.setRotation(new Vector3f(random(-1, 1),random(-1, 1),random(-1, 1)), random(PI));
//
//    body.rigidBody.setRestitution(0.9f);
//    body.rigidBody.setFriction(1);
////    body.rigidBody.setHitFraction(1);
//    body.rigidBody.setDamping(0.1f, 0.1f);
//    body.rigidBody.setUserPointer(body);
//    
//    body.displayShape.setStroke(false);
//    body.displayShape.setFill(true);
//    body.displayShape.setFill(color(255,200,0));
//    body.displayShape.setStrokeWeight(1);
//    body.displayShape.setStroke(color(0));
//    body.displayShape.setName("bullet");
//    if(obj instanceof BBox){
//      fixBoxNormals(body.displayShape);
//    }
//
//    physics.addBody(body);
//    group_bulletbodies.addChild(body.displayShape);
//    
//    body.displayShape.setName("[shooter_"+shooter_count+"] [wire]");
//    shooter_count++;
//  }
//  
//
//  // bRigid-bug: face 1 and 3, vertex order -> inverse normal
//  private void fixBoxNormals(PShape box){
//    PShape face;
//    face = box.getChild(1);
//    for(int i = 0; i < 4; i++){
//      face.setNormal(i, -1, 0, 0);
//    }
//    face = box.getChild(3);
//    for(int i = 0; i < 4; i++){
//      face.setNormal(i, +1, 0, 0);
//    }
//  }
//  
//  
//  
//  
//  public PShape createCellShape(List<WB_Point> points, float dimz, Vector3f center_of_mass){
//
//    Vector3f com = center_of_mass;
//    int num_points = points.size();
//    float dimz_half = dimz*0.5f;
//    
//    PShape cell_top = createShape();
//    cell_top.beginShape(POLYGON);
//    cell_top.normal(0, 0, -1);
//    for(WB_Point vtx : points){
//      cell_top.vertex(vtx.xf()-com.x, vtx.yf()-com.y, +dimz_half-com.z);
//    }
//    cell_top.endShape(CLOSE);
//    
//    PShape cell_bot = createShape();
//    cell_bot.beginShape(POLYGON);
//    cell_bot.normal(0, 0, -1);
//    for(WB_Point vtx : points){
//      cell_bot.vertex(vtx.xf()-com.x, vtx.yf()-com.y, -dimz_half-com.z);
//    }
//    cell_bot.endShape(CLOSE);
//    
//    PShape cell_side = createShape();
//    cell_side.beginShape(QUADS);
//
//    for(int i = 0; i <= points.size(); i++){
//      WB_Point v0 = points.get((i+0)%num_points);
//      WB_Point v1 = points.get((i+1)%num_points);
//      float v0x = v0.xf();
//      float v0y = v0.yf();
//      float v1x = v1.xf();
//      float v1y = v1.yf();
//      
//      float dx = v1x - v0x;
//      float dy = v1y - v0y;
//      
//      float nx = +dy;
//      float ny = -dx;
//      float nz = 0;
//      float nn = sqrt(nx*nx + ny*ny);
//      nx /= nn;  
//      ny /= nn;  
//      
//      cell_side.normal(nx, ny, nz);
//      cell_side.vertex(v0x-com.x, v0y-com.y, +dimz_half-com.z);
//      cell_side.vertex(v0x-com.x, v0y-com.y, -dimz_half-com.z);
//      cell_side.vertex(v1x-com.x, v1y-com.y, -dimz_half-com.z);
//      cell_side.vertex(v1x-com.x, v1y-com.y, +dimz_half-com.z);
//     
//    }
//
//    cell_side.endShape();
//    
//    
//    PShape cell = createShape(GROUP);
//    cell.addChild(cell_top);
//    cell.addChild(cell_bot);
//    cell.addChild(cell_side);
//    
//    float r = voronoi_col[0];
//    float g = voronoi_col[1];
//    float b = voronoi_col[2];
//    
//    cell.setFill(color(r,g,b));
//    cell.setFill(true);
//    cell.setStrokeWeight(1f);
//    cell.setStroke(color(r,g,b,96));
//    cell.setStroke(false);
//    
//    cell.setName("[wire]");
//    
//    return cell;
//  }
//  
//  
//  float[] voronoi_col = new float[3];
//  
//
//  
//  public void createFractureShape(){
//    reset();
//    
//    float sx = 80;
//    
////    voronoi_col = new float[]{180,140,255};
////    createFractureShape(sx * -2);
////
////    voronoi_col = new float[]{140,180,255};
////    createFractureShape(sx * -1);
////    
////    voronoi_col = new float[]{140,255,180};
////    createFractureShape(0);
////    
////    voronoi_col = new float[]{255,180,140};
////    createFractureShape(sx * +1);
////    
////    voronoi_col = new float[]{255,255,140};
////    createFractureShape(sx * +2);
//    
//    
//
//
//    voronoi_col = new float[]{140,180,255};
//    createWindow(sx * -1);
//    
//    voronoi_col = new float[]{255,180,140};
//    createWindow(sx * +0);
//    
//    voronoi_col = new float[]{255,255,140};
//    createWindow(sx * +1);
//    
//  }
//  
//
//  
//  public void createWindow(float translate_y){
//    long timer = System.currentTimeMillis();
//    
//    float translate_z = 0;
//    float pos_z = 20f;
//
//    float dimx = 400;
//    float dimy = 200;
//    float dimz = 5;
//
//    PMatrix3D mat_wall = new PMatrix3D();
//    mat_wall.translate(0, 0, pos_z);
//    mat_wall.translate(0, translate_y, translate_z);
//    mat_wall.rotateX(PI/2f);
//    mat_wall.translate(0, dimy * 0.5f, 0);
//    mat_wall.translate(0, translate_z, 0);
//    
//
//    BObject body = new BBox(this, 0, dimx, dimy, dimz);
//
//    Transform transform = asBulletTransform(mat_wall);
//    body.rigidBody.setWorldTransform(transform);
//    body.rigidBody.getMotionState().setWorldTransform(transform);
//    body.rigidBody.setUserPointer(body);
//
//  
//    float r = voronoi_col[0];
//    float g = voronoi_col[1];
//    float b = voronoi_col[2];
//    
//    body.displayShape = createShape(BOX, dimx, dimy, dimz);
//    body.displayShape.setFill(color(r,g,b));
//    body.displayShape.setFill(true);
//    body.displayShape.setStrokeWeight(1f);
//    body.displayShape.setStroke(color(r,g,b,96));
//    body.displayShape.setStroke(false);
//    body.displayShape.setName("window|wire");
//
//    group_bulletbodies.addChild(body.displayShape);
//    physics.addBody(body);
//    
//    createFitting(mat_wall,new Vector3f(dimx, dimy, dimz), true);
//    createFitting(mat_wall,new Vector3f(dimx, dimy, dimz), false);
// 
//    timer = System.currentTimeMillis() - timer;
//    System.out.println("createFractureShape "+timer+" ms");
//
//  }
//  
//  
//  public void createFractureShape(float translate_y){
//    long timer = System.currentTimeMillis();
//    
//    float translate_z = 0;
//    float pos_z = 20f;
//    
//    int num_voronoi_cells = 300;
//
//    float dimx = 400;
//    float dimy = 200;
//    float dimz = 5;
//    float dimx_half = dimx * 0.5f;
//    float dimy_half = dimy * 0.5f;
//    float dimz_half = dimz * 0.5f;
//    float off = 10;
//    
//    PMatrix3D mat_wall = new PMatrix3D();
//    mat_wall.translate(0, 0, pos_z);
//    mat_wall.translate(0, translate_y, translate_z);
//    mat_wall.rotateX(PI/2f);
//    mat_wall.translate(0, dimy * 0.5f, 0);
//    mat_wall.translate(0, translate_z, 0);
//    
//    // create centroids for voronoi-cells
//    List<WB_Point> points = new ArrayList<WB_Point>(num_voronoi_cells);
//
//    for(int sample_idx = 0; sample_idx < num_voronoi_cells; sample_idx++){
//      float r = 4 + 0.05f * (float) Math.pow(sample_idx, 1.5f);
//      float angle = sample_idx * (float) DwSampling.GOLDEN_ANGLE_R*1f;
//      float px = r * cos(angle);
//      float py = r * sin(angle);
//      
//      float rrr = r*0.2f;
//      px += random(-rrr, rrr);
//      py += random(-rrr, rrr);
//      
//      float idxn = sample_idx / (float)num_voronoi_cells;
//
//      if(random(1) < 0.8f * idxn) continue;
//      if(px > -(dimx_half-off)  && px < +(dimx_half-off) &&
//         py > -(dimy_half-off)  && py < +(dimy_half-off) )
//      {
//        points.add(new WB_Point(px, py, 0));
//      }
//    }
//    
//    // create voronoi
//    ArrayList<WB_Point> pts = new  ArrayList<WB_Point>();
//    pts.add(new WB_Point(-dimx_half, -dimy_half));
//    pts.add(new WB_Point(+dimx_half, -dimy_half));
//    pts.add(new WB_Point(+dimx_half, +dimy_half));
//    pts.add(new WB_Point(-dimx_half, +dimy_half));
//    
//    WB_Polygon boundary = new WB_Polygon(pts);
//    List<WB_VoronoiCell2D> cells = WB_Voronoi.getClippedVoronoi2D(points, boundary, 0);
//
//
//    for (int i = 0; i < cells.size(); i++) {
//      
//      WB_VoronoiCell2D cell = cells.get(i);
//      WB_Polygon cell_polygon = cell.getPolygon();
//      List<WB_Point> cell_points = cell_polygon.getPoints();
//      
//      int num_verts = cell_points.size();
//      
//      // compute center of mass
//      float[][] pnts = new float[num_verts][3];
//      for(int j = 0; j < num_verts; j++){
//        WB_Coord vtx = cell_points.get(j);
//        pnts[j][0] = vtx.xf(); 
//        pnts[j][1] = vtx.yf();
//      }
//      
//      // this one gives better results, than the voronoi center
////      DwBoundingDisk cell_bs = new DwBoundingDisk();
////      cell_bs.compute(pnts, pnts.length);
////      Vector3f center_of_mass = new Vector3f(cell_bs.pos[0], cell_bs.pos[1], 0f);
//      
////      Vector3f center_of_mass = new Vector3f();
////      center_of_mass.x = cell_polygon.getCenter().xf();
////      center_of_mass.y = cell_polygon.getCenter().yf();
////      center_of_mass.z = cell_polygon.getCenter().zf();
//
//      Vector3f center_of_mass = new Vector3f();
//      center_of_mass.x = (cell.getGenerator().xf() + cell_polygon.getCenter().xf() ) * 0.5f;
//      center_of_mass.y = (cell.getGenerator().yf() + cell_polygon.getCenter().yf() ) * 0.5f;
//      center_of_mass.z = 0;
//      
//      // create rigid body coords, center is at 0,0,0
//      ObjectArrayList<Vector3f> vertices = new ObjectArrayList<Vector3f>(pnts.length * 2);
//      for(int j = 0; j < pnts.length; j++){
//        float x = pnts[j][0] - center_of_mass.x;
//        float y = pnts[j][1] - center_of_mass.y;
//        vertices.add(new Vector3f(x, y, -dimz_half));
//        vertices.add(new Vector3f(x, y, +dimz_half));
//      }
//      
//      // create rigid body
//      float mass = (float) (cell.getArea() * dimz);
//      BConvexHull body = new MyBConvexHull(this, mass, vertices, new Vector3f(center_of_mass), true);
////
//      // setup initial body transform-matrix
//      PMatrix3D mat_p5 = new PMatrix3D(mat_wall);
//      mat_p5.translate(center_of_mass.x, center_of_mass.y, center_of_mass.z);
//      Transform transform = asBulletTransform(mat_p5);
//      
//      // rigid-body properties
//      body.rigidBody.setWorldTransform(transform);
////      body.rigidBody.setRestitution(.01f);
//      body.rigidBody.setFriction(0.94f);
//  
////      body.rigidBody.setDamping(0.2f, 0.2f);
//    
//      
//      // create PShape
//      PShape shp_cell = createCellShape(cell_points, dimz, center_of_mass);
//
//      // link everything together
//      body.displayShape = shp_cell;
//      group_bulletbodies.addChild(shp_cell);
//      physics.addBody(body);
//      
//    }
//    
//    createFitting(mat_wall,new Vector3f(dimx, dimy, dimz), true);
//    createFitting(mat_wall,new Vector3f(dimx, dimy, dimz), false);
// 
//    timer = System.currentTimeMillis() - timer;
//    System.out.println("createFractureShape "+timer+" ms");
//
//  }
//  
//  
//  public void createFitting(PMatrix3D mat_wall, Vector3f wall_dim, boolean right){
//    float dimx = wall_dim.x;
//    float dimy = wall_dim.y;
//    float dimz = wall_dim.z;
//    
//    float dimx2 = dimz*3;
//    
//    float tx  = dimx * 0.5f + dimz * 0.5f;
//    float tx2 = tx - dimx2*0.5f + dimz*0.5f;
//    
//    
//    float side = right ? 1f : -1f;
//    
//    tx  *= side;
//    tx2 *= side;
//    
//    {
//      PMatrix3D mat_pillar = mat_wall.get();
//      mat_pillar.translate(tx, 0, 0);
//      Transform transform = asBulletTransform(mat_pillar);
//      
//      BObject body = new BBox(this, 0, dimz, dimy, dimz);
//  
//      body.rigidBody.setWorldTransform(transform);
//      body.rigidBody.getMotionState().setWorldTransform(transform);
//  
//      body.displayShape = createBoxShape(new Vector3f(dimz, dimy, dimz));
//      group_bulletbodies.addChild(body.displayShape);
//      physics.addBody(body);
//    }
//    
//
//    {
//
//      
//      PMatrix3D mat_pillar = mat_wall.get();
//      mat_pillar.translate(tx2, 0, dimz);
//      Transform transform = asBulletTransform(mat_pillar);
//      
//      BObject body = new BBox(this, 0, dimx2, dimy, dimz);
//  
//      body.rigidBody.setWorldTransform(transform);
//      body.rigidBody.getMotionState().setWorldTransform(transform);
//      body.rigidBody.setFriction(0.99f);
//  
//      body.displayShape = createBoxShape(new Vector3f(dimx2, dimy, dimz));
//      group_bulletbodies.addChild(body.displayShape);
//      physics.addBody(body);
//    }
//    
//    {
//      PMatrix3D mat_pillar = mat_wall.get();
//      mat_pillar.translate(tx2, 0, -dimz);
//      Transform transform = asBulletTransform(mat_pillar);
//      
//      BObject body = new BBox(this, 0, dimx2, dimy, dimz);
//  
//      body.rigidBody.setWorldTransform(transform);
//      body.rigidBody.getMotionState().setWorldTransform(transform);
//      
//      body.rigidBody.setFriction(0.99f);
//  
//      body.displayShape = createBoxShape(new Vector3f(dimx2, dimy, dimz));
//      group_bulletbodies.addChild(body.displayShape);
//      physics.addBody(body);
//    }
//  }
//
//  
//
//  
//  public PShape createBoxShape(Vector3f dim){
//    PShape shp = createShape(BOX, dim.x, dim.y, dim.z);
//    shp.setStroke(false);
//    shp.setFill(true);
//    shp.setFill(color(16));
//    shp.setStrokeWeight(1);
//    shp.setStroke(color(0));
//    return shp;
//  }
//  
//  
//  public Transform asBulletTransform(PMatrix3D mat_p5){
//    Matrix4f mat = new Matrix4f();
//    mat.setRow(0, mat_p5.m00, mat_p5.m01, mat_p5.m02, mat_p5.m03);
//    mat.setRow(1, mat_p5.m10, mat_p5.m11, mat_p5.m12, mat_p5.m13);
//    mat.setRow(2, mat_p5.m20, mat_p5.m21, mat_p5.m22, mat_p5.m23);
//    mat.setRow(3, mat_p5.m30, mat_p5.m31, mat_p5.m32, mat_p5.m33);
//    return new Transform(mat);
//  }
//
//  
//  
//  // add ground bodies
//  public void addGround(){
//    {
//      Vector3f pos = new Vector3f(0,0,10);
//      BObject body = new BBox(this, 0, 650, 650, 20);
////      BObject body = new BBox(this, 0, pos.x, pos.y, pos.z, 650, 650, 20);
////      BObject body = new BObject(this, 0, obj, new Vector3f(), true);
//      
////      body.setPosition(pos);
////      Transform transform = new Transform();
////      body.rigidBody.getMotionState().getWorldTransform(transform);
////      body.rigidBody.setWorldTransform(transform);
//      
////      Transform transform = new Transform();
////      transform.set
//      
////      body.rigidBody.getWorldTransform(transform);
////      transform.basis.transform(arg0);
//      
////      body.rigidBody.setWorldTransform(transform);
////      body.rigidBody.getMotionState().setWorldTransform(transform);
//      
//      
////      PMatrix3D mat = new PMatrix3D();
////      mat.translate(pos.x, pos.y, pos.z);
////      Transform transform = asBulletTransform(mat);
////      body.rigidBody.setWorldTransform(transform);
////      body.rigidBody.getMotionState().setWorldTransform(transform);
//      
//      Transform transform = new Transform();
//      body.rigidBody.getWorldTransform(transform);
//      
//      transform.origin.set(pos);
//      body.rigidBody.setWorldTransform(transform);
//      body.rigidBody.getMotionState().setWorldTransform(transform);
//      
//      
//  
//      body.displayShape = createShape(BOX, 650, 650, 20);
//      body.displayShape.setStroke(false);
//      body.displayShape.setFill(true);
//      body.displayShape.setFill(color(200, 96, 16));
//      body.displayShape.setFill(color(255));
//      body.displayShape.setStrokeWeight(1);
//      body.displayShape.setStroke(color(0));
////      if(body instanceof BBox){
////        fixBoxNormals(body.displayShape);
////      }
//      physics.addBody(body);
//      group_bulletbodies.addChild(body.displayShape);
//      body.displayShape.setName("ground_box");
//    }
//  }
//  
//  
//  
//  
//
//  // render scene
//  public void displayScene(PGraphics3D pg){
//    if(pg == skylight.renderer.pg_render){
//      pg.background(16);
//    }
//    
//    if(pg == geombuffer.pg_geom){
//      pg.background(255, 255);
//      pg.pgl.clearColor(1, 1, 1, 6000);
//      pg.pgl.clear(PGL.COLOR_BUFFER_BIT);
//    }
//    
//    pg.pushMatrix();
//    pg.applyMatrix(mat_scene_view);
//    pg.shape(group_bulletbodies);
//    pg.shape(group_collisions);
//    pg.popMatrix();
//  }
//  
//  
//  // update PShape matrices
//  Transform transform = new Transform();
//  Matrix4f out = new Matrix4f();
//  
//  public void updateShapes(BObject body){
//    if (body.displayShape != null) {
//      body.displayShape.resetMatrix();
//      if (body.getMass() < 0) {
//        transform = body.rigidBody.getMotionState().getWorldTransform(transform);
//        out = transform.getMatrix(out);
//        body.transform.set(transform);
//        body.displayShape.applyMatrix(out.m00, out.m01, out.m02, out.m03, out.m10, out.m11, out.m12, out.m13, out.m20, out.m21, out.m22, out.m23, out.m30, out.m31, out.m32, out.m33);
//
//      } else {
////        transform = body.rigidBody.getWorldTransform(transform);
////        body.transform.set(transform);
////        body.displayShape.translate(transform.origin.x, transform.origin.y, transform.origin.z);
//        
//        transform = body.rigidBody.getMotionState().getWorldTransform(transform);
//        out = transform.getMatrix(out);
//        body.transform.set(transform);
//        body.displayShape.applyMatrix(out.m00, out.m01, out.m02, out.m03, out.m10, out.m11, out.m12, out.m13, out.m20, out.m21, out.m22, out.m23, out.m30, out.m31, out.m32, out.m33);
//
//      }
//    }
//  }
//
//  
//  public void saveScreenshot(){
//    File file = capture.createFilename();
////    pg_aa.save(file.getAbsolutePath());
//    save(file.getAbsolutePath());
//    System.out.println(file);
//  }
//
//
//  public static void main(String args[]) {
//    PApplet.main(new String[] { Skylight_BulletPhysics_Breakable2.class.getName() });
//  }
//}
