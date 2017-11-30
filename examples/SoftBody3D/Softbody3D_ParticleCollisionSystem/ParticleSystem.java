/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody3D.Softbody3D_ParticleCollisionSystem;


import com.thomasdiewald.pixelflow.java.geometry.DwIcosahedron;
import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSetAble;
import com.thomasdiewald.pixelflow.java.geometry.DwMeshUtils;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

public class ParticleSystem {
  
  
  // for customizing the particle we just extends the original class and
  // Override what we want to customize
  static class CustomVerletParticle3D extends DwParticle3D{
    public CustomVerletParticle3D(int idx) {
      super(idx);
    }
    
    @Override
    public void updateShapeColor(){
//      setColor(0xFF020100);
      super.updateShapeColor();
    }
    
  }
  
  
  
  // particle system
  public float PARTICLE_SCREEN_FILL_FACTOR = 0.5f;
  public int   PARTICLE_COUNT              = 500;
  

  DwParticle3D.Param particle_param = new DwParticle3D.Param();
  
  
  public PApplet papplet;
  
  public DwParticle3D[] particles;
  public PShape shp_particlesystem;
  
  public float[] bounds;
  public int size_y;
  public int size_z;
  
  public ParticleSystem(PApplet papplet, float[] bounds){
    this.papplet = papplet;
    this.bounds = bounds;
  }

  
  public void setParticleCount(int count){
    if( count == PARTICLE_COUNT && particles != null &&  particles.length == PARTICLE_COUNT){
      return;
    }
    PARTICLE_COUNT = count;
    initParticles();
  }
  
  public void setFillFactor(float screen_fill_factor){
    if(screen_fill_factor == PARTICLE_SCREEN_FILL_FACTOR){
      return;
    }
    PARTICLE_SCREEN_FILL_FACTOR = screen_fill_factor;
    initParticlesSize();
    initParticleShapes();
  }
  

  
  public void initParticles(){
    particles = new DwParticle3D[PARTICLE_COUNT];
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      particles[i] = new CustomVerletParticle3D(i);
      particles[i].setCollisionGroup(i);
      particles[i].setParamByRef(particle_param);
    }
    initParticlesSize();
    initParticlesPosition();
    initParticleShapes();
  }
  
  
  public void initParticlesSize(){
    
    float bsx = bounds[3] - bounds[0];
    float bsy = bounds[4] - bounds[1];
    float bsz = bounds[5] - bounds[2];
    
    float volume = bsx * bsy * bsz * PARTICLE_SCREEN_FILL_FACTOR;
    
    float volume_per_particle = volume / PARTICLE_COUNT;
    float radius = (float) (Math.pow(volume_per_particle, 1/3.0) * 0.5);
    
    radius = Math.max(radius, 1);
    float rand_range = 0.4f;
    float r_min = radius * (1.0f - rand_range);
    float r_max = radius * (1.0f + rand_range);
    
    DwParticle3D.MAX_RAD = r_max;
    papplet.randomSeed(0);
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      float pr = papplet.random(r_min, r_max);
      particles[i].setRadius(pr);
      particles[i].setMass(r_max*r_max/(pr*pr) );
    }
    particles[0].setRadius(r_max*1.5f);
  }
  
  public void initParticlesPosition(){
    papplet.randomSeed(0);
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      float px = papplet.random(bounds[0]+DwParticle3D.MAX_RAD, bounds[3]-DwParticle3D.MAX_RAD);
      float py = papplet.random(bounds[1]+DwParticle3D.MAX_RAD, bounds[4]-DwParticle3D.MAX_RAD);
      float pz = papplet.random(bounds[2]+DwParticle3D.MAX_RAD, bounds[5]-DwParticle3D.MAX_RAD);
      particles[i].setPosition(px, py, pz);
    }
  }
  
  public void initParticleShapes(){
    papplet.shapeMode(PConstants.CORNER);
    shp_particlesystem = papplet.createShape(PShape.GROUP);
    
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      PShape shp_particle = createParticleShape(particles[i]);
      particles[i].setShape(shp_particle);
      shp_particlesystem.addChild(shp_particle);
    }

  }
  
  DwIndexedFaceSetAble ifs;
  
  // create the shape that is going to be rendered
  public PShape createParticleShape(DwParticle3D particle){
   
    PShape shp_particle = papplet.createShape(PShape.GEOMETRY);
    
    shp_particle.resetMatrix();
    shp_particle.translate(particle.cx, particle.cy, particle.cz);
    shp_particle.rotateX(papplet.random(PConstants.TWO_PI));
    shp_particle.rotateY(papplet.random(PConstants.TWO_PI));
    shp_particle.rotateZ(papplet.random(PConstants.TWO_PI));
    shp_particle.setStroke(false);
//    shp_particle.setFill(papplet.color(160));
    
    if(ifs == null) ifs = new DwIcosahedron(1);
//    if(ifs == null) ifs = new DwCube(1);
    DwMeshUtils.createPolyhedronShape(shp_particle, ifs, 1, 3, true);

    return shp_particle;
  }
  

  void display(PGraphics pg) {
    pg.shape(shp_particlesystem);
  }
  
  

}
