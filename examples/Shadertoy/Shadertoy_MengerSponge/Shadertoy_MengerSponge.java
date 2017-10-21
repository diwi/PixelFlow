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


package Shadertoy.Shadertoy_MengerSponge;



import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwShadertoy;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;


public class Shadertoy_MengerSponge extends PApplet {
  
  //
  // Shadertoy Demo:   https://www.shadertoy.com/view/ldyGWm
  // Shadertoy Author: https://www.shadertoy.com/user/Shane
  //
  
  DwPixelFlow context;
  DwShadertoy toy;
  PGraphics2D pg_albedo;

  public void settings() {
    size(1280, 720, P2D);
    smooth(0);
  }
  
  public void setup() {
    surface.setResizable(true);
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    toy = new DwShadertoy(context, "data/MengerSponge.frag");
    
    pg_albedo = (PGraphics2D) createGraphics(512, 512, P2D);
    DwUtils.changeTextureWrap  (pg_albedo, GL2.GL_MIRRORED_REPEAT);
    DwUtils.changeTextureFilter(pg_albedo, GL2.GL_LINEAR, GL2.GL_LINEAR);
    
    pg_albedo.beginDraw();
    pg_albedo.background(200,100,5);
    pg_albedo.endDraw();
    
    frameRate(60);
  }


  public void draw() {
    if(mousePressed){
      toy.set_iMouse(mouseX, height-1-mouseY, mouseX, height-1-mouseY);
    }
    toy.set_iChannel(0, pg_albedo);
    toy.apply(this.g);

    String txt_fps = String.format(getClass().getSimpleName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", width, height, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Shadertoy_MengerSponge.class.getName() });
  }
}