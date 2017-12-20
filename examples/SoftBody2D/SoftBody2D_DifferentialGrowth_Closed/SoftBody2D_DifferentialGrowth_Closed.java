/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody2D.SoftBody2D_DifferentialGrowth_Closed;




import java.util.ArrayList;
import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwLiquidFX;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import processing.core.*;
import processing.opengl.PGraphics2D;

public class SoftBody2D_DifferentialGrowth_Closed extends PApplet {

  
  //
  // Differential Line Growth Example
  //
  // Controls:
  // LMB: drag particles
  // MMB: drag + fix particles to a location
  // RMB: disable springs, to split the chain
  //
  // key 'r': reset
  // key 'p': toggle particle display
  // key 'l': toggle liquidfx
  //
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  
  DwPixelFlow context;
  
  // parameters
  // particle behavior, different presets for different bodies
  DwPhysics.Param          param_physics      = new DwPhysics.Param();
  DwParticle.Param         param_chain        = new DwParticle.Param();
  DwSpringConstraint.Param param_spring_chain = new DwSpringConstraint.Param();
  

  // physics simulation
  DwPhysics<DwParticle2D> physics;
 
  // array for our particles
  int particles_count = 0;
  DwParticle2D[] particles = new DwParticle2D[particles_count];
  
  int particles_sorted_count = 0;
  DwParticle2D[] particles_sorted = new DwParticle2D[particles_count];
  

  // post-processing effect
  DwLiquidFX liquidfx;
  
