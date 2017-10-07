/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Miscellaneous.ImageProcessing_HarrisCornerDetection;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwHarrisCorner;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import controlP5.ControlP5;
import controlP5.Group;
import controlP5.Slider;
import processing.core.*;
import processing.opengl.PGraphics2D;


public class ImageProcessing_HarrisCornerDetection extends PApplet {
  //
  // example, to show how harris corner detection is used
  //
  
  
  // two draw-buffers for swaping
  PGraphics2D pg_src_A;
  PGraphics2D pg_src_B;
  PGraphics2D pg_src_C; // just another buffer for temporary results
  
  // library
  DwPixelFlow context;
  
  // Harris Corner Detection
  DwHarrisCorner harris;
  
  // display states
  public boolean DISPLAY_IMAGE    = true;
  public boolean DISPLAY_GEOMETRY = true;
  public boolean DISPLAY_PEPPER   = true;
  public boolean DISPLAY_CHECKER  = true;
  
  // display everything in grayscale, harris is red
  public boolean DISPLAY_GRAYSCALE = true;
  
  
  // background image
  PImage img;
  
  // checkerboard pattern
  PGraphics2D checker;
 
  int view_w; 
  int view_h;
  int gui_w = 200;
  
  public void settings() {
    img = loadImage("../data/mc_escher.jpg");
    
    view_w = img.width;
    view_h = img.height;
    size(view_w+gui_w, view_h, P2D);
    smooth(0);
  }

  public void setup() {

    // main linbrary context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    
    // Harris Corner detection
    harris = new DwHarrisCorner(context);
    
    // draw buffers
    pg_src_A = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg_src_A.smooth(0);
    
    pg_src_B = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg_src_B.smooth(0);
    
    pg_src_C = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg_src_C.smooth(0);
    
    // checkerboard pattern
    checker = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    checker.smooth(8);
    checker.loadPixels();
    int POT = 6;
    for(int y = 0; y < checker.height; y++){
      for(int x = 0; x < checker.width; x++){
        int idx = y * checker.width + x;
        checker.pixels[idx] = 0xFF000000 | (((x^y)>>POT)&1)-1;
      }
    }
    checker.updatePixels();
      
  
    createGUI();
    
    frameRate(60);
//    frameRate(1000);
  }
  
  

  
  

  // animated rectangle data
  float rs = 80;
  float rx = 600;
  float ry = 600;
  float dx = 0.6f;
  float dy = 0.25f;
  
  float rotateA = 0;
  float rotate_checker = 0;
  
  public void draw() {
    
    int w = view_w;
    int h = view_h;
    
    // update rectangle position
    rx += dx;
    ry += dy;
    // keep inside viewport
    if(rx <   rs/2) {rx =   rs/2; dx = -dx; }
    if(rx > w-rs/2) {rx = w-rs/2; dx = -dx; }
    if(ry <   rs/2) {ry =   rs/2; dy = -dy; }
    if(ry > h-rs/2) {ry = h-rs/2; dy = -dy; }
    
    
    // update input image
    pg_src_A.beginDraw();
    {
      pg_src_A.rectMode(CENTER);
      pg_src_A.clear();
      pg_src_A.background(255);
      
      if(DISPLAY_IMAGE){
        pg_src_A.image(img, 0, 0);
      }
      
      if(DISPLAY_GEOMETRY){
        pg_src_A.strokeWeight(1);
        pg_src_A.stroke(0);
        pg_src_A.line(w/2, 0, w/2, h);
        pg_src_A.line(0, h/2, w, h/2);
        pg_src_A.line(0, 0, w, h);
        pg_src_A.line(w, 0, 0, h);
        
        pg_src_A.strokeWeight(1);
        pg_src_A.stroke(0);
        pg_src_A.noFill();
        pg_src_A.ellipse(w/2, h/2, 150, 150);
        
        pg_src_A.strokeWeight(1);
        pg_src_A.stroke(0);
        pg_src_A.noFill();
        pg_src_A.rect(w/2, h/2, 300, 300);
        
        if(DISPLAY_PEPPER){
          int PEPPER = 1000;
          randomSeed(1);
          for(int i = 0; i < PEPPER; i++){
            float px = ((int) random(20, w-20));
            float py = ((int) random(20, h-20));
            
            pg_src_A.noStroke();
            pg_src_A.fill(0);
            pg_src_A.rect(px, py, 1, 1);
          }
        }
      }
      
      if(DISPLAY_CHECKER){
        pg_src_A.pushMatrix();
        pg_src_A.translate(view_w/2, view_h/2);
        
        float wh = width /2f;
        float hh = height/2f;
        float scalex = 0.01f + (mouseX - wh) / wh;
        float scaley = 0.01f + (mouseY - hh) / hh;
        pg_src_A.scale(scalex, scaley);
        pg_src_A.rotate(rotate_checker+=0.001f);
//        pg_src_A.rotate(mouseX * 0.001f);
        float cw =  pg_src_A.width * 2;
        float ch =  checker.height * 2;
        pg_src_A.image(checker, -cw/2, -ch/2, cw, ch);
        pg_src_A.popMatrix();
      }
      // moving rectangle
      pg_src_A.fill(100, 175, 255);
      pg_src_A.rect(rx, ry, rs, rs);
      
      float dx = (mouseX - pmouseX);
      float dy = (mouseY - pmouseY);
      float dd = sqrt(dx*dx + dy*dy);

      rotateA += dd * 0.01f * Math.signum(dx);
      
      // mouse-driven ellipse
      pg_src_A.pushMatrix();
      pg_src_A.translate(mouseX, mouseY);
      pg_src_A.rotate(rotateA);
      pg_src_A.fill(255, 150, 0);
      pg_src_A.noStroke();
      pg_src_A.rect(0, 0, 100, 100);
      pg_src_A.popMatrix();
    }
    pg_src_A.endDraw();
    
    

    // apply harris corner
    harris.update(pg_src_A);


    // grayscale, for better contrast
    if(DISPLAY_GRAYSCALE){
      DwFilter.get(context).luminance.apply(pg_src_A, pg_src_A);
    }
    
    // render harris corners
    harris.render(pg_src_A);

    // display result
    background(0);
    image(pg_src_A, 0, 0);

    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", pg_src_A.width, pg_src_A.height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  void swapAB(){
    PGraphics2D tmp = pg_src_A;
    pg_src_A = pg_src_B;
    pg_src_B = tmp;
  }
  
  
 
  
  
  

  
  ControlP5 cp5;
  Slider cp5_slider_sigma;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    Group group_harris = cp5.addGroup("HarrisCornerDetection")
    .setPosition(view_w, 20).setHeight(20).setWidth(gui_w)
    .setBackgroundHeight(view_h).setBackgroundColor(color(16, 220)).setColorBackground(color(16, 180));
    group_harris.getCaptionLabel().align(LEFT, CENTER);
    
