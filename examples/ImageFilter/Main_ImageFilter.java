/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package ImageFilter;



import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.filter.Filter;
import com.thomasdiewald.pixelflow.java.filter.Laplace;
import com.thomasdiewald.pixelflow.java.filter.MedianFilter;
import com.thomasdiewald.pixelflow.java.filter.SobelFilter;

import controlP5.ControlP5;
import controlP5.Group;
import controlP5.Slider;
import processing.core.*;
import processing.opengl.PGraphics2D;


public class Main_ImageFilter extends PApplet {
 
  // two draw-buffers for swaping
  PGraphics2D pg_src_A;
  PGraphics2D pg_src_B;
  PGraphics2D pg_src_C; // just another buffer for temporary results
  
  // filters
  Filter filter;


  int CONVOLUTION_KERNEL_INDEX = 0;
  
  // custom convolution kernel
  // https://en.wikipedia.org/wiki/Kernel_(image_processing)
  float[][] kernel = 
    {  
      { // lowpass: box-blur
        1/9f, 1/9f, 1/9f,
        1/9f, 1/9f, 1/9f,
        1/9f, 1/9f, 1/9f,   
      },
      { // lowpass: gauss-blur
        1/16f, 2/16f, 1/16f,
        2/16f, 4/16f, 2/16f,
        1/16f, 2/16f, 1/16f,   
      },
      { // sharpen highpass: laplace
        0,-1, 0,
       -1, 5,-1,
        0,-1, 0 
      },
      { // sharpen highpass: laplace
       -1,-1,-1,
       -1, 9,-1,
       -1,-1,-1, 
      },
      { // sharpen highpass: laplace
       -1,-2,-1,
       -2,13,-2,
       -1,-2,-1,   
      },
      { // edges 1
       +0,+1,+0,
       +1,-4,+1,
       +0,+1,+0 
      },
      { // edges 2
        +1,+1,+1,
        +1,-8,+1,
        +1,+1,+1 
       },
      { // gradient: sobel horizontal
        +1, 0,-1,
        +2, 0,-2,
        +1, 0,-1,   
      },
      { // gradient: sobel vertical
        +1,+2,+1,
         0, 0, 0,
        -1,-2,-1, 
      },
      { // gradient: sobel diagonal TL-BR
        +2, 1 ,0,
        +1, 0,-1,
        +0,-1,-2,   
      },
      {  // gradient: sobel diagonal TR-BL
         0,-1,-2,
        +1, 0,-1,
        +2,+1, 0,   
      },
      {  // emboss / structure / relief
        0,-1,-2,
       +1, 1,-1,
       +2,+1, 0,   
     },
  };
  
  
  
  // display states
  public boolean DISPLAY_IMAGE    = true;
  public boolean DISPLAY_GEOMETRY = true;
  
  // filter, currently active
  public int     DISPLAY_FILTER = 0;
  
  // how often the active filter gets applied
  public int     FILTER_STACKS = 1;
  
  // boxblur/gaussianblur
  public int     BLUR_RADIUS = 20;
  public float   GAUSSBLUR_SIGMA  = BLUR_RADIUS / 2.0f;
  public boolean GAUSSBLUR_AUTO_SIGMA = true;
  
  // bilateral filter
  public int     BILATERAL_RADIUS      = 5;
  public float   BILATERAL_SIGMA_COLOR = 0.3f;
  public float   BILATERAL_SIGMA_SPACE = 5;
  
  // laplace filter
  public int     LAPLACE_WEIGHT         = 1; // 0, 1, 2
  
  // background image
  PImage img;
  
  
  int view_w; 
  int view_h;
  int gui_w = 200;
  
  public void settings() {
    img = loadImage("mc_escher.jpg");
    
    view_w = img.width;
    view_h = img.height;
    size(view_w+gui_w, view_h, P2D);
    smooth(0);
  }

  public void setup() {

    PixelFlow context = new PixelFlow(this);
    context.print();
    context.printGL();
    
    filter = new Filter(context);

    pg_src_A = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg_src_A.smooth(0);
    
    pg_src_B = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg_src_B.smooth(0);
    
    pg_src_C = (PGraphics2D) createGraphics(view_w, view_h, P2D);
    pg_src_C.smooth(0);

      
    createGUI();
    
//    frameRate(60);
    frameRate(1000);
  }
  
  

  
  

