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


import java.util.Locale;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSceneDisplay;
import com.thomasdiewald.pixelflow.java.render.skylight.DwSkyLight;
import com.thomasdiewald.pixelflow.java.utils.DwBoundingSphere;

import peasy.CameraState;
import peasy.PeasyCam;
import peasy.org.apache.commons.math.geometry.Rotation;
import peasy.org.apache.commons.math.geometry.RotationOrder;
import peasy.org.apache.commons.math.geometry.Vector3D;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphics3D;
import processing.video.Movie;

public class Skylight_Movie extends PApplet {
  

  public static final float SQRT2 = (float) Math.sqrt(2);

  // viewport
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  // viewport camera
  PeasyCam cam;
  
  // pshapes
  PShape   group_cubes;
  PShape[] shp_cubes;
  
  // movie capture (video library)
  Movie movie;
  TimeLine timeline;
  
  // render buffers for animation soruce
  PGraphics2D pg_src;
  PGraphics2D pg_src_tmp;
  PGraphics2D pg_src_small;
  PGraphics2D pg_src_small_tmp;
  
  // canvas, for viewport renderer
  PGraphics3D pg_aa;


  // library context
  DwPixelFlow context;
  
  // skylight renderer
  DwSkyLight skylight;
  PMatrix3D mat_scene_bounds;
  float[] scene_bounds = {-500,-500,0, 500, 500, 500};
 
  // optical flow
  DwOpticalFlow opticalflow;

  // AntiAliasing
  SMAA smaa;
  
  // font
  PFont font;

  // states
  int BACKGROUND_COLOR = 255;
  boolean UPDATE_SCENE = true;
  boolean DISPLAY_SOURCE = true;
  boolean USE_PIXEL_COLORS = true;
  
  public void settings(){
    size(viewport_w, viewport_h, P3D);
    smooth(8);
  }
  
  
  public void setup(){
    surface.setLocation(viewport_x, viewport_y);
    background(BACKGROUND_COLOR);
    
    // viewport camera
    double   distance = 554;
    double[] look_at  = {-78.294, -57.279, 148.413};
    double[] rotation = { -1.413,  -0.412,   0.083};
    
    Rotation rot = new Rotation(RotationOrder.XYZ, rotation[0], rotation[1], rotation[2]);
    Vector3D center = new Vector3D(look_at[0], look_at[1], look_at[2]);
    CameraState state = new CameraState(rot, center, distance);
    
    cam = new PeasyCam(this, distance);
    cam.setState(state);
    
    // projection
    perspective(60 * PI/180f, width/(float)height, 2, 4000);
    
    
    font = createFont("../data/SourceCodePro-Regular.ttf", 12);
    
   
    // INPUT: capture
    movie = new Movie(this, "examples/data/Pulp_Fiction_Dance_Scene.mp4");
    movie.loop();
    
    
    
    timeline = new TimeLine(movie, width/4, height-30, 2*width/4, 20);


    // pixelflow library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    
    
    // scene bounds for skylight
    float sx = scene_bounds[3] - scene_bounds[0];
    float sy = scene_bounds[4] - scene_bounds[1];
    float sz = scene_bounds[5] - scene_bounds[2];
    float px = (scene_bounds[3] + scene_bounds[0]) * 0.5f;
    float py = (scene_bounds[4] + scene_bounds[1]) * 0.5f;
    float pz = (scene_bounds[5] + scene_bounds[2]) * 0.5f;
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
    
    // init skylight renderer
    skylight = new DwSkyLight(context, scene_display, mat_scene_bounds);
    // parameters for sky-light
    skylight.sky.param.iterations     = 50;
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
    opticalflow = new DwOpticalFlow(context, 0, 0);
    // parameters
    opticalflow.param.flow_scale         = 100;
    opticalflow.param.temporal_smoothing = 0.8f;
    opticalflow.param.display_mode       = 0;
    opticalflow.param.grayscale          = true;
    opticalflow.param.threshold = 2;
    
    
    // postprocessing AA
    smaa = new SMAA(context);
    // canvas, AA
    pg_aa = (PGraphics3D) createGraphics(width, height, P3D);
    pg_aa.smooth(0);
    pg_aa.textureSampling(5);


    frameRate(60);
  }


  

