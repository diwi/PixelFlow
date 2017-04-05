/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.imageprocessing.filter;


import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

public class GaussianBlur {
  
  // separated blur kernel
  static private final int[] HORZ = new int[]{1,0};
  static private final int[] VERT = new int[]{0,1};
  
  public DwPixelFlow context;


  public float DEFAULT_RADIUS_SIGMA_RATIO = 0.5f;
  
  public GaussianBlur(DwPixelFlow context){
    this.context = context;
    
    boolean DEBUG = false;
    
    if(DEBUG){
      int    radius = 2;
      double sigma  = radius/3.0;
    
      double coeff_sum = 0; // for normalization
      double[] coeff = new double[radius * 2 + 1];
  
      for (int idx = 0, i = -radius; i <= +radius; i++, idx++){
        coeff[idx] = Math.exp(-0.5 * i * i / (sigma * sigma));
        coeff_sum += coeff[idx];
      }  
      
      float coeff_sum_check = 0;
      for (int idx = 0, i = -radius; i <= +radius; i++, idx++){
        coeff[idx] /= coeff_sum;
        coeff_sum_check += coeff[idx];
  //      System.out.printf(Locale.ENGLISH, "%1.8f\n",coeff[idx]);
      }
  
      System.out.println("sum_check = "+coeff_sum_check);
      
      System.out.println("\n\nvertical:");
      for (int idx = 0, i = -radius; i <= +radius; i++, idx++){
        String sum = i == radius ? "" : "+";
        System.out.printf(Locale.ENGLISH, "%1.9f * textureOffset(tex, posn, ivec2(0,%3d)) "+sum+" \n", coeff[idx], i);
      }
      
      System.out.println("\n\nhorizontal:");
      for (int idx = 0, i = -radius; i <= +radius; i++, idx++){
        String sum = i == radius ? "" : "+";
        System.out.printf(Locale.ENGLISH, "%1.9f * textureOffset(tex, posn, ivec2(%3d, 0)) "+sum+" \n", coeff[idx], i);
      }
      
    }
  }


  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, PGraphicsOpenGL tmp, int radius) {
    apply(src, dst, tmp, radius, radius * DEFAULT_RADIUS_SIGMA_RATIO);
  }

  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, PGraphicsOpenGL tmp, int radius, float sigma) {
    if(src == tmp || dst == tmp){
      System.out.println("GaussianBlur error: read-write race");
      return;
    }
    if(radius <= 0){
      return; 
    }
    
    Texture tex_src = src.getTexture(); if(!tex_src.available()) return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available()) return;
    Texture tex_tmp = tmp.getTexture(); if(!tex_tmp.available()) return;
    
//    tmp.beginDraw();
    context.begin();
    context.beginDraw(tmp);
    pass(tex_src.glName, tmp.width, tmp.height, radius, sigma, HORZ);
    context.endDraw();
    context.end("GaussianBlur.apply - HORZ");
//    tmp.endDraw(); 
    
//    Texture tex_tmp = tmp.getTexture();
    
//    dst.beginDraw();
    context.begin();
    context.beginDraw(dst);
    pass(tex_tmp.glName, dst.width, dst.height, radius, sigma, VERT);
    context.endDraw();
    context.end("GaussianBlur.apply - VERT");
//    dst.endDraw(); 
  }
  
  public void apply(DwGLTexture src, DwGLTexture dst, DwGLTexture tmp, int radius) {
    apply(src, dst, tmp, radius, radius * DEFAULT_RADIUS_SIGMA_RATIO);
  }

  public void apply(DwGLTexture src, DwGLTexture dst, DwGLTexture tmp, int radius, float sigma) {
    if(src == tmp || dst == tmp){
      System.out.println("GaussBlur error: read-write race");
      return;
    }
    if(radius <= 0){
      return; 
    }
    
    context.begin();
    context.beginDraw(tmp);
    pass(src.HANDLE[0], tmp.w, tmp.h, radius, sigma, HORZ);
    context.endDraw();
    context.end("GaussianBlur.apply - HORZ");
    
    context.begin();
    context.beginDraw(dst);
    pass(tmp.HANDLE[0], dst.w, dst.h, radius, sigma, VERT);
    context.endDraw();
    context.end("GaussianBlur.apply - VERT");
  }
  
  DwGLSLProgram shader;
  private void pass(int tex_handle, int w, int h, int radius, float sigma, int[] dir ){
    if(shader == null) shader = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/gauss.frag");
    shader.begin();
    shader.uniform2f     ("wh_rcp", 1f/w, 1f/h);
    shader.uniform1i     ("radius", radius);
    shader.uniform1f     ("sigma" , sigma);
    shader.uniform2i     ("dir"   , dir[0], dir[1]);
    shader.uniformTexture("tex"   , tex_handle);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }

 
}
