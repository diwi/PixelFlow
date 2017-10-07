/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.dwgl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLES3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;

public class DwGLTexture3D{
  
  static private int TEX_COUNT = 0;

  public DwPixelFlow context;
  private GL2ES2 gl;

  public final int[] HANDLE = {0};

  // some default values. TODO
  public final int target     = GL2ES2.GL_TEXTURE_3D;
  public int internalFormat   = GL2ES2.GL_RGBA8;
  public int format           = GL2ES2.GL_RGBA;
  public int type             = GL2ES2.GL_UNSIGNED_BYTE;
  public int filter           = GL2ES2.GL_NEAREST;
  public int wrap             = GL2ES2.GL_CLAMP_TO_BORDER;
  public int num_channel      = 4;
  public int byte_per_channel = 1;
  
  // dimension
  public int w = 0; 
  public int h = 0;
  public int d = 0;

  // PixelBufferObject
  public final int[] HANDLE_pbo = {0};

  // Texture, for Subregion copies and data transfer
  public DwGLTexture3D texsub = null; // recursion!
  
 
  public DwGLTexture3D(){
  }
  
  public DwGLTexture3D createEmtpyCopy(){
    DwGLTexture3D tex = new DwGLTexture3D();
    tex.resize(context, this);
    return tex;
  }
 

  public void release(){
    
    // release texture
    if(isTexture()){
      gl.glDeleteTextures(1, HANDLE, 0);
      HANDLE[0] = 0;
      
      w = 0;
      h = 0;

      if(--TEX_COUNT < 0){
        TEX_COUNT = 0;
        System.out.println("ERROR: released to many textures"); 
      }
    }
    
    // release pbo
    if(hasPBO()){
      gl.glDeleteBuffers(1, HANDLE_pbo, 0);
      HANDLE_pbo[0] = 0;
    }

    // release subtexture
    if(texsub != null){
      texsub.release();
      texsub = null;
    }
    
  }

  public int w(){
    return w; 
  }
  public int h(){
    return h; 
  }
  public int d(){
    return d; 
  }
  
  public boolean isTexture(){
    return (gl != null) && (HANDLE[0] != 0);
  }
  
  public boolean isTexture2(){
    return (gl != null) && gl.glIsTexture(HANDLE[0]);
  }
  
  public boolean hasPBO(){
    return (gl != null) && (HANDLE_pbo[0] != 0);
  }

  
  public int getMem_Byte(){
    return w * h * d * byte_per_channel * num_channel;
  }
  public int getMem_KiloByte(){
    return getMem_Byte() >> 10;
  }
  public int getMem_MegaByte(){
    return getMem_Byte() >> 20;
  }
  
  public boolean resize(DwPixelFlow context, DwGLTexture3D othr){
    return resize(context, othr, othr.w, othr.h, othr.d);
  }
  
  public boolean resize(DwPixelFlow context, DwGLTexture3D othr, int w, int h, int d){
    return resize(context, 
        othr.internalFormat, 
        w, h, d, 
        othr.format, 
        othr.type, 
        othr.filter, 
        othr.wrap,
        othr.num_channel,
        othr.byte_per_channel);
  }
  
  public boolean resize(DwPixelFlow context, int w, int h, int d)
  {
    return resize(context, internalFormat, w, h, d, format, type, filter, wrap, num_channel, byte_per_channel, null);
  }
  
  public boolean resize(DwPixelFlow context, 
      int internalFormat,
      int w, int h, int d, 
      int format, int type, 
      int filter, 
      int num_channel, int byte_per_channel)
  {
    return resize(context, internalFormat, w, h, d, format, type, filter, wrap, num_channel, byte_per_channel, null);
  }
  
  public boolean resize(DwPixelFlow context, 
      int internalFormat, 
      int w, int h, int d, 
      int format, int type, 
      int filter, int wrap, 
      int num_channel, int byte_per_channel)
  {
    return resize(context, internalFormat, w, h, d, format, type, filter, wrap, num_channel, byte_per_channel, null);
  }
  
  public boolean resize(DwPixelFlow context, 
      int internalFormat, 
      int w, int h, int d, 
      int format, int type, 
      int filter, 
      int num_channel, int byte_per_channel, Buffer data)
  {
    return resize(context, internalFormat, w, h, d, format, type, filter, wrap, num_channel, byte_per_channel, data);
  }

