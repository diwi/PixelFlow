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

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import peasy.PeasyCam;
import peasycam.peasycam;
import processing.core.*;

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
  
  // spring parameters
  SpringConstraint3D.Param param_spring_cloth = new SpringConstraint3D.Param();
  
  // cloth objects
  SoftCube cloth = new SoftCube();
  
  PeasyCam peasycam;

  
  // list, that wills store the cloths
  ArrayList<SoftBody3D> softbodies = new ArrayList<SoftBody3D>();

  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 0;
  
  // entities to display
  boolean DISPLAY_PARTICLES      = true;
  boolean DISPLAY_SPRINGS_STRUCT = true;
  boolean DISPLAY_SPRINGS_SHEAR  = true;
  boolean DISPLAY_SPRINGS_BEND   = true;
  
  // first thing to do, inside draw()
  boolean NEED_REBUILD = true;
  
  // just for the window title-info
  int NUM_SPRINGS;
  int NUM_PARTICLES;
  

  public void settings(){
    size(viewport_w, viewport_h, P3D); 
    smooth(4);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    peasycam = new PeasyCam(this, 2.181, -116.050, 293.648, 1518.898);
    peasycam.setMaximumDistance(50000);
    peasycam.setRotations( -1.014,   0.858,  -0.461);

    
//    position: (1151.305, 727.349, 818.283)
//    rotation: ( -1.014,   0.858,  -0.461)
//    look-at:  (  2.181, -116.050, 293.648)
//    distance: (1518.898)
    
    printCam();
    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
//    context.printGL();
    
    physics = new VerletPhysics3D();

    int cs = 10000;
    physics.param.GRAVITY = new float[]{ 0, 0, -1};
    physics.param.bounds  = new float[]{ -cs, -cs, 0, +cs, +cs, +cs*2 };
    physics.param.iterations_collisions = 4;
    physics.param.iterations_springs    = 4;
    
    // particle parameters for Cloth
    param_particle_cloth.DAMP_BOUNDS          = 0.00f;
    param_particle_cloth.DAMP_COLLISION       = 0.99999f;
    param_particle_cloth.DAMP_VELOCITY        = 0.991f; 
   
    // spring parameters for Cloth
    param_spring_cloth.damp_dec = 0.899999f;
    param_spring_cloth.damp_inc = 0.8599f;
    
    // initial cloth building parameters, both cloth start the same
    cloth.CREATE_STRUCT_SPRINGS = true;
    cloth.CREATE_SHEAR_SPRINGS  = true;
    cloth.CREATE_BEND_SPRINGS   = !true;
    cloth.bend_spring_mode      = 0;
    cloth.bend_spring_dist      = 3;

//    createGUI();

    frameRate(60);
  }
  
  
  
  public void initBodies(){
    
    physics.reset();
    
    softbodies.clear();
    softbodies.add(cloth);

    int nodex_x = 30;
    int nodes_y = 30;
    int nodes_z = 1;
    int nodes_r = 10;
    int nodes_start_x = 0;
    int nodes_start_y = 0;
    int nodes_start_z = nodes_y * nodes_r*2 - 100;
    
    cloth.setParticleColor(color(255, 180,   0, 128));
    cloth.setParam(param_particle_cloth);
    cloth.setParam(param_spring_cloth);
    cloth.create(physics, nodex_x, nodes_y, nodes_z, nodes_r, nodes_start_x, nodes_start_y, nodes_start_z);
    cloth.getNode(              0, 0, 0).enable(false, false, false); // fix node to current location
    cloth.getNode(cloth.nodes_x-1, 0, 0).enable(false, false, false); // fix node to current location
    cloth.createParticlesShape(this);

//    SpringConstraint.makeAllSpringsBidirectional(physics.getParticles());
    
    NUM_SPRINGS   = SpringConstraint3D.getSpringCount(physics.getParticles(), true);
    NUM_PARTICLES = physics.getParticlesCount();
  }


  public void gizmo(float s){
    beginShape(LINES);
    stroke(255,0,0); vertex(0,0,0); vertex(s,0,0);
    stroke(0,255,0); vertex(0,0,0); vertex(0,s,0); 
    stroke(0,0,255); vertex(0,0,0); vertex(0,0,s); 
    endShape();
  }
  
  

  
  
  public void draw() {
    
    

    if(NEED_REBUILD){
      initBodies();
      NEED_REBUILD = false;
    }
    
    if(keyPressed){
      VerletParticle3D particle = physics.getParticles()[0];
      
      float speed = 20;
      float px = particle.cx;
      float py = particle.cy;
      float pz = particle.cz;
      if(key == 'q') px += speed;
      if(key == 'e') px -= speed;
      if(key == 'a') py += speed;
      if(key == 'd') py -= speed;
      if(key == 'w') pz += speed;
      if(key == 's') pz -= speed;
      particle.moveTo(px, py, pz, 0.2f);
    }
    
//    updateMouseInteractions();

    

    // update physics simulation
    physics.update(1);
    
    // render
    background(DISPLAY_MODE == 0 ?  255 : 92);
    
    strokeWeight(2);
    gizmo(1000);
    
    
    // 1) particles
    if(DISPLAY_PARTICLES){
      for(SoftBody3D body : softbodies){
        body.use_particles_color = (DISPLAY_MODE == 0);
        body.drawParticles(this.g);
      }
    }
    
    
    // 2) springs
    for(SoftBody3D body : softbodies){
      if(DISPLAY_SPRINGS_BEND  ) body.drawSprings(this.g, SpringConstraint3D.TYPE.BEND  , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_SHEAR ) body.drawSprings(this.g, SpringConstraint3D.TYPE.SHEAR , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_STRUCT) body.drawSprings(this.g, SpringConstraint3D.TYPE.STRUCT, DISPLAY_MODE);
    }

    // interaction stuff
//    if(DELETE_SPRINGS){
//      fill(255,64);
//      stroke(0);
//      strokeWeight(1);
//      ellipse(mouseX, mouseY, DELETE_RADIUS*2, DELETE_RADIUS*2);
//    }


    // some info, windows title
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
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
 
//  VerletParticle2D particle_mouse = null;
//  
//  public VerletParticle2D findNearestParticle(float mx, float my){
//    return findNearestParticle(mx, my, Float.MAX_VALUE);
//  }
//  
//  public VerletParticle2D findNearestParticle(float mx, float my, float search_radius){
//    float dd_min_sq = search_radius * search_radius;
//    VerletParticle2D[] particles = physics.getParticles();
//    VerletParticle2D particle = null;
//    for(int i = 0; i < particles.length; i++){
//      float dx = mx - particles[i].cx;
//      float dy = my - particles[i].cy;
//      float dd_sq =  dx*dx + dy*dy;
//      if( dd_sq < dd_min_sq){
//        dd_min_sq = dd_sq;
//        particle = particles[i];
//      }
//    }
//    return particle;
//  }
//  
//  public ArrayList<VerletParticle2D> findParticlesWithinRadius(float mx, float my, float search_radius){
//    float dd_min_sq = search_radius * search_radius;
//    VerletParticle2D[] particles = physics.getParticles();
//    ArrayList<VerletParticle2D> list = new ArrayList<VerletParticle2D>();
//    for(int i = 0; i < particles.length; i++){
//      float dx = mx - particles[i].cx;
//      float dy = my - particles[i].cy;
//      float dd_sq =  dx*dx + dy*dy;
//      if(dd_sq < dd_min_sq){
//        list.add(particles[i]);
//      }
//    }
//    return list;
//  }
//  
//  
//  public void updateMouseInteractions(){
//    // deleting springs/constraints between particles
//    if(DELETE_SPRINGS){
//      ArrayList<VerletParticle2D> list = findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
//      for(VerletParticle2D tmp : list){
//        SpringConstraint2D.deactivateSprings(tmp);
//        tmp.collision_group = physics.getNewCollisionGroupId();
//        tmp.rad_collision = tmp.rad;
//      }
//    } else {
//      if(particle_mouse != null) particle_mouse.moveTo(mouseX, mouseY, 0.2f);
//    }
//  }
//  
//  
//  boolean DELETE_SPRINGS = false;
//  float   DELETE_RADIUS = 20;
//
//  public void mousePressed(){
//    boolean mouseInteraction = true;
//    if(mouseInteraction){
//      if(mouseButton == RIGHT ) DELETE_SPRINGS = true; 
//      if(!DELETE_SPRINGS){
//        particle_mouse = findNearestParticle(mouseX, mouseY, 100);
//        if(particle_mouse != null) particle_mouse.enable(false, false, false);
//      }
//    }
//  }
//  
//  public void mouseReleased(){
//    if(!DELETE_SPRINGS && particle_mouse != null){
//      if(mouseButton == LEFT  ) particle_mouse.enable(true, true, true);
//      if(mouseButton == CENTER) particle_mouse.enable(true, false, false);
//      particle_mouse = null;
//    }
//    if(mouseButton == RIGHT ) DELETE_SPRINGS = false;
//  }
//  
  public void keyReleased(){
    if(key == 'r') initBodies();
    if(key == 's') repairAllSprings();
    if(key == 'm') applySpringMemoryEffect();
    if(key == '1') DISPLAY_MODE = 0;
    if(key == '2') DISPLAY_MODE = 1;
    if(key == 'p') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
    
    if(key == 'c') printCam();
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
  

  public static void main(String args[]) {
    PApplet.main(new String[] { VerletPhysics_Cloth_3D.class.getName() });
  }
}