package com.thomasdiewald.pixelflow.java.softbodydynamics.softbody;

import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint.TYPE;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

public abstract class DwSoftBody2D extends DwSoftBody{
  
  
  // for customizing the particle we just extends the original class and
  // Override what we want to customize
  public class CustomParticle2D extends DwParticle2D{
    
    public CustomParticle2D(int idx, float x, float y, float rad) {
      super(idx, x, y, rad);
    }
    
    @Override
    public void updateShapeColor(){
      if(use_particles_color){
        setColor(particle_color);
      } else {
        setColor(particle_gray);
      }
//      super.updateShapeColor();
    }
    
  }


  public DwParticle2D[] particles; // particles of this body

  public DwSoftBody2D(){
  }
  

  //////////////////////////////////////////////////////////////////////////////
  // RENDERING
  //////////////////////////////////////////////////////////////////////////////

  
  @Override
  public void createShapeParticles(PApplet papplet){
    createShapeParticles(papplet, false);
  }
  
  @Override
  public void createShapeParticles(PApplet papplet, boolean icosahedron){
    PShape shp = papplet.createShape(PShape.GROUP);
    for(int i = 0; i < particles.length; i++){
      PShape shp_pa = createShape(papplet, particles[i].rad);
      particles[i].setShape(shp_pa);
      shp.addChild(shp_pa);
    }
    setShapeParticles(papplet, shp);
  }
  
  
  private PShape createShape(PApplet papplet, float radius){
    PShape shape = papplet.createShape(PConstants.ELLIPSE, 0, 0, radius*2, radius*2);
    shape.setStroke(false);
    shape.setFill(true);
    return shape;
  }
  
  
  

  @Override
  public void displaySprings(PGraphics pg, DwStrokeStyle style, TYPE type){
    pg.beginShape(PConstants.LINES);
    pg.strokeWeight(style.stroke_weight);
    pg.stroke(style.stroke_color);
    
    for(int i = 0; i < particles.length; i++){
      DwParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        DwSpringConstraint2D spring = (DwSpringConstraint2D) pa.springs[j];
        if(spring.type != type) continue;
        if(!spring.enabled) continue;
        if(spring.pa != pa) continue;
        DwParticle2D pb = spring.pb;
        
        if(shade_springs_by_tension){
          float force_curr = spring.computeForce(); // the force, at this moment
          float force_relx = spring.force;          // the force, remaining after the last relaxation step
          float force = Math.abs(force_curr) + Math.abs(force_relx);
          float r = force * 10000;
          float g = force * 1000;
          float b = 0;
          pg.stroke(toARGB(r,g,b));
        } 
        
        pg.vertex(pa.cx, pa.cy); 
        pg.vertex(pb.cx, pb.cy);
      }
    }
    pg.endShape();
  }
  

  
}
  
  
  
 
  