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







package com.thomasdiewald.pixelflow.java.render.skylight;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.opengl.PGraphics3D;
import processing.opengl.PShader;


/**
 * @author Thomas
 *
 */
public class DwSkyLightRenderer {
  
  
  static public class Param{
    public float gamma = 2.2f;
    public int   wh_sky_mult = 0;
    public int   wh_sun_mult = 0;
  }
  
  public Param param = new Param();
  
  
  String dir = DwPixelFlow.SHADER_DIR+"render/skylight/";
  
  DwPixelFlow context;
  public PApplet papplet;
  
  public PShader shader;
  public PGraphics3D pg_render;
  
  public DwSceneDisplay scene_display;
  public DwScreenSpaceGeometryBuffer geom;
  public DwSkyLightShader sky;
  public DwSkyLightShader sun;

  public DwSkyLightRenderer(DwPixelFlow context, DwSceneDisplay scene_display, DwScreenSpaceGeometryBuffer geom, DwSkyLightShader sky, DwSkyLightShader sun){
    this.context = context;
    this.papplet = context.papplet;
    
    String[] src_frag = context.utils.readASCIIfile(dir+"render.frag");
    String[] src_vert = context.utils.readASCIIfile(dir+"render.vert");

    this.shader = new PShader(papplet, src_vert, src_frag);
    
    this.scene_display = scene_display;
    this.geom = geom;
    this.sky = sky;
    this.sun = sun;
    
    resize(papplet.width, papplet.height);
  }
  
  
  public boolean resize(int w, int h){
    boolean[] resized = {false};
    
    pg_render = DwUtils.changeTextureSize(papplet, pg_render, w, h, 0, resized);
    
    int w_sky = Math.max(1, w >> param.wh_sky_mult);
    int h_sky = Math.max(1, h >> param.wh_sky_mult);
    int w_sun = Math.max(1, w >> param.wh_sun_mult);
    int h_sun = Math.max(1, h >> param.wh_sun_mult);
    
    resized[0] |= sky.resize(w_sky, h_sky);
    resized[0] |= sun.resize(w_sun, h_sun);
//    resized[0] |= geom.resize(w, h);
    
    return resized[0];
  }
  
  public void reset(){
    sun.reset();
    sky.reset();
  }
  

  public void update(){
    
    DwUtils.copyMatrices((PGraphics3D) papplet.g, pg_render);

    geom.update(pg_render);
    sky.update();
    sun.update();
    
    float w = pg_render.width;
    float h = pg_render.height;
    
    float sky_int = sky.param.intensity;
    float sun_int = sun.param.intensity;
    
    float[] sky_col = sky.param.rgb;
    float[] sun_col = sun.param.rgb;
    
    pg_render.beginDraw();
    pg_render.blendMode(PConstants.REPLACE);
    pg_render.clear();
    pg_render.shader(shader);
    shader.set("wh", w, h);
    shader.set("tex_sky", sky.getSrc());
    shader.set("tex_sun", sun.getSrc());
    shader.set("mult_sun", sun_col[0] * sun_int, sun_col[1] * sun_int, sun_col[2] * sun_int);
    shader.set("mult_sky", sky_col[0] * sky_int, sky_col[1] * sky_int, sky_col[2] * sky_int);
    shader.set("gamma", param.gamma);
    scene_display.display(pg_render);
    pg_render.endDraw();
  }
  

  
}


