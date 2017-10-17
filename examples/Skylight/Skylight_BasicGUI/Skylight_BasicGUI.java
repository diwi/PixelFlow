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





package Skylight.Skylight_BasicGUI;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLightShader;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;
import com.thomasdiewald.pixelflow.java.utils.DwColorPicker;
import com.thomasdiewald.pixelflow.java.utils.DwVertexRecorder;

import controlP5.Accordion;
import controlP5.CColor;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ControllerView;
import controlP5.Group;
import controlP5.Pointer;
import peasy.*;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics3D;


public class Skylight_BasicGUI extends PApplet {
  
  //
  // Basic setup for the Skylight renderer, including UI for debugging.
  //
  // Also in this example: the use of general Matrix operations, for centering 
  // and scaling the scene globally.
  //
  // AntiAliasing: SMAA
  //
  

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = viewport_w - gui_w;
  int gui_y = 0;

  // camera control
  PeasyCam peasycam;
  
  // scene to render
  PShape shape;

  // renderer
  DwSkyLight skylight;
  
  PMatrix3D mat_scene_view;
  PMatrix3D mat_scene_bounds;
  
  boolean DISPLAY_SAMPLES_SUN = false;
  boolean DISPLAY_SAMPLES_SKY = false;
  boolean DISPLAY_TEXTURES    = false;
  
  SMAA smaa;
  PGraphics3D pg_aa;
  
  public void settings() {
    size(viewport_w, viewport_h, P3D);
    smooth(0);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    float SCENE_SCALE = 500;
    
    // camera
    peasycam = new PeasyCam(this, SCENE_SCALE*1.5f);
    peasycam.setRotations(  1.085,  -0.477,   2.910);

    perspective(60 * DEG_TO_RAD, width/(float)height, 2, SCENE_SCALE * 250);


    // load obj file into shape-object
    shape = loadShape("examples/data/skylight_demo_scene.obj");
    
    // grayscale model
    shape.setFill(color(255));
    
    // record list of vertices of the given shape
    DwVertexRecorder vertex_recorder = new DwVertexRecorder(this, shape);
   
    // compute scene bounding-sphere
    DwBoundingSphere scene_bs = new DwBoundingSphere();
    scene_bs.compute(vertex_recorder.verts, vertex_recorder.verts_count);
    
    PMatrix3D mat_bs = scene_bs.getUnitSphereMatrix();
//    mat_bs.scale(1, -1, 1);
    //  mat_bs.transpose();
    //  mat_bs.rotateX(-PI/2);
    //  mat_bs.transpose();

    // matrix, to place (centering, scaling) the scene in the viewport
    mat_scene_view = new PMatrix3D();
    mat_scene_view.scale(SCENE_SCALE);
    mat_scene_view.apply(mat_bs);
    
    // matrix, to place the scene in the skylight renderer
    mat_scene_bounds = mat_scene_view.get();
    mat_scene_bounds.invert();
    mat_scene_bounds.preApply(mat_bs);

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
    skylight.sky.param.intensity      = 3.0f;
    skylight.sky.param.rgb            = new float[]{0.25f,0.50f,1.00f};
    skylight.sky.param.shadowmap_size = 512; // quality vs. performance
    
    // parameters for sun-light
    skylight.sun.param.iterations     = 50;
    skylight.sun.param.solar_azimuth  = 45;
    skylight.sun.param.solar_zenith   = 75;
    skylight.sun.param.sample_focus   = 0.02f;
    skylight.sun.param.intensity      = 2.0f;
    skylight.sun.param.rgb            = new float[]{1.00f,0.20f,0.00f};
    skylight.sun.param.shadowmap_size = 512;
    
    // postprocessing AA
    smaa = new SMAA(context);
    pg_aa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_aa.smooth(0);
    
    
    // cp5 gui
    createGUI();

    frameRate(1000);
  }

  

