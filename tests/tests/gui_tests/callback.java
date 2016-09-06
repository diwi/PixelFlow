package tests.gui_tests;


import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.Slider;
import controlP5.Textarea;
import processing.core.*;


public class callback extends PApplet {
  

  ControlP5 cp5;
  Slider s1, s2;
  Info info;
  CallbackListener cb;
  
  public void settings() {
    size(800, 400);
  }
  
  public void setup() {


    cp5 = new ControlP5(this);

    
    // create a new instance of class Info
    // info will be used to display a controller's information and
    // will fade in when a CallbackEvent is invoked.
    info = new Info();


    // add 2 sliders
    s1 = cp5.addSlider("hello")
            .setRange(0, 100)
            .setValue(50)
            .setPosition(40, 40)
            .setSize(100, 20);
            
    s2 = cp5.addSlider("world")
            .setRange(0, 100)
            .setValue(10)
            .setPosition(40, 70)
            .setSize(100, 20);


    // the following CallbackListener will listen to any controlP5 
    // action such as enter, leave, pressed, released, releasedoutside, broadcast
    // see static variables starting with ACTION_ inside class controlP5.ControlP5Constants

    cb = new CallbackListener() {
      public void controlEvent(CallbackEvent theEvent) {
        switch(theEvent.getAction()) {
          case(ControlP5.ACTION_ENTER):
          info.n = 1;
          info.label.setText(theEvent.getController().getInfo());
          cursor(HAND);
          break;
          case(ControlP5.ACTION_LEAVE):
          case(ControlP5.ACTION_RELEASEDOUTSIDE):
          info.n = 0;
          cursor(ARROW);
          break;
        }
      }
    };

    // add the above callback to controlP5
    cp5.addCallback(cb);

    // add another callback to slider s1, callback event will only be invoked for this 
    // particular controller.
    s1.addCallback(new CallbackListener() {
      public void controlEvent(CallbackEvent theEvent) {
        if (theEvent.getAction()==ControlP5.ACTION_BROADCAST) {
          s2.setValue(s2.getMax() - s1.getValue());
        }
      }
    }
    );
  }

  public void draw() {
    background(0);
    info.update();
  }


  public void controlEvent(ControlEvent theEvent) {
    println("Got a ControlEvent for "+theEvent.name()+" = "+theEvent.value());
    info.label.setText(theEvent.getController().getInfo());
  }

  public void keyPressed() {
    // uncomment the line below to remove callback cb from controlP5
    // when a key is pressed.
    //controlP5.removeCallback(cb);
  }

  // controlEvent(CallbackEvent) is called whenever a callback 
  // has been triggered. controlEvent(CallbackEvent) is detected by 
  // controlP5 automatically.
  public void controlEvent(CallbackEvent theEvent) {
    if (theEvent.getController().equals(s2)) {
      switch(theEvent.getAction()) {
        case(ControlP5.ACTION_ENTER): 
        println("Action:ENTER");
        break;
        case(ControlP5.ACTION_LEAVE): 
        println("Action:LEAVE");
        break;
        case(ControlP5.ACTION_PRESSED): 
        println("Action:PRESSED");
        break;
        case(ControlP5.ACTION_RELEASED): 
        println("Action:RELEASED");
        break;
        case(ControlP5.ACTION_RELEASEDOUTSIDE): 
        println("Action:RELEASED OUTSIDE");
        break;
        case(ControlP5.ACTION_BROADCAST): 
        println("Action:BROADCAST");
        break;
      }
    }
  }



  public class Info {
    float a;
    float n = 0;
    String txt = "";
    Textarea label;
    
    Info() {
      label = cp5.addTextarea("Hello\nWorld")
                 .setSize(200,200)
                 .setPosition(300,40)
                 .setColor(color(255))
                 .setColorBackground(color(100,0))
                 .setLineHeight(12);
                     
    }
    
    void update() {
      a += (n-a)*0.1;
      label.setColorBackground(color(100,255*a));
    }
  }


  /*
  a list of all methods available for the CallbackEvent Controller
   use ControlP5.printPublicMethodsFor(CallbackEvent.class);
   to print the following list into the console.
   
   You can find further details about class CallbackEvent in the javadoc.
   
   Format:
   ClassName : returnType methodName(parameter type)
   
   
   controlP5.CallbackEvent : Controller getController() 
   controlP5.CallbackEvent : int getAction() 
   java.lang.Object : String toString() 
   java.lang.Object : boolean equals(Object) 
   
   
   */
  
  public static void main(String args[]) {
    PApplet.main(new String[] { callback.class.getName() });
  }
}