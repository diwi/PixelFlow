package com.thomasdiewald.pixelflow.java.softbodydynamics.softbody;


import com.thomasdiewald.pixelflow.java.geometry.DwIcosahedron;
import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSetAble;
import com.thomasdiewald.pixelflow.java.geometry.DwMeshUtils;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint.TYPE;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

public abstract class DwSoftBody3D extends DwSoftBody{
  
  
  // for customizing the particle we just extends the original class and
  // Override what we want to customize
  public class CustomParticle3D extends DwParticle3D{
    
    public CustomParticle3D(int idx, float[] v, float rad) {
      super(idx, v[0], v[1], v[2], rad);
    }
    
    public CustomParticle3D(int idx, float x, float y, float z, float rad) {
      super(idx, x, y, z, rad);
    }
    
    @Override
    public void updateShapeColor(){
      if(use_particles_color){
        if(collision_group_id != this.collision_group){
          setColor(particle_color2);
          shp_particle.setStroke(particle_color2);
        } else {
          setColor(particle_color);
          shp_particle.setStroke(particle_color);
        }
      } else {
        setColor(particle_gray);
        shp_particle.setStroke(particle_gray);
      }
//      super.updateShapeColor();
    }
    
  }

  
  public DwParticle3D[] particles; // particles of this body
 
  
  public DwSoftBody3D(){
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
      PShape shp_pa = createShape(papplet, particles[i].rad, icosahedron);
      particles[i].setShape(shp_pa);
      shp.addChild(shp_pa);
    }
    setShapeParticles(papplet, shp);
  }
  
  
  private DwIndexedFaceSetAble ifs;
  
  private PShape createShape(PApplet papplet, float radius, boolean icosahedron){
    PShape shape;
    
    if(!icosahedron){
      shape = papplet.createShape(PConstants.POINT, 0, 0);
      shape.setStroke(true);
      shape.setStrokeWeight(6);
    } else {
      if(ifs == null){
        ifs = new DwIcosahedron(1);
//        ifs = new DwCube(1);
      }
      shape = papplet.createShape(PShape.GEOMETRY);
      shape.setStroke(false);
      shape.setFill(true);
      DwMeshUtils.createPolyhedronShape(shape, ifs, radius, 3, true);
    }

    return shape;
  }
  
  public void updateParticlesShapes(){
    for(int i = 0; i < particles.length; i++){
      particles[i].updateShape();
    }
  }
  

  @Override
  public void displaySprings(PGraphics pg, DwStrokeStyle style, TYPE type){
    pg.beginShape(PConstants.LINES);
    pg.strokeWeight(style.stroke_weight);
    pg.stroke(style.stroke_color);
    
    for(int i = 0; i < particles.length; i++){
      DwParticle3D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        DwSpringConstraint3D spring = (DwSpringConstraint3D) pa.springs[j];
        if(spring.type != type) continue;
        if(!spring.enabled) continue;
        if(spring.pa != pa) continue;
        DwParticle3D pb = spring.pb;
        
        if(shade_springs_by_tension){
          float force_curr = spring.computeForce(); // the force, at this moment
          float force_relx = spring.force;          // the force, remaining after the last relaxation step
          float force = Math.abs(force_curr) + Math.abs(force_relx);
          float r = force * 10000;
          float g = force * 1000;
          float b = 0;
          pg.stroke(toARGB(r,g,b));
        } 
        
        pg.vertex(pa.cx, pa.cy, pa.cz); 
        pg.vertex(pb.cx, pb.cy, pb.cz);
      }
    }
    pg.endShape();
  }
  
  
  
  
//  public 
  public float display_normal_length = 20;

  public abstract void computeNormals();
  public abstract void displayNormals(PGraphics pg);
 

}
  
  
  
 
  