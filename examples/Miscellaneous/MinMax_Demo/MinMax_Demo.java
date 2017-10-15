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


package Miscellaneous.MinMax_Demo;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
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
  
  PGraphics2D pg;

  public void settings() {
    size(1211, 799, P2D);
    smooth(0);
  }
  
  public void setup() {
    context = new DwPixelFlow(this);
    
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
    
    // [minR, minG, minB, minA,  maxR, maxG, maxB, maxA]
    byte[] result = new byte[8];
    
    boolean GPU = !mousePressed;
    
    
    // GPU Version, parallel reduction Min/Max filter
    if(GPU)
    {
      MinMaxGlobal minmax = DwFilter.get(context).minmaxglobal;
      minmax.apply(pg);
      
      // read min/max-result from GPU memory
      minmax.getVal().getByteTextureData(result);
    }
    else
    // CPU Version, a lot slower
    {
      pg.loadPixels();
      int[] min = {Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE};
      int[] max = {Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MIN_VALUE};
      for(int i = 0; i < pg.pixels.length; i++){
        int px =  pg.pixels[i];
        int a = (px >> 24) & 0xFF;
        int r = (px >> 16) & 0xFF;
        int g = (px >>  8) & 0xFF;
        int b = (px >>  0) & 0xFF;
        
        min[0] = min(r, min[0]);   max[0] = max(r, max[0]);
        min[1] = min(g, min[1]);   max[1] = max(g, max[1]);
        min[2] = min(b, min[2]);   max[2] = max(b, max[2]);
        min[3] = min(a, min[3]);   max[3] = max(a, max[3]);
      }
      result[0] = (byte) min[0];   result[4] = (byte) max[0];
      result[1] = (byte) min[1];   result[5] = (byte) max[1];
      result[2] = (byte) min[2];   result[6] = (byte) max[2];
      result[3] = (byte) min[3];   result[7] = (byte) max[3];
    }
    
    

    // draw rendering
    blendMode(REPLACE);
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