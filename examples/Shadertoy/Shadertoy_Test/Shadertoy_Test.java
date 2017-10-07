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


package Shadertoy.Shadertoy_Test;


import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;
import processing.core.PApplet;



public class Shadertoy_Test extends PApplet {
  
  //
  // Shadertoy Demo:   
  // Shadertoy Author: 
  //
  
  // Basic 2-pass demo to get started.
  //
  // toyA: Test_BufA.frag ... creates some output
  // toy : Test.frag      ... simply copies the result from toyA
  
  DwPixelFlow context;
  DwShadertoy toy, toyA;
  
  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toyA = new DwShadertoy(context, "data/Test_BufA.frag");
    toy  = new DwShadertoy(context, "data/Test.frag");
    
    frameRate(60);
  }

  
  public void draw() {

    if(mousePressed){
      toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }
    toyA.apply(width, height);
    
    toy.set_iChannel(0, toyA);
    toy.apply(this.g);

    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_Test.class.getName() });
  }
}