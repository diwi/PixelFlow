package tests.gui_tests;


import controlP5.Accordion;
import controlP5.ControlKey;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;


public class accordeon extends PApplet {
  

  ControlP5 cp5;

  Accordion accordion;

  int c = color(0, 160, 100);

  
  public void settings() {
    size(400, 600);
 
    smooth();
  }
  
  public void setup() {
    noStroke();
    gui();
  }

  public void gui() {
    
    cp5 = new ControlP5(this);
    
    // group number 1, contains 2 bangs
    Group g1 = cp5.addGroup("myGroup1")
                  .setBackgroundColor(color(0, 64))
                  .setBackgroundHeight(150)
                  ;
    
    cp5.addBang("bang")
       .setPosition(10,20)
       .setSize(100,100)
//       .moveTo(g1)
       .setGroup(g1)
       .plugTo(this,"shuffle");
       ;
       
    // group number 2, contains a radiobutton
    Group g2 = cp5.addGroup("myGroup2")
                  .setBackgroundColor(color(0, 64))
                  .setBackgroundHeight(150)
                  ;
    
    cp5.addRadioButton("radio")
       .setPosition(10,20)
       .setItemWidth(20)
       .setItemHeight(20)
       .addItem("black", 0)
       .addItem("red", 1)
       .addItem("green", 2)
       .addItem("blue", 3)
       .addItem("grey", 4)
       .setColorLabel(color(255))
       .activate(2)
       .moveTo(g2)
       ;

    // group number 3, contains a bang and a slider
    Group g3 = cp5.addGroup("myGroup3")
                  .setBackgroundColor(color(0, 64))
                  .setBackgroundHeight(150)
                  ;
    
    cp5.addBang("shuffle")
       .setPosition(10,20)
       .setSize(40,50)
       .moveTo(g3)
       ;
       
    cp5.addSlider("hello")
       .setPosition(60,20)
       .setSize(100,20)
       .setRange(100,500)
       .setValue(100)
       .moveTo(g3)
       ;
       
    cp5.addSlider("world")
       .setPosition(60,50)
       .setSize(100,20)
       .setRange(100,500)
       .setValue(200)
       .moveTo(g3)
       ;

    // create a new accordion
    // add g1, g2, and g3 to the accordion.
    accordion = cp5.addAccordion("acc")
                   .setPosition(40,40)
                   .setWidth(200)
                   .addItem(g1)
                   .addItem(g2)
                   .addItem(g3)
                   ;
                   
    cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.open(0,1,2);}}, 'o');
    cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.close(0,1,2);}}, 'c');
    cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.setWidth(300);}}, '1');
    cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.setPosition(0,0);accordion.setItemHeight(190);}}, '2'); 
    cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.setCollapseMode(ControlP5.ALL);}}, '3');
    cp5.mapKeyFor(new ControlKey() {public void keyEvent() {accordion.setCollapseMode(ControlP5.SINGLE);}}, '4');
    cp5.mapKeyFor(new ControlKey() {public void keyEvent() {cp5.remove("myGroup1");}}, '0');
    
    accordion.open(0,1,2);
    
    // use Accordion.MULTI to allow multiple group 
    // to be open at a time.
    accordion.setCollapseMode(Accordion.MULTI);
    
    // when in SINGLE mode, only 1 accordion  
    // group can be open at a time.  
    // accordion.setCollapseMode(Accordion.SINGLE);
  }
    

  public void radio(int theC) {
    switch(theC) {
      case(0):c=color(0,200);break;
      case(1):c=color(255,0,0,200);break;
      case(2):c=color(0, 200, 140,200);break;
      case(3):c=color(0, 128, 255,200);break;
      case(4):c=color(50,128);break;
    }
  } 


  public void shuffle() {
    c = color(random(255),random(255),random(255),random(128,255));
  }


  public void draw() {
    background(220);
    
    fill(c);
    
    float s1 = cp5.getController("hello").getValue();
    ellipse(200,400,s1,s1);
    
    float s2 = cp5.getController("world").getValue();
    ellipse(300,100,s2,s2);
  }




  public static void main(String args[]) {
    PApplet.main(new String[] { accordeon.class.getName() });
  }
}