/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.imageprocessing.filter;

import java.util.HashMap;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;

public class DwFilter {

  public DwPixelFlow context;
  
  public final Copy               copy;
  public final Mix                mix;
  public final TextureMerge       tex_merge;
  public final Add                add;
  public final Multiply           multiply;
  public final Luminance          luminance;
  public final BoxBlur            boxblur;
  public final GaussianBlur       gaussblur;
  public final MedianFilter       median;
  public final SobelFilter        sobel;
  public final Laplace            laplace;
  public final BilateralFilter    bilateral;
  public final Convolution        convolution;
  public final DoG                dog;
  public final GammaCorrection    gamma;
  public final RGBL               rgbl;
  public final BinomialBlur       binomial;
  public final SummedAreaTable    summedareatable;
  public final Bloom              bloom;
  public final LuminanceThreshold luminance_threshold;
  
  
  public DwFilter(DwPixelFlow context_){
    this.context = context_;
    
    this.context.papplet.registerMethod("dispose", this);
    
    copy                = new Copy              (context);
    mix                 = new Mix               (context);
    tex_merge           = new TextureMerge      (context);
    add                 = new Add               (context);
    multiply            = new Multiply          (context);
    luminance           = new Luminance         (context);
    boxblur             = new BoxBlur           (context);
    gaussblur           = new GaussianBlur      (context);
    median              = new MedianFilter      (context);
    sobel               = new SobelFilter       (context);
    laplace             = new Laplace           (context);
    bilateral           = new BilateralFilter   (context);
    convolution         = new Convolution       (context);
    dog                 = new DoG               (context);
    gamma               = new GammaCorrection   (context);
    rgbl                = new RGBL              (context);
    binomial            = new BinomialBlur      (context);
    summedareatable     = new SummedAreaTable   (context);
    bloom               = new Bloom             (context);
    luminance_threshold = new LuminanceThreshold(context);
    
    
    filter_cache.put(context, this);
  }
  
  private static HashMap<DwPixelFlow, DwFilter> filter_cache = new HashMap<DwPixelFlow, DwFilter>();
  
  static public DwFilter get(DwPixelFlow context){
    DwFilter filter = filter_cache.get(context);
    if(filter == null){
      filter = new DwFilter(context);
      filter_cache.put(context, filter);
    }
    return filter;
  }
  
  public void dispose(){
    release();
  }
  public void release(){
//    copy           .release();
//    mix            .release();
//    add            .release();
//    multiply       .release();
//    luminance      .release();
//    boxblur        .release();
//    gaussblur      .release();
//    median         .release();
//    sobel          .release();
//    laplace        .release();
//    bilateral      .release();
//    convolution    .release();
//    dog            .release();
//    gamma          .release();
//    rgbl           .release();
//    binomial       .release();
    summedareatable.release();
    bloom.release();
  }
  
}
