/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.imageprocessing;


import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Sobel;
import processing.core.PConstants;
import processing.opengl.PGraphics2D;;

public class DwOpticalFlow {
  
  
  static public class Param {
    public int     blur_flow           = 5;
    public int     blur_input          = 10;
    public float   temporal_smoothing  = 0.50f;
    public float   flow_scale          = 50;
    public float   threshold           = 1.0f;
    public int     display_mode        = 1; // 0=dir, 1=normal, 2=shading
    public boolean grayscale           = true;
  }
  
  
  public DwPixelFlow context;
  
  public Param param = new Param();
  
  public Frame frameCurr = new Frame();
  public Frame framePrev = new Frame();
  
  DwGLSLProgram shader_OF_gray                 ;
  DwGLSLProgram shader_OF_rgba                 ;
  DwGLSLProgram shader_OF_renderVelocity       ;
  DwGLSLProgram shader_OF_renderVelocityStreams;


  public int UPDATE_STEP = 0;


  public DwOpticalFlow(DwPixelFlow context, int w, int h){
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    shader_OF_gray                  = context.createShader(DwPixelFlow.SHADER_DIR+"OpticalFlow/OpticalFlowGray.frag");                                                                     
    shader_OF_rgba                  = context.createShader(DwPixelFlow.SHADER_DIR+"OpticalFlow/OpticalFlow.frag");                                                                         
    shader_OF_renderVelocity        = context.createShader(DwPixelFlow.SHADER_DIR+"OpticalFlow/renderVelocityShading.frag");                                                               
    shader_OF_renderVelocityStreams = context.createShader(DwPixelFlow.SHADER_DIR+"OpticalFlow/renderVelocityStreams.vert", DwPixelFlow.SHADER_DIR+"OpticalFlow/renderVelocityStreams.frag");
    
    
    resize(w, h);
  }

  public void dispose(){
    release();
  }
  
  public void release(){
    frameCurr.release();
    framePrev.release();
  }
  
  public void resize(int w, int h) {
    context.begin();
    frameCurr.resize(context, w, h, param.grayscale);
    framePrev.resize(context, w, h, param.grayscale);
    context.end("OpticalFlow.resize");
  }
  
  public void reset(){
    clear(0.0f);
  }
  
  private void clear(float v){
    context.begin();
    frameCurr.clear(v);
    framePrev.clear(v);
    context.end("OpticalFlow.clear");
  }
  

  public void update(PGraphics2D pg_curr) {

    // .) swap frames
    Frame frame_T = frameCurr;
    frameCurr = framePrev;
    framePrev = frame_T;
    
    // 0) resize(w/h) or reformat(rgba/grayscale)
    resize(frameCurr.w, frameCurr.h);
    
    DwFilter filter =  DwFilter.get(context);
    
    // 1) copy/grayscale
    if(param.grayscale){
      filter.luminance.apply(pg_curr, frameCurr.frame);
    } else {
      filter.copy.apply(pg_curr, frameCurr.frame);
    }
    
    // 2) blur
    filter.gaussblur.apply(frameCurr.frame, frameCurr.frame, frameCurr.tmp, param.blur_input);

    // 3) gradients
    filter.sobel.apply(frameCurr.frame, frameCurr.sobelH, Sobel.TYPE._3x3_HORZ);
    filter.sobel.apply(frameCurr.frame, frameCurr.sobelV, Sobel.TYPE._3x3_VERT);
    
    // 4) compute optical flow
    context.begin();
    context.beginDraw(frameCurr.velocity);
    

    DwGLSLProgram shader = param.grayscale ? shader_OF_gray : shader_OF_rgba;
    
    shader.begin();
    shader.uniform2f     ("wh"              , frameCurr.velocity.w, frameCurr.velocity.h);
    shader.uniform1f     ("scale"           , param.flow_scale * -1f);
    shader.uniform1f     ("threshold"       , param.threshold);
    shader.uniformTexture("tex_curr_frame"  , frameCurr.frame);
    shader.uniformTexture("tex_prev_frame"  , framePrev.frame);
    shader.uniformTexture("tex_curr_sobelH" , frameCurr.sobelH);
    shader.uniformTexture("tex_prev_sobelH" , framePrev.sobelH);
    shader.uniformTexture("tex_curr_sobelV" , frameCurr.sobelV);
    shader.uniformTexture("tex_prev_sobelV" , framePrev.sobelV);
    shader.drawFullScreenQuad();
    shader.end();
    
    context.endDraw();
    context.end("OpticalFlow.update");
    
    // 5) blur the current velocity
    filter.gaussblur.apply(frameCurr.velocity, frameCurr.velocity, frameCurr.tmp, param.blur_flow);
    
    // 6) mix with previous velocity 
    float mix = param.temporal_smoothing;
    if(mix < 0) mix = 0; else if(mix > 0.99999) mix = 0.99999f;
    
    DwGLTexture dst = frameCurr.velocity;
    DwGLTexture srcA = framePrev.velocity;
    DwGLTexture srcB = frameCurr.velocity;
    float[]     madA = {     mix, 0};
    float[]     madB = {1f - mix, 0};
    filter.merge.apply(dst, srcA, srcB, madA, madB);
    
    UPDATE_STEP++;
  }
  
  
  

  

