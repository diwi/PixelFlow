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

package com.thomasdiewald.pixelflow.java.antialiasing.SMAA;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * 
 * SMAA - Enhances Subpixel Morphological AntiAliasing
 * 
 * PostProcessing, 3 passes, nice quality
 * 
 * SMAA.h, Version 2.8: 
 * http://www.iryoku.com/smaa/
 * 
 * copyright-note, see SMAA.h:
 *   Uses SMAA. Copyright (C) 2011 by Jorge Jimenez, Jose I. Echevarria,
 *   Tiago Sousa, Belen Masia, Fernando Navarro and Diego Gutierrez.
 * 
 * 
 * 
 * @author Thomas Diewald
 *
 */
public class SMAA {

  static public class Param {
    // no params
  }
  
  public DwPixelFlow context;
  
  public Param param = new Param();
  
  public DwGLSLProgram shader_edges;
  public DwGLSLProgram shader_blend;
  public DwGLSLProgram shader_smaa;
  
  public DwGLTexture tex_edges  = new DwGLTexture();
  public DwGLTexture tex_blend  = new DwGLTexture();
  
  private DwGLTexture tex_area   = new DwGLTexture();
  private DwGLTexture tex_search = new DwGLTexture();
  
  public SMAA(DwPixelFlow context){
    this.context = context;
    
    String dir = DwPixelFlow.SHADER_DIR+"antialiasing/SMAA/";
    this.shader_edges = context.createShader(dir+"smaa1_edges.vert", dir+"smaa1_edges.frag");
    this.shader_blend = context.createShader(dir+"smaa2_blend.vert", dir+"smaa2_blend.frag");
    this.shader_smaa  = context.createShader(dir+"smaa3_final.vert", dir+"smaa3_final.frag");
    
    ByteBuffer bbuffer_tex_search = readTexData(dir+"TexSearch_66x33_R8.bin" , new byte[ 66 *  33 * 1]);
    ByteBuffer bbuffer_tex_area   = readTexData(dir+"TexArea_160x560_RG8.bin", new byte[160 * 560 * 2]);
   
    tex_search.resize(context, GL2ES2.GL_R8 ,  66,  33, GL2ES2.GL_RED, GL2ES2.GL_UNSIGNED_BYTE, GL2ES2.GL_NEAREST, 1, 1, bbuffer_tex_search);
    tex_area  .resize(context, GL2ES2.GL_RG8, 160, 560, GL2ES2.GL_RG , GL2ES2.GL_UNSIGNED_BYTE, GL2ES2.GL_NEAREST, 2, 1, bbuffer_tex_area);
  }
  
  public void resize(int w, int h){
    tex_edges.resize(context, GL2ES2.GL_RGBA8, w, h, GL2ES2.GL_RGBA, GL2ES2.GL_UNSIGNED_BYTE, GL2ES2.GL_LINEAR, 4, 1);
    tex_blend.resize(context, GL2ES2.GL_RGBA8, w, h, GL2ES2.GL_RGBA, GL2ES2.GL_UNSIGNED_BYTE, GL2ES2.GL_LINEAR, 4, 1);
  }
  
  
  private ByteBuffer readTexData(String path, byte[] data){
    InputStream is = new DwUtils(context).createInputStream(path);
    try {
      is.read(data);
      is.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return ByteBuffer.wrap(data);
  }
  
  
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst) {
    if(src == dst){
      System.out.println("MSAA error: read-write race");
      return;
    }
    
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;

    int w = src.width;
    int h = src.height;
    
    resize(w, h);
     
    context.begin();

    // PASS 1 - Edges
    context.beginDraw(tex_edges);
    context.gl.glClearColor(0,0,0,0);
    context.gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT);
    shader_edges.begin();
    shader_edges.uniform2f     ("wh_rcp"   , 1f/w, 1f/h);
    shader_edges.uniformTexture("tex_color", tex_src.glName);
    shader_edges.drawFullScreenQuad();
    shader_edges.end();
    context.endDraw("smaa - pass1");
    
    // PASS 2 - blend
    context.beginDraw(tex_blend);
    context.gl.glClearColor(0,0,0,0);
    context.gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT);
    shader_blend.begin();
    shader_blend.uniform2f     ("wh_rcp"   , 1f/w, 1f/h);
    shader_blend.uniformTexture("tex_edges", tex_edges .HANDLE[0]);
    shader_blend.uniformTexture("tex_area" , tex_area  .HANDLE[0]);
    shader_blend.uniformTexture("tex_search",tex_search.HANDLE[0]);
    shader_blend.drawFullScreenQuad();
    shader_blend.end();
    context.endDraw("smaa - pass2");
    
    // PASS 3 - blend
    context.beginDraw(dst);
    context.gl.glClearColor(0,0,0,0);
    context.gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT);
    shader_smaa.begin();
    shader_smaa.uniform2f     ("wh_rcp"   , 1f/w, 1f/h);
    shader_smaa.uniformTexture("tex_color", tex_src.glName);
    shader_smaa.uniformTexture("tex_blend", tex_blend.HANDLE[0]);
    shader_smaa.drawFullScreenQuad();
    shader_smaa.end();
    context.endDraw("smaa - pass3");

    context.end("SMAA.apply");
  }
  

 
}