  // animated rectangle data
  float rs = 80;
  float rx = 600;
  float ry = 600;
  float dx = 0.6f;
  float dy = 0.25f;
  
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
      
      // moving rectangle
      pg_src_A.fill(100, 175, 255);
      pg_src_A.rect(rx, ry, rs, rs);
      
      // mouse-driven ellipse
      pg_src_A.fill(255, 150, 0);
      pg_src_A.noStroke();
      pg_src_A.ellipse(mouseX, mouseY, 100, 100);
    }
    pg_src_A.endDraw();
    
    
    
    
    
   
    if(GAUSSBLUR_AUTO_SIGMA){
      GAUSSBLUR_SIGMA = BLUR_RADIUS/2f;
      cp5_slider_sigma.setValue(GAUSSBLUR_SIGMA);
    }
    
    
    // APPLY FILTERS
    if( DISPLAY_FILTER == 0) { 
      filter.luminance.apply(pg_src_A, pg_src_B); swapAB(); 
    }
    if( DISPLAY_FILTER == 1) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.boxblur.apply(pg_src_A, pg_src_A, pg_src_B, BLUR_RADIUS);
      }
    }
    if( DISPLAY_FILTER == 2) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.gaussblur.apply(pg_src_A, pg_src_A, pg_src_B, BLUR_RADIUS, GAUSSBLUR_SIGMA);
      }
    }
    if( DISPLAY_FILTER == 3) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.median.apply(pg_src_A, pg_src_B, MedianFilter.TYPE._3x3_); swapAB(); 
      }
    }
    if( DISPLAY_FILTER == 4) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.median.apply(pg_src_A, pg_src_B, MedianFilter.TYPE._5x5_); swapAB(); 
      }
    }
    if( DISPLAY_FILTER == 5) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.sobel.apply(pg_src_A, pg_src_B, SobelFilter.DIR.HORZ_3x3); swapAB(); 
//        filter.sobel.apply(pg_src_A, pg_src_B, SobelFilter.DIR.TLBR_3x3); swapAB(); 
      }
    }
    if( DISPLAY_FILTER == 6) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.sobel.apply(pg_src_A, pg_src_B, SobelFilter.DIR.VERT_3x3); swapAB(); 
//        filter.sobel.apply(pg_src_A, pg_src_B, SobelFilter.DIR.BRTL_3x3); swapAB(); 
      }
    }
    if( DISPLAY_FILTER == 7) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.laplace.apply(pg_src_A, pg_src_B, Laplace.TYPE.values()[LAPLACE_WEIGHT]); swapAB(); 
      }
    }
    if( DISPLAY_FILTER == 8) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.convolution.apply(pg_src_A, pg_src_B, kernel[CONVOLUTION_KERNEL_INDEX]); swapAB(); 
      }
    }
    if( DISPLAY_FILTER == 9) { 
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.bilateral.apply(pg_src_A, pg_src_B, BILATERAL_RADIUS, BILATERAL_SIGMA_COLOR, BILATERAL_SIGMA_SPACE); swapAB(); 
      }
    }
    if( DISPLAY_FILTER == 10) {
      filter.median.apply(pg_src_A, pg_src_B, MedianFilter.TYPE._3x3_); swapAB();
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.gaussblur.apply(pg_src_A, pg_src_A, pg_src_B, BLUR_RADIUS, GAUSSBLUR_SIGMA);
      }
      filter.sobel.apply(pg_src_A, pg_src_B, SobelFilter.DIR.HORZ_3x3); swapAB();
    }
    if( DISPLAY_FILTER == 11) {
      filter.median.apply(pg_src_A, pg_src_B, MedianFilter.TYPE._3x3_); swapAB();
      for(int i = 0; i < FILTER_STACKS; i++){
        filter.gaussblur.apply(pg_src_A, pg_src_A, pg_src_B, BLUR_RADIUS, GAUSSBLUR_SIGMA);
      }
      filter.laplace.apply(pg_src_A, pg_src_B, Laplace.TYPE.values()[LAPLACE_WEIGHT]); swapAB();
    }
    if( DISPLAY_FILTER == 12) {
      filter.gaussblur.apply(pg_src_A, pg_src_C, pg_src_B, (int)(BLUR_RADIUS*2f));
      filter.gaussblur.apply(pg_src_A, pg_src_A, pg_src_B, BLUR_RADIUS);
      filter.dog.apply(pg_src_A, pg_src_C, pg_src_A, new float[]{+2,-2f});
    }

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
    
    Group group_filter = cp5.addGroup("ImageProcessing")
    .setPosition(view_w, 20).setHeight(20).setWidth(gui_w)
    .setBackgroundHeight(view_h).setBackgroundColor(color(16, 220)).setColorBackground(color(16, 180));
    group_filter.getCaptionLabel().align(LEFT, CENTER);
    
    int sx = 100, sy = 14;
    
