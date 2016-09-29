package com.thomasdiewald.pixelflow.java.softbodydynamics.softbody;


import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSetAble;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

public abstract class DwSoftBody3D extends DwSoftBody{
  
  
  // for customizing the particle we just extends the original class and
  // Override what we want to customize
  public class CustomParticle3D extends DwParticle3D{
    
    
    public CustomParticle3D(int idx, float x, float y, float z, float rad) {
      super(idx, x, y, z, rad);
    }
    
    @Override
    public void updateShapeColor(){
      if(use_particles_color){
        setColor(particle_color);
        shp_particle.setStroke(particle_color);
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
  public void createParticlesShape(PApplet papplet){
    papplet.shapeMode(PConstants.CORNER);
    shp_particles = papplet.createShape(PShape.GROUP);
    for(int i = 0; i < particles.length; i++){
      PShape shp_pa = createShape(papplet, particles[i]);
      particles[i].setShape(shp_pa);
      shp_particles.addChild(shp_pa);
    }
//    shp_particles.getTessellation();
  }

  

  DwIndexedFaceSetAble ifs;
  
  PShape createShape(PApplet papplet, DwParticle3D particle){
    PShape shape;
    
    shape = papplet.createShape(PConstants.POINT, 0, 0);
    shape.setStroke(true);
    shape.setStrokeWeight(6);

//    if(ifs == null){
//      ifs = new DwIcosahedron(1);
////      ifs = new DwCube(1);
//    }
//    shape = papplet.createShape(PShape.GEOMETRY);
//    createPolyhedronShape(shape, ifs, particle.rad, 3, true);

    return shape;
  }
  
  
  public void createPolyhedronShape(PShape shape, DwIndexedFaceSetAble ifs, float scale, int verts_per_face, boolean smooth){
    
    int type = -1;
    
    switch(verts_per_face){
      case 3: type = PConstants.TRIANGLES; break;
      case 4: type = PConstants.QUADS; break;
      default: return;
    }
   
    shape.beginShape(type);
    shape.noStroke();
    
    int  [][] faces = ifs.getFaces();
    float[][] verts = ifs.getVerts();
    
    for(int[] face : faces){
      float nx = 0, ny = 0, nz = 0;

      int num_verts = face.length;
      
      // compute face normal
      if(!smooth){
        for(int i = 0; i < num_verts; i++){
          int vi = face[i];
          nx += verts[vi][0];
          ny += verts[vi][1];
          nz += verts[vi][2];
        }
        nx /= num_verts;
        ny /= num_verts;
        nz /= num_verts;
        shape.normal(nx, ny, nz); 
      }
      
      for(int i = 0; i < num_verts; i++){
        float[] v = verts[face[i]];
        if(smooth){
          shape.normal(v[0], v[1], v[2]); 
        }
        shape.vertex(v[0]*scale, v[1]*scale, v[2]*scale);
      }
    }
    shape.endShape(PConstants.CLOSE);
  }
  

  public void displaySprings(PGraphics pg, int display_mode){
    if(display_mode == -1) return;
    
    float r=0,g=0,b=0,strokeweight=1;
    float force, force_curr, force_relx;

    pg.beginShape(PConstants.LINES);
    for(int i = 0; i < particles.length; i++){
      DwParticle3D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        DwSpringConstraint3D spring = (DwSpringConstraint3D) pa.springs[j];
        if(!spring.enabled) continue;
        if(spring.pa != pa) continue;
        DwParticle3D pb = spring.pb;

        if(display_mode == 0){
          switch(spring.type){
            case STRUCT:  if(!DISPLAY_SPRINGS_STRUCT) continue; strokeweight = 1.00f; r =   0; g =   0; b =   0; break;
            case SHEAR:   if(!DISPLAY_SPRINGS_SHEAR ) continue; strokeweight = 0.80f; r =  70; g = 140; b = 255; break;
            case BEND:    if(!DISPLAY_SPRINGS_BEND  ) continue; strokeweight = 0.60f; r = 255; g =  90; b =  30; break;
            default: continue;
          }
        }
        if(display_mode == 1){
          force_curr = spring.computeForce(); // the force, at this moment
          force_relx = spring.force;          // the force, remaining after the last relaxation step
          force = Math.abs(force_curr) + Math.abs(force_relx);
          r = force * 10000;
          g = force * 1000;
          b = 0;
        } 
        
        pg.strokeWeight(strokeweight);
        pg.stroke(r,g,b);
        pg.vertex(pa.cx, pa.cy, pa.cz); 
        pg.vertex(pb.cx, pb.cy, pb.cz);
      }
    }
    pg.endShape();
 }

  
  
  
//  public 
  public float display_normal_length = 20;

  public abstract void computeNormals();
  public abstract void displayMesh(PGraphics pg);
  public abstract void displayNormals(PGraphics pg);
 

}
  
  
  
 
  