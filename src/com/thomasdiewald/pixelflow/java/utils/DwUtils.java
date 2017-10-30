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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;



public class DwUtils {

  final static public String NL = System.getProperty("line.separator");
  
  final static public double _1_DIV_3 = 1.0 / 3.0;
  final static public float SQRT2 = (float) Math.sqrt(2);
  
  DwPixelFlow context;
  
  public DwUtils(DwPixelFlow context){
    this.context = context;
  }
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  //
  //
  // Math, Color, ....
  //
  //
  //////////////////////////////////////////////////////////////////////////////
  
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
  
  final static public double clamp(double a, double lo, double hi){
    if(a < lo) return lo;
    if(a > hi) return hi;
    return a;
  }
  
  final static public float clamp(float a, float lo, float hi){
    if(a < lo) return lo;
    if(a > hi) return hi;
    return a;
  }
  
  final static public int clamp(int a, int lo, int hi){
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
  
  // en.wikipedia.org/wiki/Smoothstep
  // www.khronos.org/registry/OpenGL-Refpages/gl4/html/smoothstep.xhtml
  final static public float smoothstep(float edge0, float edge1, float x){
    x = clamp((x - edge0)/(edge1 - edge0), 0.0f, 1.0f); // [0, 1]
    return smoothstep(x);
  }
  
  final static public double smoothstep(double edge0, double edge1, double x){
    x = clamp((x - edge0)/(edge1 - edge0), 0.0f, 1.0f); // [0, 1]
    return smoothstep(x);
  }
  
  final static public float smootherstep(float edge0, float edge1, float x){
    x = clamp((x - edge0)/(edge1 - edge0), 0.0f, 1.0f); // [0, 1]
    return smootherstep(x);
  }
  
  final static public double smootherstep(double edge0, double edge1, double x){
    x = clamp((x - edge0)/(edge1 - edge0), 0.0f, 1.0f); // [0, 1]
    return smootherstep(x);
  }


  /**  @param x [0,1] */
  final static public float smoothstep(float x){
    return x*x*(3 - 2*x);
  }
  /**  @param x [0,1] */
  final static public double smoothstep(double x){
    return x*x*(3 - 2*x);
  }
  /**  @param x [0,1] */
  final static public float smootherstep(float x){
    return x*x*x*(x*(x*6 - 15) + 10);
  }
  /**  @param x [0,1] */
  final static public double smootherstep(double x){
    return x*x*x*(x*(x*6 - 15) + 10);
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
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  //
  //
  // Texture generators
  //
  //
  //////////////////////////////////////////////////////////////////////////////
  
  
  
  
  
  

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
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  //
  //
  // File IO
  //
  //
  //////////////////////////////////////////////////////////////////////////////
  


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


  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  //
  //
  // PGraphicsOpenGL
  //
  //
  //////////////////////////////////////////////////////////////////////////////
  

  
  static public PGraphics2D changeTextureSize(PApplet papplet, PGraphics2D pg, int w, int h, int smooth, boolean[] resized)
  {   
    if(pg == null){
      pg = (PGraphics2D) papplet.createGraphics(w, h, PConstants.P2D);
      pg.smooth(smooth);
      resized[0] |= true;
    } else {
      resized[0] |= changeTextureSize(pg, w, h);
    }
    
    if(resized[0]){
      pg.loadTexture();
    }
    
    return pg;
  }
  
  static public PGraphics2D changeTextureSize(PApplet papplet, PGraphics2D pg, int w, int h, int smooth, boolean[] resized,  int internal_format, int format, int type)
  {   
    if(pg == null){
      pg = (PGraphics2D) papplet.createGraphics(w, h, PConstants.P2D);
      pg.smooth(smooth);
      resized[0] |= true;
    } else {
      resized[0] |= changeTextureSize(pg, w, h);
    }
    
    if(resized[0]){
      changeTextureFormat(pg, internal_format, format, type);
      pg.loadTexture();
    }

    return pg;
  }
  
  
  static public PGraphics3D changeTextureSize(PApplet papplet, PGraphics3D pg, int w, int h, int smooth, boolean[] resized)
  {   
    if(pg == null){
      pg = (PGraphics3D) papplet.createGraphics(w, h, PConstants.P3D);
      pg.smooth(smooth);
      resized[0] |= true;
    } else {
      resized[0] |= changeTextureSize(pg, w, h);
    }
    
    if(resized[0]){
      pg.loadTexture();
    }
    
    return pg;
  }
  
  static public PGraphics3D changeTextureSize(PApplet papplet, PGraphics3D pg, int w, int h, int smooth, boolean[] resized,  int internal_format, int format, int type)
  {   
    if(pg == null){
      pg = (PGraphics3D) papplet.createGraphics(w, h, PConstants.P3D);
      pg.smooth(smooth);
      resized[0] |= true;
    } else {
      resized[0] |= changeTextureSize(pg, w, h);
    }
    
    if(resized[0]){
      changeTextureFormat(pg, internal_format, format, type);
      pg.loadTexture();
    }

    return pg;
  }
  
  
  static public boolean changeTextureSize(PGraphicsOpenGL pg, int w, int h, int internal_format, int format, int type){
    boolean resize = changeTextureSize(pg, w, h);
    if(resize){
      changeTextureFormat(pg, internal_format, format, type);
    }
    return resize;
  }
  
  
  static public boolean changeTextureSize(PGraphicsOpenGL pg, int w, int h){
    if(pg.width == w && pg.height == h){
      return false;
    }
    
    // ... re-sizing is quite messy ...
    // TODO
    
    // restore that later
    int smooth = pg.smooth;
    
    // TODO check that
    // pg.removeCache(pg);
    
    // only way to release GL resources?
    pg.dispose(); 
    
    // TODO check that
    pg.setPrimary(false);
    
    // TODO check that
    pg.setParent(pg.parent);
    
    // possible leak:
    // --> texture = null;
    pg.setSize(w, h);
    // TODO check that

    // required, but is this supposed to be public?
    pg.initialized = false; 
    pg.smooth = smooth;
    return true;
  }
  
  
  static public void changeTextureFormat(PGraphicsOpenGL pg, int internal_format, int format, int type){
    changeTextureFormat(pg, internal_format, format, type, GL2ES2.GL_NEAREST);
  }
  
  static public void changeTextureFormat(PGraphicsOpenGL pg, int internal_format, int format, int type, int filter){
    changeTextureFormat(pg, internal_format, format, type, filter, GL2ES2.GL_CLAMP_TO_EDGE);
  }
  
  static public void changeTextureFormat(PGraphicsOpenGL pg, int internal_format, int format, int type, int filter, int wrap){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.changeTextureFormat: PGraphicsOpenGL texture not available.");
      return;
    }
    
    PGL pgl = pg.beginPGL();
    pgl.bindTexture  (tex.glTarget, tex.glName);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MIN_FILTER, filter); // GL_NEAREST, GL_LINEAR
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MAG_FILTER, filter); 
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_S, wrap);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_T, wrap);
    pgl.texImage2D   (tex.glTarget, 0, internal_format, tex.glWidth, tex.glHeight, 0, format, type, null);
    pgl.bindTexture  (tex.glTarget, 0);
    pg.endPGL();
    
    pg.beginDraw();
    pg.clear();
    pg.endDraw();
  }
  

  // GL_CLAMP
  // GL_CLAMP_TO_BORDER
  // GL_CLAMP_TO_EDGE
  // GL_REPEAT
  // GL_MIRRORED_REPEAT
  static public void changeTextureWrap(PGraphicsOpenGL pg, int wrap){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.changeTextureWrap: PGraphicsOpenGL texture not available.");
      return;
    }
    PGL pgl = pg.beginPGL();
    pgl.bindTexture  (tex.glTarget, tex.glName); 
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_S, wrap);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_T, wrap);
    pgl.bindTexture  (tex.glTarget, 0);
    pg.endPGL();
  }
  
  // GL_NEAREST
  // GL_LINEAR
  // GL_NEAREST_MIPMAP_NEAREST 
  // GL_LINEAR_MIPMAP_NEAREST  
  // GL_NEAREST_MIPMAP_LINEAR  
  // GL_LINEAR_MIPMAP_LINEAR  
  static public void changeTextureFilter(PGraphicsOpenGL pg, int min_filter, int mag_filter){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.changeTextureFilter: PGraphicsOpenGL texture not available.");
      return;
    }
    PGL pgl = pg.beginPGL();
    pgl.bindTexture  (tex.glTarget, tex.glName);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MIN_FILTER, min_filter);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MAG_FILTER, mag_filter);
    pgl.bindTexture  (tex.glTarget, 0);
    pg.endPGL();
  }
  
  
  /**
   * When chaning multiple parameters, its better to use this source-code directly.
   * 
   * @param pg
   * @param pname
   * @param param
   */
  static public void changeTextureParam(PGraphicsOpenGL pg, int pname, int param){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.changeTextureParam: PGraphicsOpenGL texture not available.");
      return;
    }
    PGL pgl = pg.beginPGL();
    pgl.bindTexture  (tex.glTarget, tex.glName);
    pgl.texParameteri(tex.glTarget, pname, param);
    pgl.bindTexture  (tex.glTarget, 0);
    pg.endPGL();
  }
  

  
  static public void generateMipMaps(PGraphicsOpenGL pg){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.generateMipMaps: PGraphicsOpenGL texture not available.");
      return;
    }

    PGL pgl = pg.beginPGL();
    pgl.bindTexture   (tex.glTarget, tex.glName);
    pgl.texParameteri (tex.glTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
//    pgl.texParameteri (tex.glTarget, GL2.GL_GENERATE_MIPMAP, GL.GL_TRUE);
    pgl.generateMipmap(tex.glTarget);
    pgl.bindTexture   (tex.glTarget, 0);
    pg.endPGL();
  }


  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  //
  //
  // HUD, Matrix, Camera, Projection, ....
  //
  //
  //////////////////////////////////////////////////////////////////////////////
  
  
  
  
  
  
  static public void copyMatrices(PGraphicsOpenGL src, PGraphicsOpenGL dst){
    dst.projection     = src.projection   .get();
    dst.camera         = src.camera       .get();
    dst.cameraInv      = src.cameraInv    .get();
    dst.modelview      = src.modelview    .get();
    dst.modelviewInv   = src.modelviewInv .get();
    dst.projmodelview  = src.projmodelview.get();
  }
  
  
  
  static public void setLookAt(PGraphicsOpenGL dst, float[] eye, float[] center, float[] up){
    dst.camera(
        eye   [0], eye   [1], eye   [2], 
        center[0], center[1], center[2], 
        up    [0], up    [1], up    [2]);
  }
  
  static public void setLookAt(PGraphicsOpenGL dst, PVector eye, PVector center, PVector up){
    dst.camera(
        eye   .x, eye   .y, eye   .z, 
        center.x, center.y, center.z, 
        up    .x, up    .y, up    .z);
  }
  
  
