/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody3D.Softbody3D_Playground;



import java.util.ArrayList;
import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBall3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftGrid3D;
import com.thomasdiewald.pixelflow.java.utils.DwCoordinateTransform;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import peasy.CameraState;
import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PShape;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;

public class Softbody3D_Playground extends PApplet {
  
  //
  // 3D Softbody Sandbox, to debug/test/profile everything.
  //
  // Lots of different objects are created of particle-arrays and spring-constraints.
  // Everything can collide with everything and also be destroyed (RMB).
  // 
  // + Collision Detection
  //
  // Controls:
  // LMB: drag particles
  // MMB: drag + fix particles to a location
  // RMB: disable springs, to deform objects
  //
  // ALT + LMB: Camera ROTATE
  // ALT + MMB: Camera PAN
  // ALT + RMB: Camera ZOOM
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
  DwSoftGrid3D cube2 = new DwSoftGrid3D();
  DwSoftBall3D ball  = new DwSoftBall3D();

  // list, that wills store the softbody objects (cloths, cubes, balls, ...)
  ArrayList<DwSoftBody3D> softbodies = new ArrayList<DwSoftBody3D>();
  
  // particle parameters
  DwParticle.Param param_cloth_particle = new DwParticle.Param();
  DwParticle.Param param_cube_particle  = new DwParticle.Param();
  DwParticle.Param param_ball_particle  = new DwParticle.Param();
  
  // spring parameters
  DwSpringConstraint.Param param_cloth_spring = new DwSpringConstraint.Param();
  DwSpringConstraint.Param param_cube_spring  = new DwSpringConstraint.Param();
  DwSpringConstraint.Param param_ball_spring  = new DwSpringConstraint.Param();
  

  // camera
  PeasyCam peasycam;
  CameraState cam_state_0;
  
  // cloth texture
  PGraphics2D texture;
  
  // global states
  int BACKGROUND_COLOR = 92;
  
  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 0;
  
  // entities to display
  boolean DISPLAY_PARTICLES      = false;
  boolean DISPLAY_MESH           = true;
  boolean DISPLAY_WIREFRAME      = false;
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
    

    ////////////////////////////////////////////////////////////////////////////
    // PARAMETER settings
    // ... to control behavior of particles, springs, etc...
    ////////////////////////////////////////////////////////////////////////////
    
    // physics world parameters
    int cs = 1500;
    param_physics.GRAVITY = new float[]{ 0, 0, -0.1f};
    param_physics.bounds  = new float[]{ -cs, -cs, 0, +cs, +cs, +cs };
    param_physics.iterations_collisions = 2;
    param_physics.iterations_springs    = 8;
    
    // particle parameters (for simulation)
    param_cloth_particle.DAMP_BOUNDS    = 0.49999f;
    param_cloth_particle.DAMP_COLLISION = 0.99999f;
    param_cloth_particle.DAMP_VELOCITY  = 0.99991f; 
    
    param_cube_particle.DAMP_BOUNDS    = 0.89999f;
    param_cube_particle.DAMP_COLLISION = 0.99999f;
    param_cube_particle.DAMP_VELOCITY  = 0.99991f; 
    
    param_ball_particle.DAMP_BOUNDS    = 0.89999f;
    param_ball_particle.DAMP_COLLISION = 0.99999f;
    param_ball_particle.DAMP_VELOCITY  = 0.99991f; 
   
    // spring parameters (for simulation)
    param_cloth_spring.damp_dec = 0.999999f;
    param_cloth_spring.damp_inc = 0.059999f;
    
    param_cube_spring.damp_dec = 0.089999f;
    param_cube_spring.damp_inc = 0.089999f;

    param_ball_spring.damp_dec = 0.999999999f;
    param_ball_spring.damp_inc = 0.0599999f;
    
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
    
    cube2.CREATE_STRUCT_SPRINGS = true;
    cube2.CREATE_SHEAR_SPRINGS  = true;
    cube2.CREATE_BEND_SPRINGS   = true;
    cube2.bend_spring_mode      = 0;
    cube2.bend_spring_dist      = 2;

    ball.CREATE_STRUCT_SPRINGS = true;
    ball.CREATE_SHEAR_SPRINGS  = true;
    ball.CREATE_BEND_SPRINGS   = true;
    ball.bend_spring_mode      = 0;
    ball.bend_spring_dist      = 3;
    
    // create textures, in this case, only for the cloth
    createClothTexture();

    // softbodies
    createBodies();
    
    // modelview/projection
    createCam();
    
    // gui
    createGUI();

