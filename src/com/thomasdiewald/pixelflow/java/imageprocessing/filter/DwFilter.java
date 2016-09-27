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
  
  public final Copy            copy;
  public final Mix             mix;
  public final Add             add;
  public final Multiply        multiply;
  public final Luminance       luminance;
  public final BoxBlur         boxblur;
  public final GaussianBlur    gaussblur;
  public final MedianFilter    median;
  public final SobelFilter     sobel;
  public final Laplace         laplace;
  public final BilateralFilter bilateral;
  public final Convolution     convolution;
  public final DoG             dog;
  
  public DwFilter(DwPixelFlow context_){
    this.context = context_;
    
    copy        = new Copy            (context);
    mix         = new Mix             (context);
    add         = new Add             (context);
    multiply    = new Multiply        (context);
    luminance   = new Luminance       (context);
    boxblur     = new BoxBlur         (context);
    gaussblur   = new GaussianBlur    (context);
    median      = new MedianFilter    (context);
    sobel       = new SobelFilter     (context);
    laplace     = new Laplace         (context);
    bilateral   = new BilateralFilter (context);
    convolution = new Convolution     (context);
    dog         = new DoG             (context);
    
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
  
}