    int sx = 100, sy = 14;
    
//    cp5.setAutoSpacing(10, 50);
    
    cp5.addSlider("blur input").setGroup(group_harris).setSize(sx, sy)
    .setRange(0, 3).setValue(harris.param.blur_input)
    .plugTo(harris.param, "blur_input").linebreak();
    
    cp5.addSlider("blur harris").setGroup(group_harris).setSize(sx, sy)
    .setRange(1, 20).setValue(harris.param.blur_harris)
    .plugTo(harris.param, "blur_harris").linebreak();
    
    cp5.addSlider("blur final").setGroup(group_harris).setSize(sx, sy)
    .setRange(0, 20).setValue(harris.param.blur_final)
    .plugTo(harris.param, "blur_final").linebreak();
    
    cp5.addSlider("scale").setGroup(group_harris).setSize(sx, sy)
    .setRange(1f, 1000f).setValue(harris.param.scale)
    .plugTo(harris.param, "scale").linebreak();
    
    cp5.addSlider("sensitivity").setGroup(group_harris).setSize(sx, sy)
    .setRange(0.01f,0.20f).setValue(harris.param.sensitivity).setDecimalPrecision(4)
    .plugTo(harris.param, "sensitivity").linebreak();
    
    cp5.addToggle("nonMaxSuppression").setGroup(group_harris).setSize(sx, sy)
    .plugTo(harris.param, "nonMaxSuppression").setValue(harris.param.nonMaxSuppression).linebreak();
    

    cp5.addCheckBox("displayContent").setGroup(group_harris).setSize(18, 18).setPosition(10, 180)
    .setItemsPerRow(1).setSpacingColumn(2).setSpacingRow(2)
    .addItem("display image"   , 0)
    .addItem("display geometry", 1)
    .addItem("display pepper"  , 2)
    .addItem("display checker" , 3)
    .activate(0)
    .activate(1)
    .activate(2)
    .activate(3)
    ;
    
    cp5.addToggle("display grayscale").setGroup(group_harris).setSize(sx, sy).setPosition(10, 300)
    .plugTo(this, "DISPLAY_GRAYSCALE").setValue(DISPLAY_GRAYSCALE).linebreak();
    
    group_harris.open();
  }
  

  public void displayContent(float[] val){
    DISPLAY_IMAGE    = val[0] > 0.0;
    DISPLAY_GEOMETRY = val[1] > 0.0;
    DISPLAY_PEPPER   = val[2] > 0.0;
    DISPLAY_CHECKER  = val[3] > 0.0;
  }
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { ImageProcessing_HarrisCornerDetection.class.getName() });
  }
}