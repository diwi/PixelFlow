/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
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

    pg_render.beginDraw();
    pg_render.hint(PConstants.DISABLE_TEXTURE_MIPMAPS);
    pg_render.textureSampling(0);
    pg_render.background(0);
    pg_render.blendMode(PConstants.REPLACE);
    pg_render.shader(shader);
    pg_render.noStroke();
    pg_render.endDraw(); 
  }
  
  public void updateMatrices(){
    DwGLTextureUtils.copyMatrices((PGraphics3D) papplet.g, pg_render);
  }
  
  public void update(){
    
    updateMatrices();

    geom.update(pg_render);

    sky.update();
    sun.update();
    
    PGraphics3D pg     = pg_render;
    PGraphics3D pg_sun = sun.getSrc();
    PGraphics3D pg_sky = sky.getSrc();
    
    float w = pg_render.width;
    float h = pg_render.height;
    
    float[] sky_intensity = sky.param.vec3_intensity;
    float[] sun_intensity = sun.param.vec3_intensity;
    
    pg.beginDraw();
    pg.clear();
    pg.shader(shader);
    shader.set("wh", w, h);
    shader.set("tex_sky", pg_sky);
    shader.set("tex_sun", pg_sun);
    shader.set("mult_sun", sun_intensity[0], sun_intensity[1], sun_intensity[2]);
    shader.set("mult_sky", sky_intensity[0], sky_intensity[1], sky_intensity[2]);
    shader.set("gamma", param.gamma);
    scene_display.display(pg);
    pg.endDraw();
  }
  
  
  public void reset(){
    sun.reset();
    sky.reset();
  }
  
}
