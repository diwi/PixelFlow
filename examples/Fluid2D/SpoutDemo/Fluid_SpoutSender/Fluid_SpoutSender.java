/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package Fluid2D.SpoutDemo.Fluid_SpoutSender;


import spout.*;
import processing.core.PApplet;

public class Fluid_SpoutSender extends PApplet {
  
  //
  // Demo: Spout sender
  //
  // Note: 
  // run sketch "Fluid_SpoutSender" to send a texture
  // run sketch "Fluid_SpoutReceiver" to receive a texture
  //
  // this requires to have the "Spout for Processing" library installed:
  // https://github.com/leadedge/SpoutProcessing/releases
  // install manually, or the PDE-Contribution-Manager
  //
  
  Spout spout;
  
  public void settings() {
    size(800, 600, P2D);
    smooth(8);
  }

  public void setup() {
    surface.setLocation(1080, 0);
    spout = new Spout(this);
    background(0);
    frameRate(120);
  }

  public void draw() {
    float velocity = min(max(dist(mouseX, mouseY, pmouseX, pmouseY) / 5f, 2), 8);
    float radius = mousePressed ? 50 : velocity * 10;
    
    strokeWeight(1);
    stroke(0);
    fill(color(mousePressed ? 0 : 255));
    ellipse(mouseX, mouseY, radius, radius );
    
    if(frameCount % 10 == 0){
      radius = random(10,40);
      float r = random(128,255);
      fill(r, r*0.75f, r*0.5f);
      ellipse(random(width), random(height), radius, radius );
    }
    
    spout.sendTexture();
  }
  
  public void keyReleased(){
    if(key == 'r') background(0);
  }

  public static void main(String[] args) {
    PApplet.main(new String[] { Fluid_SpoutSender.class.getName() });
  }

}
