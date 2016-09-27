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
import com.jogamp.opengl.GLProfile;
  
public class DwGLInfo {
  
  
  static public int getActiveProgram(GL2ES2 gl){
    int[] currentProgram = new int[1];
    gl.glGetIntegerv(GL2ES2.GL_CURRENT_PROGRAM, currentProgram, 0);
    return currentProgram[0];
  }

  
  static public void getProfiles(){
    System.out.println("--------------------------------------------------------------------------------");
    System.out.println("GLProfile.GL_PROFILE_LIST_ALL");
    String[] profilelist = GLProfile.GL_PROFILE_LIST_ALL;
    for(int i = 0; i < profilelist.length; i++){
      System.out.printf("  [%2d] %6s available: %4s\n",i, profilelist[i], GLProfile.isAvailable(profilelist[i]));
    }
    System.out.println("--------------------------------------------------------------------------------");
  }
  
  //http://www.opengl.org/sdk/docs/man/xhtml/glGet.xml
  //http://www.opengl.org/wiki/GLAPI/glGetIntegerv
  static public void getInfoOpenglGL4(GL2ES2 gl){

    String version    = gl.glGetString(GL2ES2.GL_VERSION);
    String vendor     = gl.glGetString(GL2ES2.GL_VENDOR);
    String renderer   = gl.glGetString(GL2ES2.GL_RENDERER);
    String glsl       = gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION);
//    String extensions = gl.glGetString(GL4.GL_EXTENSIONS);
    
    System.out.println("--------------------------------------------------------------------------------");
    System.out.println("OPENGL_VERSION:    " + version          );
    System.out.println("OPENGL_VENDOR:     " + vendor           );
    System.out.println("OPENGL_RENDERER:   " + renderer         );
    System.out.println("GLSL_VErSION:      " + glsl             );
//    System.out.println("GL_EXTENSIONS: " + extensions       );
    System.out.println("--------------------------------------------------------------------------------");

//    StringBuilder sb = JoglVersion.getGLInfo(gl, new StringBuilder(), !true);
//    System.out.println(sb);
  }
  
  
  static public void getInfoOpenglExtensions(GL2ES2 gl){
    System.out.println("--------------------------------------------------------------------------------");
    
    String extensions = gl.glGetString(GL2ES2.GL_EXTENSIONS);
    String[] tokens = extensions.split("\\s+");
    System.out.println("EXTENSIONS: "+tokens.length);
    for(int i = 0; i < tokens.length; i++){
      System.out.printf("[%4d] %s\n", i, tokens[i]);
    }
    System.out.println("--------------------------------------------------------------------------------");
  }

}
