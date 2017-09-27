package com.thomasdiewald.pixelflow.java.imageprocessing;

import java.time.LocalDateTime;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture3D;

import processing.core.PApplet;
import processing.opengl.PGraphicsOpenGL;


/**
 * 
 * Little ShaderToy wrapper
 * 
 * @author Thomas Diewald
 *
 */
public class DwShadertoy{
  
  DwPixelFlow context;

  float[]       iResolution        = new float[3]  ; // uniform vec3      iResolution;           // image/buffer          The viewport resolution (z is pixel aspect ratio, usually 1.0)
  float         iTime                              ; // uniform float     iTime;                 // image/sound/buffer    Current time in seconds
  float         iTimeDelta                         ; // uniform float     iTimeDelta;            // image/buffer          Time it takes to render a frame, in seconds
  int           iFrame                             ; // uniform int       iFrame;                // image/buffer          Current frame
  float         iFrameRate                         ; // uniform float     iFrameRate;            // image/buffer          Number of frames rendered per second
  float[]       iMouse             = new float[4]  ; // uniform vec4      iMouse;                // image/buffer          xy = current pixel coords (if LMB is down). zw = click pixel
  float[]       iDate              = new float[4]  ; // uniform vec4      iDate;                 // image/buffer/sound    Year, month, day, time in seconds in .xyzw
  float         iSampleRate                        ; // uniform float     iSampleRate;           // image/buffer/sound    The sound sample rate (typically 44100)
  float[]       iChannelTime       = new float[4]  ; // uniform float     iChannelTime[4];       // image/buffer          Time for channel (if video or sound), in seconds
  float[]       iChannelResolution = new float[12] ; // uniform vec3      iChannelResolution[4]; // image/buffer/sound    Input texture resolution for each channel
 
  DwGLTexture  [] iChannel2D   = new DwGLTexture[4]  ; // uniform sampler2D   iChannel0..3;          // image/buffer/sound    Sampler for input textures i
  DwGLTexture3D[] iChannel3D   = new DwGLTexture3D[4]; // uniform sampler3D   iChannel0..3;          // image/buffer/sound    Sampler for input textures i
  // DwGLTextureCube[] iChannelCube = new DwGLTexture3D[4]; // uniform samplerCube iChannel0..3;          // image/buffer/sound    Sampler for input textures i, TODO

  public DwGLSLProgram shader;
  
  public boolean AUTO_UPDATE_UNIFORMS = true;

  long timer = System.currentTimeMillis();
  
  public DwShadertoy(DwPixelFlow context, String shader_filename){
    this.context = context;
    this.shader = context.createShader(shader_filename);
  }
  
  public void set_iResolution(float x, float y, float z){
    iResolution[0] = x;
    iResolution[1] = y;
    iResolution[2] = z;
  }
  public void set_iTime(float x){
    iTime = x;
  }
  public void set_iTime(){
    iTime = (System.currentTimeMillis() - timer) / 1000f;
  }
  public void set_iTimeDelta(float x){
    iTimeDelta = x;
  }
  public void set_iFrame(int x){
    iFrame = x;
  }
  public void set_iFrameRate(float x){
    iFrameRate = x;
  }
  public void set_iMouse(float x, float y, float z, float w){
    iMouse[0] = x;
    iMouse[1] = y;
    iMouse[2] = z;
    iMouse[3] = w;
  }
  public void set_iDate(float year, float month, float day, float time_in_seconds){
    iDate[0] = year;
    iDate[1] = month;
    iDate[2] = day;
    iDate[3] = time_in_seconds;
  }
  public void set_iDate(){
    LocalDateTime time = LocalDateTime.now();
    iDate[0] = time.getYear();
    iDate[1] = time.getMonthValue();
    iDate[2] = time.getDayOfMonth();
    iDate[3] = System.currentTimeMillis() / 1000;
  }
  public void set_iSampleRate(float x){
    iSampleRate = x;
  }
  
  
  public void set_iChannelTime(float x, float y, float z, float w){
    iChannelTime[0] = x;
    iChannelTime[1] = y;
    iChannelTime[2] = z;
    iChannelTime[3] = w;
  }
  
  public void set_iChannelResolution(int channel, float x, float y, float z){
    iChannelResolution[channel * 3 + 0] = x;
    iChannelResolution[channel * 3 + 1] = y;
    iChannelResolution[channel * 3 + 2] = z;
  }
  
  public void set_iChannel(int channel, DwGLTexture tex){
    if(tex != null){
      iChannel2D[channel] = tex;
      set_iChannelResolution(channel, tex.w, tex.h, 1.0f);
    }
  }
  public void set_iChannel(int channel, DwGLTexture3D tex){
    if(tex != null){
      iChannel3D[channel] = tex;
      set_iChannelResolution(channel, tex.w, tex.h, tex.d);
    }
  }
  
  
  
  public void updateUniforms(){
    PApplet papplet = context.papplet;
    set_iFrame    (papplet.frameCount);
    set_iTimeDelta(1f/papplet.frameRate);        
    set_iFrame    (papplet.frameCount);            
    set_iFrameRate(papplet.frameRate);        
    set_iMouse    (papplet.mouseX, papplet.mouseY, papplet.mouseX, papplet.mouseY);      
    set_iTime();      
    set_iDate();             
    set_iSampleRate(44100);   
  }
  
  
  public void setUniforms(){
    
    if(AUTO_UPDATE_UNIFORMS){
      updateUniforms();
    }
    
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
 
    if(iChannel2D[0] != null) shader.uniformTexture ("iChannel0", iChannel2D[0]);
    if(iChannel2D[1] != null) shader.uniformTexture ("iChannel1", iChannel2D[1]);
    if(iChannel2D[2] != null) shader.uniformTexture ("iChannel2", iChannel2D[2]);
    if(iChannel2D[3] != null) shader.uniformTexture ("iChannel3", iChannel2D[3]);
    
    if(iChannel3D[0] != null) shader.uniformTexture ("iChannel0", iChannel3D[0]);
    if(iChannel3D[1] != null) shader.uniformTexture ("iChannel1", iChannel3D[1]);
    if(iChannel3D[2] != null) shader.uniformTexture ("iChannel2", iChannel3D[2]);
    if(iChannel3D[3] != null) shader.uniformTexture ("iChannel3", iChannel3D[3]);
  }
  


  public void apply(DwGLTexture tex_dst){
    set_iResolution(tex_dst.w, tex_dst.h, 1f);    
    
    context.begin();
    context.beginDraw(tex_dst);
    shader.begin();
    setUniforms();
    shader.drawFullScreenQuad();
    shader.end();
    context.endDraw();
    context.end();
  }
  
  
  public void apply(PGraphicsOpenGL pg_dst){
    set_iResolution(pg_dst.width, pg_dst.height, 1f);    
    
    context.begin();
    context.beginDraw(pg_dst);
    shader.begin();
    setUniforms();
    shader.drawFullScreenQuad();
    shader.end();
    context.endDraw();
    context.end();
  }
  

  
  
}