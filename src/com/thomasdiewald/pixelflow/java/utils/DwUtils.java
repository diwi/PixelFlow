/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.utils;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.opengl.PGraphics2D;



public class DwUtils {

  final static public String NL = System.getProperty("line.separator");
  
  final static public double _1_DIV_3 = 1.0 / 3.0;
  final static public float SQRT2 = (float) Math.sqrt(2);
  
  DwPixelFlow context;
  
  public DwUtils(DwPixelFlow context){
    this.context = context;
  }
  
  
  final static public int logNceil(double val, double n){
    return (int) Math.ceil(Math.log(val)/Math.log(n));
  }
  
  final static public int log2ceil(double val){
    return (int) Math.ceil(Math.log(val)/Math.log(2));
  }
  final static public int log4ceil(double val){
    return (int) Math.ceil(Math.log(val)/Math.log(4));
  }
  
  final static public float mix(float a, float b, float mix){
    return a * (1f-mix) + b * (mix);
  }
  final static public float[] mix(float[] a4, float[] b4, float mix, float[] dst4){
    if(dst4 == null || dst4.length < 4){
      dst4 = new float[4];
    }
    dst4[0] = mix(a4[0], b4[0], mix);
    dst4[1] = mix(a4[1], b4[1], mix);
    dst4[2] = mix(a4[2], b4[2], mix);
    dst4[3] = mix(a4[3], b4[3], mix);
    return dst4;
  }
  
  
  
  final static private float[] CL = new float[4];
  final static private float[] CR = new float[4];
  
  final static public float[] mixBilinear(float[] TL, float[] BL, float[] TR, float[] BR, float mix_LR, float mix_TB, float[] CC){
    if(CC == null || CC.length < 4){
      CC = new float[4];
    }
    DwUtils.mix(TL, BL, mix_TB, CL);
    DwUtils.mix(TR, BR, mix_TB, CR);
    DwUtils.mix(CL, CR, mix_LR, CC);
    return CC;
  }
  
  final static public void mult(float[] argb, float mult){
    argb[0] *= mult;
    argb[1] *= mult;
    argb[2] *= mult;
    argb[3] *= mult;
  }
  
  final static public float clamp(float a, float lo, float hi){
    if(a < lo) return lo;
    if(a > hi) return hi;
    return a;
  }
  
  final static public void clamp(float[] argb, float lo, float hi){
    argb[0] = clamp(argb[0], lo, hi);
    argb[1] = clamp(argb[1], lo, hi);
    argb[2] = clamp(argb[2], lo, hi);
    argb[3] = clamp(argb[3], lo, hi);
  }
  
 
  final static public float[] getColor(float[][] pallette, float val_norm, float[] rgb){
    if(rgb == null || rgb.length < 3){
      rgb = new float[3];
    }

    val_norm = clamp(val_norm, 0, 1);
    
    int   idx_max = pallette.length - 1;
    float val_map = val_norm * idx_max;
    int   idx = (int) val_map;
    float frac = val_map - idx;
    
    
    if(idx == idx_max){
      rgb[0] = pallette[idx][0];
      rgb[1] = pallette[idx][1];
      rgb[2] = pallette[idx][2];
    } else {
      rgb[0] = mix(pallette[idx][0], pallette[idx+1][0], frac);
      rgb[1] = mix(pallette[idx][1], pallette[idx+1][1], frac);
      rgb[2] = mix(pallette[idx][2], pallette[idx+1][2], frac);
    }

    return rgb;
  }
  

  static public PImage createSprite(PApplet papplet, int size, float exp1, float exp2, float mult){
    PImage pimg = papplet.createImage(size, size, PConstants.ARGB);
    pimg.loadPixels();
    for(int y = 0; y < size; y++){
      for(int x = 0; x < size; x++){
        int pid = y * size + x;
        
        float xn = ((x + 0.5f) / (float)size) * 2f - 1f;
        float yn = ((y + 0.5f) / (float)size) * 2f - 1f;
        float dd = (float) Math.sqrt(xn*xn + yn*yn);
        
        dd = clamp(dd, 0, 1);
        dd = (float) Math.pow(dd, exp1);
        dd = 1.0f - dd;
        dd = (float) Math.pow(dd, exp2);
        dd *= mult;
        dd = clamp(dd, 0, 1);
        pimg.pixels[pid] = ((int)(dd * 255)) << 24 | 0x00FFFFFF;
      }
    }
    pimg.updatePixels();
    return pimg;
  }
  
  
  static public PGraphics2D createCheckerBoard(PApplet papplet, int dimx, int dimy, int size, int colA, int colB){
    int num_x = (int) dimx/size;
    int num_y = (int) dimy/size;
    int off_x = (dimx - size *  num_x) / 2;
    int off_y = (dimy - size *  num_y) / 2;
    PGraphics2D pg = (PGraphics2D) papplet.createGraphics(dimx, dimy, PConstants.P2D);
    pg.smooth(0);
    pg.beginDraw();
    pg.blendMode(PConstants.REPLACE);
    pg.textureSampling(2);
    pg.noStroke();
    pg.fill(200);
    for(int y = -1; y < num_y+1; y++){
      for(int x = -1; x < num_x+1; x++){
        int px = off_x + x * size;
        int py = off_y + y * size;
        int col = (x ^ y) & 1;
        if(col == 1){
          pg.fill(colA);
        } else {
          pg.fill(colB);
        }
        pg.rect(px, py, size, size);
      }
    }
    pg.endDraw();
    return pg;
  }
  
  
  
  
  
