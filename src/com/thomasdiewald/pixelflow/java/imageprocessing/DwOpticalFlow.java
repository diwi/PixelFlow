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
  
  DwFilter filter;

  public int UPDATE_STEP = 0;


  public DwOpticalFlow(DwPixelFlow context, int w, int h){
    this.context = context;
    this.filter = DwFilter.get(context);

    shader_OF_gray                  = context.createShader(DwPixelFlow.SHADER_DIR+"OpticalFlow/OpticalFlowGray.frag");                                                                     
    shader_OF_rgba                  = context.createShader(DwPixelFlow.SHADER_DIR+"OpticalFlow/OpticalFlow.frag");                                                                         
    shader_OF_renderVelocity        = context.createShader(DwPixelFlow.SHADER_DIR+"OpticalFlow/renderVelocityShading.frag");                                                               
    shader_OF_renderVelocityStreams = context.createShader(DwPixelFlow.SHADER_DIR+"OpticalFlow/renderVelocityStreams.vert", DwPixelFlow.SHADER_DIR+"OpticalFlow/renderVelocityStreams.frag");
    
    resize(w, h);
    context.papplet.registerMethod("dispose", this);
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
    
    if(frameCurr.resized || framePrev.resized){
      UPDATE_STEP = 0;
    }
    
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
  
  
  protected void swapFrames(){
    Frame frame_T = frameCurr;
    frameCurr = framePrev;
    framePrev = frame_T;
  }
  
  
  public void update(DwGLTexture frame_src) {
    
    // .) swap frames, so frameCurr contains latest velocity data
    swapFrames();
    
    // 0) resize(w/h) or reformat(rgba/grayscale)
    resize(frame_src.w, frame_src.h);

    // 1) copy/grayscale
    if(param.grayscale){
      filter.luminance.apply(frame_src, frameCurr.frame);
    } else {
      filter.copy.apply(frame_src, frameCurr.frame);
    }
    
    // 2) run optical flow
    computeOpticalFlow();
  }
  
  
  public void update(PGraphics2D pg_curr) {
    
    // .) swap frames, so frameCurr contains latest velocity data
    swapFrames();

    // 0) resize(w/h) or reformat(rgba/grayscale)
    resize(pg_curr.width, pg_curr.height);

    // 1) copy/grayscale
    if(param.grayscale){
      filter.luminance.apply(pg_curr, frameCurr.frame);
    } else {
      filter.copy.apply(pg_curr, frameCurr.frame);
    }
    
    // 2) run optical flow
    computeOpticalFlow();
  }


  
  public void computeOpticalFlow() {

    context.begin();
    
    // 1) blur
    filter.gaussblur.apply(frameCurr.frame, frameCurr.frame, frameCurr.tmp, param.blur_input);

    // 2) gradients
    filter.sobel.apply(frameCurr.frame, frameCurr.sobelH, Sobel.TYPE._3x3_HORZ);
    filter.sobel.apply(frameCurr.frame, frameCurr.sobelV, Sobel.TYPE._3x3_VERT);
    
    // 3) compute optical flow
    context.beginDraw(frameCurr.velocity);
    DwGLSLProgram shader = param.grayscale ? shader_OF_gray : shader_OF_rgba;
    shader.begin();
    shader.uniform2f     ("wh_rcp"          , 1f/frameCurr.w, 1f/frameCurr.h);
    shader.uniform1f     ("scale"           , -param.flow_scale);
    shader.uniform1f     ("threshold"       , param.threshold);
    shader.uniformTexture("tex_curr_frame"  , frameCurr.frame);
    shader.uniformTexture("tex_prev_frame"  , framePrev.frame);
    shader.uniformTexture("tex_curr_sobelH" , frameCurr.sobelH);
    shader.uniformTexture("tex_prev_sobelH" , framePrev.sobelH);
    shader.uniformTexture("tex_curr_sobelV" , frameCurr.sobelV);
    shader.uniformTexture("tex_prev_sobelV" , framePrev.sobelV);
    shader.drawFullScreenQuad();
    shader.end();
    context.endDraw("OpticalFlow shader");

    // 4) blur the current velocity
    filter.gaussblur.apply(frameCurr.velocity, frameCurr.velocity, frameCurr.tmp, param.blur_flow);
    
    // 5) mix with previous velocity 
    DwGLTexture dst  = frameCurr.velocity;
    DwGLTexture srcA = framePrev.velocity;
    DwGLTexture srcB = frameCurr.velocity;
    float       mix  = Math.min(Math.max(param.temporal_smoothing, 0), 0.99999f);
    float[]     madA = {     mix, 0};
    float[]     madB = {1f - mix, 0};
    filter.merge.apply(dst, srcA, srcB, madA, madB);
    
    UPDATE_STEP++;
    
    context.end("OpticalFlow.update");

  }
  
  
  

  

  public void renderVelocityShading(PGraphics2D dst){
    
    int w = dst.width;
    int h = dst.height;
    
//    dst.beginDraw();
//    dst.blendMode(PConstants.BLEND);
    
    context.begin();
    context.beginDraw(dst);
    shader_OF_renderVelocity.begin();
    shader_OF_renderVelocity.uniform2f     ("wh_rcp"       , 1f/w, 1f/h);
    shader_OF_renderVelocity.uniformTexture("tex_velocity" , frameCurr.velocity);
    shader_OF_renderVelocity.drawFullScreenQuad(0, 0, w, h);
    shader_OF_renderVelocity.end();
    context.endDraw();
    context.end("OpticalFlow.renderFluidVectors");

//    dst.endDraw();
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
    
//    dst.beginDraw();
//    dst.blendMode(PConstants.BLEND);
    
    context.begin();
    context.beginDraw(dst);
    shader_OF_renderVelocityStreams.begin();
    shader_OF_renderVelocityStreams.uniform2f     ("wh_rcp"        , 1f/frameCurr.velocity.w, 1f/frameCurr.velocity.h);
    shader_OF_renderVelocityStreams.uniform1i     ("display_mode"  , param.display_mode);
    shader_OF_renderVelocityStreams.uniform2i     ("num_lines"     , lines_x, lines_y);
    shader_OF_renderVelocityStreams.uniform2f     ("spacing"       , space_x, space_y);
    shader_OF_renderVelocityStreams.uniform1f     ("velocity_scale", scale);
    shader_OF_renderVelocityStreams.uniformTexture("tex_velocity"  , frameCurr.velocity);
    shader_OF_renderVelocityStreams.drawFullScreenLines(0, 0, w, h, num_lines, line_width);
    shader_OF_renderVelocityStreams.end();
    context.endDraw();
    context.end("OpticalFlow.renderVelocityStreams");
    
//    dst.endDraw();
  }
  

  //////////////////////////////////////////////////////////////////////////////
  // DATA TRANSFER: OPENGL <-> HOST APPLICATION
  //////////////////////////////////////////////////////////////////////////////
  
  
  // Transfer velocity data from the GPU to the host-application
  // This is in general a bad idea because such operations are very slow. So 
  // either do everything in shaders, and avoid memory transfer when possible, 
  // or do it very rarely. however, this is just an example for convenience.
  

  // GPU_DATA_READ == 0 --> [x0, y0, x1, y1, ...]
  // GPU_DATA_READ == 1 --> [x0, y0,  0,  1, x1, y1, 0, 1, ....]
  public float[] getVelocity(float[] data_F4, int x, int y, int w, int h){
    return getVelocity(data_F4, x, y, w, h, 0);
  }
  
  public float[] getVelocity(float[] data_F4, int x, int y, int w, int h, int buffer_offset){
    context.begin();
    data_F4 = frameCurr.velocity.getFloatTextureData(data_F4, x, y, w, h, buffer_offset);
    context.end("Fluid.getVelocity");
    return data_F4;
  }
  
  // GPU_DATA_READ == 0 --> [x0, y0, x1, y1, ...]
  // GPU_DATA_READ == 1 --> [x0, y0,  0,  1, x1, y1, 0, 1, ....]
  public float[] getVelocity(float[] data_F4){
    context.begin();
    data_F4 = frameCurr.velocity.getFloatTextureData(data_F4);
    context.end("Fluid.getVelocity");
    return data_F4;
  }
  
  
  
  
  
  
  
  
  
  

  
  
  
  
  
  public static class Frame{
    public DwPixelFlow context;
    public DwGLTexture frame    = new DwGLTexture();
    public DwGLTexture sobelH   = new DwGLTexture();
    public DwGLTexture sobelV   = new DwGLTexture();  
    public DwGLTexture velocity = new DwGLTexture();
    public DwGLTexture tmp      = new DwGLTexture();
    
    
    public boolean resized = false;
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
      int wrap_st        = GL2ES2.GL_MIRRORED_REPEAT;
      
      this.context = context_;
      this.w = w;
      this.h = h;
        
      context.begin();
      resized = false;
      resized |= frame   .resize(context, internalformat   , w, h, format        , type, GL2ES2.GL_LINEAR, channels,2);
      resized |= sobelH  .resize(context, internalformat   , w, h, format        , type, GL2ES2.GL_LINEAR, channels,2);
      resized |= sobelV  .resize(context, internalformat   , w, h, format        , type, GL2ES2.GL_LINEAR, channels,2);
      resized |= velocity.resize(context, GL2ES2.GL_RG16F  , w, h, GL2ES2.GL_RG  , type, GL2ES2.GL_LINEAR, 2       ,2);
      resized |= tmp     .resize(context, GL2ES2.GL_RGBA16F, w, h, GL2ES2.GL_RGBA, type, GL2ES2.GL_LINEAR, 4       ,2);
      if(resized) {
        frame   .setParam_WRAP_S_T(wrap_st);
        sobelH  .setParam_WRAP_S_T(wrap_st);
        sobelV  .setParam_WRAP_S_T(wrap_st);
        velocity.setParam_WRAP_S_T(wrap_st);
        tmp     .setParam_WRAP_S_T(wrap_st);
      }
      context.end();
    }

    
  }
  
  
 
}
