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
import processing.opengl.PGraphics2D;

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
  protected PGraphics2D canvas;
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


  public DwColorPicker(PApplet papplet, int cp_x, int cp_y, int cp_w, int cp_h, int shades){
    this.papplet = papplet;
    this.pg_draw = papplet.g;
    
    
    LAST_USED = this;
    
    this.cp_x = cp_x;
    this.cp_y = cp_y;
    this.cp_w = cp_w;
    this.cp_h = cp_h;

    shades = Math.max(Math.min(shades, cp_h/2), 1);

    num_y = 1 + shades * 2;
    res_y = cp_h/(float)num_y;
    num_x = (int) (12 * Math.round(Math.ceil(cp_w/res_y)/12f));
    num_x = Math.min(Math.max(num_x, 12) + 1, 361);
    res_x = cp_w/(float)num_x;
    
    colors     = new int[num_x * num_y];
    colors_hsb = new int[num_x * num_y][3];
    colors_rgb = new int[num_x * num_y][3];

    papplet.colorMode(PConstants.HSB, 1, 1, 1);
    
    
    float hsb_h, hsb_s, hsb_b;
    int rgb;
    // HSB color layout
    // left -> right: hue 0-360
    // top -> bottom: saturation 0-100, brigthness 100-0
    for(int y = 0; y < num_y; y++){
      for(int x = 0; x < num_x; x++){
        int idx = y * num_x + x;
        
        hsb_h =         (x-1)/(float)(num_x-1);
        hsb_s =     2 * (y+1)/(float)(num_y+1);
        hsb_b = 2 - 2 * (y+1)/(float)(num_y+1);
        
        if(x == 0){
          hsb_h = 0;
          hsb_s = 0;
          hsb_b = 1 - (y)/(float)(num_y-1);
        }
        
        hsb_h =  Math.min(Math.max(hsb_h, 0), 1);
        hsb_s =  Math.min(Math.max(hsb_s, 0), 1);
        hsb_b =  Math.min(Math.max(hsb_b, 0), 1);

        rgb   = papplet.color(hsb_h, hsb_s, hsb_b);
        
        colors    [idx] = rgb;
               
        colors_hsb[idx][0] = (int)(hsb_h * 360);
        colors_hsb[idx][1] = (int)(hsb_s * 100);
        colors_hsb[idx][2] = (int)(hsb_b * 100);
        
        colors_rgb[idx][0] = (rgb >> 16) & 0xFF;
        colors_rgb[idx][1] = (rgb >>  8) & 0xFF;;
        colors_rgb[idx][2] = (rgb >>  0) & 0xFF;;
      }
    }
    papplet.colorMode(PConstants.RGB, 255,255,255);

    canvas = (PGraphics2D) papplet.createGraphics(cp_w, cp_h, PConstants.P2D);
    canvas.smooth(0);
    canvas.beginDraw();
    canvas.background(0);
    canvas.noStroke();
    for(int y = 0; y < num_y; y++){
      for(int x = 0; x < num_x; x++){
        canvas.fill(colors[y * num_x + x]);
        canvas.rect(x * res_x, y * res_y, res_x, res_y);
      }
    }
    canvas.endDraw();
    
    canvas_img = canvas.get();
    
    selectColorByCoords(0, num_y-1);
    
    papplet.registerMethod("mouseEvent", this);
    papplet.registerMethod("pre", this);
    setAutoDraw(true);
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

  
  public void setPosition(int x, int y){
    this.cp_x = x;
    this.cp_y = y;
  }
  

  public void mouseEvent(MouseEvent me) {
    int mx_global = me.getX();
    int my_global = me.getY();
    int mx = mx_global - cp_x;
    int my = my_global - cp_y;
    
    if(me.getAction() == MouseEvent.PRESS){
      SELECTION_ACTIVE = inside(mx_global, my_global);
      cb_mouseEvent(me);
    }
    if(me.getAction() == MouseEvent.RELEASE){
      SELECTION_ACTIVE = false;
      cb_mouseEvent(me);
    }
    
    if(SELECTION_ACTIVE){
      selectColor(mx, my);
      LAST_USED = this;
      cb_mouseEvent(me);
    }
  
  }
  
  public void cb_mouseEvent(MouseEvent me) {
  }
  
  public void cb_selectedColor(){
    
  }
  

  private boolean update_canvas = false;
  
  private void updateCanvas(){
    canvas.beginDraw();
    canvas.blendMode(PConstants.REPLACE);
    canvas.image(canvas_img,0,0);
    canvas.blendMode(PConstants.EXCLUSION);
    if(selected_color_idx != -1){
      int x = selected_color_idx % num_x;
      int y = selected_color_idx / num_x;
      
//      float sx = Math.max(20, res_x);
//      float sy = Math.max(20, res_y);

      float px = x * res_x + res_x/2 - 0.5f;
      float py = y * res_y + res_y/2 - 0.5f;
    
      canvas.strokeCap(PConstants.SQUARE);
      canvas.strokeWeight(1);
      canvas.stroke(255, 100);
      
//      canvas.rectMode(PConstants.CENTER);
//      canvas.noFill();
//      canvas.rect(px, py, sx, sy);    
      
      canvas.line(px, py-10, px, py+10);
      canvas.line(px-10, py, px+10, py);
      
    }

    canvas.endDraw();
  }
  
  public boolean inside(int x, int y){
    return (x >= cp_x) && (x <= cp_x + cp_w) && (y >= cp_y) && (y <= cp_y + cp_h);
  }
  
  public int selectColor(int canvas_x, int canvas_y){
    int cx = (int) (canvas_x / res_x);
    int cy = (int) (canvas_y / res_y);
    return selectColorByCoords(cx, cy);
  }
  
  public int selectColorByCoords(int cx, int cy){
    if(cx <= 0) cx = 0; else if(cx >= num_x) cx = num_x-1;
    if(cy <= 0) cy = 0; else if(cy >= num_y) cy = num_y-1;
    return selectColorByIndex(cy * num_x + cx);
  }
  
  public int selectColorByNormalizedCoords(float cx_norm, float cy_norm){
    int cx = (int) Math.round(Math.max(Math.min(cx_norm, 1), 0) * num_x-1);
    int cy = (int) Math.round(Math.max(Math.min(cy_norm, 1), 0) * num_y-1);
    return selectColorByIndex(cy * num_x + cx);
  }
  
  public int selectColorByIndex(int idx){
    int idx_min = 0;
    int idx_max = num_x * num_y - 1;
    if(idx < idx_min) idx = idx_min; else if(idx > idx_max) idx = idx_max;
    selected_color_idx = idx;
    selected_color     = colors[selected_color_idx];
    update_canvas = true;
//    updateCanvas();
    return selected_color;
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
    int idx_min = 0;
    int idx_max = num_x * num_y - 1;
    if(idx < idx_min) idx = idx_min; else if(idx > idx_max) idx = idx_max;
    return idx;
  }
  
  public int x(){ return cp_x; }
  public int y(){ return cp_y; }
  public int w(){ return cp_w; }
  public int h(){ return cp_h; }
  
  public int getNumColorsX(){ return num_x; }
  public int getNumColorsY(){ return num_y; }
  
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
