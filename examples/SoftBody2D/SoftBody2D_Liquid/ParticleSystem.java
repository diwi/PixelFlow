/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody2D.SoftBody2D_Liquid;

import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
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
      //setColor(0xFF020100);
//      super.updateShapeColor();
    }
    
  }
  
  
  
  // particle system
  public float PARTICLE_SCREEN_FILL_FACTOR = 0.9f;
  public int   PARTICLE_COUNT              = 500;
  
  // particle behavior
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
  
  
  int IDX_MOUSE_PARTICLE = 0;
  
  public void initParticlesSize(){

    float radius = (float)Math.sqrt((size_x * size_y * PARTICLE_SCREEN_FILL_FACTOR) / PARTICLE_COUNT) * 0.5f;
    float r_max = radius;
    
    IDX_MOUSE_PARTICLE = PARTICLE_COUNT-1;
    
    DwParticle2D.MAX_RAD = r_max * 5.5f;
    papplet.randomSeed(0);
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      float pr = radius;
      particles[i].setRadius(pr);
      particles[i].setMass(1);
    }
    particles[IDX_MOUSE_PARTICLE].setRadius(DwParticle2D.MAX_RAD);
    particles[IDX_MOUSE_PARTICLE].setMass(5);
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
    
    PImage sprite = createSprite(0);
    papplet.colorMode(PConstants.HSB, 360, 100, 100);
    
    for (int i = 0; i < PARTICLE_COUNT; i++) {
      PShape shp_particle = createParticleShape(particles[i], sprite);
      particles[i].setShape(shp_particle);
      if(i != IDX_MOUSE_PARTICLE){
        shp_particlesystem.addChild(shp_particle);
      }
    }
    papplet.colorMode(PConstants.RGB, 255, 255, 255);
  }
  
  public DwParticle2D getMouseParticle(){
    return particles[IDX_MOUSE_PARTICLE];
  }
  
  // create the shape that is going to be rendered
  public PShape createParticleShape(DwParticle2D particle, PImage pimg_sprite){
    
    final float rad = 2;

    PShape shp_sprite = papplet.createShape();
    shp_sprite.beginShape(PConstants.QUADS);
    shp_sprite.noStroke();
    shp_sprite.noFill();
    shp_sprite.tint(255,10,10);
    if(particle.idx == IDX_MOUSE_PARTICLE){
      shp_sprite.tint(200,100,100);
    } else {
      float r = 0 + papplet.random(-30, 30);
      float g = 100;
      float b = 100;
      shp_sprite.tint(r,g,b);
    }
    shp_sprite.textureMode(PConstants.NORMAL);
    shp_sprite.texture(pimg_sprite);
    shp_sprite.normal(0, 0, 1);
    shp_sprite.vertex(-rad, -rad, 0, 0);
    shp_sprite.vertex(+rad, -rad, 1, 0);
    shp_sprite.vertex(+rad, +rad, 1, 1);
    shp_sprite.vertex(-rad, +rad, 0, 1);
    shp_sprite.endShape();
    
    return shp_sprite;
  }
  
  
  
  

  
  
  
  
  // create sprite on the fly
  PImage createSprite(int PARTICLE_SHAPE_IDX){
    
    int size = (int)(DwParticle2D.MAX_RAD * 2f);
    size = Math.max(32, size);
    
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
          dd = dd*dd; dd = dd*dd; dd = dd*dd; dd = dd*dd; 
      
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
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  void display(PGraphics pg) {
    pg.shape(shp_particlesystem);
  }
  
  

}
