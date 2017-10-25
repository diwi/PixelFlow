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

import java.util.Locale;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DepthOfField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwScreenSpaceGeometryBuffer;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLightRenderer;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLightShader;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;
import com.thomasdiewald.pixelflow.java.utils.DwColorPicker;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;
import com.thomasdiewald.pixelflow.java.utils.DwVertexRecorder;

import controlP5.Accordion;
import controlP5.CColor;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
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
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;


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
  DwPixelFlow context;
  
  // renderer
  DwSkyLight skylight;
  
  PMatrix3D mat_scene_view;
  PMatrix3D mat_scene_bounds;
  
  boolean DISPLAY_SAMPLES_SUN = false;
  boolean DISPLAY_SAMPLES_SKY = false;
  boolean DISPLAY_TEXTURES    = false;
  boolean APPLY_DOF           = false;
  
  DepthOfField dof;
  DwScreenSpaceGeometryBuffer geombuffer;
  PGraphics3D pg_tmp;
  
  SMAA smaa;
  
  PGraphics3D pg_aa;
  PGraphics3D pg_render;
  
  
  // scene to render
  PShape shp_group;
  
  int BACKGROUND = 32;
  float SCENE_SCALE = 500;

  
  public void settings() {
    size(viewport_w, viewport_h, P3D);
    smooth(0);
  }
  
  public float clip_z_far = SCENE_SCALE * 5;
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    surface.setResizable(true);
    
 
    // library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    // context.printGL_Extensions();
    context.printGL_MemoryInfo();
    
    // camera
    peasycam = new PeasyCam(this, SCENE_SCALE * 1.5f);
