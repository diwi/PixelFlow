/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.imageprocessing;

import java.time.LocalDateTime;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture3D;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureCube;

import processing.core.PGraphics;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;


/**
 * 
 * Little ShaderToy Wrapper.
 * 
 * @author Thomas Diewald
 *
 */
public class DwShadertoy{
  
  public DwPixelFlow context;

  public float[]       iResolution        = new float[3]  ; // uniform vec3  iResolution;           // image/buffer          The viewport resolution (z is pixel aspect ratio, usually 1.0)
  public float         iTime              = 0             ; // uniform float iTime;                 // image/sound/buffer    Current time in seconds
  public float         iTimeDelta         = 0             ; // uniform float iTimeDelta;            // image/buffer          Time it takes to render a frame, in seconds
  public int           iFrame             = 0             ; // uniform int   iFrame;                // image/buffer          Current frame
  public float         iFrameRate         = 0             ; // uniform float iFrameRate;            // image/buffer          Number of frames rendered per second
  public float[]       iMouse             = new float[4]  ; // uniform vec4  iMouse;                // image/buffer          xy = current pixel coords (if LMB is down). zw = click pixel
  public float[]       iDate              = new float[4]  ; // uniform vec4  iDate;                 // image/buffer/sound    Year, month, day, time in seconds in .xyzw
  public float         iSampleRate        = 44100         ; // uniform float iSampleRate;           // image/buffer/sound    The sound sample rate (typically 44100)
  public float[]       iChannelTime       = new float[4]  ; // uniform float iChannelTime[4];       // image/buffer          Time for channel (if video or sound), in seconds
  public float[]       iChannelResolution = new float[12] ; // uniform vec3  iChannelResolution[4]; // image/buffer/sound    Input texture resolution for each channel
  public int[]         iChannel           = new int[4]    ; // uniform sampler[2D,3D,Cube] iChannel0..3; // image/buffer/sound    Sampler for input textures i


  /**
   * Default RGBA32F-Rendertarget for this buffer.
   */
  public DwGLTexture tex = new DwGLTexture();
  
  public DwGLSLProgram shader;
  
  public long iTime_start = System.currentTimeMillis();
  
  public boolean auto_reset_on_resize = true;
  
  public DwShadertoy(DwPixelFlow context, String shader_filename){
    this.context = context;
    this.context.papplet.registerMethod("dispose", this);
    createShader(shader_filename);
    resize(context.papplet.width, context.papplet.height);
  }
  

  public DwShadertoy createShader(String shader_filename){
    
    shader = context.createShader(shader_filename);
    
    // add main(), etc...
    {
      String[] src_org = shader.frag.content;
      
      int pos = shader.frag.content.length;
      
      String[] src_new = new String[pos + 4];
      System.arraycopy(src_org, 0, src_new, 0, pos);
      
      src_new[pos++] = "";
      src_new[pos++] = "out vec4 out_frag;";
      src_new[pos++] = "void main(){  mainImage(out_frag, gl_FragCoord.xy); }";
      src_new[pos++] = "";
      
      // use modified source
      shader.frag.content = src_new;
    }
    
    // no need to print a warning for unused uniforms
    shader.LOG_WARNINGS = false;
        
    reset();
    
    return this;
  }
  
  public void dispose(){
    release();
  }
  
  public void release(){
    tex.release();
  }
  
  
  public boolean resize(int w, int h){
    boolean resized = tex.resize(context, GL2.GL_RGBA32F, w, h, GL2.GL_RGBA, GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_CLAMP_TO_EDGE, 4, 4);
    if(resized && auto_reset_on_resize){
      reset();
      tex.clear(0.0f);
    }
    return resized;
  }
  
  
  
  
  
  
  
  
  /**
   * resets framecount and time.
   */
  public void reset(){
    iTime_start = System.currentTimeMillis();
    iFrame = 0;
  }
  
  
  
  /**
   * uniform vec3 iResolution; // image/buffer, viewport resolution (z is pixel aspect ratio, usually 1.0)
   * @param w
   * @param h
   * @param pixelaspect
   */
  public void set_iResolution(float w, float h, float pixelaspect){
    iResolution[0] = w;
    iResolution[1] = h;
    iResolution[2] = pixelaspect;
  }
  
  /**
   * uniform float iTime; // image/sound/buffer, Current time in seconds
   * @param time
   */
  public void set_iTime(float time){
    iTime = time;
  }
  
