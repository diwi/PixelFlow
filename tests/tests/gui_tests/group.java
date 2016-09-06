package tests.gui_tests;


import controlP5.Button;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;


public class group extends PApplet {
  
  ControlP5 cp5;
  
  public void settings() {  
    size(800,400);
  }
  
  public void setup() {  
 

    cp5 = new ControlP5(this);
    
    Group g1 = cp5.addGroup("g1")
                  .setPosition(100,100)
                  .setBackgroundHeight(100)
                  .setBackgroundColor(color(255,50))
                  ;
                       
    cp5.addBang("A-1")
       .setPosition(10,20)
       .setSize(80,20)
       .setGroup(g1)
       ;
            
    cp5.addBang("A-2")
       .setPosition(10,60)
       .setSize(80,20)
       .setGroup(g1)
       ;
       
    
    Group g2 = cp5.addGroup("g2")
                  .setPosition(250,100)
                  .setWidth(300)
                  .activateEvent(true)
                  .setBackgroundColor(color(255,80))
                  .setBackgroundHeight(100)
                  .setLabel("Hello World.")
                  ;
    
    cp5.addSlider("S-1")
       .setPosition(80,10)
       .setSize(180,9)
       .setGroup(g2)
       ;
       
    cp5.addSlider("S-2")
       .setPosition(80,20)
       .setSize(180,9)
       .setGroup(g2)
       ;
       
    cp5.addRadioButton("radio")
       .setPosition(10,10)
       .setSize(20,9)
       .addItem("black",0)
       .addItem("red",1)
       .addItem("green",2)
       .addItem("blue",3)
       .addItem("grey",4)
       .setGroup(g2)
       ;
       
    Group g3 = cp5.addGroup("g3")
                  .setPosition(600,100)
                  .setSize(150,200)
                  .setBackgroundColor(color(255,100))
                  ;
                  
    
    cp5.addScrollableList("list")
       .setPosition(10,10)
       .setSize(130,100)
       .setGroup(g3)
       .addItems(java.util.Arrays.asList("a","b","c","d","e","f","g"))
       ;
  }


  public void draw() {
    background(0);
  }


  public void controlEvent(ControlEvent theEvent) {
    if(theEvent.isGroup()) {
      println("got an event from group "
              +theEvent.getGroup().getName()
              +", isOpen? "+theEvent.getGroup().isOpen()
              );
              
    } else if (theEvent.isController()){
      println("got something from a controller "
              +theEvent.getController().getName()
              );
    }
  }


  public void keyPressed() {
    if(key==' ') {
      if(cp5.getGroup("g1")!=null) {
        cp5.getGroup("g1").remove();
      }
    }
  }


  
  public static void main(String args[]) {
    PApplet.main(new String[] { group.class.getName() });
  }
}