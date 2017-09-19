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

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * 
 * The Merge pass has several useful applications.
 * 
 * e.g. mixing/adding two or more textures based on weights (plus an additional scalar)
 * 
 * 
 * result_A = tex_A * mult_A + add_A
 * result_B = tex_B * mult_B + add_B
 * result_N = tex_N * mult_N + add_N
 * 
 * result = result_A + result_B + ... + result_N
 * 
 * 
 * where mult_A and add_A is called "mad_A" in the code ... "multiply and add"
 * 
 * 
 * e.g. this can be useful for the DoG operator where the rgb-values become negative
 * due to the difference [-mult, +mult] and instead of abs(-rgb) the values are shifted
 * by +0.5 (e.g. for 8bit unsigned byte textures)
 * mostly however, the "add_N" component will just be 0.0.
 * 
 * 
 * 
 * 
 * @author Thomas Diewald
 *
 */
public class Merge {
  public DwPixelFlow context;
  
  public DwGLSLProgram shader_merge_array;
  public DwGLSLProgram shader_merge_two;
  
  public Merge(DwPixelFlow context){
    this.context = context;
  }
  
  public void apply(DwGLTexture dst, DwGLTexture[] tex_src, float[] tex_weights){
    if(tex_src.length > tex_weights.length) return;
    
    int TEX_LAYERS = tex_src.length;
    int w = dst.w;
    int h = dst.h;
    int[] tex_handles = new int[TEX_LAYERS];
    for(int i = 0; i < TEX_LAYERS; i++){
      tex_handles[i] = tex_src[i].HANDLE[0];
    }

    context.begin();
    context.beginDraw(dst);
    apply(w, h, tex_handles, tex_weights);
    context.endDraw();
    context.end("Merge.apply");
  }
  

  public void apply(PGraphicsOpenGL dst, DwGLTexture[] tex_src, float[] tex_mad){
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    if(tex_src.length*2 != tex_mad.length) return;
    
    int TEX_LAYERS = tex_src.length;
    int w = tex_dst.glWidth;
    int h = tex_dst.glHeight;
    int[] tex_handles = new int[TEX_LAYERS];
    for(int i = 0; i < TEX_LAYERS; i++){
      tex_handles[i] = tex_src[i].HANDLE[0];
    }

    context.begin();
    context.beginDraw(dst);
    apply(w, h, tex_handles, tex_mad);
    context.endDraw();
    context.end("Merge.apply");
  }
  
  
  public void apply(PGraphicsOpenGL dst, PGraphicsOpenGL[] tex_src, float[] tex_mad){
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    if(tex_src.length*2 != tex_mad.length) return;
    
    int w = tex_dst.glWidth;
    int h = tex_dst.glHeight;
    int TEX_LAYERS = tex_src.length;
    int[] tex_handles = new int[TEX_LAYERS];
    for(int i = 0; i < TEX_LAYERS; i++){
      Texture tex_src_ = tex_src[i].getTexture();
      if(!tex_src_.available()) return;
      tex_handles[i] = tex_src_.glName;
    }
    
    context.begin();
    context.beginDraw(dst);
    apply(w, h, tex_handles, tex_mad);
    context.endDraw();
    context.end("Merge.apply");
  }
  
  
  public void apply(PGraphicsOpenGL dst, PGraphicsOpenGL src_A, PGraphicsOpenGL src_B, float[] mad_A,float[] mad_B){
    Texture tex_dst = dst  .getTexture(); if(!tex_dst.available())  return;
    Texture tex_A   = src_A.getTexture(); if(!tex_A  .available())  return;
    Texture tex_B   = src_B.getTexture(); if(!tex_B  .available())  return;
    
    int w = tex_dst.glWidth;
    int h = tex_dst.glHeight;

    context.begin();
    context.beginDraw(dst);
    apply(w, h, tex_A.glName, tex_B.glName, mad_A, mad_B);
    context.endDraw();
    context.end("Merge.apply");
  }
  
  public void apply(DwGLTexture dst, DwGLTexture src_A, DwGLTexture src_B, float[] mad_A, float[] mad_B){
    int w = dst.w;
    int h = dst.h;

    context.begin();
    context.beginDraw(dst);
    apply(w, h, src_A.HANDLE[0], src_B.HANDLE[0], mad_A, mad_B);
    context.endDraw();
    context.end("Merge.apply");
  }
  

