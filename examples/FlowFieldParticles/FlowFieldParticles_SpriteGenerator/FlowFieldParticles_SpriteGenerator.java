/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package FlowFieldParticles.FlowFieldParticles_SpriteGenerator;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;


public class FlowFieldParticles_SpriteGenerator extends PApplet {

  int gui_w = 200;
  int gui_x = 20;
  int gui_y = 20;
  
  int viewport_w = 512 + gui_w + 3*20;
  int viewport_h = 1024 + 2*20;
  int viewport_x = 230;
  int viewport_y = 0;
  
  PImage img_sprite;
  PGraphics pg_func;
  PGraphics pg_img;
  
  public int   size = 256;
  public float exp1 = 1f;
  public float exp2 = 1f;
  public float mult = 1f;
  public int   blur = 1;
  
  public void settings() {
    size(viewport_w, viewport_h);
    smooth(0);
  }

  public void setup(){
    updateSprite();
   
    createGUI();
    
    frameRate(1000);
  }
  
  
  public PImage resize(PImage img, int size){
    if(img == null){
      img = createImage(size, size, PConstants.ARGB);
    }
    if(img.width != size && img.height != size){
      img.resize(size, size);
    }
    return img;
  }
  
  public PGraphics resize(PGraphics pg, int size){
    if(pg == null || (pg.width != size && pg.height != size)){
      pg = createGraphics(size, size);
      pg.smooth(0);
    }
    return pg;
  }
  
  public void updateSprite(){
    img_sprite = createSprite(img_sprite, size, exp1, exp2, mult);
    img_sprite.loadPixels();
    
    pg_img  = resize(pg_img, size);
    pg_func = resize(pg_func, size);
    
    pg_img.beginDraw();
    pg_img.blendMode(REPLACE);
    pg_img.image(img_sprite, 0, 0);
    if(blur > 0){
      pg_img.filter(BLUR, blur);
    }
    pg_img.endDraw();
    pg_img.loadPixels();
    
    pg_func.beginDraw();
    pg_func.background(0);
    for(int i = 0; i < size; i++){
      int argb = pg_img.pixels[i + size*(size/2)];
      int a = (argb >> 24) & 0xFF;
      pg_func.noStroke();
      pg_func.fill(a);
      pg_func.rect(i, size, 1, -size * a/255f);
    }
    pg_func.endDraw();
  }
  
  
  static final public float clamp(float val, float lo, float hi){
    return (val < lo) ? lo : (val > hi) ? hi : val;
  }
  
  public PImage createSprite(PImage pimg, int size, float exp1, float exp2, float mult){
    pimg = resize(pimg, size);
    pimg.loadPixels();
    for(int y = 0; y < size; y++){
      for(int x = 0; x < size; x++){
        int pid = y * size + x;
        
        float xn = ((x + 0.5f) / (float)size) * 2f - 1f;
        float yn = ((y + 0.5f) / (float)size) * 2f - 1f;
        float dd = (float) Math.sqrt(xn*xn + yn*yn); // precompute!
        
        dd = clamp(dd, 0, 1);
        dd = (float) Math.pow(dd, exp1); // precompute!
        dd = 1.0f - dd;
        dd = (float) Math.pow(dd, exp2); // precompute!
        dd *= mult;
        dd = clamp(dd, 0, 1);
        pimg.pixels[pid] = ((int)(dd * 255)) << 24 | 0x00FFFFFF;
      }
    }
    pimg.updatePixels();
    return pimg;
  }
  
  

  public void draw(){
    background(64);
    
    int px = gui_x + gui_w + 20;
    int py = 20;
    int rw = 512;
    int rh = 512;
    pushMatrix();
    translate(px, py);
    noStroke();
    fill(0);
    rect(0, 0, rw, rh);
    image(pg_img, 0, 0, rw, rh);
    
    stroke(255,0,0, 128);
    strokeWeight(1f);
    line(rw/2, 0, rw/2, rh);
    line(0, rh/2, rw, rh/2);
    
    if(size < 32){
      stroke(64, 64);
      strokeWeight(1);
      for(int i = 0; i <= size; i++){
        float x = i * rw/(float)size;
        line(x, 0, x, rh);
        line(0, x, rw, x);
      }
    }
    
    
    translate(0, rh);
    noStroke();
    fill(0);
    rect(0, 0, rw, rh);
    image(pg_func, 0, 0, rw, rh);

    stroke(255,0,0, 128);
    strokeWeight(1f);
    line(rw/2, 0, rw/2, rh);
    line(0, rh/2, rw, rh/2);
    
    if(size < 32){
      stroke(64, 64);
      strokeWeight(1);
      for(int i = 0; i <= size; i++){
        float x = i * rw/(float)size;
        line(x, 0, x, rh);
        line(0, x, rw, x);
      }
    }
    
    popMatrix();
  }
    

  public void set_size(int val){
    size = val;
    updateSprite();
  }
  
  public void set_exp1(float val){
    exp1 = val;
    updateSprite();
  }
  public void set_exp2(float val){
    exp2 = val;
    updateSprite();
  }
  public void set_mult(float val){
    mult = val;
    updateSprite();
  }
  public void set_blur(int val){
    blur = val;
    updateSprite();
  }

  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    int sx, sy, px, py, oy;
    sx = 100; sy = 14; oy = (int)(sy*1.5f);

    ////////////////////////////////////////////////////////////////////////////
    // GUI - PARAMS
    ////////////////////////////////////////////////////////////////////////////
    Group group_sprite = cp5.addGroup("sprite");
    {
      group_sprite.setHeight(20).setSize(gui_w, height-60)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_sprite.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      px = 10;
      cp5.addSlider("size").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py+=oy)
      .setRange(1, 512).setValue(size).plugTo(this, "set_size");

      cp5.addSlider("exp1").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py+=oy)
      .setRange(0.01f, 4).setValue(exp1).plugTo(this, "set_exp1");
      
      cp5.addSlider("exp2").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py+=oy)
      .setRange(0.01f, 4).setValue(exp2).plugTo(this, "set_exp2");
      
      cp5.addSlider("mult").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py+=oy)
      .setRange(0.01f, 4).setValue(mult).plugTo(this, "set_mult");
      
      cp5.addSlider("blur").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py+=oy)
      .setRange(0, 20).setValue(blur).plugTo(this, "set_blur");
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_sprite)
      .open(0);
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { FlowFieldParticles_SpriteGenerator.class.getName() });
  }
  
  
}