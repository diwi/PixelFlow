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





package Skylight.Skylight_Basic;

import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;
import com.thomasdiewald.pixelflow.java.utils.DwVertexRecorder;

import peasy.*;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics3D;


public class Skylight_Basic extends PApplet {
  
  //
  // Basic setup for the Skylight renderer.
  // 
  // Its important to compute or define a most optimal bounding-sphere for the
  // scene. This can be done manually or automatically, as shown in this example.
  // 
  // Any existing sketch utilizing the P3D renderer can be extended to use the 
  // Skylight renderer.
  //
  
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;

  // camera control
  PeasyCam peasycam;
  
  // scene to render
  PShape shape;

  // renderer
  DwSkyLight skylight;

  public void settings() {
    size(viewport_w, viewport_h, P3D);
    smooth(0);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);

    // camera
    peasycam = new PeasyCam(this, -4.083,  -6.096,   7.000, 100);
    peasycam.setRotations(  1.085,  -0.477,   2.910);

    // projection
    perspective(60 * DEG_TO_RAD, width/(float)height, 2, 1000);

    // load obj file into shape-object
    shape = loadShape("examples/data/skylight_demo_scene.obj");

    // record list of vertices of the given shape
    DwVertexRecorder vertex_recorder = new DwVertexRecorder(this, shape);
   
    // compute scene bounding-sphere
    DwBoundingSphere scene_bs = new DwBoundingSphere();
    scene_bs.compute(vertex_recorder.verts, vertex_recorder.verts_count);

    // used for centering and re-scaling the scene
    PMatrix3D mat_scene_bounds = scene_bs.getUnitSphereMatrix();

    // callback for rendering the scene
    DwSceneDisplay scene_display = new DwSceneDisplay(){
      @Override
      public void display(PGraphics3D canvas) {
        displayScene(canvas);  
      }
    };
    
    // library context
    DwPixelFlow context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // init skylight renderer
    skylight = new DwSkyLight(context, scene_display, mat_scene_bounds);
    
    // parameters for sky-light
    skylight.sky.param.iterations     = 50;
    skylight.sky.param.solar_azimuth  = 0;
    skylight.sky.param.solar_zenith   = 0;
    skylight.sky.param.sample_focus   = 1; // full sphere sampling
    skylight.sky.param.intensity      = 1.0f;
    skylight.sky.param.rgb            = new float[]{1,1,1};
    skylight.sky.param.shadowmap_size = 256; // quality vs. performance
    
    // parameters for sun-light
    skylight.sun.param.iterations     = 50;
    skylight.sun.param.solar_azimuth  = 45;
    skylight.sun.param.solar_zenith   = 55;
    skylight.sun.param.sample_focus   = 0.05f;
    skylight.sun.param.intensity      = 1.0f;
    skylight.sun.param.rgb            = new float[]{1,1,1};
    skylight.sun.param.shadowmap_size = 512;

    frameRate(1000);
  }

  

  public void draw() {
 
    // when the camera moves, the renderer restarts
    updateCamActiveStatus();
    if(CAM_ACTIVE){
      skylight.reset();
    }


    // update renderer
    skylight.update();
    
    peasycam.beginHUD();
    // display result
    image(skylight.renderer.pg_render, 0, 0);
//    image(skylight.sky.getSrc(), 0, 0);
    peasycam.endHUD();

    // some info, window title
    int sun_pass = skylight.sun.RENDER_PASS;
    int sky_pass = skylight.sky.RENDER_PASS;
    String txt_fps = String.format(getClass().getName()+ "  [sun: %d]  [sky: %d]  [fps %6.2f]", sun_pass, sky_pass, frameRate);
    surface.setTitle(txt_fps);
  }


  public void displayScene(PGraphics canvas){
    if(canvas == skylight.renderer.pg_render){
      canvas.background(32);
    }
    canvas.shape(shape);
  }
  
  
  float[] cam_pos = new float[3];
  boolean CAM_ACTIVE = false;
  
  public void updateCamActiveStatus(){
    float[] cam_pos_curr = peasycam.getPosition();
    CAM_ACTIVE = false;
    CAM_ACTIVE |= cam_pos_curr[0] != cam_pos[0];
    CAM_ACTIVE |= cam_pos_curr[1] != cam_pos[1];
    CAM_ACTIVE |= cam_pos_curr[2] != cam_pos[2];
    cam_pos = cam_pos_curr;
  }
  
  public void printCam(){
    float[] pos = peasycam.getPosition();
    float[] rot = peasycam.getRotations();
    float[] lat = peasycam.getLookAt();
    float   dis = (float) peasycam.getDistance();
    
    System.out.printf(Locale.ENGLISH, "position: (%7.3f, %7.3f, %7.3f)\n", pos[0], pos[1], pos[2]);
    System.out.printf(Locale.ENGLISH, "rotation: (%7.3f, %7.3f, %7.3f)\n", rot[0], rot[1], rot[2]);
    System.out.printf(Locale.ENGLISH, "look-at:  (%7.3f, %7.3f, %7.3f)\n", lat[0], lat[1], lat[2]);
    System.out.printf(Locale.ENGLISH, "distance: (%7.3f)\n", dis);
  }
  
  
  public void keyReleased(){
    printCam();
  }
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_Basic.class.getName() });
  }
}