  private void apply(int w, int h, int[] tex_handles, float[] tex_mad){
    int TEX_LAYERS = tex_handles.length;
    if(shader_merge_array == null){
      shader_merge_array = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/merge_array.frag");
    }
    shader_merge_array.frag.setDefine("TEX_LAYERS", TEX_LAYERS);
    shader_merge_array.begin();
    shader_merge_array.uniform2f ("wh_rcp" , 1f/w,  1f/h);
    shader_merge_array.uniform2fv("tex_mad", TEX_LAYERS, tex_mad);
    for(int i = 0; i < TEX_LAYERS; i++){
      shader_merge_array.uniformTexture("tex_src["+i+"]", tex_handles[i]);
    }
    shader_merge_array.drawFullScreenQuad(0,0,w,h);
    shader_merge_array.end();
  }
  
  
  private void apply(int w, int h, int tex_A, int tex_B, float[] mad_A, float[] mad_B){
    if(shader_merge_two == null){
      shader_merge_two = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/merge_two.frag");
    }
    shader_merge_two.begin();
    shader_merge_two.uniform2f     ("wh_rcp", 1f/w,  1f/h);
    shader_merge_two.uniform2f     ("mad_A", mad_A[0], mad_A[1]);
    shader_merge_two.uniform2f     ("mad_B", mad_B[0], mad_B[1]);
    shader_merge_two.uniformTexture("tex_A", tex_A);
    shader_merge_two.uniformTexture("tex_B", tex_B);
    shader_merge_two.drawFullScreenQuad(0,0,w,h);
    shader_merge_two.end();
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  public void apply(PGraphicsOpenGL dst, TexMad ... tex){
    context.begin();
    context.beginDraw(dst);
    apply(dst.width, dst.height, tex);
    context.endDraw();
    context.end("Merge.apply");
  }
  
  public void apply(DwGLTexture dst, TexMad ... tex){
    context.begin();
    context.beginDraw(dst);
    apply(dst.w, dst.h, tex);
    context.endDraw();
    context.end("Merge.apply");
  }
  
  public void apply(DwGLTexture dst, TexMad texA, TexMad texB){
    context.begin();
    context.beginDraw(dst);
    apply(dst.w, dst.h, texA, texB);
    context.endDraw();
    context.end("Merge.apply");
  }
  
  public void apply(PGraphicsOpenGL dst, TexMad texA, TexMad texB){
    context.begin();
    context.beginDraw(dst);
    apply(dst.width, dst.height, texA, texB);
    context.endDraw();
    context.end("Merge.apply");
  }
  
  
  private void apply(int w, int h, TexMad ... tex){
    if(shader_merge_array == null){
      shader_merge_array = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/merge_array.frag");
    }
    shader_merge_array.frag.setDefine("TEX_LAYERS", tex.length);
    shader_merge_array.begin();
    shader_merge_array.uniform2f ("wh_rcp" , 1f/w,  1f/h);
    for(int i = 0; i < tex.length; i++){
      shader_merge_array.uniform2f     ("tex_mad["+i+"]", tex[i].mul, tex[i].add);
      shader_merge_array.uniformTexture("tex_src["+i+"]", tex[i].tex);
    }
    shader_merge_array.drawFullScreenQuad(0,0,w,h);
    shader_merge_array.end();
  }
  
  private void apply(int w, int h, TexMad texA, TexMad texB){
    if(shader_merge_two == null){
      shader_merge_two = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/merge_two.frag");
    }
    shader_merge_two.begin();
    shader_merge_two.uniform2f     ("wh_rcp" , 1f/w,  1f/h);
    shader_merge_two.uniform2f     ("mad_A", texA.mul, texA.add);
    shader_merge_two.uniform2f     ("mad_B", texB.mul, texB.add);
    shader_merge_two.uniformTexture("tex_A", texA.tex);
    shader_merge_two.uniformTexture("tex_B", texB.tex);
    shader_merge_two.drawFullScreenQuad(0,0,w,h);
    shader_merge_two.end();
  }
  
  
  
  
  static public class TexMad {
    public int   tex = 0;
    public float mul = 0;
    public float add = 0;
    
    public TexMad(DwGLTexture tex, float mul, float add){
      this.tex = tex.HANDLE[0];
      this.mul = mul;
      this.add = add;
    }
    public TexMad(PGraphicsOpenGL pg, float mul, float add){
      Texture tex = pg.getTexture(); if(!tex.available()) return;
      this.tex = tex.glName;
      this.mul = mul;
      this.add = add;
    }
  }
  
  
  
}
