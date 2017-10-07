/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Miscellaneous.DistanceTransform_Threshold;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.*;
import processing.opengl.PGraphics2D;




public class DistanceTransform_Threshold extends PApplet {
  
  //
  //
  // Distance Transform Demo
  //
  //
  // For each pixel the distance to its nearest obstacle is computed. 
  // The obstacles are given by a mask (binary image).
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
    
    pg_b.beginDraw();
    pg_b.background(0);
    pg_b.endDraw();
    
    pg_a.beginDraw();
    pg_a.background(0);
    pg_a.endDraw();
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    frameRate(1000);
  }
  

  public void draw(){

    int col_mask = color(200, 220, 255);
    
    pg_a.beginDraw();
    pg_a.rectMode(CENTER);
    pg_a.noStroke();
    pg_a.fill(col_mask);
    pg_a.rect(width/2, height/2, 400, 1);
    pg_a.rect(200, height/2, 1, 600);
    pg_a.ellipse(width-200, height/2, 30, 300);
    pg_a.rect(width/2, height-1, 1, 1);
    pg_a.endDraw();
    

    
    DwFilter filter = DwFilter.get(context);
    
    float[] mask = new float[4];
    mask[0] = ((col_mask>>16)&0xFF) / 255f; // normalized red
    mask[1] = ((col_mask>> 8)&0xFF) / 255f; // normalized green
    mask[2] = ((col_mask>> 0)&0xFF) / 255f; // normalized blue
    mask[3] = ((col_mask>>24)&0xFF) / 255f; // normalized alpha
    
    filter.distancetransform.param.voronoi_distance_normalization = 0.0075f;
    // create distance map
    filter.distancetransform.param.FG_mask = mask;
    filter.distancetransform.create(pg_a);

    // computes a binary image, based on the given distance threshold
    float dth = map(mouseX, 0, width, 0, 200);
    float[] colA = mask;
    float[] colB = new float[]{0,0,0,1};
    filter.distancetransform.computeDistanceThreshold(pg_b, dth, colA, colB);

    // display result
    background(0);
    image(pg_b, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", pg_a.width, pg_a.height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { DistanceTransform_Threshold.class.getName() });
  }
  
  
}