  public void draw() {
 
    peasycam.setActive(!cp5.isMouseOver());
    
    // when the camera moves, the renderer restarts
    updateCamActiveStatus();
    if(CAM_ACTIVE){
      skylight.reset();
    }

    // update renderer
    skylight.update();
    
    // apply AntiAliasing
    smaa.apply(skylight.renderer.pg_render, pg_aa);


    peasycam.beginHUD();
    // display result
    image(pg_aa, 0, 0);
    // display textures
    if(DISPLAY_TEXTURES){
      int dy = 10;
      int px = dy;
      int py = dy;
      int sx = 170;
      
      int shadow_sx = skylight.sun.shadowmap.pg_shadowmap.width;
      int shadow_sy = skylight.sun.shadowmap.pg_shadowmap.height;
      int view_sx   = skylight.geom.pg_geom.width;
      int view_sy   = skylight.geom.pg_geom.height;
      
      int sy_shadow = ceil(sx / (shadow_sx/(float)shadow_sy));
      int sy_geom   = ceil(sx / (view_sx  /(float)view_sy  ));
  
      noStroke();
      fill(0, 204);
      rect(0,0, sx + 2*dy, height);

      image(skylight.sun.shadowmap.pg_shadowmap, px, py              , sx, sy_shadow);
      image(skylight.sky.shadowmap.pg_shadowmap, px, py+=sy_shadow+dy, sx, sy_shadow);
      image(skylight.geom.pg_geom,               px, py+=sy_shadow+dy, sx, sy_geom);
      image(skylight.sun.getSrc(),               px, py+=sy_geom  +dy, sx, sy_geom);
      image(skylight.sky.getSrc(),               px, py+=sy_geom  +dy, sx, sy_geom); 
    }
    peasycam.endHUD();
    
    
    displayGUI();
    
    // some info, window title
    int sun_pass = skylight.sun.RENDER_PASS;
    int sky_pass = skylight.sky.RENDER_PASS;
    String txt_fps = String.format(getClass().getName()+ "  [sun: %d]  [sky: %d]  [fps %6.2f]", sun_pass, sky_pass, frameRate);
    surface.setTitle(txt_fps);
  }


  public void displayScene(PGraphics canvas){
    if(canvas == skylight.renderer.pg_render){
      canvas.background(32);
      displaySamples(canvas);
    }
    canvas.pushMatrix();
    canvas.applyMatrix(mat_scene_view);
    canvas.shape(shape);
    canvas.popMatrix();
  }
  
  
  public void displaySamples(PGraphics canvas){
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
    float[] cam_pos_curr = peasycam.getPosition();
    CAM_ACTIVE = false;
    CAM_ACTIVE |= cam_pos_curr[0] != cam_pos[0];
    CAM_ACTIVE |= cam_pos_curr[1] != cam_pos[1];
    CAM_ACTIVE |= cam_pos_curr[2] != cam_pos[2];
    cam_pos = cam_pos_curr;
  }
 

  ControlP5 cp5;
  
  public void displayGUI(){
    noLights();
    peasycam.beginHUD();
    cp5.draw();
    peasycam.endHUD();
  }
  
  public void controlEvent(ControlEvent ce) {
    String cname = ce.getName();
    boolean reset = false;
    reset |= cname.contains("solar_azimuth");
    reset |= cname.contains("solar_zenith");
    reset |= cname.contains("sample_focus");
    reset |= cname.contains("quality");
    if(reset){
      skylight.reset();
    }
  }

  public void displaySamples(float[] val){
    DISPLAY_SAMPLES_SKY = (val[0] > 0);
    DISPLAY_SAMPLES_SUN = (val[1] > 0);
    DISPLAY_TEXTURES    = (val[2] > 0);
  }
  
  float mult_fg = 1f;
  float mult_active = 2f;
  float CR = 32;
  float CG = 64;
  float CB = 128;
  int col_bg, col_fg, col_active;

