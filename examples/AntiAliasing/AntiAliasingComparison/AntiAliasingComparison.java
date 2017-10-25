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


package AntiAliasing.AntiAliasingComparison;

import java.util.Locale;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.FXAA.FXAA;
import com.thomasdiewald.pixelflow.java.antialiasing.GBAA.GBAA;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.geometry.DwCube;
import com.thomasdiewald.pixelflow.java.geometry.DwMeshUtils;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.utils.DwMagnifier;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import peasy.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PShape;
import processing.opengl.PGraphics3D;


public class AntiAliasingComparison extends PApplet {
  
  //
  // This example compares several Anti-Aliasing Algorithms.
  // 
  // 1) NOAA
  //    no AntiAliasing, default rendering mode
  //
  //
  // 2) MSAA - MultiSample AntiAliasing (8x)
  //    build in OpenGL AA, best quality, performance intensive
  //    https://www.khronos.org/opengl/wiki/Multisampling
  //
  //
  // 3) SMAA - Enhances SubPixel Morphological AntiAliasing
  //    PostProcessing, 3 passes, nice quality
  //    SMAA.h, Version 2.8: 
  //    http://www.iryoku.com/smaa/
  //    
  //
  // 4) FXAA - Fast Approximate AntiAliasing
  //    PostProcessing, 1 pass, good quality - a bit blurry sometimes
  //    created "by Timothy Lottes under NVIDIA", FXAA_WhitePaper.pdf
  //    FXAA3_11.h, Version 3.11: 
  //    https://docs.nvidia.com/gameworks/content/gameworkslibrary/graphicssamples/opengl_samples/fxaa.htm
  //
  //
  // 5) GBAA - GeometryBuffer AntiAliasing
  //    Shader Pipeline - Distance-to-Edge (GeometryShader) used for blending
  //    created by Emil Persson, 2011, http://www.humus.name
  //    article: http://www.humus.name/index.php?page=3D&ID=87
  //
  //
  //
  // controls:
  //
  // key '1': NOAA
  // key '2': MSAA(8)
  // key '3': SMAA
  // key '4': FXAA
  // key '5': GBAA
  //
  // ALT + LMB: Camera ROTATE
  // ALT + MMB: Camera PAN
  // ALT + RMB: Camera ZOOM
  //


  

  boolean START_FULLSCREEN = !true;

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;

  // camera control
  PeasyCam peasycam;
  
  // library context
  DwPixelFlow context;
  
  // AntiAliasing
  FXAA fxaa;
  SMAA smaa;
  GBAA gbaa;
  
  // AntiAliasing render targets
  PGraphics3D pg_render_noaa;
  PGraphics3D pg_render_msaa;
  PGraphics3D pg_render_smaa;
  PGraphics3D pg_render_fxaa;
  PGraphics3D pg_render_gbaa;
  
  // toggling active AntiAliasing-Mode
  AA_MODE aa_mode = AA_MODE.NoAA;
  SMAA_MODE smaa_mode = SMAA_MODE.FINAL;
  
  // AA mode, selected with keys '1' - '4'
  enum AA_MODE{  NoAA, MSAA, SMAA, FXAA, GBAA }
  
  // SMAA mode, selected with keys 'q', 'w', 'e'
  enum SMAA_MODE{ EGDES, BLEND, FINAL }
  
  // render stuff
  PFont font48, font12;
  float gamma = 2.2f;
  float BACKGROUND_COLOR = 32;
  
  DwMagnifier magnifier;

  
  public void settings() {
    if(START_FULLSCREEN){
      viewport_w = displayWidth;
      viewport_h = displayHeight;
      viewport_x = 0;
      viewport_y = 0;
      fullScreen(P3D);
    } else {
      size(viewport_w, viewport_h, P3D);
    }
    smooth(0);
  }
  
  
  public void setup() {
    surface.setResizable(true);
    surface.setLocation(viewport_x, viewport_y);

    // camera
    peasycam = new PeasyCam(this, -4.083,  -6.096,   7.000, 1300);
    peasycam.setRotations(1.085,  -0.477,   2.910);

    // processing font
    font48 = createFont("../data/SourceCodePro-Regular.ttf", 48);
    font12 = createFont("../data/SourceCodePro-Regular.ttf", 12);
    
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    // callback for scene display (used in GBAA)
    DwSceneDisplay scene_display = new DwSceneDisplay() {  
      @Override
      public void display(PGraphics3D canvas) {
        displayScene(canvas); 
      }
    };
    
    // AA post-processing modes
    fxaa = new FXAA(context);
    smaa = new SMAA(context);
    gbaa = new GBAA(context, scene_display);

    // magnifier
    int mag_wh = (int) (height/2.5f);
    magnifier = new DwMagnifier(this, 4, 0, height-mag_wh, mag_wh, mag_wh);
    
    frameRate(1000);
  }

  
  // dynamically resize render-targets
  public boolean resizeScreen(){

    boolean[] RESIZED = {false};
    
    pg_render_noaa = DwUtils.changeTextureSize(this, pg_render_noaa, width, height, 0, RESIZED);
    pg_render_msaa = DwUtils.changeTextureSize(this, pg_render_msaa, width, height, 8, RESIZED);
    pg_render_fxaa = DwUtils.changeTextureSize(this, pg_render_fxaa, width, height, 0, RESIZED);
    pg_render_smaa = DwUtils.changeTextureSize(this, pg_render_smaa, width, height, 0, RESIZED);
    pg_render_gbaa = DwUtils.changeTextureSize(this, pg_render_gbaa, width, height, 0, RESIZED);
    
    if(RESIZED[0]){
      // nothing here
    }
    peasycam.feed();
    perspective(60 * DEG_TO_RAD, width/(float)height, 2, 6000);

    return RESIZED[0];
  }

  
  
