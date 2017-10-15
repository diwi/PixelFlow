/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.imageprocessing;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;


/**
 * 
 * Builds a gradient image from a given input.<br>
 * The gradient is stored in a two-channel float texture.<br>
 * <br>
 * This class includes some GLSL programs for rendering:
 * <ul>
 * <li> Velocity Pixel Shader </li>
 * <li> Velocity Line Shader </li>
 * <li> Line Integral Convolution - LIC </li>
 * </ul>
 * 
 * @author Thomas Diewald
 *
 */
public class DwFlowField {
  
  public static class Param {
    
    public float   line_spacing = 15;
    public float   line_width   = 1.0f;
    public float   line_scale   = 1.5f;
    public boolean line_smooth  = true;
    
    public int     line_mode    = 0; // 0 or 1, in velocity direction, or normal to it
    public int     line_shading = 0; // 0 =  col_A/col_B, 1 = velocity
    
    public float[] line_col_A        = {1,1,1,1.0f};
    public float[] line_col_B        = {1,1,1,0.1f};
    
    public int     blend_mode   = 0; // BLEND=0; ADD=1
    
    public int     blur_radius     = 2;
    public int     blur_iterations = 1;
    
    public boolean HIGHP_FLOAT = false; // false=GL2.GL_RG16F, true=GL2.GL_RG32F; 
  }
  
  
  public DwPixelFlow context;
  
  public Param param = new Param();
  
  public DwGLSLProgram shader_create;
  public DwGLSLProgram shader_display_lines;
  public DwGLSLProgram shader_display_pixel;
  public DwGLSLProgram shader_display_lic;
  
  public DwGLTexture tex_vel = new DwGLTexture();
  public DwGLTexture tex_tmp = new DwGLTexture();
  
  
  int tex_wrap = GL2.GL_CLAMP_TO_EDGE;
  
