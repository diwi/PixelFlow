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


package ShaderToy_Seascape;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;



public class ShaderToy_Seascape extends PApplet {
  
  DwPixelFlow context;
  DwShadertoy toy;
  
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
    
    toy = new DwShadertoy(context, "data/Seascape_Image.frag");

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
    
    if(mousePressed){
      toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }
    toy.apply(pg_canvas);
    
    // put it on the screen
    blendMode(REPLACE);
    image(pg_canvas, 0, 0);
        
    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { ShaderToy_Seascape.class.getName() });
  }
}