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
import com.thomasdiewald.pixelflow.java.DwPixelFlow;

import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * @author Thomas
 *
 */
public class DwGLTextureUtils {
  
  static public void changeTextureFormat(PGraphicsOpenGL pg, int internal_format, int format, int type){
    pg.loadTexture();
//    FrameBuffer fbo = pg.getFrameBuffer();
    Texture     tex = pg.getTexture();
    PGL pgl;
    pgl = pg.beginPGL();
    pgl.bindTexture(tex.glTarget, tex.glName);

    
//    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_S, GL2ES2.GL_CLAMP_TO_BORDER);
//    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_T, GL2ES2.GL_CLAMP_TO_BORDER);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST); // GL_NEAREST, GL_LINEAR
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
    
//    
//    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_COMPARE_MODE, GL2ES2.GL_COMPARE_REF_TO_TEXTURE);
//    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_COMPARE_FUNC, GL2ES2.GL_LEQUAL);
//    pgl.texParameteri(tex.glTarget, GL2.GL_DEPTH_TEXTURE_MODE, GL2.GL_INTENSITY); 
    pgl.texImage2D(tex.glTarget, 0, internal_format, tex.glWidth, tex.glHeight, 0, format, type, null);

    pgl.bindTexture(tex.glTarget, 0);
    pg.endPGL();
  }
  
  
  static public void changeShadowTextureFormat(PGraphicsOpenGL pg, int internal_format, int format, int type){
    pg.loadTexture();
//    FrameBuffer fbo = pg.getFrameBuffer();
    Texture     tex = pg.getTexture();
    PGL pgl;
    pgl = pg.beginPGL();
    pgl.bindTexture(tex.glTarget, tex.glName);
    
//    float[] border =  new float[]{0,0,0,0};

  
//    pgl.texParameterfv(tex.glTarget, GL2ES2.GL_TEXTURE_BORDER_COLOR, FloatBuffer.wrap(border));
//    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_S, GL2ES2.GL_CLAMP_TO_BORDER);
//    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_T, GL2ES2.GL_CLAMP_TO_BORDER);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_S, GL2ES2.GL_CLAMP_TO_EDGE);
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_WRAP_T, GL2ES2.GL_CLAMP_TO_EDGE);
    
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST); // GL_NEAREST, GL_LINEAR
    pgl.texParameteri(tex.glTarget, GL2ES2.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
    
//    pgl.texParameteri(tex.glTarget, GL2.GL_TEXTURE_COMPARE_MODE, GL2.GL_COMPARE_REF_TO_TEXTURE);
//    pgl.texParameteri(tex.glTarget, GL2.GL_TEXTURE_COMPARE_FUNC, GL2.GL_LEQUAL);
//    pgl.texParameteri(tex.glTarget, GL2.GL_DEPTH_TEXTURE_MODE, GL2.GL_INTENSITY); 
    
    
    pgl.texImage2D(tex.glTarget, 0, internal_format, tex.glWidth, tex.glHeight, 0, format, type, null);

    pgl.bindTexture(tex.glTarget, 0);
    pg.endPGL();
  }
  
  
  static public void generateMipMaps(DwPixelFlow context, PGraphicsOpenGL pg){
    Texture tex = pg.getTexture();
//    tex.usingMipmaps(true, 3);
//    System.out.println(tex.usingMipmaps());
    context.begin();
    context.gl.glBindTexture   (GL2ES2.GL_TEXTURE_2D, tex.glName);
    context.gl.glTexParameteri (GL2ES2.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
//    context.gl.glTexParameteri (GL2ES2.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_NEAREST);
    context.gl.glGenerateMipmap(GL2ES2.GL_TEXTURE_2D);
    context.gl.glBindTexture   (GL2ES2.GL_TEXTURE_2D, 0);
    context.end();
  }
  
  
  static public void copyMatrices(PGraphics3D src, PGraphics3D dst){
    dst.projection     = src.projection   .get();
    dst.camera         = src.camera       .get();
    dst.cameraInv      = src.cameraInv    .get();
    dst.modelview      = src.modelview    .get();
    dst.modelviewInv   = src.modelviewInv .get();
    dst.projmodelview  = src.projmodelview.get();
  }
  
  static public void setLookAt(PGraphics3D dst, float[] eye, float[] center, float[] up){
    dst.camera(
        eye   [0], eye   [1], eye   [2], 
        center[0], center[1], center[2], 
        up    [0], up    [1], up    [2]);
  }
  
  static public void setLookAt(PGraphics3D dst, PVector eye, PVector center, PVector up){
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
