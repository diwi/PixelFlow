/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.dwgl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * @author Thomas Diewald
 *
 */
public class DwGLTextureUtils {
  
  
  static public PGraphics2D changeTextureSize(PApplet papplet, PGraphics2D pg, int w, int h, int smooth, boolean[] resized)
  {   
    if(pg == null){
      pg = (PGraphics2D) papplet.createGraphics(w, h, PConstants.P2D);
      pg.smooth(smooth);
      resized[0] |= true;
    } else {
      resized[0] |= changeTextureSize(pg, w, h);
    }
    
    if(resized[0]){
      pg.loadTexture();
    }
    
    return pg;
  }
  
  static public PGraphics2D changeTextureSize(PApplet papplet, PGraphics2D pg, int w, int h, int smooth, boolean[] resized,  int internal_format, int format, int type)
  {   
    if(pg == null){
      pg = (PGraphics2D) papplet.createGraphics(w, h, PConstants.P2D);
      pg.smooth(smooth);
      resized[0] |= true;
    } else {
      resized[0] |= changeTextureSize(pg, w, h);
    }
    
    if(resized[0]){
      changeTextureFormat(pg, internal_format, format, type);
      pg.loadTexture();
    }

    return pg;
  }
  
  
  static public PGraphics3D changeTextureSize(PApplet papplet, PGraphics3D pg, int w, int h, int smooth, boolean[] resized)
  {   
    if(pg == null){
      pg = (PGraphics3D) papplet.createGraphics(w, h, PConstants.P3D);
      pg.smooth(smooth);
      resized[0] |= true;
    } else {
      resized[0] |= changeTextureSize(pg, w, h);
    }
    
    if(resized[0]){
      pg.loadTexture();
    }
    
    return pg;
  }
  
  static public PGraphics3D changeTextureSize(PApplet papplet, PGraphics3D pg, int w, int h, int smooth, boolean[] resized,  int internal_format, int format, int type)
  {   
    if(pg == null){
      pg = (PGraphics3D) papplet.createGraphics(w, h, PConstants.P3D);
      pg.smooth(smooth);
      resized[0] |= true;
    } else {
      resized[0] |= changeTextureSize(pg, w, h);
    }
    
    if(resized[0]){
      changeTextureFormat(pg, internal_format, format, type);
      pg.loadTexture();
    }

    return pg;
  }
  
  
  static public boolean changeTextureSize(PGraphicsOpenGL pg, int w, int h, int internal_format, int format, int type){
    boolean resize = changeTextureSize(pg, w, h);
    if(resize){
      changeTextureFormat(pg, internal_format, format, type);
    }
    return resize;
  }
  
  
  static public boolean changeTextureSize(PGraphicsOpenGL pg, int w, int h){
    if(pg.width == w && pg.height == h){
      return false;
    }
    
    // ... re-sizing is quite messy ...
    // TODO
    
    // restore that later
    int smooth = pg.smooth;
    
    // TODO check that
    // pg.removeCache(pg);
    
    // only way to release GL resources?
    pg.dispose(); 
    
    // TODO check that
    pg.setPrimary(false);
    
    // TODO check that
    pg.setParent(pg.parent);
    
    // possible leak:
    // --> texture = null;
    pg.setSize(w, h);
    // TODO check that

    // required, but is this supposed to be public?
    pg.initialized = false; 
    pg.smooth = smooth;
    return true;
  }
  
  
  static public void changeTextureFormat(PGraphicsOpenGL pg, int internal_format, int format, int type){
    changeTextureFormat(pg, internal_format, format, type, GL2ES2.GL_NEAREST);
  }
  
  static public void changeTextureFormat(PGraphicsOpenGL pg, int internal_format, int format, int type, int filter){
    changeTextureFormat(pg, internal_format, format, type, filter, GL2ES2.GL_CLAMP_TO_EDGE);
  }
  
  static public void changeTextureFormat(PGraphicsOpenGL pg, int internal_format, int format, int type, int filter, int wrap){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.changeTextureFormat: PGraphicsOpenGL texture not available.");
      return;
    }
    