  public void draw() {
    
    resizeScreen();
     
    if(aa_mode == AA_MODE.MSAA){
      displaySceneWrap(pg_render_msaa);
      // RGB gamma correction
      DwFilter.get(context).gamma.apply(pg_render_msaa, pg_render_msaa, gamma);
    }
    
    if(aa_mode == AA_MODE.NoAA || aa_mode == AA_MODE.SMAA || aa_mode == AA_MODE.FXAA){
      displaySceneWrap(pg_render_noaa);
      // RGB gamma correction
      DwFilter.get(context).gamma.apply(pg_render_noaa, pg_render_noaa, gamma);
    }
    
    if(aa_mode == AA_MODE.SMAA) smaa.apply(pg_render_noaa, pg_render_smaa);
    if(aa_mode == AA_MODE.FXAA) fxaa.apply(pg_render_noaa, pg_render_fxaa);
    
    // only for debugging
    if(aa_mode == AA_MODE.SMAA){
      if(smaa_mode == SMAA_MODE.EGDES) DwFilter.get(context).copy.apply(smaa.tex_edges, pg_render_smaa);
      if(smaa_mode == SMAA_MODE.BLEND) DwFilter.get(context).copy.apply(smaa.tex_blend, pg_render_smaa);
    }
    
    if(aa_mode == AA_MODE.GBAA){
      displaySceneWrap(pg_render_noaa);
      // RGB gamma correction
      DwFilter.get(context).gamma.apply(pg_render_noaa, pg_render_noaa, gamma);
      gbaa.apply(pg_render_noaa, pg_render_gbaa);
    }
    
    
    PGraphics3D display = pg_render_noaa;
    
    switch(aa_mode){
      case NoAA: display = pg_render_noaa; break;
      case MSAA: display = pg_render_msaa; break;
      case SMAA: display = pg_render_smaa; break;
      case FXAA: display = pg_render_fxaa; break;
      case GBAA: display = pg_render_gbaa; break;
    }
    
    magnifier.apply(display, mouseX, mouseY);
    magnifier.displayTool();

    DwUtils.beginScreen2D(g);
//    peasycam.beginHUD();
    {
      // display Anti Aliased result
      blendMode(REPLACE);
      clear();
      image(display, 0, 0);
      blendMode(BLEND);
      
      // display magnifer
      magnifier.display(g, 0, height-magnifier.h);
      
      // display AA name
      String mode = aa_mode.name();
      String buffer = "";
      if(aa_mode == AA_MODE.SMAA){
        if(smaa_mode == SMAA_MODE.EGDES ) buffer = " ["+smaa_mode.name()+"]";
        if(smaa_mode == SMAA_MODE.BLEND ) buffer = " ["+smaa_mode.name()+"]";
      }
      
      noStroke();
      fill(0,150);
      rect(0, height-65, magnifier.w, 65);
      
      int tx, ty;
      
      tx = 10;
      ty = 20;
      textFont(font12);
      fill(200);
      text("[1] NoAA", tx, ty);
      text("[2] MSAA - MultiSample AA"           , tx, ty+=20);
      text("[3] SMAA - SubPixel Morphological AA", tx, ty+=20);
      text("[4] FXAA - Fast Approximate AA"      , tx, ty+=20);
      text("[5] GBAA - GeometryBuffer AA"        , tx, ty+=20);
      
      textFont(font48);
      tx = 20;
      ty = height-20;
      fill(0);
      text(mode + buffer, tx+2, ty+2);
      
      fill(255,200,0);
      text(mode + buffer, tx, ty);
      
    }
//    peasycam.endHUD();
    DwUtils.endScreen2D(g);
    
    // some info, window title
    String txt_fps = String.format(getClass().getName()+ "   [fps %6.2f]", frameRate);
    surface.setTitle(txt_fps);
  }


  
  public void displaySceneWrap(PGraphics3D canvas){
    canvas.beginDraw();
    DwUtils.copyMatrices((PGraphics3D) this.g, canvas);
    float BACKGROUND_COLOR_GAMMA = (float) (Math.pow(BACKGROUND_COLOR/255.0, gamma) * 255.0);

    // background
    canvas.blendMode(PConstants.BLEND);
    canvas.background(BACKGROUND_COLOR_GAMMA);
    displayScene(canvas);
    canvas.endDraw();
  }
  
  

