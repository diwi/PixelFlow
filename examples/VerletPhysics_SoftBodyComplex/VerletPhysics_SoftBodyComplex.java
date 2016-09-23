/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package VerletPhysics_SoftBodyComplex;




import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics2D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies2D.SoftCircle;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies2D.SoftBody2D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies2D.SoftGrid;

import processing.core.*;

public class VerletPhysics_SoftBodyComplex extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  // physics simulation
  VerletPhysics2D physics;
  
  // list, that wills store the cloths
  ArrayList<SoftBody2D> softbodies;

  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 1;
  
  // entities to display
  boolean DISPLAY_PARTICLES      = true;
  boolean DISPLAY_SPRINGS_STRUCT = true;
  boolean DISPLAY_SPRINGS_SHEAR  = true;
  boolean DISPLAY_SPRINGS_BEND   = true;
  
  // just for the window title-info
  int NUM_SPRINGS;
  int NUM_PARTICLES;
  
  // first thing to do, inside draw()
  boolean NEED_REBUILD = true;
  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  


  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
//    context.printGL();
    
    physics = new VerletPhysics2D();

    // global physics parameters
    physics.param.GRAVITY = new float[]{ 0, 0.2f };
    physics.param.bounds  = new float[]{ 0, 0, width, height };
    physics.param.iterations_collisions = 4;
    physics.param.iterations_springs    = 8;
    
    frameRate(60);
  }
  

  public void initBodies(){
   
    physics.reset();
    
    softbodies = new ArrayList<SoftBody2D>();
    
  
    // particle parameters: same behavior for all
    VerletParticle2D.Param param_particle = new VerletParticle2D.Param();
    
    // spring parameters: different spring behavior for different bodies
    SpringConstraint2D.Param param_spring_cloth    = new SpringConstraint2D.Param();
    SpringConstraint2D.Param param_spring_softbody = new SpringConstraint2D.Param();
    SpringConstraint2D.Param param_spring_chain    = new SpringConstraint2D.Param();
    SpringConstraint2D.Param param_spring_circle   = new SpringConstraint2D.Param();
    
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

    SoftGrid lattice_girder = new SoftGrid();
    SoftGrid chain          = new SoftGrid();
    SoftCircle circle         = new SoftCircle();
    SoftGrid box            = new SoftGrid();
    
    // lattice girder
    {
      nodex_x = 15;
      nodes_y = 2;
      nodes_r = 20;
      nodes_start_x = 200;
      nodes_start_y = 100;
      SoftGrid body = lattice_girder;
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
      body.createParticlesShape(this);
      softbodies.add(body);
    }
    

    // chain
    { 
      nodes_start_x = nodes_start_x + (nodex_x-1)*nodes_r*2;
      nodes_start_y = nodes_start_y + (nodes_y-1)*nodes_r*2 +  50;
      nodex_x = 1;
      nodes_y = 10;
      nodes_r = 10;
      SoftGrid body = chain;
      body.CREATE_BEND_SPRINGS    = false;
      body.CREATE_SHEAR_SPRINGS   = false;
      body.self_collisions        = true; 
      body.collision_radius_scale = 1.00f; 
      body.setParam(param_particle);
      body.setParam(param_spring_chain);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.setParticleColor(color(0,128));
      body.createParticlesShape(this);
      softbodies.add(body);
    }
    
    // spring-constraint: lattice <-> chain
    {
      VerletParticle2D pa = lattice_girder.getNode(14, 1);
      VerletParticle2D pb = chain         .getNode( 0, 0);
      float rest_len = lattice_girder.nodes_r + chain.nodes_r;
      SpringConstraint2D.addSpring(pa, pb, rest_len*rest_len, param_spring_chain, SpringConstraint2D.TYPE.STRUCT);
    }

    
    // circle
    {
      nodes_r = 10;
      nodes_start_y = height-150;
      SoftCircle body = circle;
      body.CREATE_BEND_SPRINGS  = false;
      body.CREATE_SHEAR_SPRINGS = false;
      body.bend_spring_mode = 3;
      body.setParam(param_particle);
      body.setParam(param_spring_circle);
      body.create(physics, nodes_start_x, nodes_start_y, 60, nodes_r);
      body.setParticleColor(color(0,160));
      body.createParticlesShape(this);
      softbodies.add(body);
    }
    

    // spring-constraint: circle <-> chain
    {
      VerletParticle2D pa = circle.getNode(0);
      VerletParticle2D pb = chain .getNode(0, 9);
      float rest_len = chain.nodes_r * 5;
      SpringConstraint2D.addSpring(pa, pb, rest_len*rest_len, param_spring_chain, SpringConstraint2D.TYPE.STRUCT);
    }
    
    
    //box
    {
      nodex_x = 10;
      nodes_y = 10;
      nodes_r = 8;
      nodes_start_x = 100;
      nodes_start_y = height-450;
      SoftGrid body = box;
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      body.bend_spring_dist     = 2;
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.setParticleColor(color(0,128));
      body.createParticlesShape(this);
      softbodies.add(body);
    }

    
    NUM_SPRINGS   = SpringConstraint2D.getSpringCount(physics.getParticles(), true);
    NUM_PARTICLES = physics.getParticlesCount();
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
      for(SoftBody2D body : softbodies){
        body.use_particles_color = (DISPLAY_MODE == 0);
        body.drawParticles(this.g);
      }
    }
    
    // 2) springs
    for(SoftBody2D body : softbodies){
      if(DISPLAY_SPRINGS_BEND  ) body.drawSprings(this.g, SpringConstraint2D.TYPE.BEND  , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_SHEAR ) body.drawSprings(this.g, SpringConstraint2D.TYPE.SHEAR , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_STRUCT) body.drawSprings(this.g, SpringConstraint2D.TYPE.STRUCT, DISPLAY_MODE);
    }

    // interaction stuff
    if(DELETE_SPRINGS){
      fill(255,64);
      stroke(0);
      strokeWeight(1);
      ellipse(mouseX, mouseY, DELETE_RADIUS*2, DELETE_RADIUS*2);
    }

    // stats, to the title window
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  
  
  
  // this resets all springs and particles, to some of its initial states
  // can be used after deactivating springs with the mouse
  public void repairAllSprings(){
    SpringConstraint2D.makeAllSpringsUnidirectional(physics.getParticles());
    for(SoftBody2D body : softbodies){
      for(VerletParticle2D pa : body.particles){
        pa.setCollisionGroup(body.collision_group_id);
        pa.setRadiusCollision(pa.rad());
      }
    }
  }
  
  
  // update all springs rest-lengths, based on current particle position
  // the effect is, that the body keeps the current shape
  public void applySpringMemoryEffect(){
    for(SoftBody2D body : softbodies){
      for(VerletParticle2D pa : body.particles){
        for(int i = 0; i < pa.spring_count; i++){
          pa.springs[i].updateRestlength();
        }
      }
    }
  }
  
  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 
  VerletParticle2D particle_mouse = null;
  
  public VerletParticle2D findNearestParticle(float mx, float my){
    return findNearestParticle(mx, my, Float.MAX_VALUE);
  }
  
  public VerletParticle2D findNearestParticle(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    VerletParticle2D[] particles = physics.getParticles();
    VerletParticle2D particle = null;
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
  
  public ArrayList<VerletParticle2D> findParticlesWithinRadius(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    VerletParticle2D[] particles = physics.getParticles();
    ArrayList<VerletParticle2D> list = new ArrayList<VerletParticle2D>();
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
      ArrayList<VerletParticle2D> list = findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
      for(VerletParticle2D tmp : list){
        SpringConstraint2D.deactivateSprings(tmp);
        tmp.collision_group = physics.getNewCollisionGroupId();
        tmp.rad_collision = tmp.rad;
      }
    } else {
      if(particle_mouse != null) particle_mouse.moveTo(mouseX, mouseY, 0.2f);
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
    PApplet.main(new String[] { VerletPhysics_SoftBodyComplex.class.getName() });
  }
}