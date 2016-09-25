/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package VerletPhysics_Cloth_3D;




import java.util.ArrayList;
import java.util.Locale;

import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.filter.Filter;
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint3D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle3D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics3D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies3D.SoftBody3D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies3D.SoftCube;

import peasy.CameraState;
import peasy.PeasyCam;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;

public class VerletPhysics_Cloth_3D extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = viewport_w - gui_w;
  int gui_y = 0;
  
  PixelFlow context;
  
  // physics simulation
  VerletPhysics3D physics;
  
  // particle parameters
  VerletParticle3D.Param param_particle_cloth = new VerletParticle3D.Param();
  VerletParticle3D.Param param_particle_cube  = new VerletParticle3D.Param();
  
  // spring parameters
  SpringConstraint3D.Param param_spring_cloth = new SpringConstraint3D.Param();
  SpringConstraint3D.Param param_spring_cube  = new SpringConstraint3D.Param();


  // cloth objects
  SoftCube cloth = new SoftCube();
  SoftCube cube  = new SoftCube();
  SoftCube cube2 = new SoftCube();
  PeasyCam peasycam;

  
  // list, that wills store the cloths
  ArrayList<SoftBody3D> softbodies = new ArrayList<SoftBody3D>();

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
  boolean NEED_REBUILD = true;
  
  // just for the window title-info
  int NUM_SPRINGS;
  int NUM_PARTICLES;
  
  CameraState cam_state_0;
  
  PGraphics2D texture;

  public void settings(){
    size(viewport_w, viewport_h, P3D); 
    smooth(8);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    double   distance = 1518.898;
    double[] look_at  = {69.042,  26.385,   5.913};
    double[] rotation = {-0.652,   0.894,  -0.814};
    
    peasycam = new PeasyCam(this, look_at[0], look_at[1], look_at[2], distance);
    peasycam.setMaximumDistance(10000);
    peasycam.setMinimumDistance(0.1f);
    peasycam.setRotations(rotation[0], rotation[1], rotation[2]);
    
    cam_state_0 = peasycam.getState();
    
    float fov = PI/3.0f;
    float cameraZ = (height/2.0f) / tan(fov/2.0f);
    perspective(fov, width/(float)(height), cameraZ/100.0f, cameraZ*20.0f);
    

    // main library context
    context = new PixelFlow(this);
    context.print();
    context.printGL();
    
    physics = new VerletPhysics3D();

    int cs = 1500;
    physics.param.GRAVITY = new float[]{ 0, 0, -0.1f};
    physics.param.bounds  = new float[]{ -cs, -cs, 0, +cs, +cs, +cs };
    physics.param.iterations_collisions = 2;
    physics.param.iterations_springs    = 8;
    
    // particle parameters for Cloth
    param_particle_cloth.DAMP_BOUNDS    = 0.49999f;
    param_particle_cloth.DAMP_COLLISION = 0.99999f;
    param_particle_cloth.DAMP_VELOCITY  = 0.99991f; 
    
    // particle parameters for the cube
    param_particle_cube.DAMP_BOUNDS    = 0.89999f;
    param_particle_cube.DAMP_COLLISION = 0.99999f;
    param_particle_cube.DAMP_VELOCITY  = 0.99991f; 
   
   
    // spring parameters for Cloth
    param_spring_cloth.damp_dec = 0.999999f;
    param_spring_cloth.damp_inc = 0.059999f;
    
    // spring parameters for cube
    param_spring_cube.damp_dec = 0.089999f;
    param_spring_cube.damp_inc = 0.089999f;
    
    // initial cloth building parameters, both cloth start the same
    cloth.CREATE_STRUCT_SPRINGS = true;
    cloth.CREATE_SHEAR_SPRINGS  = true;
    cloth.CREATE_BEND_SPRINGS   = true;
    cloth.bend_spring_mode      = 0;
    cloth.bend_spring_dist      = 2;
    
    
    cube.CREATE_STRUCT_SPRINGS = true;
    cube.CREATE_SHEAR_SPRINGS  = true;
    cube.CREATE_BEND_SPRINGS   = true;
    cube.bend_spring_mode      = 0;
    cube.bend_spring_dist      = 2;
    
    cube2.CREATE_STRUCT_SPRINGS = true;
    cube2.CREATE_SHEAR_SPRINGS  = true;
    cube2.CREATE_BEND_SPRINGS   = true;
    cube2.bend_spring_mode      = 0;
    cube2.bend_spring_dist      = 2;
    
    createClothTexture();

    cloth.texture_XYp = texture;

//    createGUI();

    frameRate(600);
  }
  
  
  public void createClothTexture(){
    PFont font = createFont("Calibri", 200);

    int tex_w = 1024;
    int tex_h = 1024;

    // create texture for text shadow
    PGraphics2D pg_tmp = (PGraphics2D) createGraphics(tex_w, tex_h, P2D);
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
    Filter.get(context).gaussblur.apply(pg_shadow, pg_shadow, pg_tmp, 15);
    Filter.get(context).multiply .apply(pg_shadow, pg_shadow, new float[]{1,1,1,3});
    Filter.get(context).gaussblur.apply(pg_shadow, pg_shadow, pg_tmp, 15);


    // now create the real texture
    texture = (PGraphics2D) createGraphics(tex_w, tex_h, P2D);
    texture.smooth(8);
    texture.beginDraw();
    texture.background(255,200,50);

    // grid, for better contrast
    int num_lines = 40;
    float dx = tex_w/(float)(num_lines-1);
    float dy = tex_h/(float)(num_lines-1);
    texture.strokeWeight(1f);
    texture.stroke(0);
    for(int ix = 0; ix < num_lines; ix++){
      texture.line((int)(dx*ix), 0,(int)(dx*ix), tex_h);
      texture.line(0, (int)(dy*ix), tex_w, (int)(dy*ix));
    }

    // some random rectangles
    texture.stroke(0);
    texture.strokeWeight(1f);
    texture.rectMode(CENTER);
    for(int i = 0; i < 256; i++){
      float rx      = random(tex_w);
      float ry      = random(tex_h);
      float rad     = random(10, 64);
      float shading = random(0, 255);
      texture.fill(shading*1.0f, shading*0.6f, shading*0.2f);
      texture.rect((int)rx, (int)ry, (int)rad, (int)rad, 3);
    }
    
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
  
  
  
  public void initBodies(){
    
    physics.reset();
    
    softbodies.clear();
    softbodies.add(cloth);
    softbodies.add(cube);
    softbodies.add(cube2);
    
    int nodex_x = 40;
    int nodes_y = 40;
    int nodes_z = 1;
    int nodes_r = 10;
    int nodes_start_x = 0;
    int nodes_start_y = 0;
    int nodes_start_z = nodes_y * nodes_r*2-200;
    
    cloth.setParticleColor(color(200,150,50));
    cloth.setMaterialColor(color(255,200,100));
    cloth.setParam(param_particle_cloth);
    cloth.setParam(param_spring_cloth);
    cloth.self_collisions = true;
    cloth.collision_radius_scale = 1f;
    cloth.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cloth.getNode(              0, 0, 0).enable(false, false, false); // fix node to current location
    cloth.getNode(cloth.nodes_x-1, 0, 0).enable(false, false, false); // fix node to current location
    cloth.createParticlesShape(this);
    
    nodex_x = 30;
    nodes_y = 2;
    nodes_z = 15;
    nodes_r = 10;
    nodes_start_x = 300;
    nodes_start_y = 300;
    nodes_start_z = nodes_y * nodes_r*2+200;
    
    cube.setParticleColor(color(64));
    cube.setMaterialColor(color(96));
    cube.setParam(param_particle_cube);
    cube.setParam(param_spring_cube);
    cube.self_collisions = false;
    cube.self_collisions = true;
    cube.collision_radius_scale = 1f;
    cube.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cube.createParticlesShape(this);
    

    nodex_x = 3;
    nodes_y = 3;
    nodes_z = 25;
    nodes_r = 10;
    nodes_start_x = 500;
    nodes_start_y = -nodex_x * nodes_r * 4;
    nodes_start_z = nodes_y * nodes_r*2+200;
    
    cube2.setParticleColor(color(10, 100, 120));
    cube2.setMaterialColor(color(40, 180, 255));
    cube2.setParam(param_particle_cube);
    cube2.setParam(param_spring_cube);
    cube2.self_collisions = false;
    cube2.self_collisions = true;
    cube2.collision_radius_scale = 1f;
    cube2.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cube2.createParticlesShape(this);
    
//    SpringConstraint.makeAllSpringsBidirectional(physics.getParticles());
    
    NUM_SPRINGS   = SpringConstraint3D.getSpringCount(physics.getParticles(), true);
    NUM_PARTICLES = physics.getParticlesCount();
  }




  

  
  //////////////////////////////////////////////////////////////////////////////
  // draw()
  //////////////////////////////////////////////////////////////////////////////
  
  public void draw() {
    
    
    if(NEED_REBUILD){
      initBodies();
      NEED_REBUILD = false;
    }
    
    pg_projmodelview    .set(((PGraphics3D) this.g).projmodelview);
    pg_projmodelview_inv.set(((PGraphics3D) this.g).projmodelview);
    pg_projmodelview_inv.invert();
    
    int particles_count = physics.getParticlesCount();
    VerletParticle3D[] particles = physics.getParticles();
    
    
    if(APPLY_WIND){
      for(int i = 0; i < particles_count; i++){
        VerletParticle3D pa = particles[i];
        pa.addForce(0, noise(i) *0.5f, 0);
      }
    }

 
    
    updateMouseInteractions();
    
    // disable peasycam-interaction while we edit the model
    peasycam.setActive(MOVE_CAM);
 
    // update physics simulation
    if(UPDATE_PHYSICS){
      physics.update(1);
    }
    
    
    // update normals of the body surfaces
    for(SoftBody3D body : softbodies){
      body.computeNormals();
    }
    

    
    
    ////////////////////////////////////////////////////////////////////////////
    // RENDER this madness
    ////////////////////////////////////////////////////////////////////////////
    background(92);
    
    
    // XY-grid, gizmo, scene-bounds
    strokeWeight(2);
    displayGridXY(20, 100);
    displayGizmo(1000);
    displayAABB(physics.param.bounds);
    
    // lights, material props
    // lights();
    pointLight(220, 180, 140, -1000, -1000, -100);
    ambientLight(96, 96, 96);
    directionalLight(210, 210, 210, -1, -1, -1);
    lightFalloff(1.0f, 0.001f, 0.0f);
    lightSpecular(255, 0, 0);
    specular(255, 0, 0);
    shininess(5);
    
    
    // 1) particles
    if(DISPLAY_PARTICLES)
    {
      for(SoftBody3D body : softbodies){
//        body.use_particles_color = (DISPLAY_MODE == 0);
        body.use_particles_color = true;
        body.drawParticles(this.g);
      }
    }
    
    
    // 2) springs
    if(DISPLAY_SRPINGS){
      for(SoftBody3D body : softbodies){
        body.DISPLAY_SPRINGS_BEND   = DISPLAY_SPRINGS_BEND;
        body.DISPLAY_SPRINGS_SHEAR  = DISPLAY_SPRINGS_SHEAR;
        body.DISPLAY_SPRINGS_STRUCT = DISPLAY_SPRINGS_STRUCT;
        body.displaySprings(this.g, DISPLAY_MODE);
      }
    }
    
    
    // 3) mesh, solid
    if(DISPLAY_MESH){
      stroke(0);
      strokeWeight(0.1f);
      noStroke();
      for(SoftBody3D body : softbodies){
        body.displayMesh(this.g);
      }
    }
    
    
    // 4) normals
    if(DISPLAY_NORMALS){
      stroke(0);
      strokeWeight(0.5f);
      for(SoftBody3D body : softbodies){
        body.displayNormals(this.g);
      }
    }
    

    // 5) interaction stuff
    displayMouseInteraction();


    // some info, windows title
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
  
  
  
  // transformation matrices
  PMatrix3D pg_projmodelview     = new PMatrix3D();
  PMatrix3D pg_projmodelview_inv = new PMatrix3D();
  // vec4-buffers, for transformation
  float[]   mouse_world     = new float[4];
  float[]   mouse_screen    = new float[4];
  float[]   particle_world  = new float[4];
  float[]   particle_screen = new float[4];
  
  float            particle_screen_z;
  VerletParticle3D particle_nearest = null;
  
  // this transforms a coordinate (vec4) from model-space to screen-space
  public void transformToScreen(VerletParticle3D particle, float[] buf_world, float[] buf_screen){
    buf_world[0] = particle.cx;
    buf_world[1] = particle.cy;
    buf_world[2] = particle.cz;
    buf_world[3] = 1;
    transformToScreen(buf_world, buf_screen);
  }
  public void transformToScreen(float[] buf_world, float[] buf_screen){
    buf_world[3] = 1;
    pg_projmodelview.mult(buf_world, buf_screen);
    float w_inv = 1f/buf_screen[3];
    buf_screen[0] = ((buf_screen[0] * w_inv) * +0.5f + 0.5f) * width;
    buf_screen[1] = ((buf_screen[1] * w_inv) * -0.5f + 0.5f) * height;
    buf_screen[2] = ((buf_screen[2] * w_inv) * +0.5f + 0.5f);
  }
  
  // this transforms a coordinate (vec4) from screen-space to model-space
  public void transformToWorld(float[] buf_screen, float[] buf_world){
    buf_screen[0] = ((buf_screen[0]/(float)width ) * 2 - 1) * +1;
    buf_screen[1] = ((buf_screen[1]/(float)height) * 2 - 1) * -1;
    buf_screen[2] = ((buf_screen[2]              ) * 2 - 1) * +1;
    buf_screen[3] = 1;
    pg_projmodelview_inv.mult(buf_screen, buf_world);
    float w_inv = 1f/buf_world[3];
    buf_world[0] *= w_inv;
    buf_world[1] *= w_inv;
    buf_world[2] *= w_inv;
  }
  

  
  void findNearestParticle(float mx, float my, float radius){
    int particles_count = physics.getParticlesCount();
    VerletParticle3D[] particles = physics.getParticles();
    
    float dd_min = SNAP_RADIUS * radius;

    particle_nearest = null;
    // transform Particles: world -> screen
    for(int i = 0; i < particles_count; i++){
      VerletParticle3D pa = particles[i];
      transformToScreen(pa, particle_world, particle_screen);
      float dx = particle_screen[0] - mx;
      float dy = particle_screen[1] - my;
      float dd_sq = dx*dx + dy*dy;
      if(dd_sq < dd_min){
        dd_min = dd_sq;
        particle_nearest = pa;
        particle_screen_z = particle_screen[2];
      }
    }  
  }
  
  ArrayList<VerletParticle3D> particles_within_radius = new ArrayList<VerletParticle3D>();
  void findParticlesWithinRadius(float mx, float my, float radius){
    int particles_count = physics.getParticlesCount();
    VerletParticle3D[] particles = physics.getParticles();
    
    float dd_min = radius * radius;
    particles_within_radius.clear();

    // transform Particles: world -> screen
    for(int i = 0; i < particles_count; i++){
      VerletParticle3D pa = particles[i];
      transformToScreen(pa, particle_world, particle_screen);
      float dx = particle_screen[0] - mx;
      float dy = particle_screen[1] - my;
      float dd_sq = dx*dx + dy*dy;
      if(dd_sq < dd_min){
        particles_within_radius.add(pa);
      }
    }  
  }
  
  boolean APPLY_WIND     = false;
  boolean MOVE_CAM       = false;
  boolean MOVE_PARTICLE  = false;
  boolean SNAP_PARTICLE  = false;
  float   SNAP_RADIUS    = 60;
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
        if(MOVE_PARTICLE && particle_nearest != null){
          if(mouseButton == LEFT  ) particle_nearest.enable(true, true, true);
          if(mouseButton == CENTER) particle_nearest.enable(true, false, false);
          particle_nearest = null;
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
    if(key == 'r') initBodies();
    if(key == 's') repairAllSprings();
    if(key == 'm') applySpringMemoryEffect();
    if(key == '1') DISPLAY_MODE = 0;
    if(key == '2') DISPLAY_MODE = 1;
    
    if(key == '3') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
    if(key == '4') DISPLAY_MESH      = !DISPLAY_MESH;
    if(key == '5') DISPLAY_SRPINGS   = !DISPLAY_SRPINGS;
    if(key == '6') DISPLAY_NORMALS   = !DISPLAY_NORMALS;

    
    if(key == ' ') UPDATE_PHYSICS = !UPDATE_PHYSICS;
    if(key == 'c') printCam();
    if(key == 'v') peasycam.setState(cam_state_0, 700);
    
    MOVE_CAM = false; 
  }

  
  
  public void updateMouseInteractions(){
    
    // deleting springs/constraints between particles
    if(DELETE_SPRINGS){
      findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
      for(VerletParticle3D particle : particles_within_radius){
        SpringConstraint3D.deactivateSprings(particle);
        particle.collision_group = physics.getNewCollisionGroupId();
        particle.rad_collision = particle.rad;
        particle.all_springs_deactivated = true;
      }
    } 
    
    if(!MOVE_PARTICLE){
      findNearestParticle(mouseX, mouseY, SNAP_RADIUS);
      SNAP_PARTICLE = particle_nearest != null;
    }
    
    if(SNAP_PARTICLE){
      mouse_screen[0] = mouseX;
      mouse_screen[1] = mouseY;
      mouse_screen[2] = particle_screen_z;
      transformToWorld(mouse_screen, mouse_world);

      if(MOVE_PARTICLE){
        particle_nearest.enable(false, false, false);
        particle_nearest.moveTo(mouse_world, 0.1f);
      }
    }
  }
  
  
  public void displayMouseInteraction(){
    if(SNAP_PARTICLE){
      int col_snap         = color(255, 100, 200);
      int col_move_release = color(64, 180, 0);
      int col_move_fixed   = color(255, 30, 10); 
      
      int col = col_snap;
      if(MOVE_PARTICLE){
        col = col_move_release;
        if(mouseButton == CENTER){
          col = col_move_fixed;
        }
      }
       
      strokeWeight(1);
      stroke(col);
      line(particle_nearest.cx, particle_nearest.cy, particle_nearest.cz, mouse_world[0], mouse_world[1], mouse_world[2]);
    
      strokeWeight(10);
      stroke(col);
      point(particle_nearest.cx, particle_nearest.cy, particle_nearest.cz);
      
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
  }
  
  
  
  
  // this resets all springs and particles, to some of its initial states
  // can be used after deactivating springs with the mouse
  public void repairAllSprings(){
    SpringConstraint3D.makeAllSpringsUnidirectional(physics.getParticles());
    for(SoftBody3D body : softbodies){
      for(VerletParticle3D pa : body.particles){
        pa.setCollisionGroup(body.collision_group_id);
        pa.setRadiusCollision(pa.rad());
      }
    }
  }
  
  
  // update all springs rest-lengths, based on current particle position
  // the effect is, that the body keeps the current shape
  public void applySpringMemoryEffect(){
    for(SoftBody3D body : softbodies){
      for(VerletParticle3D pa : body.particles){
        for(int i = 0; i < pa.spring_count; i++){
          pa.springs[i].updateRestlength();
        }
      }
    }
  }
  
  

  void printCam(){
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
      strokeWeight(2);
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
  

  

  

  public static void main(String args[]) {
    PApplet.main(new String[] { VerletPhysics_Cloth_3D.class.getName() });
  }
}