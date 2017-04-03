/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package BloomDemo;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import processing.core.PApplet;
import processing.core.PFont;
import processing.opengl.PGraphics2D;


public class BloomDemo extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  DwPixelFlow context;

  DwFilter filter;
  
  PGraphics2D pg_src_A;
  PGraphics2D pg_src_B;
  
  PFont font12;

  public void settings(){
//    size(viewport_w, viewport_h, P2D);
    fullScreen(P2D);
    smooth(0);
  }
  
  public void setup(){
    surface.setLocation(viewport_x, viewport_y);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    filter = new DwFilter(context);
    
//    System.out.println("width/height "+width+"/"+height);
    
    pg_src_A = (PGraphics2D) createGraphics(width, height, P2D);
    pg_src_A.smooth(8);
    
    pg_src_B = (PGraphics2D) createGraphics(width, height, P2D);
    pg_src_B.smooth(8);
    
    font12 = createFont("SourceCodePro-Regular.ttf", 12);

    background(0);

//    frameRate(60);
    frameRate(1000);
  }
  
  
  public void draw(){

    
    // just draw something into pg_src_A
    pg_src_A.beginDraw();
    {
      pg_src_A.blendMode(BLEND); // default
      pg_src_A.rectMode(CENTER);
      pg_src_A.background(0);
      
      int num_x = 20;
      int num_y = ceil(num_x / (width / (float) height));
   
      float size_x = (width  - 1) / (float)num_x;
      float size_y = (height - 1) / (float)num_y;
      float scale_xy = 0.4f;
      
      for(int y = 0; y < num_y; y++){
        for(int x = 0; x < num_x; x++){
          float px = x * size_x + size_x * 0.5f;
          float py = y * size_y + size_y * 0.5f;
     
          float norm_x = x/(float)(num_x - 1);
          float norm_y = y/(float)(num_y - 1);
          
          float rgb_r = 255 - 255 * norm_x * norm_y;
          float rgb_g = norm_x * 255;
          float rgb_b = norm_y * 255;
          int col =  color(rgb_r, rgb_g, rgb_b);
          
          pg_src_A.pushMatrix();
          pg_src_A.translate(px, py);
          pg_src_A.strokeWeight(2f);
          pg_src_A.stroke(col);
          if( ((x^y)&1) == 0){
            pg_src_A.fill(col);
          }else {
            pg_src_A.noFill();
          }
          pg_src_A.rect(0, 0, size_x * scale_xy, size_y * scale_xy, 0);
          pg_src_A.popMatrix();
        }
      }

      pg_src_A.stroke(255);
      pg_src_A.fill(0, 240);
      pg_src_A.strokeWeight(2);
      pg_src_A.ellipse(mouseX, mouseY, height/4, height/4);
    }
    pg_src_A.endDraw();
    

    if(DISPLAY_MODE != 0){
      // luminance pass
      filter.luminance_threshold.param.threshold = 0.0f; // when 0, all colors are used
      filter.luminance_threshold.param.exponent  = 5;
      filter.luminance_threshold.apply(pg_src_A, pg_src_B);
      
      // bloom pass
      // if the original image is used as source, the previous luminance pass 
      // can just be skipped
//      filter.bloom.setBlurLayers(10);
      filter.bloom.param.mult   = map(mouseX, 0, width, 0, 10);
      filter.bloom.param.radius = map(mouseY, 0, height, 0, 1);
      filter.bloom.apply(pg_src_B, pg_src_A);
//      filter.bloom.apply(pg_src_A, pg_src_A); // use src as luminance
    }


    switch(DISPLAY_MODE){
      case 2: filter.copy.apply(pg_src_B, pg_src_A); break;
      case 3: filter.copy.apply(filter.bloom.tex_blur_dst[0], pg_src_A); break;
      case 4: filter.copy.apply(filter.bloom.tex_blur_dst[1], pg_src_A); break;
      case 5: filter.copy.apply(filter.bloom.tex_blur_dst[2], pg_src_A); break;
      case 6: filter.copy.apply(filter.bloom.tex_blur_dst[3], pg_src_A); break;
      case 7: filter.copy.apply(filter.bloom.tex_blur_dst[4], pg_src_A); break;
    }
    
    // display result
    blendMode(REPLACE);
    background(0);
    image(pg_src_A, 0, 0);
    
    // mouse position hints
    int s = 3;
    blendMode(BLEND);
    strokeCap(SQUARE);

    strokeWeight(s*2);

    stroke(0, 50);
    line(0, s, width, s);
    line(s, 0, s, height);
    
    stroke(0, 160);
    line(0, s, mouseX, s);
    line(s, 0, s, mouseY);
    

    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]  [mode %d]  [frame %d]   [fps %6.2f]", pg_src_A.width, pg_src_A.height, DISPLAY_MODE, frameCount, frameRate);
    surface.setTitle(txt_fps);
    
    // key hints
    textFont(font12);
    int tx = 15;
    int ty = 15;
    int gap = 15;
    stroke(255);
    text(txt_fps         , tx, ty+=gap);
    text("'0' bloom OFF" , tx, ty+=gap);
    text("'1' Bloom ON"  , tx, ty+=gap);
    text("'2' Luminance" , tx, ty+=gap);
    text("'3' Blur[0]"   , tx, ty+=gap);
    text("'4' Blur[1]"   , tx, ty+=gap);
    text("'5' Blur[2]"   , tx, ty+=gap);
    text("'6' Blur[3]"   , tx, ty+=gap);
    text("'7' Blur[4]"   , tx, ty+=gap);
    text("mouseX: bloom multiplier"   , tx, ty+=gap);
    text("mouseY: bloom radius"       , tx, ty+=gap);

    
  }
  
  int DISPLAY_MODE = 1;
  
  public void keyReleased(){
    if(key == '0') DISPLAY_MODE = 0;
    if(key == '1') DISPLAY_MODE = 1;
    if(key == '2') DISPLAY_MODE = 2;
    if(key == '3') DISPLAY_MODE = 3;
    if(key == '4') DISPLAY_MODE = 4;
    if(key == '5') DISPLAY_MODE = 5;
    if(key == '6') DISPLAY_MODE = 6;
    if(key == '7') DISPLAY_MODE = 7;
  }

  public static void main(String args[]) {
    PApplet.main(new String[] { BloomDemo.class.getName() });
  }
}