//    cp5.setAutoSpacing(10, 50);
    
    cp5.addSlider("blur radius").setGroup(group_filter).setSize(sx, sy)
    .setRange(1, 100).setValue(BLUR_RADIUS)
    .plugTo(this, "BLUR_RADIUS").linebreak();
    
    cp5_slider_sigma = cp5.addSlider("gauss sigma").setGroup(group_filter).setSize(sx, sy)
    .setRange(0, 100).setValue(GAUSSBLUR_SIGMA)
    .plugTo(this, "GAUSSBLUR_SIGMA").linebreak();
    
    cp5.addToggle("auto sigma").setGroup(group_filter).setSize(sy, sy)
    .setValue(GAUSSBLUR_AUTO_SIGMA)
    .plugTo(this, "GAUSSBLUR_AUTO_SIGMA").linebreak();
    
    cp5.addSlider("convolution index").setGroup(group_filter).setSize(sx, sy)
    .setRange(0, kernel.length-1).setValue(CONVOLUTION_KERNEL_INDEX)
    .plugTo(this, "CONVOLUTION_KERNEL_INDEX").linebreak();
    
    cp5.addSlider("laplace weight").setGroup(group_filter).setSize(sx, sy)
    .setRange(0, 2).setValue(LAPLACE_WEIGHT)
    .plugTo(this, "LAPLACE_WEIGHT").linebreak();
    
    cp5.addSlider("bil radius").setGroup(group_filter).setSize(sx, sy)
    .setRange(1, 10).setValue(BILATERAL_RADIUS)
    .plugTo(this, "BILATERAL_RADIUS").linebreak();
    
    cp5.addSlider("bil sigma color").setGroup(group_filter).setSize(sx, sy)
    .setRange(0, 1).setValue(BILATERAL_SIGMA_COLOR)
    .plugTo(this, "BILATERAL_SIGMA_COLOR").linebreak();
    
    cp5.addSlider("bil sigma space").setGroup(group_filter).setSize(sx, sy)
    .setRange(0, 10).setValue(BILATERAL_SIGMA_SPACE)
    .plugTo(this, "BILATERAL_SIGMA_SPACE").linebreak();
    
    
    cp5.addSlider("filter stacks").setGroup(group_filter).setSize(sx, sy)
    .setRange(1, 10).setValue(FILTER_STACKS)
    .setNumberOfTickMarks(10)
    .plugTo(this, "FILTER_STACKS").linebreak();
    
    
    cp5.addCheckBox("displayContent").setGroup(group_filter).setSize(18, 18).setPosition(10, 250)
    .setItemsPerRow(1).setSpacingColumn(2).setSpacingRow(2)
    .addItem("display image"   , 1)
    .addItem("display geometry", 5)
    .activate(0)
    .activate(1)
    ;
    
    cp5.addRadio("displayFilter").setGroup(group_filter)
        .setPosition(10, 310).setSize(18,18)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
        .addItem("luminance"                 ,  0)
        .addItem("box blur"                  ,  1)
        .addItem("gauss blur"                ,  2)
        .addItem("median 3x3"                ,  3)
        .addItem("median 5x5"                ,  4)
        .addItem("sobel 3x3 horz"            ,  5)
        .addItem("sobel 3x3 vert"            ,  6)
        .addItem("laplace"                   ,  7)
        .addItem("convolution"               ,  8)
        .addItem("bilateral"                 ,  9)
        .addItem("median + gauss + sobel(H)" , 10)
        .addItem("median + gauss + laplace"  , 11)
        .addItem("Dog"                       , 12)
        .activate(DISPLAY_FILTER)
        ;

    group_filter.open();
  }
  

  
  public void displayFilter(int val){
    DISPLAY_FILTER = val;
  }
  
  public void displayContent(float[] val){
    DISPLAY_IMAGE    = val[0] > 0.0;
    DISPLAY_GEOMETRY = val[1] > 0.0;
  }
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { Main_ImageFilter.class.getName() });
  }
}