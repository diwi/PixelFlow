/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package com.thomasdiewald.pixelflow.java.dwgl;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

import com.jogamp.opengl.GL2ES2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

public class DwGLSLShader{
  
  public DwPixelFlow context;
  public GL2ES2 gl;
  public int type;
  public int HANDLE;
  public String type_str;
  public String path;
  public String[] content;

  
  //  vertex shader (fullscreenquad) will be generated automatically.
  public DwGLSLShader(DwPixelFlow context, int type){

    this.context = context;
    this.gl = context.gl;
    if(type != GL2ES2.GL_VERTEX_SHADER){
      System.out.println("ERROR: DwGLSLShader is not of type \"GL_VERTEX_SHADER\"");
    }

    this.type = type;
    this.path = "fullscreenquad.vert";

    content = new String[]
        {
           " "
          ,"#version 150"
          ,""
          ,"precision mediump float;"
          ,"precision mediump int;"
          ,""                                     
          ,"void main(){"                         
          ,"  int x = ((gl_VertexID<<1) & 2) - 1;"
          ,"  int y = ((gl_VertexID   ) & 2) - 1;"
          ,"  gl_Position = vec4(x,y,0,1);"
          ,"}"
          ," "
        };
    
    
    for(int i = 0; i < content.length; i++){
      content[i] += DwUtils.NL;
    }
    build();
  }

  public DwGLSLShader(DwPixelFlow context, int type, String path){
    this.context = context;
    this.gl = context.gl;
    this.type = type;
    this.path = path;
    
    this.content = loadSource(path);
    
    build();
  }
  
  
  
  public String[] loadSource(String path){
    ArrayList<String> source = new ArrayList<String>();
    
    loadSource(0, source, new File(path));
    
    String[] content = new String[source.size()];
    source.toArray(content);
    return content;
  }
  
  public void loadSource(int depth, ArrayList<String> source, File file){
//    System.out.println("parsing file: "+file);
    String[] lines = context.utils.readASCIIfile(file.getPath());
    
    if(depth++ > 5){
      throw new StackOverflowError("recursive #include: "+file);
    }
    
    File file_dir = file.getParentFile();
    
    for(int i = 0; i < lines.length; i++){
      String line = lines[i];
      String line_trim = line.trim();
      if(line_trim.startsWith("#include")){
        String include_file = line_trim.substring("#include".length()).replace("\"", "").trim();
        File file_to_include = new File(file_dir, include_file);
        loadSource(depth, source, file_to_include);
      } else {
        source.add(line + DwUtils.NL);
      }
    }
  }
  
  
  


  public void release(){
    gl.glDeleteShader(HANDLE); HANDLE = 0;
  }


  public void build() {
    release(); // clear anything, in case the program gets rebuild

    HANDLE  = gl.glCreateShader(type);
    gl.glShaderSource(HANDLE, content.length, content, (int[]) null, 0);
    gl.glCompileShader(HANDLE);
    DwGLSLShader.getShaderInfoLog (gl, HANDLE, type_str+" ("+path+")");

    DwGLError.debug(gl, "DwGLSLShader.build");
  }


  public void printShader(){
    System.out.println("");
    System.out.println(type_str+": "+path);
    for(int i = 0; i < content.length; i++){
      System.out.printf("[%3d]  %s", i, content[i]);
    }
    System.out.println("");
  }

  public void printCompiledShader(){
    getShaderSource(gl, HANDLE);
  }


  public static void getShaderInfoLog(GL2ES2 gl, int shader_id, String info) {
    if(shader_id==-1) return;

    IntBuffer log_len = IntBuffer.allocate(1);
    gl.glGetShaderiv(shader_id, GL2ES2.GL_INFO_LOG_LENGTH, log_len);

    ByteBuffer buffer = ByteBuffer.allocate(log_len.get(0));
    gl.glGetShaderInfoLog(shader_id, log_len.get(0), null, buffer);

    String log = Charset.forName("US-ASCII").decode(buffer).toString();

    if( log.length() > 1 && log.charAt(0) != 0){
      System.out.println(info);
      System.out.println(log);
    }
  }


  public static void getShaderSource(GL2ES2 gl, int shader_id){
    if(shader_id==-1) return;

    IntBuffer log_len = IntBuffer.allocate(1);
    gl.glGetShaderiv(shader_id, GL2ES2.GL_SHADER_SOURCE_LENGTH, log_len);

    ByteBuffer buffer = ByteBuffer.allocate(log_len.get(0));
    gl.glGetShaderSource(shader_id, log_len.get(0), null, buffer);

    String log = Charset.forName("US-ASCII").decode(buffer).toString();

    if( log.length() > 1 && log.charAt(0) != 0){
      System.out.println(log);
    }
  }
  




}