  static public float[] COL_TL = { 64,  0,  0, 255};
  static public float[] COL_TR = {255,128,  0, 255};
  static public float[] COL_BL = {  0,128,255, 255};
  static public float[] COL_BR = {255,255,255, 255};
  static public float[] COL_CC = {  0,  0,  0,   0};
  
  
  static public PGraphics2D createBackgroundNoiseTexture(PApplet papplet, int dimx, int dimy){
   
    PGraphics2D pg = (PGraphics2D) papplet.createGraphics(dimx, dimy, PConstants.P2D);
    pg.smooth(0);
    
    pg.beginDraw();
    pg.noStroke();
    
    for(int y = 0; y < dimy; y++){
      for(int x = 0; x < dimx; x++){
        float nx = x / (float) pg.width;
        float ny = y / (float) pg.height;
        float nval = papplet.noise(x * 0.025f, y * 0.025f) * 1.7f  + 0.3f;
        DwUtils.mixBilinear(COL_TL, COL_BL, COL_TR, COL_BR, nx, ny, COL_CC);
        DwUtils.mult(COL_CC, nval);
        DwUtils.clamp(COL_CC, 0, 255);
        pg.fill(COL_CC[0], COL_CC[1], COL_CC[2], 255);
        pg.rect(x, y, 1, 1);
      }
    }
    
    int num_points = dimx * dimy / 4;
    for(int i = 0; i < num_points; i++){
      float x = papplet.random(0, dimx-1);
      float y = papplet.random(0, dimy-1);
      pg.fill(0, papplet.random(255));
      pg.rect(x, y, 1, 1);
    }

    pg.endDraw();
    return pg;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  


  public String[] readASCIIfile(InputStream inputstream) {
    BufferedReader reader = null;

    int num_lines = 0;
    String[] lines = new String[2048];

    try {
      reader = new BufferedReader(new InputStreamReader(inputstream));
      String line = null;

      while ((line = reader.readLine()) != null) {
        if (num_lines == lines.length) {
          lines = Arrays.copyOf(lines, num_lines << 1);
        }
        lines[num_lines++] = line;
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return Arrays.copyOf(lines, num_lines);
  }

  
  
  
  
  
//  public InputStream createInputStream(String path) {
//
//    InputStream inputstream = null;
//
//    if (inputstream == null) {
//      URL url = DwUtils.class.getClassLoader().getResource(path);
//      inputstream = DwUtils.class.getClassLoader().getResourceAsStream(path);
//      if (url != null && inputstream != null) {
//        System.out.println("v0 url: " + url.getFile());
//        // System.out.println("v2 uri: "+ url.toURI());
//      }
//    }
//
//    if (inputstream == null) {
//      URL url = context.papplet.getClass().getResource(path);
//    
//      inputstream = context.papplet.getClass().getResourceAsStream(path);
//      if (url != null && inputstream != null) {
//        System.out.println("v1 url: " + url.getFile());
//        // System.out.println("v1 uri: "+ url.toURI());
//      } 
//    }
//
//    if (inputstream == null) {
//      inputstream = context.papplet.createInput(path);
//      if(inputstream != null){
//        System.out.println("v2 path: "+path);
//      }
//    }
//
//    if (inputstream == null) {
//      System.out.println("could not create inputstream for " + path);
//    }
//
//    return inputstream;
//  }

  
  
  private boolean DEBUG = !true;
  
  
  public InputStream createInputStream(String path) {

    InputStream inputstream = null;
    
    if (inputstream == null) {
      File file = new File(path);
      if(file.exists()){
        try {
          inputstream = new FileInputStream(file);
          if(DEBUG)System.out.println("v0 path: " + file);
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }
      }
    }

    
    if (inputstream == null) {
      URL url = DwUtils.class.getClassLoader().getResource(path);
      if(url != null){
        inputstream = DwUtils.class.getClassLoader().getResourceAsStream(path);
        if (inputstream != null) {
          if(DEBUG)System.out.println("v0 url: " + url.getFile());
        }
      }
    }
    

    if (inputstream == null) {
      URL url = context.papplet.getClass().getResource(path);
      if(url != null){
        inputstream = context.papplet.getClass().getResourceAsStream(path);
        if (inputstream != null) {
          if(DEBUG)System.out.println("v1 path: " + url);
        } 
      }
    }

    
    // no success so far, so try the processing way (slower)
    if (inputstream == null) {
      inputstream = context.papplet.createInput(path);
      if(inputstream != null){
        if(DEBUG)System.out.println("v2 path: "+path);
      }
    }

    
    if (inputstream == null) {
      System.out.println("DwUtils ERROR: could not create inputstream for " + path);
    }

    return inputstream;
  }

  
  
  
  
  
  public String[] readASCIIfile(String path) {
    InputStream inputstream = createInputStream(path);
    String[] lines = readASCIIfile(inputstream);
    return lines;
  }
  
  public String[] readASCIIfileNL(String path) {
    String[] lines = readASCIIfile(path);
    for(int i = 0; i < lines.length; i++){
      lines[i] += DwUtils.NL;
    }
    return lines;
  }


}
