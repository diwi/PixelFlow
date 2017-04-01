/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package SoftBody2D_Cloth;




import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint.TYPE;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody2D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftGrid2D;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;
import processing.opengl.PGraphics2D;

public class Debug_PShape extends PApplet {
  
  //
  // This examples creates 2 cloth-objects (softbodies).
  // To compare them and checkout the different effect of parameters, both start 
  // with the same particle/spring parameters, and the gui is used to alter them.
  // 
  // + Collision Detection
  //
  // Controls:
  // LMB: drag particles
  // MMB: drag + fix particles to a location
  // RMB: disable springs, to rip the cloth
  //
  // + GUI
  //
  
  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = viewport_w - gui_w;
  int gui_y = 0;
  
  
  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  

  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
   
    
    frameRate(600);
  }
  
  
  public void draw() {
    background(255);
    
    int ny = 25;
    int nx = 25;
    int sx = 20;
    int sy = 20;
    int dx = 20;
    int dy = 20;
    PGraphics pg = this.g;
    
    
    pg.beginShape(LINES);
    pg.stroke(0);
    pg.strokeWeight(0.5f);
    for(int y = 0; y < ny; y++){
      for(int x = 0; x < nx; x++){
        int px = sx + x * dx;
        int py = sy + y * dy;
        pg.vertex(px, py);
        pg.vertex(px+10, py+10);
      }
    }
    pg.endShape();
    
    PShape shp_grp = pg.createShape(GROUP);
    shp_grp.setName("root");
    PShape shp = pg.createShape();
    shp.setName("lines");
    shp.beginShape(LINES);
    shp.stroke(0);
    shp.strokeWeight(0.5f);
    for(int y = 0; y < ny; y++){
      for(int x = 0; x < nx; x++){
        int px = sx + x * dx;
        int py = sy + y * dy;
        shp.vertex(px, py);
        shp.vertex(px+10, py+10);
      }
    }
    shp.endShape();
    shp_grp.addChild(shp);
    pg.shape(shp_grp);
    
    
    
    
  
    // info
    String txt_fps = String.format(getClass().getName()+ "    [frame %d]   [fps %6.2f]",frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  

  
  
  
  

  
  public static void main(String args[]) {
    PApplet.main(new String[] { Debug_PShape.class.getName() });
  }
}