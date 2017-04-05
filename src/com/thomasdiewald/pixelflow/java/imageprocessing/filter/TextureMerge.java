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
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLShader.GLSLDefine;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * @author Thomas
 *
 */
public class TextureMerge {
  public DwPixelFlow context;
  
  public DwGLSLProgram shader;
  
  public TextureMerge(DwPixelFlow context){
    this.context = context;
  }
  
  
  private void updateShader(int TEX_LAYERS){
    // 4) create/update shader
    if(shader == null){
      shader = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/texture_merge.frag");
    }
    
    // update shader
    GLSLDefine define = shader.frag.glsl_defines.get("TEX_LAYERS");
    if(Integer.parseInt(define.value) != TEX_LAYERS){
      define.value = ""+TEX_LAYERS;
      shader.build();
    }
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
    
    // create/update shader
    updateShader(TEX_LAYERS);
    
    // merge
    context.begin();
    context.beginDraw(dst);
    apply(w, h, tex_handles, tex_weights);
    context.endDraw();
    context.end("TextureMerge.apply");
  }
  

  public void apply(PGraphicsOpenGL dst, DwGLTexture[] tex_src, float[] tex_weights){
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    if(tex_src.length > tex_weights.length) return;
    
    int TEX_LAYERS = tex_src.length;
    int w = tex_dst.glWidth;
    int h = tex_dst.glHeight;
    int[] tex_handles = new int[TEX_LAYERS];
    for(int i = 0; i < TEX_LAYERS; i++){
      tex_handles[i] = tex_src[i].HANDLE[0];
    }
    
    // create/update shader
    updateShader(TEX_LAYERS);
    
    // merge
    context.begin();
    context.beginDraw(dst);
    apply(w, h, tex_handles, tex_weights);
    context.endDraw();
    context.end("TextureMerge.apply");
  }
  
  
  public void apply(PGraphicsOpenGL dst, PGraphicsOpenGL[] tex_src, float[] tex_weights){
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    if(tex_src.length > tex_weights.length) return;
    
    int TEX_LAYERS = tex_src.length;
    int w = tex_dst.glWidth;
    int h = tex_dst.glHeight;
    int[] tex_handles = new int[TEX_LAYERS];
    for(int i = 0; i < TEX_LAYERS; i++){
      Texture tex_src_ = tex_src[i].getTexture();
      if(!tex_src_.available()) return;
      tex_handles[i] = tex_src_.glName;
    }
    
    // create/update shader
    updateShader(TEX_LAYERS);
    
    // merge
    context.begin();
    context.beginDraw(dst);
    apply(w, h, tex_handles, tex_weights);
    context.endDraw();
    context.end("TextureMerge.apply");
  }
  

  private void apply(int w, int h, int[] tex_handles, float[] tex_weights){
    int TEX_LAYERS = tex_handles.length;
    
    // additive blend
    context.gl.glEnable(GLES3.GL_BLEND);
    context.gl.glBlendEquationSeparate(GLES3.GL_FUNC_ADD, GLES3.GL_FUNC_ADD);
    context.gl.glBlendFuncSeparate(GLES3.GL_SRC_ALPHA, GLES3.GL_ONE, GLES3.GL_ONE, GLES3.GL_ONE);
    
    shader.begin();
    shader.uniform1fv("tex_weights", TEX_LAYERS, tex_weights);
    shader.uniform2f ("wh_rcp"     , 1f/w,  1f/h);
    for(int i = 0; i < TEX_LAYERS; i++){
      shader.uniformTexture("tex_src["+i+"]", tex_handles[i]);
    }
    shader.drawFullScreenQuad(0,0,w,h);
    shader.end();
  }
  
  
}
