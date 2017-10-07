/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package OpticalFlow.OpticalFlow_ExportPFM.Demo_OpticalFlowDraw_PFM;


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

public class Demo_OpticalFlowDraw_PFM extends PApplet {
  
  //
  // Optical flow computed based on image sequence.
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
  PGraphics2D pg_src;
  
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
    opticalflow.param.flow_scale         = 50;
    opticalflow.param.temporal_smoothing = 0.8f;
    opticalflow.param.display_mode       = 0;
    
    // render target
    pg_oflow = (PGraphics2D) createGraphics(width, height, P2D);
    pg_oflow.smooth(8);

    // drawing canvas, used as input for the optical flow
    pg_src = (PGraphics2D) createGraphics(width, height, P2D);
    pg_src.smooth(8);
  
    frameRate(60);
//    frameRate(1000);
  }
  

  // animated rectangle data
  float rs = 80;
  float rx = 100;
  float ry = 100;
  float dx = 3;
  float dy = 2.4f;
  
  public void draw() {

    // update rectangle position
    rx += dx;
    ry += dy;
    // keep inside viewport
    if(rx <        rs/2) {rx =        rs/2; dx = -dx; }
    if(rx > width -rs/2) {rx = width -rs/2; dx = -dx; }
    if(ry <        rs/2) {ry =        rs/2; dy = -dy; }
    if(ry > height-rs/2) {ry = height-rs/2; dy = -dy; }
    
    // update input image
    pg_src.beginDraw();
    pg_src.clear();
    pg_src.background(0);
    
    pg_src.rectMode(CENTER);
    pg_src.fill(150, 200, 255);
    pg_src.rect(rx, ry, rs, rs, rs/3f);
    
    pg_src.fill(200, 150, 255);
    pg_src.noStroke();
    pg_src.ellipse(mouseX, mouseY, 100, 100);
    pg_src.endDraw();
    

    // update Optical Flow
    opticalflow.update(pg_src);
    
    
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
    opticalflow.renderVelocityStreams(pg_oflow, 10);
    
    // display result
    background(0);
    image(pg_src, 0, 0);
    image(pg_oflow, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", pg_oflow.width, pg_oflow.height, opticalflow.UPDATE_STEP, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public void keyReleased(){
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
    PApplet.main(new String[] { Demo_OpticalFlowDraw_PFM.class.getName() });
  }
}