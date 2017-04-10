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





package com.thomasdiewald.pixelflow.java.render.skylight;

import java.util.ArrayList;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureUtils;
import com.thomasdiewald.pixelflow.java.sampling.DwSampling;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PMatrix3D;
import processing.core.PVector;
import processing.opengl.PGraphics3D;
import processing.opengl.PShader;


public class DwSkyLightShader {
  
  static public final float TO_RAD = (float)Math.PI/180f;
  
  static public class Param{
    
    public float   intensity = 1f;
    public float[] rgb = {1,1,1};
    
    public float   solar_azimuth = 0;
    public float   solar_zenith  = 0;
    public float   sample_focus  = 1f;
    
    public float   iterations = 20;
//    public boolean singlesided = true;
    public int     shadowmap_size = 1024;
  }
  
  

  public Param param = new Param();
  

  
  
  public PMatrix3D mat_sun = new PMatrix3D();
  
  String dir = DwPixelFlow.SHADER_DIR+"render/skylight/";

  DwPixelFlow context;
  public PApplet papplet;

  public PShader shader;
  public DwShadowMap shadowmap;
  public PGraphics3D[] pg_shading = new PGraphics3D[2];

  public ArrayList<float[]> samples = new ArrayList<float[]>();

  public int RENDER_PASS = 0;
 
  public DwSceneDisplay scene_display;
  public DwScreenSpaceGeometryBuffer geombuffer;

  public DwSkyLightShader(DwPixelFlow context, DwSceneDisplay scene_display, DwScreenSpaceGeometryBuffer geombuffer, DwShadowMap shadowmap){
    this.context = context;
    this.papplet = context.papplet;
    
    String[] src_frag = context.utils.readASCIIfile(dir+"skylight.frag");
    String[] src_vert = context.utils.readASCIIfile(dir+"skylight.vert");

    this.shader = new PShader(papplet, src_vert, src_frag);
    
//    this.shader        = papplet.loadShader(dir+"skylight.frag", dir+"skylight.vert");
    this.scene_display = scene_display;
    this.geombuffer    = geombuffer;
    this.shadowmap     = shadowmap;

    resize(papplet.width, papplet.height);
  }
  
  public void resize(int w, int h){
    for(int i = 0; i < pg_shading.length; i++){
      pg_shading[i] = (PGraphics3D) papplet.createGraphics(w, h, PConstants.P3D);
      pg_shading[i].smooth(0);
      pg_shading[i].textureSampling(5);
      
      DwGLTextureUtils.changeTextureFormat(pg_shading[i], GL2.GL_R32F, GL2.GL_RED, GL2.GL_FLOAT);
      
      pg_shading[i].beginDraw();
      pg_shading[i].hint(PConstants.DISABLE_TEXTURE_MIPMAPS);
      pg_shading[i].background(0xFFFFFFFF);
      pg_shading[i].blendMode(PConstants.REPLACE);
      pg_shading[i].shader(shader);
      pg_shading[i].noStroke();
      pg_shading[i].endDraw();
    }
    reset();
  }

  
  public void generateSampleDirection(){
   
    // create shadowmap direction
    float[] center = {0,0,0};
    

    float[] up = DwSampling.uniformSampleSphere_Halton(RENDER_PASS+1);
    
    // create new sample direction
//    float[] sample = DwSampling.uniformSampleHemisphere_Halton(RENDER_PASS+2);
    float[] sample = DwSampling.uniformSampleSphere_Halton(RENDER_PASS+2);
    float[] eye = new float[3];
    eye[0] = sample[0] * param.sample_focus;
    eye[1] = sample[1] * param.sample_focus;
    eye[2] = sample[2] + (1.0f-param.sample_focus);
    
    // project to bounding-sphere
    float dd = (float)Math.sqrt(eye[0]*eye[0] + eye[1]*eye[1] + eye[2]*eye[2]);
    eye[0] /= dd;
    eye[1] /= dd;
    eye[2] /= dd;

    // rotate
    mat_sun.reset();
    mat_sun.rotateZ(param.solar_azimuth * TO_RAD);
    mat_sun.rotateY(param.solar_zenith  * TO_RAD);
    eye = mat_sun.mult(eye, new float[3]);
    samples.add(eye);
    
    shadowmap.setDirection(eye, center, up);
    
    
    
    
//    // create shadowmap direction
//    float[] center = {0,0,0};
//    float[] up = DwSampling.uniformSampleSphere_Halton(RENDER_PASS+1);
//    
//    // create new sample direction
////    float[] sample = DwSampling.uniformSampleHemisphere_Halton(RENDER_PASS+1);
//    float[] sample = DwSampling.uniformSampleSphere_Halton(RENDER_PASS+1);
//    float[] eye = new float[3];
//    eye[0] = sample[0];
//    eye[1] = sample[1];
//    eye[2] = sample[2];
//    
//    // project to bounding-sphere
//    float dd = (float)Math.sqrt(eye[0]*eye[0] + eye[1]*eye[1] + eye[2]*eye[2]);
//    eye[0] /=  dd;
//    eye[1] /=  dd;
//    eye[2] /=  dd;
//
//    // rotate
////    setOrientation(param.solar_azimuth, param.solar_zenith);
////    mat_sun.reset();
////    mat_sun.rotateZ(param.solar_azimuth * TO_RAD);
////    mat_sun.rotateY(param.solar_zenith  * TO_RAD);
////    eye = mat_sun.mult(eye, new float[3]);
//    samples.add(eye);
//    
//    shadowmap.setDirection(eye, center, up);
//    
     
  }
  