  public void renderVelocityShading(PGraphics2D dst){
    
    int w = dst.width;
    int h = dst.height;
    
    dst.beginDraw();
    dst.blendMode(PConstants.BLEND);
    
    context.begin();
    shader_OF_renderVelocity.begin();
    shader_OF_renderVelocity.uniform2f     ("wh"            , w, h);
    shader_OF_renderVelocity.uniformTexture("tex_velocity"  , frameCurr.velocity);
    shader_OF_renderVelocity.drawFullScreenQuad(0, 0, w, h);
    shader_OF_renderVelocity.end();
    context.end("OpticalFlow.renderFluidVectors");
    dst.endDraw();
  }
  
  
  public void renderVelocityStreams(PGraphics2D dst, int spacing){

    if(param.display_mode > 1){
      return;
    }

    int w = dst.width;
    int h = dst.height;
    
    int   lines_x    = Math.round(w / spacing);
    int   lines_y    = Math.round(h / spacing);
    int   num_lines  = lines_x * lines_y;
    float space_x    = w / (float) lines_x;
    float space_y    = h / (float) lines_y;
    float scale      = (space_x + space_y) * 1;
    float line_width = 1.0f;
    
    dst.beginDraw();
    dst.blendMode(PConstants.BLEND);
    
    context.begin();
    shader_OF_renderVelocityStreams.begin();
    shader_OF_renderVelocityStreams.uniform2f     ("wh"            , frameCurr.velocity.w, frameCurr.velocity.h);
    shader_OF_renderVelocityStreams.uniform1i     ("display_mode"  , param.display_mode);
    shader_OF_renderVelocityStreams.uniform2i     ("num_lines"     , lines_x, lines_y);
    shader_OF_renderVelocityStreams.uniform2f     ("spacing"       , space_x, space_y);
    shader_OF_renderVelocityStreams.uniform1f     ("velocity_scale", scale);
    shader_OF_renderVelocityStreams.uniformTexture("tex_velocity"  , frameCurr.velocity);
    shader_OF_renderVelocityStreams.drawFullScreenLines(0, 0, w, h, num_lines, line_width);
    shader_OF_renderVelocityStreams.end();
    context.end("OpticalFlow.renderVelocityStreams");
    
    dst.endDraw();
  }
  

  // GPU_DATA_READ == 0 --> [x0, y0, x1, y1, ...]
  // GPU_DATA_READ == 1 --> [x0, y0,  0,  1, x1, y1, 0, 1, ....]
  public float[] getVelocity(float[] data_F4){
    context.begin();
    float[] data = frameCurr.velocity.getFloatTextureData(data_F4);
    context.end("OpticalFlow.getVelocity");
    return data;
  }
  
  
  
  
  
  
  
  
  
  

  
  
  
  
  
  public static class Frame{
    public DwPixelFlow context;
    public DwGLTexture frame    = new DwGLTexture();
    public DwGLTexture sobelH   = new DwGLTexture();
    public DwGLTexture sobelV   = new DwGLTexture();  
    public DwGLTexture velocity = new DwGLTexture();
    public DwGLTexture tmp      = new DwGLTexture();
    
    protected int w, h;
    
    public Frame(){
    }
    
    public void release(){
      tmp     .release();
      frame   .release();
      sobelH  .release();
      sobelV  .release();  
      velocity.release();
    }
    
    public void clear(float v){
      context.begin();
      tmp     .clear(v);
      frame   .clear(v);
      sobelH  .clear(v);
      sobelV  .clear(v);  
      velocity.clear(v);
      context.end();
    }
    
    public void resize(DwPixelFlow context_, int w, int h, boolean grayscale){
      int internalformat = grayscale ? GL2ES2.GL_R16F : GL2ES2.GL_RGBA16F;
      int format         = grayscale ? GL2ES2.GL_RED  : GL2ES2.GL_RGBA;
      int channels       = grayscale ? 1              : 4;
      int type           = GL2ES2.GL_FLOAT;
      
      this.context = context_;
      this.w = w;
      this.h = h;
        
      context.begin();
      boolean resized = false;

      resized |= frame   .resize(context, internalformat   , w, h, format        , type, GL2ES2.GL_LINEAR, channels,4);
      resized |= sobelH  .resize(context, internalformat   , w, h, format        , type, GL2ES2.GL_LINEAR, channels,4);
      resized |= sobelV  .resize(context, internalformat   , w, h, format        , type, GL2ES2.GL_LINEAR, channels,4);
      resized |= velocity.resize(context, GL2ES2.GL_RG16F  , w, h, GL2ES2.GL_RG  , type, GL2ES2.GL_LINEAR, 2       ,4);
      resized |= tmp     .resize(context, GL2ES2.GL_RGBA16F, w, h, GL2ES2.GL_RGBA, type, GL2ES2.GL_LINEAR, 4       ,4);
      if(resized) updateParams();
      context.end();
    }

    private void updateParams(){
      context.begin();
      frame   .setParam_WRAP_S_T(GL2ES2.GL_MIRRORED_REPEAT);
      sobelH  .setParam_WRAP_S_T(GL2ES2.GL_MIRRORED_REPEAT);
      sobelV  .setParam_WRAP_S_T(GL2ES2.GL_MIRRORED_REPEAT);
      velocity.setParam_WRAP_S_T(GL2ES2.GL_MIRRORED_REPEAT);
      tmp     .setParam_WRAP_S_T(GL2ES2.GL_MIRRORED_REPEAT);
      context.end();
    }
    
  }
  
  
  
   
 
}
