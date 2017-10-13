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

package com.thomasdiewald.pixelflow.java.imageprocessing.filter;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * 
 * @author Thomas Diewald
 * 
 * Distance Transform:
 * 
 *  Jumpflood Algorithm (JFA)
 *
 *  Jump Flooding in GPU with Applications to Voronoi Diagram and Distance Transform
 *  www.comp.nus.edu.sg/~tants/jfa/i3d06.pdf
 *
 */
public class DistanceTransform {
  
  public static class Param{
    // just a visual scale for the voronoi example
    public float voronoi_distance_normalization = 0.1f;
    
    public float[] FG_mask = {0,0,0,1};
    public boolean FG_invert = false;
  }
  
  public Param param = new Param();
  
  protected DwPixelFlow context;
  
  protected DwGLSLProgram shader_init;
  protected DwGLSLProgram shader_dtnn;
  
  protected DwGLSLProgram shader_dist;
  protected DwGLSLProgram shader_threshold;
  protected DwGLSLProgram shader_voronoi;

  
  public DwGLTexture.TexturePingPong tex_dtnn = new DwGLTexture.TexturePingPong();
  public DwGLTexture                 tex_dist = new DwGLTexture();
  public DwGLTexture                 tex_threshold = new DwGLTexture();
  
  public DistanceTransform(DwPixelFlow context){
    this.context = context;
    
    String data_path = DwPixelFlow.SHADER_DIR+"Filter/";
    
    shader_init = context.createShader((Object)"dt_init", data_path+"distancetransform.frag");
    shader_dtnn = context.createShader((Object)"dt_dtnn", data_path+"distancetransform.frag");
    
    shader_init.frag.setDefine("PASS_INIT", 1);
    shader_dtnn.frag.setDefine("PASS_DTNN", 1);
    shader_dtnn.frag.setDefine("TEXACCESS", 0);
    
    shader_dist      = context.createShader((Object)"dt_dist"     , data_path+"distancetransform_distance.frag");
    shader_voronoi   = context.createShader((Object)"dt_voronoi"  , data_path+"distancetransform_voronoi.frag");
    shader_threshold = context.createShader((Object)"dt_threshold", data_path+"distancetransform_threshold.frag");
  }
  
  public void release(){
    tex_dtnn.release();
    tex_dist.release();
    tex_threshold.release();
  }
  
  public void resize(int w, int h){
    
    tex_dtnn.resize(context, GL2.GL_RG16UI, w, h, GL2.GL_RG_INTEGER, GL2.GL_UNSIGNED_SHORT, GL2.GL_NEAREST, GL2.GL_CLAMP_TO_EDGE, 2, 2);

    int POS_MAX_LIMIT = 0x7FFF;
    int POS_MAX = Math.max(w, h) * 2;
      
    if(POS_MAX > POS_MAX_LIMIT){
      System.out.println("DistanceTransform.resize ERROR: max possible texture size: "+(POS_MAX_LIMIT/2));
    }
    
  }
  
  public void create(PGraphicsOpenGL pg_mask){
    Texture tex_mask = pg_mask.getTexture();  if(!tex_mask.available())  return;
    create(tex_mask.glName, tex_mask.glWidth, tex_mask.glHeight);
  }
  
  public void create(DwGLTexture tex_mask){    
    create(tex_mask.HANDLE[0], tex_mask.w, tex_mask.h);
  }
  
