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




package Skylight.Skylight_ClothSimulation;



import java.util.ArrayList;
import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftGrid3D;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;
import com.thomasdiewald.pixelflow.java.utils.DwCoordinateTransform;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import peasy.CameraState;
import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics3D;

public class Skylight_ClothSimulation extends PApplet {

  //
  // Cloth simulation + Skylight Renderer
  // 
  // ... for testing and tweaking interactivity and realtime behaviour.
  //
  // AntiAliasing: SMAA
  //
  //
  // -- CONTROLS --
  //
  // LMB: drag particles
  // MMB: drag + fix particles to a location
  // RMB: disable springs, to deform objects
  //
  // ALT + LMB: Camera ROTATE
  // ALT + MMB: Camera PAN
  // ALT + RMB: Camera ZOOM
  //
  // 'r'      restart
  // ' '      toggle physics update
  //
  // '1'      display-mode colors
  // '2'      display-mode tension (affects only springs)
  // '3'      toggle  display particles
  // '4'      toggle display mesh
  // '5'      toggle display springs
  // '6'      toggle display normals
  //
  //

  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = viewport_w - gui_w;
  int gui_y = 0;
  
  // main library context
  DwPixelFlow context;
  
  DwPhysics.Param param_physics = new DwPhysics.Param();
  
  // // physics simulation object
  DwPhysics<DwParticle3D> physics = new DwPhysics<DwParticle3D>(param_physics);
  
  // cloth objects
  DwSoftGrid3D cloth = new DwSoftGrid3D();
  DwSoftGrid3D cube1 = new DwSoftGrid3D();

  // list, that wills store the softbody objects (cloths, cubes, balls, ...)
  ArrayList<DwSoftBody3D> softbodies = new ArrayList<DwSoftBody3D>();
  
  // particle parameters
  DwParticle.Param param_cloth_particle = new DwParticle.Param();
  DwParticle.Param param_cube_particle  = new DwParticle.Param();
  
  // spring parameters
  DwSpringConstraint.Param param_cloth_spring = new DwSpringConstraint.Param();
  DwSpringConstraint.Param param_cube_spring  = new DwSpringConstraint.Param();

  // camera
  PeasyCam peasycam;
  CameraState cam_state_0;
  
  
  // renderer
  DwSkyLight skylight;
  
  // AntiAliasing, SMAA
  SMAA smaa;
  PGraphics3D pg_aa;

  
  PMatrix3D mat_scene_bounds;
  
  

  // global states
  int BACKGROUND_COLOR = 32;
  
  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 0;
  
  // entities to display
  boolean DISPLAY_PARTICLES      = false;
  boolean DISPLAY_MESH           = true;
  boolean DISPLAY_NORMALS        = false;
  boolean DISPLAY_SRPINGS        = false;
  
  boolean DISPLAY_SPRINGS_STRUCT = true;
  boolean DISPLAY_SPRINGS_SHEAR  = true;
  boolean DISPLAY_SPRINGS_BEND   = true;
  
  boolean UPDATE_PHYSICS         = true;
  
  // first thing to do, inside draw()
  boolean NEED_REBUILD = false;
  

