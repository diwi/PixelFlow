/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Fluid2D.Fluid_GetStarted;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;

import processing.core.*;
import processing.opengl.PGraphics2D;

public class Fluid_GetStarted extends PApplet {
  
  // Hello World Example for fluid simulations.
  //
  // Controls:
  // LMB/MMB/RMB: add density + Velocity
  //
  
  
  
  // fluid simulation
  DwFluid2D fluid;
  
  // render targets
  PGraphics2D pg_fluid;
  
  public void settings() {
    size(800, 800, P2D);
  }
  
  public void setup() {
       
    // library context
    DwPixelFlow context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    
    // fluid simulation
    fluid = new DwFluid2D(context, width, height, 1);
    
    // some fluid parameters
    fluid.param.dissipation_velocity = 0.70f;
    fluid.param.dissipation_density  = 0.99f;

    // adding data to the fluid simulation
    fluid.addCallback_FluiData(new  DwFluid2D.FluidData(){
      public void update(DwFluid2D fluid) {
        if(mousePressed){
          float px     = mouseX;
          float py     = height-mouseY;
          float vx     = (mouseX - pmouseX) * +15;
          float vy     = (mouseY - pmouseY) * -15;
          fluid.addVelocity(px, py, 14, vx, vy);
          fluid.addDensity (px, py, 20, 0.0f, 0.4f, 1.0f, 1.0f);
          fluid.addDensity (px, py,  8, 1.0f, 1.0f, 1.0f, 1.0f);
        }
      }
    });
   
    // render-target
    pg_fluid = (PGraphics2D) createGraphics(width, height, P2D);
    
    frameRate(60);
  }
  

  public void draw() {    
    // update simulation
    fluid.update();
    
    // clear render target
    pg_fluid.beginDraw();
    pg_fluid.background(0);
    pg_fluid.endDraw();

    // render fluid stuff
    fluid.renderFluidTextures(pg_fluid, 0);

    // display
    image(pg_fluid, 0, 0);
  }
  


  public static void main(String args[]) {
    PApplet.main(new String[] { Fluid_GetStarted.class.getName() });
  }
}