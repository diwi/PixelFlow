/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */




package OpticalFlow_MovieSettings;



import java.util.Locale;

import com.thomasdiewald.pixelflow.java.OpticalFlow;
import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.filter.Filter;

import controlP5.CheckBox;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.video.Capture;
import processing.video.Movie;


public class OpticalFlow_MovieSettings extends PApplet {
 
  

  
  int view_w = 1280;
  int view_h = 720;
  int view_x = 230;
  int view_y = 0;
  
  
  int gui_w = 200;

  int pg_movie_w = view_w - gui_w;
  int pg_movie_h = view_h;
  
  int movie_w = 0;
  int movie_h = 0;
  
  //main library context
  PixelFlow context;
  
  // optical flow
  OpticalFlow opticalflow;
  
  // buffer for the capture-image
  PGraphics2D pg_cam_a, pg_cam_b; 

  // offscreen render-target
  PGraphics2D pg_oflow;
  
  // Movie
  Movie movie;
  TimeLine timeline;
  
  PFont font;
  
  // some state variables for the GUI/display
  boolean APPLY_GRAYSCALE = true;
  boolean APPLY_BILATERAL = true;
  int     VELOCITY_LINES  = 6;
  
  
  public void settings() {
    size(view_w, view_h, P2D);
    smooth(4);
  }

  public void setup() {
    
    surface.setLocation(view_x, view_y);
    
    // main library context
    context = new PixelFlow(this);
    context.print();
    context.printGL();
      
    // OF
    opticalflow = new OpticalFlow(context, pg_movie_w, pg_movie_h);
    
   
    pg_cam_a = (PGraphics2D) createGraphics(pg_movie_w, pg_movie_h, P2D);
    pg_cam_a.noSmooth();
    
    pg_cam_b = (PGraphics2D) createGraphics(pg_movie_w, pg_movie_h, P2D);
    pg_cam_b.noSmooth();
    
    pg_oflow = (PGraphics2D) createGraphics(pg_movie_w, pg_movie_h, P2D);
    pg_oflow.smooth(4);
    
    
//    movie = new Movie(this, "examples/data/GoPro_ Owl Dance-Off.mp4");
    movie = new Movie(this, "examples/data/Pulp_Fiction_Dance_Scene.mp4");
//    movie = new Movie(this, "examples/data/Michael Jordan Iconic Free Throw Line Dunk.mp4");
    movie.loop();
    
    timeline = new TimeLine(movie);
    timeline.setPosition(0, height-20, pg_movie_w, 20);
    
    // processing font
    font = createFont("SourceCodePro-Regular.ttf", 12);
        
    createGUI();
    
    background(0);
    frameRate(60);
  }
  

  

  

  public void draw() {
    
    if( movie.available() ){
      movie.read();
      
      movie_w = movie.width;
      movie_h = movie.height;
      
      float mov_w_fit = pg_movie_w;
      float mov_h_fit = (pg_movie_w/(float)movie_w) * movie_h;
      
      if(mov_h_fit > pg_movie_h){
        mov_h_fit = pg_movie_h;
        mov_w_fit = (pg_movie_h/(float)movie_h) * movie_w;
      }
      
      
      
      // render to offscreenbuffer
      pg_cam_a.beginDraw();
      pg_cam_a.background(0);
      pg_cam_a.imageMode(CENTER);
      pg_cam_a.pushMatrix();
      pg_cam_a.translate(pg_movie_w/2f, pg_movie_h/2f);
      pg_cam_a.scale(0.95f);
      pg_cam_a.image(movie, 0, 0, mov_w_fit, mov_h_fit);
      pg_cam_a.popMatrix();
    
      pg_cam_a.endDraw();
      
      // apply filters (not necessary)
      if(APPLY_GRAYSCALE){
        Filter.get(context).luminance.apply(pg_cam_a, pg_cam_a);
      }
      if(APPLY_BILATERAL){
        Filter.get(context).bilateral.apply(pg_cam_a, pg_cam_b, 5, 0.10f, 4);
        swapCamBuffer();
      }
      

      // update Optical Flow
      opticalflow.update(pg_cam_a);
      
    }
    

    
  

    
    // render Optical Flow
    pg_oflow.beginDraw();
    pg_oflow.clear();
    pg_oflow.image(pg_cam_a, 0, 0);
    pg_oflow.endDraw();

    // add flow-vectors to the image
    if(opticalflow.param.display_mode == 2){
      opticalflow.renderVelocityShading(pg_oflow);
    }
    opticalflow.renderVelocityStreams(pg_oflow, VELOCITY_LINES);
    
    // display result
    background(0);
    image(pg_oflow, 0, 0);
    
    timeline.draw(mouseX, mouseY);
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", view_w, view_h, opticalflow.UPDATE_STEP, frameRate);
    surface.setTitle(txt_fps);
   
  }
  
  
  public void mouseMoved(){

  }
  
