/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.imageprocessing.filter;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;


/**
 * DoG - Difference of Gaussian
 * 
 * ... can be used for edge detection.
 * 
 * @author Thomas Diewald
 *
 */
public class DoG {
  
  public static class Param{
    public int   kernel_A =  10; // radius of gauss kernel A
    public int   kernel_B =   5; // radius of gauss kernel B
    public float mult     = 2.5f;
    public float shift    = 0.5f;
  }
  
  public Param param = new Param();
  public DwPixelFlow context;

  public DoG(DwPixelFlow context){
    this.context = context;
  }
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst, PGraphicsOpenGL tmp) {
    Texture tex_src = src.getTexture(); if(!tex_src.available())   return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst .available())  return;
    Texture tex_tmp = dst.getTexture(); if(!tex_tmp .available())  return;
    
    if(src == dst) System.out.println("WARNING: DoG.apply src == dst");
    if(src == tmp) System.out.println("WARNING: DoG.apply src == tmp");
    if(dst == tmp) System.out.println("WARNING: DoG.apply dst == tmp");
    
    DwFilter filter = DwFilter.get(context);
    
    filter.gaussblur.apply(src, dst, tmp, param.kernel_A);
    filter.gaussblur.apply(src, src, tmp, param.kernel_B);
    
    float[] madA = { +param.mult, param.shift * 0.5f};
    float[] madB = { -param.mult, param.shift * 0.5f};
    filter.merge.apply(dst, src, dst, madA, madB);      
  }
  
  
}