    frameRate(600);
  }
  
  
  public void createClothTexture(){
   
    PFont font = createFont("Calibri", 200);

    int tex_w = 1024;
    int tex_h = 1024;

    // create texture for text shadow
    PGraphics2D pg_tmp    = (PGraphics2D) createGraphics(tex_w, tex_h, P2D);
    PGraphics2D pg_shadow = (PGraphics2D) createGraphics(tex_w, tex_h, P2D);
    pg_shadow.smooth(8);
    pg_shadow.beginDraw();
    pg_shadow.clear();
    pg_shadow.fill(0);
    pg_shadow.textAlign(CENTER, CENTER);
    pg_shadow.textFont(font);
    pg_shadow.text("PixelFlow", tex_w/2, tex_h/2 - 100);
    pg_shadow.text("Physics"  , tex_w/2, tex_h/2 + 100);
    pg_shadow.endDraw();
    
    // set blendMode to REPLACE, for the Filter-ops
    pg_tmp   .beginDraw(); pg_tmp   .blendMode(REPLACE); pg_tmp   .endDraw();
    pg_shadow.beginDraw(); pg_shadow.blendMode(REPLACE); pg_shadow.endDraw();
    
    // blur/mult/blur, to get kind of a dark shadow
    DwFilter.get(context).gaussblur.apply(pg_shadow, pg_shadow, pg_tmp, 15);
    DwFilter.get(context).multiply .apply(pg_shadow, pg_shadow, new float[]{1,1,1,3});
    DwFilter.get(context).gaussblur.apply(pg_shadow, pg_shadow, pg_tmp, 15);

    // now create the real texture
    texture = (PGraphics2D) createGraphics(tex_w, tex_h, P2D);
    texture.smooth(8);
    texture.beginDraw();
    texture.background(255,200,50);
    
    // grid, for better contrast
    int num_lines = 40;
    float dx = tex_w/(float)(num_lines-1);
    float dy = tex_h/(float)(num_lines-1);
    texture.strokeWeight(2f);
    texture.stroke(0, 100);
    for(int ix = 0; ix < num_lines; ix++){
      texture.line((int)(dx*ix), 0,(int)(dx*ix), tex_h);
      texture.line(0, (int)(dy*ix), tex_w, (int)(dy*ix));
    }
   

    // some random rectangles
//    texture.stroke(0);
//    texture.strokeWeight(1f);
//    texture.rectMode(CENTER);
//    for(int i = 0; i < 256; i++){
//      float rx      = random(tex_w);
//      float ry      = random(tex_h);
//      float rad     = random(10, 64);
//      float shading = random(0, 255);
//      texture.fill(shading*1.0f, shading*0.6f, shading*0.2f);
//      texture.rect((int)rx, (int)ry, (int)rad, (int)rad, 3);
//    }
    
    // text-shadow
    texture.image(pg_shadow, 0, 0, tex_w, tex_h);
    
    // text
    texture.fill(70,170,255);
    texture.textAlign(CENTER, CENTER);
    texture.textFont(font);
    texture.text("PixelFlow", tex_w/2, tex_h/2 - 100);
    texture.text("Physics"  , tex_w/2, tex_h/2 + 100);
    
    // border
    texture.rectMode(CORNER);
    texture.noFill();
    texture.stroke(0);
    texture.strokeWeight(6f);
    texture.rect(0, 0, tex_w, tex_h);
    
    texture.endDraw();
    
  }
  
  
  
  public void createBodies(){
    
    
    // first thing to do!
    physics.reset();
    
    int nodex_x, nodes_y, nodes_z, nodes_r;
    int nodes_start_x, nodes_start_y, nodes_start_z;
    int ball_subdivisions, ball_radius;
    float r,g,b,s;
    
    boolean particles_volume = !false;
    
    
    // add to global list
    softbodies.clear();
    softbodies.add(cloth);
    softbodies.add(cube1);
    softbodies.add(cube2);
    softbodies.add(ball);
    
    // set some common things, like collision behavior
    for(DwSoftBody3D body : softbodies){
      body.self_collisions = true;
      body.collision_radius_scale = 1f;
    }
    
    
    ///////////////////// CLOTH ////////////////////////////////////////////////
    nodex_x = 40;
    nodes_y = 40;
    nodes_z = 1;
    nodes_r = 10;
    nodes_start_x = 0;
    nodes_start_y = 0;
    nodes_start_z = nodes_y * nodes_r*2-200;
    r = 255;
    g = 200;
    b = 100;
    s = 1f;
    cloth.texture_XYp = texture;
    cloth.setMaterialColor(color(r  ,g  ,b  ));
    cloth.setParticleColor(color(r*s,g*s,b*s));
    cloth.setParam(param_cloth_particle);
    cloth.setParam(param_cloth_spring);
    cloth.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cloth.createShapeParticles(this, particles_volume);
    cloth.getNode(              0, 0, 0).enable(false, false, false);
    cloth.getNode(cloth.nodes_x-1, 0, 0).enable(false, false, false);

    
    
    //////////////////// CUBE //////////////////////////////////////////////////
    nodex_x = 30;
    nodes_y = 2;
    nodes_z = 15;
    nodes_r = 10;
    nodes_start_x = 300;
    nodes_start_y = 300;
    nodes_start_z = nodes_y * nodes_r*2+200;
    r = 128;
    g = 64;
    b = 64;
    s = 1f;
    cube1.setMaterialColor(color(r  ,g  ,b  ));
    cube1.setParticleColor(color(r*s,g*s,b*s));
    cube1.setParam(param_cube_particle);
    cube1.setParam(param_cube_spring);
    cube1.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cube1.createShapeParticles(this, particles_volume);
    
    //////////////////// CUBE //////////////////////////////////////////////////
    nodex_x = 3;
    nodes_y = 3;
    nodes_z = 25;
    nodes_r = 10;
    nodes_start_x = 500;
    nodes_start_y = -nodex_x * nodes_r * 4;
    nodes_start_z = nodes_y * nodes_r*2+200;
    r =  40;
    g = 180;
    b = 255;
    s = 1f;
    cube2.setMaterialColor(color(r  ,g  ,b  ));
    cube2.setParticleColor(color(r*s,g*s,b*s));
    cube2.setParam(param_cube_particle);
    cube2.setParam(param_cube_spring);
    cube2.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cube2.createShapeParticles(this, particles_volume);
    
    //////////////////// BALL //////////////////////////////////////////////////
    ball_subdivisions = 3;
    ball_radius = 140;
    nodes_start_x = 400;
    nodes_start_y = 100;
    nodes_start_z = 800;
    r = 100;
    g = 150;
    b = 0;
    s = 1f;
    ball.setMaterialColor(color(r  ,g  ,b  ));
    ball.setParticleColor(color(r*s,g*s,b*s));
    ball.setParam(param_ball_particle);
    ball.setParam(param_ball_spring);
    ball.create(physics, ball_subdivisions, ball_radius, nodes_start_x, nodes_start_y, nodes_start_z);
    ball.createShapeParticles(this, particles_volume);
    
  }




  

  
  //////////////////////////////////////////////////////////////////////////////
  // draw()
  //////////////////////////////////////////////////////////////////////////////
  
  public void draw() {
    
    if(NEED_REBUILD){
      createBodies();
      NEED_REBUILD = false;
    }

    // add additional forces, e.g. Wind, ...
    int particles_count = physics.getParticlesCount();
    DwParticle[] particles = physics.getParticles();
    if(APPLY_WIND){
      float[] wind = new float[3];
      for(int i = 0; i < particles_count; i++){
        wind[1] = noise(i) *0.5f;
        particles[i].addForce(wind);
      }
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
    

    ////////////////////////////////////////////////////////////////////////////
    // RENDER this madness
    ////////////////////////////////////////////////////////////////////////////
    background(BACKGROUND_COLOR);
    
    // disable peasycam-interaction while we edit the model
    peasycam.setActive(MOVE_CAM);
    
    
    // XY-grid, gizmo, scene-bounds
    strokeWeight(2);
    displayGridXY(20, 100);
    displayGizmo(1000);
    displayAABB(physics.param.bounds);
    
    // lights, materials
    // lights();
    pointLight(220, 180, 140, -1000, -1000, -100);
    ambientLight(96, 96, 96);
    directionalLight(210, 210, 210, -1, -1.5f, -2);
    lightFalloff(1.0f, 0.001f, 0.0f);
    lightSpecular(255, 0, 0);
    specular(255, 0, 0);
    shininess(5);
    
    
    // 3) mesh, solid
    if(DISPLAY_MESH){
      for(DwSoftBody3D body : softbodies){
        body.createShapeMesh(this.g);
      }
    }
    

    // 1) particles
    if(DISPLAY_PARTICLES){
      for(DwSoftBody3D body : softbodies){
        body.use_particles_color = true;
        body.displayParticles(this.g);
      }
    }
    
    

    if(DISPLAY_SRPINGS){
      for(DwSoftBody3D body : softbodies){
        body.shade_springs_by_tension = (DISPLAY_MODE == 1);
        body.displaySprings(this.g, new DwStrokeStyle(color(255,  90,  30), 0.3f), DwSpringConstraint.TYPE.BEND);
        body.displaySprings(this.g, new DwStrokeStyle(color( 70, 140, 255), 0.6f), DwSpringConstraint.TYPE.SHEAR);
        body.displaySprings(this.g, new DwStrokeStyle(color(  0,   0,   0), 1.0f), DwSpringConstraint.TYPE.STRUCT);
      }
    }

    
    // 3) mesh, solid
    if(DISPLAY_MESH){
      for(DwSoftBody3D body : softbodies){
        body.displayMesh(this.g);
      }
    }
    
    
    if(DISPLAY_WIREFRAME){
      for(DwSoftBody3D body : softbodies){
        body.createShapeWireframe(this.g, new DwStrokeStyle(color(0), 0.5f));
      }
    }
    
    if(DISPLAY_WIREFRAME){
      for(DwSoftBody3D body : softbodies){
        body.displayWireframe(this.g);
      }
    }

    
    // 4) normals
    if(DISPLAY_NORMALS){
      stroke(0);
      strokeWeight(0.5f);
      for(DwSoftBody3D body : softbodies){
        body.displayNormals(this.g);
      }
    }
    
    // 5) interaction stuff
    displayMouseInteraction();
    
    
    displayGUI();

    // info
    int NUM_SPRINGS   = physics.getSpringCount();
    int NUM_PARTICLES = physics.getParticlesCount();
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  
 
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
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
    float[]      screen = new float[4];

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
    DwParticle[] particles = physics.getParticles();
    
    float radius_sq = radius * radius;
    float dd_min = radius_sq;
    float dz_min = 1;
    float[] screen = new float[4];
    pnearest = null;
    
    // transform Particles: world -> screen
    for(int i = 0; i < particles_count; i++){
      DwParticle3D pa = (DwParticle3D) particles[i];
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
    DwParticle[] particles = physics.getParticles();
    
    float dd_min = radius * radius;
    particles_within_radius.clear();
    float[] screen = new float[4];
    
    // transform Particles: world -> screen
    for(int i = 0; i < particles_count; i++){
      DwParticle3D pa = (DwParticle3D) particles[i];
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
  
  
  
  
  
  
  
  
  // update all springs rest-lengths, based on current particle position
  // the effect is, that the body keeps the current shape
  public void applySpringMemoryEffect(){
    ArrayList<DwSpringConstraint> springs = physics.getSprings();
    for(DwSpringConstraint spring : springs){
      spring.updateRestlength();
    }
  }
  
  
  
  
  
  boolean APPLY_WIND     = false;
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
    
    if(key == 'm') applySpringMemoryEffect();
    
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
    double   distance = 1518.898;
    double[] look_at  = { 58.444, -48.939, 167.661};
    double[] rotation = { -0.744,   0.768,  -0.587};
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
  
  
  
  
  
  
  
  
  
  
  
  
  

  
  //////////////////////////////////////////////////////////////////////////////
  // Scene Display Utilities
  //////////////////////////////////////////////////////////////////////////////
  
  PShape shp_gizmo;
  PShape shp_gridxy;
  PShape shp_aabb;
  
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
  
  public void displayGridXY(int lines, float s){
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
    shape(shp_gridxy);

  }
  
  public void displayAABB(float[] aabb){
    if(shp_aabb == null){
      float xmin = aabb[0], xmax = aabb[3];
      float ymin = aabb[1], ymax = aabb[4];
      float zmin = aabb[2], zmax = aabb[5];
      
      shp_aabb = createShape(GROUP);
      
      PShape plane_zmin = createShape();
      plane_zmin.beginShape(QUAD);
      plane_zmin.stroke(0);
      plane_zmin.strokeWeight(1);
      plane_zmin.fill(192);
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
    shape(shp_aabb);
  }
  

  

  
  
  
  
  
  
  
  ////////////////////////////////////////////////////////////////////////////
  // GUI
  ////////////////////////////////////////////////////////////////////////////
  
  
  public void setDisplayMode(int val){
    DISPLAY_MODE = val;
  }
  
  public void setDisplayTypes(float[] val){
    DISPLAY_PARTICLES = (val[0] > 0);
    DISPLAY_MESH      = (val[1] > 0);
    DISPLAY_SRPINGS   = (val[2] > 0);
    DISPLAY_NORMALS   = (val[3] > 0);
    DISPLAY_WIREFRAME = (val[4] > 0);
  }
  
  public void setGravity(float val){
    physics.param.GRAVITY[2] = -val;
  }
  
  public void togglePause(){
    UPDATE_PHYSICS = !UPDATE_PHYSICS;
  }
  
  ControlP5 cp5;
  
  public void displayGUI(){
    noLights();
    peasycam.beginHUD();
    cp5.draw();
    peasycam.endHUD();
  }
  
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    cp5.setAutoDraw(false);

    int sx, sy, px, py, oy;
    sx = 100; sy = 14; oy = (int)(sy*1.4f);
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - CLOTH
    ////////////////////////////////////////////////////////////////////////////
    Group group_physics = cp5.addGroup("global");
    {
      group_physics.setHeight(20).setSize(gui_w, height)
      .setBackgroundColor(color(0, 204)).setColorBackground(color(0, 204));
      group_physics.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      int bsx = (gui_w-40)/3;
      cp5.addButton("rebuild").setGroup(group_physics).plugTo(this, "createBodies").setSize(bsx, 18).setPosition(px, py);
      cp5.addButton("pause")  .setGroup(group_physics).plugTo(this, "togglePause").setSize(bsx, 18).setPosition(px+=bsx+10, py);
      cp5.addButton("cam_0")   .setGroup(group_physics).plugTo(this, "resetCam").setSize(bsx, 18).setPosition(px+=bsx+10, py);
      
      px = 10; 
      cp5.addSlider("gravity").setGroup(group_physics).setSize(sx, sy).setPosition(px, py+=(int)(oy*1.5f))
          .setRange(0, 1).setValue(physics.param.GRAVITY[1]).plugTo(this, "setGravity");
      
      cp5.addSlider("iter: springs").setGroup(group_physics).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 20).setValue(physics.param.iterations_springs).plugTo( physics.param, "iterations_springs");
      
      cp5.addSlider("iter: collisions").setGroup(group_physics).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 8).setValue(physics.param.iterations_collisions).plugTo( physics.param, "iterations_collisions");
      
      cp5.addRadio("setDisplayMode").setGroup(group_physics).setSize(sy,sy).setPosition(px, py+=(int)(oy*1.4f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("springs: colored",0)
          .addItem("springs: tension",1)
          .activate(DISPLAY_MODE);
      
      cp5.addCheckBox("setDisplayTypes").setGroup(group_physics).setSize(sy,sy).setPosition(px, py+=(int)(oy*2.4f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("PARTICLES", 0).activate(DISPLAY_PARTICLES ? 0 : 5)
          .addItem("MESH "    , 1).activate(DISPLAY_MESH      ? 1 : 5)
          .addItem("SRPINGS"  , 2).activate(DISPLAY_SRPINGS   ? 2 : 5)
          .addItem("NORMALS"  , 3).activate(DISPLAY_NORMALS   ? 3 : 5)
          .addItem("WIREFRAME", 4).activate(DISPLAY_WIREFRAME ? 4 : 5);
    }
    
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - SPRINGS
    ////////////////////////////////////////////////////////////////////////////
    Group group_springs = cp5.addGroup("springs");
    {
      Group group_cloth = group_springs;
      
      group_cloth.setHeight(20).setSize(gui_w, 210)
      .setBackgroundColor(color(0, 204)).setColorBackground(color(0, 204));
      group_cloth.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;

      cp5.addSlider("Cloth.tensile").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.01f, 1).setValue(param_cloth_spring.damp_dec).plugTo(param_cloth_spring, "damp_dec");
      
      cp5.addSlider("Cloth.pressure").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.01f, 1).setValue(param_cloth_spring.damp_inc).plugTo(param_cloth_spring, "damp_inc");

      cp5.addSlider("Cube.tensile").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=(int)(oy*2))
          .setRange(0.01f, 1).setValue(param_cube_spring.damp_dec).plugTo(param_cube_spring, "damp_dec");
  
      cp5.addSlider("Cube.pressure").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.01f, 1).setValue(param_cube_spring.damp_inc).plugTo(param_cube_spring, "damp_inc");
  
      cp5.addSlider("Ball.tensile").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=(int)(oy*2))
          .setRange(0.01f, 1).setValue(param_ball_spring.damp_dec).plugTo(param_ball_spring, "damp_dec");

      cp5.addSlider("Ball.pressure").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.01f, 1).setValue(param_ball_spring.damp_inc).plugTo(param_ball_spring, "damp_inc");

    }
   
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_springs)
      .addItem(group_physics)
      .open(0, 1, 2);
   
  }
  

  
  


  public static void main(String args[]) {
    PApplet.main(new String[] { Softbody3D_Playground.class.getName() });
  }
}