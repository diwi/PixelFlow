/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package com.thomasdiewald.pixelflow.java.dwgl;


import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.glu.GLU;

public class DwGLError {
  
  public static boolean DEBUG_OUT = false;
  
  public static final GLU glu = new GLU();
  public static int ERROR_CODE = 0;
  
  public static boolean debug(GL2ES2 gl, String debug_note) {
    ERROR_CODE = gl.glGetError();
    if (DEBUG_OUT) System.out.println("--------------------------<  ERROR_CHECK >--------------------------( "+debug_note+" )");
    boolean has_error = (ERROR_CODE != GL2ES2.GL_NO_ERROR);
    if (has_error ){
      System.out.println(debug_note+" | GL_ERROR: "+glu.gluErrorString(ERROR_CODE));
    }
    if (DEBUG_OUT) System.out.println("--------------------------< /ERROR_CHECK >--------------------------");
    return has_error;
  }
  
  
  public static boolean FBO(GL2ES2 gl, int[] handle_fbo){
    int rval = ERROR_CODE = gl.glCheckFramebufferStatus(handle_fbo[0]);
    System.out.println("glCheckFramebufferStatus = "+rval);
    if( rval == GL2ES2.GL_FRAMEBUFFER_COMPLETE ){
      return true;
    } else {
      
      
      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS         ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS        ");
      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS            ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_FORMATS           ");
//      if( rval == GL2ES2.GL_FRAMEBUFFER_UNDEFINED                     ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_UNDEFINED                    ");
      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT         ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT        ");
      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
//      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER        ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER       ");
//      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER        ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER       ");
      if( rval == GL2ES2.GL_FRAMEBUFFER_UNSUPPORTED                   ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_UNSUPPORTED                  ");
      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE        ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE       ");
      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE        ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE       ");
//      if( rval == GL2ES2.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS      ) System.out.println("FBO-ERROR: GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS     ");
      System.out.println("FBO-ERROR: unknown errorcode");
    }
    
    return false;
  }
}