  // render something
  public void displayScene(PGraphics3D canvas){
    // lights
    canvas.directionalLight(255, 255, 255, 200,600,400);
    canvas.directionalLight(255, 255, 255, -200,-600,-400);
    canvas.ambientLight(64, 64, 64);
    
//    canvas.shape(shape);
    sceneShape(canvas);
  }
  
  
  PShape shp_scene;
  
  public void sceneShape(PGraphics3D canvas){
    if(shp_scene != null){
      canvas.shape(shp_scene);
      return;
    }
    
    shp_scene = createShape(GROUP);
    
    int num_boxes = 50;
    int num_spheres = 50;
    float bb_size = 800;
    float xmin = -bb_size;
    float xmax = +bb_size;
    float ymin = -bb_size;
    float ymax = +bb_size;
    float zmin =  0;
    float zmax = +bb_size;

    colorMode(HSB, 360, 1, 1);
    randomSeed(0);

    for(int i = 0; i < num_boxes; i++){
      float px = random(xmin, xmax);
      float py = random(ymin, ymax);
      float sx = random(200) + 10;
      float sy = random(200) + 10;
      float sz = random(zmin, zmax);

      
      float off = 45;
      float base = 0;
      float hsb_h = base + random(-off,off);
      float hsb_s = 1;
      float hsb_b = random(0.1f,1.0f);
      int shading = color(hsb_h, hsb_s, hsb_b);
      
      PShape shp_box = createShape(BOX, sx, sy, sz);
      shp_box.setFill(true);
      shp_box.setStroke(false);
      shp_box.setFill(shading);
      shp_box.translate(px, py, sz/2);
      shp_scene.addChild(shp_box);
    }
    

    DwCube cube_smooth = new DwCube(4);
    DwCube cube_facets = new DwCube(2);

    for(int i = 0; i < num_spheres; i++){
      float px = random(xmin, xmax);
      float py = random(ymin, ymax);
      float pz = random(zmin, zmax);
      float rr = random(50) + 50;
      boolean facets = true;//(i%2 == 0);

      float off = 20;
      float base = 225;
      float hsb_h = base + random(-off,off);
      float hsb_s = random(0.1f,1.0f);
      float hsb_b = 1;
      int shading = color(hsb_h, hsb_s, hsb_b);
      
      PShape shp_sphere = createShape(PShape.GEOMETRY);
      if(facets){
        DwMeshUtils.createPolyhedronShape(shp_sphere, cube_facets, 1, 4, false);
      } else {
        DwMeshUtils.createPolyhedronShape(shp_sphere, cube_smooth, 1, 4, true);
      }

      shp_sphere.setStroke(false);
      shp_sphere.setStroke(color(0));
      shp_sphere.setStrokeWeight(0.01f / rr);
      shp_sphere.setFill(true);
      shp_sphere.setFill(shading);
      shp_sphere.resetMatrix();
      shp_sphere.scale(rr);
      shp_sphere.translate(px, py, pz);
      shp_scene.addChild(shp_sphere);
    }
    colorMode(RGB, 255, 255, 255);
    
    PShape shp_rect = createShape(RECT, -1000, -1000, 2000, 2000);
    shp_rect.setStroke(false);
    shp_rect.setFill(true);
    shp_rect.setFill(color(255));
    
    shp_scene.addChild(shp_rect);
  }
  
  
  public PShape createGridXY(int lines, float s){
    PShape shp_gridxy = createShape();
    shp_gridxy.beginShape(LINES);
    shp_gridxy.stroke(0);
    shp_gridxy.strokeWeight(1f);
    float d = lines*s;
    for(int i = 0; i <= lines; i++){
      shp_gridxy.vertex(-d,-i*s,0); shp_gridxy.vertex(d,-i*s,0);
      shp_gridxy.vertex(-d,+i*s,0); shp_gridxy.vertex(d,+i*s,0);
      
      shp_gridxy.vertex(-i*s,-d,0); shp_gridxy.vertex(-i*s,d,0);
      shp_gridxy.vertex(+i*s,-d,0); shp_gridxy.vertex(+i*s,d,0);
    }
    shp_gridxy.endShape();
    return shp_gridxy;
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
    if(key == 'c') printCam();
    
    if(key == '1') aa_mode = AA_MODE.NoAA;
    if(key == '2') aa_mode = AA_MODE.MSAA;
    if(key == '3') aa_mode = AA_MODE.SMAA;
    if(key == '4') aa_mode = AA_MODE.FXAA;
    if(key == '5') aa_mode = AA_MODE.GBAA;
    
    if(key == 'q') smaa_mode = SMAA_MODE.EGDES;
    if(key == 'w') smaa_mode = SMAA_MODE.BLEND;
    if(key == 'e') smaa_mode = SMAA_MODE.FINAL;
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { AntiAliasingComparison.class.getName() });
  }
}
