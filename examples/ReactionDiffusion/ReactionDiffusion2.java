/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package ReactionDiffusion;



import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;



public class ReactionDiffusion2 extends PApplet {
  

  
  DwGLSLProgram shader_grayscott;
  DwGLSLProgram shader_render;
  
  DwGLTexture.TexturePingPong tex_grayscott = new DwGLTexture.TexturePingPong();
  PGraphics2D tex_render;
 
  DwPixelFlow context;
  int pass = 0;
  
  public void settings() {
    size(800, 800, P2D);
    smooth(0);
  }
  
  public void setup() {
    
    // pixelflow context
    context = new DwPixelFlow(this);

    // multipass rendering texture
    tex_grayscott.resize(context, GL2.GL_RGBA8, width, height, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, 4, 1);
    
    // glsl shader
    shader_grayscott = context.createShader("data/grayscott2.frag");
    shader_render    = context.createShader("data/render2.frag");
        
    // init
    tex_render = (PGraphics2D) createGraphics(width, height, P2D);
    tex_render.smooth(0);
    tex_render.beginDraw();
    tex_render.textureSampling(2);
    tex_render.blendMode(REPLACE);
    tex_render.clear();
    tex_render.noStroke();
    tex_render.background(0xFFFF0000);
    tex_render.fill(0x0000FFFF);
    tex_render.noStroke();
    tex_render.rectMode(CENTER);
    tex_render.rect(width/2, height/2, 20, 20);
    tex_render.endDraw();

    // copy initial data to source texture
    DwFilter.get(context).copy.apply(tex_render, tex_grayscott.src);

    frameRate(1000);
  }


  public void reactionDiffusionPass(){
    context.beginDraw(tex_grayscott.dst);
    shader_grayscott.begin();
    shader_grayscott.uniform1f     ("dA"    , 1.0f  );
    shader_grayscott.uniform1f     ("dB"    , 0.5f  );
    shader_grayscott.uniform1f     ("feed"  , 0.055f);
    shader_grayscott.uniform1f     ("kill"  , 0.062f);
    shader_grayscott.uniform1f     ("dt"    , 1f    );
    shader_grayscott.uniform2f     ("wh_rcp", 1f/width, 1f/height);
    shader_grayscott.uniformTexture("tex"   , tex_grayscott.src);
    shader_grayscott.drawFullScreenQuad();
    shader_grayscott.end();
    context.endDraw("reactionDiffusionPass()"); 
    tex_grayscott.swap();
    pass++;
  }
  
  public void draw() {
    
    // multipass rendering, ping-pong 
    context.begin();
    for(int i = 0; i < 100; i++){
      reactionDiffusionPass();
    }
    context.end();

    // display result
    context.begin();
    context.beginDraw(tex_render);
    shader_render.begin();
    shader_render.uniform2f     ("wh_rcp", 1f/width, 1f/height);
    shader_render.uniformTexture("tex"   , tex_grayscott.src);
    shader_render.drawFullScreenQuad();
    shader_render.end();
    context.endDraw(); 
    context.end("render()");
    
    blendMode(REPLACE);
    image(tex_render, 0, 0);
    
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, pass, frameRate);
    surface.setTitle(txt_fps);
  }
  
  public void keyReleased(){
    if(key == 's') saveFrame("grayscott2.jpg");
  }
  
  

  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { ReactionDiffusion2.class.getName() });
  }
}