  public void createGUI(){
    
    col_bg     = color(4, 220);
    col_fg     = color(CR*mult_fg, CG*mult_fg, CB*mult_fg);
    col_active = color(CR*mult_active, CG*mult_active, CB*mult_active);
    
    CColor theme = ControlP5.getColor();
    theme.setForeground(col_fg);
    theme.setBackground(col_bg);
    theme.setActive(col_active);
    
    cp5 = new ControlP5(this);
    cp5.setAutoDraw(false);

    int sx, sy, px, py, oy;
    sx = 100; sy = 14; oy = (int)(sy*1.4f);
    
    int col_group = color(8,220);
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - SKYLIGHT
    ////////////////////////////////////////////////////////////////////////////
    Group group_skylight = cp5.addGroup("skylight");
    {
      group_skylight.setHeight(20).setSize(gui_w, height)
      .setBackgroundColor(col_group).setColorBackground(col_group);
      group_skylight.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;

      cp5.addCheckBox("displaySamples").setGroup(group_skylight).setSize(sy,sy).setPosition(px, py+=(int)(oy*1.4f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("SKY.samples", 0).activate(DISPLAY_SAMPLES_SKY ? 0 : 5)
          .addItem("SUN.samples", 1).activate(DISPLAY_SAMPLES_SUN ? 1 : 5)
          .addItem("textures"   , 2).activate(DISPLAY_TEXTURES    ? 2 : 5)
      ;
  
      DwSkyLightShader.Param param_sky = skylight.sky.param;
      DwSkyLightShader.Param param_sun = skylight.sun.param;

      cp5.addSlider("sky.iterations").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(int)(oy*5.4f))
          .setRange(0, 200).setValue(param_sky.iterations).plugTo(param_sky, "iterations");
      cp5.addSlider("sky.quality").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(32, 2048).setValue(param_sky.shadowmap_size).plugTo(param_sky, "shadowmap_size");
      cp5.addSlider("sky.solar_azimuth").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(oy*1.5f))
          .setRange(0, 360).setValue(param_sky.solar_azimuth).plugTo(param_sky, "solar_azimuth");
      cp5.addSlider("sky.solar_zenith").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 90).setValue(param_sky.solar_zenith).plugTo(param_sky, "solar_zenith");
      cp5.addSlider("sky.sample_focus").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.001f, 1).setValue(param_sky.sample_focus).plugTo(param_sky, "sample_focus");
      cp5.addSlider("sky.intensity").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(oy*1.5f))
          .setRange(0, 7).setValue(param_sky.intensity).plugTo(param_sky, "intensity");
      new ColorPicker(cp5, "sky.colorpicker", gui_w-20, 40, 100, param_sky.rgb).setGroup(group_skylight).setPosition(px, py+=(oy*1.5f));

      py += 80;
      
      cp5.addSlider("sun.iterations").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(int)(oy*1.5f))
          .setRange(0, 200).setValue(param_sun.iterations).plugTo(param_sun, "iterations");
      cp5.addSlider("sun.quality").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(32, 2048).setValue(param_sun.shadowmap_size).plugTo(param_sun, "shadowmap_size");
      cp5.addSlider("sun.solar_azimuth").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(oy*1.5f))
          .setRange(0, 360).setValue(param_sun.solar_azimuth).plugTo(param_sun, "solar_azimuth");
      cp5.addSlider("sun.solar_zenith").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 90).setValue(param_sun.solar_zenith).plugTo(param_sun, "solar_zenith");
      cp5.addSlider("sun.sample_focus").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.001f, 1).setValue(param_sun.sample_focus).plugTo(param_sun, "sample_focus");
      cp5.addSlider("sun.intensity").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(oy*1.5f))
          .setRange(0, 7).setValue(param_sun.intensity).plugTo(param_sun, "intensity");
      new ColorPicker(cp5, "sun.colorpicker", gui_w-20, 40, 100, param_sun.rgb).setGroup(group_skylight).setPosition(px, py+=(oy*1.5f));
    }
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_skylight)
      .open(0, 1);
   
  }
  


  
  /**
   * Creating a new cp5-controller for PixelFlows colorpicker.
   */
  static class ColorPicker extends Controller<ColorPicker> {
    ControlP5 cp5;
    DwColorPicker colorpicker;
    Pointer mouse = getPointer();
    float[] rgb;

    ColorPicker(ControlP5 cp5, String theName, int dim_x, int dim_y, int ny, float[] rgb) {
      super(cp5, theName);

      setSize(dim_x, dim_y);
      this.cp5 = cp5;
      this.rgb = rgb;
      this.colorpicker = new DwColorPicker(cp5.papplet, 0, 0, dim_x, dim_y);
      this.colorpicker.setAutoDraw(false);
      this.colorpicker.setAutoMouse(false);
      createPallette(ny);
      
      setView(new ControllerView<ColorPicker>() {
        public void display(PGraphics p, ColorPicker b) {
          colorpicker.display();
        }
      });
    }
    
    public ColorPicker createPallette(int shadesY){
      colorpicker.createPallette(shadesY);
      colorpicker.selectColorByRGB((int)(rgb[0]*255f), (int)(rgb[1]*255f), (int)(rgb[2]*255f));
      return this;
    }
    
    public ColorPicker createPallette(int shadesX, int shadesY){
      colorpicker.createPallette(shadesX, shadesY);
      colorpicker.selectColorByRGB((int)(rgb[0]*255f), (int)(rgb[1]*255f), (int)(rgb[2]*255f));
      return this;
    }

    public void selectColor(){
      colorpicker.selectColorByCoords(mouse.x(), mouse.y());
      int[] selected = colorpicker.getSelectedRGBColor();
      rgb[0] = selected[0] / 255f;
      rgb[1] = selected[1] / 255f;
      rgb[2] = selected[2] / 255f;
    }

    protected void onPress() {
      selectColor();
    }
    protected void onDrag() {
      selectColor();
    }
  }
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_BasicGUI.class.getName() });
  }
}