  public void draw(){
    updateCamActiveStatus();

    // scene update, optical flow
    if(UPDATE_SCENE && movie.available()){
      movie.read();
      resize();

      updateAnim();
      
      opticalflow.renderVelocityShading(pg_src);
    }
    
    // skylight
    if(UPDATE_SCENE || CAM_ACTIVE){
      skylight.reset();
    }
    skylight.update();

    // AntiAliasing
    smaa.apply(skylight.renderer.pg_render, pg_aa);

    // display
    cam.beginHUD();
    blendMode(REPLACE);
    image(pg_aa, 0, 0);
    blendMode(BLEND);
    
    if(DISPLAY_SOURCE && pg_src != null){
      float r = pg_src.width / (float) pg_src.height;
      int h = 120;
      int w = (int) (h * r + 0.5f);
      image(pg_src, 0, height-h, w, h);
    }
    
    timeline.draw(mouseX, mouseY);
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
      canvas.background(BACKGROUND_COLOR);
    }

    // draw shapes
    canvas.pushMatrix();
    canvas.translate(0, 0, cube_size + cube_size * cube_numy /2);
    canvas.rotateX(-PI/2);
    canvas.shape(group_cubes);
    canvas.popMatrix();
    
    // draw ground plane
    float sx = scene_bounds[3] - scene_bounds[0];
    float sy = scene_bounds[4] - scene_bounds[1];
    canvas.noStroke();
    canvas.fill(255);
    canvas.ellipse(0,0, sx * SQRT2, sy * SQRT2);
  }
  
  
  

  public void keyReleased(){
    if(key == 'r') resize();
    if(key == ' ') toggleUpdate();
    if(key == 'c') BACKGROUND_COLOR = BACKGROUND_COLOR==255 ? 8 : 255;
    if(key == 'v') printCam();
    if(key == 'b') DISPLAY_SOURCE = !DISPLAY_SOURCE;
    if(key == 'p') USE_PIXEL_COLORS = !USE_PIXEL_COLORS;
  }
  
  public void toggleUpdate(){
    UPDATE_SCENE = !UPDATE_SCENE;
    if(UPDATE_SCENE){
      movie.play();
    } else {
      movie.pause();
    }
  }
  
  
  public void mouseReleased(){
    if(timeline.inside(mouseX, mouseY)){
      timeline.jumpToMoviePos();
      opticalflow.reset();
    }
  }
  
  
  
  int num_voxels = 8000;
  
  public void resize(){
    
    if(!movie.available()){
      return;
    }
    
    int src_w = movie.width;
    int src_h = movie.height;
    
    if(src_w == 0 || src_h == 0){
      return;
    }
    
    // 1) x * y = count_limit
    // 2) x / y = ratio
    // x = ratio * y;
    // y * y * ratio = count_limit
    // y = sqrt(count_limit / ratio);
    
    float wh_ratio = src_w / (float) src_h;
    int num_y = (int) (Math.sqrt(num_voxels / wh_ratio) + 0.5f);
    int num_x = (int) (num_y * wh_ratio + 0.5f);
    
    
    if(pg_src != null){
      if(pg_src.width  == src_w && 
         pg_src.height == src_h) return;
    }
    
    movie.jump(80);
    

    
    opticalflow.resize(src_w, src_h);

    pg_src = (PGraphics2D) createGraphics(src_w, src_h, P2D);
    pg_src.smooth(0);
    
    pg_src_tmp = (PGraphics2D) createGraphics(src_w, src_h, P2D);
    pg_src_tmp.smooth(0);

    pg_src_small = (PGraphics2D) createGraphics(num_x, num_y, P2D);
    pg_src_small.smooth(0);
    
    pg_src_small_tmp = (PGraphics2D) createGraphics(num_x, num_y, P2D);
    pg_src_small_tmp.smooth(0);
    
    group_cubes = createShape(GROUP);
    shp_cubes = new PShape[num_x * num_y];
    
    for(int y = 0; y < num_y; y++){
      for(int x = 0; x < num_x; x++){
        int idx = y * num_x + x;
        PShape shp_cube = createShape(BOX, 1, 1, 1);
        shp_cube.setFill(true);
        shp_cube.setStroke(false);
        shp_cube.setStroke(color(0));
        shp_cubes[idx] = shp_cube;
        group_cubes.addChild(shp_cube);
      }
    }
  }
  


  

  
  int   cube_numx = 0;
  int   cube_numy = 0;
  float cube_size = 0;
  
  
  DwGLTexture tex_of_velocity = new DwGLTexture();
  float[]     buf_of_velocity;

  public void updateAnim(){
    
    pg_src.beginDraw();
    pg_src.blendMode(BLEND);
    pg_src.image(movie, 0, 0);
    pg_src.endDraw();
    
    cube_numx = pg_src_small.width;
    cube_numy = pg_src_small.height;
    
    // blur source texture
    DwFilter.get(context).gaussblur.apply(pg_src, pg_src, pg_src_tmp, 5);

    // update optical flow
    opticalflow.update(pg_src);
    
    // down-scale velocity texture and read to local memory
    tex_of_velocity.resize(context, opticalflow.frameCurr.velocity, cube_numx, cube_numy);
    DwFilter.get(context).copy.apply(opticalflow.frameCurr.velocity, tex_of_velocity);
    buf_of_velocity = tex_of_velocity.getFloatTextureData(buf_of_velocity);

    // down-scale color texture and read to local memory
    pg_src_small.beginDraw();
    pg_src_small.image(pg_src, 0, 0, cube_numx, cube_numy);
    pg_src_small.endDraw();
    // blur again
    DwFilter.get(context).gaussblur.apply(pg_src_small, pg_src_small, pg_src_small_tmp, 1);
    // get pixel buffer
    pg_src_small.loadPixels();
    
    
    float scene_dimx = scene_bounds[3] - scene_bounds[0];
    float scene_dimy = scene_bounds[4] - scene_bounds[1];
    float scene_dimz = scene_bounds[5] - scene_bounds[2];
    float bounds_off = 100;

    float dimx = (scene_dimx - bounds_off*2) / cube_numx;
    float dimy = (scene_dimy - bounds_off*2) / cube_numy;
    
    cube_size = min(dimx, dimy);

    float tx = -cube_size * cube_numx * 0.5f;
    float ty = -cube_size * cube_numy * 0.5f;
    float tz = 0;
    
    PMatrix3D mat = new PMatrix3D();
    
    float FLOW_MAX = 30;
    
    for(int y = 0; y < cube_numy; y++){
      for(int x = 0; x < cube_numx; x++){
        int idx = y * cube_numx + x;

        int rgb = pg_src_small.pixels[idx];
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >>  8) & 0xFF;
        int b = (rgb >>  0) & 0xFF;
        float gray = (r + g + b) / (3f * 255f);
        
        int flow_idx = (cube_numy - y - 1) * cube_numx + x;
        float flows = 3;
        float flowx = buf_of_velocity[flow_idx * 2 + 0] * +flows;
        float flowy = buf_of_velocity[flow_idx * 2 + 1] * -flows;
        
        float flow_mm = flowx*flowx + flowy*flowy;
        float flow_m = (float) Math.pow(flow_mm, 0.40f) * 2;
        
        if(flow_m > FLOW_MAX) flow_m = FLOW_MAX;
        
        float flow_norm = flow_m / FLOW_MAX;
        
        if(!USE_PIXEL_COLORS){
          float mix = 0.15f;
          
          float fr = mix + flow_norm * (1-mix);
          float fg = mix + flow_norm * (1-mix) * 0.5f;
          float fb = mix + flow_norm * (1-mix) * 0.1f;
          
          r = (int) (fr * 255);
          g = (int) (fg * 255);
          b = (int) (fb * 255);
          rgb = 0xFF000000 | r << 16 | g << 8 | b;
        }
        
//
//        float px = x * cube_size;
//        float py = y * cube_size;
//        float pz = scene_dimz * gray * 0.25f;
//        
//        pz = max(pz, 0);
//
//        
//        PShape cube = shp_cubes[idx];
//
//        cube.resetMatrix();
//        cube.rotateZ((float) Math.atan2(flowy, flowx));
//        cube.scale(cube_size + flow_m);
//        cube.translate(tx+px, ty+py, tz+pz);
// 
//        cube.setFill(rgb);
        
        
        
        float px = x * cube_size;
        float py = y * cube_size;
        float pz = 0;
        
        float sx = cube_size + flow_m;
        float sy = cube_size + flow_m;
        float sz = cube_size + flow_m * 2 + scene_dimz * gray * 0.35f;
         
        float flow_ang = (float) Math.atan2(flowy, flowx);
        pz = max(pz, 0);

        mat.reset();
        mat.translate(tx+px, ty+py, tz+pz);
        mat.rotateZ(flow_ang);
        mat.rotateY(35 * flow_norm * PI/180f);
        mat.rotateZ(-flow_ang);
        mat.scale(sx, sy, sz);
        
        
   
        
        
        
        
        
        
        
        
        
        
        PShape cube = shp_cubes[idx];
        cube.resetMatrix();
        cube.applyMatrix(mat);
        cube.setFill(rgb);
      }
    }
  }
  
  
  
  
  
  
  
  
  
  
  
  // Movie Timeline
  class TimeLine{
    float x, y, w, h;
    Movie movie;
    
    public TimeLine(Movie movie, float x, float y, float w, float h){
      this.movie = movie;
      setPosition(x,y,w,h);
    }
    
    public void setPosition(float x, float y, float w, float h){
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }
    
    public boolean inside(float mx, float my){
      return mx >= x && mx <= (x+w) && my >= y && my <= (y+h);
    }
    
    public void jumpToMoviePos(){
      if(inside(mouseX, mouseY)){
        float movie_pos = map(mouseX-x, 0, w, 0, movie.duration());
        movie.jump(movie_pos);
      }
    }
    
    public void draw(float mx, float my){
      float time      = movie.time();
      float duration  = movie.duration();
      float movie_pos = w * time / duration;
      String time_str = String.format(Locale.ENGLISH, "%1.2f", time);
      
      // timeline
      fill(64, 180);
      noStroke();
      rect(x, y, w, h, 4);
      
      // time handle
      fill(32);
      rect(x+movie_pos-25, y, 50, 20, 4);
      
      // time, as text in seconds
      fill(180);
      textFont(font);
      textAlign(CENTER, CENTER);
      text(time_str, x + movie_pos, y + h/2 - 2);
      
      if(inside(mx, my)){
        float hoover_pos = duration * (mx - x) / w;
        String hoover_str = String.format(Locale.ENGLISH, "%1.2f", hoover_pos);
        
        // time handle
        fill(32, 128);
        rect(mx-25, y, 50, 20, 4);
        
        // time, as text in seconds
        fill(180);
        textFont(font);
        textAlign(CENTER, CENTER);
        text(hoover_str, mx, y + h/2 - 2);
      }  
    }
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
  
  
  public void printCam(){
    float[] pos = cam.getPosition();
    float[] rot = cam.getRotations();
    float[] lat = cam.getLookAt();
    float   dis = (float) cam.getDistance();
    
    System.out.printf(Locale.ENGLISH, "position: (%7.3f, %7.3f, %7.3f)\n", pos[0], pos[1], pos[2]);
    System.out.printf(Locale.ENGLISH, "rotation: (%7.3f, %7.3f, %7.3f)\n", rot[0], rot[1], rot[2]);
    System.out.printf(Locale.ENGLISH, "look-at:  (%7.3f, %7.3f, %7.3f)\n", lat[0], lat[1], lat[2]);
    System.out.printf(Locale.ENGLISH, "distance: (%7.3f)\n", dis);
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Skylight_Movie.class.getName() });
  }
}