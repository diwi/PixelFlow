/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package com.thomasdiewald.pixelflow.java.dwgl;


import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;

public class DwGLSLProgram {
  
  public DwPixelFlow context;
  public GL2ES2 gl;
  public int HANDLE;

  public DwGLSLShader vert;
  public DwGLSLShader frag;
  
  public String name;
  
  //  if vert_path is null, the shader (fullscreenquad) will be generated automatically.
  public DwGLSLProgram(DwPixelFlow context, String vert_path, String frag_path) {
    if(vert_path != null) {
      this.vert = new DwGLSLShader(context, GL2ES2.GL_VERTEX_SHADER, vert_path);
    } else {
      this.vert = new DwGLSLShader(context, GL2ES2.GL_VERTEX_SHADER);
      vert_path = this.vert.path;
    }
    
    this.frag = new DwGLSLShader(context, GL2ES2.GL_FRAGMENT_SHADER, frag_path);
    this.name = vert_path+"/"+vert_path;
    this.gl = context.gl;
    this.context = context;
    build();
  }

  public void release(){
    vert.release();
    frag.release();
    gl.glDeleteProgram(HANDLE); HANDLE = 0;
  }
  
  public DwGLSLProgram build() {

    gl.glDeleteProgram(HANDLE); HANDLE = 0;

    HANDLE = gl.glCreateProgram();
    gl.glAttachShader(HANDLE, vert.HANDLE);
    gl.glAttachShader(HANDLE, frag.HANDLE);
    gl.glLinkProgram(HANDLE);
    
//    gl.glValidateProgram(HANDLE);
    DwGLSLProgram.getProgramValidateStatus(gl, HANDLE);
    DwGLSLProgram.getProgramInfoLog(gl, HANDLE, ">> PROGRAM_INFOLOG: "+vert+" / "+frag+":\n");

    DwGLError.debug(gl, "DwGLSLProgram.build");
    return this;
  }


  // Query information
  public static void getProgramInfoLog(GL2ES2 gl, int program_id, String info) {
    if(program_id==-1) return;
    
    IntBuffer log_len = IntBuffer.allocate(1);
    gl.glGetProgramiv(program_id, GL2ES2.GL_INFO_LOG_LENGTH, log_len);

    ByteBuffer buffer = ByteBuffer.allocate(log_len.get(0));
    gl.glGetProgramInfoLog(program_id, log_len.get(0), null, buffer);
    
//    buffer.put(log_len.get(0)-1, (byte) ' ');
    String log = Charset.forName("US-ASCII").decode(buffer).toString();
    
    if( log.length() > 1 && log.charAt(0) != 0){
      System.out.println(info);
      System.out.println(log);
    }
  }
  

  public static void getProgramValidateStatus(GL2ES2 gl, int program_id) {
    if(program_id==-1) return;
    
    IntBuffer log_len = IntBuffer.allocate(1);
    gl.glGetProgramiv(program_id, GL2ES2.GL_VALIDATE_STATUS, log_len);
    
    ByteBuffer buffer = ByteBuffer.allocate(log_len.get(0));
    gl.glGetProgramInfoLog(program_id, log_len.get(0), null, buffer);
    
    String log = Charset.forName("US-ASCII").decode(buffer).toString();
    
    if( log.length() > 1 && log.charAt(0) != 0){
      System.out.println(log);
    }
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
//  boolean IS_ACTIVE = false;
  
  // Comfort Methods
  public DwGLSLProgram begin(){
    gl.glUseProgram(HANDLE);
//    IS_ACTIVE = true;
    return this;
  }
  
  public void end(){
    clearUniformTextures();
//    IS_ACTIVE = false; 
    gl.glUseProgram(0);
  }
  
  
  
  

  
  public boolean LOG_WARNINGS = true;
  
  private int getUniformLocation(String uniform_name){
    int LOC_name = gl.glGetUniformLocation(HANDLE, uniform_name);
    if(LOC_name == -1){
      if(LOG_WARNINGS){
        System.out.println(name+": uniform location \""+uniform_name+"\" = -1");
      }
    }
    return LOC_name;
  }
  
 
  private int active_uniform_location = -1;
  
  
  public int uniformTexture(String uniform_name, DwGLTexture texture){
    return uniformTexture(uniform_name, texture.HANDLE[0]);
  }
  
  public int uniformTexture(String uniform_name, int HANDLE_tex){
    int active_uniform_location_cur = -1;
    int LOC_name = getUniformLocation(uniform_name);
    if(LOC_name == -1){
      if(LOG_WARNINGS){
        System.out.println(name+": uniform location \""+uniform_name+"\" = -1");
      }
    } else {
      active_uniform_location_cur = ++active_uniform_location;
      gl.glUniform1i(LOC_name, active_uniform_location_cur); 
      gl.glActiveTexture(GL2ES2.GL_TEXTURE0 + active_uniform_location_cur); 
      gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, HANDLE_tex);
    }
    return active_uniform_location_cur;
  }
  
