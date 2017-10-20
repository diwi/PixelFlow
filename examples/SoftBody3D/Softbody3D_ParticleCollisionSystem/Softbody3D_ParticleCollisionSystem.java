/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody3D.Softbody3D_ParticleCollisionSystem;



import java.util.ArrayList;
import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;
import com.thomasdiewald.pixelflow.java.utils.DwCoordinateTransform;


import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import peasy.CameraState;
import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PShape;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;

public class Softbody3D_ParticleCollisionSystem extends PApplet {
  
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
  
  // particle system, cpu
  ParticleSystem particlesystem;
  

  // camera
  PeasyCam peasycam;
  CameraState cam_state_0;
  
  // cloth texture
  PGraphics2D texture;
  
  // global states
  int BACKGROUND_COLOR = 92;

  boolean UPDATE_PHYSICS         = true;
  
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
    
    // particle system object
    particlesystem = new ParticleSystem(this, param_physics.bounds);
    
    // set some parameters
    particlesystem.PARTICLE_COUNT              = 1000;
    particlesystem.PARTICLE_SCREEN_FILL_FACTOR = 0.50f;
    
    particlesystem.particle_param.DAMP_BOUNDS    = 0.99999f;
    particlesystem.particle_param.DAMP_COLLISION = 0.99999f;
    particlesystem.particle_param.DAMP_VELOCITY  = 0.99999f;
    
    particlesystem.initParticles();
    
    // modelview/projection
    createCam();
    
    // gui
    createGUI();

    frameRate(600);
  }
  
  
  


  
  //////////////////////////////////////////////////////////////////////////////
  // draw()
  //////////////////////////////////////////////////////////////////////////////
  
  public void draw() {
    
    // update interactions, like editing particle positions, deleting springs, etc...
    updateMouseInteractions();
    
    physics.setParticles(particlesystem.particles, particlesystem.particles.length);
    
    // update physics simulation
    if(UPDATE_PHYSICS){
      physics.update(1);
    }
    

    // disable peasycam-interaction while we edit the model
    peasycam.setActive(MOVE_CAM);
    
    displayScene();

    displayGUI();

    // info
    int NUM_SPRINGS   = physics.getSpringCount();
    int NUM_PARTICLES = physics.getParticlesCount();
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  
 
  public void displayScene(){

    background(BACKGROUND_COLOR);
    
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
    
    particlesystem.display(this.g);

    displayMouseInteraction();
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

  }
  
  
  
  

  
  
  
  boolean MOVE_CAM       = false;
  boolean MOVE_PARTICLE  = false;
  boolean SNAP_PARTICLE  = false;
  float   SNAP_RADIUS    = 30;
  float   DELETE_RADIUS  = 15;

  public void mousePressed(){
    if((mouseButton == LEFT || mouseButton == CENTER) && !MOVE_CAM){
      MOVE_PARTICLE = true;
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
    }
   
    MOVE_PARTICLE  = false;
  }
  
  public void keyPressed(){
    if(key == CODED){
      if(keyCode == ALT){
        MOVE_CAM = true;
      }
    }
  }
  
  public void keyReleased(){
    if(key == ' ') UPDATE_PHYSICS = !UPDATE_PHYSICS;
    
    if(key == 'c') printCam();
    if(key == 'v') resetCam();
    
    MOVE_CAM = false; 
  }


  
  
  
  public void createCam(){
    // camera - modelview
    double   distance = 3518.898;
    double[] look_at  = {257.660, -332.919, 148.795};
    double[] rotation = { -0.599,   0.723,  -0.802};
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
    }
    
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - PARTICLES
    ////////////////////////////////////////////////////////////////////////////
    Group group_particles = cp5.addGroup("Particles");
    {
      
      group_particles.setHeight(20).setSize(gui_w, 200)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_particles.getCaptionLabel().align(CENTER, CENTER);
      
      sx = 100; px = 10; py = 10;oy = (int)(sy*1.4f);
      
      cp5.addButton("reset particles").setGroup(group_particles).setWidth(160).setPosition(10, 10).plugTo(particlesystem, "initParticles");

      cp5.addSlider("Particle count").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy+10)
          .setRange(10, 10000).setValue(particlesystem.PARTICLE_COUNT).plugTo(particlesystem, "setParticleCount");
      
      cp5.addSlider("Fill Factor").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
      .setRange(0.2f, 1.5f).setValue(particlesystem.PARTICLE_SCREEN_FILL_FACTOR).plugTo(particlesystem, "setFillFactor");
      
      cp5.addSlider("VELOCITY").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy+10)
          .setRange(0.85f, 1.0f).setValue(particlesystem.particle_param.DAMP_VELOCITY).plugTo(particlesystem.particle_param, "DAMP_VELOCITY");
      
      cp5.addSlider("SPRINGINESS").setGroup(group_particles).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1f).setValue(particlesystem.particle_param.DAMP_COLLISION).plugTo(particlesystem.particle_param, "DAMP_COLLISION");
      
   
    }
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_particles)
      .addItem(group_physics)
      .open(0, 1);
   
  }
  

  
  


  public static void main(String args[]) {
    PApplet.main(new String[] { Softbody3D_ParticleCollisionSystem.class.getName() });
  }
}