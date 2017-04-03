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
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLShader.GLSLDefine;

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
  public DwGLTexture[] tex_blur_dst = new DwGLTexture[0];
  public DwGLTexture[] tex_blur_tmp = new DwGLTexture[0];
  public DwGLTexture   tex_luminance = null;
  
  // texture weights (merge pass)
  public float[] tex_weights;
  
  // merge shader
  public DwGLSLProgram shader_merge;
  
  
  public Bloom(DwPixelFlow context){
    this.context = context;
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
    for(int i = 0; i < tex_blur_dst.length; i++){
      if(tex_blur_tmp[i] != null) tex_blur_tmp[i].release();
      if(tex_blur_dst[i] != null) tex_blur_dst[i].release();
    }
    if(tex_luminance != null) tex_luminance.release();
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
    if(tex_blur_dst.length != BLUR_LAYERS){
//      System.out.println("Bloom BLUR_LAYERS: "+BLUR_LAYERS);
      release();
      tex_blur_dst = new DwGLTexture[BLUR_LAYERS];
      tex_blur_tmp = new DwGLTexture[BLUR_LAYERS];
      for(int i = 0; i < BLUR_LAYERS; i++){
        tex_blur_dst[i] = new DwGLTexture();
        tex_blur_tmp[i] = new DwGLTexture();
      }
      tex_luminance = new DwGLTexture();
    }

    
    // 3) allocate textures
    int internal_format = GLES3.GL_RGBA8;
    int format          = GLES3.GL_RGBA;
    int type            = GLES3.GL_UNSIGNED_BYTE;
    int filter          = GLES3.GL_LINEAR;
    int wrap            = GLES3.GL_CLAMP_TO_EDGE;
    
    tex_luminance.resize(context, internal_format, wi, hi, format, type, filter, 4, 1);
    tex_luminance.setParam_WRAP_S_T(wrap);
    
    for(int i = 0; i < BLUR_LAYERS; i++){
      tex_blur_tmp[i].resize(context, internal_format, wi, hi, format, type, filter, 4, 1);
      tex_blur_dst[i].resize(context, internal_format, wi, hi, format, type, filter, 4, 1);
      tex_blur_tmp[i].setParam_WRAP_S_T(wrap);
      tex_blur_dst[i].setParam_WRAP_S_T(wrap);
      
      wi >>= 1;
      hi >>= 1;
    }
    

    // 4) create/update shader
    if(shader_merge == null){
      shader_merge = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/bloom_merge.frag");
    }
    
    GLSLDefine define = shader_merge.frag.glsl_defines.get("BLUR_LAYERS");
    if(Integer.parseInt(define.value) != BLUR_LAYERS){
      define.value = ""+BLUR_LAYERS;
      shader_merge.build();
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
    
    int w = src.width;
    int h = src.height;
    
    // lazy init/allocation of all resources
    resize(w, h);
    
    // 1) luminance pass, use "src" as luminance
    {
      DwFilter.get(context).copy.apply(src, tex_luminance);
    }

    // 2) blur pass
    {
      DwGLTexture tex_blur_src = tex_luminance;
      for(int i = 0; i < BLUR_LAYERS; i++){
        int radius = i + 2;
        DwFilter.get(context).gaussblur.apply(tex_blur_src, tex_blur_dst[i], tex_blur_tmp[i], radius);
        tex_blur_src = tex_blur_dst[i];
      }
    }
    
   
    // 3) composition: merge blur-textures
    {
      tex_weights = computeWeights(tex_weights);

      context.begin();
      context.beginDraw(dst);
      
      // additive blend
      context.gl.glEnable(GLES3.GL_BLEND);
      context.gl.glBlendEquationSeparate(GLES3.GL_FUNC_ADD, GLES3.GL_FUNC_ADD);
      context.gl.glBlendFuncSeparate(GLES3.GL_SRC_ALPHA, GLES3.GL_ONE, GLES3.GL_ONE, GLES3.GL_ONE);
      
      shader_merge.begin();
      shader_merge.uniform1fv("tex_weights", BLUR_LAYERS, tex_weights);
      shader_merge.uniform2f ("wh_rcp"     , 1f/w,  1f/h);
      for(int i = 0; i < BLUR_LAYERS; i++){
        shader_merge.uniformTexture("tex_blur["+i+"]", tex_blur_dst[i]);
      }
      shader_merge.drawFullScreenQuad(0,0,w,h);
      shader_merge.end();
      
      context.endDraw();
      context.end("Bloom.apply");
    }
    
  }
  

}
