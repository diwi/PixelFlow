/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package OpticalFlow.OpticalFlow_ExportPFM.Demo_OpticalFlowMovie_PFM;


import java.io.File;
import java.io.IOException;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwOpticalFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.utils.DwFrameCapture;
import com.thomasdiewald.pixelflow.java.utils.DwPortableFloatMap;

import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Movie;

public class Demo_OpticalFlowMovie_PFM extends PApplet {
  
  //
  // Optical flow computed based on image sequence of a Movie.
  // 
  // The resulting velocity-textures gets exported as PFM-files.
  //
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  DwPixelFlow context;
  DwOpticalFlow opticalflow;
  PGraphics2D pg_oflow;
  
  boolean APPLY_GRAYSCALE = false;
  boolean APPLY_BILATERAL = true;
  
  // buffer for the movie-image
  PGraphics2D pg_movie_a, pg_movie_b; 
  
  Movie movie;
  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(8);
  }

  public void setup() {
    
    surface.setLocation(viewport_x, viewport_y);

    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // opticalflow
    opticalflow = new DwOpticalFlow(context, width, height);
    
    // some flow parameters
    opticalflow.param.blur_flow           = 5;
    opticalflow.param.blur_input          = 10;
    opticalflow.param.temporal_smoothing  = 0.50f;
    opticalflow.param.flow_scale          = 50;
    opticalflow.param.threshold           = 1.0f; // flow vector length threshold
    opticalflow.param.display_mode        = 0;
    
    // render target
    pg_oflow = (PGraphics2D) createGraphics(width, height, P2D);
    pg_oflow.smooth(8);



    // movie file is not contained in the library release
    // to keep the file size small. please use one of your own videos instead.
    movie = new Movie(this, "examples/data/Pulp_Fiction_Dance_Scene.mp4");
    movie.loop();
    movie.frameRate(24);

    frameRate(160);
  }
  

  void resize(Movie movie){
    int mw = movie.width;
    int mh = movie.height;
    
    if(pg_movie_a == null || pg_movie_a.width != mw || pg_movie_a.height != mh){
      
      pg_movie_a = (PGraphics2D) createGraphics(mw, mh, P2D);
      pg_movie_a.smooth(0);
      
      pg_movie_b = (PGraphics2D) createGraphics(mw, mh, P2D);
      pg_movie_b.smooth(0);
      
      pg_oflow = (PGraphics2D) createGraphics(mw, mh, P2D);
      pg_oflow.smooth(0);
      
      System.out.println("resized optical flow frames: "+mw+", "+mh);
    }
  }

  
