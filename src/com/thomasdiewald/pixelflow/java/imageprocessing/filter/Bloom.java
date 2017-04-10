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
import com.thomasdiewald.pixelflow.java.dwgl.DwGLRenderSettingsCallback;
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
  public Merge tex_merge;
  
  
  public Bloom(DwPixelFlow context){
    this.context = context;
    this.tex_merge = new Merge(context);
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
    if(weights == null || weights.length != BLUR_LAYERS*2){
      weights = new float[BLUR_LAYERS*2];
    }
    
    float step = 1f / BLUR_LAYERS;
    for(int i = 0; i < BLUR_LAYERS; i++){
      float fac = 1f - step * i;
      float weight = DwUtils.mix(fac, 1.0f + step - fac, param.radius);
      weights[i*2 + 0] = param.mult * weight;
      weights[i*2 + 1] = 0;

//      weights[i] = param.mult * step;
    }
    return weights;
  }
  
  /**
   * 
   * "src_luminance" serves as the source texture for the bloom pass.
   * 
   * "dst_bloom" is the merged result of several iterations of gaussian-blurs.
   * 
   * "dst_bloom" still needs to be additively blended with the main source texture.
   * 
   * apply(src_luminance, dst_bloom, dst_composition) can be called instead.
   * 
   * 
   * @param src_luminance
   * @param dst_bloom
   */
//  private void apply(PGraphicsOpenGL src_luminance, PGraphicsOpenGL dst_bloom){
//    Texture tex_src = src_luminance.getTexture(); if(!tex_src.available())  return;
//    Texture tex_dst = dst_bloom    .getTexture(); if(!tex_dst.available())  return;
//    
//    // lazy init/allocation
//    resize(tex_dst.glWidth, tex_dst.glHeight);
//    
//    // 1) use src-texture as base for blur-levels
//    DwFilter.get(context).copy.apply(src_luminance, tex_blur[0]);
//    
//    // 2) blur passes
//    for(int i = 1; i < BLUR_LAYERS; i++){
//      DwFilter.get(context).gaussblur.apply(tex_blur[i-1], tex_blur[i], tex_temp[i], i + 2);
//    }
//    
//    // 3) compute blur-texture weights
//    tex_weights = computeWeights(tex_weights);
//    
//    // 4) merge blur-textures
//    tex_merge.apply(dst_bloom, tex_blur, tex_weights);
//  }
  
  /**
   * 
   * "src_luminance" serves as the source texture for the bloom pass.
   * e.g this texture can be the result of a brightness-prepass on dst_composition.
   *  
   * "dst_bloom" is the merged result of several iterations of gaussian-blurs.
   * 
   * "dst_composition" is the final result of additive blending with "dst_bloom".
   * 
   * 
   * @param src_luminance
   * @param dst_bloom
   * @param dst_composition
   */
  public void apply(PGraphicsOpenGL src_luminance, PGraphicsOpenGL dst_bloom, PGraphicsOpenGL dst_composition){
//    if(dst_bloom == null){
//      return;
//    }
//    
//      // compute bloom, based on src_luminance
//    apply(src_luminance, dst_bloom);
//    
//    if(dst_composition == null){
//      return;
//    }
//    
//    if(dst_composition == dst_bloom){
//      System.out.println("Bloom.apply WARNING: dst_composition == dst_bloom");
//    }
//    
//    int w = dst_composition.width;
//    int h = dst_composition.height;
//
//    // additive blend: dst_composition +  dst_bloom
//    dst_composition.beginDraw();
//    dst_composition.pushStyle();
//    dst_composition.pushMatrix();
//    dst_composition.pushProjection();
//    
//    dst_composition.resetMatrix();
//    if(dst_composition.is3D()){
//      dst_composition.ortho(0, w, -h, 0, 0, 1);
//    }
//
//    dst_composition.hint(PConstants.DISABLE_DEPTH_TEST);
//    dst_composition.blendMode(PConstants.ADD);
//    dst_composition.image(dst_bloom, 0, 0);
//    dst_composition.hint(PConstants.ENABLE_DEPTH_TEST);
//    
//    dst_composition.popProjection();
//    dst_composition.popMatrix();
//    dst_composition.popStyle();
//    dst_composition.endDraw();
    
    int w = 0;
    int h = 0;
    
    if(dst_composition != null){
      w = dst_composition.width;
      h = dst_composition.height;
    } else if(dst_bloom != null){
      w = dst_bloom.width;
      h = dst_bloom.height;
    } else {
      return;
    }

    
    DwFilter filter = DwFilter.get(context);
    
    // 0) lazy init/allocation
    resize(w, h);
   
    // 1) use src-texture as base for blur-levels
    filter.copy.apply(src_luminance, tex_blur[0]);
    
    // 2) blur passes
    context.begin();
    filter.gaussblur.apply(tex_blur[0], tex_blur[0], tex_temp[0], 3);
    for(int i = 1; i < BLUR_LAYERS; i++){
      filter.gaussblur.apply(tex_blur[i-1], tex_blur[i], tex_temp[i], i * 2 + 2);
    }
    context.end("Bloom.gaussian bluring");
    
    // 3) compute blur-texture weights
    tex_weights = computeWeights(tex_weights);
    
    
    // 4a) merge + blend: dst_bloom is not null, therefore the extra pass
    if(dst_bloom != null){
      tex_merge.apply(dst_bloom, tex_blur, tex_weights);
      if(dst_composition != null){
        context.pushRenderSettings(additive_blend);
        filter.copy.apply(dst_bloom, dst_composition);
        context.popRenderSettings();
      }
      return;
    }
    
    // 4b) merge + blend:  dst_bloom is null, so we merge + blend into dst_composition
    context.pushRenderSettings(additive_blend);
    tex_merge.apply(dst_composition, tex_blur, tex_weights);
    context.popRenderSettings();
    
    


  }
  
  /**
   * applies the bloom directly on dst
   * @param dst
   */
  public void apply(PGraphicsOpenGL dst){
    apply(dst, null, dst);
  }
  
  
  
  DwGLRenderSettingsCallback additive_blend = new DwGLRenderSettingsCallback() {
    @Override
    public void set(DwPixelFlow context, int x, int y, int w, int h) {
      context.gl.glEnable(GLES3.GL_BLEND);
      context.gl.glBlendEquationSeparate(GLES3.GL_FUNC_ADD, GLES3.GL_FUNC_ADD);
      context.gl.glBlendFuncSeparate(GLES3.GL_SRC_ALPHA, GLES3.GL_ONE, GLES3.GL_ONE, GLES3.GL_ONE);
    }
  };
  
  

}
