package VerletPhysics_Dev;

import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

public abstract class SoftBody{
  
  
  // for customizing the particle we just extends the original class and
  // Override what we want to customize
  class CustomVerletParticle2D extends VerletParticle2D{
    public CustomVerletParticle2D(int idx, float x, float y, float rad) {
      super(idx, x, y, rad);
    }
    
    @Override
    public void updateShapeColor(){
      setColor(particle_color);
//      super.updateShapeColor();
    }
    
  }

  
  // general attributes
  
  public boolean CREATE_STRUCT_SPRINGS = true;
  public boolean CREATE_SHEAR_SPRINGS  = true;
  public boolean CREATE_BEND_SPRINGS   = true;
  
  int collision_group_id;       // particles that share the same id, are ignored during collision tests
  int num_nodes;                // number of particles for this object
  int nodes_offset;             // offset in the global array, used for creating a unique id
  VerletParticle2D[] particles; // particles of this body
  PShape shp_particles;         // shape for drawing all particles of this body
  
  public SoftBody(){
  }
  
  


  //////////////////////////////////////////////////////////////////////////////
  // RENDERING
  //////////////////////////////////////////////////////////////////////////////
  public int particle_color;
  
  public void createShape(PApplet papplet, int particle_color){
    this.particle_color = particle_color;

    papplet.shapeMode(PConstants.CORNER);
    shp_particles = papplet.createShape(PShape.GROUP);
    for(int i = 0; i < particles.length; i++){
      float rad = particles[i].rad;
      PShape shp_pa = papplet.createShape(PConstants.ELLIPSE, 0, 0, rad*2, rad*2);
      shp_pa.setStroke(false);
      shp_pa.setFill(true);
      shp_pa.setFill(particle_color);
      
      particles[i].setShape(shp_pa);
      shp_particles.addChild(shp_pa);
    }
    
    shp_particles.getTessellation();
  }

  public void drawParticles(PGraphics pg){
    pg.shape(shp_particles);
  }
  
  
  public void drawSprings(PGraphics pg, SpringConstraint.TYPE type){
    pg.beginShape(PConstants.LINES);
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint spring = pa.springs[j];
        if(!spring.is_the_good_one) continue;
        if(type != null && type != spring.type) continue;
        
        switch(spring.type){
          case STRUCT:  pg.strokeWeight(1.0f );  pg.stroke(  0,  0,  0); break;
          case SHEAR:   pg.strokeWeight(0.80f);  pg.stroke( 70,140,255); break;
          case BEND:    pg.strokeWeight(0.70f);  pg.stroke(255, 90, 30); break;
          default: continue;
        }
        
        VerletParticle2D pb = spring.pb;
        pg.vertex(pa.cx, pa.cy); 
        pg.vertex(pb.cx, pb.cy);
      }
    }
    pg.endShape();
  }
  
  
  
  public void drawTension(PGraphics pg){
    float r,g,b;
    float force;
    float force_curr;
    float force_relx;
    
    pg.beginShape(PConstants.LINES);
    for(int i = 0; i < particles.length; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint spring = pa.springs[j];
        if(!spring.is_the_good_one) continue;

        VerletParticle2D pb = spring.pb;
        
        force_curr = spring.computeForce(); // the force, at this moment
        force_relx = spring.force;          // the force, remaining after the last relaxation step
        
        force = Math.abs(force_curr) + Math.abs(force_relx);
        
        r = force * 10000;
        g = force * 1000;
        b = 0;
        
        if(spring.type == SpringConstraint.TYPE.STRUCT) pg.strokeWeight(1.0f);  
        if(spring.type == SpringConstraint.TYPE.SHEAR ) pg.strokeWeight(0.6f);
        if(spring.type == SpringConstraint.TYPE.BEND  ) pg.strokeWeight(0.3f);  
        pg.stroke(r, g, b);
        pg.vertex(pa.cx, pa.cy); 
        pg.vertex(pb.cx, pb.cy);
      }
    }
    pg.endShape();
  }
  
  

}
  
  
  
 
  