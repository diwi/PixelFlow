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




package Skylight.Skylight_PoissonSphereSamples;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.geometry.DwIcosahedron;
import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSetAble;
import com.thomasdiewald.pixelflow.java.geometry.DwMeshUtils;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.sampling.PoissonDiscSamping3D;
import com.thomasdiewald.pixelflow.java.sampling.PoissonSample;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics3D;

public class Skylight_PoissonSphereSamples extends PApplet {
  
  //
  // Animated Spheres (Poisson Distribution) + Skylight Renderer
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
  float rmin = 40;
  float rmax = 100;
  float roff = 1;
  int new_points = 50;
  
  PoissonDiscSamping3D<MyPoissonSample> pds;
  PShape shp_samples_spheres;
  PShape shp_samples_points;
  
  
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
    skylight.sky.param.iterations     = 60;
    skylight.sky.param.solar_azimuth  = 0;
    skylight.sky.param.solar_zenith   = 0;
    skylight.sky.param.sample_focus   = 1; // full sphere sampling
    skylight.sky.param.intensity      = 1.0f;
    skylight.sky.param.rgb            = new float[]{1,1,1};
    skylight.sky.param.shadowmap_size = 512; // quality vs. performance
    
    // parameters for sun-light
    skylight.sun.param.iterations     = 60;
    skylight.sun.param.solar_azimuth  = 45;
    skylight.sun.param.solar_zenith   = 55;
    skylight.sun.param.sample_focus   = 0.10f;
    skylight.sun.param.intensity      = 1.0f;
    skylight.sun.param.rgb            = new float[]{1,1,1};
    skylight.sun.param.shadowmap_size = 512;
    
    
    
    generatePoissonSampling();

    frameRate(60);
  }
  
  
  static class MyPoissonSample extends PoissonSample{
    public float anim_rad = 0;
    public float anim_speed = 0;
    public float rad_inc = 0.01f;
 
    public MyPoissonSample(float x, float y, float z, float r, float r_collision) {
      super(x, y, z, r, r_collision);
    }
        
    public void initAnimationRadius(){
      anim_speed = 0.015f + (float) (Math.random() * 0.04f);
      rad_inc = (float) Math.PI;
    }
    
    public void updateAnimation(){
      rad_inc += anim_speed;
      anim_rad = (float) (Math.sin(rad_inc) * 0.5 + 0.5f); //[0, 1]
      anim_rad *= 1.5f;
      float min_scale = 0.2f;
      anim_rad = min_scale + anim_rad *(1 - min_scale);
    }
  }
  
  
  public void generatePoissonSampling(){
    
    pds = new PoissonDiscSamping3D<MyPoissonSample>() {
      @Override
      public MyPoissonSample newInstance(float x, float y, float z, float r, float rcollision){
        return new MyPoissonSample(x,y,z,r,rcollision);
      }
    };
    
    pds.generatePoissonSampling(bounds, rmin, rmax, roff, new_points);
    
    shp_samples_spheres = createShape(GROUP);
    shp_samples_points  = createShape(GROUP);
    for(MyPoissonSample sample : pds.samples){
      addShape(sample);
      sample.initAnimationRadius();
    }
    
  }
  
  
  public void updateSpheres(){
    PShape[] shp_spheres = shp_samples_spheres.getChildren();
    for(int i = 0; i < shp_spheres.length; i++){
      PShape shp_sphere = shp_spheres[i];
      MyPoissonSample sample = pds.samples.get(i);
      sample.updateAnimation();
      shp_sphere.resetMatrix();
      shp_sphere.scale(sample.anim_rad);
      shp_sphere.translate(sample.x(), sample.y(), sample.z());
    }
  }
  


  public void draw(){
    updateCamActiveStatus();
    
    if(UPDATE_SPHERES){
      updateSpheres();
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
    int triangles = ifs.getFacesCount() * pds.samples.size();
    String txt_fps = String.format(getClass().getName()+ "  [sun: %d]  [sky: %d]  [triangles: %d]  [fps %6.2f]", sun_pass, sky_pass, triangles, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public void displayScene(PGraphics3D canvas){
    if(canvas == skylight.renderer.pg_render){
      canvas.background(16);
      displaySamples(canvas);
    }

    canvas.shape(shp_samples_spheres);
    
    // ground plane
    float sx = bounds[3] - bounds[0];
    float sy = bounds[4] - bounds[1];
    float sz = bounds[5] - bounds[2];
    
    canvas.noStroke();
    canvas.fill(16,64,180);
    canvas.box(sx*1.5f, sy*1.5f, 10);
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
  
  
  
  
  
  DwIndexedFaceSetAble ifs;
  
  int verts_per_face = 0;
  
  void addShape(MyPoissonSample sample){
    PShape shp_point = createShape(POINT, sample.x(), sample.y(), sample.z());
    shp_point.setStroke(color(255));
    shp_point.setStrokeWeight(3);
    shp_samples_points.addChild(shp_point);
    

    if(ifs == null){
      ifs = new DwIcosahedron(2); verts_per_face = 3;
//      ifs = new DwCube(3); verts_per_face = 4;
    }
    PShape shp_sphere = createShape(PShape.GEOMETRY);
    shp_sphere.setStroke(false);
    shp_sphere.setFill(color(255));
    
    colorMode(HSB, 360, 1, 1);
    float hsb_h = 15 + (float)(Math.random() - 0.5f) * 45 ;
    float hsb_s = (float) Math.random() * 0.99f + 0.01f;
    float hsb_b = hsb_s*3;
    
    shp_sphere.setFill(color(hsb_h, hsb_s, hsb_b));
    colorMode(RGB);
    
    shp_sphere.resetMatrix();
    shp_sphere.translate(sample.x(), sample.y(), sample.z());
    
    boolean flat_shading = random(1) > 0.5f;
   
    DwMeshUtils.createPolyhedronShape(shp_sphere, ifs, sample.rad(), verts_per_face, flat_shading);
    
    shp_samples_spheres.addChild(shp_sphere);
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
    if(key == 'r') generatePoissonSampling();
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_PoissonSphereSamples.class.getName() });
  }
}