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

package com.thomasdiewald.pixelflow.java.imageprocessing.filter;

import com.jogamp.opengl.GLES3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * @author Thomas Diewald
 * 
 * 
 * resources:
 * 
 * 1) http://prideout.net/archive/bloom/
 *
 * 2) https://threejs.org/examples/webgl_postprocessing_unreal_bloom.html
 *    three.js/examples/js/postprocessing/UnrealBloomPass.js 
 *    
 * 3) http://www.gamasutra.com/view/feature/130520/realtime_glow.php
 *
 */
public class Bloom {
  
  public static class Param{
    public float mult   = 1f;   // [0, whatever]
    public float radius = 0.5f; // [0, 1]
  }
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
  // number of mip-levels for bluring
  private int     BLUR_LAYERS_MAX = 10;
  private int     BLUR_LAYERS = 5;
  private boolean BLUR_LAYERS_auto = true;

  // blur textures
  public DwGLTexture[] tex_blur = new DwGLTexture[0];
  public DwGLTexture[] tex_temp = new DwGLTexture[0];
  
  // texture weights (merge pass)
  public float[] tex_weights;
  
  // Filter for merging the textures
  public TextureMerge tex_merge;
  
  
  public Bloom(DwPixelFlow context){
    this.context = context;
    this.tex_merge = new TextureMerge(context);
  }
  
  public void setBlurLayersMax(int BLUR_LAYERS_MAX){
    this.BLUR_LAYERS_MAX = BLUR_LAYERS_MAX;
    this.BLUR_LAYERS = Math.min(BLUR_LAYERS, BLUR_LAYERS_MAX);
  }
  
  public void setBlurLayers(int BLUR_LAYERS){
    this.BLUR_LAYERS = Math.min(BLUR_LAYERS, BLUR_LAYERS_MAX);
    this.BLUR_LAYERS_auto = false;
  }
  
  public int getNumBlurLayers(){
    return BLUR_LAYERS;
  }
  
  public void release(){
    for(int i = 0; i < tex_blur.length; i++){
      if(tex_temp[i] != null) tex_temp[i].release();
      if(tex_blur[i] != null) tex_blur[i].release();
    }
  }
  

  public void resize(int w, int h){
    int wi = w;
    int hi = h;
    
    // 1) compute number of blur layers
    if(BLUR_LAYERS_auto){
      BLUR_LAYERS = Math.max(DwUtils.log2ceil(wi), DwUtils.log2ceil(hi)) >> 1;
      BLUR_LAYERS = Math.min(BLUR_LAYERS, BLUR_LAYERS_MAX);
    }

    // 2) init/release textures if needed
    if(tex_blur.length != BLUR_LAYERS){
      release();
      tex_blur = new DwGLTexture[BLUR_LAYERS];
      tex_temp = new DwGLTexture[BLUR_LAYERS];
      for(int i = 0; i < BLUR_LAYERS; i++){
        tex_blur[i] = new DwGLTexture();
        tex_temp[i] = new DwGLTexture();
      }
    }

    // 3) allocate textures
    for(int i = 0; i < BLUR_LAYERS; i++){
      tex_temp[i].resize(context, GLES3.GL_RGBA8, wi, hi, GLES3.GL_RGBA, GLES3.GL_UNSIGNED_BYTE, GLES3.GL_LINEAR, 4, 1);
      tex_blur[i].resize(context, GLES3.GL_RGBA8, wi, hi, GLES3.GL_RGBA, GLES3.GL_UNSIGNED_BYTE, GLES3.GL_LINEAR, 4, 1);
      tex_temp[i].setParam_WRAP_S_T(GLES3.GL_CLAMP_TO_EDGE);
      tex_blur[i].setParam_WRAP_S_T(GLES3.GL_CLAMP_TO_EDGE);
      wi >>= 1;
      hi >>= 1;
    }
  }
  

  private float[] computeWeights(float[] weights){
    if(weights == null || weights.length != BLUR_LAYERS){
      weights = new float[BLUR_LAYERS];
    }
    
    float step = 1f / BLUR_LAYERS;
    for(int i = 0; i < BLUR_LAYERS; i++){
      float fac = 1f - step * i;
      float weight = DwUtils.mix(fac, 1.0f + step - fac, param.radius);
      weights[i] = param.mult * weight;

//      weights[i] = param.mult * step;
    }
    return weights;
  }
  
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst){
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    
    // lazy init/allocation
    resize(dst.width, dst.height);
    
    // 1) use src-texture as base for blur-levels
    DwFilter.get(context).copy.apply(src, tex_blur[0]);
    
    // 2) blur passes
    for(int i = 1; i < BLUR_LAYERS; i++){
      DwFilter.get(context).gaussblur.apply(tex_blur[i-1], tex_blur[i], tex_temp[i], i + 2);
    }
    
    // 3) compute blur-texture weights
    tex_weights = computeWeights(tex_weights);
    
    // 4) merge blur-textures
    tex_merge.apply(dst, tex_blur, tex_weights);   
  }
  

}