  /**
   * uniform float iTime; // image/sound/buffer, Current time in seconds
   */
  public void set_iTime(){
    iTime = (System.currentTimeMillis() - iTime_start) / 1000f;
  }
  /**
   * uniform float iTimeDelta; // image/buffer, Time it takes to render a frame, in seconds
   * @param time_delta
   */
  public void set_iTimeDelta(float time_delta){
    iTimeDelta = time_delta;
  }
  /**
   * uniform int iFrame; // image/buffer, Current frame
   * @param framecount
   */
  public void set_iFrame(int framecount){
    iFrame = framecount;
  }
  /**
   * uniform float iFrameRate; // image/buffer, Number of frames rendered per second
   * @param framerate
   */
  public void set_iFrameRate(float framerate){
    iFrameRate = framerate;
  }
  /**
   * uniform vec4 iMouse; // image/buffer, xy = current pixel coords (if LMB is down). zw = click pixel
   * @param press_mx
   * @param press_my
   * @param click_mx
   * @param click_my
   */
  public void set_iMouse(float press_mx, float press_my, float click_mx, float click_my){
    iMouse[0] = press_mx;
    iMouse[1] = press_my;
    iMouse[2] = click_mx;
    iMouse[3] = click_my;
  }
  /**
   * uniform vec4 iDate; // image/buffer/sound, Year, month, day, time in seconds in .xyzw
   * @param year
   * @param month
   * @param day
   * @param time_in_seconds
   */
  public void set_iDate(float year, float month, float day, float time_in_seconds){
    iDate[0] = year;
    iDate[1] = month;
    iDate[2] = day;
    iDate[3] = time_in_seconds;
  }
  /**
   * uniform vec4 iDate; // image/buffer/sound, Year, month, day, time in seconds in .xyzw
   */
  public void set_iDate(){
    LocalDateTime time = LocalDateTime.now();
    iDate[0] = time.getYear();
    iDate[1] = time.getMonthValue();
    iDate[2] = time.getDayOfMonth();
    iDate[3] = System.currentTimeMillis() / 1000;
  }
  /**
   * uniform float iSampleRate; // image/buffer/sound, The sound sample rate (typically 44100)
   * @param x
   */
  public void set_iSampleRate(float samplerate){
    iSampleRate = samplerate;
  }
  /**
   * uniform float iChannelTime[4]; // image/buffer, Time for channel (if video or sound), in seconds
   * @param time_CH0
   * @param time_CH1
   * @param time_CH2
   * @param time_CH3
   */
  public void set_iChannelTime(float time_CH0, float time_CH1, float time_CH2, float time_CH3){
    iChannelTime[0] = time_CH0;
    iChannelTime[1] = time_CH1;
    iChannelTime[2] = time_CH2;
    iChannelTime[3] = time_CH3;
  }
  
  
  
  /**
   * uniform vec3 iChannelResolution[4]; // image/buffer/sound, Input texture resolution for each channel
   * @param channel
   * @param dim_w
   * @param dim_h
   * @param dim_d
   */
  public void set_iChannelResolution(int channel, float dim_w, float dim_h, float dim_d){
    iChannelResolution[channel * 3 + 0] = dim_w;
    iChannelResolution[channel * 3 + 1] = dim_h;
    iChannelResolution[channel * 3 + 2] = dim_d;
  }
  /**
   * sampler2D, sampler3D, samplerCube
   * @param channel
   * @param type
   */
  public void set_iChannelType(int channel, String type){
    shader.frag.setDefine("SAMPLER"+channel, type);
  }
  
  /**
   * uniform sampler2D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, int tex_handle){
    iChannel[channel] = tex_handle;
  }
  


  
  
  
  /**
   * generic function to set channel texture data
   * uniform sampler2D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * 
   * @param channel
   * @param tex_handle  gl texture handle
   * @param tex_w       width
   * @param tex_h       height
   * @param tex_d       depth
   * @param type        sampler-type
   */
  public void set_iChannel(int channel, int tex_handle, int tex_w, int tex_h, int tex_d, String type){
    set_iChannel          (channel, tex_handle);
    set_iChannelResolution(channel, tex_w, tex_h, tex_d);
    set_iChannelType      (channel, type);
  }
  