  public void mouseReleased(){
    if(timeline.inside(mouseX, mouseY)){
      float movie_pos = map(mouseX, 0, pg_cam_a.width, 0, movie.duration());
      movie.jump(movie_pos);
      System.out.println(movie_pos);
    }
  }

  
  class TimeLine{
    float x, y, w, h;
    Movie movie;
    public TimeLine(Movie movie){
      this.movie = movie;

    }
    
    public void setPosition(float x, float y, float w, float h){
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }
    
    public boolean inside(float mx, float my){
      return mx >= x && mx <= (x+w) && my >= y && my <= (y+h);
    }
    
    
    
    public void draw(float mx, float my){
      float time      = movie.time();
      float duration  = movie.duration();
      float movie_pos = w * time / duration;
      String time_str = String.format(Locale.ENGLISH, "%1.2f", time);
      
      // timeline
      fill(64, 200);
      noStroke();
      rect(x, y, w, h);
      
      // time handle
      fill(200, 200);
      rect(x+movie_pos-25, y, 50, 20, 8);
      
      // time, as text in seconds
      fill(0);
      textFont(font);
      textAlign(CENTER, CENTER);
      text(time_str, x + movie_pos, y + h/2 - 2);
      
      if(inside(mx, my)){
        float hoover_pos = duration * (mx - x) / w;
        String hoover_str = String.format(Locale.ENGLISH, "%1.2f", hoover_pos);
        
        // time handle
        fill(200, 50);
        rect(mx-25, y, 50, 20, 8);
        
        // time, as text in seconds
        fill(200, 100);
        textFont(font);
        textAlign(CENTER, CENTER);
        text(hoover_str, mx, y + h/2 - 2);
      }
      
      
      
      
    }
  }
  
  
  void swapCamBuffer(){
    PGraphics2D tmp = pg_cam_a;
    pg_cam_a = pg_cam_b;
    pg_cam_b = tmp;
  }
  
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    int sx = 100, sy = 14;
    int px = 10, py = 20, oy = 20;
    
    Group group_oflow = cp5.addGroup("OpticalFlow")
    .setPosition(width-gui_w, 20).setHeight(20).setWidth(gui_w)
    .setBackgroundHeight(view_h).setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_oflow.getCaptionLabel().align(LEFT, CENTER);
    
    cp5.addSlider("blur input").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 30).setValue(opticalflow.param.blur_input)
    .plugTo(opticalflow.param, "blur_input").linebreak();
    
    cp5.addSlider("blur flow").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 10).setValue(opticalflow.param.blur_flow)
    .plugTo(opticalflow.param, "blur_flow").linebreak();
    
    cp5.addSlider("temporal smooth").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 1).setValue(opticalflow.param.temporal_smoothing)
    .plugTo(opticalflow.param, "temporal_smoothing").linebreak();
    
    cp5.addSlider("flow scale").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 200f).setValue(opticalflow.param.flow_scale)
    .plugTo(opticalflow.param, "flow_scale").linebreak();

    cp5.addSlider("threshold").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=oy)
    .setRange(0, 2.0f).setValue(opticalflow.param.threshold)
    .plugTo(opticalflow.param, "threshold").linebreak();
    
    
    cp5.addRadio("setDisplayMode").setGroup(group_oflow).setSize(18, 18).setPosition(px, py+=oy)
        .setSpacingColumn(40).setSpacingRow(2).setItemsPerRow(3)
        .addItem("dir", 0)
        .addItem("normal", 1)
        .addItem("Shading", 2)
        .activate(opticalflow.param.display_mode);
    
    cp5.addSlider("line density").setGroup(group_oflow).setSize(sx, sy).setPosition(px, py+=(int)(18*1.5f))
    .setRange(1, 10).setValue(VELOCITY_LINES)
    .plugTo(this, "VELOCITY_LINES").linebreak();
    
    
    cp5.addSpacer("display").setGroup(group_oflow).setPosition(px, py+=oy);

    CheckBox cb = cp5.addCheckBox("activeFilters").setGroup(group_oflow).setSize(18, 18).setPosition(px, py+=oy)
    .setItemsPerRow(1).setSpacingColumn(3).setSpacingRow(3)
    .addItem("grayscale"       , 0)
    .addItem("bilateral filter", 0)
    ;
    
    if(APPLY_GRAYSCALE) cb.activate(0);
    if(APPLY_BILATERAL) cb.activate(1);
    


    group_oflow.open();
  }
  
  
  public void setDisplayMode(int val){
    opticalflow.param.display_mode = val;
  }

  public void activeFilters(float[] val){
    APPLY_GRAYSCALE = (val[0] > 0);
    APPLY_BILATERAL = (val[1] > 0);
  }
  
  

  public static void main(String args[]) {
    PApplet.main(new String[] { OpticalFlow_MovieSettings.class.getName() });
  }
}