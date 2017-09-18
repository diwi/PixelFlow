package com.thomasdiewald.pixelflow.java.flowfield;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLES3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Copy;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DistanceTransform;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

public class DwFlowFieldObstacles {
  
  public static class Param {
    public float[] FG_mask = {0,0,0,1};
    public boolean FG_invert = false;
    public float   FG_offset = 0.0f;
  }
  
  
  public Param param = new Param();
  
  public DwPixelFlow context;
  
//  protected String data_path = DwPixelFlow.SHADER_DIR+"flowfield/";
  protected String data_path = "D:/data/__Eclipse/workspace/WORKSPACE_FLUID/PixelFlow/src/com/thomasdiewald/pixelflow/glsl/flowfield/";
  
  public DwGLSLProgram shader_obstacles_FG;
  public DwGLSLProgram shader_obstacles_dist;
  
  public DwGLTexture tex_obstacles_FG   = new DwGLTexture();
  public DwGLTexture tex_obstacles_dist = new DwGLTexture();
  
  public Copy copy;
  public DistanceTransform dtobs;
  public DwFlowField ff_obstacle;


  public DwFlowFieldObstacles(DwPixelFlow context){    
    this.context = context;

    shader_obstacles_FG   = context.createShader(data_path+"obstacles_FG.frag");
    shader_obstacles_dist = context.createShader(data_path+"obstacles_dist.frag");
    
    copy = new Copy(context);
    dtobs = new DistanceTransform(context);
    ff_obstacle = new DwFlowField(context);
  }
  
  public void release(){
    dtobs.release();
    
    ff_obstacle.release();

    tex_obstacles_FG.release();
    tex_obstacles_dist.release();
  }
  
  
  public void resize(int w, int h){
    tex_obstacles_FG  .resize(context, GL2.GL_RGBA, w, h, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, 4, 1);
    tex_obstacles_dist.resize(context, GL2.GL_R32F, w, h, GL2.GL_RED  , GL2.GL_FLOAT       , GL2.GL_LINEAR, 1, 4);
    tex_obstacles_dist.setParam_WRAP_S_T(GL2.GL_CLAMP_TO_EDGE);
  }
  
  
  
  public void create(PGraphicsOpenGL pg_scene){
    
    Texture tex_scene = pg_scene.getTexture(); if(!tex_scene.available())  return;
    
    int w = pg_scene.width;
    int h = pg_scene.height;
    
    resize(w, h);
    
//    copy.apply(pg_scene, tex_obstacles);

    context.begin();
    
    // create FG mask
    context.beginDraw(tex_obstacles_FG);
    shader_obstacles_FG.begin();
    shader_obstacles_FG.uniform4fv    ("FG_mask"   , 1, param.FG_mask);
    shader_obstacles_FG.uniform1i     ("FG_invert" , param.FG_invert ? 1 : 0);
    shader_obstacles_FG.uniformTexture("tex_scene", tex_scene.glName);
    shader_obstacles_FG.drawFullScreenQuad();
    shader_obstacles_FG.end();
    context.endDraw();
    

    // apply distance transform
    dtobs.param.mask = new float[]{1,1,0,1};
    dtobs.create(tex_obstacles_FG);
    
    param.FG_offset = Math.max(0, param.FG_offset);

    // create distance field
//    dtobs.computeDistanceField();
    context.beginDraw(tex_obstacles_dist);
    shader_obstacles_dist.begin();
    shader_obstacles_dist.uniform2f("mad", 1, param.FG_offset);
    shader_obstacles_dist.uniformTexture("tex_FG"  , tex_obstacles_FG);
    shader_obstacles_dist.uniformTexture("tex_dtnn", dtobs.tex_dtnn.src);
    shader_obstacles_dist.drawFullScreenQuad();
    shader_obstacles_dist.end();
    context.endDraw();
    
    // create flow field
    ff_obstacle.create(tex_obstacles_dist);
    
    context.end();
  }
  
  
}
