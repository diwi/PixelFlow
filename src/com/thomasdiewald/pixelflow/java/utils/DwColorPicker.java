/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
 * 
 * src  - www.github.com/diwi/PixelFlow
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.utils;

import java.util.Locale;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.MouseEvent;

/**
 * @author Thomas Diewald
 * 
 * 
 * Experimential ColorPicker. WIP.
 * 
 *
 */
public class DwColorPicker {
  
  // not tread safe
  public static DwColorPicker LAST_USED;
 
  protected PApplet papplet;
  protected PGraphics canvas;
  protected PImage canvas_img;
  
  protected int cp_x, cp_y;
  protected int cp_w, cp_h;
  
  protected int   num_x, num_y;
  protected float res_x, res_y;
  
  public int[]   colors;
  public int[][] colors_rgb;
  public int[][] colors_hsb;
  
  protected boolean SELECTION_ACTIVE = false;
  protected int     selected_color_idx = 0;
  protected int     selected_color     = 0;

  protected PGraphics pg_draw;

  public DwColorPicker(PApplet papplet, int cp_x, int cp_y, int cp_w, int cp_h){
    this(papplet, cp_x, cp_y, cp_w, cp_h, 5);
  }

  public DwColorPicker(PApplet papplet, int cp_x, int cp_y, int cp_w, int cp_h, int shades_y){
    this.papplet = papplet;
    papplet.registerMethod("pre", this);
    setAutoDraw(true);
    setAutoMouse(true);
    setDrawCanvas(papplet.g); 
    setPosition(cp_x, cp_y);
    setSize(cp_w, cp_h);
    createPallette(shades_y);
    LAST_USED = this;
  }
  
  public void setPosition(int x, int y){
    this.cp_x = x;
    this.cp_y = y;
  }
  public void setSize(int w, int h){
    this.cp_w = w;
    this.cp_h = h;
  }

  
  /**
   * Automatically compute the number of shades in X-direction
   * 
   * @param shades_y
   */
  public void createPallette(int shades_y){
    int ny, nx;
    float ry;
    
    ny = shades_y + 1 - (shades_y & 1); // odd number
    ny = Math.min(Math.max(ny, 1), cp_h);
  
    ry = cp_h / (float) ny;
    nx = (int) (12 * Math.round(Math.ceil(cp_w/ry)/12f));
    nx = Math.min(Math.max(nx, 12) + 1, 361);

    createPallette(nx, ny);
  }
  
  public void createPallette(int shades_x, int shades_y){

    if(num_x == shades_x && num_y == shades_y){
      return;
    }
    
    num_x = shades_x;
    num_y = shades_y;
    
    res_y = cp_h / (float) num_y;
    res_x = cp_w / (float) num_x;
    
    colors     = new int[num_x * num_y];
    colors_hsb = new int[num_x * num_y][3];
    colors_rgb = new int[num_x * num_y][3];
    
    papplet.colorMode(PConstants.HSB, 1, 1, 1);
    
    float hsb_h, hsb_s, hsb_b;
    int rgb;
    // HSB color layout
    // left -> right: hue 0-360
    // top -> bottom: saturation 0-100, brightness 100-0
    for(int y = 0; y < num_y; y++){
      for(int x = 0; x < num_x; x++){
        int idx = y * num_x + x;
        
        hsb_h =         (x-1)/(float)(num_x-1);
        hsb_s =     2 * (y+1)/(float)(num_y+1);
        hsb_b = 2 - 2 * (y+1)/(float)(num_y+1);
        
        // grayscale, left
        if(x == 0){
          hsb_h = 0;
          hsb_s = 0;
          hsb_b = 1 - (y)/(float)(num_y-1);
        } else {
          hsb_h =  Math.min(Math.max(hsb_h, 0), 1);
          hsb_s =  Math.min(Math.max(hsb_s, 0), 1);
          hsb_b =  Math.min(Math.max(hsb_b, 0), 1);
        }

        rgb = papplet.color(hsb_h, hsb_s, hsb_b);
        
        colors    [idx] = rgb;
               
        colors_hsb[idx][0] = (int)(hsb_h * 360);
        colors_hsb[idx][1] = (int)(hsb_s * 100);
        colors_hsb[idx][2] = (int)(hsb_b * 100);
        
        colors_rgb[idx][0] = (rgb >> 16) & 0xFF;
        colors_rgb[idx][1] = (rgb >>  8) & 0xFF;
        colors_rgb[idx][2] = (rgb >>  0) & 0xFF;
      }
    }
    papplet.colorMode(PConstants.RGB, 255,255,255);
    
    
    if(canvas == null || canvas.width != cp_w || canvas.height != cp_h){
      if(canvas == null){
        papplet.g.removeCache(canvas);
      }
      canvas = papplet.createGraphics(cp_w, cp_h);
      canvas.smooth(0);
    }
    canvas.beginDraw();
    canvas.background(0);
    canvas.noStroke();
    // pick the faster drawing method -> TOW the least amount of rectangles
    if((num_y * num_x) < (cp_h * cp_w))
    {
      int sx = (int) Math.ceil(res_x + 0.0);
      int sy = (int) Math.ceil(res_y + 0.0);
      for(int y = 0; y < num_y; y++){
        for(int x = 0; x < num_x; x++){
          canvas.fill(colors[y * num_x + x]);
          int px = (int) Math.ceil(x * res_x);
          int py = (int) Math.ceil(y * res_y);
          canvas.rect(px, py, sx, sy);
        }
      }
    } 
    else 
    {
      for(int y = 0; y < cp_h; y++){
        for(int x = 0; x < cp_w; x++){
          canvas.fill(selectColorByCoords(x, y));
          canvas.rect(x, y, 1, 1);
        }
      }
    }
    canvas.endDraw();
    
    canvas_img = canvas.get();
    
    // finally, select default ... black
    selectColorByGrid(0, num_y-1);
  }

  
  

  
  
  
  private boolean update_canvas = false;
  
