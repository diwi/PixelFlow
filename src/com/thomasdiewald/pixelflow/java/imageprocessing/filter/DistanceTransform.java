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

import com.jogamp.opengl.GLES3;
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
  }
  
  public Param param = new Param();
  
  protected DwPixelFlow context;
  
  protected DwGLSLProgram shader_init;
  protected DwGLSLProgram shader_dtnn;
  protected DwGLSLProgram shader_voronoi;
 
  public DwGLTexture.TexturePingPong tex_dtnn = new DwGLTexture.TexturePingPong();
  
  public DistanceTransform(DwPixelFlow context){
    this.context = context;
  }
  
  public void release(){
    tex_dtnn.release();
  }
  
  public void resize(int w, int h){
    
    tex_dtnn.resize(context, GLES3.GL_RG16UI, w, h, GLES3.GL_RG_INTEGER, GLES3.GL_UNSIGNED_SHORT, GLES3.GL_NEAREST, 2, 2);
    tex_dtnn.src.setParam_WRAP_S_T(GLES3.GL_CLAMP_TO_EDGE);
    tex_dtnn.dst.setParam_WRAP_S_T(GLES3.GL_CLAMP_TO_EDGE); 
    
    if(shader_init == null || shader_dtnn == null){
      shader_init = context.createShader((Object)"dt_init", DwPixelFlow.SHADER_DIR+"Filter/distancetransform.frag");
      shader_dtnn = context.createShader((Object)"dt_dtnn", DwPixelFlow.SHADER_DIR+"Filter/distancetransform.frag");
      
      shader_init.frag.setDefine("PASS_INIT", 1);
      shader_dtnn.frag.setDefine("PASS_DTNN", 1);
      shader_dtnn.frag.setDefine("TEXACCESS", 0);

      int POS_MAX_LIMIT = 0x7FFF;
      int POS_MAX = Math.max(w, h) * 2;
      
      if(POS_MAX > POS_MAX_LIMIT){
        System.out.println("DistanceTransform.resize ERROR: max possible texture size: "+(POS_MAX_LIMIT/2));
      }

//      shader_init.frag.getDefine("POS_MAX"  ).value = ""+POS_MAX;
//      shader_dtnn.frag.getDefine("POS_MAX"  ).value = ""+POS_MAX;
    }
    
  }
  
  
  public void create(PGraphicsOpenGL mask){
    Texture tex_mask = mask.getTexture();  if(!tex_mask.available())  return;
    
    int w = tex_mask.glWidth;
    int h = tex_mask.glHeight;
    
    resize(w, h);
    
    // init
    context.begin();
    
    context.beginDraw(tex_dtnn.dst);
    shader_init.begin();
    shader_init.uniformTexture("tex_mask", tex_mask.glName);
    shader_init.drawFullScreenQuad();
    shader_init.end();
    context.endDraw("DistanceTransform.create init");  
    tex_dtnn.swap();
    
    
    // update
    int passes = DwUtils.log2ceil(Math.max(w, h)) - 1;
    for(int jump = 1 << passes; jump > 0; jump >>= 1 ){
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
  
  
  /**
   * texel-data lookup at the nearest neighbor -> voronoi
   * 
   * @param src
   * @param dst
   */
  public void apply(PGraphicsOpenGL src, PGraphicsOpenGL dst){
    Texture tex_src  = src.getTexture();  if(!tex_src .available())  return;
    Texture tex_dst  = dst.getTexture();  if(!tex_dst .available())  return;
    
    if(src == dst){
      System.out.println("DistanceTransform.apply error: read-write race");
    }
    
    if(shader_voronoi == null){
      shader_voronoi = context.createShader(DwPixelFlow.SHADER_DIR+"Filter/distancetransform_voronoi.frag");
    }
    
    context.begin();
    context.beginDraw(dst);
    shader_voronoi.begin();
    shader_voronoi.uniformTexture("tex_src", tex_src.glName);
    shader_voronoi.uniformTexture("tex_dtnn", tex_dtnn.src);
    shader_voronoi.uniform1f     ("dist_norm", param.voronoi_distance_normalization);
    shader_voronoi.drawFullScreenQuad();
    shader_voronoi.end();
    context.endDraw();
    context.end("DistanceTransform.apply");
  }
  
}
