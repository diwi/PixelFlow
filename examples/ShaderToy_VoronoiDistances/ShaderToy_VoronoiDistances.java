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


package ShaderToy_VoronoiDistances;



import java.nio.ByteBuffer;

import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;


public class ShaderToy_VoronoiDistances extends PApplet {

  DwPixelFlow context;
  DwShadertoy toy;
  PGraphics2D pg_canvas;
  DwGLTexture tex0 = new DwGLTexture();

  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toy = new DwShadertoy(context, "data/VoronoiDistances_Image.frag");
    
    // create noise texture
    byte[] bdata = new byte[256 * 256 * 4];
    ByteBuffer bbuffer = ByteBuffer.wrap(bdata);
    for(int i = 0; i < bdata.length; i++){
      bdata[i] = (byte) random(0, 256);
    }
    tex0.resize(context, GL2.GL_RGBA8, 256, 256, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, GL2.GL_REPEAT, 4, 1, bbuffer);
    
    frameRate(60);
  }

  public void resizeScene(){
    if(pg_canvas == null || width != pg_canvas.width || height != pg_canvas.height){
      pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
      toy.reset();
    }
  }
  
  public void draw() {
    resizeScene();

    toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    toy.set_iChannel(0, tex0);
    toy.apply(pg_canvas);
    
    blendMode(REPLACE);
    image(pg_canvas, 0, 0);
        
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { ShaderToy_VoronoiDistances.class.getName() });
  }
}