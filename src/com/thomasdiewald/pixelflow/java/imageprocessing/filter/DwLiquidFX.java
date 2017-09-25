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

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Sobel;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge.TexMad;

import processing.opengl.PGraphicsOpenGL;

/**
 * 
 * PostProcessing Effect for applying a liquid-like or plastic-like effect on 
 * an existing PGraphicsOpenGL canvas.<br>
 * <br>
 * The image must be a composition of a Foreground (opaque color) and 
 * a Background (transparent color).<br>
 * <br>
 * Effects include some sort of specular Highlights, SubSurfaceScattering,
 * and some blur + threshold fusion (similar to metaballs).
 * 
 * 
 * 
 * @author Thomas Diewald, Copyright(2017)
 *
 */
public class DwLiquidFX {

  static public class Param{
    public int        base_LoD           = 1;
    public int        base_blur_radius   = 2;
    public float      base_threshold     = 0.7f;
    public float      base_threshold_pow = 5;
    public boolean    highlight_enabled  = true;
    public float      highlight_decay    = 0.60f;
    public int        highlight_LoD      = 1;
    public Sobel.TYPE highlight_dir      = Sobel.TYPE._3x3_BLTR;
    public boolean    highlight_dir_inv  = true;
    
    public boolean    sss_enabled        = true;
    public float      sss_decay          = 0.70f;
    public int        sss_LoD            = 3;
    public Sobel.TYPE sss_dir            = Sobel.TYPE._3x3_VERT;
    public boolean    sss_dir_inv        = true;
  }
  
  // parameter
  public Param param = new Param();
  
  // pixelflow context
  public DwPixelFlow context;
  public DwFilter filter;
  
  // copy of the original texture, way faster to work with
  public DwGLTexture tex_particles = new DwGLTexture();
  
  public DwLiquidFX(DwPixelFlow context){
    this.context = context;
    this.filter = DwFilter.get(context);
  }
  
  protected final int SWIZZLE_R = GL2.GL_RED;
  protected final int SWIZZLE_G = GL2.GL_GREEN;
  protected final int SWIZZLE_B = GL2.GL_BLUE;
  protected final int SWIZZLE_A = GL2.GL_ALPHA;
  protected final int SWIZZLE_0 = GL2.GL_ZERO;
  protected final int SWIZZLE_1 = GL2.GL_ONE;

  protected int[] swizzle_RGBA = {SWIZZLE_R, SWIZZLE_G, SWIZZLE_B, SWIZZLE_A};
  protected int[] swizzle_AAA0 = {SWIZZLE_A, SWIZZLE_A, SWIZZLE_A, SWIZZLE_0};
  protected int[] swizzle_AAA1 = {SWIZZLE_A, SWIZZLE_A, SWIZZLE_A, SWIZZLE_1};
  protected int[] swizzle_000A = {SWIZZLE_0, SWIZZLE_0, SWIZZLE_0, SWIZZLE_A};

  protected float[] lo = {0,0,0,0}; // low clamp
  protected float[] hi = {5,5,5,5}; // high clamp
  
