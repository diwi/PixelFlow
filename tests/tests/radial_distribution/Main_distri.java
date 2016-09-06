package tests.radial_distribution;



import java.util.Random;

import processing.core.*;

public class Main_distri extends PApplet {
  
 


  public void settings() {
    System.out.println("settings()");
    size(800, 800, P2D);
    smooth(2);
  }
  





  public void setup() {

  }

  public void draw() {

    background(255);

    int count = mouseX*10;
    float radius = 200;


    
//    {
//      float rand1 = (float) Math.random();
//      float rand = (float) Math.random();
//      float noise = (float)(rand * Math.PI * 2);
//      
//  
//      
//      for(int i = 0; i < count; i++){
//        float r = radius * i / (float)count;
//        float x = cos(i*i*noise);
//        float y = sin(i*i*noise);
//        
//        float px = width /2 + x * r;
//        float py = height/2 + y * r;
//        
//        stroke(0);
//        fill(0);
//        noStroke();
//        ellipse(px, py, 6, 6);
//      }
//    }
    
    
    
//    {
//      
//      Random randgen = new Random();
//      randgen.setSeed(0);
//      
//      float rand1 = randgen.nextFloat();
//      float rand = randgen.nextFloat();
//      float noise = (float)(rand * Math.PI * 2);
//      
//  
//      float arc = TWO_PI / count;
//      
//      for(int i = 0; i < count; i++){
////        float r = radius * i / (float)count;
//
//        float rand2 = randgen.nextFloat();
//        
//        float r = radius*sqrt( (i*mouseX)%count / (float)count );
//            
//        float x = cos(i * arc);
//        float y = sin(i * arc);
//        
//        float px = width /2 + x * r;
//        float py = height/2 + y * r;
//        
//        stroke(0);
//        fill(0);
//        noStroke();
//        ellipse(px, py, 6, 6);
//      }
//    }
    
    
    
    
    {
      
      Random randgen = new Random();
      randgen.setSeed(0);
      
      float rand1 = randgen.nextFloat();
      float rand = randgen.nextFloat();
      float noise = (float)(rand * Math.PI * 2);
      
  
      float arc = TWO_PI / count;
      
      

      float golden_angle_d = 180* (3 - sqrt(5));
      float golden_angle_r = PI * (3 - sqrt(5));

      
      for(int i = 0; i < count; i++){
        
        float inorm = i/(float)count;
        
//        float r = radius * sin(1-sqrt(inorm) );
        
//        float r = radius * sqrt((1-cos(inorm))) ;
//        float r = radius * ((1-cos(sqrt(inorm)))) ;
        
//        System.out.println(cos(sqrt(inorm)));
        
        float r = radius * sqrt(inorm) ;
        
        
//        float r = radius * sin(1-sqrt(i/(float)count) );
//        float r = radius * sin((i) /(float)count );
        
        float x = cos(i * golden_angle_r);
        float y = sin(i * golden_angle_r);
        
        float px = width /2 + x * r;
        float py = height/2 + y * r;
        
        stroke(0);
        fill(0);
        noStroke();
        ellipse(px, py, 2, 2);
      }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    noFill();
    stroke(200);
//    ellipse(width /2, height/2, radius*2, radius*2);
    
  }
  
  
  
  
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_distri.class.getName() });
  }
}