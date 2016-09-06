package tests.morph;



import java.util.ArrayList;
import java.util.Locale;

import com.thomasdiewald.pixelflow.src.Fluid;
import com.thomasdiewald.pixelflow.src.ParticleSystem;
import com.thomasdiewald.pixelflow.src.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.src.dwgl.DwGLTexture.TexturePingPong;

import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.opengl.Texture;


public class Main_morph extends PApplet {


  ArrayList<PVector> circle = new ArrayList<PVector>();
  ArrayList<PVector> square = new ArrayList<PVector>();
  ArrayList<PVector> morph = new ArrayList<PVector>();
  
  boolean state = false;
  
  public void settings() {
    size(640, 360, P2D);
  }

  public void setup() {

    for (int angle = 0; angle < 360; angle += 9) {
      PVector v = PVector.fromAngle(radians(angle-135));
      v.mult(100);
      circle.add(v);
      // Let's fill out morph ArrayList with blank PVectors while we are at it
      morph.add(new PVector());
    }

    // A square is a bunch of vertices along straight lines
    // Top of square
    for (int x = -50; x < 50; x += 10) {
      square.add(new PVector(x, -50));
    }
    // Right side
    for (int y = -50; y < 50; y += 10) {
      square.add(new PVector(50, y));
    }
    // Bottom
    for (int x = 50; x > -50; x -= 10) {
      square.add(new PVector(x, 50));
    }
    // Left side
    for (int y = 50; y > -50; y -= 10) {
      square.add(new PVector(-50, y));
    }
  }

  public void draw() {
    background(51);

    float totalDistance = 0;
    
    for (int i = 0; i < circle.size(); i++) {
      PVector v1;
      if (state) {
        v1 = circle.get(i);
      } else {
        v1 = square.get(i);
      }
      // Get the vertex we will draw
      PVector v2 = morph.get(i);
      // Lerp to the target
      v2.lerp(v1, 0.1f);
      // Check how far we are from target
      totalDistance += PVector.dist(v1, v2);
    }
    
    // If all the vertices are close, switch shape
    if (totalDistance < 0.1) {
      state = !state;
    }
    
    // Draw relative to center
    translate(width/2, height/2);
    strokeWeight(4);
    // Draw a polygon that makes up all the vertices
    beginShape();
    noFill();
    stroke(255);
    for (PVector v : morph) {
      vertex(v.x, v.y);
    }
    endShape(CLOSE);
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_morph.class.getName() });
  }
}