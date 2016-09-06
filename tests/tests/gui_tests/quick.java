package tests.gui_tests;


import controlP5.Button;
import controlP5.ControlP5;
import processing.core.*;


public class quick extends PApplet {


  ControlP5 cp5;

  float s1 = 5;
  float s2 = 2;
  boolean t1 = true;
  boolean t2 = true;
  boolean t3 = true;
  boolean t4 = true;
  float n1 = 100;
  int n2 = 50;
  
  public void settings(){
    size(600,400);
  }
  
  Button bhide;

  public void setup() {

    noStroke();
    cp5 = new ControlP5(this);
    cp5.addButton("hi");

    cp5.addButton("HIDE",1);
    bhide = cp5.addButton("b2",2);
    cp5.addButton("b3",3);
    cp5.addButton("b4",4).linebreak();
    cp5.addSlider("s1",0,10);
    cp5.addSlider("s2",0,10).linebreak();
    cp5.addButton("b5");
    cp5.addToggle("t1");
    cp5.addToggle("t2");
    cp5.addToggle("t3");
    cp5.addToggle("t4").linebreak();
    cp5.addNumberbox("n1");
    cp5.addNumberbox("n2");
  
  }

  public  void draw() {
    background(0);
    if(t1) {
      fill(s1*25);
      rect(0,200,150,height);
    }
    if(t2) {
      fill(s2*25);
      rect(150,200,150,height);
    }
    if(t3) {
      fill(n1);
      rect(300,200,150,height);
    }
    if(t4) {
      fill(n2);
      rect(450,200,150,height);
    }
  }

  public void HIDE(int theN) {
    println(theN);
//    cp5.hide(bhide);
    cp5.hide();
  }

  public void b2(int theN) {
    println(theN);
  }
  
  public static void main(String args[]) {
    PApplet.main(new String[] { quick.class.getName() });
  }
}