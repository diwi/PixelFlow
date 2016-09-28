/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package SoftBody2D_Playground;




import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.particlephysics.DwParticle;
import com.thomasdiewald.pixelflow.java.particlephysics.DwParticle2D;
import com.thomasdiewald.pixelflow.java.particlephysics.DwPhysics;
import com.thomasdiewald.pixelflow.java.particlephysics.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.particlephysics.softbodies2D.DwSoftBody2D;
import com.thomasdiewald.pixelflow.java.particlephysics.softbodies2D.DwSoftCircle;
import com.thomasdiewald.pixelflow.java.particlephysics.softbodies2D.DwSoftGrid;

import processing.core.*;

public class SoftBody2D_Playground extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  
  // physics parameters
  DwPhysics.Param param_physics = new DwPhysics.Param();
  
  // particle parameters: same behavior for all
  DwParticle.Param param_particle = new DwParticle.Param();
  
  // spring parameters: different spring behavior for different bodies
  DwSpringConstraint.Param param_spring_cloth    = new DwSpringConstraint.Param();
  DwSpringConstraint.Param param_spring_softbody = new DwSpringConstraint.Param();
  DwSpringConstraint.Param param_spring_chain    = new DwSpringConstraint.Param();
  DwSpringConstraint.Param param_spring_circle   = new DwSpringConstraint.Param();
  
  // physics simulation
  DwPhysics<DwParticle2D> physics;
  
  // list, that wills store the cloths
  ArrayList<DwSoftBody2D> softbodies;
  

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
  
  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  

  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    DwPixelFlow context = new DwPixelFlow(this);
    context.print();
