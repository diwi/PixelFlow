package VerletPhysics_Cloth;




import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletPhysics2D;

import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.verletPhysics2D.SpringConstraint;

import processing.core.*;

public class VerletPhysics_Cloth extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  VerletPhysics2D physics;
  
  VerletParticle2D[] particles = new VerletParticle2D[0];
  
  VerletParticle2D.Param param_cloth    = new VerletParticle2D.Param();
  VerletParticle2D.Param param_softbody = new VerletParticle2D.Param();
  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    

    physics = new VerletPhysics2D();

    physics.param.GRAVITY = new float[]{ 0, 0.1f };
    physics.param.bounds  = new float[]{ 0, 0, width, height };
    physics.param.iterations_collisions = 8;
    physics.param.iterations_springs    = 8;
    
    int idx = 0;

    // Cloth Parameters
    // Spring contraction is almost 100%, while expansion is very low
    param_cloth.DAMP_BOUNDS          = 0.90f;
    param_cloth.DAMP_COLLISION       = 0.90f;
    param_cloth.DAMP_VELOCITY        = 0.991f; 
    param_cloth.DAMP_SPRING_decrease = 0.999999f;    // ~ 100% contraction (... to restlength)
    param_cloth.DAMP_SPRING_increase = 0.00000999999f; // ~ 0% expansion (... to restlength)
    
    // SoftBody Parameters
    // Spring contraction AND expansion is almost 100%
    param_softbody.DAMP_BOUNDS          = 0.90f;
    param_softbody.DAMP_COLLISION       = 0.90f;
    param_softbody.DAMP_VELOCITY        = 0.999999f;
    param_softbody.DAMP_SPRING_decrease = 0.1999999f; // ~ 100% contraction (... to restlength)
    param_softbody.DAMP_SPRING_increase = 0.199999f; // ~ 100% expansion (... to restlength)
    
    
    // Cloth / SoftBody objects
    SoftBody cloth;

    
    int nodex_x, nodes_y, nodes_r;
    float nodes_start_x, nodes_start_y;
    
    nodex_x = 40;
    nodes_y = 40;
    nodes_r = 5;
    nodes_start_x = 50;
    nodes_start_y = 40;
    cloth = new SoftBody(idx++);
    particles = cloth.create(particles, param_cloth, nodex_x, nodes_y, nodes_r,  nodes_start_x, nodes_start_y);
    cloth.getNode(              0, 0).enable(false, false, false);
    cloth.getNode(cloth.nodes_x-1, 0).enable(false, false, false);
    
