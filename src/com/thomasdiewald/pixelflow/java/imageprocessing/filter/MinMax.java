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
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;



/**
 * 
 * 
 * Parallel reduction Min/Max filter.
 * 
 * Step size can be 2 or 4, where 4 turns out to be a little bit faster.
 * 
 * @author Thomas
 *
 */
public class MinMax {
  
  public static enum Mode{
    MIN, MAX
  }
  
  protected int STEP_SIZE = 4; // 2 or 4
  
  public DwPixelFlow context;
  
  public int layers = 0;
  public DwGLTexture[] tex = new DwGLTexture[layers];
  
  public DwGLSLProgram shader_min;
  public DwGLSLProgram shader_max;
  
  public MinMax(DwPixelFlow context){
    this.context = context;
    
    this.shader_min = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/reduction_min.frag");
    this.shader_max = context.createShader(this, DwPixelFlow.SHADER_DIR+"Filter/reduction_max.frag");
    
    shader_min.frag.setDefine("STEP_SIZE", STEP_SIZE);
    shader_max.frag.setDefine("STEP_SIZE", STEP_SIZE);
  }
  
  public void release(){
    for(int i = 0; i < tex.length; i++){
      if(tex[i] != null) tex[i].release();
    }
  }
  

  public void resize(DwGLTexture src){

    int w = src.w;
    int h = src.h;
    
    // 1) compute number of blur layers
    layers = Math.max(DwUtils.logNceil(STEP_SIZE, w), DwUtils.logNceil(STEP_SIZE, h)) + 1;
 
    // 2) init/release textures if needed
    if(tex.length < layers){
      release();
      tex = new DwGLTexture[layers];
      for(int i = 0; i < layers; i++){
        tex[i] = new DwGLTexture();
      }
    }

    // 3) allocate textures
    for(int i = 0; i < layers; i++){
      tex[i].resize(context, src, w, h);
      tex[i].setParam_WRAP_S_T(GLES3.GL_CLAMP_TO_EDGE);
//      System.out.println(i+", "+w+", "+h);
      w = (int) Math.ceil(w / (float) STEP_SIZE);
      h = (int) Math.ceil(h / (float) STEP_SIZE);
    }
  }
  

  public void apply(DwGLTexture src, MinMax.Mode mode){
    resize(src);
    
    DwFilter.get(context).copy.apply(src, tex[0]);
    
    DwGLSLProgram shader = (mode == Mode.MAX) ? shader_max : shader_min;
    
    context.begin();
    for(int i = 1; i < layers; i++){
//      System.out.println("PR: "+i+", "+tex[i].w+", "+tex[i].h);
      context.beginDraw(tex[i]);
      shader.begin();
      shader.uniform2f("wh_rcp", 1f/tex[i-1].w, 1f/tex[i-1].h);
      shader.uniformTexture("tex", tex[i-1]);
      shader.drawFullScreenQuad();
      shader.end();
      context.endDraw();
    }
    context.end("MinMax.apply");
  }
  
  
  public DwGLTexture getLatest(){
    return tex[layers-1];
  }
  
  
 
}
