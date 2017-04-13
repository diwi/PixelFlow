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
import com.jogamp.opengl.GLES3;

public class DwGLFrameBuffer {
  public GL2ES2 gl;
  

  public int[] HANDLE_fbo = null;
  public int[] color_attachments = new int[0];; // currently bound rendertargets
  
  public DwGLFrameBuffer(){
  }
  public DwGLFrameBuffer(GL2ES2 gl){
    allocate(gl);
  }
  
  public void release(){
    if(gl != null){
      if(HANDLE_fbo != null) gl.glDeleteFramebuffers(1, HANDLE_fbo, 0); 
      HANDLE_fbo = null;
      gl = null;
    }
  }
  

  public void allocate(GL2ES2 gl){
    if(HANDLE_fbo == null){
      HANDLE_fbo = new int[1];
      this.gl = gl;
      gl.glGenFramebuffers(1, HANDLE_fbo, 0);
    }
  }
  
  
  public void bind(){
    gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, HANDLE_fbo[0]);
  }
  

  // textures must be of the same size
  public void bind(int ... HANDLE_tex){
    
    if(IS_ACTIVE){
      unbind(); // unbind, in case of bind() is called consecutively
    }
    
    color_attachments = new int[HANDLE_tex.length];
    
    bind();
    for(int i = 0; i < HANDLE_tex.length; i++){
      color_attachments[i] = GL2ES2.GL_COLOR_ATTACHMENT0 + i;
      gl.glFramebufferTexture2D(GL2ES2.GL_FRAMEBUFFER, color_attachments[i], GL2ES2.GL_TEXTURE_2D, HANDLE_tex[i], 0);
    }
    
    gl.glDrawBuffers(color_attachments.length, color_attachments, 0);
    IS_ACTIVE = true;
  }
  
  
  public boolean IS_ACTIVE = false;
  
   
  
  public void bind(DwGLTexture ... tex){
   
    if(IS_ACTIVE){
      unbind(); // unbind, in case of bind() is called consecutively
    }
    
    color_attachments = new int[tex.length];
    
    gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, HANDLE_fbo[0]);
    for(int i = 0; i < tex.length; i++){
      color_attachments[i] = GL2ES2.GL_COLOR_ATTACHMENT0 + i;
      gl.glFramebufferTexture2D(GL2ES2.GL_FRAMEBUFFER, color_attachments[i], GL2ES2.GL_TEXTURE_2D, tex[i].HANDLE[0], 0);
    }
    
    
    gl.glDrawBuffers(color_attachments.length, color_attachments, 0);
    IS_ACTIVE = true;
  }
  
  
  public void unbind(){
    for(int i = 0; i < color_attachments.length; i++){
      gl.glFramebufferTexture2D(GL2ES2.GL_FRAMEBUFFER, color_attachments[i], GL2ES2.GL_TEXTURE_2D, 0, 0);
    }
    color_attachments = new int[0];
    gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, 0);
    IS_ACTIVE = false;
  }
  
  public boolean isActive(){
    return IS_ACTIVE;
  }
  
  
  public void clearTexture(float r, float g, float b, float a, DwGLTexture ... tex){
    bind(tex);
    int w = tex[0].w();
    int h = tex[0].h();
    gl.glViewport(0, 0, w, h);
    gl.glColorMask(true, true, true, true);
    gl.glClearColor(r,g,b,a);
    gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT);
    unbind();
    DwGLError.debug(gl, "DwGLFrameBuffer.clearTexture");
  }
  
  
  public void clearTexture(float v, DwGLTexture ... tex){
    clearTexture(v,v,v,v,tex);
  }
  
  
  
  
  
  
  
  
  
  public void setRenderBuffer(int HANDLE_rbo, boolean depth, boolean stencil){
    boolean is_active = isActive();
    if(!is_active) bind();
  
    if(depth  )gl.glFramebufferRenderbuffer(GLES3.GL_FRAMEBUFFER, GLES3.GL_DEPTH_ATTACHMENT  , GLES3.GL_RENDERBUFFER, HANDLE_rbo);
    if(stencil)gl.glFramebufferRenderbuffer(GLES3.GL_FRAMEBUFFER, GLES3.GL_STENCIL_ATTACHMENT, GLES3.GL_RENDERBUFFER, HANDLE_rbo);
    
    if(!is_active) unbind();
  }
  
}