  public void settings(){
    size(viewport_w, viewport_h, P3D); 
    smooth(8);
  }
  
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);

    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    
    smaa = new SMAA(context);
    
    pg_aa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_aa.smooth(0);
    pg_aa.textureSampling(5);
    

    ////////////////////////////////////////////////////////////////////////////
    // PARAMETER settings
    // ... to control behavior of particles, springs, etc...
    ////////////////////////////////////////////////////////////////////////////
    
    // physics world parameters
    param_physics.GRAVITY = new float[]{ 0, 0, -0.1f};
    param_physics.bounds  = new float[]{ -400, -400, 0, +1200, +1200, +1200 };
    param_physics.iterations_collisions = 2;
    param_physics.iterations_springs    = 8;
    
    // particle parameters (for simulation)
    param_cloth_particle.DAMP_BOUNDS    = 0.49999f;
    param_cloth_particle.DAMP_COLLISION = 0.99999f;
    param_cloth_particle.DAMP_VELOCITY  = 0.99991f; 
    
    param_cube_particle.DAMP_BOUNDS    = 0.89999f;
    param_cube_particle.DAMP_COLLISION = 0.99999f;
    param_cube_particle.DAMP_VELOCITY  = 0.99991f; 
    
    // spring parameters (for simulation)
    param_cloth_spring.damp_dec = 0.999999f;
    param_cloth_spring.damp_inc = 0.009999f;
    
    param_cube_spring.damp_dec = 0.49999f;
    param_cube_spring.damp_inc = 0.049999f;

    // soft-body parameters (for building)
    cloth.CREATE_STRUCT_SPRINGS = true;
    cloth.CREATE_SHEAR_SPRINGS  = true;
    cloth.CREATE_BEND_SPRINGS   = true;
    cloth.bend_spring_mode      = 0;
    cloth.bend_spring_dist      = 2;
    
    cube1.CREATE_STRUCT_SPRINGS = true;
    cube1.CREATE_SHEAR_SPRINGS  = true;
    cube1.CREATE_BEND_SPRINGS   = true;
    cube1.bend_spring_mode      = 0;
    cube1.bend_spring_dist      = 2;

    // softbodies
    createBodies();
    
    float[] bounds = param_physics.bounds;
    
    float sx = bounds[3] - bounds[0];
    float sy = bounds[4] - bounds[1];
    float sz = bounds[5] - bounds[2];
    
    float px = (bounds[3] + bounds[0]) * 0.5f;
    float py = (bounds[4] + bounds[1]) * 0.5f;
    float pz = (bounds[5] + bounds[2]) * 0.5f;
    float rad = (float)(Math.sqrt(sx*sx + sy*sy + sz*sz) * 0.5f);
    
    
    
    DwBoundingSphere scene_bs = new DwBoundingSphere();
    scene_bs.set(px, py, pz, rad*1.5f);
    
    mat_scene_bounds = scene_bs.getUnitSphereMatrix();

    // callback for rendering the scene
    DwSceneDisplay scene_display = new DwSceneDisplay(){
      @Override
      public void display(PGraphics3D canvas) {
        displayScene(canvas);  
      }
    };
    

    // init skylight renderer
    skylight = new DwSkyLight(context, scene_display, mat_scene_bounds);
    
    // parameters for sky-light
    skylight.sky.param.iterations     = 30;
    skylight.sky.param.solar_azimuth  = 0;
    skylight.sky.param.solar_zenith   = 0;
    skylight.sky.param.sample_focus   = 1; // full sphere sampling
    skylight.sky.param.intensity      = 1.0f;
    skylight.sky.param.rgb            = new float[]{1,1,1};
    skylight.sky.param.shadowmap_size = 512; // quality vs. performance
    
    // parameters for sun-light
    skylight.sun.param.iterations     = 30;
    skylight.sun.param.solar_azimuth  = 45;
    skylight.sun.param.solar_zenith   = 45;
    skylight.sun.param.sample_focus   = 0.12f;
    skylight.sun.param.intensity      = 1.2f;
    skylight.sun.param.rgb            = new float[]{1,1,1};
    skylight.sun.param.shadowmap_size = 512;
    
    
    
    
    // modelview/projection
    createCam();
    
    frameRate(600);
  }
  


  
  
  
  public void createBodies(){
    
    // first thing to do!
    physics.reset();
    
    int nodex_x, nodes_y, nodes_z, nodes_r;
    int nodes_start_x, nodes_start_y, nodes_start_z;
    float r,g,b,s;
    

    // add to global list
    softbodies.clear();
    softbodies.add(cloth);
    softbodies.add(cube1);

    // set some common things, like collision behavior
    for(DwSoftBody3D body : softbodies){
      body.self_collisions = true;
      body.collision_radius_scale = 1f;
    }
    
    
    ///////////////////// CLOTH ////////////////////////////////////////////////
    nodex_x = 40;
    nodes_y = 40;
    nodes_z = 1;
    nodes_r = 12;
    nodes_start_x = 0;
    nodes_start_y = 0;
    nodes_start_z = 400;
    r = 255;
    g = 255;
    b = 255;
    s = 1f;
    cloth.setMaterialColor(color(r  ,g  ,b  ));
    cloth.setParticleColor(color(r*s,g*s,b*s));
    cloth.setParam(param_cloth_particle);
    cloth.setParam(param_cloth_spring);
    cloth.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cloth.createShapeParticles(this, true);
    // fix all 4 corners
    cloth.getNode(        0,         0, 0).enable(false, false, false);
    cloth.getNode(nodex_x-1,         0, 0).enable(false, false, false);
    cloth.getNode(nodex_x-1, nodes_y-1, 0).enable(false, false, false);
    cloth.getNode(        0, nodes_y-1, 0).enable(false, false, false);


    //////////////////// CUBE //////////////////////////////////////////////////
    nodex_x = 30;
    nodes_y = 20;
    nodes_z = 1;
    nodes_r = 12;
    nodes_start_x = 100;
    nodes_start_y = 200;
    nodes_start_z = 600;
    r = 255;
    g = 96;
    b = 0;
    s = 1f;
    cube1.setMaterialColor(color(r  ,g  ,b  ));
    cube1.setParticleColor(color(r*s,g*s,b*s));
    cube1.setParam(param_cube_particle);
    cube1.setParam(param_cube_spring);
    cube1.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cube1.createShapeParticles(this, true);
 

  }




  

  
  //////////////////////////////////////////////////////////////////////////////
  // draw()
  //////////////////////////////////////////////////////////////////////////////
  
  public void draw() {
    
    if(NEED_REBUILD){
      createBodies();
      NEED_REBUILD = false;
    }

    // update interactions, like editing particle positions, deleting springs, etc...
    updateMouseInteractions();
    
    // update physics simulation
    if(UPDATE_PHYSICS){
      physics.update(1);
    }
    
    // update softbody surface normals
    for(DwSoftBody3D body : softbodies){
      body.computeNormals();
    }
    
    // 3) mesh, solid
    if(DISPLAY_MESH){
      for(DwSoftBody3D body : softbodies){
        body.createShapeMesh(this.g);
      }
    }
    
    // disable peasycam-interaction while we edit the model
    peasycam.setActive(MOVE_CAM);
    
    
    updateCamActiveStatus();
    
    if(UPDATE_PHYSICS || CAM_ACTIVE){
      skylight.reset();
    }
    skylight.update();
  
    // Apply AntiAliasing
    smaa.apply(skylight.renderer.pg_render, pg_aa);
  
    peasycam.beginHUD();
    image(pg_aa, 0, 0);
    peasycam.endHUD();


    // info
    int NUM_SPRINGS   = physics.getSpringCount();
    int NUM_PARTICLES = physics.getParticlesCount();
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  
  
  
  public void displayScene(PGraphics3D canvas){
    ////////////////////////////////////////////////////////////////////////////
    // RENDER this madness
    ////////////////////////////////////////////////////////////////////////////
    boolean COMPOSING_RENDERING = canvas == skylight.renderer.pg_render;
    
    if(COMPOSING_RENDERING){
      canvas.background(BACKGROUND_COLOR);
//      displaySamples(canvas);
    }
    
    // XY-grid, gizmo, scene-bounds
    canvas.strokeWeight(2);
    displayAABB(canvas, physics.param.bounds);
    

    // 1) particles
    if(DISPLAY_PARTICLES){
      for(DwSoftBody3D body : softbodies){
        body.use_particles_color = true;
        body.displayParticles(canvas);
      }
    }
    
    // 2) springs
    if(DISPLAY_SRPINGS && COMPOSING_RENDERING){
      for(DwSoftBody3D body : softbodies){
        body.shade_springs_by_tension = (DISPLAY_MODE == 1);
        body.displaySprings(this.g, new DwStrokeStyle(color(255,  90,  30), 1.0f), DwSpringConstraint.TYPE.BEND);
        body.displaySprings(this.g, new DwStrokeStyle(color( 70, 140, 255), 1.0f), DwSpringConstraint.TYPE.SHEAR);
        body.displaySprings(this.g, new DwStrokeStyle(color(  0,   0,   0), 1.0f), DwSpringConstraint.TYPE.STRUCT);
      }
    }
    
    // 3) mesh, solid
    if(DISPLAY_MESH){
      for(DwSoftBody3D body : softbodies){
        body.displayMesh(canvas);
      }
    }
    
    // 4) normals
    if(DISPLAY_NORMALS){
      stroke(0);
      strokeWeight(0.5f);
      for(DwSoftBody3D body : softbodies){
        body.displayNormals(canvas);
      }
    }
    
    // 5) interaction stuff
    displayMouseInteraction();
  }
  
  
 
  
  
  
  /*
   * 
   * 
   * the following code can just be skipped. 
   * It is just some user-interaction stuff, key/mouse handling, etc...
   * AABB/gizmo/grid drawing
   * 
   * 
   */
  
  
  
  

  
  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
  
  // coordinate transformation: world <-> screen
  // for interaction
  final DwCoordinateTransform transform = new DwCoordinateTransform();
  
  float[] mouse_world  = new float[4];
  float[] mouse_screen = new float[4];

  ParticleTransform pnearest;
  
  class ParticleTransform{
    DwParticle particle = null;
    float[]    screen = new float[4];

    public ParticleTransform(DwParticle particle, float[] screen){
      set(particle, screen);
    }
    public void set(DwParticle particle, float[] screen){
      this.particle = particle;
      this.screen[0] = screen[0];
      this.screen[1] = screen[1];
      this.screen[2] = screen[2];
      this.screen[3] = screen[3];
    }
  }
  

  void findNearestParticle(float mx, float my, float radius){
    int particles_count = physics.getParticlesCount();
    DwParticle3D[] particles = (DwParticle3D[]) physics.getParticles();

    float radius_sq = radius * radius;
    float dd_min = radius_sq;
    float dz_min = 1;
    float[] screen = new float[4];
    pnearest = null;
    
    // transform Particles: world -> screen
    for(int i = 0; i < particles_count; i++){
      DwParticle3D pa = particles[i];
      transform.transformToScreen(pa, screen);
      float dx = screen[0] - mx;
      float dy = screen[1] - my;
      float dz = screen[2];
      float dd_sq = dx*dx + dy*dy;
      if(dd_min > dd_sq){
        if(dz_min > dz){
          dz_min = dz;
          dd_min = dd_sq;
          pnearest = new ParticleTransform(pa, screen);
        }
      }
    }  
  }
  
  ArrayList<DwParticle> particles_within_radius = new ArrayList<DwParticle>();
  void findParticlesWithinRadius(float mx, float my, float radius){
    int particles_count = physics.getParticlesCount();
    DwParticle3D[] particles = (DwParticle3D[]) physics.getParticles();
    
    float dd_min = radius * radius;
    particles_within_radius.clear();
    float[] screen = new float[4];
    
    // transform Particles: world -> screen
    for(int i = 0; i < particles_count; i++){
      DwParticle3D pa = particles[i];
      transform.transformToScreen(pa, screen);
      float dx = screen[0] - mx;
      float dy = screen[1] - my;
      float dd_sq = dx*dx + dy*dy;
      if(dd_min > dd_sq){
        particles_within_radius.add(pa);
      }
    }  
  }
  
  
  public void updateMouseInteractions(){
    
    // update transformation stuff
    transform.useCurrentTransformationMatrix((PGraphics3D) this.g);
    
    // deleting springs/constraints between particles
    if(DELETE_SPRINGS){
      findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
      for(DwParticle particle : particles_within_radius){
        particle.enableAllSprings(false);
        particle.collision_group = physics.getNewCollisionGroupId();
        particle.rad_collision = particle.rad;
        particle.all_springs_deactivated = true;
      }
    } 
    
    if(!MOVE_PARTICLE){
      findNearestParticle(mouseX, mouseY, SNAP_RADIUS);
      SNAP_PARTICLE = pnearest != null;
    }
    
    if(SNAP_PARTICLE){
      mouse_screen[0] = mouseX;
      mouse_screen[1] = mouseY;
      mouse_screen[2] = pnearest.screen[2];
      transform.screenToWorld(mouse_screen, mouse_world);
      
      if(MOVE_PARTICLE){
        pnearest.particle.enable(false, false, false);
        pnearest.particle.moveTo(mouse_world, 0.1f);
      }
    }
  }
  
  
  public void displayMouseInteraction(){
    pushStyle();
    if(SNAP_PARTICLE){

      int col_move_release = color(64, 200, 0);
      int col_move_fixed   = color(255, 30, 10); 
      
      int col = (mouseButton == CENTER) ? col_move_fixed : col_move_release;
      
      DwParticle particle = pnearest.particle;

      strokeWeight(1);
      stroke(col);
      line(particle.x(), particle.y(), particle.z(), mouse_world[0], mouse_world[1], mouse_world[2]);
    
      strokeWeight(10);
      stroke(col);
      point(particle.x(), particle.y(), particle.z());
      
      peasycam.beginHUD();
      stroke(col);
      strokeWeight(1);
      noFill();
      ellipse(mouseX, mouseY, 15, 15);
      peasycam.endHUD();
    }

    if(DELETE_SPRINGS){
      peasycam.beginHUD();
      strokeWeight(2);
      stroke(255,0,0);
      fill(255, 0, 0, 64);
 
      ellipse(mouseX, mouseY, DELETE_RADIUS*2, DELETE_RADIUS*2);
      peasycam.endHUD();
    }
    popStyle();
  }
  
  
  
  
  
  
  
  
  
  
  
  
 
  boolean MOVE_CAM       = false;
  boolean MOVE_PARTICLE  = false;
  boolean SNAP_PARTICLE  = false;
  float   SNAP_RADIUS    = 30;
  boolean DELETE_SPRINGS = false;
  float   DELETE_RADIUS  = 15;

  public void mousePressed(){
    if((mouseButton == LEFT || mouseButton == CENTER) && !MOVE_CAM){
      MOVE_PARTICLE = true;
    }
    if(mouseButton == RIGHT && !MOVE_CAM){
      DELETE_SPRINGS = true;
    }
  }
  
  public void mouseReleased(){
    if(!MOVE_CAM){
//      if(!DELETE_SPRINGS && particle_nearest != null){
        if(MOVE_PARTICLE && pnearest != null){
          if(mouseButton == LEFT  ) pnearest.particle.enable(true, true, true);
          if(mouseButton == CENTER) pnearest.particle.enable(true, false, false);
          pnearest = null;
        }

//      }
      if(mouseButton == RIGHT) DELETE_SPRINGS = false;
    }
   
    MOVE_PARTICLE  = false;
    DELETE_SPRINGS = false;
  }
  
  public void keyPressed(){
    if(key == CODED){
      if(keyCode == ALT){
        MOVE_CAM = true;
      }
    }
  }
  
  public void keyReleased(){
    if(key == 'r') createBodies();
    if(key == '1') DISPLAY_MODE = 0;
    if(key == '2') DISPLAY_MODE = 1;
    
    if(key == '3') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
    if(key == '4') DISPLAY_MESH      = !DISPLAY_MESH;
    if(key == '5') DISPLAY_SRPINGS   = !DISPLAY_SRPINGS;
    if(key == '6') DISPLAY_NORMALS   = !DISPLAY_NORMALS;

    if(key == ' ') UPDATE_PHYSICS = !UPDATE_PHYSICS;
    if(key == 'c') printCam();
    if(key == 'v') resetCam();
    
    MOVE_CAM = false; 
  }



  
  public void createCam(){
    // camera - modelview
    double   distance = 1472.938;
    double[] look_at  = {147.029, 270.201, -63.601};
    double[] rotation = { -0.408,   0.833,  -1.127};
    peasycam = new PeasyCam(this, look_at[0], look_at[1], look_at[2], distance);
    peasycam.setMaximumDistance(10000);
    peasycam.setMinimumDistance(0.1f);
    peasycam.setRotations(rotation[0], rotation[1], rotation[2]);
    cam_state_0 = peasycam.getState();
    
    // camera - projection
    float fovy    = PI/3.0f;
    float aspect  = width/(float)(height);
    float cameraZ = (height*0.5f) / tan(fovy*0.5f);
    float zNear   = cameraZ/100.0f;
    float zFar    = cameraZ*20.0f;
    perspective(fovy, aspect, zNear, zFar);
  }
  
  public void resetCam(){
    peasycam.setState(cam_state_0, 700);
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
  
  
  float[] cam_pos = new float[3];
  boolean CAM_ACTIVE = false;
  
  public void updateCamActiveStatus(){
    float[] cam_pos_curr = peasycam.getPosition();
    CAM_ACTIVE = false;
    CAM_ACTIVE |= cam_pos_curr[0] != cam_pos[0];
    CAM_ACTIVE |= cam_pos_curr[1] != cam_pos[1];
    CAM_ACTIVE |= cam_pos_curr[2] != cam_pos[2];
    cam_pos = cam_pos_curr;
  }
  
  
  

  //////////////////////////////////////////////////////////////////////////////
  // Scene Display Utilities
  //////////////////////////////////////////////////////////////////////////////
  
  PShape shp_gizmo;
  PShape shp_gridxy;
  PShape shp_aabb;
  
  public void displayGizmo(PGraphics3D canvas, float s){
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
  
  public void displayGridXY(PGraphics3D canvas, int lines, float s){
    if(shp_gridxy == null){
      shp_gridxy = createShape();
      shp_gridxy.beginShape(LINES);
      shp_gridxy.stroke(0);
      shp_gridxy.strokeWeight(0.3f);
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
  
  
  public void displayAABB(PGraphics3D canvas, float[] aabb){
    if(shp_aabb == null){
      float xmin = aabb[0], xmax = aabb[3];
      float ymin = aabb[1], ymax = aabb[4];
      float zmin = aabb[2], zmax = aabb[5];
      
      shp_aabb = createShape(GROUP);
      
      PShape plane_zmin = createShape();
      plane_zmin.beginShape(QUAD);
      plane_zmin.stroke(0);
      plane_zmin.strokeWeight(1);
      plane_zmin.fill(16,96,192);
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
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_ClothSimulation.class.getName() });
  }
}