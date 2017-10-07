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




package Skylight.Skylight_Movie;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;
import processing.video.Capture;

public class Skylight_Capture1 extends PApplet {
  
  //
  // Animated Cubes + Skylight Renderer
  // 
  // ... for testing and tweaking interactivity and realtime behaviour.
  //
  // AntiAliasing: SMAA
  // Bloom Shader
  //
  //
  // -- CONTROLS -- 
  //
  // LMB: camera orbit
  // MMB: camera pan
  // RMB: camera zoom
  //
  // key ' ': toggle sphere animation
  // key 's': toggle skylight samples display
  // key 'b': toggle bloom
  // key 'r': restart
  //
  //
  
  

  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  
  PeasyCam cam;
  
  float[] bounds = {-500,-500,0, 500, 500, 500};

  int src_w = 640;
  int src_h = 480;
  
  
  PShape group_cubes;
  

  
  // camera capture (video library)
  Capture capture;

  
  // renderer
  DwSkyLight skylight;
 
  PMatrix3D mat_scene_bounds;
  
  
  DwOpticalFlow opticalflow;
  PGraphics2D pg_oflow;
  PGraphics2D pg_src;
  
  
  DwPixelFlow context;
  SMAA smaa;
  
  PGraphics3D pg_aa;
  PGraphics3D pg_tmp;
  
  // state variables
  boolean DISPLAY_RADIUS = true;
  boolean GENERATE_SPHERES = true;
  
  // key 'b'
  boolean APPLY_BLOOM = !true;
  
  public void settings(){
    size(viewport_w, viewport_h, P3D);
    smooth(0);
  }
  
  
  public void setup(){
    surface.setLocation(viewport_x, viewport_y);
    
    cam = new PeasyCam(this, 0, 0, 0, 1400);
    
    // webcam capture
    capture = new Capture(this, src_w, src_h, 30);
    capture.start();
    
    perspective(60 * PI/180f, width/(float)height, 2, 4000);

    float sx = bounds[3] - bounds[0];
    float sy = bounds[4] - bounds[1];
    float sz = bounds[5] - bounds[2];
    
    float px = (bounds[3] + bounds[0]) * 0.5f;
    float py = (bounds[4] + bounds[1]) * 0.5f;
    float pz = (bounds[5] + bounds[2]) * 0.5f;
    float rad = (float)(Math.sqrt(sx*sx + sy*sy + sz*sz) * 0.5f);
    
    DwBoundingSphere scene_bs = new DwBoundingSphere();
    scene_bs.set(px, py, pz, rad*1.5f);
    
    mat_scene_bounds = scene_bs.getUnitSphereMatrix();

    // callback for rendering the scene
    DwSceneDisplay scene_display = new DwSceneDisplay(){
      @Override
      public void display(PGraphics3D canvas) {
        displayScene(canvas);  
      }
    };
    
    // library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    

    
    // opticalflow
    opticalflow = new DwOpticalFlow(context, src_w, src_h);
    
    // some flow parameters
    opticalflow.param.flow_scale         = 100;
    opticalflow.param.temporal_smoothing = 0.5f;
    opticalflow.param.display_mode       = 0;
    opticalflow.param.grayscale          = true;
    
    // render target
    pg_oflow = (PGraphics2D) createGraphics(src_w, src_h, P2D);
    pg_oflow.smooth(8);

    // drawing canvas, used as input for the optical flow
    pg_src = (PGraphics2D) createGraphics(src_w, src_h, P2D);
    pg_src.smooth(8);
    
    
    
    smaa = new SMAA(context);
    // postprocessing AA
    pg_aa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_aa.smooth(0);
    pg_aa.textureSampling(5);


    pg_tmp = (PGraphics3D) createGraphics(width, height, P3D);
    pg_tmp.smooth(0);
    pg_tmp.textureSampling(5);
    
    
    
    // init skylight renderer
    skylight = new DwSkyLight(context, scene_display, mat_scene_bounds);
    
    // parameters for sky-light
    skylight.sky.param.iterations     = 100;
    skylight.sky.param.solar_azimuth  = 0;
    skylight.sky.param.solar_zenith   = 0;
    skylight.sky.param.sample_focus   = 1f; // full sphere sampling
    skylight.sky.param.intensity      = 2.0f;
    skylight.sky.param.rgb            = new float[]{1,1,1};
    skylight.sky.param.shadowmap_size = 512; // quality vs. performance
    
    // parameters for sun-light
    skylight.sun.param.iterations     = 100;
    skylight.sun.param.solar_azimuth  = 35;
    skylight.sun.param.solar_zenith   = 45;
    skylight.sun.param.sample_focus   = 0.1f;
    skylight.sun.param.intensity      = 1.0f;
    skylight.sun.param.rgb            = new float[]{1,1,1};
    skylight.sun.param.shadowmap_size = 512;
    
    
    createScene();
    
    frameRate(60);
  }
  
  
  // animated rectangle data
  float rs = 80;
  float rx = 100;
  float ry = 100;
  float dx = 3;
  float dy = 2.4f;
  