//    context.printGL();
    
    physics = new DwPhysics<DwParticle2D>(param_physics);

    // global physics parameters
    param_physics.GRAVITY = new float[]{ 0, 0.2f };
    param_physics.bounds  = new float[]{ 0, 0, width, height };
    param_physics.iterations_collisions = 4;
    param_physics.iterations_springs    = 4;
    
    // particle parameters
    param_particle.DAMP_BOUNDS     = 0.40f;
    param_particle.DAMP_COLLISION  = 0.9990f;
    param_particle.DAMP_VELOCITY   = 0.991f; 
    
    // spring parameters
    param_spring_cloth   .damp_dec = 0.999999f;
    param_spring_cloth   .damp_inc = 0.000599f;
    
    param_spring_softbody.damp_dec = 0.999999f;
    param_spring_softbody.damp_inc = 0.999999f;
    
    param_spring_chain   .damp_dec = 0.599999f;
    param_spring_chain   .damp_inc = 0.599999f;
    
    param_spring_circle  .damp_dec = 0.999999f;
    param_spring_circle  .damp_inc = 0.999999f;

    frameRate(60);
  }
  
  
  
  public void initBodies(){
    
    physics.reset();
    
    softbodies = new ArrayList<DwSoftBody2D>();
    
    
    // create some particle-bodies: Cloth / SoftBody
    int nodex_x, nodes_y, nodes_r;
    float nodes_start_x, nodes_start_y;

    // cloth
    {
      nodex_x = 30;
      nodes_y = 30;
      nodes_r = 7;
      nodes_start_x = 50;
      nodes_start_y = 70;
      DwSoftGrid body = new DwSoftGrid();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 2;
      body.setParam(param_particle);
      body.setParam(param_spring_cloth);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(             0, 0).enable(false, false, false); // fix node to current location
      body.getNode(body.nodes_x-1, 0).enable(false, false, false); // fix node to current location
      body.setParticleColor(color(255,180,0,160));
      body.createParticlesShape(this);
      softbodies.add(body);
    }
    
    // grid
    {
      nodex_x = 10;
      nodes_y = 20;
      nodes_r = 7;
      nodes_start_x = width/2;
      nodes_start_y = height/2;
      DwSoftGrid body = new DwSoftGrid();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 2;
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.setParticleColor(color(0,128));
      body.createParticlesShape(this);
      softbodies.add(body);
    }
    
    // grid
    {
      nodex_x = 7;
      nodes_y = 22;
      nodes_r = 7;
      nodes_start_x = 500;
      nodes_start_y = 300;
      DwSoftGrid body = new DwSoftGrid();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(0, 0).enable(false, false, false); // fix node to current location
      body.setParticleColor(color(0,180,255,160));
      body.createParticlesShape(this);
      softbodies.add(body);
    }
    
    // lattice girder
    {
      nodex_x = 15;
      nodes_y = 2;
      nodes_r = 20;
      nodes_start_x = 500;
      nodes_start_y = 100;
      DwSoftGrid body = new DwSoftGrid();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(0, 0).enable(false, false, false); // fix node to current location
      body.getNode(0, 1).enable(false, false, false); // fix node to current location
      body.setParticleColor(color(0,128));
      body.createParticlesShape(this);
      softbodies.add(body);
    }
    

    // chain
    {
      nodex_x = 70;
      nodes_y = 1;
      nodes_r = 10;
      nodes_start_x = 500;
      nodes_start_y = 200;
      DwSoftGrid body = new DwSoftGrid();
      body.CREATE_BEND_SPRINGS  = false;
      body.CREATE_SHEAR_SPRINGS = false;
      body.self_collisions      = true; // particles of this body can collide among themselves
      body.collision_radius_scale = 1.00f; // funny, if bigger than 1 and self_collisions = true
      body.setParam(param_particle);
      body.setParam(param_spring_chain);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode( 0, 0).enable(false, false, false); // fix node to current location
      body.getNode(35, 0).enable(false, false, false);
      body.setParticleColor(color(0,128));
      body.createParticlesShape(this);
      softbodies.add(body);
    }
    
    // circle
    {
      nodes_r = 10;
      nodes_start_x = 300;
      nodes_start_y = height-150;
      DwSoftCircle body = new DwSoftCircle();
      body.CREATE_BEND_SPRINGS  = false;
      body.CREATE_SHEAR_SPRINGS = false;
      body.bend_spring_mode = 3;
      body.setParam(param_particle);
      body.setParam(param_spring_circle);
      body.create(physics, nodes_start_x, nodes_start_y, 70, nodes_r);
      body.setParticleColor(color(0,160));
      body.createParticlesShape(this);
      softbodies.add(body);
    }

  }


  
  
  public void draw() {

    if(NEED_REBUILD){
      initBodies();
      NEED_REBUILD = false;
    }
    
    updateMouseInteractions();    
    
    // update physics simulation
    physics.update(1);
    
    // render
    background(DISPLAY_MODE == 0 ?  255 : 92);
    
    // 1) particles
    if(DISPLAY_PARTICLES){
      for(DwSoftBody2D body : softbodies){
        body.use_particles_color = (DISPLAY_MODE == 0);
        body.drawParticles(this.g);
      }
    }
    
    // 2) springs
    for(DwSoftBody2D body : softbodies){
      if(DISPLAY_SPRINGS_BEND  ) body.drawSprings(this.g, DwSpringConstraint.TYPE.BEND  , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_SHEAR ) body.drawSprings(this.g, DwSpringConstraint.TYPE.SHEAR , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_STRUCT) body.drawSprings(this.g, DwSpringConstraint.TYPE.STRUCT, DISPLAY_MODE);
    }

    // interaction stuff
    if(DELETE_SPRINGS){
      fill(255,64);
      stroke(0);
      strokeWeight(1);
      ellipse(mouseX, mouseY, DELETE_RADIUS*2, DELETE_RADIUS*2);
    }

    // info
    int NUM_SPRINGS   = physics.getSpringCount();
    int NUM_PARTICLES = physics.getParticlesCount();
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  
  
  // this resets all springs and particles, to some of its initial states
  // can be used after deactivating springs with the mouse
  public void repairAllSprings(){
    for(DwSoftBody2D body : softbodies){
      for(DwParticle pa : body.particles){
        pa.setCollisionGroup(body.collision_group_id);
        pa.setRadiusCollision(pa.rad());
        pa.enableAllSprings(true);
      }
    }
  }
  
  
  // update all springs rest-lengths, based on current particle position
  // the effect is, that the body keeps the current shape
  public void applySpringMemoryEffect(){
    ArrayList<DwSpringConstraint> springs = physics.getSprings();
    for(DwSpringConstraint spring : springs){
      spring.updateRestlength();
    }
  }
  
  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 
  DwParticle particle_mouse = null;
  
  public DwParticle findNearestParticle(float mx, float my){
    return findNearestParticle(mx, my, Float.MAX_VALUE);
  }
  
  public DwParticle findNearestParticle(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    DwParticle2D[] particles = physics.getParticles();
    DwParticle particle = null;
    for(int i = 0; i < particles.length; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq =  dx*dx + dy*dy;
      if( dd_sq < dd_min_sq){
        dd_min_sq = dd_sq;
        particle = particles[i];
      }
    }
    return particle;
  }
  
  public ArrayList<DwParticle> findParticlesWithinRadius(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    DwParticle2D[] particles = physics.getParticles();
    ArrayList<DwParticle> list = new ArrayList<DwParticle>();
    for(int i = 0; i < particles.length; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq =  dx*dx + dy*dy;
      if(dd_sq < dd_min_sq){
        list.add(particles[i]);
      }
    }
    return list;
  }
  
  
  public void updateMouseInteractions(){
    // deleting springs/constraints between particles
    if(DELETE_SPRINGS){
      ArrayList<DwParticle> list = findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
      for(DwParticle tmp : list){
        tmp.enableAllSprings(false);
        tmp.collision_group = physics.getNewCollisionGroupId();
        tmp.rad_collision = tmp.rad;
      }
    } else {
      if(particle_mouse != null){
        float[] mouse = {mouseX, mouseY};
        particle_mouse.moveTo(mouse, 0.2f);
      }
    }
  }
  
  
  boolean DELETE_SPRINGS = false;
  float   DELETE_RADIUS  = 10;

  public void mousePressed(){
    if(mouseButton == RIGHT ) DELETE_SPRINGS = true;
    
    if(!DELETE_SPRINGS){
      particle_mouse = findNearestParticle(mouseX, mouseY, 100);
      if(particle_mouse != null) particle_mouse.enable(false, false, false);
    }
  }
  
  public void mouseReleased(){
    if(particle_mouse != null && !DELETE_SPRINGS){
      if(mouseButton == LEFT  ) particle_mouse.enable(true, true,  true );
      if(mouseButton == CENTER) particle_mouse.enable(true, false, false);
      particle_mouse = null;
    }
    if(mouseButton == RIGHT ) DELETE_SPRINGS = false;
  }
  
  public void keyReleased(){
    if(key == 's') repairAllSprings();
    if(key == 'r') initBodies();
    if(key == 'm') applySpringMemoryEffect();
    if(key == '1') DISPLAY_MODE = 0;
    if(key == '2') DISPLAY_MODE = 1;
    if(key == 'p') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
  }
  

  
  public static void main(String args[]) {
    PApplet.main(new String[] { SoftBody2D_Playground.class.getName() });
  }
}