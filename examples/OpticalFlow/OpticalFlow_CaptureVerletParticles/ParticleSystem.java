/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package OpticalFlow.OpticalFlow_CaptureVerletParticles;

import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;

public class ParticleSystem {
  
  // particle system
  public float PARTICLE_SCREEN_FILL_FACTOR = 0.9f;
  public int   PARTICLE_COUNT              = 500;
  public int   PARTICLE_SHAPE_IDX          = 1;
  
  // particle behavior
  public float MULT_FLUID    = 0.50f;
  public float MULT_GRAVITY  = 0.50f;
  
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
  
  public void setParticleShape(int val){
    PARTICLE_SHAPE_IDX = val;
    if( PARTICLE_SHAPE_IDX != -1){
      initParticleShapes();
    }
  }
  
  public void initParticles(){
    particles = new DwParticle2D[PARTICLE_COUNT];
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      particles[i] = new DwParticle2D(i);
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
    
    PImage sprite = createSprite();
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      PShape shp_particle = createParticleShape(particles[i], sprite);
      particles[i].setShape(shp_particle);
      shp_particlesystem.addChild(shp_particle);
    }
  }
  
  
  // just some shape presets
  public PShape createParticleShape(DwParticle2D particle, PImage sprite_img){
    
    final float rad = particle.rad;

    PShape shp_particle = papplet.createShape(PShape.GROUP);
    
    if( PARTICLE_SHAPE_IDX >= 0 && PARTICLE_SHAPE_IDX < 4){
      
      PShape sprite = papplet.createShape(PShape.GEOMETRY);
      sprite.beginShape(PConstants.QUAD);
      sprite.noStroke();
      sprite.noFill();
      sprite.textureMode(PConstants.NORMAL);
      sprite.texture(sprite_img);
      sprite.normal(0, 0, 1);
      sprite.vertex(-rad, -rad, 0, 0);
      sprite.vertex(+rad, -rad, 1, 0);
      sprite.vertex(+rad, +rad, 1, 1);
      sprite.vertex(-rad, +rad, 0, 1);
      sprite.endShape();
      
      shp_particle.addChild(sprite);
    }
    else if( PARTICLE_SHAPE_IDX == 4){   
      
      float threshold1 = 1;   // radius shortening for arc segments
      float threshold2 = 140; // arc between segments
      
      double arc1 = Math.acos(Math.max((rad-threshold1), 0) / rad);
      double arc2 = (180 - threshold2) * Math.PI / 180;
      double arc = Math.min(arc1, arc2);
      
      int num_vtx = (int)Math.ceil(2*Math.PI/arc);
      
//      System.out.println(num_vtx);

      PShape circle = papplet.createShape(PShape.GEOMETRY);
      circle.beginShape();
      circle.noStroke();
      circle.fill(200,100);
      for(int i = 0; i < num_vtx; i++){
        float vx = (float) Math.cos(i * 2*Math.PI/num_vtx) * rad;
        float vy = (float) Math.sin(i * 2*Math.PI/num_vtx) * rad;
        circle.vertex(vx, vy);
      }
      circle.endShape(PConstants.CLOSE);

      PShape line = papplet.createShape(PShape.GEOMETRY);
      line.beginShape(PConstants.LINES);
      line.stroke(0, 100);
      line.strokeWeight(1);
      line.vertex(0, 0);
      line.vertex(-(rad-1), 0);
      line.endShape();
      
//      PShape circle = papplet.createShape(PConstants.ELLIPSE, 0, 0, rad*2, rad*2);
//      circle.setStroke(false);
//      circle.setFill(papplet.color(200,100));
//
//      PShape line = papplet.createShape(PConstants.LINE, 0, 0, -(rad-1), 0);
//      line.setStroke(papplet.color(0,200));
//      line.setStrokeWeight(1); 
      
      shp_particle.addChild(circle);
      shp_particle.addChild(line);
    }
    
    return shp_particle;
    
  }
  
  
  // create sprite on the fly
  PImage createSprite(){
    
    int size = (int)(DwParticle2D.MAX_RAD * 1.5f);
    size = Math.max(9, size);
    
    PImage pimg = papplet.createImage(size, size, PConstants.ARGB);
    pimg.loadPixels();
    
    float center_x = size/2f;
    float center_y = size/2f;
    
    for(int y = 0; y < size; y++){
      for(int x = 0; x < size; x++){
        int pid = y * size + x;
        
        float dx = center_x - (x+0.5f);
        float dy = center_y - (y+0.5f);
        float dd = (float)Math.sqrt(dx*dx + dy*dy) * 1f;
   
        dd = dd/(size*0.5f); // normalize
        
        // DISC
        if(PARTICLE_SHAPE_IDX == 0){
          if(dd<0) dd=0; else if(dd>1) dd=1;
          dd = dd*dd; dd = dd*dd; dd = dd*dd;
      
          dd = 1-dd;
          int a = (int)(dd*255);
          pimg.pixels[pid] = a << 24 | 0x00FFFFFF;
        }
        // SPOT
        else if(PARTICLE_SHAPE_IDX == 1){
          if(dd<0) dd=0; else if(dd>1) dd=1;
          dd = 1-dd;
//          dd = dd*dd;
          int a = (int)(dd*255);
          pimg.pixels[pid] = a << 24 | 0x00FFFFFF;
        }
        // DONUT
        else if(PARTICLE_SHAPE_IDX == 2){
          dd = Math.abs(0.6f - dd);
          dd *= 1.8f;
          dd = 1-dd;
          dd = dd*dd*dd;
          if(dd<0) dd=0; else if(dd>1) dd=1;
          int a = (int)(dd*255);
          pimg.pixels[pid] = a << 24 | 0x00FFFFFF;
        }
        // RECT
        else if(PARTICLE_SHAPE_IDX == 3){
          int a = 255;
          if(Math.abs(dx) < size/3f && Math.abs(dy) < size/3f) a = 0;
          pimg.pixels[pid] = a << 24 | 0x00FFFFFF;
        } else {
          pimg.pixels[pid] = 0;
        }
        
      }
    }
    pimg.updatePixels();
 
    return pimg;
  }
  
  
  

  // not sure if this is necessary, but i guess opengl stuff needs to be released internally.
  public void clearShapes(){
    if(shp_particlesystem != null){
      for(int i = shp_particlesystem.getChildCount()-1; i >= 0; i--){
        shp_particlesystem.removeChild(i);
      }
    }
  }
  

  
  void display(PGraphics pg) {
    if(PARTICLE_SHAPE_IDX != -1){
      pg.shape(shp_particlesystem);
    }
  }
  
  

}