  public void apply(PGraphicsOpenGL pg_particles){
    apply(pg_particles, pg_particles);
  }
  
  
  public void apply(PGraphicsOpenGL pg_src, PGraphicsOpenGL pg_dst){
    int w = pg_src.width;
    int h = pg_src.height;
//    tex_particles.resize(context, GL2.GL_RGBA8, w, h, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, 4, 1);
    tex_particles.resize(context, GL2.GL_RGBA16F, w, h, GL2.GL_RGBA, GL2.GL_FLOAT, GL2.GL_LINEAR, 4, 2);
//    for(int i = 0; i < 50; i++){
    filter.copy.apply(pg_src, tex_particles);
    apply(tex_particles);
//    }
    filter.copy.apply(tex_particles, pg_dst);

  }
  
  
  public void apply(DwGLTexture tex_particles){
    apply(tex_particles, tex_particles);
  }
  
  
  public void apply(DwGLTexture tex_src, DwGLTexture tex_dst){
    
    context.begin();
    
    // get max required LoD level
    int lod_max = 0;
    lod_max = Math.max(lod_max, param.base_LoD     );
    lod_max = Math.max(lod_max, param.sss_LoD      );
    lod_max = Math.max(lod_max, param.highlight_LoD);
    
    // generate blur layers ... the real max LoD level is checked internally
    filter.gausspyramid.setBlurLayers(lod_max + 1);
    filter.gausspyramid.apply(tex_src, param.base_blur_radius);
    
    // get real max LoD level, make sure to not get any OOB
    lod_max = filter.gausspyramid.getNumBlurLayers() - 1;

    // correct max LoD levels
    param.base_LoD      = Math.min(lod_max, param.base_LoD     );
    param.sss_LoD       = Math.min(lod_max, param.sss_LoD      );
    param.highlight_LoD = Math.min(lod_max, param.highlight_LoD);
    
    // indices
    int LoD_base      = Math.max(0, param.base_LoD     );
    int LoD_highlight = Math.max(0, param.highlight_LoD);
    int LoD_sss1      = Math.max(0, param.sss_LoD      );
    int LoD_sss2      = Math.max(0, param.sss_LoD   - 1);
    
    
    // sobel mad args
    float[] highlight_dir_mad = {param.highlight_dir_inv ? -0.5f : 0.5f,0};
    float[] sss_dir_mad       = {param.sss_dir_inv       ? -0.5f : 0.5f,0};
    
    DwGLTexture tex_blur_base = filter.gausspyramid.tex_blur[LoD_base];
 
    // highlights
    if(param.highlight_enabled)
    {
      DwGLTexture tex_blur = filter.gausspyramid.tex_blur[LoD_highlight];
      DwGLTexture tex_edge = filter.gausspyramid.tex_temp[LoD_highlight];

//      // based on luminance edge
//      filter.sobel.apply(tex_blur, tex_edge, Sobel.TYPE._3x3_VERT, new float[]{-0.5f,0});
//      filter.luminance.apply(tex_edge, tex_edge);
//      filter.multiply.apply(tex_edge, tex_edge, new float[]{2,2,2,0});
//      filter.threshold.apply(tex_edge, tex_edge, new float[]{0.5f, 0.5f, 0.5f, 0.0f});
//      filter.merge.apply(tex_blur_base, tex_blur_base, tex_edge, mad_A, mad_B);
      
      //  based on alpha edge
      filter.sobel.apply(tex_blur, tex_edge, param.highlight_dir, highlight_dir_mad);
      filter.clamp.apply(tex_edge, tex_edge, lo, hi);
      tex_edge.swizzle(swizzle_000A);
      filter.threshold.param.threshold_val = new float[]{0, 0, 0, param.highlight_decay};
      filter.threshold.param.threshold_pow = new float[]{1, 1, 1, 5};
      filter.threshold.param.threshold_mul = new float[]{1, 1, 1, 1};
      filter.threshold.apply(tex_edge, tex_edge);
      tex_edge.swizzle(swizzle_AAA0);

      TexMad tm0 = new TexMad(tex_blur_base, 1, 0);
      TexMad tm1 = new TexMad(tex_edge     , 1, 0);
      filter.merge.apply(tex_blur_base, tm0, tm1);     
      
      tex_edge.swizzle(swizzle_RGBA);
    }

    filter.copy.apply(tex_blur_base, tex_dst);
    
    
    // sub-surface-scattering
    if(param.sss_enabled)
    {
      DwGLTexture tex_blur = filter.gausspyramid.tex_blur[LoD_sss1];
      DwGLTexture tex_edge = filter.gausspyramid.tex_temp[LoD_sss2];
      
      float add = param.sss_decay;
      float mul = 1.5f/(0.5f + add);
      float[] mad = new float[]{mul, add};
      
      filter.sobel.apply(tex_blur, tex_edge, param.sss_dir, sss_dir_mad);
      filter.clamp.apply(tex_edge, tex_edge, lo, hi);
      tex_edge.swizzle(swizzle_000A);
      filter.mad.apply(tex_edge, tex_edge, mad);
      tex_edge.swizzle(swizzle_AAA1);
      filter.multiply.apply(tex_dst, tex_dst, tex_edge);
      tex_edge.swizzle(swizzle_RGBA);
    }
    
    // cut border, smooth AA thresholding
    filter.threshold.param.threshold_val = new float[]{0, 0, 0, param.base_threshold};
    filter.threshold.param.threshold_pow = new float[]{1, 1, 1, param.base_threshold_pow};
    filter.threshold.param.threshold_mul = new float[]{1, 1, 1, 1};
    filter.threshold.apply(tex_dst, tex_dst);
    

    context.end();
  }
  
}
