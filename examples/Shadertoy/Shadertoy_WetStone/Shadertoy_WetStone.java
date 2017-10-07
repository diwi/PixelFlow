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


package Shadertoy.Shadertoy_WetStone;



import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;
import processing.core.PApplet;



public class Shadertoy_WetStone extends PApplet {
  
  //
  // Shadertoy Demo:   https://www.shadertoy.com/view/ldSSzV
  // Shadertoy Author: https://www.shadertoy.com/user/TDM
  //
  
  DwPixelFlow context;
  DwShadertoy toy;

  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);

    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    toy = new DwShadertoy(context, "data/WetStone.frag");
    
    frameRate(60);
  }


  public void draw() {
    if(mousePressed){
      toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }

    toy.apply(g);

    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_WetStone.class.getName() });
  }
}