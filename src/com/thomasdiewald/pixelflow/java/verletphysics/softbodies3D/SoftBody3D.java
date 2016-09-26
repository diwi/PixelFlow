package com.thomasdiewald.pixelflow.java.verletphysics.softbodies3D;


import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.geometry.Icosahedron;
import com.thomasdiewald.pixelflow.java.geometry.Tetrahedron;
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint3D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle3D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

public abstract class SoftBody3D{
  
  
  // for customizing the particle we just extends the original class and
  // Override what we want to customize
  public class CustomVerletParticle3D extends VerletParticle3D{
    
    
    public CustomVerletParticle3D(int idx, float x, float y, float z, float rad) {
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

  
  // general attributes
  
  // can be used for sub-classes
  public boolean CREATE_STRUCT_SPRINGS = true;
  public boolean CREATE_SHEAR_SPRINGS  = true;
  public boolean CREATE_BEND_SPRINGS   = true;
  
  public boolean self_collisions = false; // true, all particles get a different collision_group_id
  public int collision_group_id;       // particles that share the same id, are ignored during collision tests
  public int num_nodes;                // number of particles for this object
  public int nodes_offset;             // offset in the global array, used for creating a unique id
  public VerletParticle3D[] particles; // particles of this body
  public PShape shp_particles;         // shape for drawing all particles of this body
  
  
  public VerletParticle3D.Param   param_particle = new VerletParticle3D.Param();
  public SpringConstraint3D.Param param_spring   = new SpringConstraint3D.Param();
  
  
  public SoftBody3D(){
  }
  
  
  
  

  
  public void setParam(VerletParticle3D.Param param_particle){
    this.param_particle = param_particle;
  }
  public void setParam(SpringConstraint3D.Param param_spring){
    this.param_spring = param_spring;
  }
  
  


  //////////////////////////////////////////////////////////////////////////////
  // RENDERING
  //////////////////////////////////////////////////////////////////////////////
  public int particle_color = 0xFF5C0000; // color(0, 92), default
  public int particle_gray  = 0xFF5C0000; // color(0, 92)
  public boolean use_particles_color = true;
  public float collision_radius_scale = 1.33333f;
  
  public int material_color = 0xFF555555;
  
  public void setParticleColor(int particle_color){
    this.particle_color = particle_color;
  }
  
  public void setMaterialColor(int material_color){
    this.material_color = material_color;
  }
  
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

  
  Tetrahedron tetrahedron;
  Icosahedron icosahedron;
  PShape createShape(PApplet papplet, VerletParticle3D particle){
    PShape shape;
    
    shape = papplet.createShape(PConstants.POINT, 0, 0);
    shape.setStroke(true);
    shape.setStrokeWeight(6);

//    if(tetrahedron == null){
//      tetrahedron = new Tetrahedron();
//      tetrahedron.create(0);
//    }
//    
//    if(icosahedron == null){
//      icosahedron = new Icosahedron();
//      icosahedron.create(0);
//    }
    
//    shape = papplet.createShape(PShape.GEOMETRY);
//    createPolyhedronShape(shape, tetrahedron.faces, tetrahedron.vertices, particle.rad);
//    createPolyhedronShape(shape, icosahedron.faces, icosahedron.vertices, particle.rad);

    return shape;
  }
  
  
  public void createPolyhedronShape(PShape shape, ArrayList<int[]> faces, ArrayList<float[]> vertices, float scale){
    shape.beginShape(PConstants.TRIANGLES);
    shape.noStroke();
    for(int[] face : faces){
      float[] v0 = vertices.get(face[0]);
      float[] v1 = vertices.get(face[1]);
      float[] v2 = vertices.get(face[2]);

      // flat shading
      float nx = (v0[0] + v1[0] + v2[0])/3f;
      float ny = (v0[1] + v1[1] + v2[1])/3f;
      float nz = (v0[2] + v1[2] + v2[2])/3f;
      shape.normal(nx, ny, nz); 
      
//      shape.normal(v0[0], v0[1], v0[2]); 
      shape.vertex(v0[0]*scale, v0[1]*scale, v0[2]*scale);
//      shape.normal(v1[0], v1[1], v1[2]); 
      shape.vertex(v1[0]*scale, v1[1]*scale, v1[2]*scale);
//      shape.normal(v2[0], v2[1], v2[2]); 
      shape.vertex(v2[0]*scale, v2[1]*scale, v2[2]*scale);
    }
    shape.endShape();
  }
  

  
  public void drawParticles(PGraphics pg){
    if(shp_particles != null){
      pg.shape(shp_particles);
    }
  }
  
  
  public boolean DISPLAY_SPRINGS_STRUCT = true;
  public boolean DISPLAY_SPRINGS_SHEAR  = true;
  public boolean DISPLAY_SPRINGS_BEND   = true;
  

  public void displaySprings(PGraphics pg, int display_mode){
    if(display_mode == -1) return;
    
    float r=0,g=0,b=0,strokeweight=1;
    float force, force_curr, force_relx;

    pg.beginShape(PConstants.LINES);
    for(int i = 0; i < particles.length; i++){
      VerletParticle3D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint3D spring = pa.springs[j];
        VerletParticle3D pb = spring.pb;
        if(!spring.is_the_good_one) continue;
        
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
  
  
  
 
  