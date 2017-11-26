/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody2D.SoftBody2D_DifferentialGrowth;




import java.util.ArrayList;
import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import processing.core.*;

public class SoftBody2D_DifferentialGrowth2 extends PApplet {

  
  //
  // A simple Verlet Physics Example, that shows how to create a chain by 
  // creating particles and chaining them together with spring-constraints.
  // The springs-color shows the current stress, ... at a range from red to black.
  // 
  // + Collision Detection
  //
  // Controls:
  // LMB: drag particles
  // MMB: drag + fix particles to a location
  // RMB: disable springs, to split the chain
  //
  // key ' ': add particles at the current mouse location
  // key 'p': toggle particle display
  //
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  
  // parameters
  // particle behavior, different presets for different bodies
  DwPhysics.Param          param_physics      = new DwPhysics.Param();
  DwParticle.Param         param_chain        = new DwParticle.Param();
  DwSpringConstraint.Param param_spring_chain = new DwSpringConstraint.Param();
  
  // physics simulation
  DwPhysics<DwParticle2D> physics;
 
  // all we need is an array of particles
  int particles_count = 0;
  DwParticle2D[] particles = new DwParticle2D[particles_count];
  
  
  int particles_sorted_count = 0;
  DwParticle2D[] particles_sorted = new DwParticle2D[particles_count];
  ArrayList<DwParticle2D> particles_solo = new ArrayList<DwParticle2D>();
  
  boolean DISPLAY_PARTICLES = true;

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
    
    param_physics.GRAVITY = new float[]{ 0, 0.2f };
    param_physics.bounds  = new float[]{ 0, 0, width, height };
    param_physics.iterations_collisions = 2;
    param_physics.iterations_springs    = 2;
    
    // parameters for chain-particles
    param_chain.DAMP_BOUNDS          = 0.50f;
    param_chain.DAMP_COLLISION       = 0.199999f;
    param_chain.DAMP_VELOCITY        = 0.999999f; 

    param_spring_chain.damp_dec = 0.399999f;
    param_spring_chain.damp_inc = 0.399999f;
    
    // physics simulation object
    physics = new DwPhysics<DwParticle2D>(param_physics);
    
    reset();
    
