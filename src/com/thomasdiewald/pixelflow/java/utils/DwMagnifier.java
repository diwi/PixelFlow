package com.thomasdiewald.pixelflow.java.utils;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.opengl.PGraphics2D;



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
  


 
  public void setDisplayPosition(int x, int y){
    this.x = x;
    this.y = y;
  }
  
  protected PGraphics pg_mag;
  protected int mag_px = 0;
  protected int mag_py = 0;
  
  public void apply(PGraphics pg_mag, int mag_px, int mag_py){
    setMagPosition(mag_px, mag_py);
    apply(pg_mag);
  }
  
  
  public void setMagCenter(boolean center){
    this.center = center;
  }
  
  public void setMagPosition(int mag_px, int mag_py){
    if(center){
      mag_px -= pg_region.width /2;
      mag_py -= pg_region.height/2;
    }
    
    this.mag_px = mag_px;
    this.mag_py = mag_py;
  }
  
  public void apply(PGraphics pg_mag){
    this.pg_mag = pg_mag;
    pg_region.beginDraw();
    pg_region.background(0);
    pg_region.image(pg_mag, -mag_px, -mag_py);
    pg_region.endDraw();
  }
  
  
  public void displayTool(){
    if(pg_mag == null){
      return;
    }
      
    boolean offscreen = pg_mag != papplet.g;
    if(offscreen) pg_mag.beginDraw();
    DwUtils.beginScreen2D(pg_mag);
    {
//      pg_mag.pushStyle(); // TODO
      pg_mag.blendMode(PConstants.EXCLUSION);
      pg_mag.rectMode(PConstants.CORNER);
      pg_mag.noFill();
      pg_mag.stroke(255, 128);
      pg_mag.strokeWeight(1);
      pg_mag.rect(mag_px+0.5f, mag_py+0.5f,  pg_region.width, pg_region.height);
//      pg_mag.popStyle();
    }
    DwUtils.endScreen2D(pg_mag);
    if(offscreen) pg_mag.endDraw();
  }
  
  public void display(PGraphics pg_canvas){
    display(pg_canvas, x, y);
  }
  
  public void display(PGraphics pg_canvas, int x, int y){
    setDisplayPosition(x, y);
    pg_canvas.image(pg_region, x, y, w, h);
    pg_canvas.rectMode(PConstants.CORNER);
    pg_canvas.stroke(128);
    pg_canvas.strokeWeight(1);
    pg_canvas.noFill();
    pg_canvas.rect(x, y, w, h);
  }
  
}
