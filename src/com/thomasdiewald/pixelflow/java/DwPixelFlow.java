/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java;


import java.util.HashMap;
import java.util.Stack;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLContext;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLFrameBuffer;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLRenderSettingsCallback;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLError;

import processing.core.PApplet;
import processing.opengl.FrameBuffer;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;


public class DwPixelFlow{
                                     
  static public class PixelFlowInfo{
    
    static public final String version = "0.51";
    static public final String name    = "PixelFlow";
    static public final String author  = "Thomas Diewald";
    static public final String web     = "http://www.thomasdiewald.com";
    static public final String git     = "https://github.com/diwi/PixelFlow.git";
    
    public String toString(){
      return name+" v"+version +"  -  "+web;
    }
  }

  static public final PixelFlowInfo INFO;
  
  static{
    INFO = new PixelFlowInfo();
  }
  
  
  public static final String SHADER_DIR = "/com/thomasdiewald/pixelflow/glsl/";
  
  public PApplet papplet;
  public PJOGL   pjogl;
  public GL2ES2  gl;
  
  public final DwUtils utils;
  
  private int scope_depth = 0;
  
  public DwGLFrameBuffer framebuffer;
  
  
  private HashMap<String, DwGLSLProgram> shader_cache = new HashMap<String, DwGLSLProgram>();
  
  public DwPixelFlow(PApplet papplet){
    this.papplet = papplet;
    
    this.pjogl   = (PJOGL) papplet.beginPGL();
    this.gl = begin(); end();
    
//    int[] rval = new int[2];
//    gl.glGetIntegerv(GL2.GL_MIN_PROGRAM_TEXEL_OFFSET, rval, 0);
//    gl.glGetIntegerv(GL2.GL_MAX_PROGRAM_TEXEL_OFFSET, rval, 1);
//    System.out.println("GL_MIN_PROGRAM_TEXEL_OFFSET "+rval[0]);
//    System.out.println("GL_MAX_PROGRAM_TEXEL_OFFSET "+rval[1]);
    
    pjogl.enableFBOLayer();


    papplet.registerMethod("dispose", this);
    
    utils = new DwUtils(this);
   
    framebuffer = new DwGLFrameBuffer(gl);
  }
  
  public void dispose(){
    release();
  }
  
  
  public void release(){

    // release shader
//    int count = 0;
    for(String key : shader_cache.keySet()){
      DwGLSLProgram shader = shader_cache.get(key);
      shader.release();
//      System.out.printf("released Shader: [%2d] %s\n", count, key);
//      count++;
    }
    shader_cache.clear();
        
    if(framebuffer != null){
      framebuffer.release();
      framebuffer = null;
    }
  }
  
  
  
//  GLSL  |  OpenGL 
// -------|---------  
//  1.10  |   2.0
//  1.20  |   2.1
//  1.30  |   3.0
//  1.40  |   3.1
//  1.50  |   3.2
//  3.30  |   3.3
//  4.00  |   4.0
//  4.10  |   4.1
//  4.20  |   4.2
//  4.30  |   4.3
//  4.40  |   4.4


  // https://github.com/processing/processing/wiki/Advanced-OpenGL
  // PJOGL.profile = 3;

  public GL2ES2 begin(){
//    System.out.printf("%"+(scope_depth*2+1)+"s GLScope.begin %d\n", " ", scope_depth);
    if( scope_depth == 0){
      pjogl = (PJOGL) papplet.beginPGL(); 
      gl    = pjogl.gl.getGL2ES2();
    }

    scope_depth++;
    return gl;
  }
  
  public void end(){
    endDraw(); // just in case, a framebuffer is still bound
    
    scope_depth--;
//    System.out.printf("%"+(scope_depth*2+1)+"s GLScope.end   %d\n", " ", scope_depth);
    if(scope_depth == 0){
      papplet.endPGL();
    }
    scope_depth = Math.max(scope_depth, 0);
  }
  
  public void end(String error_msg){
    errorCheck(error_msg);
    end();
  }
  

  
  
  
  
  
  public boolean ACTIVE_FRAMEBUFFER = false;
  

  public void beginDraw(DwGLTexture ... dst){
//    if(ACTIVE_FRAMEBUFFER) return;
    ACTIVE_FRAMEBUFFER = true;
    framebuffer.bind(dst);
    defaultRenderSettings(0, 0, dst[0].w, dst[0].h);
  }
  
  PGraphicsOpenGL pgl_dst = null;
  public void beginDraw(PGraphicsOpenGL dst){
    ACTIVE_FRAMEBUFFER = true;
    beginDraw(dst, true);
  }
  
  public void beginDraw(PGraphicsOpenGL dst, boolean multisample){
    ACTIVE_FRAMEBUFFER = true;
    FrameBuffer fbo = dst.getFrameBuffer(multisample);
    if(fbo == null){
      multisample = false;
      fbo = dst.getFrameBuffer(multisample);
    }
    fbo.bind();
    defaultRenderSettings(0, 0, fbo.width, fbo.height);
    if(multisample){
      gl.glEnable(GL.GL_MULTISAMPLE);
    }
    this.pgl_dst = dst;
  }
  public void endDraw(){
    ACTIVE_FRAMEBUFFER = false;
    if(framebuffer != null && framebuffer.isActive()){
      framebuffer.unbind();
    } else {
      gl.glBindFramebuffer(GL2ES2.GL_FRAMEBUFFER, 0);
    }
    
    if(pgl_dst != null){
      updateFBO(pgl_dst);
      pgl_dst = null;
    }
  }
  
  public void endDraw(String error_msg){
    endDraw();
    errorCheck(error_msg);
  }
  