    frameRate(60);
  }
  
  
  float radius = 8;
  float restlen_scale = 0.1f;
  
  
  public void reset(){
    physics.reset();
    particles_count = 0;
    particles = new DwParticle2D[particles_count];
    particles_sorted_count = 0;
    particles_sorted = new DwParticle2D[particles_count];
    particles_solo.clear();
    
    float x, y;
    float rad = radius;
    float restlen = radius * restlen_scale;
//    
//    x = width/2 - 10*radius/2;
//    y = height/2;
//    DwParticle2D pa = createParticle(x, y, rad, restlen, null, null);
//    
//    x = width/2 + 10*radius/2;
//    y = height/2;
//    DwParticle2D pb = createParticle(x, y, rad, restlen, pa, null);
//    
//    pa.enable(false, false, false);
//    pb.enable(false, false, false);
    
    
    float radius = 150;
    int count = 500;
    for(int i = 0; i < count; i++){
      float a = TWO_PI * i / (float)count;
      float px = width /2 + sin(a) * radius;
      float py = height/2 + cos(a) * radius;
      createParticle(px, py, rad, restlen, null, null);
    }
    
    
    for(int i = 0; i < count; i++){
      int ia = (i + 0) % count;
      int ib = (i + 1) % count;

      DwParticle2D pa = particles[ia];
      DwParticle2D pb = particles[ib];
      DwSpringConstraint2D.addSpring(physics, pa, pb, restlen, param_spring_chain);
    }
    
//    particles[0].enable(false, false, false);
    
    createSortedParticles();
  }
  
  


  public DwParticle2D createParticle(float spawn_x, float spawn_y, float radius, float restlen, DwParticle2D p_prev, DwParticle2D p_next){

    int idx_curr = particles_count;

    DwParticle2D pa = new DwParticle2D(idx_curr);
    pa.setMass(1);
    pa.setParamByRef(param_chain);
    pa.setPosition(spawn_x, spawn_y);
    pa.setRadius(radius);
    pa.setRadiusCollision(radius);
    pa.setCollisionGroup(idx_curr);
    addParticleToList(pa);

    if(p_prev != null) DwSpringConstraint2D.addSpring(physics, pa, p_prev, restlen, param_spring_chain);
    if(p_next != null) DwSpringConstraint2D.addSpring(physics, pa, p_next, restlen, param_spring_chain);
     
    return pa;
  }
  

  // kind of the same what an ArrayList<VerletParticle2D> would do.
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
  
  
  public void draw() {

    int count = min(5, ceil(particles_sorted_count / 100f));
  
    for(int i = 0; i < count; i++){
      float rand = random(1);
//      rand = rand * rand;
//      rand = rand * rand;
//      rand = rand * rand;
//      rand = rand * rand;
//      rand = rand * rand;
//      rand = sqrt(rand);
//      rand = sqrt(rand);
//      rand = sqrt(rand);
//      rand = sqrt(rand);

      int idx = (int) (rand * particles_sorted_count);
      
//      idx = 0;
      
      if(particles_sorted[idx] == null){
        System.out.println("idx : "+idx+", "+particles_sorted_count);
      }
      
      DwSpringConstraint2D spring = (DwSpringConstraint2D) particles_sorted[idx].springs[1];
      
//      DwSpringConstraint2D spring = (DwSpringConstraint2D) springs.get(idx);
      DwParticle2D pa = spring.pa;
      DwParticle2D pb = spring.pb;
      
      
//      float dx = pb.cx - pa.cx;
//      float dy = pb.cy - pa.cy;
//      float dd_sq = dx*dx + dy*dy;
//      float dd = (float) Math.sqrt(dd_sq);

      
      // get rid of old spring
      spring.removeSpring(physics);
      
      float range = 0.95f;
      float mix = random(1) * range + (1 - range) * 0.5f;


      float jitter = radius * restlen_scale;
      
      float px = pa.cx * mix + pb.cx * (1-mix) + random(-jitter, +jitter);
      float py = pa.cy * mix + pb.cy * (1-mix) + random(-jitter, +jitter);
      
      float rad = radius;
//      float restlen = (rad + max(pa.rad_collision, pa.rad_collision) ) * 0.1f;
      float restlen = radius * 0.1f;
//      restlen = dd * 0.4f;
      // add new spring
      createParticle(px, py, rad, restlen, pa, pb);
    }
    
    
//    if(frameCount % 5 == 0){
//      
//      float rand = random(1);
//      int idx = (int) (rand * particles_count);
//      DwParticle2D pcurr = particles[idx];
//      float px = pcurr.cx + random(-1,1) * 1;
//      float py = pcurr.cy + random(-1,1) * 1;
//   
//      DwParticle2D pa = createParticle(px, py, 20, 0, null, null);
//      particles_solo.add(pa);
//    }
    
 
    
    createSortedParticles();
    

    updateMouseInteractions();
    
    // update physics simulation
    physics.update(1);
      
    // render
    background(32);
    
//    noFill();
//    strokeWeight(1);
//    beginShape(LINES);
//    for(int i = 0; i < particles_count; i++){
//      DwParticle2D pa = particles[i];
//      for(int j = 0; j < pa.spring_count; j++){
//        DwSpringConstraint2D spring = (DwSpringConstraint2D) pa.springs[j];
//        if(spring.pa != pa) continue;
//        if(!spring.enabled) continue;
//        
//        DwParticle2D pb = spring.pb;
//        float force = Math.abs(spring.force);
//        float r = force*5000f;
//        float g = r/10;
//        float b = 0;
//        stroke(r,g,b);
//        vertex(pa.cx, pa.cy);
//        vertex(pb.cx, pb.cy);
//        
//      }
//    }
//    endShape();
    

    drawGrowth(DISPLAY_PARTICLES, true);
    
    noStroke();
    fill(0);
    for(DwParticle2D pa : particles_solo){
      ellipse(pa.cx, pa.cy, pa.rad*2, pa.rad*2);
    }
    
    
//    if(DISPLAY_PARTICLES){
//      noStroke();
//      fill(0);
//      for(int i = 0; i < particles_count; i++){
//        DwParticle2D particle = particles[i];
//        ellipse(particle.cx, particle.cy, particle.rad*2, particle.rad*2);
//      }
//    }
    
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
  
  
  
  public void createSortedParticles(){
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
  
  public void drawGrowth(boolean display_particles, boolean display_line){

    DwParticle2D pprev;
    DwParticle2D pcurr;
    
    noFill();
    strokeWeight(1);
//    strokeJoin(ROUND);
//    strokeJoin(MITER);
//    strokeCap(ROUND);
    stroke(0);
    
    colorMode(HSB, 1);
    
    if(display_line){
//      beginShape();
//      for(int i = 0; i < count; i++){
//        pcurr = particles_sorted[i];
//        stroke(i/(float)count, 1, 1);
//        vertex(pcurr.cx, pcurr.cy);
//      }
//      endShape();
      
      
//      beginShape(LINES);
//      for(int i = 1; i < count; i++){
//        pprev = particles_sorted[i-1];
//        pcurr = particles_sorted[i];
//        stroke(i/(float)count, 1, 1);
//        vertex(pprev.cx, pprev.cy);
//        vertex(pcurr.cx, pcurr.cy);
//      }
//      endShape();
      
      
      for(int i = 1; i < particles_sorted_count; i++){
        pprev = particles_sorted[i-1];
        pcurr = particles_sorted[i]; 

        stroke(i/(float)particles_sorted_count, 1, 1);
        
        line(pprev.cx, pprev.cy, pcurr.cx, pcurr.cy);
      }
      
    }


    
    if(display_particles){
      noStroke();
      for(int i = 0; i < particles_sorted_count; i++){
        pcurr = particles_sorted[i];
        fill(i/(float)particles_sorted_count, 1, 1);
        
        ellipse(pcurr.cx, pcurr.cy, pcurr.rad, pcurr.rad);
      }
    }
    
    
    
    
    
    
    colorMode(RGB, 255);
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
      if(mouseButton == CENTER) particle_mouse.enable(false, false, false);
      particle_mouse = null;
    }
    if(mouseButton == RIGHT ) DELETE_SPRINGS = false;
  }
  
  public void keyReleased(){
    if(key == 'r') reset();
    if(key == 'p') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
  }

  public static void main(String args[]) {
    PApplet.main(new String[] { SoftBody2D_DifferentialGrowth2.class.getName() });
  }
}