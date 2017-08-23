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
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.MinMax;
import processing.core.PApplet;
import processing.opengl.PGraphics2D;


public class MinMax_Demo extends PApplet {
  
  DwPixelFlow context;
  
  // Min-Max Filter
  MinMax minmax;
  
  // temp texture, to copy the PGraphics
  DwGLTexture tex = new DwGLTexture();
  
  PGraphics2D pg;

  public void settings() {
    size(1200, 800, P2D);
    smooth(0);
  }
  
  public void setup() {
    
    context = new DwPixelFlow(this);
    
    minmax = new MinMax(context);
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
    
    // copy to native opengl tex
    DwFilter.get(context).copy.apply(pg, tex);
    
    // run parallel reduction Min/Max filter
    byte[] min = new byte[4];
    byte[] max = new byte[4];

    // min
    minmax.apply(tex, MinMax.Mode.MIN);
    minmax.getLatest().getByteTextureData(min);
    
    // max
    minmax.apply(tex, MinMax.Mode.MAX);
    minmax.getLatest().getByteTextureData(max);
    
    
    // draw rendering
    clear();
    image(pg, 0, 0);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]  [fps %6.2f] [min %s] [max %s]", width, height, frameRate, formatRGBA(min), formatRGBA(max));
    surface.setTitle(txt_fps);
    
  }


  
  public String formatRGBA(byte[] data){
    int r = data[0] & 0xFF;
    int g = data[1] & 0xFF;
    int b = data[2] & 0xFF;
    int a = data[3] & 0xFF;
    return String.format("%3d,%3d,%3d,%3d", r, g, b, a);
  }
 
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { MinMax_Demo.class.getName() });
  }
}