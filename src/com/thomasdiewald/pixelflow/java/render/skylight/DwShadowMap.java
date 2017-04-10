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







package com.thomasdiewald.pixelflow.java.render.skylight;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureUtils;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PMatrix3D;
import processing.core.PVector;
import processing.opengl.PGraphics3D;
import processing.opengl.PShader;


public class DwShadowMap {
  
  String dir = DwPixelFlow.SHADER_DIR+"render/skylight/";
  
  DwPixelFlow context;
  public PApplet papplet;
  
  public PShader shader_shadow;
  public PGraphics3D pg_shadowmap;
  
  public PMatrix3D mat_scene_bounds;
  
  public PVector lightdir = new PVector();

  public DwSceneDisplay scene_display;
  
  public DwShadowMap(DwPixelFlow context, int size, DwSceneDisplay scene_display, PMatrix3D mat_scene_bounds){
    this.context = context;
    this.papplet = context.papplet;
    this.mat_scene_bounds = mat_scene_bounds;
    
    String[] src_frag = context.utils.readASCIIfile(dir+"shadowmap.frag");
    String[] src_vert = context.utils.readASCIIfile(dir+"shadowmap.vert");

    this.shader_shadow = new PShader(papplet, src_vert, src_frag);
    
//    this.shader_shadow = papplet.loadShader(dir+"shadowmap.frag", dir+"shadowmap.vert");
    this.scene_display = scene_display;
    
    resize(size);
  }
  

  public void resize(int wh){
    pg_shadowmap = (PGraphics3D) papplet.createGraphics(wh, wh, PConstants.P3D);
    pg_shadowmap.smooth(0);
    pg_shadowmap.textureSampling(2);
    
    DwGLTextureUtils.changeShadowTextureFormat(pg_shadowmap, GL2.GL_R32F, GL2.GL_RED, GL2.GL_FLOAT);

    pg_shadowmap.beginDraw();
    pg_shadowmap.hint(PConstants.DISABLE_TEXTURE_MIPMAPS);
    
    pg_shadowmap.background(0xFFFFFFFF);
    pg_shadowmap.blendMode(PConstants.REPLACE);
    pg_shadowmap.shader(shader_shadow);
    pg_shadowmap.noStroke();
    pg_shadowmap.endDraw();
    
    setOrtho();
  }
  
  
  public void update(){
    pg_shadowmap.beginDraw();
    pg_shadowmap.background(0xFFFFFFFF);
    pg_shadowmap.noStroke();
    pg_shadowmap.applyMatrix(mat_scene_bounds);
    scene_display.display(pg_shadowmap);
    pg_shadowmap.endDraw();
  }
  


  public void setOrtho(){
    pg_shadowmap.ortho(-1, 1,-1, 1, 0, 2);
  }
  
  public void setPerspective(float fovy){
    pg_shadowmap.perspective(fovy, 1, 1, 2);
  }
  
  public void setDirection(float[] eye, float[] center, float[] up){
    DwGLTextureUtils.setLookAt(pg_shadowmap, eye, center, up);
    lightdir.set(eye);
  }
  public void setDirection(PVector eye, PVector center, PVector up){
    DwGLTextureUtils.setLookAt(pg_shadowmap, eye, center, up);
    lightdir.set(eye);
  }
  
  public PMatrix3D getShadowmapMatrix(){
    // 1) create shadowmap matrix, 
    //    to transform positions from camera-space to the shadowmap-space (light-space)
    PMatrix3D mat_shadow = new PMatrix3D();
    // ndc (shadowmap) -> normalized (shadowmap) 
    //         [-1,+1] -> [0,1]
    mat_shadow.scale(0.5f);
    mat_shadow.translate(1,1,1);

    // model (world) -> modelview (shadowmap) -> ndc (shadowmap)
    mat_shadow.apply(pg_shadowmap.projmodelview);
    
    return mat_shadow;
  }
  
  public PMatrix3D getModelView(){
    pg_shadowmap.updateProjmodelview();
    return pg_shadowmap.modelview;
  }
  
  public PMatrix3D getProjection(){
    pg_shadowmap.updateProjmodelview();

    // 1) create shadowmap matrix, 
    //    to transform positions from camera-space to the shadowmap-space (light-space)
    PMatrix3D mat_shadow = new PMatrix3D();
    // ndc (shadowmap) -> normalized (shadowmap) 
    //         [-1,+1] -> [0,1]
    mat_shadow.scale(0.5f);
    mat_shadow.translate(1,1,1);

    // model (world) -> modelview (shadowmap) -> ndc (shadowmap)
    mat_shadow.apply(pg_shadowmap.projection);
    
    return mat_shadow;
  }
  

  
}
