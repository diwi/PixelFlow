package VerletPhysics_Softbodies;




import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics2D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies2D.SoftBall;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies2D.SoftBody2D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies2D.SoftGrid;

import processing.core.*;

public class VerletPhysics_Softbodies extends PApplet {

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
  
  
  // particle behavior, different presets for different bodies
  VerletParticle2D.Param param_cloth    = new VerletParticle2D.Param();
  VerletParticle2D.Param param_softbody = new VerletParticle2D.Param();
  VerletParticle2D.Param param_chain    = new VerletParticle2D.Param();
  VerletParticle2D.Param param_circle   = new VerletParticle2D.Param();

  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 0;
  
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

    physics.param.GRAVITY = new float[]{ 0, 0.1f };
    physics.param.bounds  = new float[]{ 0, 0, width, height };
    physics.param.iterations_collisions = 4;
    physics.param.iterations_springs    = 4;
    
    // Cloth Parameters
    // Spring contraction is almost 100%, while expansion is very low
    param_cloth.DAMP_BOUNDS          = 0.40f;
    param_cloth.DAMP_COLLISION       = 0.9990f;
    param_cloth.DAMP_VELOCITY        = 0.991f; 
    param_cloth.DAMP_SPRING_decrease = 0.999999f;     // contraction (... to restlength)
    param_cloth.DAMP_SPRING_increase = 0.0005999999f; // expansion   (... to restlength)

    // grid, almost rigid
    param_softbody.DAMP_BOUNDS          = 0.40f;
    param_softbody.DAMP_COLLISION       = 0.9990f;
    param_softbody.DAMP_VELOCITY        = 0.999999f;
    param_softbody.DAMP_SPRING_decrease = 0.9999999f;
    param_softbody.DAMP_SPRING_increase = 0.9999999f;
    
    // chain, not so rigid
    param_chain.DAMP_BOUNDS          = 0.40f;
    param_chain.DAMP_COLLISION       = 0.9990f;
    param_chain.DAMP_VELOCITY        = 0.999999f;
    param_chain.DAMP_SPRING_decrease = 0.5999999f;
    param_chain.DAMP_SPRING_increase = 0.5999999f;
    
    // circle, almost rigid
    param_circle.DAMP_BOUNDS          = 0.40f;
    param_circle.DAMP_COLLISION       = 0.9990f;
    param_circle.DAMP_VELOCITY        = 0.999999f;
    param_circle.DAMP_SPRING_decrease = 0.9999999f;
    param_circle.DAMP_SPRING_increase = 0.9999999f;
 