//  static public void swap(PGraphics[] pg){
//    PGraphics tmp = pg[0];
//    pg[0] = pg[1];
//    pg[1] = tmp;
//  }
  
  static public void swap(Object[] obj){
    Object tmp = obj[0];
    obj[0] = obj[1];
    obj[1] = tmp;
  }
  
  
  static private boolean pushed_lights = false;

  static public void beginScreen2D(PGraphics pg){
    pg.pushStyle();
    pg.hint(PConstants.DISABLE_DEPTH_TEST);
    pg.pushMatrix();
    pg.resetMatrix();
    if(pg.isGL() && pg.is3D()){
      PGraphicsOpenGL pgl = (PGraphicsOpenGL)pg;
      pushed_lights = pgl.lights;
      pgl.lights = false;
      pgl.pushProjection();
      pgl.ortho(0, pg.width, -pg.height, 0, -Float.MAX_VALUE, +Float.MAX_VALUE);
    }
  }
  
  static public void endScreen2D(PGraphics pg){
    if(pg.isGL() && pg.is3D()){
      PGraphicsOpenGL pgl = (PGraphicsOpenGL)pg;
      pgl.popProjection();
      pgl.lights = pushed_lights;
    }
    pg.popMatrix();
    pg.hint(PConstants.ENABLE_DEPTH_TEST);
    pg.popStyle();
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
}
