package VerletPhysics_SpringChain;


import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletPhysics2D;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.SpringConstraint;

import processing.core.*;

public class SpringChain extends PApplet {

  int viewport_w = 1200;
  int viewport_h = 800;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  
  VerletPhysics2D physics;

  VerletParticle2D.Param param = new VerletParticle2D.Param();

  
  int particles_count = 0;
  VerletParticle2D[] particles = new VerletParticle2D[particles_count];
  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  
 
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    
    physics = new VerletPhysics2D();

    physics.param.GRAVITY = new float[]{ 0, 1 };
    physics.param.bounds  = new float[]{ 0, 0, width, height };
    physics.param.iterations_collisions = 4;
    physics.param.iterations_springs    = 4;
    
    
    
    param.DAMP_BOUNDS    = 0.91f;
    param.DAMP_COLLISION = 0.999999f;
    param.DAMP_VELOCITY  = 0.99f;
    param.DAMP_SPRING_decrease = 0.9999999f;
    param.DAMP_SPRING_increase = 0.9999999f;
    
    randomSeed(1);
    for(int i = 0; i < 201; i++){
      build();
      if(i%50 == 0){
        particles[particles_count-1].enable(false, false, false);
      }
    }


    frameRate(60);
  }

  
  public void draw() {
    
    if(keyPressed && key == ' '){
      build();
    }
 
    background(255);

    // mouse interaction
    if(particle_mouse != null){
      float damping = 1;
      float dx = mouseX - particle_mouse.cx;
      float dy = mouseY - particle_mouse.cy;
      particle_mouse.cx += dx * damping;
      particle_mouse.cy += dy * damping;
    } 

   
    physics.update(particles, particles_count, 1);
    


    // draw
    beginShape(LINES);
    for(int i = 0; i < particles_count; i++){
      VerletParticle2D pa = particles[i];
      
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint spring = pa.springs[j];
        VerletParticle2D pb = particles[spring.idx];
  
        switch(spring.type){
          case STRUCT:
            strokeWeight(1);
            stroke(0);
            vertex(pa.cx, pa.cy);
            vertex(pb.cx, pb.cy);
            break;
  //        case SHEAR:
  //          strokeWeight(0.5f);
  //          stroke(0,255,0);
  //          vertex(pa.cx, pa.cy);
  //          vertex(pb.cx, pb.cy);
  //          break;
  //        case BEND:
  //          strokeWeight(0.5f);
  //          stroke(255,0,0);
  //          vertex(pa.cx, pa.cy);
  //          vertex(pb.cx, pb.cy);
  //          break;
          default:
            break;
        }
      }
    }
    endShape();
    
    
    for(int i = 0; i < particles_count; i++){
      VerletParticle2D pa = particles[i];
      fill(0);
//      stroke(100,100);
      noStroke();
      ellipse(pa.cx, pa.cy, pa.rad*2, pa.rad*2);
    }
    

    
    
    
   
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  
  
  
  
  
  
  
  public void build(){
    int idx_prev = particles_count-1;
    
//    idx_prev = (int) random(0, particle_count);
//    idx_prev = 0;

    if(particles_count > 0 && idx_prev >= 0){
      createParticle(particles[idx_prev]);
//      createParticle(particles[idx_prev]);
    } else {
      createParticle(null);
    }
  }
  
  
  public void createParticle(VerletParticle2D particle_prev){
    
    int idx_curr = particles_count;
    int off = 50;
    float radius = 5;

    VerletParticle2D particle_curr = new VerletParticle2D(idx_curr);
    particle_curr.setCollisionGroup(idx_curr);
    particle_curr.setMass(1);
    particle_curr.setParamByRef(param);
    particle_curr.setRadius(radius);
    particle_curr.setPosition(random(off, width-off), random(off, height-off));
    if(idx_curr == 0) particle_curr.enable(true, true, true);
    addParticle(particle_curr);
    
    if(particle_prev != null){
      float restlen = radius*2f;
      float rest_len_sq = restlen*restlen;
      
      particle_curr.addSpring(new SpringConstraint(particle_prev.idx, rest_len_sq));
      particle_prev.addSpring(new SpringConstraint(particle_curr.idx, rest_len_sq));
    }
  }
  
  
  public void reset(){
    particles_count = 0;
    particles = new VerletParticle2D[particles_count];
  }
  
  public void addParticle(VerletParticle2D particle){
    if(particles_count >= particles.length){
      int new_len = (int) Math.max(2, Math.ceil(particles_count*1.5f) );
      if(particles == null){
        particles = new VerletParticle2D[new_len];
      } else {
        particles = Arrays.copyOf(particles, new_len);
      }
    }
    
    particles[particles_count++] = particle;
  }
  

  
  VerletParticle2D particle_mouse = null;
  
  public VerletParticle2D findNearestParticle(float mx, float my){
    VerletParticle2D particle = null;
    float dd_min = Float.MAX_VALUE;
    for(int i = 0; i < particles_count; i++){
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
    
  public void mousePressed(){
    particle_mouse = findNearestParticle(mouseX, mouseY);
    if(mouseButton == CENTER) particle_mouse.enable(true, true, true);
    if(mouseButton == RIGHT ) particle_mouse.enable(true, false, false);
  }
  
  public void mouseReleased(){
    if(mouseButton == CENTER) particle_mouse.enable(true, true, true);
    if(mouseButton == RIGHT ) particle_mouse.enable(true, false, false);
    particle_mouse.px = particle_mouse.cx = mouseX;
    particle_mouse.py = particle_mouse.cy = mouseY;
    particle_mouse = null;
  }

  
  public void keyReleased(){
    if(key == 'n')  build();
    if(key == 'r')  reset();
  }

  
  public static void main(String args[]) {
    PApplet.main(new String[] { SpringChain.class.getName() });
  }
}