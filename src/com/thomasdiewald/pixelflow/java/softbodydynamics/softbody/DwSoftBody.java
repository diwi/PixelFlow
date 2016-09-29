package com.thomasdiewald.pixelflow.java.softbodydynamics.softbody;


import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PShape;

public abstract class DwSoftBody{
  
 
  // general attributes
  public DwPhysics<? extends DwParticle> physics;
  
  // used for displaying
  public boolean DISPLAY_SPRINGS_STRUCT = true;
  public boolean DISPLAY_SPRINGS_SHEAR  = true;
  public boolean DISPLAY_SPRINGS_BEND   = true;
  
  // can be used for sub-classes
  public boolean CREATE_STRUCT_SPRINGS = true;
  public boolean CREATE_SHEAR_SPRINGS  = true;
  public boolean CREATE_BEND_SPRINGS   = true;
  
  public boolean self_collisions = false; // true, all particles get a different collision_group_id
  public int collision_group_id;       // particles that share the same id, are ignored during collision tests
  public int num_nodes;                // number of particles for this object
  public int nodes_offset;             // offset in the global array, used for creating a unique id
  public DwParticle[] particles; // particles of this body
  public PShape shp_particles;         // shape for drawing all particles of this body
  
  
  public DwParticle.Param         param_particle = new DwParticle.Param();
  public DwSpringConstraint.Param param_spring   = new DwSpringConstraint.Param();
  
  
  public DwSoftBody(){
  }
  
  
  
  public void setParam(DwParticle.Param param_particle){
    this.param_particle = param_particle;
  }
  public void setParam(DwSpringConstraint.Param param_spring){
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
  
  public abstract void createParticlesShape(PApplet papplet);

  
  public void displayParticles(PGraphics pg){
    if(shp_particles != null){
      pg.shape(shp_particles);
    }
  }
  

  public abstract void displaySprings(PGraphics pg, int display_mode);

  public abstract void displayMesh(PGraphics pg);


}
  
  
  
 
  