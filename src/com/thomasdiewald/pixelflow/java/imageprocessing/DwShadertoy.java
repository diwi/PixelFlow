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

  // DwGLTexture    [] iChannel2D   = new DwGLTexture[4]  ; // uniform sampler2D   iChannel0..3;          // image/buffer/sound    Sampler for input textures i
  // DwGLTexture3D  [] iChannel3D   = new DwGLTexture3D[4]; // uniform sampler3D   iChannel0..3;          // image/buffer/sound    Sampler for input textures i
  // DwGLTextureCube[] iChannelCube = new DwGLTexture3D[4]; // uniform samplerCube iChannel0..3;          // image/buffer/sound    Sampler for input textures i, TODO

  /**
   * Default RGBA32F-Rendertarget for this buffer.
   */
  public DwGLTexture tex = new DwGLTexture();
  
  public DwGLSLProgram shader;
  
  public long iTime_start = System.currentTimeMillis();
  
  public DwShadertoy(DwPixelFlow context, String shader_filename){
    this.context = context;
    createShader(shader_filename);
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
  
  
  
  public boolean resize(int w, int h){
    boolean resized = tex.resize(context, GL2.GL_RGBA32F, w, h, GL2.GL_RGBA, GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_CLAMP_TO_EDGE, 4, 4);
    if(resized){
      reset();
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
   * uniform sampler2D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, DwShadertoy toy){
    if(toy.tex.isTexture() && channel >= 0 && channel <= 3){
      iChannel[channel] = toy.tex.HANDLE[0];
      set_iChannelResolution(channel, toy.tex.w, toy.tex.h, 1.0f);
      shader.frag.setDefine("SAMPLER"+channel, "sampler2D");
    }
  }
  
  /**
   * uniform sampler2D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, PGraphicsOpenGL pg){
    if(channel >= 0 && channel <= 3){
      Texture tex = pg.getTexture(); 
      if(tex.available()){
        iChannel[channel] = tex.glName;
        set_iChannelResolution(channel, tex.glWidth, tex.glHeight, 1.0f);
        shader.frag.setDefine("SAMPLER"+channel, "sampler2D");
      }
    }
  }
  
  /**
   * uniform sampler2D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, DwGLTexture tex2D){
    if(tex2D != null && channel >= 0 && channel <= 3){
      iChannel[channel] = tex2D.HANDLE[0];
      set_iChannelResolution(channel, tex2D.w, tex2D.h, 1.0f);
      shader.frag.setDefine("SAMPLER"+channel, "sampler2D");
    }
  }
  /**
   * uniform sampler3D iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, DwGLTexture3D tex3D){
    if(tex3D != null && channel >= 0 && channel <= 3){
      iChannel[channel] = tex3D.HANDLE[0];
      set_iChannelResolution(channel, tex3D.w, tex3D.h, tex3D.d);
      shader.frag.setDefine("SAMPLER"+channel, "sampler3D");
    }
  }
  
  /**
   * uniform samplerCube iChannel0..3; // image/buffer/sound, Sampler for input textures i
   * @param channel
   * @param tex
   */
  public void set_iChannel(int channel, DwGLTextureCube texCube){
    System.out.println("WARNING: DwShadertoy.set_iChannel(int, DwGLTextureCube) is not implemented yet!");
//    if(texCube != null && channel >= 0 && channel <= 3){
//      iChannel[channel] = texCube.HANDLE[0];
//      set_iChannelResolution(channel, texCube.w, texCube.h, texCube.d);
//      shader.frag.setDefine("SAMPLER"+channel, "samplerCube");
//    }
  }
  



  
  protected void render(int w, int h){
    
    set_iResolution(w, h, 1f);
    set_iFrameRate(context.papplet.frameRate);
    set_iTimeDelta(1f/context.papplet.frameRate);
    set_iTime();
    set_iDate();

    shader.begin();
    shader.uniform3fv("iResolution"       , 1, iResolution       ); // vec3  iResolution;           image/buffer      
    shader.uniform1f ("iTime"             , iTime                ); // float iTime;                 image/sound/buffer
    shader.uniform1f ("iTimeDelta"        , iTimeDelta           ); // float iTimeDelta;            image/buffer      
    shader.uniform1i ("iFrame"            , iFrame               ); // int   iFrame;                image/buffer      
    shader.uniform1f ("iFrameRate"        , iFrameRate           ); // float iFrameRate;            image/buffer      
    shader.uniform4fv("iMouse"            , 1, iMouse            ); // vec4  iMouse;                image/buffer      
    shader.uniform4fv("iDate"             , 1, iDate             ); // vec4  iDate;                 image/buffer/sound
    shader.uniform1f ("iSampleRate"       , iSampleRate          ); // float iSampleRate;           image/buffer/sound
    shader.uniform1fv("iChannelTime"      , 4, iChannelTime      ); // float iChannelTime[4];       image/buffer      
    shader.uniform3fv("iChannelResolution", 4, iChannelResolution); // vec3  iChannelResolution[4]; image/buffer/sound
    
    shader.uniformTexture ("iChannel0", iChannel[0]);
    shader.uniformTexture ("iChannel1", iChannel[1]);
    shader.uniformTexture ("iChannel2", iChannel[2]);
    shader.uniformTexture ("iChannel3", iChannel[3]);
    
 
//    if(iChannel2D[0] != null) shader.uniformTexture ("iChannel0", iChannel2D[0]);
//    if(iChannel2D[1] != null) shader.uniformTexture ("iChannel1", iChannel2D[1]);
//    if(iChannel2D[2] != null) shader.uniformTexture ("iChannel2", iChannel2D[2]);
//    if(iChannel2D[3] != null) shader.uniformTexture ("iChannel3", iChannel2D[3]);
//    
//    if(iChannel3D[0] != null) shader.uniformTexture ("iChannel0", iChannel3D[0]);
//    if(iChannel3D[1] != null) shader.uniformTexture ("iChannel1", iChannel3D[1]);
//    if(iChannel3D[2] != null) shader.uniformTexture ("iChannel2", iChannel3D[2]);
//    if(iChannel3D[3] != null) shader.uniformTexture ("iChannel3", iChannel3D[3]);
    
    shader.drawFullScreenQuad();
    shader.end();
    
    set_iFrame(iFrame + 1);
  }
  
  /**
   * execute shader, using tex_dst as rendertarget.
   * @param tex_dst
   */
  public void apply(){
    context.begin();
    context.beginDraw(tex);
    render(tex.w, tex.h);
    context.endDraw();
    context.end();
  }
  
  
  /**
   * execute shader, using tex_dst as rendertarget.
   * @param tex_dst
   */
  public void apply(DwGLTexture tex_dst){
    context.begin();
    context.beginDraw(tex_dst);
    render(tex_dst.w, tex_dst.h);
    context.endDraw();
    context.end();
  }
  
  /**
   * execute shader, using pg_dst as rendertarget.
   * @param tex_dst
   */
  public void apply(PGraphicsOpenGL pg_dst){
    pg_dst.getTexture();
    context.begin();
    context.beginDraw(pg_dst);
    render(pg_dst.width, pg_dst.height);
    context.endDraw();
    context.end();
  }
  

  
  
}