//    peasycam.setRotations(  0.422,   1.075,  -2.085);
//    peasycam.setDistance(475.257);
//    peasycam.lookAt(-33.842, -38.242,  64.793);

    
    
    // create scene
    {
      // load obj file into shape-object
      PShape shp_obj = loadShape("examples/data/skylight_demo_scene.obj");
      shp_obj.setFill(color(255));
  //    shp_obj.setFill(false);
  //    shp_obj.setStroke(true);
  //    shp_obj.setStroke(color(16));
  //    shp_obj.setStrokeWeight(1.1f);
  
      
      // create another shape
  //    sphereDetail(10);
      PShape shp_box = createShape(BOX, 5, 5, 80);
      shp_box.setFill(true);
      shp_box.setFill(color(64));
      shp_box.setStroke(false);
      shp_box.rotateX(0.2f);
      shp_box.rotateZ(0.5f);
      shp_box.translate(+20, +25, 5);
          
      PShape shp_sphere = createShape(SPHERE, 7);
      shp_sphere.setFill(true);
      shp_sphere.setFill(color(64));
      shp_sphere.setStroke(false);
      shp_sphere.translate(+10, -30, 7);
      
  //    PShape shp_ground = createShape(BOX, 100,100, 2);
  //    shp_ground.setFill(true);
  //    shp_ground.setFill(color(255));
  //    shp_ground.translate(0, 0, -1); 
      
      shp_group = createShape(GROUP);
      shp_group.addChild(shp_obj);
      shp_group.addChild(shp_box);
      shp_group.addChild(shp_sphere);
  //    shp_group.addChild(shp_ground);
    }
    
    // record list of vertices of the given shape
    DwVertexRecorder vertex_recorder = new DwVertexRecorder(this, shp_group);
   
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
    

    // Depth of Field
    dof = new DepthOfField(context);
    geombuffer = new DwScreenSpaceGeometryBuffer(context, scene_display);
    
    // init skylight renderer
    skylight = new DwSkyLight(context, scene_display, mat_scene_bounds);
    
    // parameters for sky-light
    skylight.sky.param.iterations     = 50;
    skylight.sky.param.solar_azimuth  = 0;
    skylight.sky.param.solar_zenith   = 0;
    skylight.sky.param.sample_focus   = 1; // full sphere sampling
    skylight.sky.param.intensity      = 3.0f;
    skylight.sky.param.rgb            = new float[]{0.10f,0.40f,1.00f};
    skylight.sky.param.shadowmap_size = 512; // quality vs. performance
    
    // parameters for sun-light
    skylight.sun.param.iterations     = 50;
    skylight.sun.param.solar_azimuth  = 45;
    skylight.sun.param.solar_zenith   = 75;
    skylight.sun.param.sample_focus   = 0.1f;
    skylight.sun.param.intensity      = 1.2f;
    skylight.sun.param.rgb            = new float[]{1.00f,0.50f,0.00f};
    skylight.sun.param.shadowmap_size = 512;
    
    // postprocessing AA
    smaa = new SMAA(context);
    
    // ControlP5
    createGUI();

    frameRate(1000);
  }


  public boolean resizeScene(){
    
    viewport_w = width;
    viewport_h = height;

    boolean[] RESIZED = {false};
    
    pg_aa     = DwUtils.changeTextureSize(this, pg_aa    , width, height, 0, RESIZED);
    pg_render = DwUtils.changeTextureSize(this, pg_render, width, height, 0, RESIZED);
    pg_tmp    = DwUtils.changeTextureSize(this, pg_tmp   , width, height, 0, RESIZED, GL2.GL_RGBA16F, GL2.GL_RGBA, GL2.GL_FLOAT);

    skylight.resize(width, height);
    
    if(RESIZED[0]){
      // nothing here
    }
    peasycam.feed();
    perspective(60 * DEG_TO_RAD, width/(float)height, 2, clip_z_far);
    
    return RESIZED[0];
  }
    

  public void draw() {
    
    resizeScene();
 
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
    
    
    // apply DoF
    if(APPLY_DOF){
      int mult_blur = 15;
      
      geombuffer.update(skylight.renderer.pg_render);
      DwFilter filter = DwFilter.get(context);
      filter.gaussblur.apply(geombuffer.pg_geom, geombuffer.pg_geom, pg_tmp, 3);

      dof.param.focus_pos = new float[]{0.5f, 0.5f};
//      dof.param.focus_pos[0] = map(mouseX, 0, width , 0, 1);
//      dof.param.focus_pos[1] = map(mouseY, 0, height, 1, 0);
      dof.param.mult_blur = mult_blur;
      dof.param.clip_z_far = clip_z_far;
      dof.apply(pg_aa, pg_render, geombuffer);
      filter.copy.apply(pg_render, pg_aa);
    }


    DwUtils.beginScreen2D(g);
//    peasycam.beginHUD();
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
    
    displayCross();
    
    displayGUI();
   
//    peasycam.endHUD();
    DwUtils.endScreen2D(g);
    
    
    // some info, window title
    int sun_pass = skylight.sun.RENDER_PASS;
    int sky_pass = skylight.sky.RENDER_PASS;
    String txt_fps = String.format(getClass().getName()+ "  [sun: %d]  [sky: %d]  [fps %6.2f]", sun_pass, sky_pass, frameRate);
    surface.setTitle(txt_fps);
  }

  public void displayCross(){
    pushMatrix();
    float cursor_s = 10;
    float fpx = (       dof.param.focus_pos[0]) * width;
    float fpy = (1.0f - dof.param.focus_pos[1]) * height;
    blendMode(EXCLUSION);
    translate(fpx, fpy);
    strokeWeight(1);
    stroke(255,200);
    line(-cursor_s, 0, +cursor_s, 0);
    line(0, -cursor_s, 0, +cursor_s);
    blendMode(BLEND);
    popMatrix();
  }
  
  

  public void displayScene(PGraphicsOpenGL canvas){
    if(canvas == skylight.renderer.pg_render){
      canvas.background(BACKGROUND);
      displaySamples(canvas);
    }
    
    if(canvas == geombuffer.pg_geom){
      canvas.pgl.clearDepth(1.0f);
      canvas.pgl.clearColor(1, 1, 1, clip_z_far);
      canvas.pgl.clear(PGL.COLOR_BUFFER_BIT | PGL.DEPTH_BUFFER_BIT);
    }
    
    canvas.pushMatrix();
    canvas.applyMatrix(mat_scene_view);
    canvas.shape(shp_group);
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
  
  
  
  
  
  
  public void keyReleased(){
    if(key == 'c') printCam();
    if(key == 'm'){
      context.printGL_MemoryInfo();
    }
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
 

  ControlP5 cp5;
  
  public void displayGUI(){
    gui_x = width - gui_w;
    cp5.setPosition(gui_x, gui_y);
    cp5.draw();
  }
  

  float mult_bg     = 0.25f;
  float mult_fg     = 1f;
  float mult_active = 2f;
  float CR = 0;
  float CG = 64;
  float CB = 128;
  int col_bg, col_fg, col_active;
  
  public void createGUI(){
    
    col_bg     = color(CR*mult_bg, CG*mult_bg, CB*mult_bg, 220);
    col_fg     = color(CR*mult_fg, CG*mult_fg, CB*mult_fg, 255);
    col_active = color(CR*mult_active, CG*mult_active, CB*mult_active);
    
    CColor theme = ControlP5.getColor();
    theme.setForeground(col_fg);
    theme.setBackground(col_bg);
    theme.setActive(col_active);
    
    cp5 = new ControlP5(this);
    cp5.setAutoDraw(false);
    cp5.setPosition(gui_x, gui_y);
    
    int sx, sy, px, py, oy;
    sx = 100; sy = 14; oy = (int)(sy*1.4f);
    
    int col_group = color(8,220);
    
    final DwSkyLightRenderer.Param param_renderer = skylight.renderer.param;
    final DwSkyLightShader  .Param param_sky      = skylight.sky.param;
    final DwSkyLightShader  .Param param_sun      = skylight.sun.param;
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - SKYLIGHT
    ////////////////////////////////////////////////////////////////////////////
    Group group_skylight = cp5.addGroup("skylight");
    {
      group_skylight.setHeight(20).setSize(gui_w, height);
      group_skylight.setBackgroundColor(col_group).setColorBackground(col_group);
      group_skylight.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;

      cp5.addCheckBox("displaySamples").setGroup(group_skylight).setSize(sy,sy).setPosition(px, py+=(int)(oy*1.4f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("SKY.samples", 0).activate(DISPLAY_SAMPLES_SKY ? 0 : 5)
          .addItem("SUN.samples", 1).activate(DISPLAY_SAMPLES_SUN ? 1 : 5)
          .addItem("textures"   , 2).activate(DISPLAY_TEXTURES    ? 2 : 5)         
          .addItem("DoF"        , 3).activate(APPLY_DOF           ? 3 : 5)
      ;
      
      cp5.addSlider("gamma").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(int)(oy*5.4f))
          .setRange(1, 2.2f).setValue(param_renderer.gamma);

      cp5.addSlider("BACKGROUND").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 255).setValue(BACKGROUND).setDecimalPrecision(0);
      
      int sky_quality = DwUtils.log2ceil(param_sky.shadowmap_size);
      int sun_quality = DwUtils.log2ceil(param_sun.shadowmap_size);

      cp5.addSlider("sky.iterations").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(int)(oy*2.5f))
          .setRange(0, 150).setValue(param_sky.iterations).setDecimalPrecision(0);
      cp5.addSlider("sky.quality").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(6, 12).setValue(sky_quality).setDecimalPrecision(0).setNumberOfTickMarks(7).snapToTickMarks(true);
      cp5.addSlider("sky.solar_azimuth").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(oy*1.5f))
          .setRange(0, 360).setValue(param_sky.solar_azimuth);
      cp5.addSlider("sky.solar_zenith").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 90).setValue(param_sky.solar_zenith);
      cp5.addSlider("sky.sample_focus").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.001f, 1).setValue(param_sky.sample_focus);
      cp5.addSlider("sky.intensity").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(oy*1.5f))
          .setRange(0, 10).setValue(param_sky.intensity);
      new ColorPicker(cp5, "sky.colorpicker", gui_w-20, 40, 100, param_sky.rgb).setGroup(group_skylight).setPosition(px, py+=(oy*1.5f));

      py += 80;
      
      cp5.addSlider("sun.iterations").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(int)(oy*1.5f))
          .setRange(0, 150).setValue(param_sun.iterations).setDecimalPrecision(0);
      cp5.addSlider("sun.quality").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(6, 12).setValue(sun_quality).setDecimalPrecision(0).setNumberOfTickMarks(7).snapToTickMarks(true);
      cp5.addSlider("sun.solar_azimuth").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(oy*1.5f))
          .setRange(0, 360).setValue(param_sun.solar_azimuth);
      cp5.addSlider("sun.solar_zenith").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 90).setValue(param_sun.solar_zenith);
      cp5.addSlider("sun.sample_focus").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0.02f, 0.50f).setValue(param_sun.sample_focus);
      cp5.addSlider("sun.intensity").setGroup(group_skylight).setSize(sx, sy).setPosition(px, py+=(oy*1.5f))
          .setRange(0, 10).setValue(param_sun.intensity);
      new ColorPicker(cp5, "sun.colorpicker", gui_w-20, 40, 100, param_sun.rgb).setGroup(group_skylight).setPosition(px, py+=(oy*1.5f));
    }

    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setCollapseMode(Accordion.MULTI)
      .addItem(group_skylight)
      .open();
    
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI EVENT LISTENER
    ////////////////////////////////////////////////////////////////////////////
    cp5.addCallback(new CallbackListener() {
      
      int           ACTION;
      Controller<?> CTRL;
      String        CTRL_NAME;
      float         CTRL_VAL;
      boolean       update;
      
      public boolean isControl(String name){
        return name.equals(CTRL_NAME);
      }
      
      @Override
      public void controlEvent(CallbackEvent event) {
        
        ACTION    = event.getAction();
        CTRL      = event.getController();
        CTRL_NAME = CTRL.getName();
        CTRL_VAL  = CTRL.getValue();
        
        update = false;
//        update |= ACTION == ControlP5.ACTION_PRESS    ;
        update |= ACTION == ControlP5.ACTION_RELEASE;
        update |= ACTION == ControlP5.ACTION_RELEASE_OUTSIDE;
        update |= ACTION == ControlP5.ACTION_CLICK;
        update |= ACTION == ControlP5.ACTION_BROADCAST;

        if(update){

          if(isControl("gamma"            )) param_renderer.gamma     = CTRL_VAL;
          if(isControl("BACKGROUND"       )) BACKGROUND               = (int) CTRL_VAL;
          if(isControl("SKY.samples"      )) DISPLAY_SAMPLES_SKY      = CTRL_VAL > 0;
          if(isControl("SUN.samples"      )) DISPLAY_SAMPLES_SUN      = CTRL_VAL > 0;
          if(isControl("textures"         )) DISPLAY_TEXTURES         = CTRL_VAL > 0;
          if(isControl("DoF"              )) APPLY_DOF                = CTRL_VAL > 0;
       
          if(isControl("sky.iterations"   )) param_sky.iterations     = (int) CTRL_VAL;
          if(isControl("sky.quality"      )) param_sky.shadowmap_size = 1 << (int) CTRL_VAL;
          if(isControl("sky.solar_azimuth")) param_sky.solar_azimuth  =       CTRL_VAL;
          if(isControl("sky.solar_zenith" )) param_sky.solar_zenith   =       CTRL_VAL;
          if(isControl("sky.sample_focus" )) param_sky.sample_focus   =       CTRL_VAL;
          if(isControl("sky.intensity"    )) param_sky.intensity      =       CTRL_VAL;
         
          if(isControl("sun.iterations"   )) param_sun.iterations     = (int) CTRL_VAL;
          if(isControl("sun.quality"      )) param_sun.shadowmap_size = 1 << (int) CTRL_VAL;
          if(isControl("sun.solar_azimuth")) param_sun.solar_azimuth  =       CTRL_VAL;
          if(isControl("sun.solar_zenith" )) param_sun.solar_zenith   =       CTRL_VAL;
          if(isControl("sun.sample_focus" )) param_sun.sample_focus   =       CTRL_VAL;
          if(isControl("sun.intensity"    )) param_sun.intensity      =       CTRL_VAL;
          
          boolean reset = false;
          reset |= CTRL_NAME.contains("solar_azimuth");
          reset |= CTRL_NAME.contains("solar_zenith");
          reset |= CTRL_NAME.contains("sample_focus");
          reset |= CTRL_NAME.contains("quality");
          if(reset){
            skylight.reset();
          }
        }
      }
      
    });
    
    
  }
  


  
  /**
   * Creating a new cp5-controller for PixelFlows colorpicker.
   */
  static class ColorPicker extends Controller<ColorPicker> {
    ControlP5 cp5;
    DwColorPicker colorpicker;
    Pointer mouse = getPointer();
    float[] rgb;   
    int hud_sy = 16;

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
        public void display(PGraphics pg, ColorPicker cp) {
          colorpicker.display();
          
          int dim_x = getWidth();
 
          int    cp_col = colorpicker.getSelectedColor();
          String cp_rgb = colorpicker.getSelectedRGBasString();
          // String cp_hsb = colorpicker.getSelectedHSBasString();

          int sy = hud_sy;
          int px = 0;
          int py = colorpicker.h()+1;
          
          pg.noStroke();
          pg.fill(200, 50);
          pg.rect(px-1, py, dim_x+2, sy+1);
          pg.fill(cp_col);
          pg.rect(px, py, sy, sy);
          
          pg.fill(255);
          pg.text(cp_rgb, px + sy + 5, py+8);
//          pg.text(cp_hsb, px + sy * 2, py+8);

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
