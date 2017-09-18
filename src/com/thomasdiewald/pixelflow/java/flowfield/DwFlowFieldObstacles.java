package com.thomasdiewald.pixelflow.java.flowfield;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
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
  
  public DistanceTransform dt_obstacles;
  public DwFlowField ff_obstacle;


  public DwFlowFieldObstacles(DwPixelFlow context){    
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    shader_obstacles_FG   = context.createShader(data_path+"obstacles_FG.frag");
    shader_obstacles_dist = context.createShader(data_path+"obstacles_dist.frag");
    
    dt_obstacles = new DistanceTransform(context);
    ff_obstacle = new DwFlowField(context);
  }
  
  public void dispose(){
    release();
  }
   
  public void release(){
    dt_obstacles.release();
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
    
    context.begin();
    
    // 0) assure size
    resize(w, h);
    
    // 1) create FG mask
    context.beginDraw(tex_obstacles_FG);
    shader_obstacles_FG.begin();
    shader_obstacles_FG.uniform4fv    ("FG_mask"  , 1, param.FG_mask);
    shader_obstacles_FG.uniform1i     ("FG_invert", param.FG_invert ? 1 : 0);
    shader_obstacles_FG.uniformTexture("tex_scene", tex_scene.glName);
    shader_obstacles_FG.drawFullScreenQuad();
    shader_obstacles_FG.end();
    context.endDraw("DwFlowFieldObstacles.create() create FG mask");
    
    // 2) apply distance transform
    dt_obstacles.param.FG_mask = new float[]{1,1,0,1}; // only obstacle EDGES
    dt_obstacles.create(tex_obstacles_FG);
    
    // 3) create distance field
    context.beginDraw(tex_obstacles_dist);
    shader_obstacles_dist.begin();
    shader_obstacles_dist.uniform2f     ("mad", 1, Math.max(0, param.FG_offset));
    shader_obstacles_dist.uniformTexture("tex_FG"  , tex_obstacles_FG);
    shader_obstacles_dist.uniformTexture("tex_dtnn", dt_obstacles.tex_dtnn.src);
    shader_obstacles_dist.drawFullScreenQuad();
    shader_obstacles_dist.end();
    context.endDraw("DwFlowFieldObstacles.create() distance field");
    
    // 4) create flow field
    ff_obstacle.create(tex_obstacles_dist);
    
    context.end("DwFlowFieldObstacles.create()");
  }
  
  
}