  /**
   * uniform sampler2D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, DwShadertoy toy){
    if(toy.tex.isTexture()){
      set_iChannel(channel, toy.tex.HANDLE[0], toy.tex.w, toy.tex.h, 1, "sampler2D");
    }
  }
  
  /**
   * uniform sampler2D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, PGraphicsOpenGL pg){
    Texture tex2D = pg.getTexture(); 
    if(tex2D.available()){
      set_iChannel(channel, tex2D.glName, tex2D.glWidth, tex2D.glHeight, 1, "sampler2D");
    }
  }
  
  /**
   * uniform sampler2D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, DwGLTexture tex2D){
    if(tex2D.isTexture()){
      set_iChannel(channel, tex2D.HANDLE[0], tex2D.w, tex2D.h, 1, "sampler2D");
    }
  }
  /**
   * uniform sampler3D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, DwGLTexture3D tex3D){
    if(tex3D.isTexture()){
      set_iChannel(channel, tex3D.HANDLE[0], tex3D.w, tex3D.h, tex3D.d, "sampler3D");
    }
  }
  
  /**
   * uniform samplerCube iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, DwGLTextureCube texCube){
    System.out.println("WARNING: DwShadertoy.set_iChannel(int, DwGLTextureCube) is not implemented yet!");
//    if(texCube.isTexture()){
//      set_iChannel(channel, texCube.HANDLE[0], texCube.w, texCube.h, texCube.d, "samplerCube");
//    }
  }
  



  
  protected void render(int w, int h){
    
    set_iResolution(w, h, 1f);
    set_iFrameRate(context.papplet.frameRate);
    set_iTimeDelta(1f/context.papplet.frameRate);
    set_iTime();
    set_iDate();
    
    shader.begin();
    shader.uniform3fv    ("iResolution"       , 1, iResolution       );
    shader.uniform1f     ("iTime"             , iTime                );
    shader.uniform1f     ("iTimeDelta"        , iTimeDelta           );
    shader.uniform1i     ("iFrame"            , iFrame               );
    shader.uniform1f     ("iFrameRate"        , iFrameRate           );
    shader.uniform4fv    ("iMouse"            , 1, iMouse            );
    shader.uniform4fv    ("iDate"             , 1, iDate             );
    shader.uniform1f     ("iSampleRate"       , iSampleRate          );
    shader.uniform1fv    ("iChannelTime"      , 4, iChannelTime      );
    shader.uniform3fv    ("iChannelResolution", 4, iChannelResolution);
    shader.uniformTexture("iChannel0"         , iChannel[0]          );
    shader.uniformTexture("iChannel1"         , iChannel[1]          );
    shader.uniformTexture("iChannel2"         , iChannel[2]          );
    shader.uniformTexture("iChannel3"         , iChannel[3]          );
    shader.drawFullScreenQuad();
    shader.end();
    
    set_iFrame(iFrame + 1);
  }
  
  

  
  /**
   * execute shader, using the default rendertarget.
   */
  public void apply(int w, int h){
    resize(w, h);
    context.begin();
    context.beginDraw(tex);
    render(tex.w, tex.h);
    context.endDraw();
    context.end();
  }
  
  
  /**
   * execute shader, using the given rendertarget.
   * @param tex_dst
   */
  public void apply(DwGLTexture tex_dst){
    resize(tex_dst.w, tex_dst.h);
    context.begin();
    context.beginDraw(tex_dst);
    render(tex_dst.w, tex_dst.h);
    context.endDraw();
    context.end();
  }
  
  /**
   * execute shader, using the given PGRaphics-rendertarget.
   * @param tex_dst
   */
  public void apply(PGraphicsOpenGL pg_dst){
    resize(pg_dst.width, pg_dst.height);
    pg_dst.getTexture();
    context.begin();
    context.beginDraw(pg_dst);
    render(pg_dst.width, pg_dst.height);
    context.endDraw();
    context.end();
  }
  
  /**
   * execute shader, using the given PGRaphics-rendertarget.
   * @param tex_dst
   */
  public void apply(PGraphics pg_dst){
    if(pg_dst.isGL()){
      apply((PGraphicsOpenGL)pg_dst);
    }
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  
  static public enum TexFilter{
    NEAREST(GL2.GL_NEAREST, GL2.GL_NEAREST),
    LINEAR(GL2.GL_LINEAR, GL2.GL_LINEAR),
    MIPMAP(GL2.GL_LINEAR_MIPMAP_LINEAR, GL2.GL_LINEAR);
    
    int min, mag;
    
    private TexFilter(int min, int mag){
      this.min = min;
      this.mag = mag;
    }
  }
  
  static public enum TexWrap{
    CLAMP(GL2.GL_CLAMP_TO_EDGE),
    REPEAT(GL2.GL_MIRRORED_REPEAT);
    
    int wrap;
    
    private TexWrap(int wrap){
      this.wrap = wrap;
    }
  }
  
    
  public static void setTextureFilter(DwGLTexture tex, TexFilter filter){
    tex.setParamFilter(filter.min, filter.mag);
    if(filter == TexFilter.MIPMAP){
      tex.generateMipMap();
    }
  }
  public static void setTextureWrap(DwGLTexture tex, TexWrap wrap){
    tex.setParamWrap(wrap.wrap); 
  }

  

  
  
}