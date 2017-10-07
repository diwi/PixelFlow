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




package Skylight.Skylight_Cubes;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics3D;

public class Skylight_Cubes extends PApplet {
  
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
  
  float[] bounds = {-500,-500,0, 500, 500, 1000};

  int num_x = 10;
  int num_y = 10;
//  int cubes_dimz = 100;
  
  PShape group_cubes;

  
  // renderer
  DwSkyLight skylight;
 
  PMatrix3D mat_scene_bounds;
  
  
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

  

  public void draw(){
    updateCamActiveStatus();
    
    if(UPDATE_SPHERES){
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

    cam.beginHUD();
    blendMode(REPLACE);
    image(pg_aa, 0, 0);
    blendMode(BLEND);
    cam.endHUD();
    
    // some info, window title
    int sun_pass = skylight.sun.RENDER_PASS;
    int sky_pass = skylight.sky.RENDER_PASS;
    int num_cubes = num_x * num_y;
    String txt_fps = String.format(getClass().getName()+ "  [sun: %d]  [sky: %d]  [cubes: %d]  [fps %6.2f]", sun_pass, sky_pass, num_cubes, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public void displayScene(PGraphics3D canvas){
    if(canvas == skylight.renderer.pg_render){
      canvas.background(16);
      displaySamples(canvas);
    }

    canvas.shape(group_cubes);
    
    // ground plane
    float sx = bounds[3] - bounds[0];
    float sy = bounds[4] - bounds[1];
    float sz = bounds[5] - bounds[2];
    
    canvas.noStroke();
    canvas.fill(16,64,180);
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
  
  public void createScene(){
    
//    int res = 4;
    
    num_x = 100;
    num_y = 100;
    
    
    if(group_cubes == null){
      group_cubes = createShape(GROUP);
      shp_cubes = new PShape[num_x * num_y];
    }
    

    
    float scene_dimx = bounds[3] - bounds[0];
    float scene_dimy = bounds[4] - bounds[1];
    float scene_dimz = bounds[5] - bounds[2];
    
    float bounds_off = 100;
    
    float dimx = (scene_dimx - bounds_off*2) / num_x;
    float dimy = (scene_dimy - bounds_off*2) / num_y;
    
    float dim = min(dimx, dimy);

    float tx = -dim * num_x * 0.5f;
    float ty = -dim * num_y * 0.5f;
    
    float dims = 1;
    
    for(int y = 0; y < num_y; y++){
      for(int x = 0; x < num_x; x++){
        int idx = y * num_x + x;
//        float dim_z = random(scene_dimz/4, scene_dimz);
        float nval = noise(3*x/(float)num_x, 2*y/(float)num_y);
        float dim_z = scene_dimz * nval*nval;
        PShape shp_cube = createShape(BOX, dim*dims, dim*dims, dim*dims*1);
        shp_cube.setStroke(false);
        shp_cube.setStroke(color(0));
        shp_cube.setFill(true);
        shp_cube.setFill(color(255, 255*nval,255*nval*nval));
        shp_cube.translate(tx + x * dim, ty + y * dim, dim_z);
        group_cubes.addChild(shp_cube);
        shp_cubes[idx] = shp_cube;
      }
    }
    

//    int cubes_dimz = 100;
  }
  
  float anim_counter = 0;
  
  public void updateAnim(){
    
    float scene_dimx = bounds[3] - bounds[0];
    float scene_dimy = bounds[4] - bounds[1];
    float scene_dimz = bounds[5] - bounds[2];
    
    float bounds_off = 100;
    
    float dimx = (scene_dimx - bounds_off*2) / num_x;
    float dimy = (scene_dimy - bounds_off*2) / num_y;
    
    float dim = min(dimx, dimy);

    float tx = -dim * num_x * 0.5f;
    float ty = -dim * num_y * 0.5f;
    
    anim_counter += 0.003f;
    
    float anim_frequency = 2;
    
    for(int y = 0; y < num_y; y++){
      for(int x = 0; x < num_x; x++){
        int idx = y * num_x + x;
        PShape shp_cube = shp_cubes[idx];
        
        float nval = noise(anim_counter + anim_frequency*x/(float)num_x, anim_counter + anim_frequency*y/(float)num_y);
        float dim_z = scene_dimz * nval * nval;
        
        float rgb_r = 255;
        float rgb_g = 255*nval;
        float rgb_b = 255*nval*nval;
        
        
        shp_cube.setFill(color(rgb_r, rgb_g, rgb_b));

        shp_cube.resetMatrix();
//        shp_cube.rotateY(nval*PI);
//        shp_cube.rotateX(nval*PI);
//        shp_cube.rotateZ(nval*PI);
        shp_cube.translate(tx + x * dim, ty + y * dim, dim_z);
//        shp_cube.rotateY(nval*PI/4f);
//        shp_cube.rotateX(nval*PI);
//        shp_cube.rotateZ(nval*PI);
        shp_cube.rotateZ(cos(nval*10));
//        shp_cube.rotateY(nval*PI/4f);
//        shp_cube.rotateZ(nval*PI);
//        shp_cube.rotateX(nval*PI);
//        shp_cube.rotateY(nval*PI);
      }
    }
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_Cubes.class.getName() });
  }
}