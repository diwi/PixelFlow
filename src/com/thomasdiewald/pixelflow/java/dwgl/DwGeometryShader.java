package com.thomasdiewald.pixelflow.java.dwgl;

import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLShader;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.core.PApplet;
import processing.opengl.PJOGL;
import processing.opengl.PShader;

public class DwGeometryShader extends PShader {
  
  public int glGeometry;
  public String geometrySource;
  public String[] src_geom;
  public String filename_geom;

  public DwGeometryShader(PApplet papplet, String filename_vert, String filename_geom, String filename_frag) {
    super(papplet, filename_vert, filename_frag);
    
    this.filename_geom = filename_geom;
    
    this.src_geom = papplet.loadStrings(filename_geom);
    for(int i = 0; i < src_geom.length; i++){
      src_geom[i] += DwUtils.NL;
    }
  }
  
  
  public DwGeometryShader(PApplet papplet, String[] src_vert, String[] src_geom, String[] src_frag) {
    super(papplet, src_vert, src_frag);
    this.src_geom = src_geom;
    
    for(int i = 0; i < src_geom.length; i++){
      src_geom[i] += DwUtils.NL;
    }
  }

  
  @Override
  protected void setup(){
    PJOGL pjogl = (PJOGL) pgl;
    GL3 gl = pjogl.gl.getGL3();
    
    glGeometry = gl.glCreateShader(GL3.GL_GEOMETRY_SHADER);
    gl.glShaderSource(glGeometry, src_geom.length, src_geom, (int[]) null, 0);
    gl.glCompileShader(glGeometry);
    DwGLSLShader.getShaderInfoLog(gl, glGeometry, GL3.GL_GEOMETRY_SHADER+" ("+filename_geom+")");

    pgl.attachShader(glProgram, glGeometry);
  }
}