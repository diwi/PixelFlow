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
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint3D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle3D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics3D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies3D.SoftBody3D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies3D.SoftCube;

import peasy.CameraState;
import peasy.PeasyCam;
import processing.core.*;
import processing.opengl.PGraphics3D;

public class VerletPhysics_Cloth_3D extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = viewport_w - gui_w;
  int gui_y = 0;
  
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
  SoftCube cube2  = new SoftCube();
  PeasyCam peasycam;

  
  // list, that wills store the cloths
  ArrayList<SoftBody3D> softbodies = new ArrayList<SoftBody3D>();

  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 0;
  
  // entities to display
  boolean DISPLAY_PARTICLES      = !true;
  boolean DISPLAY_MESH           = true;
  boolean DISPLAY_SRPINGS        = true;
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

  public void settings(){
    size(viewport_w, viewport_h, P3D); 
    smooth(4);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    peasycam = new PeasyCam(this, 2.181, -116.050, 293.648, 1518.898);
    peasycam.setMaximumDistance(10000);
    peasycam.setMinimumDistance(0.1f);
    peasycam.setRotations( -1.014,   0.858,  -0.461);
    
    cam_state_0 = peasycam.getState();
    
    float fov = PI/3.0f;
    float cameraZ = (height/2.0f) / tan(fov/2.0f);
    perspective(fov, width/(float)(height), cameraZ/100.0f, cameraZ*20.0f);
    


    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
//    context.printGL();
    
    physics = new VerletPhysics3D();

    int cs = 1500;
    physics.param.GRAVITY = new float[]{ 0, 0, -0.1f};
    physics.param.bounds  = new float[]{ -cs, -cs, 0, +cs, +cs, +cs };
    physics.param.iterations_collisions = 4;
    physics.param.iterations_springs    = 4;
    
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
    param_spring_cloth.damp_inc = 0.099999f;
    
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

//    createGUI();

    frameRate(600);
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
    
    cloth.setParticleColor(color(64));
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
    
    cube.setParticleColor(color(255, 180,   0));
    cube.setParam(param_particle_cube);
    cube.setParam(param_spring_cube);
    cube.self_collisions = false;
    cube.self_collisions = true;
    cube.collision_radius_scale = 1f;
    cube.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
//    cube.getNode(              0, 0, 0).enable(false, false, false); // fix node to current location
    cube.createParticlesShape(this);
    


    nodex_x = 3;
    nodes_y = 3;
    nodes_z = 25;
    nodes_r = 10;
    nodes_start_x = 500;
    nodes_start_y = -nodex_x * nodes_r * 4;
    nodes_start_z = nodes_y * nodes_r*2+200;
    
    cube2.setParticleColor(color(40, 180, 255));
    cube2.setParam(param_particle_cube);
    cube2.setParam(param_spring_cube);
    cube2.self_collisions = false;
    cube2.self_collisions = true;
    cube2.collision_radius_scale = 1f;
    cube2.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
//    cube.getNode(              0, 0, 0).enable(false, false, false); // fix node to current location
    cube2.createParticlesShape(this);
    
//    SpringConstraint.makeAllSpringsBidirectional(physics.getParticles());
    
    NUM_SPRINGS   = SpringConstraint3D.getSpringCount(physics.getParticles(), true);
    NUM_PARTICLES = physics.getParticlesCount();
  }




  

  
  // transformation matrices
  PMatrix3D pg_projmodelview     = new PMatrix3D();
  PMatrix3D pg_projmodelview_inv = new PMatrix3D();
  // vec4-buffers, for transformation
  float[]   mouse_world     = new float[4];
  float[]   mouse_screen    = new float[4];
  float[]   particle_world  = new float[4];
  float[]   particle_screen = new float[4];
  float     particle_screen_z;
  
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
  
  
  
  
  VerletParticle3D particle_nearest = null;
  
  
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
      if(!DELETE_SPRINGS && particle_nearest != null){
        if(mouseButton == LEFT  ) particle_nearest.enable(true, true, true);
        if(mouseButton == CENTER) particle_nearest.enable(true, false, false);
        particle_nearest = null;
      }
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

    peasycam.setActive(MOVE_CAM || (!SNAP_PARTICLE));
 
    // update physics simulation
    if(UPDATE_PHYSICS){
      physics.update(1);
    }
    

    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // RENDER this madness
    ////////////////////////////////////////////////////////////////////////////
    background(92);
    
    strokeWeight(2);
    displayGridXY(20, 100);
    displayGizmo(1000);
    displayAABB(physics.param.bounds);
    
//    lights();
//    directionalLight(92,64,64, 1, 0, 1);
//    specular(255,0,0);
//    shininess(5);
//    lights();
//    pointLight(255, 255, 255,  500,  500,  1000);
    pointLight(220, 180, 140, -1000, -1000, -10);
  //  directionalLight(255, 255, 255, -1, -1, -1);
    
    ambientLight(128, 128, 128);
    directionalLight(200, 200, 200, -1, -1, -1);
    lightFalloff(1.0f, 0.001f, 0.0f);