  // render canvas
  PGraphics2D pg_particles;
  
  
  // some global settings
  float radius;
  float restlen_scale;
  
  
  // some state variables
  boolean DISPLAY_PARTICLES = true;
  boolean APPLY_LIQUIDFX = true;
  int BACKGROUND_COLOR = 16;
  
  
  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(0);
  }
  

  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    
    
    param_physics.GRAVITY = new float[]{ 0, 0.0f };
    param_physics.bounds  = new float[]{ 0, 0, width, height };
    param_physics.iterations_collisions = 2;
    param_physics.iterations_springs    = 2;
    
    // parameters for chain-particles
    param_chain.DAMP_BOUNDS    = 0.50f;
    param_chain.DAMP_COLLISION = 0.199999f;
    param_chain.DAMP_VELOCITY  = 0.7999999f; 

    param_spring_chain.damp_dec = 0.499999f;
    param_spring_chain.damp_inc = 0.499999f;
    
    // particle radius
    radius = 10;
    // spring length scale
    restlen_scale = 0.2f;
    
    

    // physics simulation object
    physics = new DwPhysics<DwParticle2D>(param_physics);
    
    liquidfx = new DwLiquidFX(context);

    pg_particles = (PGraphics2D) createGraphics(width, height, P2D);
    pg_particles.smooth(0);
    
    reset();
    
    frameRate(60);
  }
  

  
  
  public void reset(){
    physics.reset();
    particles_count = 0;
    particles = new DwParticle2D[particles_count];
    particles_sorted_count = 0;
    particles_sorted = new DwParticle2D[particles_count];
    
//    initVersion_01();
    initVersion_02();
    
    sortParticles();
  }
  
  
  
  
  public void initVersion_01(){
    float x,y;
 
    x = 250;
    y = height/2;
    DwParticle2D pa = createParticle(x, y, radius);
    
    x = width-250;
    y = height/2;
    DwParticle2D pb = createParticle(x, y, radius, pa, null);
    
    pa.enable(false, false, false);
    pb.enable(false, false, false);
  }
  
  
  public void initVersion_02(){
    
    float spawn_radius = 20;
    int count = 200;
    for(int i = 0; i < count; i++){
      float a = TWO_PI * i / (float)count;
      float px = width /2 + sin(a) * spawn_radius;
      float py = height/2 + cos(a) * spawn_radius;
      createParticle(px, py, radius);
    }
    
    for(int i = 0; i < count; i++){
      int ia = (i + 0) % count;
      int ib = (i + 1) % count;

      DwParticle2D pa = particles[ia];
      DwParticle2D pb = particles[ib];
      DwSpringConstraint2D.addSpring(physics, pa, pb, radius * restlen_scale, param_spring_chain);
    }
    
  }
  

  
  
  public DwParticle2D createParticle(float spawn_x, float spawn_y, float radius){
    return createParticle(spawn_x, spawn_y, radius, null, null);
  }

  public DwParticle2D createParticle(float spawn_x, float spawn_y, float radius, DwParticle2D p_prev, DwParticle2D p_next){

    int idx_curr = particles_count;
    DwParticle2D pa = new DwParticle2D(idx_curr);
    pa.setMass(1);
    pa.setParamByRef(param_chain);
    pa.setPosition(spawn_x, spawn_y);
    pa.setRadius(radius);
    pa.setRadiusCollision(radius);
    pa.setCollisionGroup(idx_curr);
    addParticleToList(pa);
    
    float restlen = radius * restlen_scale;
    if(p_prev != null) DwSpringConstraint2D.addSpring(physics, pa, p_prev, restlen, param_spring_chain);
    if(p_next != null) DwSpringConstraint2D.addSpring(physics, pa, p_next, restlen, param_spring_chain);
     
    return pa;
  }
  

  // kind of the same what an ArrayList<DwParticle2D> would do.
  public void addParticleToList(DwParticle2D particle){
    if(particles_count >= particles.length){
      int new_len = (int) Math.max(2, Math.ceil(particles_count*1.5f) );
      if(particles == null){
        particles = new DwParticle2D[new_len];
        particles_sorted = new DwParticle2D[new_len];
      } else {
        particles = Arrays.copyOf(particles, new_len);
        particles_sorted = Arrays.copyOf(particles_sorted, new_len);
      }

    }
    particles[particles_count++] = particle;
    physics.setParticles(particles, particles_count);
  }
  
  
  
  
  public void lineGrowth(){
    int num_new_particles = min(5, ceil(particles_sorted_count / 100f));

    for(int i = 0; i < num_new_particles; i++){
      
      // pick a random particle
      int idx = (int) (random(1) * particles_sorted_count);
      
      // get spring and two attached particles
      DwSpringConstraint2D spring = (DwSpringConstraint2D) particles_sorted[idx].springs[1];
      if(spring == null){
        spring = (DwSpringConstraint2D) particles_sorted[idx].springs[0];
      }
      DwParticle2D pa = spring.pa;
      DwParticle2D pb = spring.pb;
      
      // get rid of old spring
      spring.removeSpring(physics);
      float jitter = radius * 0.01f;
      
      // spawn new particle somewhere in the middle
      float px = (pa.cx + pb.cx) * 0.5f + random(-1, 1) * jitter;
      float py = (pa.cy + pb.cy) * 0.5f + random(-1, 1) * jitter;
      
      // add new spring
      createParticle(px, py, radius, pa, pb);
    }
  }
  
  
  
  
  
  public void draw() {

    // insert new particles to grow the linked list
    lineGrowth();
    
    // creat sorted list of particles
    sortParticles();

    // user 
    updateMouseInteractions();
    
    // physics
    physics.update(1);
      
    // render
    pg_particles.beginDraw();
    pg_particles.blendMode(REPLACE);
    pg_particles.background(BACKGROUND_COLOR, 0);
    pg_particles.blendMode(BLEND);
    drawParticleLine(pg_particles, DISPLAY_PARTICLES, true);
    pg_particles.endDraw();
    
    
    // post processing
    if(APPLY_LIQUIDFX){
      liquidfx.param.base_LoD = 1;
      liquidfx.param.base_blur_radius = 2;
      liquidfx.param.highlight_enabled = true;
      liquidfx.param.highlight_LoD = 1;
      liquidfx.param.sss_enabled = false;
      liquidfx.apply(pg_particles);
    }
  
    // display
    background(BACKGROUND_COLOR);
    blendMode(BLEND);
    image(pg_particles, 0, 0);
    
    // info
    int NUM_SPRINGS   = physics.getSpringCount();
    int NUM_PARTICLES = physics.getParticlesCount();
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  
  // get next particle in the chain
  public DwParticle2D getNextParticle(DwParticle2D pcurr, DwParticle2D pprev){
    
    int icurr = pcurr.idx;
    int iprev = (pprev != null) ? pprev.idx : -1;
    
    if(pcurr.spring_count >= 1){
      int i0 = pcurr.springs[0].idxPa();
      int i1 = pcurr.springs[0].idxPb();
      if(i0 != icurr && i0 != iprev) return particles[i0];
      if(i1 != icurr && i1 != iprev) return particles[i1];
    }
    
    if(pcurr.spring_count >= 2){
      int i0 = pcurr.springs[1].idxPa();
      int i1 = pcurr.springs[1].idxPb();
      if(i0 != icurr && i0 != iprev) return particles[i0];
      if(i1 != icurr && i1 != iprev) return particles[i1];
    }

    return null;
  }
  
  
  // save particles in a sorted list, as they are lined up in the chain
  public void sortParticles(){
    particles_sorted_count = 0;
    
    DwParticle2D pstart = particles[0];
    DwParticle2D pprev = null;
    DwParticle2D pcurr = pstart;
    DwParticle2D pnext = null;
    
    while(pcurr != null && pnext != pstart){
      particles_sorted[particles_sorted_count++] = pcurr;
      pnext = getNextParticle(pcurr, pprev);
      pprev = pcurr;
      pcurr = pnext;
    }
  }
  
  
  // draw particles/lines
  public void drawParticleLine(PGraphics2D pg, boolean display_particles, boolean display_line){

    DwParticle2D pprev;
    DwParticle2D pcurr;
    
    pg.colorMode(HSB, 1);

    if(display_line){
      pg.beginShape();
      pg.noStroke();
      pg.fill(0,0,0.9f);
      for(int i = 0; i < particles_sorted_count; i++){
        pcurr = particles_sorted[i]; 

//        float hue = i/(float)particles_sorted_count;
//        pg.fill(hue, 1, 1);
        pg.vertex(pcurr.cx, pcurr.cy);
      }
      pg.endShape();
    }
    
//    if(display_line){
//      pg.strokeWeight(1);
//      pg.fill(128);
//      for(int i = 1; i < particles_sorted_count; i++){
//        pprev = particles_sorted[i-1];
//        pcurr = particles_sorted[i]; 
//
//        float hue = i/(float)particles_sorted_count;
//        pg.stroke(hue, 1, 1);
//        pg.line(pprev.cx, pprev.cy, pcurr.cx, pcurr.cy);
//      }
//    }
    

    if(display_particles){
  
      pg.noStroke();
      for(int i = 0; i < particles_sorted_count; i++){
        pcurr = particles_sorted[i];
        float radius = max(pcurr.rad * 1.0f, 5);
        float hue = i/(float)particles_sorted_count;
        pg.fill(hue, 1.0f, 1);
        pg.ellipse(pcurr.cx, pcurr.cy, radius, radius);
      }
    }
    
    pg.colorMode(RGB, 255);
  }
  
  

  

  
  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 
  DwParticle2D particle_mouse = null;
  
  public DwParticle2D findNearestParticle(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    DwParticle2D particle = null;
    for(int i = 0; i < particles_count; i++){
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
    ArrayList<DwParticle> list = new ArrayList<DwParticle>();
    for(int i = 0; i < particles_count; i++){
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
    if(particle_mouse != null){
      float[] mouse = {mouseX, mouseY};
      particle_mouse.moveTo(mouse, 0.2f);
    }
  }
  

  public void mousePressed(){
    particle_mouse = findNearestParticle(mouseX, mouseY, 100);
    if(particle_mouse != null) particle_mouse.enable(false, false, false);
  }
  
  public void mouseReleased(){
    if(particle_mouse != null){
      if(mouseButton == LEFT  ) particle_mouse.enable(true, true,  true );
      if(mouseButton == CENTER) particle_mouse.enable(false, false, false);
      particle_mouse = null;
    }
  }
  
  public void keyReleased(){
    if(key == 'r') reset();
    if(key == 'p') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
    if(key == 'l') APPLY_LIQUIDFX = !APPLY_LIQUIDFX;
  }

  public static void main(String args[]) {
    PApplet.main(new String[] { SoftBody2D_DifferentialGrowth_Closed.class.getName() });
  }
}