    frameRate(600);
  }
  
  
  
  public void initBodies(){
    
    physics.reset();
    
    softbodies = new ArrayList<SoftBody2D>();
    
    
    // create some particle-bodies: Cloth / SoftBody
    int nodex_x, nodes_y, nodes_r;
    float nodes_start_x, nodes_start_y;

    // cloth
    {
      nodex_x = 40;
      nodes_y = 40;
      nodes_r = 5;
      nodes_start_x = 50;
      nodes_start_y = 70;
      SoftGrid body = new SoftGrid();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 2;
      body.create(physics, param_cloth, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(             0, 0).enable(false, false, false); // fix node to current location
      body.getNode(body.nodes_x-1, 0).enable(false, false, false); // fix node to current location
      body.createShape(this, color(255,180,0,160));
      softbodies.add(body);
    }
    
    // grid
    {
      nodex_x = 15;
      nodes_y = 25;
      nodes_r = 5;
      nodes_start_x = width/2;
      nodes_start_y = height/2;
      SoftGrid body = new SoftGrid();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 2;
      body.create(physics, param_softbody, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.createShape(this, color(0,128));
      softbodies.add(body);
    }
    
    // grid
    {
      nodex_x = 10;
      nodes_y = 30;
      nodes_r = 5;
      nodes_start_x = 500;
      nodes_start_y = 300;
      SoftGrid body = new SoftGrid();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      body.create(physics, param_softbody, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(0, 0).enable(false, false, false); // fix node to current location
      body.createShape(this, color(0,180,255,160));
      softbodies.add(body);
    }
    
    // lattice girder
    {
      nodex_x = 15;
      nodes_y = 2;
      nodes_r = 20;
      nodes_start_x = 500;
      nodes_start_y = 100;
      SoftGrid body = new SoftGrid();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      body.create(physics, param_softbody, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(0, 0).enable(false, false, false); // fix node to current location
      body.getNode(0, 1).enable(false, false, false); // fix node to current location
      body.createShape(this, color(0,128));
      softbodies.add(body);
    }
    

    // chain
    {
      nodex_x = 70;
      nodes_y = 1;
      nodes_r = 10;
      nodes_start_x = 500;
      nodes_start_y = 200;
      SoftGrid body = new SoftGrid();
      body.CREATE_BEND_SPRINGS  = false;
      body.CREATE_SHEAR_SPRINGS = false;
      body.create(physics, param_chain, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(0, 0).enable(false, false, false); // fix node to current location
      body.createShape(this, color(0,128));
      body.getNode(35, 0).enable(false, false, false);
      softbodies.add(body);
    }
    
    // circle
    {
      nodes_r = 10;
      nodes_start_x = 300;
      nodes_start_y = height-150;
      SoftBall body = new SoftBall();
      body.CREATE_BEND_SPRINGS  = false;
      body.CREATE_SHEAR_SPRINGS = false;
      body.bend_spring_mode = 3;
      body.create(physics, param_circle, nodes_start_x, nodes_start_y, 70, nodes_r);
      body.createShape(this, color(0,160));
      softbodies.add(body);
    }
    

    
//    SpringConstraint.makeAllSpringsUnidirectional(physics.getParticles()); // default anyways
//    SpringConstraint.makeAllSpringsBidirectional (physics.getParticles());
//    int num_of_alll_springs = SpringConstraint.getSpringCount(physics.getParticles(), false);
//    int num_of_good_springs = SpringConstraint.getSpringCount(physics.getParticles(), true);
//    System.out.println("springs1: "+ num_of_good_springs);
//    System.out.println("springs2: "+ num_of_alll_springs);
//    System.out.println("number of particles = "+physics.getParticlesCount());
//    System.out.println("springs/particles = "+num_of_good_springs / (float)physics.getParticlesCount());
    
    
    NUM_SPRINGS   = SpringConstraint.getSpringCount(physics.getParticles(), true);
    NUM_PARTICLES = physics.getParticlesCount();
  }


  
  
  public void draw() {


    if(NEED_REBUILD){
      initBodies();
      NEED_REBUILD = false;
    }
    
    // Mouse Interaction: particles position
    if(!DELETE_SPRINGS && particle_mouse != null){
      VerletParticle2D particle = particle_mouse;
      float dx = mouseX - particle.cx;
      float dy = mouseY - particle.cy;
      
      float damping_pos = 0.2f;
      particle.px = particle.cx;
      particle.py = particle.cy;
      particle.cx  += dx * damping_pos;
      particle.cy  += dy * damping_pos;
    }
    
    // Mouse Interaction: deleting springs/constraints between particles
    if(DELETE_SPRINGS && mousePressed){
      ArrayList<VerletParticle2D> list = findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
      for(VerletParticle2D tmp : list){
        SpringConstraint.deactivateSprings(tmp);
        tmp.collision_group = physics.getNewCollisionGroupId();
        tmp.rad_collision = tmp.rad;
      }
    }

    
    
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
      if(DISPLAY_SPRINGS_BEND  ) body.drawSprings(this.g, SpringConstraint.TYPE.BEND  , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_SHEAR ) body.drawSprings(this.g, SpringConstraint.TYPE.SHEAR , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_STRUCT) body.drawSprings(this.g, SpringConstraint.TYPE.STRUCT, DISPLAY_MODE);
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
    SpringConstraint.makeAllSpringsUnidirectional(physics.getParticles());
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
  
  boolean DELETE_SPRINGS = false;
  float   DELETE_RADIUS = 10;

  public void mousePressed(){
    if(mouseButton == RIGHT ) DELETE_SPRINGS = true;
    
    if(!DELETE_SPRINGS){
      particle_mouse = findNearestParticle(mouseX, mouseY, 100);
      if(particle_mouse != null){
        if(mouseButton == LEFT  ) particle_mouse.enable(false, false, false);
        if(mouseButton == CENTER) particle_mouse.enable(false, false, false);
      }
    }
  }
  
  public void mouseReleased(){
    if(particle_mouse != null && !DELETE_SPRINGS){
      if(mouseButton == LEFT  ) particle_mouse.enable(true, true, true);
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
    PApplet.main(new String[] { VerletPhysics_Softbodies.class.getName() });
  }
}