package tests.morph;



import java.util.ArrayList;
import processing.core.*;


public class Main_morph2 extends PApplet {
  

  
  public class MorphShape{
    
    ArrayList<float[]> shape1 = new ArrayList<float[]>();
    ArrayList<float[]> shape2 = new ArrayList<float[]>();

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
        shape1.add(new float[]{px, py});
        px -= seg_len;
      }

      // LEFT
      for(int i = 0; i < NUM_SEGS; i++){
        shape1.add(new float[]{px, py});
        py -= seg_len;
      }

      // TOP
      for(int i = 0; i < NUM_SEGS; i++){
        shape1.add(new float[]{px, py});
        px += seg_len;
      }
      shape1.add(new float[]{px, py});
      
    }
    
    public void createShape2(){
      int NUM_POINTS = shape1.size();
      
      float size = 150;
      float arc_min = PI/4f;
      float arc_max = TWO_PI-arc_min;
      float arc_range = arc_max - arc_min;
      float arc_step = arc_range/(NUM_POINTS-1);
   
      for (int i = 0; i < NUM_POINTS; i++) {
        float arc = arc_min + i * arc_step;
        float vx = size * cos(arc);
        float vy = size * sin(arc);
        shape2.add(new float[]{vx, vy});
      }

    }
    

    float morph_mix = 1f;
    int   morph_state = 1;
    
    public void drawAnimated(float ease){
      morph_mix *= ease;
      if(morph_mix < 0.0001f){
        morph_mix = 1f;
        morph_state ^= 1;
      } 
      
      morph.draw(morph_state == 0 ? morph_mix : 1-morph_mix);
    }
    
    
    public void draw(float mix){
      beginShape();
      for (int i = 0; i < shape1.size(); i++) {
        float[] v1 = shape1.get(i);
        float[] v2 = shape2.get(i);
        float vx = v1[0] * (1.0f - mix) + v2[0] * mix;
        float vy = v1[1] * (1.0f - mix) + v2[1] * mix;
        vertex(vx, vy);
      }
      endShape();
    }
     
  }


  MorphShape morph;
  
  
  public void settings() {
    size(640, 360, P2D);
  }

  public void setup() {

    morph = new MorphShape();

  }

 
  public void draw() {
    background(51);


    translate(width/2, height/2);
    
    noFill();
    strokeWeight(4);
    stroke(255);
    
    morph.drawAnimated(0.95f);
  }
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_morph2.class.getName() });
  }
}