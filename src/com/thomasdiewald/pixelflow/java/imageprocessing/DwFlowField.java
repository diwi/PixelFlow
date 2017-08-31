package com.thomasdiewald.pixelflow.java.imageprocessing;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import processing.opengl.PGraphicsOpenGL;

public class DwFlowField {
  
  public static class Param {
    public float   line_width   = 1.0f;
    public float   line_spacing = 10;
    public float   line_scale   = 1f;
    public float[] col_A        = {0,0,0,1.0f};
    public float[] col_B        = {0,0,0,0.1f};
    
    public int     blend_mode   = 0; // BLEND=0; ADD=1
  }
  
  public DwPixelFlow context;
  
  public Param param = new Param();
  
  public DwGLSLProgram shader_create;
  public DwGLSLProgram shader_display;
  
  public DwGLTexture tex_flowfield = new DwGLTexture();
  public DwGLTexture tex_tmp = new DwGLTexture();

  protected String data_path = DwPixelFlow.SHADER_DIR+"Filter/";
  
  public DwFlowField(DwPixelFlow context){
    this.context = context;
    
    shader_create  = context.createShader(data_path+"flowfield_create.frag");
    shader_display = context.createShader(data_path+"flowfield_display.glsl", data_path+"flowfield_display.glsl");
    shader_display.frag.setDefine("SHADER_FRAG", 1);
    shader_display.vert.setDefine("SHADER_VERT", 1);
  }
  
  public void release(){
    tex_flowfield.release();
    tex_tmp.release();
  }
  int count = 0;
  public void resize(int w, int h){
    tex_flowfield.resize(context, GL2.GL_RG32F, w, h, GL2.GL_RG, GL.GL_FLOAT, GL2.GL_NEAREST, 2, 2);
    tex_tmp.resize(context, tex_flowfield);
    tex_tmp.clear(0);
  }
  
  public void create(DwGLTexture tex_src){

    int w = tex_src.w;
    int h = tex_src.h;
    
    resize(w, h);
    
    
    context.begin();
    context.beginDraw(tex_flowfield);
    shader_create.begin();
    shader_create.uniform2f     ("wh_rcp" , 1f/w, 1f/h);
    shader_create.uniformTexture("tex_src", tex_src);
    shader_create.drawFullScreenQuad();
    shader_create.end();
    context.endDraw();
    context.end("FlowField.create()");
  }
  
  
  public void display(PGraphicsOpenGL dst){

    int w = dst.width;
    int h = dst.height;
    
    int   lines_x    = Math.round(w/param.line_spacing);
    int   lines_y    = Math.round(h/param.line_spacing);
    int   num_lines  = lines_x * lines_y;
    float space_x    = w / (float) lines_x;
    float space_y    = h / (float) lines_y;
    float scale      = (space_x + space_y) * param.line_scale;

    shader_display.vert.setDefine("LINE_MODE", 0);
    
    context.begin();
    context.beginDraw(dst);
    blendMode();
    shader_display.begin();
    shader_display.uniform2f     ("wh_rcp"        , 1f/tex_flowfield.w, 1f/tex_flowfield.h);
    shader_display.uniform4fv    ("col_A"         , 1, param.col_A);
    shader_display.uniform4fv    ("col_B"         , 1, param.col_B);
    shader_display.uniform2i     ("num_lines"     , lines_x, lines_y);
    shader_display.uniform2f     ("spacing"       , space_x, space_y);
    shader_display.uniform1f     ("vel_scale"     , scale);
    shader_display.uniformTexture("tex_velocity"  , tex_flowfield);
    shader_display.drawFullScreenLines(0, 0, w, h, num_lines, param.line_width);
    shader_display.end();
    context.endDraw();
    context.end("OpticalFlow.renderVelocityStreams");
  }
  
  protected void blendMode(){
    context.gl.glEnable(GL.GL_BLEND);
    switch(param.blend_mode){
      case 0:  context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); break; // BLEND
      case 1:  context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE)                ; break; // ADD
      default: context.gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA); break; // BLEND
    }
  }
  

}