  public void setGeometryBuffer(DwScreenSpaceGeometryBuffer geombuffer){
    this.geombuffer = geombuffer;
  }
  
  
  public void update(){
    for(int i = 0; i < param.iterations; i++){
      updateStep();
    }
  }

  public void updateStep(){
    
    if( shadowmap.pg_shadowmap.width != param.shadowmap_size){
      shadowmap.resize(param.shadowmap_size);
    }
    
    // 1) shadow pass
    generateSampleDirection();
    shadowmap.update();

    PGraphics3D pg_dst = getDst();
    PGraphics3D pg_src = getSrc();
    
    // 2) render pass
    pg_dst.beginDraw();
    pg_dst.shader(shader);
    setUniforms();
    shader.set("tex_src", pg_src);
    pg_dst.resetMatrix();
    pg_dst.resetProjection();
    pg_dst.noStroke();
    pg_dst.fill(255);
    pg_dst.rect(-1,-1,2,2);
    pg_dst.endDraw();
    DwGLTextureUtils.swap(pg_shading);

    RENDER_PASS++;
  }
  
  public PGraphics3D getSrc(){ return pg_shading[0]; }
  public PGraphics3D getDst(){ return pg_shading[1]; }
  

  void setUniforms() {

    // 1) modelview (camera) -> model (world)
    PMatrix3D mat_modelviewInv = geombuffer.pg_geom.modelviewInv.get();

    // camera -> world -> shadowmap
    PMatrix3D mat_shadow = shadowmap.getShadowmapMatrix();
    mat_shadow.apply(mat_modelviewInv);
    mat_shadow.transpose(); // processing
    
    
    PMatrix3D mat_shadow_normal = mat_shadow.get();
    mat_shadow_normal.invert();
    mat_shadow_normal.transpose(); // processing
    
    PMatrix3D mat_shadow_normal_modelview = shadowmap.getModelView().get();
    mat_shadow_normal_modelview.apply(mat_modelviewInv);
    mat_shadow_normal_modelview.transpose(); // processing
    mat_shadow_normal_modelview.invert();
    mat_shadow_normal_modelview.transpose(); // processing
    
    PMatrix3D mat_shadow_normal_projection = shadowmap.getProjection().get();
    mat_shadow_normal_projection.invert();
    mat_shadow_normal_projection.transpose(); // processing
    

    
//    PMatrix3D mat_shadow_modelview = new PMatrix3D(shadowmap.pg_shadowmap.modelview);
//    mat_shadow_modelview.apply(mat_modelviewInv);
//    mat_shadow_modelview.transpose();

    // 2) transform light direction into camera space = inverse-transpose-modelView * direction
    mat_modelviewInv.transpose();
    PVector light_dir_cameraspace = mat_modelviewInv.mult(shadowmap.lightdir, null);
    light_dir_cameraspace.normalize();
    
    // projection matrix of the geometry buffer
    PMatrix3D mat_projection = geombuffer.pg_geom.projection.get();
    mat_projection.transpose(); // processing
    
    // temporal averaging
    float pass_mix = RENDER_PASS/(RENDER_PASS+1.0f);
    
    float w_shadow = shadowmap.pg_shadowmap.width;
    float h_shadow = shadowmap.pg_shadowmap.height;
    
    // shadow offset
    float shadow_map_size = Math.min(w_shadow, h_shadow);
    float shadow_bias_mag = 0.33f/shadow_map_size;
    
//    shadow_bias_mag = scene_scale/ shadow_map_size;

    float w = geombuffer.pg_geom.width;
    float h = geombuffer.pg_geom.height;
    
//    PMatrix3D mat_screen_to_eye = new PMatrix3D();
//    mat_screen_to_eye.scale(w, h, 1);
//    mat_screen_to_eye.scale(0.5f);
//    mat_screen_to_eye.translate(1,1,1);
//    mat_screen_to_eye.apply(pg.projection);
//    mat_screen_to_eye.invert();
//    mat_screen_to_eye.transpose(); // processing, row-col switch
    

    
    // 3) update shader uniforms
    shader.set("dir_light", light_dir_cameraspace);
    shader.set("mat_shadow", mat_shadow);
//    shader.set("mat_shadow_normal", mat_shadow_normal);
    shader.set("mat_shadow_normal_modelview", mat_shadow_normal_modelview, true);
    shader.set("mat_shadow_normal_projection", mat_shadow_normal_projection, true);
    

    
//    shader.set("mat_screen_to_eye", mat_screen_to_eye);
    shader.set("mat_projection", mat_projection);
//    shader.set("mat_shadow_modelview", mat_shadow_modelview);
    shader.set("tex_shadow", shadowmap.pg_shadowmap);
    shader.set("tex_geombuffer", geombuffer.pg_geom);
    
    shader.set("pass_mix", pass_mix);
    shader.set("wh", w, h); // should match the dimensions of the shading buffers
    shader.set("wh_shadow", w_shadow, h_shadow); // should match the dimensions of the shading buffers
    shader.set("shadow_bias_mag", shadow_bias_mag);
//    shader.set("singlesided", param.singlesided ? 1 : 0);
  }


  public void reset(){
    for(int i = 0; i < pg_shading.length; i++){
      pg_shading[i].beginDraw();
      pg_shading[i].hint(PConstants.DISABLE_DEPTH_TEST);
      pg_shading[i].blendMode(PConstants.REPLACE);
      pg_shading[i].textureSampling(0);
      pg_shading[i].hint(PConstants.DISABLE_TEXTURE_MIPMAPS);
      pg_shading[i].clear();
      pg_shading[i].resetMatrix();
      pg_shading[i].resetProjection();
      pg_shading[i].noStroke();
      pg_shading[i].endDraw();
    }
    RENDER_PASS = 0;
    samples.clear();
  }




}
