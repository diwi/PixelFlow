/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package MinMax_Demo;



import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.MinMaxGlobal;
import processing.core.PApplet;
import processing.opengl.PGraphics2D;


public class MinMax_Demo extends PApplet {
  
  
  //
  //
  // Demo to show the usage of the Min/Max filter.
  //
  // The Filter computes the global Min/Max values of a given texture.
  //
  //
  
  DwPixelFlow context;
  
  // Min-Max Filter
  MinMaxGlobal minmax;
  
  // temp texture, to copy the PGraphics
  DwGLTexture tex = new DwGLTexture();
  
  PGraphics2D pg;

  public void settings() {
    size(1211, 799, P2D);
    smooth(0);
  }
  
  public void setup() {
    
    context = new DwPixelFlow(this);
    
    minmax = new MinMaxGlobal(context);
    tex.resize(context, GL2.GL_RGBA8, width, height, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, 4, 1);

    pg = (PGraphics2D) createGraphics(width, height, P2D);
    pg.smooth(0);

    frameRate(1000);
  }

  
  public void draw(){
    
    // set a single pixel value
    {
      int cx = (int) map(mouseX, 0, width , 0, 255);
      int cy = (int) map(mouseY, 0, height, 0, 255);
      
      pg.beginDraw();
      pg.blendMode(REPLACE);
      pg.background(128, 128);
      pg.noStroke();
      pg.fill(cx,cx,cy,cy);
      pg.rect(mouseX, mouseY, 1, 1);
      pg.endDraw();
    }
    
    // copy to native OpenGL tex
    DwFilter.get(context).copy.apply(pg, tex);
    
    // run parallel reduction Min/Max filter
    minmax.apply(tex);
    
    // read min/max-result
    byte[] result = minmax.getVal().getByteTextureData(null);


    // draw rendering
    clear();
    image(pg, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]  [fps %6.2f] [min %s] [max %s]", width, height, frameRate, formatRGBA(result, 0), formatRGBA(result, 4));
    surface.setTitle(txt_fps);
    
  }


  
  public String formatRGBA(byte[] data, int off){
    int r = data[off+0] & 0xFF;
    int g = data[off+1] & 0xFF;
    int b = data[off+2] & 0xFF;
    int a = data[off+3] & 0xFF;
    return String.format("%3d,%3d,%3d,%3d", r, g, b, a);
  }
 
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { MinMax_Demo.class.getName() });
  }
}