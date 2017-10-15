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
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;



/**
 * 
 * 
 * Parallel reduction Min/Max filter.
 * 
 * Step size can be 2, 3 or 4, where 4 turns out to be a little bit faster.
 * 
 * @author Thomas
 *
 */
public class MinMaxGlobal {
  

  protected int STEP_SIZE = 4; // 2,3,4
  
  public DwPixelFlow context;
  
  public int layers = 0;
  public DwGLTexture[] tex = new DwGLTexture[layers];
  
  public DwGLSLProgram shader_min;
  public DwGLSLProgram shader_max;
  public DwGLSLProgram shader_map_v1; // per texel
  public DwGLSLProgram shader_map_v2; // per channel
  
  public MinMaxGlobal(DwPixelFlow context){
    this.context = context;
    
    String path = DwPixelFlow.SHADER_DIR+"Filter/";
    
    shader_min = context.createShader((Object)(this+"_MIN"), path+"MinMaxGlobal.frag");
    shader_max = context.createShader((Object)(this+"_MAX"), path+"MinMaxGlobal.frag");
    
    shader_min.frag.setDefine("MODE_MIN", 1);
    shader_max.frag.setDefine("MODE_MAX", 1);
    
    shader_min.frag.setDefine("STEP_SIZE", STEP_SIZE);
    shader_max.frag.setDefine("STEP_SIZE", STEP_SIZE);
    
    shader_map_v1 = context.createShader((Object)(this+"_MAP_V1"), path+"MinMaxGlobal_Map.frag");
    shader_map_v2 = context.createShader((Object)(this+"_MAP_V2"), path+"MinMaxGlobal_Map.frag");
    
    shader_map_v1.frag.setDefine("PER_CHANNEL", 0);
    shader_map_v2.frag.setDefine("PER_CHANNEL", 1);
  }
  
  public void release(){
    for(int i = 0; i < tex.length; i++){
      if(tex[i] != null){
        tex[i].release();
        tex[i] = null;
      }
    }
  }
  
  
  public void resize(DwGLTexture tex){
    resize(tex.internalFormat, tex.w, tex.h, tex.format, tex.type, tex.num_channel, tex.byte_per_channel);
  }
  
  public void resize(PGraphicsOpenGL pg){
    resize(GL2.GL_RGBA8, pg.width, pg.height, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, 4, 1);
  }
  
  public void resize(int iformat, int w, int h, int format, int type, int num_channel, int byte_per_channel){

    // 1) compute number of blur layers
    layers = Math.max(DwUtils.logNceil(w, STEP_SIZE), DwUtils.logNceil(h, STEP_SIZE)) + 1;

    // 2) init/release textures if needed
    if(tex.length != layers){
      release();
      tex = new DwGLTexture[layers];
      for(int i = 0; i < layers; i++){
        tex[i] = new DwGLTexture();
      }
    } 

    // 3) allocate textures
    for(int i = 0; i < layers; i++){
      if(i == layers-1){
        w = 2;
        h = 1;
      }
      tex[i].resize(context, iformat, w, h, format, type, GL2.GL_NEAREST, GL2.GL_MIRRORED_REPEAT, num_channel, byte_per_channel);
      w = (int) Math.ceil(w / (float) STEP_SIZE);
      h = (int) Math.ceil(h / (float) STEP_SIZE);
    }
  }

 
  public void apply(DwGLTexture tex_src){
    apply(tex_src, true, true);
  }
  
  public void apply(PGraphicsOpenGL pg_src){
    apply(pg_src, true, true);
  }
  
  public void apply(PGraphicsOpenGL pg_src, boolean MIN, boolean MAX){
    if(!MIN && !MAX) return;
    resize(pg_src);
    DwFilter.get(context).copy.apply(pg_src, tex[0]);
    apply(MIN, MAX);
  }
  
  public void apply(DwGLTexture tex_src, boolean MIN, boolean MAX){
    if(!MIN && !MAX) return;
    resize(tex_src);
    DwFilter.get(context).copy.apply(tex_src, tex[0]);
    apply(MIN, MAX);
  }
  

  private void apply(boolean MIN, boolean MAX){
    context.begin(); 
    if(MIN) run(shader_min, 0);
    if(MAX) run(shader_max, 1);
    context.end();
  }
  
  /**
   * <pre>
   *  min ... frag[0,0] 
   *  max ... frag[1,0] 
   * </pre>
   * @param shader
   * @param ox frag offset x
   * @param oy frag offset y
   */
  private void run(DwGLSLProgram shader, int ox){
    shader.begin();
    for(int i = 1; i < layers; i++){
      DwGLTexture dst = tex[i];
      DwGLTexture src = tex[i-1];
      context.beginDraw(dst);
      if(i == layers-1){
        shader.scissors(ox, 0, 1, 1); 
        shader.uniform2i("off", ox, 0);
      } else {
        shader.uniform2i("off", 0, 0);
      }
      shader.uniform2f("wh_rcp", 1f/src.w, 1f/src.h);
      shader.uniformTexture("tex", src);
      shader.drawFullScreenQuad();
      context.endDraw("MinMaxGlobal.run");
    }
    shader.end();
  }
  
  
  
  
 
  /**
   * 
   * remap pixels [min, max] to [0, 1]
   * 
   */
  public void map(DwGLTexture tex){
    map(tex, tex, false);
  }
  
  /**
   * 
   * remap pixels [min, max] to [0, 1]
   * 
   */
  public void map(DwGLTexture tex_src, DwGLTexture tex_dst){
    map(tex_src, tex_dst, false);
  }

  /**
   * 
   * remap pixels [min, max] to [0, 1]
   * 
   */
  public void map(DwGLTexture tex_src, DwGLTexture tex_dst, boolean per_channel){
    context.begin();
    context.beginDraw(tex_dst);
    map(tex_dst.w, tex_dst.h, tex_src.HANDLE[0], per_channel);
    context.endDraw();
    context.end("MinMaxGlobal.map");
  }

  /**
   * 
   * remap pixels [min, max] to [0, 1]
   * 
   */
  public void map(PGraphicsOpenGL pg){
    map(pg, pg, false);
  }

  /**
   * 
   * remap pixels [min, max] to [0, 1]
   * 
   */
  public void map(PGraphicsOpenGL pg_src, PGraphicsOpenGL pg_dst){
    map(pg_src, pg_dst, false);
  }

  /**
   * 
   * remap pixels [min, max] to [0, 1]
   * 
   */
  public void map(PGraphicsOpenGL pg_src, PGraphicsOpenGL pg_dst, boolean per_channel){
    Texture tex_src = pg_src.getTexture(); if(!tex_src.available()) return;
    context.begin();
    context.beginDraw(pg_dst);
    map(pg_dst.width, pg_dst.height, tex_src.glName, per_channel);
    context.endDraw();
    context.end("MinMaxGlobal.map");
  }
  
  
  protected void map(int w, int h, int handle_src, boolean per_channel){
    DwGLSLProgram shader = per_channel ? shader_map_v2 : shader_map_v1;
    shader.begin();
    shader.uniform2f("wh_rcp", 1f/w, 1f/h);
    shader.uniformTexture("tex_src", handle_src);
    shader.uniformTexture("tex_minmax", getVal());
    shader.drawFullScreenQuad();
    shader.end();
  }
  
  
  
  
  
  
  
  
  /**
   *  
   * @return  A two-pixel texture, where pixel(0,0) has the min-value and pixel(1,0) the max-value.
   * 
   */
  public DwGLTexture getVal(){
    return tex[layers-1];
  }
  
  
 
}
