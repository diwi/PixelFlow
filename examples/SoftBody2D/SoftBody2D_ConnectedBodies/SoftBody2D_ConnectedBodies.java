/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody2D.SoftBody2D_ConnectedBodies;




import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBall2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftGrid2D;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import processing.core.*;

public class SoftBody2D_ConnectedBodies extends PApplet {
  
  //
  // This examples creates a couple of different softbody objects and connects
  // some of their vertices with spring constraints.
  // The result are compound objects. 
  // Depending on how these connections are setup, very complex bodies can be
  // created, e.g. for simulating material/structure combinations etc...
  //
  // The Color of the springs show the stress.
  // 
  // + Collision Detection
  //
  // Controls:
  // LMB: drag particles
  // MMB: drag + fix particles to a location
  // RMB: disable springs, to deform objects
  //
  // + GUI
  //
  
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  
  // physics parameters
  DwPhysics.Param param_physics = new DwPhysics.Param();
  
  // physics simulation
  DwPhysics<DwParticle2D> physics;
  
  // list, that wills store the cloths
  ArrayList<DwSoftBody2D> softbodies;

  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 1;
  
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
    param_physics.iterations_springs    = 8;
    
    frameRate(60);
  }
  

  public void initBodies(){
   
    physics.reset();
    
    softbodies = new ArrayList<DwSoftBody2D>();
    
  
    // particle parameters: same behavior for all
    DwParticle.Param param_particle = new DwParticle.Param();
    
    // spring parameters: different spring behavior for different bodies
    DwSpringConstraint.Param param_spring_cloth    = new DwSpringConstraint.Param();
    DwSpringConstraint.Param param_spring_softbody = new DwSpringConstraint.Param();
    DwSpringConstraint.Param param_spring_chain    = new DwSpringConstraint.Param();
    DwSpringConstraint.Param param_spring_circle   = new DwSpringConstraint.Param();
    
    // particle parameters
    param_particle.DAMP_BOUNDS     = 0.40f;
    param_particle.DAMP_COLLISION  = 0.9990f;
    param_particle.DAMP_VELOCITY   = 0.991f; 
    
    // spring parameters
    param_spring_cloth   .damp_dec = 0.999999f;
    param_spring_cloth   .damp_inc = 0.000599f;
    
    param_spring_softbody.damp_dec = 0.999999f;
    param_spring_softbody.damp_inc = 0.999999f;
    
    param_spring_chain   .damp_dec = 0.699999f;
    param_spring_chain   .damp_inc = 0.00099999f;
    
    param_spring_circle  .damp_dec = 0.999999f;
    param_spring_circle  .damp_inc = 0.999999f;
    
    
    
    // create some particle-bodies: Cloth / SoftBody
    int nodex_x, nodes_y, nodes_r;
    float nodes_start_x, nodes_start_y;

    DwSoftGrid2D lattice_girder = new DwSoftGrid2D();
    DwSoftGrid2D chain          = new DwSoftGrid2D();
    DwSoftBall2D circle         = new DwSoftBall2D();
    DwSoftGrid2D box            = new DwSoftGrid2D();
    
    // lattice girder
    {
      nodex_x = 15;
      nodes_y = 2;
      nodes_r = 20;
      nodes_start_x = 200;
      nodes_start_y = 100;
      DwSoftGrid2D body = lattice_girder;
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      body.bend_spring_dist     = nodex_x;
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(0, 0).enable(false, false, false); // fix node to current location
      body.getNode(0, 1).enable(false, false, false); // fix node to current location
      body.setParticleColor(color(0,128));
      body.createShapeParticles(this);
      softbodies.add(body);
    }
    

    // chain
    { 
      nodes_start_x = nodes_start_x + (nodex_x-1)*nodes_r*2;
      nodes_start_y = nodes_start_y + (nodes_y-1)*nodes_r*2 +  50;
      nodex_x = 1;
      nodes_y = 10;
      nodes_r = 10;
      DwSoftGrid2D body = chain;
      body.CREATE_BEND_SPRINGS    = false;
      body.CREATE_SHEAR_SPRINGS   = false;
      body.self_collisions        = true; 
      body.collision_radius_scale = 1.00f; 
      body.setParam(param_particle);
      body.setParam(param_spring_chain);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.setParticleColor(color(0,128));
      body.createShapeParticles(this);
      softbodies.add(body);
    }
    
    // spring-constraint: lattice <-> chain
    {
      DwParticle2D pa = lattice_girder.getNode(14, 1);
      DwParticle2D pb = chain         .getNode( 0, 0);
      float rest_len = lattice_girder.nodes_r + chain.nodes_r;
      DwSpringConstraint2D.addSpring(physics, pa, pb, rest_len, param_spring_chain);
    }

    
    // circle
    {
      nodes_r = 10;
      nodes_start_y = height-150;
      DwSoftBall2D body = circle;
      body.CREATE_BEND_SPRINGS  = false;
      body.CREATE_SHEAR_SPRINGS = false;
      body.bend_spring_mode = 1;
      body.bend_spring_dist = 5;
      body.setParam(param_particle);
      body.setParam(param_spring_circle);
      body.create(physics, nodes_start_x, nodes_start_y, 70, nodes_r);
      body.setParticleColor(color(0,160));
      body.createShapeParticles(this);
      softbodies.add(body);
    }
    

    // spring-constraint: circle <-> chain
    {
      DwParticle2D pa = circle.getNode(0);
      DwParticle2D pb = chain .getNode(0, 9);
      float rest_len = chain.nodes_r * 5;
      DwSpringConstraint2D.addSpring(physics, pa, pb, rest_len, param_spring_chain);
    }
    
    
    //box
    {
      nodex_x = 10;
      nodes_y = 10;
      nodes_r = 8;
      nodes_start_x = 100;
      nodes_start_y = height-450;
      DwSoftGrid2D body = box;
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      body.bend_spring_dist     = 2;
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.setParticleColor(color(0,128));
      body.createShapeParticles(this);
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
        body.displayParticles(this.g);
      }
    }
    
    
    for(DwSoftBody2D body : softbodies){
      body.shade_springs_by_tension = (DISPLAY_MODE == 1);
      body.displaySprings(this.g, new DwStrokeStyle(color(255,  90,  30), 0.3f), DwSpringConstraint.TYPE.BEND);
      body.displaySprings(this.g, new DwStrokeStyle(color( 70, 140, 255), 0.6f), DwSpringConstraint.TYPE.SHEAR);
      body.displaySprings(this.g, new DwStrokeStyle(color(  0,   0,   0), 1.0f), DwSpringConstraint.TYPE.STRUCT);
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
  float   DELETE_RADIUS  = 20;

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
    PApplet.main(new String[] { SoftBody2D_ConnectedBodies.class.getName() });
  }
}