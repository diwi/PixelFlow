/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
 * 
 * src - www.github.com/diwi/PixelFlow
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
 * @author Thomas Diewald<br>
 * 
 * 
 * resources:<br>
 * <br>
 * 1) http://prideout.net/archive/bloom/<br>
 *
 * 2) https://threejs.org/examples/webgl_postprocessing_unreal_bloom.html<br>
 *    three.js/examples/js/postprocessing/UnrealBloomPass.js <br>
 *    
 * 3) http://www.gamasutra.com/view/feature/130520/realtime_glow.php<br>
 *
 */
public class Bloom {
  
  public static class Param{
    public int   blur_radius = 2;
    public float mult        = 1f;   // [0, whatever]
    public float radius      = 0.5f; // [0, 1]
  }
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
  public GaussianBlurPyramid gaussianpyramid;
  
  // texture weights (merge pass)
  public float[] tex_weights;
  
  // Filter for merging the textures
  public Merge merge;
  
  
  public Bloom(DwPixelFlow context){
    this.context = context;
    this.merge = new Merge(context);
    this.gaussianpyramid = new GaussianBlurPyramid(context);
  }
  
  public void setBlurLayersMax(int BLUR_LAYERS_MAX){
    gaussianpyramid.setBlurLayersMax(BLUR_LAYERS_MAX);
  }
  
  public void setBlurLayers(int BLUR_LAYERS){
    gaussianpyramid.setBlurLayers(BLUR_LAYERS);
  }
  
  public int getNumBlurLayers(){
    return gaussianpyramid.getNumBlurLayers();
  }
  
  public void release(){
    gaussianpyramid.release();
  }
  


  private float[] computeWeights(float[] weights){
    
    int BLUR_LAYERS = gaussianpyramid.getNumBlurLayers();
    
    if(weights == null || weights.length != BLUR_LAYERS*2){
      weights = new float[BLUR_LAYERS*2];
    }
    
    float step = 1f / BLUR_LAYERS;
    for(int i = 0; i < BLUR_LAYERS; i++){
      float fac = 1f - step * i;
      float weight = DwUtils.mix(fac, 1.0f + step - fac, param.radius);
      weights[i*2 + 0] = param.mult * weight;
      weights[i*2 + 1] = 0;
    }
    return weights;
  }
  

  
  
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
  public void apply(DwGLTexture src_luminance, DwGLTexture dst_bloom, DwGLTexture dst_composition){

    // 1) create blur levels
    gaussianpyramid.apply(src_luminance, param.blur_radius);
    
    // 2) compute blur-texture weights
    tex_weights = computeWeights(tex_weights);

    // 3a) merge + blend: dst_bloom is not null, therefore the extra pass
    if(dst_bloom != null){
      //merge.apply(dst_bloom, gaussianpyramid.tex_blur, tex_weights);
      mergeBlurLayers(dst_bloom);
      if(dst_composition != null){
        context.pushRenderSettings(additive_blend);
        DwFilter.get(context).copy.apply(dst_bloom, dst_composition);
        context.popRenderSettings();
      }
      return;
    }
    
    // 3b) merge + blend:  dst_bloom is null, so we merge + blend into dst_composition
    context.pushRenderSettings(additive_blend);
    //merge.apply(dst_composition, gaussianpyramid.tex_blur, tex_weights);
    mergeBlurLayers(dst_composition);
    context.popRenderSettings();
  }
  
  
  
  
  
  
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

    // 1) create blur levels
    gaussianpyramid.apply(src_luminance, param.blur_radius);
    
    // 2) compute blur-texture weights
    tex_weights = computeWeights(tex_weights);

    // 3a) merge + blend: dst_bloom is not null, therefore the extra pass
    if(dst_bloom != null){
      //merge.apply(dst_bloom, gaussianpyramid.tex_blur, tex_weights);
      mergeBlurLayers(dst_bloom);
      if(dst_composition != null){
        context.pushRenderSettings(additive_blend);
        DwFilter.get(context).copy.apply(dst_bloom, dst_composition);
        context.popRenderSettings();
      }
      return;
    }
    
    // 3b) merge + blend:  dst_bloom is null, so we merge + blend into dst_composition
    context.pushRenderSettings(additive_blend);
    //merge.apply(dst_composition, gaussianpyramid.tex_blur, tex_weights);
    mergeBlurLayers(dst_composition);
    context.popRenderSettings();
  }
  
  
  private Merge.TexMad[] alloc(){
    int num_layers = getNumBlurLayers();
    Merge.TexMad[] tm = new Merge.TexMad[num_layers];
    
    for(int i = 0; i < num_layers; i++){
      tm[i] = new Merge.TexMad(gaussianpyramid.getTexture(i), tex_weights[i*2+0], tex_weights[i*2+1]);
    }
    return tm;
  }
  
  private void mergeBlurLayers(PGraphicsOpenGL dst){
    merge.apply(dst, alloc());
  }
  
  private void mergeBlurLayers(DwGLTexture dst){
    merge.apply(dst, alloc());
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
