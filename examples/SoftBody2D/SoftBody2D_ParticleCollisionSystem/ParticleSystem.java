/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody2D.SoftBody2D_ParticleCollisionSystem;

import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;

public class ParticleSystem {
  
  
  // for customizing the particle we just extends the original class and
  // Override what we want to customize
  static class CustomVerletParticle2D extends DwParticle2D{
    public CustomVerletParticle2D(int idx) {
      super(idx);
    }
    
    @Override
    public void updateShapeColor(){
      setColor(0xFF020100);
//      super.updateShapeColor();
    }
    
  }
  
  
  
  // particle system
  public float PARTICLE_SCREEN_FILL_FACTOR = 0.9f;
  public int   PARTICLE_COUNT = 500;
  
  // particle behavior
  public float MULT_GRAVITY = 0.50f;
  
  DwParticle2D.Param particle_param = new DwParticle2D.Param();
  
  
  public PApplet papplet;
  
  public DwParticle2D[] particles;
  public PShape shp_particlesystem;
  
  public int size_x;
  public int size_y;
  
  public ParticleSystem(PApplet papplet, int size_x, int size_y){
    this.papplet = papplet;
    
    this.size_x = size_x;
    this.size_y = size_y;
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
    particles = new DwParticle2D[PARTICLE_COUNT];
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      particles[i] = new CustomVerletParticle2D(i);
      particles[i].setCollisionGroup(i);
      particles[i].setParamByRef(particle_param);
    }
    initParticlesSize();
    initParticlesPosition();
    initParticleShapes();
  }
  
  
  public void initParticlesSize(){

    float radius = (float)Math.sqrt((size_x * size_y * PARTICLE_SCREEN_FILL_FACTOR) / PARTICLE_COUNT) * 0.5f;
    radius = Math.max(radius, 1);
    float rand_range = 0.5f;
    float r_min = radius * (1.0f - rand_range);
    float r_max = radius * (1.0f + rand_range);
    
    DwParticle2D.MAX_RAD = r_max;
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
      float px = papplet.random(0, size_x - 1);
      float py = papplet.random(0, size_y - 1);
      particles[i].setPosition(px, py);
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
  
  
  // create the shape that is going to be rendered
  public PShape createParticleShape(DwParticle2D particle){
    
    final float rad = particle.rad;

    PShape shp_particle = papplet.createShape(PShape.GROUP);
    
    // compute circle resolution, depending on the radius we reduce/increase
    // the number of vertices we need to render
    float threshold1 = 1;   // radius shortening for arc segments
    float threshold2 = 140; // arc between segments
    
    double arc1 = Math.acos(Math.max((rad-threshold1), 0) / rad);
    double arc2 = (180 - threshold2) * Math.PI / 180;
    double arc = Math.min(arc1, arc2);
    
    int num_vtx = (int)Math.ceil(2*Math.PI/arc);
    
    // actual circle
    PShape circle = papplet.createShape(PShape.GEOMETRY);
    circle.beginShape();
    circle.noStroke();
    circle.fill(200,100);
    for(int i = 0; i < num_vtx; i++){
      float vx = (float) Math.cos(i * 2*Math.PI/num_vtx) * 1;
      float vy = (float) Math.sin(i * 2*Math.PI/num_vtx) * 1;
      circle.vertex(vx, vy);
    }
    circle.endShape(PConstants.CLOSE);

    // line, to indicate the velocity-direction of the particle
    PShape line = papplet.createShape(PShape.GEOMETRY);
    line.beginShape(PConstants.LINES);
    line.stroke(255, 100);
    line.strokeWeight(1f/rad);
    line.vertex(0, 0);
    line.vertex(-1, 0);
    line.endShape();
    
    shp_particle.addChild(circle);
    shp_particle.addChild(line);
    
    return shp_particle;
  }
  

  void display(PGraphics pg) {
    pg.shape(shp_particlesystem);
  }
  
  

}