  public void updateCanvas(){
    canvas.beginDraw();
    canvas.clear();
    canvas.blendMode(PConstants.REPLACE);
    canvas.image(canvas_img,0,0);
    // draw cursor over selected bin
    if(selected_color_idx != -1){
      int x = selected_color_idx % num_x;
      int y = selected_color_idx / num_x;
      int px = (int) ((x + 0.5f) * res_x);
      int py = (int) ((y + 0.5f) * res_y);
      canvas.blendMode(PConstants.EXCLUSION);
      canvas.strokeCap(PConstants.SQUARE);
      canvas.strokeWeight(1);
      canvas.stroke(255, 100);
      canvas.line(px   , py-10, px   , py+10);
      canvas.line(px-10, py   , px+10, py   );
    }
    canvas.endDraw();
  }
  

  public void pre(){
    if(update_canvas){
      update_canvas = false;
      updateCanvas();
      cb_selectedColor();
    }
  }
  
  public void dispose(){
    papplet.unregisterMethod("mouseEvent", this);
    papplet.unregisterMethod("draw"      , this);
    papplet.unregisterMethod("pre"       , this);
    auto_draw = false;
    auto_mouse = false;
  }
  
  boolean auto_draw = false;
  public DwColorPicker setAutoDraw(boolean auto_draw){
    if(this.auto_draw != auto_draw){
      if(auto_draw){
        papplet.registerMethod("draw", this);
      } else {
        papplet.unregisterMethod("draw", this);
      }
    }
    this.auto_draw = auto_draw;
    return this;
  }
  
  boolean auto_mouse = false;
  public DwColorPicker setAutoMouse(boolean auto_mouse){
    if(this.auto_mouse != auto_mouse){
      if(auto_mouse){
        papplet.registerMethod("mouseEvent", this);
      } else {
        papplet.unregisterMethod("mouseEvent", this);
      }
    }
    this.auto_mouse = auto_mouse;
    return this;
  }
  

  
  

  

  public void mouseEvent(MouseEvent me) {
    int mx_global = me.getX();
    int my_global = me.getY();
    int mx = mx_global - cp_x;
    int my = my_global - cp_y;
    
    if(me.getAction() == MouseEvent.PRESS){
      SELECTION_ACTIVE = inside(mx, my);
      cb_mouseEvent(me);
    }
    if(me.getAction() == MouseEvent.RELEASE){
      SELECTION_ACTIVE = false;
      cb_mouseEvent(me);
    }
    
    if(SELECTION_ACTIVE){
      selectColorByCoords(mx, my);
      LAST_USED = this;
      cb_mouseEvent(me);
    }
  
  }
  
  /**
   * In case someone overrides/extends this class. TODO
   * @param me
   */
  public void cb_mouseEvent(MouseEvent me) {
  }
  
  
  /**
   * In case someone overrides/extends this class. TODO
   * @param me
   */
  public void cb_selectedColor(){
  }
  



