/**
 * 
 * PixelFlow | Copyright (C]) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package ShaderToy_BasicMontecarlo;



import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;

public class ShaderToy_BasicMontecarlo extends PApplet {
  
  DwPixelFlow context;
  DwShadertoy toy_bufA;
  DwShadertoy toy_image;
  
  DwGLTexture tex0 = new DwGLTexture();
  PGraphics2D pg_canvas;
  
  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toy_bufA  = new DwShadertoy(context, "data/BasicMontecarlo_BufA.frag");
    toy_image = new DwShadertoy(context, "data/BasicMontecarlo_Image.frag");
    
    frameRate(60);
  }

  public void resizeScene(){
    if(pg_canvas == null || width != pg_canvas.width || height != pg_canvas.height){
      pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);

      tex0.resize(context, GL2.GL_RGBA32F, width, height, GL2.GL_RGBA, GL2.GL_FLOAT, GL2.GL_LINEAR, GL2.GL_CLAMP_TO_EDGE, 4, 4);
      
      toy_bufA.reset();
      toy_image.reset();
    }
  }
  

  public void draw() {
    
    resizeScene();
    
    toy_bufA.set_iChannel(0, tex0);
    toy_bufA.apply(tex0);
    
    toy_image.set_iChannel(0, tex0);
    toy_image.apply(pg_canvas);
    
    // put it on the screen
    blendMode(REPLACE);
    image(pg_canvas, 0, 0);
        
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  public static void main(String args[]) {
    PApplet.main(new String[] { ShaderToy_BasicMontecarlo.class.getName() });
  }
}