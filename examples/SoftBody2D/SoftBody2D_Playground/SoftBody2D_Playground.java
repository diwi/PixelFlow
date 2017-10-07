/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package SoftBody2D.SoftBody2D_Playground;




import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBall2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftGrid2D;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;

public class SoftBody2D_Playground extends PApplet {
  
  //
  // 2D Softbody Sandbox, to debug/test/profile everything.
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
  // + GUI
  //
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = viewport_w-gui_w;
  int gui_y = 0;
  
  
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
  ArrayList<DwSoftBody2D> softbodies = new ArrayList<DwSoftBody2D>();
  

  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 0;
  
  // entities to display
  boolean DISPLAY_PARTICLES      = true;
  boolean DISPLAY_MESH           = !true;
  boolean DISPLAY_SRPINGS        = true;
  
  boolean DISPLAY_SPRINGS_STRUCT = true;
  boolean DISPLAY_SPRINGS_SHEAR  = true;
  boolean DISPLAY_SPRINGS_BEND   = true;
  
  boolean UPDATE_PHYSICS         = true;
  
  // first thing to do, inside draw()
  boolean NEED_REBUILD = false;
  
  
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

    
    createBodies();
    
    createGUI();
    
    frameRate(60);
  }
  
  
  
  public void createBodies(){
    physics.reset();
    
    softbodies.clear();
    
    
    // create some particle-bodies: Cloth / SoftBody
    float r,g,b,a,s;
    int nodex_x, nodes_y, nodes_r;
    float nodes_start_x, nodes_start_y;

    // cloth
    {
      nodex_x = 30;
      nodes_y = 30;
      nodes_r = 7;
      nodes_start_x = 50;
      nodes_start_y = 70;
      DwSoftGrid2D body = new DwSoftGrid2D();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 2;
      r = 255;
      g = 180;
      b = 0;
      a = 160;
      s = 1f;
      body.setMaterialColor(color(r  ,g  ,b  , a));
      body.setParticleColor(color(r*s,g*s,b*s, a));
      body.setParam(param_particle);
      body.setParam(param_spring_cloth);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(             0, 0).enable(false, false, false); // fix node to current location
      body.getNode(body.nodes_x-1, 0).enable(false, false, false); // fix node to current location
      body.createShapeParticles(this);
      softbodies.add(body);
    }
    
    // grid
    {
      nodex_x = 10;
      nodes_y = 20;
      nodes_r = 7;
      nodes_start_x = width/2;
      nodes_start_y = height/2;
      DwSoftGrid2D body = new DwSoftGrid2D();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 2;
      r = 0;
      g = 0;
      b = 0;
      a = 128;
      s = 1f;
      body.setMaterialColor(color(r  ,g  ,b  , a));
      body.setParticleColor(color(r*s,g*s,b*s, a));
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.createShapeParticles(this);
      softbodies.add(body);
    }
    
    // grid
    {
      nodex_x = 7;
      nodes_y = 22;
      nodes_r = 7;
      nodes_start_x = 500;
      nodes_start_y = 300;
      DwSoftGrid2D body = new DwSoftGrid2D();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      r = 0;
      g = 180;
      b = 255;
      a = 160;
      s = 1f;
      body.setMaterialColor(color(r  ,g  ,b  , a));
      body.setParticleColor(color(r*s,g*s,b*s, a));
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(0, 0).enable(false, false, false); // fix node to current location
      body.createShapeParticles(this);
      softbodies.add(body);
    }
    
    // lattice girder
    {
      nodex_x = 15;
      nodes_y = 2;
      nodes_r = 20;
      nodes_start_x = 500;
      nodes_start_y = 100;
      DwSoftGrid2D body = new DwSoftGrid2D();
      body.CREATE_SHEAR_SPRINGS = true;
      body.CREATE_BEND_SPRINGS  = true;
      body.bend_spring_mode     = 0;
      r = 0;
      g = 0;
      b = 0;
      a = 128;
      s = 1f;
      body.setMaterialColor(color(r  ,g  ,b  , a));
      body.setParticleColor(color(r*s,g*s,b*s, a));
      body.setParam(param_particle);
      body.setParam(param_spring_softbody);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode(0, 0).enable(false, false, false); // fix node to current location
      body.getNode(0, 1).enable(false, false, false); // fix node to current location
      body.createShapeParticles(this);
      softbodies.add(body);
    }
    

    // chain
    {
      nodex_x = 70;
      nodes_y = 1;
      nodes_r = 10;
      nodes_start_x = 500;
      nodes_start_y = 200;
      DwSoftGrid2D body = new DwSoftGrid2D();
      body.CREATE_BEND_SPRINGS  = false;
      body.CREATE_SHEAR_SPRINGS = false;
      body.self_collisions      = true; // particles of this body can collide among themselves
      body.collision_radius_scale = 1.00f; // funny, if bigger than 1 and self_collisions = true
      r = 0;
      g = 0;
      b = 0;
      a = 128;
      s = 1f;
      body.setMaterialColor(color(r  ,g  ,b  , a));
      body.setParticleColor(color(r*s,g*s,b*s, a));
      body.setParam(param_particle);
      body.setParam(param_spring_chain);
      body.create(physics, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      body.getNode( 0, 0).enable(false, false, false); // fix node to current location
      body.getNode(35, 0).enable(false, false, false);
      body.createShapeParticles(this);
      softbodies.add(body);
    }
    
    // circle
    {
      nodes_r = 10;
      nodes_start_x = 300;
      nodes_start_y = height-150;
      DwSoftBall2D body = new DwSoftBall2D();
      body.CREATE_BEND_SPRINGS  = false;
      body.CREATE_SHEAR_SPRINGS = false;
      body.bend_spring_mode = 0;
      body.bend_spring_dist = 8;
      r = 0;
      g = 0;
      b = 0;
      a = 160;
      s = 1f;
      body.setMaterialColor(color(r  ,g  ,b  , a));
      body.setParticleColor(color(r*s,g*s,b*s, a));
      body.setParam(param_particle);
      body.setParam(param_spring_circle);
      body.create(physics, nodes_start_x, nodes_start_y, 70, nodes_r);
      body.createShapeParticles(this);
      softbodies.add(body);
    }

  }


 
  
  public void draw() {

    if(NEED_REBUILD){
      createBodies();
      NEED_REBUILD = false;
    }
    
    updateMouseInteractions();

    // update physics simulation
    physics.update(1);
    
    // render
    background(DISPLAY_MODE == 0 ?  255 : 92);
    
    
    // 3) mesh, solid
    if(DISPLAY_MESH){
      for(DwSoftBody2D body : softbodies){
        body.createShapeMesh(this.g);
      }
    }
    
    
    // 1) particles
    if(DISPLAY_PARTICLES){
      for(DwSoftBody2D body : softbodies){
//        body.use_particles_color = (DISPLAY_MODE == 0);
        body.displayParticles(this.g);
      }
    }
    
    // 2) mesh, solid
    if(DISPLAY_MESH){
      for(DwSoftBody2D body : softbodies){
        body.displayMesh(this.g);
      }
    }
    
    
    if(DISPLAY_SRPINGS){
      for(DwSoftBody2D body : softbodies){
        body.shade_springs_by_tension = (DISPLAY_MODE == 1);
        body.displaySprings(this.g, new DwStrokeStyle(color(255,  90,  30), 0.3f), DwSpringConstraint.TYPE.BEND);
        body.displaySprings(this.g, new DwStrokeStyle(color( 70, 140, 255), 0.6f), DwSpringConstraint.TYPE.SHEAR);
        body.displaySprings(this.g, new DwStrokeStyle(color(  0,   0,   0), 1.0f), DwSpringConstraint.TYPE.STRUCT);
      }
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
    if(cp5.isMouseOver()) return;
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
    if(key == 'm') applySpringMemoryEffect();

    if(key == 'r') createBodies();
    if(key == '1') DISPLAY_MODE = 0;
    if(key == '2') DISPLAY_MODE = 1;
    
    if(key == '3') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
    if(key == '4') DISPLAY_MESH      = !DISPLAY_MESH;
    if(key == '5') DISPLAY_SRPINGS   = !DISPLAY_SRPINGS;

    if(key == ' ') UPDATE_PHYSICS = !UPDATE_PHYSICS;
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
  }
  
  public void setGravity(float val){
    physics.param.GRAVITY[1] = val;
  }
  
  public void togglePause(){
    UPDATE_PHYSICS = !UPDATE_PHYSICS;
  }
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    cp5.setAutoDraw(true);

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
          .addItem("SRPINGS"  , 2).activate(DISPLAY_SRPINGS   ? 2 : 5);
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
          .setRange(0.01f, 1).setValue(param_spring_cloth.damp_dec).plugTo(param_spring_cloth, "damp_dec");
      
      cp5.addSlider("Cloth.pressure").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.01f, 1).setValue(param_spring_cloth.damp_inc).plugTo(param_spring_cloth, "damp_inc");

      cp5.addSlider("Cube.tensile").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=(int)(oy*2))
          .setRange(0.01f, 1).setValue(param_spring_softbody.damp_dec).plugTo(param_spring_softbody, "damp_dec");
  
      cp5.addSlider("Cube.pressure").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.01f, 1).setValue(param_spring_softbody.damp_inc).plugTo(param_spring_softbody, "damp_inc");
  
      cp5.addSlider("Ball.tensile").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=(int)(oy*2))
          .setRange(0.01f, 1).setValue(param_spring_circle.damp_dec).plugTo(param_spring_circle, "damp_dec");

      cp5.addSlider("Ball.pressure").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.01f, 1).setValue(param_spring_circle.damp_inc).plugTo(param_spring_circle, "damp_inc");

    }
   
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_springs)
      .addItem(group_physics)
//      .open(0, 1)
      ;
   
  }
  
  
  
  
  
  
  
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { SoftBody2D_Playground.class.getName() });
  }
}