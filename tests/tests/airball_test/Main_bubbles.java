package tests.airball_test;



import com.thomasdiewald.pixelflow.examples.AirBalls.Ball;
import com.thomasdiewald.pixelflow.src.Fluid;

import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;

public class Main_bubbles extends PApplet {
  
 


  public void settings() {
    System.out.println("settings()");
    size(800, 800, P2D);
    smooth(2);
  }
  

  int numBalls = 12;

  Ball[] balls = new Ball[numBalls];

  public void setup() {
    for (int i = 0; i < numBalls; i++) {
      balls[i] = new Ball(random(width), random(height), random(30, 70), i);
    }
    noStroke();
    fill(255, 204);
  }

  public void draw() {
    background(0);
    for (Ball ball : balls) {
      ball.applyCollisions(balls);
      ball.applyGravity();
      ball.updatePosition(0, 0, width, height);
      ball.display(this.g);  
    }
  }
  
  
  
  
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_bubbles.class.getName() });
  }
}