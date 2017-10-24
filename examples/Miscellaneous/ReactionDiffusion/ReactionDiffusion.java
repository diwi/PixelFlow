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


package Miscellaneous.ReactionDiffusion;



import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;



public class ReactionDiffusion extends PApplet {
  
  //
  // reaction-diffusion, grayscott
  //
  // using custom shaders/textures for fast multipass rendering.
  //
  
  DwGLSLProgram shader_grayscott;
  DwGLSLProgram shader_render;
  
  // multipass rendering texture
  DwGLTexture.TexturePingPong tex_grayscott = new DwGLTexture.TexturePingPong();
  
  // final render target for display
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
    context.print();
    context.printGL();

    // 1) 32 bit per channel
    tex_grayscott.resize(context, GL2.GL_RG32F, width, height, GL2.GL_RG, GL2.GL_FLOAT, GL2.GL_NEAREST, 2, 4);
    
    // 2) 16 bit per channel, lack of precision is obvious in the result, its fast though
//    tex_grayscott.resize(context, GL2.GL_RG16F, width, height, GL2.GL_RG, GL2.GL_FLOAT, GL2.GL_NEAREST, 2, 2);
     
    // 3) 16 bit per channel, better than 2)
//    tex_grayscott.resize(context, GL2.GL_RG16_SNORM, width, height, GL2.GL_RG, GL2.GL_FLOAT, GL2.GL_NEAREST, 2, 2);
    

    // glsl shader
    shader_grayscott = context.createShader("data/grayscott.frag");
    shader_render    = context.createShader("data/render.frag");

    // init
    tex_render = (PGraphics2D) createGraphics(width, height, P2D);
    tex_render.smooth(0);
    tex_render.beginDraw();
    tex_render.textureSampling(2);
    tex_render.blendMode(REPLACE);
    tex_render.clear();
    tex_render.noStroke();
    tex_render.background(0x00FF0000);
    tex_render.fill      (0x0000FF00);
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

    // create display texture
    context.beginDraw(tex_render);
    shader_render.begin();
    shader_render.uniform2f     ("wh_rcp", 1f/width, 1f/height);
    shader_render.uniformTexture("tex"   , tex_grayscott.src);
    shader_render.drawFullScreenQuad();
    shader_render.end();
    context.endDraw("render()"); 
    context.end();
    

    // put it on the screen
    blendMode(REPLACE);
    image(tex_render, 0, 0);
        
    if(frameCount == 1000) saveFrame("data/grayscott1.jpg");

    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, pass, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { ReactionDiffusion.class.getName() });
  }
}