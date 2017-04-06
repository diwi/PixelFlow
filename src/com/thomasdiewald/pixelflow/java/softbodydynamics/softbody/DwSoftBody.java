package com.thomasdiewald.pixelflow.java.softbodydynamics.softbody;


import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint.TYPE;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

public abstract class DwSoftBody{
  
 
  // general attributes
  public DwPhysics<? extends DwParticle> physics;
  
  // can be used for sub-classes
  public boolean CREATE_STRUCT_SPRINGS = true;
  public boolean CREATE_SHEAR_SPRINGS  = true;
  public boolean CREATE_BEND_SPRINGS   = true;
  
  public boolean self_collisions = false; // true, all particles get a different collision_group_id
  public int collision_group_id;       // particles that share the same id, are ignored during collision tests
  public int num_nodes;                // number of particles for this object
  public int nodes_offset;             // offset in the global array, used for creating a unique id
  
//  public DwParticle[] particles; // particles of this body
  
  public PShape shp_particles;         // shape for drawing the particles
  public PShape shp_mesh;              // shape for drawing the mesh
  public PShape shp_wireframe;         // shape for drawing the edges
  
  public DwParticle.Param         param_particle = new DwParticle.Param();
  public DwSpringConstraint.Param param_spring   = new DwSpringConstraint.Param();
  
  
  private void removeChilds(PShape shp){
    if(shp == null){
      return;
    }
    int num_childs = shp.getChildCount();
    for(int i = num_childs-1; i >= 0; i--){
      removeChilds(shp.getChild(i));
      shp.removeChild(i);
    }
  }
  
  protected void printChilds(PShape shp, String indent){
    if(shp == null){
      return;
    }
    int num_childs = shp.getChildCount();
    
    System.out.println(indent+shp.getName()+": "+num_childs);
    for(int i = num_childs-1; i >= 0; i--){
      printChilds(shp.getChild(i), indent+"  ");
    }
  }
  
  protected void setShapeParticles(PApplet pg, PShape child){
    if(shp_particles == null){
      shp_particles = pg.createShape(PConstants.GROUP);
      shp_particles.setName("shp_particles (root)");
    } else {
      removeChilds(shp_particles);
    }
    child.setName("shp_particles");
    shp_particles.addChild(child);
  }
  
  
  protected void setShapeMesh(PApplet pg, PShape child){
    if(shp_mesh == null){
      shp_mesh = pg.createShape(PConstants.GROUP);
      shp_mesh.setName("shp_mesh (root)");
    } else {
      removeChilds(shp_mesh);
    }
    child.setName("shp_mesh");
    shp_mesh.addChild(child);
  }
  
  
  protected void setShapeWireframe(PApplet pg, PShape child){
    if(shp_wireframe == null){
      shp_wireframe = pg.createShape(PConstants.GROUP);
      shp_wireframe.setName("shp_wireframe (root)");
    } else {
      removeChilds(shp_wireframe);
    }
    child.setName("shp_wireframe");
    shp_wireframe.addChild(child);
  }
  

  
  
  
  
  
  public int particle_color = 0xFF5C0000; // color(0, 92), default
  public int particle_color2 = 0xFF5C0000; // color(0, 92), default
  public int particle_gray  = 0xFF5C0000; // color(0, 92)
  public boolean use_particles_color = true;
  public float collision_radius_scale = 1.33333f;
  
  public int material_color = 0xFF555555;
  
  public boolean shade_springs_by_tension = false;
  
  
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

  
  

  
  public final void displayParticles(PGraphics pg){
    if(shp_particles != null){
      pg.shape(shp_particles);
    }
  }
  
  public final void displayMesh(PGraphics pg){
    if(shp_mesh != null){
      pg.shape(shp_mesh);
    }
  }
  
  public final void displayWireframe(PGraphics pg){
    if(shp_wireframe != null){
      pg.shape(shp_wireframe);
    }
  }
  
  public abstract void displaySprings(PGraphics pg, DwStrokeStyle style, TYPE type);

  
  
  

  public abstract void createShapeParticles(PApplet papplet, boolean icosahedron);
  public abstract void createShapeParticles(PApplet papplet);
  public abstract void createShapeMesh(PGraphics pg);
  public abstract void createShapeWireframe(PGraphics pg, DwStrokeStyle style);
  
  
  
//  StrokeStyle   style_stroke    = new StrokeStyle();
//  MeshStyle     style_mesh      = new MeshStyle();
//  ParticleStyle style_particles = new ParticleStyle();
//  
//  public void setStyle(MeshStyle style){
//    this.style_mesh = style;
//  }
//  public void setStyle(StrokeStyle style){
//    this.style_stroke = style;
//  }
//  public void setStyle(ParticleStyle style){
//    this.style_particles = style;
//  }
//  
  
  

  
//  static public class MeshStyle{
//    public int fill_color = 0xFF000000;
//    PGraphics texture = null;
//    public MeshStyle(){}
//  }
//  
//  static public class ParticleStyle{
//    public int fill_color_A = 0xFF000000;
//    public int fill_color_B = 0xFF000000;
//    public ParticleStyle(){}
//  }
  
  
  
  final protected int toARGB(float[] rgb){
    return toARGB(rgb[0], rgb[1], rgb[2]);
  }
  
  final protected int toARGB(float r_, float g_, float b_){
    int r = (int)Math.min(Math.max(r_, 0), 255);
    int g = (int)Math.min(Math.max(g_, 0), 255);
    int b = (int)Math.min(Math.max(b_, 0), 255);
    return 0xFF000000 | r<<16 | g<<8 | b;
  }

}
  
  
  
 
  