  public void updateSrc(){
    
    float w = pg_src.width;
    float h = pg_src.height;
    
    // update rectangle position
    rx += dx;
    ry += dy;
    // keep inside viewport
    if(rx <   rs/2) {rx =   rs/2; dx = -dx; }
    if(rx > w-rs/2) {rx = w-rs/2; dx = -dx; }
    if(ry <   rs/2) {ry =   rs/2; dy = -dy; }
    if(ry > h-rs/2) {ry = h-rs/2; dy = -dy; }
    
    // update input image
    pg_src.beginDraw();
    pg_src.clear();
    pg_src.background(160,96,64);
    
    pg_src.rectMode(CENTER);
    pg_src.fill(150, 200, 255);
    pg_src.rect(rx, ry, rs, rs, rs/3f);
    
    pg_src.fill(200, 150, 255);
    pg_src.noStroke();
    pg_src.ellipse(mouseX, mouseY, 100, 100);
    pg_src.endDraw();
    


  }

  

  public void draw(){
    updateCamActiveStatus();
    

    
    if(UPDATE_SPHERES && capture.available()){
      capture.read();

      pg_src.beginDraw();
      pg_src.image(capture, 0, 0);
      pg_src.endDraw();
      
//      updateSrc();
      updateAnim();
    }

    if(UPDATE_SPHERES || CAM_ACTIVE){
      skylight.reset();
    }
    
    skylight.update();

    
    // AntiAliasing ... SMAA
    smaa.apply(skylight.renderer.pg_render, pg_aa);
    
    if(APPLY_BLOOM){
      DwFilter filter = DwFilter.get(context);
      
      filter.luminance_threshold.param.threshold = 0.5f;
      filter.luminance_threshold.param.exponent  = 1;
      
      filter.luminance_threshold.apply(pg_aa, pg_tmp);
      
      filter.bloom.param.mult   =  0.2f; //map(mouseX, 0, width, 0, 1);
      filter.bloom.param.radius =  0.6f; //map(mouseY, 0, height, 0, 1);
      
      filter.bloom.apply(pg_tmp, pg_tmp, pg_aa);
    }
    
    // render Optical Flow
    pg_oflow.beginDraw();
    pg_oflow.clear();
    pg_oflow.endDraw();
    
    opticalflow.param.display_mode = (mousePressed && mouseButton == CENTER) ? 1 : 0;
    opticalflow.renderVelocityStreams(pg_oflow, 10);

    cam.beginHUD();
    blendMode(REPLACE);
    image(pg_aa, 0, 0);
    
    float w = 200;
    float h = w * pg_oflow.height / (float)pg_oflow.width;
    
    image(pg_oflow, 0, 0, w, h);
    blendMode(BLEND);
    cam.endHUD();
    
    // some info, window title
    int sun_pass = skylight.sun.RENDER_PASS;
    int sky_pass = skylight.sky.RENDER_PASS;
    int num_cubes = pg_src_small == null ? 0 : pg_src_small.pixels.length;
    String txt_fps = String.format(getClass().getName()+ "  [sun: %d]  [sky: %d]  [cubes: %d]  [fps %6.2f]", sun_pass, sky_pass, num_cubes, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public void displayScene(PGraphics3D canvas){
    if(canvas == skylight.renderer.pg_render){
      canvas.background(16);
      displaySamples(canvas);
    }
    
    // ground plane
    float sx = bounds[3] - bounds[0];
    float sy = bounds[4] - bounds[1];
    float sz = bounds[5] - bounds[2];

    canvas.pushMatrix();
    canvas.translate(0, 0, 20 + sz/2);
    canvas.rotateX(-PI/2);
    canvas.shape(group_cubes);
    canvas.popMatrix();

    
    canvas.noStroke();
    canvas.fill(16,64,180);
//    canvas.fill(180,120,64);
//    canvas.box(sx*1.5f, sy*1.5f, 10);
    canvas.box(sx, sy, 10);
  }
  
  
  boolean DISPLAY_SAMPLES_SUN = true;
  boolean DISPLAY_SAMPLES_SKY = true;
  
  public void displaySamples(PGraphics canvas){
    if(!DISPLAY_SAMPLES){
      return;
    }
    canvas.pushMatrix();

    PMatrix3D mat_samples = mat_scene_bounds.get();
    mat_samples.invert();
    
    // draw sunlight samples
    if(DISPLAY_SAMPLES_SUN){
      canvas.stroke(255,200,0);
      canvas.strokeWeight(2);
      canvas.beginShape(POINTS);
      for(float[] pos : skylight.sun.samples){
        pos = mat_samples.mult(pos, null);
        canvas.vertex(pos[0], pos[1], pos[2]);
      }
      canvas.endShape();
    }
    
    // draw skylight samples
    if(DISPLAY_SAMPLES_SKY){
      canvas.stroke(0,200,255);
      canvas.strokeWeight(1);
      canvas.beginShape(POINTS);
      for(float[] pos : skylight.sky.samples){
        pos = mat_samples.mult(pos, null);
        canvas.vertex(pos[0], pos[1], pos[2]);
      }
      canvas.endShape();
    }
    
    canvas.popMatrix();
  }
  
  
  
 
  
  float[] cam_pos = new float[3];
  boolean CAM_ACTIVE = false;
  
  public void updateCamActiveStatus(){
    float[] cam_pos_curr = cam.getPosition();
    CAM_ACTIVE = false;
    CAM_ACTIVE |= cam_pos_curr[0] != cam_pos[0];
    CAM_ACTIVE |= cam_pos_curr[1] != cam_pos[1];
    CAM_ACTIVE |= cam_pos_curr[2] != cam_pos[2];
    cam_pos = cam_pos_curr;
  }
  
  

  boolean UPDATE_SPHERES = true;
  boolean DISPLAY_SAMPLES = false;
  
  PShape shp_gizmo;

  public void displayGizmo(float s){
    if(shp_gizmo == null){
      strokeWeight(1);
      shp_gizmo = createShape();
      shp_gizmo.beginShape(LINES);
      shp_gizmo.stroke(255,0,0); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(s,0,0);
      shp_gizmo.stroke(0,255,0); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(0,s,0); 
      shp_gizmo.stroke(0,0,255); shp_gizmo.vertex(0,0,0); shp_gizmo.vertex(0,0,s); 
      shp_gizmo.endShape();
    }
    shape(shp_gizmo);
  }
  
  
  public void keyReleased(){
    if(key == ' ') UPDATE_SPHERES = !UPDATE_SPHERES;
    if(key == 's') DISPLAY_SAMPLES = !DISPLAY_SAMPLES;
    
    if(key == 'b') APPLY_BLOOM = !APPLY_BLOOM;
    if(key == 'r') createScene();
  }
  
  PShape[] shp_cubes;
  
  
  
  DwGLTexture tex_vel_small = new DwGLTexture();
  PGraphics2D pg_src_small;
  PGraphics2D pg_src_small_tmp;
  
  PGraphics2D pg_src_tmp;
  
  public void createScene(){
    
    int scale = 5;
    
    int src_w = pg_src.width;
    int src_h = pg_src.height;
    
    int num_x = src_w/scale;
    int num_y = src_h/scale;

    pg_src_small     = (PGraphics2D) createGraphics(num_x, num_y, P2D);
    pg_src_small_tmp = (PGraphics2D) createGraphics(num_x, num_y, P2D);
    
    pg_src_small.loadPixels();
    
    opticalflow.resize(src_w, src_h);
    
    pg_src_tmp = (PGraphics2D) createGraphics(src_w, src_h, P2D);
    
    tex_vel_small.resize(context, opticalflow.frameCurr.velocity);
    tex_vel_small.resize(context, num_x, num_y);
    
    pg_oflow = (PGraphics2D) createGraphics(src_w, src_h, P2D);

    group_cubes = createShape(GROUP);
    shp_cubes = new PShape[num_x * num_y];
    
    for(int y = 0; y < num_y; y++){
      for(int x = 0; x < num_x; x++){
        int idx = y * num_x + x;

        PShape shp_cube = createShape(BOX, 1, 1, 1);
        shp_cube.setStroke(false);
        shp_cube.setStroke(color(0));
        shp_cube.setFill(true);
        shp_cubes[idx] = shp_cube;
        group_cubes.addChild(shp_cube);
      }
    }
  }
  


  
  float[] flow;
  
  public void updateAnim(){
    
    if(pg_src_small == null){
      createScene();
    }
    
    int num_x = pg_src_small.width;
    int num_y = pg_src_small.height;
    
    DwFilter.get(context).gaussblur.apply(pg_src, pg_src, pg_src_tmp, 3);
    
    pg_src_small.beginDraw();
    pg_src_small.image(pg_src, 0, 0, num_x, num_y);
    pg_src_small.endDraw();
    
    opticalflow.update(pg_src);
    DwFilter.get(context).copy.apply(opticalflow.frameCurr.velocity, tex_vel_small);
    flow = tex_vel_small.getFloatTextureData(flow);

    DwFilter.get(context).gaussblur.apply(pg_src_small, pg_src_small, pg_src_small_tmp, 3);
    
    pg_src_small.loadPixels();
    



    float scene_dimx = bounds[3] - bounds[0];
    float scene_dimy = bounds[4] - bounds[1];
    float scene_dimz = bounds[5] - bounds[2];
    float bounds_off = 100;

    float dimx = (scene_dimx - bounds_off*2) / num_x;
    float dimy = (scene_dimy - bounds_off*2) / num_y;
    
    float dim = min(dimx, dimy);

    float tx = -dim * num_x * 0.5f;
    float ty = -dim * num_y * 0.5f;
    float tz = 10;

    for(int y = 0; y < num_y; y++){
      for(int x = 0; x < num_x; x++){
        int idx = y * num_x + x;

        int rgb = pg_src_small.pixels[idx];
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b = (rgb >>  0) & 0xFF;
        
        int flow_idx = (num_y - y - 1) * num_x + x;
        float flows = 3;
        float flowx = flow[flow_idx * 2 + 0] * +flows;
        float flowy = flow[flow_idx * 2 + 1] * -flows;
        
        float flow_mm = flowx*flowx + flowy*flowy;
        float flow_m = (float) Math.pow(flow_mm, 0.5f);
        
  
        float gray = (r + g + b) / (3f * 255f);
        
        float px = x * dim;
        float py = y * dim;
        float pz = scene_dimz * gray * 0.25f + +flow_m;
        
        pz = max(pz, 0);

        
        PShape cube = shp_cubes[idx];

        cube.resetMatrix();
        cube.scale(dim);
        cube.translate(tx+px, ty+py, tz+pz);
 
        cube.setFill(rgb);
      }
    }
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_Capture1.class.getName() });
  }
}