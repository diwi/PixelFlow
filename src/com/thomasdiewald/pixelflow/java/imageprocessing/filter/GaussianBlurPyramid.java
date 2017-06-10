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
public class GaussianBlurPyramid {
  
  public DwPixelFlow context;
  
  // number of mip-levels for bluring
  private int     BLUR_LAYERS_MAX = 10;
  private int     BLUR_LAYERS = 5;

  public GaussianBlur gaussblur;

  // blur textures
  public DwGLTexture[] tex_blur = new DwGLTexture[0];
  public DwGLTexture[] tex_temp = new DwGLTexture[0];
  
  public GaussianBlurPyramid(DwPixelFlow context){
    this.context = context;
    this.gaussblur = new GaussianBlur(context);
  }
  
  public void setBlurLayersMax(int BLUR_LAYERS_MAX){
    this.BLUR_LAYERS_MAX = BLUR_LAYERS_MAX;
    this.BLUR_LAYERS = Math.min(BLUR_LAYERS, BLUR_LAYERS_MAX);
  }
  
  public void setBlurLayers(int BLUR_LAYERS){
    this.BLUR_LAYERS = Math.min(BLUR_LAYERS, BLUR_LAYERS_MAX);
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
  
  public void resize(PGraphicsOpenGL src){
    resize(src.width, src.height, GLES3.GL_RGBA8, GLES3.GL_RGBA, GLES3.GL_UNSIGNED_BYTE, 4, 1);
  }
  
  public void resize(DwGLTexture src){
    resize(src.w, src.h, src.internalFormat, src.format, src.type, src.num_channel, src.byte_per_channel);
  }

  public void resize(int w, int h, int internal_format, int format, int type, int num_channels, int bbp){
    int wi = w;
    int hi = h;
    
    // 1) compute number of blur layers
    int layers_limit = Math.max(DwUtils.log2ceil(wi), DwUtils.log2ceil(hi)) >> 1;
    BLUR_LAYERS_MAX = Math.min(BLUR_LAYERS_MAX, layers_limit);
    BLUR_LAYERS     = Math.min(BLUR_LAYERS_MAX, BLUR_LAYERS);
    
    // 2) init/release textures if needed
    if(tex_blur.length < BLUR_LAYERS){
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
      tex_temp[i].resize(context, internal_format, wi, hi, format, type, GLES3.GL_LINEAR, num_channels, bbp);
      tex_blur[i].resize(context, internal_format, wi, hi, format, type, GLES3.GL_LINEAR, num_channels, bbp);
      tex_temp[i].setParam_WRAP_S_T(GLES3.GL_CLAMP_TO_EDGE);
      tex_blur[i].setParam_WRAP_S_T(GLES3.GL_CLAMP_TO_EDGE);
      wi = (int) Math.ceil(wi * 0.5f);
      hi = (int) Math.ceil(hi * 0.5f);
    }
  }
  

  public void apply(PGraphicsOpenGL src, int radius){
    resize(src);
   
    DwFilter.get(context).copy.apply(src, tex_blur[0]);
    
    context.begin();
    gaussblur.apply(tex_blur[0], tex_blur[0], tex_temp[0], 1 + radius);
    for(int i = 1; i < BLUR_LAYERS; i++){
      gaussblur.apply(tex_blur[i-1], tex_blur[i], tex_temp[i], i * 2 + radius);
    }
    context.end("GaussianPyramid.gaussian bluring");
  }
  
  
  public void apply(DwGLTexture src, int radius){
    resize(src);
   
    DwFilter.get(context).copy.apply(src, tex_blur[0]);
    
    context.begin();
    gaussblur.apply(tex_blur[0], tex_blur[0], tex_temp[0], 1 + radius);
    for(int i = 1; i < BLUR_LAYERS; i++){
      gaussblur.apply(tex_blur[i-1], tex_blur[i], tex_temp[i], i * 2 + radius);
    }
    context.end("GaussianPyramid.gaussian bluring");
  }

}