  // apparently this needs to be done. 
  // instead, loadTexture() needs to be called, ...i guess
  private void updateFBO(PGraphicsOpenGL pg){
    FrameBuffer mfb = pg.getFrameBuffer(true);
    FrameBuffer ofb = pg.getFrameBuffer(false);
    if (ofb != null && mfb != null) {
      mfb.copyColor(ofb);
    }
  }
  
  
 
  public void defaultRenderSettings(int x, int y, int w, int h){
    rendersettings_default.set(this, 0, 0, w, h);
    if(!rendersettings_user.isEmpty()){
      rendersettings_user.peek().set(this, 0, 0, w, h);
    }
  }
  
  private static class DefaultRenderSettings implements DwGLRenderSettingsCallback{
    @Override
    public void set(DwPixelFlow context, int x, int y, int w, int h) {
      GL2ES2 gl = context.gl;
      gl.glViewport(x, x, w, h);
      gl.glColorMask(true, true, true, true);
      gl.glDepthMask(false);
      gl.glDisable(GL.GL_DEPTH_TEST);
      gl.glDisable(GL.GL_SCISSOR_TEST);
      gl.glDisable(GL.GL_STENCIL_TEST);
      gl.glDisable(GL.GL_BLEND);
      gl.glDisable(GL.GL_MULTISAMPLE);
    }
  }
  
  final private DwGLRenderSettingsCallback rendersettings_default = new DefaultRenderSettings();

  Stack<DwGLRenderSettingsCallback> rendersettings_user = new Stack<DwGLRenderSettingsCallback>();
  
  public void pushRenderSettings(DwGLRenderSettingsCallback rendersettings){
    rendersettings_user.push(rendersettings);
  }
  public void popRenderSettings(){
    rendersettings_user.pop();
  }
  
  
  
  
  
  
  
  public DwGLSLProgram createShader(String path_fragmentshader){
    return createShader((Object)null, path_fragmentshader);
  }
  
  public DwGLSLProgram createShader(Object o, String path_fragmentshader){
    return createShader(o, null, path_fragmentshader);
  }
  
  public DwGLSLProgram createShader(String path_vertexshader, String path_fragmentshader){
    return createShader((Object) null, path_vertexshader, path_fragmentshader);
  }
  
  public DwGLSLProgram createShader(Object o, String path_vertexshader, String path_fragmentshader){

    // TODO: this might be a source for problems. 
    // given paths, when relative, could cause collisions.
    // to avoid this, pass "this" as the first argument
    String key = "";
//    if(o != null) key += "["+o.getClass().getCanonicalName()+"]";
    if(o != null) key += "["+o.hashCode()+"]";
    if(path_vertexshader != null) key += ""+path_vertexshader+"[]";
    key += ""+path_fragmentshader+"";
    
     
    DwGLSLProgram shader = shader_cache.get(key);
    if(shader == null){
      shader = new DwGLSLProgram(this, path_vertexshader, path_fragmentshader);
      shader_cache.put(key, shader);
    } 
    return shader;
  }
   

  
  
  
  
  
  
  

  public void getGLTextureHandle(PGraphicsOpenGL pg, int[] tex_handle){
    int fbo_handle = pg.getFrameBuffer().glFbo;
    int target     = GL2ES2.GL_FRAMEBUFFER;
    int attachment = GL2ES2.GL_COLOR_ATTACHMENT0;
    int[] params   = new int[1]; 
    gl.glBindFramebuffer(target, fbo_handle);
    gl.glGetFramebufferAttachmentParameteriv(target, attachment, GL2ES2.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, params, 0);
    if( params[0] == GL2ES2.GL_TEXTURE){
      gl.glGetFramebufferAttachmentParameteriv(target, attachment, GL2ES2.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, params, 0);
      tex_handle[0] = params[0];
    } else {
      tex_handle[0] = -1;
    }
    gl.glBindFramebuffer(target, 0);
  }
  
  
  
  
  
  public void errorCheck(String msg){
    DwGLError.debug(gl, msg);
  }
  
  
  
  
  public void printGL(){

    System.out.println("-------------------------------------------------------------------");
    GLContext glcontext = gl.getContext();
    
    String opengl_version    = gl.glGetString(GL2ES2.GL_VERSION).trim();
    String opengl_vendor     = gl.glGetString(GL2ES2.GL_VENDOR).trim();
    String opengl_renderer   = gl.glGetString(GL2ES2.GL_RENDERER).trim();
  //  String opengl_extensions = gl.glGetString(GL2ES2.GL_EXTENSIONS).trim();
//    String glsl_version      = gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION).trim();
  
    System.out.println("    OPENGL_VENDOR:         "+opengl_vendor);
    System.out.println("    OPENGL_RENDERER:       "+opengl_renderer);
    System.out.println("    OPENGL_VERSION:        "+opengl_version);
//    System.out.println("    OPENGL_EXTENSIONS:     "+opengl_extensions);
//    System.out.println("    GLSL_VERSION:          "+glsl_version);
    
    System.out.println("    GLSLVersionString:     "+ glcontext.getGLSLVersionString().trim());
    System.out.println("    GLSLVersionNumber:     "+ glcontext.getGLSLVersionNumber());
    System.out.println("    GLVersion:             "+ glcontext.getGLVersion().trim());
    System.out.println("    GLVendorVersionNumber: "+ glcontext.getGLVendorVersionNumber());
//    System.out.println("    GLVersionNumber:       "+ glcontext.getGLVersionNumber());
    
    System.out.println("-------------------------------------------------------------------");
    
  }

  
  public void print(){
    System.out.println(INFO);
  }
  
  


  


  
  
  
}
