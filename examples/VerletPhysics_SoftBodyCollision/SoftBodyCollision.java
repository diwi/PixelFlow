package VerletPhysics_SoftBodyCollision;




import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics2D;

import processing.core.*;

public class SoftBodyCollision extends PApplet {

  int viewport_w = 1200;
  int viewport_h = 800;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  

  VerletPhysics2D physics;
  
  VerletParticle2D[] particles = new VerletParticle2D[0];
  
  VerletParticle2D.Param param_softbody = new VerletParticle2D.Param();
  VerletParticle2D.Param param_cloth    = new VerletParticle2D.Param();
  
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
    physics.param.iterations_springs    = 16;
    
    int idx = 0;
    
    param_softbody.DAMP_BOUNDS    = 0.2f;
    param_softbody.DAMP_COLLISION = 1;
    param_softbody.DAMP_VELOCITY  = 1;
    param_softbody.DAMP_SPRING_decrease = 0.999999f;
    param_softbody.DAMP_SPRING_increase = 0.999999f;
    
    param_cloth.DAMP_BOUNDS    = 0.2f;
    param_cloth.DAMP_COLLISION = 1;
    param_cloth.DAMP_VELOCITY  = 1;
    param_cloth.DAMP_SPRING_decrease = 0.999999f;
    param_cloth.DAMP_SPRING_increase = 0.0009999f;
    

    SoftBody softbody;
    SoftCircle softcircle;
  
    softbody = new SoftBody(idx++);
    particles = softbody.create(particles, param_softbody, 5, 5, 10, 100, 100);
    
    softbody = new SoftBody(idx++);
    particles = softbody.create(particles, param_softbody, 5, 5, 10, 200, 100);
    
    softbody = new SoftBody(idx++);
    particles = softbody.create(particles, param_softbody, 10, 10, 10, 150, 300);
    
    softcircle = new SoftCircle(idx++, 10);
    particles = softcircle.create(particles, param_softbody, 900, 100, 70);
    
    softbody = new SoftBody(idx++);
    particles = softbody.create(particles, param_cloth, 40, 20, 10,  400, 300);
    softbody.getNode(particles,  0, 0).enable(true, false, false);
    softbody.getNode(particles, 39, 0).enable(true, false, false);
    
    frameRate(60);
  }
  


  
  public void draw() {
    

    background(255);
      
    // mouse interaction
    if(particle_mouse != null){
      float damping = 1;
      float dx = mouseX - particle_mouse.cx;
      float dy = mouseY - particle_mouse.cy;
      particle_mouse.cx += dx * damping;
      particle_mouse.cy += dy * damping;
    } 

    
    physics.update(particles, particles.length, 1);

  
    // draw
    beginShape(LINES);
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint spring = pa.springs[j];
        VerletParticle2D pb = spring.pb;
  
        switch(spring.type){
          case STRUCT:
            strokeWeight(1);
            stroke(0,0,0);
            vertex(pa.cx, pa.cy);
            vertex(pb.cx, pb.cy);
            break;
          case SHEAR:
            strokeWeight(0.5f);
            stroke(0,255,0);
            vertex(pa.cx, pa.cy);
            vertex(pb.cx, pb.cy);
            break;
          case BEND:
            strokeWeight(0.5f);
            stroke(255,0,0);
            vertex(pa.cx, pa.cy);
            vertex(pb.cx, pb.cy);
            break;
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
  
  
  
  public VerletParticle2D findNearestParticle(float mx, float my){
    VerletParticle2D particle = null;
    float dd_min = Float.MAX_VALUE;
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

  
  public static void main(String args[]) {
    PApplet.main(new String[] { SoftBodyCollision.class.getName() });
  }
}