//  int frameCount_movie = 0;
  
  public void draw() {

    if(movie.available()){
      movie.read();

      resize(movie);
//      frameCount_movie++;
//      float ratio_frameCount = frameCount_movie / (float)frameCount;
//      float ratio_frameRate  = movie.frameRate / (float)frameRate;
//      System.out.println(frameCount_movie+", "+frameCount+", "+ratio_frameCount);
//      System.out.println(movie.frameRate+", "+frameRate+", "+ratio_frameRate);
      
      // put onto gl buffer
      pg_movie_a.beginDraw();
      pg_movie_a.blendMode(REPLACE);
      pg_movie_a.image(movie, 0, 0);
      pg_movie_a.endDraw();
      
      // apply filters (not necessary)
      if(APPLY_GRAYSCALE){
        DwFilter.get(context).luminance.apply(pg_movie_a, pg_movie_a);
      }
      if(APPLY_BILATERAL){
        DwFilter.get(context).bilateral.apply(pg_movie_a, pg_movie_b, 5, 0.10f, 4);
        swapCamBuffer();
      }
      
      // compute Optical Flow
      opticalflow.update(pg_movie_a);
      
      // write PFM
      if(pmf_write_sequence_enabled && frameCount % pmf_write_every_nth_frame == 0){
        writePFM(opticalflow.frameCurr.velocity);
      }
      
      
      // render Optical Flow
      pg_oflow.beginDraw();
      pg_oflow.clear();
      pg_oflow.endDraw();
      
      // opticalflow visualizations
      // 1) velocity is displayed as dense, colored shading
      if(mousePressed && mouseButton == RIGHT) opticalflow.renderVelocityShading(pg_oflow);
      
      // 2) velocity is displayed as vectors
      //    display_mode = 0 --> lines, along the velocity direction
      //    display_mode = 1 --> lines, normal to the velocity direction
      opticalflow.param.display_mode = (mousePressed && mouseButton == CENTER) ? 1 : 0;
      opticalflow.renderVelocityStreams(pg_oflow, Math.round(pmf_scale * 4));
      
      



      // original dimensions
      int screen_w = width;
      int screen_h = height;
      int movie_w = pg_movie_a.width;
      int movie_h = pg_movie_a.height;
      
      // compute scale factor for display
      float scalex = screen_w / (float) movie_w;
      float scaley = screen_h / (float) movie_h;
      float scale = Math.min(scalex, scaley);
      
      // compute position and size, to fit images into screen
      int dw = (int) (movie_w * scale);
      int dh = (int) (movie_h * scale);
      int dx = (int) (-dw/2f + screen_w/2f);
      int dy = (int) (-dh/2f + screen_h/2f);
      
      // display movie, and optical flow vectors on top
      background(0);
      image(pg_movie_a, dx, dy, dw, dh);
      image(pg_oflow  , dx, dy, dw, dh);
    }
    


    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", pg_oflow.width, pg_oflow.height, opticalflow.UPDATE_STEP, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  void swapCamBuffer(){
    PGraphics2D tmp = pg_movie_a;
    pg_movie_a = pg_movie_b;
    pg_movie_b = tmp;
  }
  
   
  public void keyReleased(){
    if(key == '1') APPLY_GRAYSCALE = !APPLY_GRAYSCALE;
    if(key == '2') APPLY_BILATERAL = !APPLY_BILATERAL;
    
    if(key == 'w') writePFM(opticalflow.frameCurr.velocity);
    if(key == 'e') pmf_write_sequence_enabled = !pmf_write_sequence_enabled;
  }
  
  
  
  

  
  
  
  // PFM EXPORT SETTINGS
  boolean pmf_write_sequence_enabled = !true;
  int     pmf_write_every_nth_frame  = 1;
  float   pmf_scale                  = 2;
  boolean pmf_debug_check            = false;
  boolean normalize_velocities       = true;
  
  // PMF write/reader
  DwPortableFloatMap pfm_w = new DwPortableFloatMap();
  DwPortableFloatMap pfm_r = new DwPortableFloatMap();

  // used to create filenames for the PFM sequence
  DwFrameCapture fcapture;
  
  // texture, for down-sampling before writing the pmf-file.
  // TODO: better down-sampling if necessary
  DwGLTexture tex_small = new DwGLTexture();
  
  /**
   * 
   * Writes a PFM-File (Color, 3 x 32bit float) based on the given DwGLTexture.
   * 
   * PFM ... Portable Float Map
   * 
   */
  public void writePFM(DwGLTexture tex){
    
    // compute smaller texture dimensions
    int w = (int) Math.ceil(tex.w / pmf_scale);
    int h = (int) Math.ceil(tex.h / pmf_scale);

    // prepare smaller texture for quicker transfer and export
    tex_small.resize(context, GL2.GL_RGB32F, w, h, GL2.GL_RGB, GL2.GL_FLOAT, GL2.GL_LINEAR, 3, 4, null);
    
    // copy original velocity-texture to a smaller one, GPU
    DwFilter.get(context).copy.apply(tex, tex_small);
    
    // read velocity-texture from GPU memory to our local memory
    pfm_w.float_array = tex_small.getFloatTextureData(pfm_w.float_array);
    
    // optionally do some post-process, CPU
    if(normalize_velocities){
      normalizeVelocities(pfm_w.float_array, w, h);
    }
    
    // write PMF file
    try {
      
      // DwFrameCapture is only used here for creating a filename
      if(fcapture == null){
        fcapture = new DwFrameCapture(this, "examples/");
      }
      
      // create unique filename
      File filename = fcapture.createFilename(String.format("%d_%d", w, h), "pfm");
      
      // write file
      pfm_w.write(filename,  pfm_w.float_array, w, h);
      System.out.println("pfm-file: "+filename.getAbsolutePath());

      // optionally check data
      if(pmf_debug_check){
        pfm_r.read(filename);
        pfm_r.debugPrint();
        pfm_r.debugCompare(pfm_w);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  
  
  
  /**
   * 
   * normalizes the velocity vectors and saves the magnitude in the blue channel.
   * r ... velocity x   [-1, +1]
   * g ... velocity y   [-1, +1]
   * b ... velocity mag [ 0, Float.MAX_VALUE]
   * 
   */
  public void normalizeVelocities(float[] rgb, int w, int h){
    int num_pixel = w * h;
    for(int i = 0, pi = 0; i < num_pixel; i++, pi += 3){
      float x = rgb[pi + 0];
      float y = rgb[pi + 1];
      float mag = (float) Math.sqrt(x*x + y*y);
      rgb[pi + 0] = x / mag;
      rgb[pi + 1] = y / mag;
      rgb[pi + 2] = mag;
    }
  }

  


  public static void main(String args[]) {
    PApplet.main(new String[] { Demo_OpticalFlowMovie_PFM.class.getName() });
  }
}