package Miscellaneous.MultipleWindows;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import processing.core.PApplet;
import processing.opengl.PGraphics2D;

public class MultipleWindows extends PApplet {

  //
  // Simple demo to test multiple resizeable PApplet Windows.
  //
   
  DwPixelFlow context;
  
  ChildApplet childA;
  ChildApplet childB;
  ChildApplet childC;

  public void settings() {
    size(400, 300, P2D);
    smooth(0);
    
    System.out.println("creating window 0");
  }

  public void setup() {
    childA = new ChildApplet(1, 700,   0, 400, 300);
    childB = new ChildApplet(2, 300, 300, 400, 300);
    childC = new ChildApplet(3, 700, 300, 400, 300);
    
    childA.fill_color = color(255,  0,  0);
    childB.fill_color = color(  0,255,  0);
    childC.fill_color = color(255,255,  0);
   
    surface.setLocation(300, 0);
    surface.setResizable(true);
    
    
    context = new DwPixelFlow(this);
    context.print();
    context.printGL();
  }
  
  
  public void resize(){
    // resize textures, etc...
  }
  

  public void draw() {
    
    resize();
    
    background(32);

    fill(160);
    textAlign(CENTER, CENTER);
    text("main window", width/2, height/2);
    
    String txt = String.format("Window 0   %6.2fps", frameRate);
    surface.setTitle(txt);
  }


  
  
  static class ChildApplet extends PApplet {
    
    int window_id, vx, vy, vw, vh;
    int fill_color;
    
    PGraphics2D pga, tmp;
    DwPixelFlow context;
    
    public ChildApplet(int window_id, int vx, int vy, int vw, int vh) {
      super();
      this.window_id = window_id;
      this.vx = vx;
      this.vy = vy;
      this.vw = vw;
      this.vh = vh;
      
      PApplet.runSketch(new String[] { this.getClass().getName() }, this);
    }

    public void settings() {
      size(vw, vh, P2D);
      smooth(0);
      
      System.out.println("creating window "+window_id);
    }

    public void setup() {
      surface.setLocation(vx, vy);
      surface.setResizable(true);

      context = new DwPixelFlow(this);

      pga = (PGraphics2D) createGraphics(width, height, P2D);
      pga.smooth(0);
      
      tmp = (PGraphics2D) createGraphics(width, height, P2D);
      tmp.smooth(0);
    }
    
    public void resize(){
      DwUtils.changeTextureSize(pga, width, height);
      DwUtils.changeTextureSize(tmp, width, height);
    }

    public void draw() {
      
      resize();
      
      pga.beginDraw();
      pga.background(32);
      pga.rectMode(CENTER);
      pga.fill(fill_color);
      pga.rect(mouseX, mouseY, 50, 50);
      pga.endDraw();
      
      int blur_radius = 5 + window_id * 10;
      DwFilter.get(context).gaussblur.apply(pga, pga, tmp, blur_radius);
      
      image(pga, 0, 0);
      
      fill(160);
      textAlign(CENTER, CENTER);
      text("window "+window_id, width/2, height/2);
      
      String txt = String.format("Window %d   %6.2fps", window_id, frameRate);
      surface.setTitle(txt);
    }
  }

  
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { MultipleWindows.class.getName() });
  }
}