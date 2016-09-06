/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package src.filter;


import processing.opengl.PGraphics2D;
import processing.opengl.Texture;
import src.PixelFlow;
import src.dwgl.DwGLSLProgram;
import src.dwgl.DwGLTexture;

public class Multiply {
  
  public PixelFlow context;
  
  public Multiply(PixelFlow context){
    this.context = context;
  }
  
  public void apply(PGraphics2D src, PGraphics2D dst, float[] multiplier) {
    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
       
    dst.beginDraw();
    context.begin();
    apply(tex_src.glName, dst.width, dst.height, multiplier);
    context.end("Multiply.apply");
    dst.endDraw();
  }
  
  public void apply(PGraphics2D src, DwGLTexture dst, float[] multiplier) {
    Texture tex_src = src.getTexture();
    if(!tex_src.available()) 
      return;
       
    context.begin();
    context.beginDraw(dst);
    apply(tex_src.glName, dst.w, dst.h, multiplier);
    context.endDraw();
    context.end("Multiply.apply");
  }
  
  
  public void apply(DwGLTexture src, DwGLTexture dst, float[] multiplier) {
    context.begin();
    context.beginDraw(dst);
    apply(src.HANDLE[0], dst.w, dst.h, multiplier);
    context.endDraw();
    context.end("Multiply.apply");
  }
  
  DwGLSLProgram shader;
  public void apply(int tex_handle, int w, int h, float[] multiplier){
    if(shader == null) shader = context.createShader(PixelFlow.SHADER_DIR+"Filter/multiply.frag");
    shader.begin();
    shader.uniform2f     ("wh" , w, h);
    shader.uniformTexture("tex", tex_handle);
    shader.uniform4fv    ("multiplier", 1, multiplier);
    shader.drawFullScreenQuad(0, 0, w, h);
    shader.end();
  }
  
  
}
