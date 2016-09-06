package tests.graph;


import processing.core.*;


public class valueMapping extends PApplet {
  


  
  public void settings() {
    size(800, 800);
 
    smooth(4);
  }
  
  public void setup() {
    noStroke();
    background(250);
    create();
    
    float harris;
    float HC = 0.04f;
    
    float dx = 5;
    float dy = -5;
    
    float dxx = dx*dx;
    float dyy = dy*dy;
    float dxy = dx*dy;

    float sum_dxy = dxx + dyy;
    
    System.out.println((dxx * dyy - (dxy * dxy)));
    

    // This is the Noble variant on the Harris detector, from 
    // Alison Noble, "Descriptions of Image Surfaces", PhD thesis, Department of Engineering Science, Oxford University 1989, p45.     
    harris = (dxx * dyy - (dxy * dxy)) / sum_dxy;

    System.out.println(harris);
    // Original Harris detector
    harris = (dxx * dyy - (dxy * dxy)) - (sum_dxy * sum_dxy * HC);
    System.out.println(harris);
  }


  public void draw() {
  }
  
  public void create(){
 
    int num_values = 100;
    
    float w = width;
    float h = height;
    
    int grid = 10;
    stroke(200);
    strokeWeight(1);
    for(int iy = 0; iy < grid+1; iy++){
      for(int ix = 0; ix < grid+1; ix++){
        
        float x = ix * w / (float) grid;
        float y = iy * h / (float) grid;
        
        line(x, 0, x, h);
        line(0, y, w, y);
      }
      
    }
    
    line(0,0, w,h);
    
    for(int i = 0; i < num_values; i++){
      
      float val_old = i * 1f / num_values;
      float val_new = (float) fx(val_old, 1/5.0);

      float px = val_old * w;
      float py = val_new * h;
      
      stroke(0);
      strokeWeight(4);
      point(px, py);
    }
    
    
  }
  
  
  public double fx(double val, double exp){
    double val_new = val;
    val_new = Math.pow(val_new, exp);
    return val_new;
  }
  



  public static void main(String args[]) {
    PApplet.main(new String[] { valueMapping.class.getName() });
  }
}