/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
package com.thomasdiewald.pixelflow.java.dwgl;

import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.Texture;

/**
 * @author Thomas
 *
 */
public class DwGLTextureUtils {
  
  static public void changeTextureFormat(PGraphics3D pg, int internal_format, int format, int type){
    pg.loadTexture();
//    FrameBuffer fbo = pg.getFrameBuffer();
    Texture     tex = pg.getTexture();
    PGL pgl;
    pgl = pg.beginPGL();
    pgl.bindTexture(tex.glTarget, tex.glName);
    pgl.texImage2D (tex.glTarget, 0, internal_format, tex.glWidth, tex.glHeight, 0, format, type, null);
    pgl.bindTexture(tex.glTarget, 0);
    pg.endPGL();
  }
  
}
