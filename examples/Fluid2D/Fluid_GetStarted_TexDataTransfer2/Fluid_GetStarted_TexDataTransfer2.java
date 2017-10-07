/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Fluid2D.Fluid_GetStarted_TexDataTransfer2;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.fluid.DwFluid2D;

import processing.core.*;
import processing.opengl.PGraphics2D;

public class Fluid_GetStarted_TexDataTransfer2 extends PApplet {
  
  // Basic example for texture data transfer

  // fluid simulation
  DwFluid2D fluid;
  
  // render targets
  PGraphics2D pg_fluid;
  
  public void settings() {
    size(600, 600, P2D);
  }
  
  public void setup() {
       
    // library context
    DwPixelFlow context = new DwPixelFlow(this);
    context.print();
    context.printGL();
    System.out.println("Example: "+this.getClass().getSimpleName());
    
    // fluid simulation
    fluid = new DwFluid2D(context, width, height, 1);
    
    // some fluid parameters
    fluid.param.dissipation_velocity = 0.90f;
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
        
        float px     = fluid.fluid_w/2;
        float py     = 0;
        fluid.addDensity (px, py, 50, 1.0f, 0.4f, 0.0f, 1.0f);
        fluid.addDensity (px, py, 40, 1.0f, 1.0f, 1.0f, 1.0f);
        fluid.addTemperature(px, py, 50, 2);
      }
    });
   
    // render-target
    pg_fluid = (PGraphics2D) createGraphics(width, height, P2D);
    pg_fluid.smooth(8);
    
    frameRate(60);
  }
  
  
  float[] data_vel;

  public void draw() {    
    
    // update simulation
    fluid.update();
    
    if(data_vel == null){
      data_vel = new float[fluid.fluid_w * fluid.fluid_h * 2];
    }
    
    // transfer velocity frame
    // every frame a horizontal strip of size 100x600 is transfered.
    // this speeds-up the transfer, but also slows down the "visual"
    // animation by 6
    int num_strips = 6;
    int strip_id = frameCount % num_strips;
    int strip_w = fluid.fluid_w;
    int strip_h = fluid.fluid_h / num_strips;
    int strip_x = 0;
    int strip_y = strip_id * strip_h;
    int strip_len = strip_w * strip_h * 2;
    int buffer_offset = strip_id * strip_len;
    
    data_vel = fluid.getVelocity(data_vel, strip_x, strip_y, strip_w, strip_h, buffer_offset);
    
    // draw velocity vectors
    float vel_mult = 2;
    int grid_points = 20;
    float grid_space = fluid.fluid_w / (float)(grid_points + 1);

    beginShape(LINES);
    strokeWeight(1);
    stroke(255);
    for(int gy = 0; gy < grid_points; gy++){
      for(int gx = 0; gx < grid_points; gx++){
        
        float gx_pos1 = grid_space + gx * grid_space;
        float gy_pos1 = grid_space + gy * grid_space;
        
        int gx_fluid = round(gx_pos1);
        int gy_fluid = round(fluid.fluid_h - 1 - gy_pos1);
        int gid_fluid = gy_fluid * fluid.fluid_w + gx_fluid; // inverted y-coord
        
        float vel_x = data_vel[gid_fluid * 2 + 0];
        float vel_y = data_vel[gid_fluid * 2 + 1];

        float gx_pos2 = gx_pos1 + vel_x * vel_mult;
        float gy_pos2 = gy_pos1 - vel_y * vel_mult; // inverted y-velocity

        vertex(gx_pos1, gy_pos1);
        vertex(gx_pos2, gy_pos2);
      }
    }
    endShape();
    
    
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
    PApplet.main(new String[] { Fluid_GetStarted_TexDataTransfer2.class.getName() });
  }
}