//    nodex_x = 20;
//    nodes_y = 60;
//    nodes_r = 5;
//    nodes_start_x = 160;
//    nodes_start_y = 60;
//    particles = cloth.create(particles, param_cloth, nodex_x, nodes_y, nodes_r,  nodes_start_x, nodes_start_y);
//    cloth.getNode(              0, 0).enable(false, false, false);
//    cloth.getNode(cloth.nodes_x-1, 0).enable(false, false, false);

    nodex_x = 15;
    nodes_y = 25;
    nodes_r = 5;
    nodes_start_x = width/2;
    nodes_start_y = height/2;
    cloth = new SoftBody(idx++);

    particles = cloth.create(particles, param_softbody, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
    
    
    nodex_x = 10;
    nodes_y = 30;
    nodes_r = 5;
    nodes_start_x = width - nodex_x*nodes_r*5;
    nodes_start_y = 200;
    cloth = new SoftBody(idx++);
    particles = cloth.create(particles, param_softbody, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
    cloth.getNode(               0, 0).enable(false, false, false);

    
    
   
    
    nodex_x = 10;
    nodes_y = 2;
    nodes_r = 25;
    nodes_start_x = 500;
    nodes_start_y = 100;
    cloth = new SoftBody(idx++);
    particles = cloth.create(particles, param_softbody, nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
    cloth.getNode(               0, 0).enable(false, false, false);
    cloth.getNode(               0, 1).enable(false, false, false);
    
    
    
    SpringConstraint.makeAllSpringsUnidirectional(particles); // default anyways
//    SpringConstraint.makeAllSpringsBidirectional(particles);
    frameRate(600);
  }
  


  
  public void draw() {
    

    background(255);
      
    if(!INTERACTION_DELETE_SPRING && particle_mouse != null){
      VerletParticle2D particle = particle_mouse;
      float dx = mouseX - particle.cx;
      float dy = mouseY - particle.cy;
      
      float damping_pos = 0.2f;
      particle.px = particle.cx;
      particle.py = particle.cy;
      particle.cx  += dx * damping_pos;
      particle.cy  += dy * damping_pos;
    }
    
    if(INTERACTION_DELETE_SPRING && mousePressed){
      float radius = 10;

      ArrayList<VerletParticle2D> list = findParticlesWithinRadius(mouseX, mouseY, radius*radius);
      for(VerletParticle2D tmp : list){
        SpringConstraint.deleteSprings(particles, tmp.idx);
        tmp.collision_group = physics.getNewCollisionGroupId();
      }
      
      fill(255,0,0,32);
      stroke(0);
      strokeWeight(1);
      ellipse(mouseX, mouseY, radius*2, radius*2);
    }

    
    physics.update(particles, particles.length, 1);

    drawParticles();
    // draw
    draw(SpringConstraint.TYPE.BEND);
    draw(SpringConstraint.TYPE.SHEAR);
    draw(SpringConstraint.TYPE.STRUCT);
//    draw(null);

    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  

  public void draw(SpringConstraint.TYPE type){
    beginShape(LINES);
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint spring = pa.springs[j];
        if(!spring.parent) continue;
        VerletParticle2D pb = particles[spring.idx];
        
        if(type != null && type != spring.type) continue;
        
        switch(spring.type){
          case STRUCT: strokeWeight(   1); stroke(  0,  0,  0); vertex(pa.cx, pa.cy); vertex(pb.cx, pb.cy); break;
          case SHEAR:  strokeWeight(0.8f); stroke( 40,140,255); vertex(pa.cx, pa.cy); vertex(pb.cx, pb.cy); break;
          case BEND:   strokeWeight(0.5f); stroke(255,180,  0); vertex(pa.cx, pa.cy); vertex(pb.cx, pb.cy); break;
          default: break;
        }
      }
    }
    endShape();
  }
  
  public void drawParticles(){
    noFill();
    noStroke();
    fill(0, 32);
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      ellipse(pa.cx, pa.cy, pa.rad*2, pa.rad*2);
    }
  }
  
  
  
  
  
  
  
  VerletParticle2D particle_mouse = null;
  
  public VerletParticle2D findNearestParticle(float mx, float my){
    return findNearestParticle(mx, my, Float.MAX_VALUE);
  }
  
  public VerletParticle2D findNearestParticle(float mx, float my, float dd_min){
    VerletParticle2D particle = null;
    for(int i = 0; i < particles.length; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq = dx*dx + dy*dy;
      if( dd_sq < dd_min){
        dd_min = dd_sq;
        particle = particles[i];
      }
    }
    return particle;
  }
  
  public ArrayList<VerletParticle2D> findParticlesWithinRadius(float mx, float my, float dd_min){
    ArrayList<VerletParticle2D> list = new ArrayList<VerletParticle2D>();
    for(int i = 0; i < particles.length; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq = dx*dx + dy*dy;
      if( dd_sq < dd_min){
        list.add(particles[i]);
      }
    }
    return list;
  }
    

  boolean INTERACTION_DELETE_SPRING = false;
  
  
  boolean state_enable_collisions;
  boolean state_enable_springs;
  boolean state_enable_forces;

  public void mousePressed(){
    if(!INTERACTION_DELETE_SPRING){
      particle_mouse = findNearestParticle(mouseX, mouseY);
      // push states
      state_enable_collisions = particle_mouse.enable_collisions;
      state_enable_springs    = particle_mouse.enable_springs   ;
      state_enable_forces     = particle_mouse.enable_forces    ;  
      if(mouseButton == LEFT  ) particle_mouse.enable(true, false, !true);
      if(mouseButton == CENTER) particle_mouse.enable(false, false, false);
      if(mouseButton == RIGHT ) particle_mouse.enable(false, false, false);
    }
  }
  
  public void mouseReleased(){
    if(particle_mouse != null && !INTERACTION_DELETE_SPRING){
      if(mouseButton == LEFT  ) particle_mouse.enable(state_enable_collisions, state_enable_springs, state_enable_forces);
      if(mouseButton == CENTER) particle_mouse.enable(true, true, true);
      if(mouseButton == RIGHT ) particle_mouse.enable(true, false, false);
      particle_mouse = null;
    }
  }
  

  public void keyPressed(){
    if(key ==' ') INTERACTION_DELETE_SPRING = true;
  }
  public void keyReleased(){
    if(key ==' ') INTERACTION_DELETE_SPRING = false;
  }
  
  public static void main(String args[]) {
    PApplet.main(new String[] { VerletPhysics_Cloth.class.getName() });
  }
}