  public DwFlowField(DwPixelFlow context){
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    String data_path = DwPixelFlow.SHADER_DIR+"Filter/";
    
    shader_create        = context.createShader(data_path+"flowfield_create.frag");
    shader_display_pixel = context.createShader(data_path+"flowfield_display_pixel.frag");
    shader_display_lines = context.createShader(data_path+"flowfield_display_lines.glsl", data_path+"flowfield_display_lines.glsl");
    shader_display_lines.frag.setDefine("SHADER_FRAG", 1);
    shader_display_lines.vert.setDefine("SHADER_VERT", 1);
    shader_display_lic   = context.createShader(data_path+"flowfield_display_line_integral_convolution.frag");
  }
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex_vel.release();
    tex_tmp.release();
    tex_lic.release();
  }
  
  public void reset(){
    tex_vel.clear(0);
  }

  public boolean resize(int w, int h){
    int internalFormat   = GL2.GL_RG16F;
    int byte_per_channel = 2;
    if(param.HIGHP_FLOAT){
      internalFormat   = GL2.GL_RG32F;
      byte_per_channel = 4;
    }
    boolean resized = tex_vel.resize(context, internalFormat, w, h, GL2.GL_RG, GL.GL_FLOAT, GL2.GL_LINEAR, tex_wrap, 2, byte_per_channel);
    if(resized){
      tex_vel.clear(0);
    }
    return resized;
  }
  
  public void create(PGraphicsOpenGL pg_src){
    Texture tex_src = pg_src.getTexture(); if(!tex_src.available())  return;
    create(tex_src.glName, tex_src.glWidth, tex_src.glHeight);
  }
  
  public void create(DwGLTexture tex_src){
    create(tex_src.HANDLE[0], tex_src.w, tex_src.h);
  }
  
  public void create(int tex_src, int w_src, int h_src){
    context.begin();

    resize(w_src, h_src);
    
    int w_dst = tex_vel.w;
    int h_dst = tex_vel.h;
    
    context.beginDraw(tex_vel);
    shader_create.begin();
    shader_create.uniform2f     ("wh_rcp" , 1f/w_dst, 1f/h_dst);
    shader_create.uniformTexture("tex_src", tex_src);
    shader_create.drawFullScreenQuad();
    shader_create.end();
    context.endDraw("FlowField.create");

    blur(param.blur_iterations, param.blur_radius);

    context.end();
  }
  
  

  
  public void blur(){
    blur(param.blur_iterations, param.blur_radius);
  }

  public void blur(int iterations, int radius){
    if(!tex_vel.isTexture() || iterations <= 0 || radius <= 0){
      return;
    }
    
    tex_tmp.resize(context, tex_vel);
    tex_tmp.setParamWrap(GL2.GL_MIRRORED_REPEAT);
    tex_vel.setParamWrap(GL2.GL_MIRRORED_REPEAT);
    
    for(int i = 0; i < iterations; i++){
      DwFilter.get(context).gaussblur.apply(tex_vel, tex_vel, tex_tmp, radius);
    }
    
    tex_vel.setParamWrap(tex_wrap);
    context.errorCheck("FlowField.blur()");
  }
  
  

  
  public void displayLines(PGraphicsOpenGL dst){
    int   w = dst.width;
    int   h = dst.height;
    int   lines_x   = (int) Math.ceil(w/param.line_spacing);
    int   lines_y   = (int) Math.ceil(h/param.line_spacing);
    int   num_lines = lines_x * lines_y;
    float scale     = param.line_scale;

    context.begin();
    context.beginDraw(dst);
    blendMode();
    shader_display_lines.vert.setDefine("LINE_MODE"   , param.line_mode);
    shader_display_lines.vert.setDefine("LINE_SHADING", param.line_shading);
    shader_display_lines.begin();
    shader_display_lines.uniform4fv    ("col_A"         , 1, param.line_col_A);
    shader_display_lines.uniform4fv    ("col_B"         , 1, param.line_col_B);
    shader_display_lines.uniform2i     ("wh_lines"      ,    lines_x,    lines_y);
    shader_display_lines.uniform2f     ("wh_lines_rcp"  , 1f/lines_x, 1f/lines_y);
    shader_display_lines.uniform1f     ("vel_scale"     , scale);
    shader_display_lines.uniformTexture("tex_velocity"  , tex_vel);
    shader_display_lines.drawFullScreenLines(num_lines, param.line_width, param.line_smooth);
    shader_display_lines.end();
    context.endDraw("FlowField.displayLines");
    context.end();
  }
  
  public void displayPixel(PGraphicsOpenGL dst){
    int w = dst.width;
    int h = dst.height;
    context.begin();
    context.beginDraw(dst);
    blendMode();
    shader_display_pixel.begin();
    shader_display_pixel.uniform2f     ("wh_rcp"      ,   1f/w, 1f/h);
    shader_display_pixel.uniformTexture("tex_velocity"  , tex_vel);
    shader_display_pixel.drawFullScreenQuad();
    shader_display_pixel.end();
    context.endDraw();
    context.end("FlowField.displayPixel");
  }
  
  
  protected void blendMode(){
    context.gl.glEnable(GL.GL_BLEND);
    context.gl.glBlendEquation(GL.GL_FUNC_ADD);
    switch(param.blend_mode){
      case 0:  context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE); break; // BLEND
      case 1:  context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE                , GL.GL_ONE, GL.GL_ONE); break; // ADD
      default: context.gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE); break; // BLEND
    }
  }
  
  
  
  
  public static class ParamLIC{
    public int     iterations        = 1;
    public int     num_samples       = 25;
    public float   acc_mult          = 1.0f;
    public float   vel_mult          = 1.00f;
    public float   intensity_mult    = 1f;
    public float   intensity_exp     = 1f;
    public boolean TRACE_FORWARD     = false;
    public boolean TRACE_BACKWARD    = true;
  }
  
  
  public ParamLIC param_lic = new ParamLIC();
  
  public DwGLTexture.TexturePingPong tex_lic = new DwGLTexture.TexturePingPong();
  

  public void displayLineIntegralConvolution(PGraphicsOpenGL dst, PGraphicsOpenGL src){
    resizeLic(dst.width, dst.height);
    DwFilter.get(context).copy.apply(src, tex_lic.src);
    applyLineIntegralConvolution();
    DwFilter.get(context).copy.apply(tex_lic.src, dst);
  }
  
  public void displayLineIntegralConvolution(DwGLTexture dst, DwGLTexture src){
    resizeLic(dst.w, dst.h);
    DwFilter.get(context).copy.apply(src, tex_lic.src);
    applyLineIntegralConvolution();
    DwFilter.get(context).copy.apply(tex_lic.src, dst);
  }
  
  // TODO, custom formats?
  private void resizeLic(int w, int h){
    tex_lic.resize(context, GL2.GL_RGBA8, w, h, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4, 1);
//    tex_lic.resize(context, GL2.GL_RGBA16F, w, h, GL2.GL_RGBA, GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_MIRRORED_REPEAT, 4, 2);
  }
  
  private void applyLineIntegralConvolution(){
    
    if(!param_lic.TRACE_FORWARD && !param_lic.TRACE_BACKWARD){
      return;
    }
    
    int w_dst = tex_lic.dst.w;
    int h_dst = tex_lic.dst.h;
    int w_vel = tex_vel.w;
    int h_vel = tex_vel.h;
    
    boolean APPLY_EXP_SHADING = param_lic.intensity_exp != 0.0;
    int TRACE_B = param_lic.TRACE_BACKWARD ? 1 : 0;
    int TRACE_F = param_lic.TRACE_FORWARD  ? 1 : 0;
    int num_samples = (int) Math.ceil(param_lic.num_samples / (float)(TRACE_B + TRACE_F));

    shader_display_lic.frag.setDefine("NUM_SAMPLES"      , num_samples);
    shader_display_lic.frag.setDefine("TRACE_FORWARD"    , TRACE_F);
    shader_display_lic.frag.setDefine("TRACE_BACKWARD"   , TRACE_B);
    shader_display_lic.frag.setDefine("APPLY_EXP_SHADING", APPLY_EXP_SHADING ? 1 : 0);
    
    float[] acc_minmax = {0, 1};
    float[] vel_minmax = {0, Math.max(1, param_lic.acc_mult)};


    context.begin();
    for(int i = 0; i < param_lic.iterations; i++){
      context.beginDraw(tex_lic.dst);
      shader_display_lic.begin();
      shader_display_lic.uniform2f     ("wh_rcp"    , 1f/w_dst, 1f/h_dst);
      shader_display_lic.uniform2f     ("wh_vel_rcp", 1f/w_vel, 1f/h_vel);
      shader_display_lic.uniform1f     ("acc_mult"  , param_lic.acc_mult);
      shader_display_lic.uniform1f     ("vel_mult"  , param_lic.vel_mult);
      shader_display_lic.uniform2f     ("acc_minmax", acc_minmax[0], acc_minmax[1]);
      shader_display_lic.uniform2f     ("vel_minmax", vel_minmax[0], vel_minmax[1]);
      shader_display_lic.uniform1f     ("intensity_mult", param_lic.intensity_mult);
      if(APPLY_EXP_SHADING){
        shader_display_lic.uniform1f     ("intensity_exp" , param_lic.intensity_exp);
      }
      shader_display_lic.uniformTexture("tex_src"   , tex_lic.src);
      shader_display_lic.uniformTexture("tex_acc"   , tex_vel);
      shader_display_lic.drawFullScreenQuad();
      shader_display_lic.end();
      context.endDraw();
      tex_lic.swap();
    }
    context.end("FlowField.displayLineIntegralConvolution");
  }
  
  

}
