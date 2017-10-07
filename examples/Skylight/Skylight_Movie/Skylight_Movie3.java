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
import processing.video.Movie;

public class Skylight_Movie3 extends PApplet {
  
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
  Movie movie;

  
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
    
    cam = new PeasyCam(this, 0, 0, 0, 1000);
    
    // webcam capture
    // movie file is not contained in the library release
    // to keep the file size small. please use one of your own videos instead.
    movie = new Movie(this, "examples/data/Pulp_Fiction_Dance_Scene.mp4");
    movie.loop();

    
    perspective(60 * PI/180f, width/(float)height, 2, 4000);

    float sx = bounds[3] - bounds[0];
    float sy = bounds[4] - bounds[1];
    float sz = bounds[5] - bounds[2];
    
    float px = (bounds[3] + bounds[0]) * 0.5f;
    float py = (bounds[4] + bounds[1]) * 0.5f;
    float pz = (bounds[5] + bounds[2]) * 0.5f;
    float rad = (float)(Math.sqrt(sx*sx + sy*sy + sz*sz) * 0.5f);
    
    DwBoundingSphere scene_bs = new DwBoundingSphere();
    scene_bs.set(px, py, pz, rad);
    
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
    skylight.sun.param.iterations     = 80;
    skylight.sun.param.solar_azimuth  = 35;
    skylight.sun.param.solar_zenith   = 45;
    skylight.sun.param.sample_focus   = 0.05f;
    skylight.sun.param.intensity      = 1.0f;
    skylight.sun.param.rgb            = new float[]{1,1,1};
    skylight.sun.param.shadowmap_size = 512;
    
    
    
    
    // opticalflow
    opticalflow = new DwOpticalFlow(context, src_w, src_h);
    
    // some flow parameters
    opticalflow.param.flow_scale         = 100;
    opticalflow.param.temporal_smoothing = 0.8f;
    opticalflow.param.display_mode       = 0;
    opticalflow.param.grayscale          = true;
    opticalflow.param.threshold = 2;
    
    tex_vel_small.resize(context, opticalflow.frameCurr.velocity);
    

    resize();
    
