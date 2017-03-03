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
//  public DwParticle[] particles; // particles of this body
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
  public int particle_color2 = 0xFF5C0000; // color(0, 92), default
  public int particle_gray  = 0xFF5C0000; // color(0, 92)
  public boolean use_particles_color = true;
  public float collision_radius_scale = 1.33333f;
  
  public int material_color = 0xFF555555;
  
  public void setParticleColor(float[] rgb){
    setParticleColor(toARGB(rgb));
  }
  
//  static public float darken_ = 0.6f;
  public void setParticleColor(int particle_color){
    this.particle_color = particle_color;
    
    int a = (particle_color >> 24) & 0xFF;
    int r = (particle_color >> 16) & 0xFF;
    int g = (particle_color >>  8) & 0xFF;
    int b = (particle_color >>  0) & 0xFF;
    float s = 0.8f;
    r = (int)Math.round(r*s);
    g = (int)Math.round(g*s);
    b = (int)Math.round(b*s);
    this.particle_color2 = a << 24 | r << 16 | g << 8 | b;
  }
  
  public void setMaterialColor(int material_color){
    this.material_color = material_color;
  }
  public void setMaterialColor(float[] rgb){
    this.material_color = toARGB(rgb);
  }
  
  private int toARGB(float[] rgb){
    int a = 255;
    int r = (int)Math.min(Math.max(rgb[0], 0), 255);
    int g = (int)Math.min(Math.max(rgb[1], 0), 255);
    int b = (int)Math.min(Math.max(rgb[2], 0), 255);
    return a << 24 | r << 16 | g << 8 | b;
  }
  
  public abstract void createParticlesShape(PApplet papplet, boolean icosahedron);
  public abstract void createParticlesShape(PApplet papplet);

  
  public void displayParticles(PGraphics pg){
    if(shp_particles != null){
      pg.shape(shp_particles);
    }
  }
  
//  public void setParticlesVisibility(boolean visible){
////    System.out.println(particles);
////    for(DwParticle particle : particles){
////      particle.updateShape(visible);
////    }
//  }


  public abstract void displaySprings(PGraphics pg, int display_mode);

  public abstract void displayMesh(PGraphics pg);
  public abstract void displayWireFrame(PGraphics pg, float strokeWeight);

}
  
  
  
 
  