    PGL pgl = pg.beginPGL();
    pgl.bindTexture  (tex.glTarget, tex.glName);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MIN_FILTER, filter); // GL_NEAREST, GL_LINEAR
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MAG_FILTER, filter); 
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_S, wrap);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_T, wrap);
    pgl.texImage2D   (tex.glTarget, 0, internal_format, tex.glWidth, tex.glHeight, 0, format, type, null);
    pgl.bindTexture  (tex.glTarget, 0);
    pg.endPGL();
    
    pg.beginDraw();
    pg.clear();
    pg.endDraw();
  }
  

  // GL_CLAMP
  // GL_CLAMP_TO_BORDER
  // GL_CLAMP_TO_EDGE
  // GL_REPEAT
  // GL_MIRRORED_REPEAT
  static public void changeTextureWrap(PGraphicsOpenGL pg, int wrap){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.changeTextureWrap: PGraphicsOpenGL texture not available.");
      return;
    }
    PGL pgl = pg.beginPGL();
    pgl.bindTexture  (tex.glTarget, tex.glName); 
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_S, wrap);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_T, wrap);
    pgl.bindTexture  (tex.glTarget, 0);
    pg.endPGL();
  }
  
  // GL_NEAREST
  // GL_LINEAR
  // GL_NEAREST_MIPMAP_NEAREST 
  // GL_LINEAR_MIPMAP_NEAREST  
  // GL_NEAREST_MIPMAP_LINEAR  
  // GL_LINEAR_MIPMAP_LINEAR  
  static public void changeTextureFilter(PGraphicsOpenGL pg, int min_filter, int mag_filter){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.changeTextureFilter: PGraphicsOpenGL texture not available.");
      return;
    }
    PGL pgl = pg.beginPGL();
    pgl.bindTexture  (tex.glTarget, tex.glName);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MIN_FILTER, min_filter);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MAG_FILTER, mag_filter);
    pgl.bindTexture  (tex.glTarget, 0);
    pg.endPGL();
  }
  
  
  /**
   * When chaning multiple parameters, its better to use this source-code directly.
   * 
   * @param pg
   * @param pname
   * @param param
   */
  static public void changeTextureParam(PGraphicsOpenGL pg, int pname, int param){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.changeTextureParam: PGraphicsOpenGL texture not available.");
      return;
    }
    PGL pgl = pg.beginPGL();
    pgl.bindTexture  (tex.glTarget, tex.glName);
    pgl.texParameteri(tex.glTarget, pname, param);
    pgl.bindTexture  (tex.glTarget, 0);
    pg.endPGL();
  }
  

  
  static public void generateMipMaps(PGraphicsOpenGL pg){
    Texture tex = pg.getTexture();
    if(!tex.available()){
      System.out.println("ERROR DwGLTextureUtils.generateMipMaps: PGraphicsOpenGL texture not available.");
      return;
    }

    PGL pgl = pg.beginPGL();
    pgl.bindTexture   (tex.glTarget, tex.glName);
    pgl.texParameteri (tex.glTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
//    pgl.texParameteri (tex.glTarget, GL2.GL_GENERATE_MIPMAP, GL.GL_TRUE);
    pgl.generateMipmap(tex.glTarget);
    pgl.bindTexture   (tex.glTarget, 0);
    pg.endPGL();
  }


  
  
  
  static public void copyMatrices(PGraphicsOpenGL src, PGraphicsOpenGL dst){
    dst.projection     = src.projection   .get();
    dst.camera         = src.camera       .get();
    dst.cameraInv      = src.cameraInv    .get();
    dst.modelview      = src.modelview    .get();
    dst.modelviewInv   = src.modelviewInv .get();
    dst.projmodelview  = src.projmodelview.get();
  }
  
  
  
  static public void setLookAt(PGraphicsOpenGL dst, float[] eye, float[] center, float[] up){
    dst.camera(
        eye   [0], eye   [1], eye   [2], 
        center[0], center[1], center[2], 
        up    [0], up    [1], up    [2]);
  }
  
  static public void setLookAt(PGraphicsOpenGL dst, PVector eye, PVector center, PVector up){
    dst.camera(
        eye   .x, eye   .y, eye   .z, 
        center.x, center.y, center.z, 
        up    .x, up    .y, up    .z);
  }
  
  
  static public void swap(PGraphics[] pg){
    PGraphics tmp = pg[0];
    pg[0] = pg[1];
    pg[1] = tmp;
  }
  
}