  public boolean inside(int x, int y){
    return (x >= 0) && (x <= cp_w) && (y >= 0) && (y <= cp_h);
  }
  

  public int selectColorByNormalizedCoords(float cxn, float cyn){
    int cx = (int) (DwUtils.clamp(cxn, 0, 1) * cp_w / res_x);
    int cy = (int) (DwUtils.clamp(cyn, 0, 1) * cp_h / res_y);
    return selectColorByGrid(cx, cy);
  }

  public int selectColorByCoords(int cx, int cy){
    cx = (int) (DwUtils.clamp(cx, 0, cp_w) / res_x);
    cy = (int) (DwUtils.clamp(cy, 0, cp_h) / res_y);
    return selectColorByGrid(cx, cy);
  }

  public int selectColorByGrid(int cx, int cy){
    cx = DwUtils.clamp(cx, 0, num_x-1);
    cy = DwUtils.clamp(cy, 0, num_y-1);
    return selectColorByIndex(cy * num_x + cx);
  }
  
  
  // find the "closest" color
  public int selectColorByRGB(int r, int g, int b){
    int idx = 0;
    int dd_min = Integer.MAX_VALUE;
    for(int i = 0; i < colors_rgb.length; i++){
      int dr = colors_rgb[i][0] - r;
      int dg = colors_rgb[i][1] - g;
      int db = colors_rgb[i][2] - b;
      int dd = dr*dr + dg*dg + db*db;
      
      if(dd_min > dd) {
        dd_min = dd;
        idx = i;
      }
    }
    return selectColorByIndex(idx);
  }
  
  public int selectColorByIndex(int idx){
    selected_color_idx = DwUtils.clamp(idx, 0,  num_x * num_y - 1);
    selected_color     = colors[selected_color_idx];
    update_canvas = true;
    return selected_color;
  }

  


  
  
  public int getSelectedColorIdx(){
    return selected_color_idx;
  }
  
  public int getSelectedColor(){
    return selected_color;
  }
  
  public int[] getSelectedRGBColor(){
    return colors_rgb[selected_color_idx];
  }
  
  public int[] getSelectedHSBColor(){
    return colors_hsb[selected_color_idx];
  }
 
  public int getColorByIndex(int idx){
    return colors[assureIndex(idx)];
  }
  
  public int[] getRGBColorByIndex(int idx){
    return colors_rgb[assureIndex(idx)];
  }
  public int[] getHSBColorByIndex(int idx){
    return colors_hsb[assureIndex(idx)];
  }
  
  private int assureIndex(int idx){
    return DwUtils.clamp(idx, 0,  num_x * num_y - 1);
  }
  
  public int x(){ return cp_x; }
  public int y(){ return cp_y; }
  public int w(){ return cp_w; }
  public int h(){ return cp_h; }
  
  public int getNX(){ return num_x; }
  public int getNY(){ return num_y; }
  
  public String getSelectedRGBasString(){
    int[] rgb = getSelectedRGBColor();
    return String.format(Locale.ENGLISH, "RGB: %3d,%3d,%3d", rgb[0], rgb[1], rgb[2]);
  }
  public String getSelectedHSBasString(){
    int[] hsb = getSelectedHSBColor();
    return String.format(Locale.ENGLISH, "HSB: %3d,%3d,%3d", hsb[0], hsb[1], hsb[2]);
  }
  
  
  
  
  
  
  
  
  
  
  public void setDrawCanvas(PGraphics draw_canvas){
    pg_draw = draw_canvas;
  }

  public void draw(){
    display();
    displayHUD();
  }

  
  public void display(){
    pg_draw.noStroke();
    pg_draw.rectMode(PConstants.CORNER);
    pg_draw.fill(200, 50);
    pg_draw.rect(cp_x-1, cp_y-1, cp_w+2, cp_h+2);
    pg_draw.image(canvas, cp_x, cp_y); 
  }
  
  
  public void displayHUD(){
    int px = cp_x;
    int py = cp_y + cp_h + 15;
    pg_draw.stroke(255);
    pg_draw.strokeWeight(1);
    pg_draw.fill(getSelectedColor());
    pg_draw.rect(px, py, 25, 25, 0);
  
    pg_draw.fill(255);
    pg_draw.text(getSelectedHSBasString(), px + 30, py + 9);
    pg_draw.text(getSelectedRGBasString(), px + 30, py + 9 + 9 + 7);
  }
  
  
}
