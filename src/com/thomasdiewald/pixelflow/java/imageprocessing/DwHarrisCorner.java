/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.imageprocessing;


import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Sobel;
import processing.opengl.PGraphics2D;;

public class DwHarrisCorner {
  
  static public class Param {
    public int     blur_input  = 1;
    public int     blur_harris = 3;
    public int     blur_final  = 2;
    public float   scale       = 500;
    public float   sensitivity = 0.12f; // parameter K: 0.04 - 0.15
    public boolean nonMaxSuppression = !true;
  }
  
  public DwPixelFlow context;
  
  public Param param = new Param();
  
  public Frame frameCurr = new Frame();
  
  private DwGLSLProgram shader_harrisMatrix;
  private DwGLSLProgram shader_harrisCorner;
  private DwGLSLProgram shader_maximumSupp ;
  private DwGLSLProgram shader_render      ;
  
  public DwHarrisCorner(DwPixelFlow context){
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    String path1 = DwPixelFlow.SHADER_DIR+"HarrisCornerDetection/";
    String path2 = DwPixelFlow.SHADER_DIR+"Filter/";
    
    shader_harrisMatrix = context.createShader(path1+"harrisMatrix.frag");
    shader_harrisCorner = context.createShader(path1+"harrisCorner.frag");
    shader_maximumSupp  = context.createShader(path2+"LocalNonMaxSuppression.frag");
    shader_render       = context.createShader(path1+"harrisRender.frag");
  }

  public void dispose(){
    release();
  }
  
  public void release(){
    frameCurr.release();
  }
  
  public void reset(){
    clear(0.0f);
  }
  
  private void clear(float v){
    context.begin();
    frameCurr.clear(v);
    context.end("HarrisCorner.clear");
  }
  

  public void update(PGraphics2D pg_curr) {
    
    int w = pg_curr.width;
    int h = pg_curr.height;

    // 0) resize(w/h) or reformat(rgba/grayscale)
    frameCurr.resize(context, w, h);
    
    DwFilter filter = DwFilter.get(context);
    
    // 1) grayscale
    filter.luminance.apply(pg_curr, frameCurr.frame);

    // 2) blur
    filter.gaussblur.apply(frameCurr.frame, frameCurr.frame, frameCurr.tmp, param.blur_input);

    // 3) gradients
    filter.sobel.apply(frameCurr.frame, frameCurr.sobelH, Sobel.TYPE._3x3_HORZ); 
    filter.sobel.apply(frameCurr.frame, frameCurr.sobelV, Sobel.TYPE._3x3_VERT); 
    
    // 4) harrisMatrix
    context.begin();
    context.beginDraw(frameCurr.harrisMatrix);
    shader_harrisMatrix.begin();
    shader_harrisMatrix.uniform2f     ("wh"    , w, h);
    shader_harrisMatrix.uniformTexture("tex_dx", frameCurr.sobelH);
    shader_harrisMatrix.uniformTexture("tex_dy", frameCurr.sobelV);
    shader_harrisMatrix.drawFullScreenQuad();
    shader_harrisMatrix.end();
    context.endDraw();
    context.end("HarrisCorner.harrisMatrix");
    
    // 5) blur harrisMatrix
    filter.gaussblur.apply(frameCurr.harrisMatrix, frameCurr.harrisMatrix, frameCurr.tmp, param.blur_harris);
    
    // 6) harrisCorner
    context.begin();
    context.beginDraw(frameCurr.harrisCorner);
    shader_harrisCorner.begin();
    shader_harrisCorner.uniform2f     ("wh"     , w, h);
    shader_harrisCorner.uniform1f     ("scale"  , param.scale);
    shader_harrisCorner.uniform1f     ("harrisK", param.sensitivity);
    shader_harrisCorner.uniformTexture("tex_harrisMatrix", frameCurr.harrisMatrix);
    shader_harrisCorner.drawFullScreenQuad();
    shader_harrisCorner.end();
    context.endDraw();
    context.end("HarrisCorner.HarrisCorner");
    
    if(param.nonMaxSuppression){
      // 6) Local Non Maximum Suppression
      context.begin();
      context.beginDraw(frameCurr.tmp);
      shader_maximumSupp.begin();
      shader_maximumSupp.uniform2f     ("wh"              , w, h);
      shader_maximumSupp.uniformTexture("tex_harrisCorner", frameCurr.harrisCorner);
      shader_maximumSupp.drawFullScreenQuad();
      shader_maximumSupp.end();
      context.endDraw();
      context.end("HarrisCorner.nonMaxSuppression");
      
      // copy back
      filter.copy.apply(frameCurr.tmp, frameCurr.harrisCorner);
    }
    
    filter.gaussblur.apply(frameCurr.harrisCorner, frameCurr.harrisCorner, frameCurr.tmp, param.blur_final);

  }
  
  
  public void render(PGraphics2D dst){

    int w = dst.width;
    int h = dst.height;
    
    context.begin();
    context.beginDraw(dst);
    context.gl.glEnable(GL2.GL_BLEND);
    context.gl.glBlendEquationSeparate(GL2.GL_FUNC_ADD, GL2.GL_FUNC_ADD);
    context.gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ONE);
    shader_render.begin();
    shader_render.uniform2f     ("wh"              , w, h);
    shader_render.uniformTexture("tex_harrisCorner", frameCurr.harrisCorner);
    shader_render.drawFullScreenQuad();
    shader_render.end();
    context.endDraw();
    context.end("HarrisCorner.render");
  }

  
  
  
  
  public static class Frame{
    public DwPixelFlow context;
    public DwGLTexture frame        = new DwGLTexture();
    public DwGLTexture sobelH       = new DwGLTexture();
    public DwGLTexture sobelV       = new DwGLTexture();  
    public DwGLTexture harrisMatrix = new DwGLTexture();
    public DwGLTexture harrisCorner = new DwGLTexture();
    public DwGLTexture tmp          = new DwGLTexture();
    
    protected int w, h;
    
    public Frame(){
    }
    
    public void release(){
      tmp         .release();
      frame       .release();
      sobelH      .release();
      sobelV      .release();  
      harrisMatrix.release();  
      harrisCorner.release();
    }
    
    public void clear(float v){
      context.begin();
      frame       .clear(v);
      sobelH      .clear(v);
      sobelV      .clear(v);
      harrisMatrix.clear(v);  
      harrisCorner.clear(v);  
      tmp         .clear(v);
      context.end();
    }
    
    public boolean resize(DwPixelFlow context_, int w, int h){

      this.context = context_;
      this.w = w;
      this.h = h;
        
      context.begin();
      boolean resized = false;
      resized |= frame       .resize(context, GL2.GL_R16F   , w, h, GL2.GL_RED , GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 1,4);
      resized |= sobelH      .resize(context, GL2.GL_R16F   , w, h, GL2.GL_RED , GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 1,4);
      resized |= sobelV      .resize(context, GL2.GL_R16F   , w, h, GL2.GL_RED , GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 1,4);
      resized |= harrisMatrix.resize(context, GL2.GL_RGB16F , w, h, GL2.GL_RGB , GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 3,4);
      resized |= harrisCorner.resize(context, GL2.GL_R16F   , w, h, GL2.GL_RED , GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 1,4);
      resized |= tmp         .resize(context, GL2.GL_RGBA16F, w, h, GL2.GL_RGBA, GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4,4);
      context.end();
      return resized;
    }

  }
  
  
  
   
 
}
