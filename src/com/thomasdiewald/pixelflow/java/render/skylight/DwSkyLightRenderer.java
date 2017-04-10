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

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureUtils;
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
    
    resize();
  }
  
  
  public void resize(){
    int w = papplet.width;
    int h = papplet.height;
    
    pg_render = (PGraphics3D) papplet.createGraphics(w, h, PConstants.P3D);
    pg_render.smooth(0);
    pg_render.textureSampling(5);
    
    pg_render.beginDraw();
    pg_render.hint(PConstants.DISABLE_TEXTURE_MIPMAPS);
    pg_render.background(0);
    pg_render.blendMode(PConstants.REPLACE);
    pg_render.shader(shader);
    pg_render.noStroke();
    pg_render.endDraw(); 
  }
  
  public void updateMatrices(){
    DwGLTextureUtils.copyMatrices((PGraphics3D) papplet.g, pg_render);
  }
//  public int STEP = 0;
  
  public void update(){
    
//    updateMatrices();

    geom.update((PGraphics3D) papplet.g);

    sky.update();
    sun.update();
    
    PGraphics3D pg     = pg_render;
    PGraphics3D pg_sun = sun.getSrc();
    PGraphics3D pg_sky = sky.getSrc();
    
    float w = pg_render.width;
    float h = pg_render.height;
    
    float sky_int = sky.param.intensity;
    float sun_int = sun.param.intensity;
    
    float[] sky_col = sky.param.rgb;
    float[] sun_col = sun.param.rgb;
    
//    STEP = 0;
    pg.beginDraw();
    updateMatrices();
    pg.clear();
    pg.shader(shader);
    shader.set("wh", w, h);
    shader.set("tex_sky", pg_sky);
    shader.set("tex_sun", pg_sun);
    shader.set("mult_sun", sun_col[0] * sun_int, sun_col[1] * sun_int, sun_col[2] * sun_int);
    shader.set("mult_sky", sky_col[0] * sky_int, sky_col[1] * sky_int, sky_col[2] * sky_int);
    shader.set("gamma", param.gamma);
    scene_display.display(pg);
    
//    pg.resetMatrix();
//    pg.resetProjection();
//    pg.noStroke();
//    pg.fill(0);
//    pg.rect(-1,-1,2,2);
    
    
//    STEP = 1;
////    pg.clear();
//    pg.resetShader();
//    scene_display.display(pg);
    
    pg.endDraw();
    

  }
  
  
  public void reset(){
    sun.reset();
    sky.reset();
  }
  
}