//    lightSpecular(204, 204, 204);
//    specular(128, 128, 128);
//    shininess(64);
    



    // 1) particles
    if(DISPLAY_PARTICLES)
    {
      for(SoftBody3D body : softbodies){
//        body.use_particles_color = (DISPLAY_MODE == 0);
        body.use_particles_color = true;
        body.drawParticles(this.g);
      }
    }
    
    if(DISPLAY_SRPINGS){
      // 2) springs
      for(SoftBody3D body : softbodies){
        if(body != cloth || DISPLAY_MODE == 1){
          body.DISPLAY_SPRINGS_BEND   = DISPLAY_SPRINGS_BEND;
          body.DISPLAY_SPRINGS_SHEAR  = DISPLAY_SPRINGS_SHEAR;
          body.DISPLAY_SPRINGS_STRUCT = DISPLAY_SPRINGS_STRUCT;
          
  //        body.drawSprings(this.g, SpringConstraint3D.TYPE.BEND  , DISPLAY_MODE);
  //        body.drawSprings(this.g, SpringConstraint3D.TYPE.SHEAR , DISPLAY_MODE);
  //        body.drawSprings(this.g, SpringConstraint3D.TYPE.STRUCT, DISPLAY_MODE);
  
          body.displaySprings(this.g, DISPLAY_MODE);
        }
      }
    }
    if(DISPLAY_MESH){
      if(DISPLAY_MODE == 0){
        displayCloth((SoftCube) softbodies.get(0));
      }
    }
    
    
    // interaction stuff
    displayMouseInteraction();


    // some info, windows title
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  
  
  public void displayCloth(SoftCube cloth){
    boolean DRAW_NORMALS = false;
    

    
    int idx, count;
    int nodes_x = cloth.nodes_x;
    int nodes_y = cloth.nodes_y;
    
    float[][] normals = new float[nodes_x * nodes_y][3];
    
    // compute normals  
    float nx, ny, nz;
    float[][] cross = new float[4][3];
    
    for(int v = 0; v < nodes_y; v++){
      for(int u = 0; u < nodes_x; u++){
        idx = v * nodes_x + u;
        
        VerletParticle3D pC = cloth.getNode(u, v  , 0);
        VerletParticle3D pT = cloth.getNode(u, v-1, 0);
        VerletParticle3D pB = cloth.getNode(u, v+1, 0);
        VerletParticle3D pL = cloth.getNode(u-1, v, 0);
        VerletParticle3D pR = cloth.getNode(u+1, v, 0);
        
        count  = cross(pC, pT, pR, cross[0]);
        count += cross(pC, pR, pB, cross[count]);
        count += cross(pC, pB, pL, cross[count]);
        count += cross(pC, pL, pT, cross[count]);
   
        nx = ny = nz = 0;
        for(int k = 0; k < count; k++){
          nx += cross[k][0];
          ny += cross[k][1];
          nz += cross[k][2];
        }
        
        float dd_sq  = nx*nx + ny*ny + nz*nz;
        float dd_inv = 1f/(float)(Math.sqrt(dd_sq)+0.00001f);
        
        normals[idx][0] = nx * dd_inv;
        normals[idx][1] = ny * dd_inv;
        normals[idx][2] = nz * dd_inv;  
      }
    }
    
 
   
    
//    cloth.nodes_x;
    float norm_len = 20f;

    VerletParticle3D p;
//    lights();
    fill(255,200,100);
    noStroke();
    strokeWeight(0.4f);
    stroke(0);
    for(int v = 0; v < nodes_y-1; v++){
      beginShape(QUAD_STRIP);
      for(int u = 0; u < nodes_x; u++){
        
        idx = v * nodes_x + u;
        p = cloth.getNode(u, v+0, 0);
        nx = normals[idx][0] * norm_len;
        ny = normals[idx][1] * norm_len;
        nz = normals[idx][2] * norm_len;
        normal(-nx, -ny, -nz);
        vertex(p.cx, p.cy, p.cz);
        
        idx = (v+1) * nodes_x + u;
        p = cloth.getNode(u, v+1, 0);
        nx = normals[idx][0] * norm_len;
        ny = normals[idx][1] * norm_len;
        nz = normals[idx][2] * norm_len;
        normal(-nx, -ny, -nz);
        vertex(p.cx, p.cy, p.cz);
      }
      endShape();
    }
    

   
    
    if(DRAW_NORMALS){
      stroke(0);
      strokeWeight(0.5f);
      for(int v = 0; v < nodes_y; v++){
        beginShape(LINES);
        for(int u = 0; u < nodes_x; u++){
          idx = v * nodes_x + u;
          p = cloth.getNode(u, v, 0);
          nx = normals[idx][0] * norm_len;
          ny = normals[idx][1] * norm_len;
          nz = normals[idx][2] * norm_len;
          vertex(p.cx   , p.cy   , p.cz   );
          vertex(p.cx+nx, p.cy+ny, p.cz+nz);
        }
        endShape();
      }
    }
  }
  
  
  
  
  
  
  
  
  
  
  
  

  public int cross(VerletParticle3D p, VerletParticle3D pA, VerletParticle3D pB, float[] cross){
    if(pA == null || pB == null) return 0;
    float dxA = pA.cx - p.cx;
    float dyA = pA.cy - p.cy;
    float dzA = pA.cz - p.cz;
    
    float dxB = pB.cx - p.cx;
    float dyB = pB.cy - p.cy;
    float dzB = pB.cz - p.cz;
    
    cross[0] = dyA * dzB - dyB * dzA;
    cross[1] = dzA * dxB - dzB * dxA;
    cross[2] = dxA * dyB - dxB * dyA;
    return 1;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
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
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 




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
  

  public static void main(String args[]) {
    PApplet.main(new String[] { VerletPhysics_Cloth_3D.class.getName() });
  }
}