  public void create(int HANDLE_tex, int w, int h){

    context.begin();
    
    resize(w, h);
    
    // init
    context.beginDraw(tex_dtnn.dst);
    shader_init.begin();
    shader_init.uniform4fv    ("FG_mask", 1, param.FG_mask);
    shader_init.uniform1i     ("FG_invert", param.FG_invert ? 1 : 0);
    shader_init.uniformTexture("tex_mask", HANDLE_tex);
    shader_init.drawFullScreenQuad();
    shader_init.end();
    context.endDraw("DistanceTransform.create init");  
    tex_dtnn.swap();
    
    // update
    int passes = DwUtils.log2ceil(Math.max(w, h)) - 1;
    for(int jump = 1 << passes; jump > 0; jump >>= 1){
      context.beginDraw(tex_dtnn.dst);
      shader_dtnn.begin();
      shader_dtnn.uniform2i("wh", w, h);
      shader_dtnn.uniform3i("jump", -jump, 0, jump);
      shader_dtnn.uniformTexture("tex_dtnn", tex_dtnn.src);
      shader_dtnn.drawFullScreenQuad();
      shader_dtnn.end();
      context.endDraw("DistanceTransform.create update");
      tex_dtnn.swap();
    }
    
    context.end("DistanceTransform.create");
  }
  
  
 
  
  

  public void computeDistanceField(){
    int w = tex_dtnn.src.w;
    int h = tex_dtnn.src.h;
    
    tex_dist.resize(context, GL2.GL_R32F, w, h, GL2.GL_RED, GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_CLAMP_TO_EDGE, 1, 4);
    
    context.begin();
    context.beginDraw(tex_dist);
    shader_dist.begin();
    shader_dist.uniformTexture("tex_dtnn", tex_dtnn.src);
    shader_dist.drawFullScreenQuad();
    shader_dist.end();
    context.endDraw();
    context.end("DistanceTransform.computeDistance");
  }
  
  
  public void computeDistanceThreshold(PGraphicsOpenGL dst, float distance_threshold, float[] colA, float[] colB){
    Texture tex_dst  = dst.getTexture();  if(!tex_dst .available())  return;
 
    int w = dst.width;
    int h = dst.height;
    
    context.begin();
    context.beginDraw(dst);
    shader_threshold.begin();
    shader_threshold.uniform2f     ("wh_rcp"   , 1f/w, 1f/h);
    shader_threshold.uniform4fv    ("colA"     , 1, colA);
    shader_threshold.uniform4fv    ("colB"     , 1, colB);
    shader_threshold.uniform1f     ("threshold", distance_threshold);
    shader_threshold.uniformTexture("tex_dtnn" , tex_dtnn.src);
    shader_threshold.drawFullScreenQuad();
    shader_threshold.end();
    context.endDraw();
    context.end("DistanceTransform.computeDistanceThreshold");
  }
  
  
  /**
   * texel-data lookup at the nearest neighbor -> voronoi
   * 
   * @param src src color at nn-lookup position
   * @param dst
   */
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst){
    Texture tex_src = src.getTexture();  if(!tex_src .available()) return;
    Texture tex_dst = dst.getTexture();  if(!tex_dst .available()) return;
    
    if(src == dst){
      System.out.println("DistanceTransform.apply error: read-write race");
    }
    int w_dst = dst.width;
    int h_dst = dst.height;
    
    int w_src = src.width;
    int h_src = src.height;
    
    int w_dtnn = tex_dtnn.src.w;
    int h_dtnn = tex_dtnn.src.h;

    context.begin();
    context.beginDraw(dst);
    shader_voronoi.begin();
    shader_voronoi.uniform2f     ("wh_rcp"     , 1f/w_dst, 1f/h_dst);
    shader_voronoi.uniform2f     ("wh_src_rcp" , 1f/w_src, 1f/h_src);
    shader_voronoi.uniform2f     ("wh_dtnn_rcp", 1f/w_dtnn, 1f/h_dtnn);
    shader_voronoi.uniform1f     ("dist_norm"  , param.voronoi_distance_normalization);
    shader_voronoi.uniformTexture("tex_src"    , tex_src.glName);
    shader_voronoi.uniformTexture("tex_dtnn"   , tex_dtnn.src);
    shader_voronoi.drawFullScreenQuad();
    shader_voronoi.end();
    context.endDraw();
    context.end("DistanceTransform.apply");
  }
  
}