    frameRate(60);
  }



  

  public void draw(){
    updateCamActiveStatus();

    
    if(UPDATE_SPHERES && movie.available()){
      movie.read();
      
      resize();

      pg_src.beginDraw();
      pg_src.image(movie, 0, 0);
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
    
    if(opticalflow != null){
      // render Optical Flow
      pg_oflow.beginDraw();
      pg_oflow.blendMode(REPLACE);
      pg_oflow.clear();
      pg_oflow.endDraw();
      
//      opticalflow.param.display_mode = 1;
//      opticalflow.renderVelocityStreams(pg_oflow, 10);
      opticalflow.renderVelocityShading(pg_oflow);
    }

    cam.beginHUD();
    blendMode(REPLACE);
    image(pg_aa, 0, 0);
    blendMode(BLEND);
    if(opticalflow != null){
      float w = 200;
      float h = w * pg_oflow.height / (float)pg_oflow.width;
      image(pg_src  , 0, 0, w, h);
      image(pg_oflow, 0, 0, w, h);
    }

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
      canvas.background(255);
    }
    
    // ground plane
    float sx = bounds[3] - bounds[0];
    float sy = bounds[4] - bounds[1];
    float sz = bounds[5] - bounds[2];
    
//    sx *= 2;
//    sy *= 2;
    canvas.pushMatrix();
    canvas.translate(0, 0, cube_size + cube_size * cube_numy /2);
    canvas.rotateX(-PI/2);
    canvas.shape(group_cubes);
    canvas.popMatrix();

//    canvas.beginShape(QUAD);
//    canvas.texture(pg_src);
//    canvas.textureMode(NORMAL);
//    canvas.vertex(bounds[0], bounds[1], 20, 0,0);
//    canvas.vertex(bounds[3], bounds[1], 20, 1,0);
//    canvas.vertex(bounds[3], bounds[4], 20, 1,1);
//    canvas.vertex(bounds[0], bounds[4], 20, 0,1);
//    canvas.endShape();
    
    canvas.noStroke();
    
    canvas.fill(255);
//    canvas.fill(16,64,255);
//    canvas.fill(96,160,255);
//    canvas.fill(180,120,64);
//    canvas.fill(255,180,128);
//    canvas.box(sx*1.5f, sy*1.5f, 10);
//    canvas.box(sx, sy, 10);
    
//    canvas.rect(-sx/2, -sy/2, sx, sy);
    canvas.ellipse(0,0, sx * SQRT2, sy * SQRT2);
  }
  
  float SQRT2 = (float) Math.sqrt(2);

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
  
  
  public void keyReleased(){
    if(key == ' ') UPDATE_SPHERES = !UPDATE_SPHERES;
    if(key == 's') DISPLAY_SAMPLES = !DISPLAY_SAMPLES;
    
    if(key == 'b') APPLY_BLOOM = !APPLY_BLOOM;
    if(key == 'r') resize();
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  PShape[] shp_cubes;
  
  
  
  
  
  DwGLTexture tex_vel_small = new DwGLTexture();
  PGraphics2D pg_src_small;
  PGraphics2D pg_src_small_tmp;
  
  PGraphics2D pg_src_tmp;
  
 
  
  public void resize(){
    
    if(!movie.available()){
      return;
    }
    
    int src_w = movie.width;
    int src_h = movie.height;
    
    if(src_w == 0 || src_h == 0){
      return;
    }
    
    if(pg_src != null){
      if(pg_src.width  == src_w && 
         pg_src.height == src_h) return;
    }
    
    movie.jump(80);
    
    // 1) x * y = count_limit
    // 2) x / y = ratio
    // x = ratio * y;
    // y * y * ratio = count_limit
    // y = sqrt(count_limit / ratio);

    int num_limit = 10000;
    float wh_ratio = src_w / (float) src_h;

    int num_y = (int) (Math.sqrt(num_limit / wh_ratio) + 0.5f);
    int num_x = (int) (num_y * wh_ratio + 0.5f);
    
    
    opticalflow.resize(src_w, src_h);
    tex_vel_small.resize(context, num_x, num_y);
    
    // render target
    pg_oflow = (PGraphics2D) createGraphics(src_w, src_h, P2D);
    pg_oflow.smooth(8);

    // drawing canvas, used as input for the optical flow
    pg_src = (PGraphics2D) createGraphics(src_w, src_h, P2D);
    pg_src.smooth(0);
    
    pg_src_tmp = (PGraphics2D) createGraphics(src_w, src_h, P2D);
    pg_src_tmp.smooth(0);

    
    pg_src_small = (PGraphics2D) createGraphics(num_x, num_y, P2D);
    pg_src_small.smooth(0);
    
    pg_src_small_tmp = (PGraphics2D) createGraphics(num_x, num_y, P2D);
    pg_src_small_tmp.smooth(0);
    
//    pg_src_small.loadPixels();
    

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
  
  int   cube_numx = 0;
  int   cube_numy = 0;
  float cube_size = 0;
  
  
  void swap(){
    PGraphics2D pg_tmp = pg_src;
    pg_src = pg_src_tmp;
    pg_src_tmp = pg_tmp;
  }
  
  public void updateAnim(){
    
   
    cube_numx = pg_src_small.width;
    cube_numy = pg_src_small.height;
    
  
    // apply filters (not necessary)
//    if(APPLY_GRAYSCALE){
//      DwFilter.get(context).luminance.apply(pg_movie_a, pg_movie_a);
//    }
//    if(APPLY_BILATERAL)
    DwFilter.get(context).gaussblur.apply(pg_src, pg_src, pg_src_tmp, 5);
    
//    {
//      DwFilter.get(context).bilateral.apply(pg_src, pg_src_tmp, 5, 0.10f, 4);
//      swap();
//    }
    

    
    pg_src_small.beginDraw();
    pg_src_small.image(pg_src, 0, 0, cube_numx, cube_numy);
    pg_src_small.endDraw();
    
    opticalflow.update(pg_src);
    DwFilter.get(context).copy.apply(opticalflow.frameCurr.velocity, tex_vel_small);
    flow = tex_vel_small.getFloatTextureData(flow);

    DwFilter.get(context).gaussblur.apply(pg_src_small, pg_src_small, pg_src_small_tmp, 1);
    
    pg_src_small.loadPixels();
    
    float scene_dimx = bounds[3] - bounds[0];
    float scene_dimy = bounds[4] - bounds[1];
    float scene_dimz = bounds[5] - bounds[2];
    float bounds_off = 100;

    float dimx = (scene_dimx - bounds_off*2) / cube_numx;
    float dimy = (scene_dimy - bounds_off*2) / cube_numy;
    
    cube_size = min(dimx, dimy);

    float tx = -cube_size * cube_numx * 0.5f;
    float ty = -cube_size * cube_numy * 0.5f;
    float tz = 10;

    for(int y = 0; y < cube_numy; y++){
      for(int x = 0; x < cube_numx; x++){
        int idx = y * cube_numx + x;

        int rgb = pg_src_small.pixels[idx];
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b = (rgb >>  0) & 0xFF;
        
        int flow_idx = (cube_numy - y - 1) * cube_numx + x;
        float flows = 3;
        float flowx = flow[flow_idx * 2 + 0] * +flows;
        float flowy = flow[flow_idx * 2 + 1] * -flows;
        
        float flow_mm = flowx*flowx + flowy*flowy;
        float flow_m = (float) Math.pow(flow_mm, 0.5f) * 0.75f;
        
  
        float gray = (r + g + b) / (3f * 255f);
        
        float px = x * cube_size;
        float py = y * cube_size;
        float pz = scene_dimz * gray * 0.25f;
        
        pz = max(pz, 0);

        
        PShape cube = shp_cubes[idx];

        cube.resetMatrix();
        cube.rotateZ((float) Math.atan2(flowy, flowx));
        cube.scale(cube_size + flow_m);
        cube.translate(tx+px, ty+py, tz+pz);
 
        cube.setFill(rgb);
      }
    }
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_Movie3.class.getName() });
  }
}