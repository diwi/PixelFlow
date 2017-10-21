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

package com.thomasdiewald.pixelflow.java.antialiasing.GBAA;

import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGeometryShader;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.Texture;

public class GBAA {
  
  String dir = DwPixelFlow.SHADER_DIR+"antialiasing/GBAA/";
  
  DwPixelFlow context;
  public PApplet papplet;
  public PGraphics3D pg_edges;
  
  public DwSceneDisplay scene_display;
  
  public DwGeometryShader shader_edges;
  public DwGLSLProgram shader_edges1;
  public DwGLSLProgram shader_gbaa;
  
  public GBAA(DwPixelFlow context, DwSceneDisplay scene_display){
    this.context = context;
    this.papplet = context.papplet;
    this.scene_display = scene_display;

    String[] src_vert = context.utils.readASCIIfile(dir+"GBAA_edges.vert");
    String[] src_geom = context.utils.readASCIIfile(dir+"GBAA_edges.geom");
    String[] src_frag = context.utils.readASCIIfile(dir+"GBAA_edges.frag");

    this.shader_edges = new DwGeometryShader(papplet, src_vert, src_geom, src_frag);
    this.shader_gbaa = context.createShader(dir+"GBAA_blending.frag");
  }
  
  public boolean resize(int w, int h){
    boolean[] resized = {false};
    pg_edges = DwUtils.changeTextureSize(papplet, pg_edges, w, h, 0, resized, GL3.GL_RG16F, GL3.GL_RG, GL3.GL_FLOAT);
    return resized[0];
  }
  
  public int MODE = 0;

  public void apply(PGraphics3D src, PGraphics3D dst){
    
    int w = src.width;
    int h = src.height;
    
    resize(w, h);
    
    // 1) GeometryBuffer Pass
    pg_edges.beginDraw();
    DwUtils.copyMatrices(src, pg_edges);
    pg_edges.blendMode(PConstants.REPLACE);
    pg_edges.pgl.clearColor(0.5f, 0.5f, 0.5f, 0.5f);
    pg_edges.pgl.clear(PGL.COLOR_BUFFER_BIT);
    shader_edges.set("wh", (float)w, (float)h);
    pg_edges.shader(shader_edges);
    pg_edges.noStroke();
    scene_display.display(pg_edges);
    pg_edges.endDraw();
    

    
    // 2) AA Pass
    if(src == dst){
      System.out.println("GBAA error: read-write race");
      return;
    }
    
    Texture tex_src = src.getTexture(); if(!tex_src.available())  return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    Texture tex_edges = pg_edges.getTexture(); if(!tex_edges.available())  return;

    context.begin();
    context.beginDraw(dst);
    shader_gbaa.begin();
    shader_gbaa.uniform2f     ("wh_rcp" , 1f/w, 1f/h);
    shader_gbaa.uniformTexture("tex_src", tex_src.glName);
    shader_gbaa.uniformTexture("tex_edges", tex_edges.glName);
    shader_gbaa.drawFullScreenQuad();
    shader_gbaa.end();
    context.endDraw();
    context.end("GBAA.apply");
  
  }
  
}
