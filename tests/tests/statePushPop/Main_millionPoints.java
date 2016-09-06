package tests.statePushPop;



import processing.core.*;

public class Main_millionPoints extends PApplet {
  
 


  public void settings() {
    size(800, 800, P2D);
    smooth(2);
  }
  

  public void setup() {
    
    beginGL();
    
    beginGL();
    endGL();
    
    endGL();

  }

  public void draw() {

    
    noFill();
    stroke(200);

  }
  
  
  int depth = 0;
  public void beginGL(){

    String space = String.format("%"+ (2+depth*2) +"s", " ");
    System.out.println(space+"begin "+depth);
    depth++;
  }
  
  public void endGL(){
    depth--;
    String space = String.format("%"+ (2+depth*2) +"s", " ");
    System.out.println(space+"end "+depth);

  }
  
  
  
  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_millionPoints.class.getName() });
  }
}