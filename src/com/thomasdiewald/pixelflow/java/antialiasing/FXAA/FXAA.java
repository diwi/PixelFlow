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

package com.thomasdiewald.pixelflow.java.antialiasing.FXAA;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * 
 * FXAA - Fast Approximate AntiAliasing
 * 
 * PostProcessing, 1 pass, good quality - a bit blurry sometimes
 * 
 * created "by Timothy Lottes under NVIDIA", FXAA_WhitePaper.pdf
 * FXAA3_11.h, Version 3.11: 
 * https://docs.nvidia.com/gameworks/content/gameworkslibrary/graphicssamples/opengl_samples/fxaa.htm
 * 
 * 
 * copyright-note, see FXAA3_11.h:
 *   Uses FXAA. Copyright (c) 2014-2015, NVIDIA CORPORATION.
 *  
 * @author Thomas Diewald
 *
 */
public class FXAA {

 
  
  static public class Param {
    // Amount of sub-pixel aliasing removal. Can effect sharpness.
    //   1.00 - upper limit (softer)
    //   0.75 - default amount of filtering
    //   0.50 - lower limit (sharper, less sub-pixel aliasing removal)
    //   0.25 - almost off
    //   0.00 - completely off
    public float QualitySubpix = 0.75f;

    // The minimum amount of local contrast required to apply algorithm.
    //   0.333 - too little (faster)
    //   0.250 - low quality
    //   0.166 - default
    //   0.125 - high quality 
    //   0.063 - overkill (slower)
    public float QualityEdgeThreshold = 0.125f;

    // Trims the algorithm from processing darks.
    //   0.0833 - upper limit (default, the start of visible unfiltered edges)
    //   0.0625 - high quality (faster)
    //   0.0312 - visible limit (slower)
    // Special notes when using FXAA_GREEN_AS_LUMA,
    //   Likely want to set this to zero.
    //   As colors that are mostly not-green
    //   will appear very dark in the green channel!
    //   Tune by looking at mostly non-green content,
    //   then start at zero and increase until aliasing is a problem.
    public float QualityEdgeThresholdMin = 0.0f;
  }
  
  
  public DwPixelFlow context;
  
  public Param param = new Param();
  
  
  
  public FXAA(DwPixelFlow context){
    this.context = context;
    
    this.shader = context.createShader(DwPixelFlow.SHADER_DIR+"antialiasing/FXAA/FXAA.frag");
  }
  

  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst) {
    if(src == dst){
      System.out.println("FXAA error: read-write race");
      return;
    }
    
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    
    // RGBL ... red, green, blue, luminance, for FXAA
    DwFilter.get(context).rgbl.apply(src, src);
       
    context.begin();
    setLinearTextureFiltering(tex_src.glName);
    context.beginDraw(dst);
    apply(tex_src.glName, dst.width, dst.height);
    context.endDraw();
    resetTextureFiltering(tex_src.glName);
    context.end("FXAA.apply");
  }
  
  
  DwGLSLProgram shader;
  public void apply(int tex_handle, int w, int h){
//    if(shader == null) shader = context.createShader(DwPixelFlow.SHADER_DIR+"antialiasing/FXAA/FXAA.frag");
    shader.begin();
    shader.uniform2f     ("wh_rcp" , 1f/w, 1f/h);
    shader.uniformTexture("tex", tex_handle);
    shader.uniform1f     ("QualitySubpix"          , param.QualitySubpix          );
    shader.uniform1f     ("QualityEdgeThreshold"   , param.QualityEdgeThreshold   );
    shader.uniform1f     ("QualityEdgeThresholdMin", param.QualityEdgeThresholdMin);
    shader.drawFullScreenQuad();
    shader.end();
  }
  
  
  
  
  
  protected int[] filter_minmag = new int[2];
  protected int tex_target = GL2.GL_TEXTURE_2D;
  protected void setLinearTextureFiltering(int handle_tex){
    context.begin();
    context.gl.glBindTexture      (tex_target, handle_tex);
    context.gl.glGetTexParameteriv(tex_target, GL2ES2.GL_TEXTURE_MIN_FILTER, filter_minmag, 0);
    context.gl.glGetTexParameteriv(tex_target, GL2ES2.GL_TEXTURE_MAG_FILTER, filter_minmag, 1);
    context.gl.glTexParameteri    (tex_target, GL2ES2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
    context.gl.glTexParameteri    (tex_target, GL2ES2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
    context.gl.glBindTexture      (tex_target, 0);
    context.end();
  }
  
  protected void resetTextureFiltering(int handle_tex){
    context.begin();
    context.gl.glBindTexture      (tex_target, handle_tex);
    context.gl.glTexParameteri    (tex_target, GL2ES2.GL_TEXTURE_MIN_FILTER, filter_minmag[0]);
    context.gl.glTexParameteri    (tex_target, GL2ES2.GL_TEXTURE_MAG_FILTER, filter_minmag[1]);
    context.gl.glBindTexture      (tex_target, 0);
    context.end();
  }
  
 
}
