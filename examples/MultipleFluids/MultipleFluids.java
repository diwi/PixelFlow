/**
 * 
 * Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com - MIT License
 * 
 * 
 * ___PixelFlow___
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * 
 */


package MultipleFluids;



import com.thomasdiewald.pixelflow.java.Fluid;
import com.thomasdiewald.pixelflow.java.PixelFlow;

import processing.core.*;
import processing.opengl.PGraphics2D;


public class MultipleFluids extends PApplet {
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 237;
  int viewport_y = 0;
  
  int border = 50;
  
  
//  int viewport_w = 1150;
//  int viewport_h =  600;
  
  FLuidSystem fluidsystem1;
  FLuidSystem fluidsystem2;
  
  
  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  }
  
  public void setup() {
    
    surface.setLocation(viewport_x, viewport_y);
      
    PixelFlow context = new PixelFlow(this);
    context.print();
    context.printGL();

    fluidsystem1 = new FLuidSystem(0, context, (viewport_w-3*border)/2, viewport_h-2*border, 1);
    fluidsystem2 = new FLuidSystem(1, context, (viewport_w-3*border)/2, viewport_h-2*border, 1);
    
    fluidsystem1.fluid.param.dissipation_velocity = 0.99f;
    
    frameRate(60);
  }
  
    

  public void draw() {
    
    fluidsystem1.setPosition(border, border);
    fluidsystem2.setPosition(border*2 + fluidsystem1.w, border);
   
    fluidsystem1.update();
    fluidsystem2.update();
    
    background(64);
    fluidsystem1.display();
    fluidsystem2.display();
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", viewport_w, viewport_h, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  
  
  
  
  
  
  
 
  
  class FLuidSystem{
    
    int IDX;
    int px = 0;
    int py = 0;
    
    int w, h, fluidgrid_scale;
    int BACKGROUND_COLOR = 0;
    
    PixelFlow glscope;
    Fluid fluid;
    MyFluidData cb_fluid_data;

    PGraphics2D pg_fluid;
    PGraphics2D pg_obstacles;
    
    FLuidSystem(int IDX, PixelFlow glscope, int w, int h, int fluidgrid_scale){
      this.IDX = IDX;
      this.glscope = glscope;
      this.w = w;
      this.h = h;
      this.fluidgrid_scale = fluidgrid_scale;
     
      fluid = new Fluid(glscope, w, h, fluidgrid_scale);
      
      fluid.param.dissipation_density     = 0.99f;
      fluid.param.dissipation_velocity    = 0.85f;
      fluid.param.dissipation_temperature = 0.99f;
      fluid.param.vorticity               = 0.00f;
      fluid.param.timestep                = 0.25f;
      fluid.param.num_jacobi_projection   = 80;
      
      fluid.addCallback_FluiData(new MyFluidData(this));

      pg_fluid = (PGraphics2D) createGraphics(w, h, P2D);
      pg_fluid.smooth(4);
      
      pg_obstacles = (PGraphics2D) createGraphics(w, h, P2D);
      pg_obstacles.smooth(4);
      pg_obstacles.beginDraw();
      pg_obstacles.clear();
      pg_obstacles.fill(64);
      pg_obstacles.noStroke();
      pg_obstacles.ellipse(w/2, 2*h/3f, 100, 100);
      pg_obstacles.endDraw();
    }
    
    void update(){
  
      fluid.addObstacles(pg_obstacles);
      fluid.update();

      pg_fluid.beginDraw();
//      pg_fluid.clear();
      pg_fluid.background(BACKGROUND_COLOR);
      pg_fluid.endDraw();
      
      fluid.renderFluidTextures(pg_fluid, 0);
    }

    public void setPosition(int px, int py){
      this.px = px;
      this.py = py;
    }

    public void display(){
      image(pg_fluid    , px, py);
      image(pg_obstacles, px, py);
    }
  }
  
  
  
  
  class MyFluidData implements Fluid.FluidData{
    
    FLuidSystem system;
    
    MyFluidData(FLuidSystem system){
      this.system = system;
    }
    
    @Override
    // this is called during the fluid-simulation update step.
    public void update(Fluid fluid) {

      float px, py, vx, vy, radius, vscale, r, g, b, a, temperature;

      int w = system.w;
      int h = system.h;
      
      if(system.IDX == 0){
        temperature = 0.5f;
        vscale = 15;
        px     = w/2-0;
        py     = 0;
        radius = h/6f;
        fluid.addDensity (px, py, radius, 1.0f, 0.0f, 0.40f, 1f, 1);
        radius = w/6f;
        fluid.addTemperature(px, py, radius, temperature);
      }
      
      if(system.IDX == 1){
        temperature = 1.5f;
        vscale = 15;
        px     = w/2-0;
        py     = 0;
        radius = h/6f;
        fluid.addDensity (px, py, radius, 0.0f, 0.4f, 1.00f, 1f, 1);
        radius = w/6f;
        fluid.addTemperature(px, py, radius, temperature);
      }
      
      
      boolean mouse_input = mousePressed;
      
      
      if(mouse_input ){
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        
        // shift by relative position
        px -= system.px;
        py -= system.py;
        
        if(mouseButton == LEFT){
          radius = 10;
          fluid.addVelocity(px, py, radius, vx, vy);
        }
        if(mouseButton == CENTER){
          radius = 20;
          fluid.addDensity (px, py, radius, 1, 1, 1, 1f, 1);
        }
        if(mouseButton == RIGHT){
          radius = 15;
          fluid.addTemperature(px, py, radius, 5f);
        }
      }
      
    }
    
  }
  
  



  
  public static void main(String args[]) {
    PApplet.main(new String[] { MultipleFluids.class.getName() });
  }
}