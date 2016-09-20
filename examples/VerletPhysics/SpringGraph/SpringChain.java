package VerletPhysics.SpringGraph;




import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.CollisionGridAccelerator;
import com.thomasdiewald.pixelflow.java.verletPhysics2D.VerletParticle2D;
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
  
  
  public static float GRAVITY = 0f;

  VerletParticle2D.Param param = new VerletParticle2D.Param();

  int particle_count = 0;
  VerletParticle2D[] particles = new VerletParticle2D[particle_count];
  
  CollisionGridAccelerator collsion_grid;
  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  
  
  public void addParticle(VerletParticle2D particle){
    if(particle_count >= particles.length){
      int new_len = (int) Math.max(2, Math.ceil(particle_count*1.5f) );
      if(particles == null){
        particles = new VerletParticle2D[new_len];
      } else {
        particles = Arrays.copyOf(particles, new_len);
      }
    }
    
    particles[particle_count++] = particle;
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    collsion_grid = new CollisionGridAccelerator();
    
    int idx = 0;
    
    param.DAMP_BOUNDS    = 1f;
    param.DAMP_COLLISION = 0.999999f;
    param.DAMP_VELOCITY  = 0.99f;
    param.DAMP_SPRING_decrease = 1;
    param.DAMP_SPRING_increase = 1;
    
    GRAVITY = 0f;


    frameRate(60);
  }
  

  public void reset(){
    particle_count = 0;
    particles = new VerletParticle2D[particle_count];
    
  }
  
  public void keyReleased(){
    if(key == 'n'){
      build();
    }
    
    if( key == 'r'){
      reset();
    }
  }
  
  public void build(){
    int idx_prev = particle_count-1;
    
//    idx_prev = (int) random(0, particle_count);
//    idx_prev = 0;

    if(particle_count > 0 && idx_prev >= 0){
      createParticle(particles[idx_prev]);
//      createParticle(particles[idx_prev]);
    } else {
      createParticle(null);
    }
  }
  
  
  public void createParticle(VerletParticle2D particle_prev){
    int idx_curr = particle_count;
    int off = 50;
    VerletParticle2D particle_curr = new VerletParticle2D(idx_curr);
    
    float radius = 5;
    particle_curr.setCollisionGroup(idx_curr);
    particle_curr.setMass(1);
    particle_curr.setParamByRef(param);
    particle_curr.setRadius(radius);
    particle_curr.setPosition(random(off, width-off), random(off, height-off));
    if(idx_curr == 0) particle_curr.enable(true, true, true);
    addParticle(particle_curr);
    
    if(particle_prev != null){
    float restlen = radius * 2 * 1.1f;
    float rest_len_sq = restlen*restlen;
    
//    particle_curr.addSpring(new SpringConstraint(particle_prev.idx, 0.0f, 0.000f, rest_len_sq, SpringConstraint.TYPE.STRUCT));
//    particle_prev.addSpring(new SpringConstraint(particle_curr.idx, 0.0f, 0.000f, rest_len_sq, SpringConstraint.TYPE.STRUCT));
    particle_curr.addSpring(new SpringConstraint(particle_prev.idx, rest_len_sq));
    particle_prev.addSpring(new SpringConstraint(particle_curr.idx, rest_len_sq));
    }
  }
  
  

  
  public void draw() {
    
    if(keyPressed && key == ' '){
      build();
    }


    background(255);
      
    float timestep = 1f;
    int iterations_springs = 4;
    int iterations_collisions = 2;

    // mouse interaction
    if(particle_mouse != null){
      float damping = 1;
      float dx = mouseX - particle_mouse.cx;
      float dy = mouseY - particle_mouse.cy;
      particle_mouse.cx += dx * damping;
      particle_mouse.cy += dy * damping;
    } 
      
    // iterative spring refinement
    for(int k = 0; k < iterations_springs; k++){
      for(int i = 0; i < particle_count; i++){
        particles[i].beforeSprings();
      }
      for(int i = 0; i < particle_count; i++){
        particles[i].updateSprings(particles);
      }
      for(int i = 0; i < particle_count; i++){
        particles[i].afterSprings(0, 0, width-0, height-0);
      }
    }


    // verlet integration
    for(int i = 0; i < particle_count; i++){
      particles[i].addGravity(0.0f, GRAVITY);
      particles[i].updatePosition(0, 0, width-0, height-0, timestep);
    }
 
    for(int k = 0; k < iterations_collisions; k++){
      collsion_grid.updateCollisions(particles, particle_count);
    }
    
    
    
  
    // draw
    
    
    for(int i = 0; i < particle_count; i++){
      VerletParticle2D pa = particles[i];
      fill(200,200);
      stroke(100,100);
      ellipse(pa.cx, pa.cy, pa.rad*2, pa.rad*2);
    }
    
    beginShape(LINES);
    for(int i = 0; i < particle_count; i++){
      VerletParticle2D pa = particles[i];
      
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint spring = pa.springs[j];
        VerletParticle2D pb = particles[spring.idx];
  
        switch(spring.type){
          case STRUCT:
            strokeWeight(1);
            stroke(0,0,0);
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

    
    
    
   
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  VerletParticle2D particle_mouse = null;
  
  public void mousePressed(){
    float dd_min = Float.MAX_VALUE;
    for(int i = 0; i < particle_count; i++){
      float dx = mouseX - particles[i].cx;
      float dy = mouseY - particles[i].cy;
      float dd_sq = dx*dx + dy*dy;
      if( dd_sq < dd_min){
        dd_min = dd_sq;
        particle_mouse = particles[i];
      }
    }
    if(particle_mouse == null) return;
    
    if(mouseButton == CENTER){
      particle_mouse.enable(true, true, true);
    }
    if(mouseButton == RIGHT ){
      particle_mouse.enable(false, false, false);
    }
  }
  public void mouseReleased(){
    if(particle_mouse == null) return;
    
    if(mouseButton == CENTER){
      particle_mouse.enable(true, true, true);
    }
    if(mouseButton == RIGHT ){
      particle_mouse.enable(false, false, false);
    }
    particle_mouse.px = particle_mouse.cx = mouseX;
    particle_mouse.py = particle_mouse.cy = mouseY;
    particle_mouse = null;

  }

  

 

  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { SpringChain.class.getName() });
  }
}