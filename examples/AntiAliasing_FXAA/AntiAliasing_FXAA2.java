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





package AntiAliasing_FXAA;

import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.FXAA.FXAA;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureUtils;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.sampling.DwSampling;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;
import com.thomasdiewald.pixelflow.java.utils.DwVertexRecorder;

import peasy.*;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics3D;


public class AntiAliasing_FXAA2 extends PApplet {
  
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
  
  PGraphics3D pg_render;
  PGraphics3D pg_fxaa;
  
//  PGraphics3D pg_rgbl;
  
  FXAA fxaa;

  public void settings() {
    size(viewport_w, viewport_h, P3D);
    smooth(0);
  }
  
  public void setup() {
    surface.setLocation(viewport_x, viewport_y);

    // camera
    peasycam = new PeasyCam(this, -4.083,  -6.096,   7.000, 500);
    peasycam.setRotations(  1.085,  -0.477,   2.910);

    // projection
    perspective(60 * DEG_TO_RAD, width/(float)height, 2, 5000);

    // load obj file into shape-object
    shape = loadShape("examples/data/skylight_demo_scene.obj");
    shape.scale(10);
    
    pg_render = (PGraphics3D) createGraphics(width, height, P3D);
    pg_render.smooth(0);
    pg_render.textureSampling(5);
    pg_render.beginDraw();
//    pg_render.hint(PConstants.DISABLE_TEXTURE_MIPMAPS);
//    pg_render.blendMode(PConstants.BLEND);
    pg_render.endDraw(); 
    
    
//    pg_rgbl = (PGraphics3D) createGraphics(width, height, P3D);
//    pg_rgbl.smooth(0);
//    pg_rgbl.textureSampling(5);
//    pg_rgbl.beginDraw();
//    pg_rgbl.blendMode(PConstants.REPLACE);
//    pg_rgbl.endDraw(); 
    
    pg_fxaa   = (PGraphics3D) createGraphics(width, height, P3D);
    pg_fxaa.smooth(0);
    pg_fxaa.textureSampling(5);
//    pg_fxaa.hint(PConstants.DISABLE_TEXTURE_MIPMAPS);
    pg_fxaa.beginDraw();
    pg_fxaa.blendMode(PConstants.REPLACE);
    pg_fxaa.endDraw();
    
    DwPixelFlow context = new DwPixelFlow(this);
    fxaa = new FXAA(context);
    
    frameRate(1000);
  }

  

  public void draw() {
    



    DwGLTextureUtils.copyMatrices((PGraphics3D)this.g, pg_render);

    pg_render.beginDraw();
    pg_render.background(32);
    pg_render.blendMode(PConstants.BLEND);
    pg_render.lights();
    displayScene(pg_render);
    pg_render.endDraw();
    
    pg_render.beginDraw();
    pg_render.blendMode(PConstants.REPLACE);
    pg_render.endDraw();
    
//    pg_rgbl.beginDraw();
//    pg_rgbl.blendMode(PConstants.REPLACE);
//    pg_rgbl.clear();
//    pg_rgbl.endDraw();
    DwFilter.get(fxaa.context).rgbl.apply(pg_render, pg_render);

    
    
//    pg_fxaa.beginDraw();
//    pg_fxaa.blendMode(PConstants.REPLACE);
//    pg_fxaa.clear();
//    pg_fxaa.endDraw();
    fxaa.apply(pg_render, pg_fxaa);
  
    peasycam.beginHUD();
    

    
    if(keyPressed && key == ' ') {
      image(pg_render, 0, 0);
    } else {
      image(pg_fxaa, 0, 0);
    }
//    image(pg_rgbl, 0, 0);
    peasycam.endHUD();

    // some info, window title
    String txt_fps = String.format(getClass().getName()+ "   [fps %6.2f]", frameRate);
    surface.setTitle(txt_fps);
  }


  public void displayScene(PGraphics canvas){
    canvas.shape(shape);
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
    PApplet.main(new String[] { AntiAliasing_FXAA2.class.getName() });
  }
}