  public void clearUniformTextures(){
    while(active_uniform_location >= 0){
      gl.glActiveTexture(GL2ES2.GL_TEXTURE0 + active_uniform_location); 
      gl.glBindTexture(GL2ES2.GL_TEXTURE_2D, 0);
      --active_uniform_location;
    }
  }
  
  
  public void uniform1fv(String uniform_name, int count, float[] vec1){
    gl.glUniform1fv(getUniformLocation(uniform_name), count, vec1, 0);
  }
  public void uniform2fv(String uniform_name, int count, float[] vec2){
    gl.glUniform2fv(getUniformLocation(uniform_name), count, vec2, 0);
  }
  public void uniform3fv(String uniform_name, int count, float[] vec3){
    gl.glUniform3fv(getUniformLocation(uniform_name), count, vec3, 0);
  }
  public void uniform4fv(String uniform_name, int count, float[] vec4){
    gl.glUniform4fv(getUniformLocation(uniform_name), count, vec4, 0);
  }
  
  
  public void uniform1f(String uniform_name, float v0){
    gl.glUniform1f(getUniformLocation(uniform_name), v0);
  }
  public void uniform2f(String uniform_name, float v0, float v1){
    gl.glUniform2f(getUniformLocation(uniform_name), v0, v1);
  }
  public void uniform3f(String uniform_name, float v0, float v1, float v2){
    gl.glUniform3f(getUniformLocation(uniform_name), v0, v1, v2);
  }
  public void uniform4f(String uniform_name, float v0, float v1, float v2, float v3){
    gl.glUniform4f(getUniformLocation(uniform_name), v0, v1, v2, v3);
  }
  
  
  public void uniform1i(String uniform_name, int v0){
    gl.glUniform1i(getUniformLocation(uniform_name), v0);
  }
  public void uniform2i(String uniform_name, int v0, int v1){
    gl.glUniform2i(getUniformLocation(uniform_name), v0, v1);
  }
  public void uniform3i(String uniform_name, int v0, int v1, int v2){
    gl.glUniform3i(getUniformLocation(uniform_name), v0, v1, v2);
  }
  public void uniform4i(String uniform_name, int v0, int v1, int v2, int v3){
    gl.glUniform4i(getUniformLocation(uniform_name), v0, v1, v2, v3);
  }
  
  
  
  public void drawFullScreenQuad(int x, int y, int w, int h){
    gl.glViewport(x, y, w, h);
    gl.glDrawArrays(GL2ES2.GL_TRIANGLE_STRIP, 0, 4);
  }
  public void drawFullScreenQuad(){
    gl.glDrawArrays(GL2ES2.GL_TRIANGLE_STRIP, 0, 4);
  }
  
  public void drawFullScreenLines(int x, int y, int w, int h, int num_lines, float line_width){
    gl.glViewport(x, y, w, h);
    gl.glEnable(GL.GL_LINE_SMOOTH );
//    gl.glEnable(GL.GL_BLEND);
//    gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
    gl.glLineWidth(line_width);
    gl.glDrawArrays(GL2ES2.GL_LINES, 0, num_lines * 2);
  }
  public void drawFullScreenPoints(int x, int y, int w, int h, int num_points){
    gl.glViewport(x, y, w, h);
//    gl.glDisable(GL.GL_BLEND);
    
//    gl.glEnable(GL.GL_BLEND);
//    gl.glBlendFunc( GL.GL_SRC_ALPHA, GL.GL_SRC_ALPHA );
//    gl.glBlendEquation(GL.GL_FUNC_ADD);
    
    gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);
    gl.glEnable(GL3.GL_VERTEX_PROGRAM_POINT_SIZE);
    gl.glEnable(GL2ES1.GL_POINT_SPRITE);
    
    gl.glDrawArrays(GL2ES2.GL_POINTS, 0, num_points);
  }
  
  
  
  
  
  
  
  

  
}