  public boolean resize(DwPixelFlow context, 
      int internalFormat, 
      int w, int h, int d, 
      int format, int type, 
      int filter, int wrap, 
      int num_channel, int byte_per_channel, Buffer data)
  {

    this.context = context;
    this.gl = context.gl;
    
    if(w <= 0 || h <= 0 || d <= 0) return false;
    
    // figure out what needs to be done for this texture
    boolean B_ALLOC=false, B_RESIZE=false, B_FILTER=false, B_WRAP=false, B_BIND=false;
    
    B_ALLOC  |= !isTexture();
    
    B_RESIZE |= B_ALLOC;
    B_RESIZE |= this.w              != w;              // width
    B_RESIZE |= this.h              != h;              // height
    B_RESIZE |= this.d              != d;              // depth
    B_RESIZE |= this.internalFormat != internalFormat; // internalFormat 
    B_RESIZE |= this.format         != format;         // format 
    B_RESIZE |= this.type           != type;           // type
    
    B_FILTER |= B_ALLOC || B_RESIZE;
    B_FILTER |= this.filter != filter;
    
    B_WRAP   |= B_ALLOC || B_RESIZE;
    B_WRAP   |= this.wrap != wrap;
    
    B_BIND = B_ALLOC | B_RESIZE | B_FILTER | B_WRAP;
    
    
    // assign fields
    this.w                = w;
    this.h                = h;
    this.d                = d;
    this.internalFormat   = internalFormat;
    this.format           = format;
    this.type             = type;
    this.filter           = filter;
    this.wrap             = wrap;
    this.num_channel      = num_channel;
    this.byte_per_channel = byte_per_channel;
    
    
    if(B_ALLOC){
      gl.glGenTextures(1, HANDLE, 0);
      TEX_COUNT++;
    }
    
    if(B_RESIZE)
    {
      gl.glBindTexture(target, HANDLE[0]);
      
      gl.glBindTexture(target, HANDLE[0]);
//    gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_BASE_LEVEL, 0);
//    gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAX_LEVEL, 0);
      
      gl.glPixelStorei(GL2ES2.GL_UNPACK_ALIGNMENT, 1);
      gl.glPixelStorei(GL2ES2.GL_PACK_ALIGNMENT  , 1);
//    int[] val = new int[1];
//    gl.glGetIntegerv(GL2ES2.GL_UNPACK_ALIGNMENT, val, 0);
//    System.out.println("GL_UNPACK_ALIGNMENT "+val[0]);
//    gl.glGetIntegerv(GL2ES2.GL_PACK_ALIGNMENT, val, 0);
//    System.out.println("GL_PACK_ALIGNMENT "+val[0]);
      
      gl.glTexImage3D   (target, 0, internalFormat, w, h, d, 0, format, type, data);
//    gl.glTexSubImage2D(target, 0, 0, 0, w, h, format, type, data);
    }
      
    if(B_FILTER)
    {
      gl.glBindTexture(target, HANDLE[0]);
      gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MIN_FILTER, filter); // GL_NEAREST, GL_LINEAR
      gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_MAG_FILTER, filter);
    }

    if(B_WRAP)
    {
      gl.glBindTexture(target, HANDLE[0]);
      gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_R, wrap);
      gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_S, wrap);
      gl.glTexParameteri(target, GL2ES2.GL_TEXTURE_WRAP_T, wrap);
      gl.glTexParameterfv(target, GL2ES2.GL_TEXTURE_BORDER_COLOR, new float[]{0,0,0,0}, 0);
    }

    if(B_BIND)
    {
      gl.glBindTexture(target, 0);   
      context.errorCheck("DwGLTexture.resize tex");
    }

    return B_RESIZE;
  }
  
  
  
  /**
   * <pre>
   * GL_CLAMP_TO_EDGE
   * GL_CLAMP_TO_BORDER
   * GL_MIRRORED_REPEAT 
   * GL_REPEAT (default)
   * GL_MIRROR_CLAMP_TO_EDGE
   * </pre>
   * @param wrap
   */
  public void setParamWrap(int wrap){
    this.wrap = wrap;
    gl.glBindTexture  (target, HANDLE[0]);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_R, wrap);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_S, wrap);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_WRAP_T, wrap);
    gl.glBindTexture  (target, 0);
  }
  
  /**
   * <pre>
   * GL_CLAMP_TO_EDGE
   * GL_CLAMP_TO_BORDER
   * GL_MIRRORED_REPEAT 
   * GL_REPEAT (default)
   * GL_MIRROR_CLAMP_TO_EDGE
   * </pre>
   * @param wrap
   */
  public void setParamWrap(int wrap, float[] border_color){
    this.wrap = wrap;
    gl.glBindTexture   (target, HANDLE[0]);
    gl.glTexParameteri (target, GL2.GL_TEXTURE_WRAP_R, wrap);
    gl.glTexParameteri (target, GL2.GL_TEXTURE_WRAP_S, wrap);
    gl.glTexParameteri (target, GL2.GL_TEXTURE_WRAP_T, wrap);
    gl.glTexParameterfv(target, GL2.GL_TEXTURE_BORDER_COLOR, border_color, 0);
    gl.glBindTexture   (target, 0);
  }
  
  /**
   * <pre>
   * GL_TEXTURE_MAG_FILTER
   *  - GL_NEAREST
   *  - GL_LINEAR (default)
   *  
   * GL_TEXTURE_MIN_FILTER
   *  - GL_NEAREST ................... nearest texel
   *  - GL_LINEAR .................... linear  texel
   *  - GL_NEAREST_MIPMAP_NEAREST .... nearest texel, nearest mipmap (default)
   *  - GL_LINEAR_MIPMAP_NEAREST ..... linear  texel, nearest mipmap 
   *  - GL_NEAREST_MIPMAP_LINEAR ..... nearest texel, linear mipmap
   *  - GL_LINEAR_MIPMAP_LINEAR ...... linear  texel, linear mipmap
   * </pre>
   * 
   * @param minfilter
   * @param magfilter
   */
  public void setParamFilter(int filter){
    this.filter = filter;
    gl.glBindTexture  (target, HANDLE[0]);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_MIN_FILTER, filter);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_MAG_FILTER, filter);
    gl.glBindTexture  (target, 0);
  }
  
 
  /**
   * <pre>
   * GL_TEXTURE_MAG_FILTER
   *  - GL_NEAREST
   *  - GL_LINEAR (default)
   *  
   * GL_TEXTURE_MIN_FILTER
   *  - GL_NEAREST ................... nearest texel
   *  - GL_LINEAR .................... linear  texel
   *  - GL_NEAREST_MIPMAP_NEAREST .... nearest texel, nearest mipmap (default)
   *  - GL_LINEAR_MIPMAP_NEAREST ..... linear  texel, nearest mipmap 
   *  - GL_NEAREST_MIPMAP_LINEAR ..... nearest texel, linear mipmap
   *  - GL_LINEAR_MIPMAP_LINEAR ...... linear  texel, linear mipmap
   * </pre>
   * 
   * @param minfilter
   * @param magfilter
   */
  public void setParamFilter(int minfilter, int magfilter){
    gl.glBindTexture  (target, HANDLE[0]);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_MIN_FILTER, minfilter);
    gl.glTexParameteri(target, GL2.GL_TEXTURE_MAG_FILTER, magfilter);
    gl.glBindTexture  (target, 0);
  }
  

  public void generateMipMap(){
    gl.glBindTexture   (target, HANDLE[0]);
    gl.glTexParameteri (target, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
    gl.glGenerateMipmap(target);
    gl.glBindTexture   (target, 0);
  }
  
  public void setParam_Border(float[] border){
    gl.glBindTexture   (target, HANDLE[0]);
    gl.glTexParameterfv(target, GLES3.GL_TEXTURE_BORDER_COLOR, border, 0);
    gl.glBindTexture   (target, 0);
  }
  public void setParam_Border(int[] border){
    gl.glBindTexture    (target, HANDLE[0]);
    gl.glTexParameterIiv(target, GLES3.GL_TEXTURE_BORDER_COLOR, border, 0);
    gl.glBindTexture    (target, 0);
  }
  
  
  
  
  public void glTexParameteri(int param, int value, boolean bind){
    if(bind) bind();
    glTexParameteri(param, value);
    if(bind) unbind();
  }
  public void glTexParameteriv(int param, int[] value, boolean bind){
    if(bind) bind();
    glTexParameteriv(param, value);
    if(bind) unbind();
  }
  public void glTexParameterf(int param, float value, boolean bind){
    if(bind) bind();
    glTexParameterf(param, value);
    if(bind) unbind();
  }
  public void glTexParameterfv(int param, float[] value, boolean bind){
    if(bind) bind();
    glTexParameterfv(param, value);
    if(bind) unbind();
  }
  
  
  public void glTexParameteri(int param, int value){
    gl.glTexParameteri (target, param, value);
  }
  public void glTexParameteriv(int param, int[] value){
    gl.glTexParameteriv (target, param, value, 0);
  }
  public void glTexParameterf(int param, float value){
    gl.glTexParameterf (target, param, value);
  }
  public void glTexParameterfv(int param, float[] value){
    gl.glTexParameterfv(target, param, value, 0);
  }
  
  
  
  public void bind(){
    gl.glBindTexture(target, HANDLE[0]);
  }
  public void unbind(){
    gl.glBindTexture(target, 0);
  }
  
  
  public void swizzle(int[] i4_GL_TEXTURE_SWIZZLE_RGBA){
    glTexParameteriv(GL2.GL_TEXTURE_SWIZZLE_RGBA, i4_GL_TEXTURE_SWIZZLE_RGBA, true);
  }
  
  
  
  private DwGLTexture3D createTexSubImage(int x, int y, int w, int h){
    // create/resize texture from the size of the subregion
    if(texsub == null){
      texsub = new DwGLTexture3D();
    }
    
    if(x + w > this.w) { System.out.println("Error DwGLTexture.createTexSubImage: region-x is not within texture bounds"); }
    if(y + h > this.h) { System.out.println("Error DwGLTexture.createTexSubImage: region-y is not within texture bounds "); }
    
    texsub.resize(context, internalFormat, w, h, d, format, type, filter, num_channel, byte_per_channel);
    
    // copy the subregion to the texture
    context.beginDraw(this, 0);
    gl.glBindTexture(target, texsub.HANDLE[0]);
    gl.glCopyTexSubImage3D(target, 0, 0, 0, 0, x, y, w, h);
    gl.glBindTexture(target, 0);
    context.endDraw("DwGLTexture.createTexSubImage");
    return texsub;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  /**
   *  GPU_DATA_READ == 0 ... a lot faster for the full texture.<br>
   *                         only slightly slower for single texels.<br>
   *  <br>                      
   *  GPU_DATA_READ == 1 ... very slow for the full texture.<br>
   *                         takes twice as long as "getData_GL2GL3()".<br>
   *  <br>                                         
   */
  public static int GPU_DATA_READ = 0;
  
  
  
  // TODO: get data
  
  /**
   * 
   * @param cx
   * @param cy
   * @param canvas_w
   * @param h
   * @param data array, for the returned opengl-texture-data
   * @return
   */
  public ByteBuffer getData_GL2ES3(){
    return getData_GL2ES3(0,0,w,h);
  }
  
  public ByteBuffer getData_GL2ES3(int x, int y, int w, int h){
    int data_len = w * h * num_channel;
    int buffer_size = data_len * byte_per_channel;
  
    context.beginDraw(this, 0);
    if(HANDLE_pbo[0] == 0){
      gl.glGenBuffers(1, HANDLE_pbo, 0);
    }
    gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, HANDLE_pbo[0]);
    gl.glBufferData(GL2ES3.GL_PIXEL_PACK_BUFFER, buffer_size, null, GL2ES3.GL_DYNAMIC_READ);
    gl.glReadPixels(x, y, w, h, format, type, 0);
    
    ByteBuffer bbuffer = gl.glMapBufferRange(GL2ES3.GL_PIXEL_PACK_BUFFER, 0, buffer_size, GL2ES3.GL_MAP_READ_BIT);
//    ByteBuffer bbuffer = gl.glMapBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, GL2ES3.GL_READ_ONLY);
    
    gl.glUnmapBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER);
    gl.glBindBuffer(GL2ES3.GL_PIXEL_PACK_BUFFER, 0);
    context.endDraw();
    
    DwGLError.debug(gl, "DwGLTexture.getData_GL2ES3");
    return bbuffer;
  }
  

  

  
  
  
  
  
  
  public void getData_GL2GL3(int x, int y, int w, int h, Buffer buffer){
    DwGLTexture3D tex = this;
    
    // create a new texture, the size of the given region, and copy the pixels to it
    if(!(x == 0 && y == 0 && w == this.w && h == this.h)){
      tex = createTexSubImage(x,y,w,h);
    }

    // transfer pixels from the sub-region texture to the host application
    tex.getData_GL2GL3(buffer);
  }

  // copy texture-data to given float array
  public void getData_GL2GL3(Buffer buffer){
    int data_len = w * h * num_channel;
    
    if(buffer.remaining() < data_len){
      System.out.println("ERROR DwGLTexture.getData_GL2GL3: buffer to small: "+buffer.capacity() +" < "+data_len);
      return;
    }

    GL2GL3 gl23 = gl.getGL2GL3();
    gl23.glBindTexture(target, HANDLE[0]);
    gl23.glGetTexImage(target, 0, format, type, buffer);
    gl23.glBindTexture(target, 0);
    
    DwGLError.debug(gl, "DwGLTexture.getData_GL2GL3");
  }
  
  
  
  
  
  
  

  
  
  
  
  
  
  
  ////////////////////Texture Data Transfer - Integer //////////////////////////
  
  public int[] getIntegerTextureData(int[] data){
    return getIntegerTextureData(data, 0, 0, w, h, 0);
  }
  
  public int[] getIntegerTextureData(int[] data, int x, int y, int w, int h){
    return getIntegerTextureData(data, x, y, w, h, 0);
  }
  
  public int[] getIntegerTextureData(int[] data, int x, int y, int w, int h, int data_off){
    int data_len = w * h * num_channel;
    data = realloc(data, data_off + data_len);
    if(GPU_DATA_READ == 0){
      getData_GL2GL3(x, y, w, h, IntBuffer.wrap(data).position(data_off));
    } else if(GPU_DATA_READ == 1){
      getData_GL2ES3(x, y, w, h).asIntBuffer().get(data, data_off, data_len);
    }
    return data; 
  }
  
  
  //////////////////// Texture Data Transfer - Float ///////////////////////////

  public float[] getFloatTextureData(float[] data){
    return getFloatTextureData(data, 0, 0, w, h, 0);
  }
  
  public float[] getFloatTextureData(float[] data, int x, int y, int w, int h){
    return getFloatTextureData(data, x, y, w, h, 0);
  }
  
  public float[] getFloatTextureData(float[] data, int x, int y, int w, int h, int data_off){
    int data_len = w * h * num_channel;
    data = realloc(data, data_off + data_len);
    if(GPU_DATA_READ == 0){
      getData_GL2GL3(x, y, w, h, FloatBuffer.wrap(data).position(data_off));
    } else if(GPU_DATA_READ == 1){
      getData_GL2ES3(x, y, w, h).asFloatBuffer().get(data, data_off, data_len);
    }
    return data; 
  }
  
  
  
  //////////////////// Texture Data Transfer - Byte ///////////////////////////
  
  /**
   * 
   *  byte[] px_byte = Fluid.getByteTextureData(Fluid.tex_obstacleC.src, null);            
   *  PGraphics2D pg_tmp = (PGraphics2D) createGraphics(Fluid.fluid_w, Fluid.fluid_h, P2D);
   *  pg_tmp.loadPixels();                                                                     
   *  for(int i = 0; i < pg_tmp.pixels.length; i++){                                           
   *    int O = (int)(px_byte[i]);                                                             
   *    pg_tmp.pixels[i] = O << 24 | O << 16 | O << 8 | O;                                     
   *  }                                                                                        
   *  pg_tmp.updatePixels();                                                                   
   * 
   * 
   * @param tex_min
   * @param data
   * @return
   */
  
  public byte[] getByteTextureData(byte[] data){
    return getByteTextureData(data, 0, 0, w, h, 0);
  }
  
  public byte[] getByteTextureData(byte[] data, int x, int y, int w, int h){
    return getByteTextureData(data, x, y, w, h, 0);
  }
  
  public byte[] getByteTextureData(byte[] data, int x, int y, int w, int h, int data_off){
    int data_len = w * h * num_channel;
    data = realloc(data, data_off + data_len);
    if(GPU_DATA_READ == 0){
      getData_GL2GL3(x, y, w, h, ByteBuffer.wrap(data).position(data_off));
    } else if(GPU_DATA_READ == 1){
      getData_GL2ES3(x, y, w, h).get(data, data_off, data_len);
    }
    return data; 
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  static private final float[] realloc(float[] data, int size){
    if(data == null || data.length < size){
      float[] data_new = new float[size];
      if(data != null){
        System.arraycopy(data, 0, data_new, 0, data.length);
      }
      data = data_new;
    }
    return data;
  }
  
  static private final int[] realloc(int[] data, int size){
    if(data == null || data.length < size){
      int[] data_new = new int[size];
      if(data != null){
        System.arraycopy(data, 0, data_new, 0, data.length);
      }
      data = data_new;
    }
    return data;
  }
  
  
  static private final byte[] realloc(byte[] data, int size){
    if(data == null || data.length < size){
      byte[] data_new = new byte[size];
      if(data != null){
        System.arraycopy(data, 0, data_new, 0, data.length);
      }
      data = data_new;
    }
    return data;
  }
  
  
  
  
  
  
  
  
  
  
// not tested
//  public boolean setData(Buffer data, int offset_x, int offset_y, int size_x, int size_y){
//    if( offset_x + size_x > this.w ) return false;
//    if( offset_y + size_y > this.h ) return false;
//    
//    gl.glBindTexture  (target, HANDLE[0]);
//    gl.glTexSubImage2D(target, 0, offset_x, offset_y, size_x, size_y, format, type, data);
//    gl.glBindTexture  (target, 0);
//    
//    return true;
//  }
//  
//  public boolean setData(Buffer data){
//    return setData(data, 0, 0, w, h);
//  }   

  
  public void clear(float v){
    clear(v,v,v,v);
  }
  
  DwGLTexture3D[] layers_tex = new DwGLTexture3D[1];
  int[]           layers_idx = new int[1];
  
  public void clear(float r, float g, float b, float a){
    layers_tex[0] = this;
    for(int i = 0; i < d; i++){
      layers_idx[0] = i;
      context.framebuffer.clearTexture(r,g,b,a, layers_tex, layers_idx);
    }
  }
  
//  public void beginDraw(){
//    framebuffer.bind(this);
//    gl.glViewport(0, 0, w, h);
//    
//    // default settings
//    gl.glColorMask(true, true, true, true);
//    gl.glDepthMask(false);
//    gl.glDisable(GL.GL_DEPTH_TEST);
//    gl.glDisable(GL.GL_STENCIL_TEST);
//    gl.glDisable(GL.GL_BLEND);
//    //  gl.glClearColor(0, 0, 0, 0);
//    //  gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);
//  }
//  public void endDraw(){
//    framebuffer.unbind();
//  }


  static public class TexturePingPong{
    public DwGLTexture3D src = new DwGLTexture3D(); 
    public DwGLTexture3D dst = new DwGLTexture3D(); 

    public TexturePingPong(){
    }

    
    public boolean resize(DwPixelFlow context, int internalFormat, int w, int h, int d, int format, int type, int filter, int wrap, int  num_channel, int byte_per_channel){
      boolean resized = false;
      resized |= src.resize(context, internalFormat, w, h, d, format, type, filter, wrap, num_channel, byte_per_channel);
      resized |= dst.resize(context, internalFormat, w, h, d, format, type, filter, wrap, num_channel, byte_per_channel);
      return resized;
    }

    public boolean resize(DwPixelFlow context, int internalFormat, int w, int h, int d, int format, int type, int filter, int  num_channel, int byte_per_channel){
      boolean resized = false;
      resized |= src.resize(context, internalFormat, w, h, d, format, type, filter, num_channel, byte_per_channel);
      resized |= dst.resize(context, internalFormat, w, h, d, format, type, filter, num_channel, byte_per_channel);
      return resized;
    }
    
    
//    public void resize(DwPixelFlow context, int internalFormat, int w, int h, int d, int format, int type, int filter, int  num_channel, int byte_per_channel){
//      src.resize(context, internalFormat, w, h, d, format, type, filter, num_channel, byte_per_channel);
//      dst.resize(context, internalFormat, w, h, d, format, type, filter, num_channel, byte_per_channel);
//    }

    public void release(){
      if(src != null){ src.release(); }
      if(dst != null){ dst.release(); }
    }

    public void swap(){
      DwGLTexture3D tmp;
      tmp = src;
      src = dst;
      dst = tmp;
    }
    
    public void clear(float v){
      src.clear(v);
      dst.clear(v);
    }
    public void clear(float r, float g, float b, float a){
      src.clear(r,g,b,a);
      dst.clear(r,g,b,a);
    }
    
  }


}