/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
 * 
 * src  - www.github.com/diwi/PixelFlow
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package AntiAliasing.Simple_SMAA;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.SMAA.SMAA;
import processing.core.PApplet;
import processing.opengl.PGraphics2D;


public class Simple_SMAA extends PApplet {
  
  
  // SMAA - Enhances SubPixel Morphological AntiAliasing
  //    PostProcessing, 3 passes, nice quality
  //    SMAA.h, Version 2.8: 
  //    http://www.iryoku.com/smaa/


  // library context
  DwPixelFlow context;
  
  // AntiAliasing
  SMAA smaa;

  // AntiAliasing render targets
  PGraphics2D canvas;
  PGraphics2D canvas_aa;

  public void settings() {
    size(800, 600, P2D);
    smooth(0);
  }
  
  public void setup() {
    // main library context
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
   
    // canvas for drawing, P2D or P3D
    canvas = (PGraphics2D) createGraphics(width, height, P2D);
    canvas.smooth(0);
    
    // canvas for AntiAliasing, P2D or P3D
    canvas_aa = (PGraphics2D) createGraphics(width, height, P2D);
    canvas_aa.smooth(0);

    // SMAA filter (post processing)
    smaa = new SMAA(context);
  }


  public void draw() {

    // draw something
    canvas.beginDraw();
    canvas.background(255);
    canvas.stroke(0);
    canvas.strokeWeight(2);
    canvas.line(width/2, height/2, mouseX, mouseY);
    canvas.pushMatrix();
    canvas.translate(width/2, height/2);
    canvas.rotate(frameCount/1000f);
    canvas.rectMode(CENTER);
    canvas.noStroke();
    canvas.fill(100, 50, 0);
    canvas.rect(0, 0, 500, 20);   
    canvas.fill(0, 50, 100);
    canvas.rect(0, 0, 20, 500);
    canvas.popMatrix();
    canvas.endDraw();
    
    // apply AntiAliasing
    boolean apply_aa = !mousePressed;
    if(apply_aa){
      smaa.apply(canvas, canvas_aa);
    }

    // display result
    blendMode(REPLACE);
    image(apply_aa ? canvas_aa : canvas, 0, 0);
 
  }


  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Simple_SMAA.class.getName() });
  }
}
