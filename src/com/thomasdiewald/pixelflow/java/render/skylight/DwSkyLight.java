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
import processing.core.PMatrix3D;
import processing.opengl.PShader;


/**
 * @author Thomas
 *
 */
public class DwSkyLight {

  DwPixelFlow context;

  public PShader shader;

  public DwSceneDisplay scene_display;
  public DwScreenSpaceGeometryBuffer geom;
  public DwSkyLightShader sky;
  public DwSkyLightShader sun;
  public DwSkyLightRenderer renderer;
  

  public DwSkyLight(DwPixelFlow context, DwSceneDisplay scene_display, PMatrix3D mat_scene_bounds){

    this.scene_display = scene_display;
 
    int shadowmap_wh = 1024; // default value, can be resized anytime
    
    DwShadowMap sun_shadowmap = new DwShadowMap(context, shadowmap_wh, scene_display, mat_scene_bounds);
    DwShadowMap sky_shadowmap = new DwShadowMap(context, shadowmap_wh, scene_display, mat_scene_bounds);
    
    geom = new DwScreenSpaceGeometryBuffer(context, scene_display);
    
    sun = new DwSkyLightShader(context, scene_display, geom, sun_shadowmap);
    sky = new DwSkyLightShader(context, scene_display, geom, sky_shadowmap);
    
    // composition of sky and sun for rendering
    renderer = new DwSkyLightRenderer(context, scene_display, geom, sky, sun);
    
    // parameters for sky-light
    sky.param.solar_azimuth  = 0;
    sky.param.solar_zenith   = 0;
    sky.param.sample_focus   = 1;
    sky.param.rgb = new float[]{1,1,1};
    
    // parameters for sun-light
    sun.param.solar_azimuth  = 45;
    sun.param.solar_zenith   = 80;
    sun.param.sample_focus   = 0.05f;
    sun.param.rgb = new float[]{1,1,1};
  }
  
  public void update(){
    renderer.update();
  }
  public void reset(){
    renderer.reset();
  }

}
