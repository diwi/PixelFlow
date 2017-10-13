/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Miscellaneous.DistanceTransform_Voronoi;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.*;
import processing.opengl.PGraphics2D;




public class DistanceTransform_Voronoi extends PApplet {
  
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
  
  PGraphics2D pg_mask;
  PGraphics2D pg_color;
  PGraphics2D pg_voronoi;
  
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

    pg_mask = (PGraphics2D) createGraphics(width, height, P2D);
    pg_mask.smooth(0);
    
    int w = width /4;
    int h = height/4;
    
    pg_color = (PGraphics2D) createGraphics(w, h, P2D);
    pg_color.smooth(0);
    
    pg_color.beginDraw();
    pg_color.noStroke();
    for(int y = 0; y < h; y++){
      for(int x = 0; x < w; x++){
        float off = 64;
        float r = off + (255-off) * x/(float)w;
        float g = off + (255-off) * y/(float)h;
        int rgba = color(r, (r+g) * 0.5f, g);
        pg_color.fill(rgba);
        pg_color.rect(x,y,1,1);
      }
    }
    pg_color.endDraw();
    
    
    pg_voronoi = (PGraphics2D) createGraphics(width, height, P2D);
    pg_voronoi.smooth(0);

    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    frameRate(600);
  }
  

  public void draw(){

    int col_clear = color(0);
    int col_mask  = color(255);

    pg_mask.beginDraw();
    if(mousePressed){
      pg_mask.noStroke();
      pg_mask.fill(col_clear);
      pg_mask.ellipse(mouseX, mouseY, 50, 50);
    } else {
      pg_mask.strokeWeight(1);
      pg_mask.stroke(col_mask);
      pg_mask.line(mouseX, mouseY, pmouseX, pmouseY);
    }
    pg_mask.endDraw();
    

    // Distance Transform:
    
    // 1) The distance-field/distance-map is created of the mask "pg_a"
    // 2) a voronoi is created by reading the position in the distance map
    //    and copying the texel data from the source texture "pg_a"
    
    DwFilter filter = DwFilter.get(context);
    
    float[] mask = new float[4];
    mask[0] = ((col_mask>>16)&0xFF) / 255f; // normalized red
    mask[1] = ((col_mask>> 8)&0xFF) / 255f; // normalized green
    mask[2] = ((col_mask>> 0)&0xFF) / 255f; // normalized blue
    mask[3] = ((col_mask>>24)&0xFF) / 255f; // normalized alpha
    
    // create distance map
    filter.distancetransform.param.FG_mask = mask;
    filter.distancetransform.param.FG_invert = false;
    filter.distancetransform.create(pg_mask);
    
    // create voronoi. just an example, better create your own shader for this.
    filter.distancetransform.param.voronoi_distance_normalization = 0.005f;
    filter.distancetransform.apply(pg_color, pg_voronoi);
    
    // re-map luminance from [darkest-brightest] to [0, 255]
    filter.minmaxglobal.apply(pg_voronoi);
    filter.minmaxglobal.map(pg_voronoi);

    // display voronoi
    background(0);
    image(pg_mask, 0, 0);
    image(pg_color, 0, 0);
    image(pg_voronoi, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", pg_mask.width, pg_mask.height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { DistanceTransform_Voronoi.class.getName() });
  }
  
  
}