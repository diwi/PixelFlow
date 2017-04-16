package com.thomasdiewald.pixelflow.java.utils;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphicsOpenGL;



/**
 * 
 * A tool to magnify a region of interest of a given PGraphics object.
 * 
 * Just for debugging purpose.
 * 
 * @author Thomas Diewald
 *
 */


public class DwMagnifier{
  
  protected PApplet papplet;
  protected PGraphics2D pg_region;
  
  protected float magnification = 1f;
  
  public int x, y, w, h;
  
  protected boolean center = true;
 
  public DwMagnifier(PApplet papplet, float magnification, int x, int y, int w, int h){
    this.papplet       = papplet;
    this.magnification = magnification;
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    setMagnification(magnification);
  }
  
  public void setMagnification(float magnification){
    if(this.magnification == magnification && pg_region != null){
      return;
    }
    this.magnification = magnification;
    
    int region_w = Math.round(w/magnification);
    int region_h = Math.round(h/magnification);
    pg_region = (PGraphics2D) papplet.createGraphics(region_w, region_h, PConstants.P2D);
    pg_region.smooth();
    pg_region.textureSampling(2);
  }
  
  public void setCenter(boolean center){
    this.center = center;
  }

 
  
  protected PGraphics pg_mag;
  protected int mag_px = 0;
  protected int mag_py = 0;
  
  public void apply(PGraphics pg_mag, int mag_px, int mag_py){
    if(center){
      mag_px -= pg_region.width /2;
      mag_py -= pg_region.height/2;
    }
    
    pg_region.beginDraw();
    pg_region.background(0);
    pg_region.image(pg_mag, -mag_px, -mag_py);
    pg_region.endDraw();
    
    this.mag_px = mag_px;
    this.mag_py = mag_py;
    this.pg_mag = pg_mag;
  }
  
  public void displayTool(){
    if(pg_mag != null){
      
      int region_w = pg_region.width;
      int region_h = pg_region.height;
    
      int src_w = pg_mag.width;
      int src_h = pg_mag.height;
      
      pg_mag.beginDraw();
      pg_mag.pushStyle();
      pg_mag.pushMatrix();
      ((PGraphicsOpenGL) pg_mag).pushProjection();
      pg_mag.hint(PConstants.DISABLE_DEPTH_TEST);
      
      pg_mag.resetMatrix();
      if(pg_mag.is3D()){
        pg_mag.ortho(0, src_w, -src_h, 0, 0, 1);
      }

      pg_mag.blendMode(PConstants.EXCLUSION);
      pg_mag.rectMode(PConstants.CORNER);
      pg_mag.noFill();
      pg_mag.stroke(255, 128);
      pg_mag.strokeWeight(1);
      pg_mag.rect(mag_px+0.5f, mag_py+0.5f, region_w, region_h);
      
      
      pg_mag.hint(PConstants.ENABLE_DEPTH_TEST);
      ((PGraphicsOpenGL) pg_mag).popProjection();
      pg_mag.popMatrix();
      pg_mag.popStyle();
      pg_mag.endDraw();
    }
  }
  
  public void display(PGraphics pg_canvas){
//    pg_canvas.beginDraw();
    pg_canvas.image(pg_region, x, y, w, h);
    pg_canvas.rectMode(PConstants.CORNER);
    pg_canvas.stroke(128);
    pg_canvas.strokeWeight(1);
    pg_canvas.noFill();
    pg_canvas.rect(x, y, w, h);
//    pg_canvas.endDraw();
  }
  
}
