/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package DistanceTransformDemo;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.*;
import processing.opengl.PGraphics2D;




public class DistanceTransformDemo extends PApplet {
  
  //
  //
  // Distance Transform Demo
  //
  //
  // For each pixel the distance to its nearest obstacle is computed. 
  // The obstacles are given by a mask (binary image).
  // The result is called a distance-field or distance map can for example be
  // used to create Voronoi Diagrams, etc...
  //
  // This demo uses an implementation of the JumpFlood-algorithm, which can
  // be executed entirely on the GPU.
  //
  // 
  // Other techniques:
  //
  // Distance Transform, CPU ..... http://thomasdiewald.com/blog/?p=1994
  //
  // The same result can be achieved with any other Nearest-Neighbor technique
  // e.g. Space Partitioning Structures like Octree, Kdtree etc...
  //
  // Kd-Tree, CPU (Processing) ... http://thomasdiewald.com/blog/?p=1689
  // Kd-Tree, GPU (WebGL) ........ http://thomasdiewald.com/blog/?p=1825
  //
  //
  //
  
  boolean START_FULLSCREEN = !true;

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  PGraphics2D pg_a;
  PGraphics2D pg_b;
  PGraphics2D pg_c;
  
  DwPixelFlow context;

  public void settings() {
    if(START_FULLSCREEN){
      viewport_w = displayWidth;
      viewport_h = displayHeight;
      viewport_x = 0;
      viewport_y = 0;
      fullScreen(P3D);
    } else {
      size(viewport_w, viewport_h, P3D);
    }
    smooth(0);
  }
  
  
  public void setup(){
    surface.setLocation(viewport_x, viewport_y);

    pg_a = (PGraphics2D) createGraphics(width, height, P2D);
    pg_a.smooth(0);
    
    pg_b = (PGraphics2D) createGraphics(width, height, P2D);
    pg_b.smooth(0);
    
    pg_c = (PGraphics2D) createGraphics(width, height, P2D);
    pg_c.smooth(0);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    frameRate(60);
  }
  
  
  public void draw(){
    
    float col_r = map(mouseX, 0, width, 0, 255);
    float col_g = map(mouseY, 0, height, 0, 255);
    float rot = frameCount/60f;
    
    pg_a.beginDraw();
    pg_a.background(0);
    pg_a.rectMode(CENTER);
    pg_a.noFill();
    pg_a.strokeWeight(2);
    pg_a.stroke(col_r, col_g, 255);
    pg_a.rect(width/2, height/2, width-100,  height-100);
    
    pg_a.line(width/2, 0, width/2, height);
    
    pg_a.pushMatrix();
    pg_a.translate(mouseX,  mouseY);
    pg_a.rotate(rot);
    pg_a.stroke(255-col_r*0.5f, 255-col_g, 0);
    pg_a.rect(0, 0, 250, 250);
    pg_a.popMatrix();
    
    pg_a.pushMatrix();
    pg_a.translate(width-mouseX,  mouseY);
    pg_a.rotate(-rot);
    pg_a.stroke(255-col_r*0.5f, 255-col_g, 0);
    pg_a.rect(0, 0, 250, 250);
    pg_a.popMatrix();
    
    pg_a.endDraw();
    
    DwFilter filter =  DwFilter.get(context);
    
    // if any color component (r, g or b) is not 0.0, then the pixel is used 
    // for the mask
    filter.copy.apply(pg_a, pg_c);

    
    // Distance Transform:
    
    // 1) The distance-field/distance-map is created of the mask "pg_c"
    // 2) a voronoi is created by reading the position in the distance map
    //    and copying the texel data from the source texture "pg_a"
    
    filter.distancetransform.param.voronoi_distance_normalization = 0.0035f;
    // create distance map
    filter.distancetransform.create(pg_c);
    // create voronoi. just an example, better create your own shader for this.
    filter.distancetransform.apply(pg_a, pg_b);

    // display voronoi
    background(0);
    image(pg_b, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", pg_a.width, pg_a.height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  

  
  
  public void swapAB(){
    PGraphics2D tmp = pg_a; pg_a = pg_b; pg_b = tmp;
  }
  public void swapAC(){
    PGraphics2D tmp = pg_a; pg_a = pg_c; pg_c = tmp;
  }

  public static void main(String args[]) {
    PApplet.main(new String[] { DistanceTransformDemo.class.getName() });
  }
  
  
}