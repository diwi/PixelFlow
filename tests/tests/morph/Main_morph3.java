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


public class Main_morph3 extends PApplet {
  

  
  public class MorphShape{
    
    ArrayList<PVector> shape1 = new ArrayList<PVector>();
    ArrayList<PVector> shape2 = new ArrayList<PVector>();
    ArrayList<PVector> morph = new ArrayList<PVector>();
   
    public MorphShape(){
      createShape1();
      createShape2();
    }
    
    // square
    public void createShape1(){
      
      float size = 150;
      int NUM_SEGS = 50;
      float seg_len = size/NUM_SEGS;
      
      float px = +size/2;
      float py = +size/2;
     
      // BOTTOM
      for(int i = 0; i < NUM_SEGS; i++){
        shape1.add(new PVector(px, py));
        px -= seg_len;
      }

      // LEFT
      for(int i = 0; i < NUM_SEGS; i++){
        shape1.add(new PVector(px, py));
        py -= seg_len;
      }

      // TOP
      for(int i = 0; i < NUM_SEGS; i++){
        shape1.add(new PVector(px, py));
        px += seg_len;
      }
      shape1.add(new PVector(px, py));
      
    }
    
    public void createShape2(){
      int NUM_POINTS = shape1.size();
      
      float size = 150;
      float arc_min = PI/4f;
      float arc_max = TWO_PI-arc_min;
      float arc_range = arc_max - arc_min;
      float arc_step = arc_range/(NUM_POINTS-1);
   
      for (int i = 0; i < NUM_POINTS; i++) {
        PVector v = PVector.fromAngle(arc_min + i * arc_step);
        v.mult(size);
        shape2.add(v);
      }
      
      
      for (int i = 0; i < NUM_POINTS; i++) {
        morph.add(new PVector());
      }
    }
    public void draw2(float mix){
      beginShape();
      for (int i = 0; i < shape1.size(); i++) {
        PVector v1 = shape1.get(i);
        PVector v2 = shape2.get(i);
        PVector v = PVector.lerp(v1, v2, mix);
        vertex(v.x, v.y);
      }
      endShape();
    }
    
    
    public void draw(){
      float totalDistance = 0;
      
      for (int i = 0; i < shape1.size(); i++) {
        PVector v1;
        if (state) {
          v1 = shape1.get(i);
        } else {
          v1 = shape2.get(i);
        }
        // Get the vertex we will draw
        PVector v2 = morph.get(i);
        // Lerp to the target
        v2.lerp(v1, 0.05f);
        // Check how far we are from target
        totalDistance += PVector.dist(v1, v2);
      }
      
      // If all the vertices are close, switch shape
      if (totalDistance < 1) {
        state = !state;
      }
      
      // Draw relative to center
      strokeWeight(4);
      // Draw a polygon that makes up all the vertices
      beginShape();
      noFill();
      stroke(255);
      for (PVector v : morph) {
        vertex(v.x, v.y);
      }
      endShape();
    }
    
  }


  
  boolean state = false;
  
  MorphShape morph;
  
  public void settings() {
    size(640, 360, P2D);
  }

  public void setup() {

    morph = new MorphShape();

  }

  
  float morph_amount = 0;
  
  float ease = 0.95f;
  float mix = 1f;
  
  int morph_state = 1;
  
  public void draw() {
    background(51);

    morph_amount += 0.05f;
//    System.out.println(sin(morph_amount));
    
    
    
    translate(width/2, height/2);
    
    noFill();
    strokeWeight(4);
    stroke(255);
    
    pushMatrix();
    translate(-100, 0);
    morph.draw();
    popMatrix();
    
    pushMatrix();
    translate(+100, 0);
    mix *= ease;

    if( mix < 0.00005f){
      mix = 1f;
      morph_state ^= 1;
    } 
    
    morph.draw2(morph_state == 0 ? mix : 1-mix);
    popMatrix();

    
    
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_morph3.class.getName() });
  }
}