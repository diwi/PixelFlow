/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.accelerationstructures;

import java.util.Arrays;

public class DwCollisionGrid{
  
  private float CELL_SIZE = 10f;
  private int   GRID_X; 
  private int   GRID_Y;

  private int               HEAD_PTR;
  private int[]             HEAD = new int[0];
  private int[]             NEXT = new int[0];
  private DwCollisionObject[] DATA = new DwCollisionObject[0];

  public DwCollisionGrid(){
  }
  
  public DwCollisionGrid(float[] bounds, float max_radius){
    init(bounds, max_radius);
  }
  
  
  public void init(float[] bounds_, float max_radius){
    CELL_SIZE = max_radius * 2;
    bounds = bounds_;
    int gx = (int) Math.ceil((bounds[3] - bounds[0])/CELL_SIZE)+1;
    int gy = (int) Math.ceil((bounds[4] - bounds[1])/CELL_SIZE)+1;
    int ppll_len = gx * gy * 4; // just a guess
    
    // 1) resize if necessary
    resize(gx, gy, ppll_len);
  }
  
  private void resize(int gx, int gy, int PPLL_size){
    
    // HEAD pointers
    if( (gx * gy) > HEAD.length){
      HEAD = new int[gx * gy];
//      System.out.println("CollisionGridAccelerator.resize -> HEAD: "+gx+", "+gy);
    }

    // NEXT pointers, DATA array
    if(PPLL_size > NEXT.length){
      int size_new = (int)(PPLL_size * 1.2f);
      NEXT = new int              [size_new];
      DATA = new DwCollisionObject[size_new];
//      System.out.println("CollisionGridAccelerator.resize -> NEXT/DATA: "+size_new+", "+PPLL_size);
    }
    
    // clear NEXT pointers
    for(int i = 0; i < HEAD.length; i++) HEAD[i] = 0;
//    for(int i = 0; i < NEXT.length; i++) NEXT[i] = 0;
//    for(int i = 0; i < DATA.length; i++) DATA[i] = null; 

    // reset HEAD pointer
    HEAD_PTR = 0;
    
    // set grid size
    GRID_X = gx;
    GRID_Y = gy;
  }
  
  
  
  public void insertRealloc(DwCollisionObject object){
    int TMP_HEAD_PTR = HEAD_PTR;
    insert(object);
    
    // resize if necessary
    if(HEAD_PTR > NEXT.length){
      // push
      int[]               TMP_HEAD = Arrays.copyOf(HEAD, HEAD.length);
      int[]               TMP_NEXT = Arrays.copyOf(NEXT, NEXT.length);
      DwCollisionObject[] TMP_DATA = Arrays.copyOf(DATA, DATA.length);
      
      // realloc
      resize(GRID_X, GRID_Y, HEAD_PTR);
      
      // pop
      HEAD_PTR = TMP_HEAD_PTR;
      System.arraycopy(TMP_HEAD, 0, HEAD, 0, TMP_HEAD.length);
      System.arraycopy(TMP_NEXT, 0, NEXT, 0, TMP_NEXT.length);
      System.arraycopy(TMP_DATA, 0, DATA, 0, TMP_DATA.length);
      
      // insert again
      insert(object);
    }
  }
  
  
//  public void testCollision
  
  
  public void insert(DwCollisionObject object){
    float pr = object.radCollision();
    float px = object.x();
    float py = object.y();
    
    px -= bounds[0];
    py -= bounds[1];
    
    int xmin = (int)((px-pr)/CELL_SIZE);  xmin = Math.max(xmin, 0);
    int xmax = (int)((px+pr)/CELL_SIZE);  xmax = Math.min(xmax, GRID_X-1);
    int ymin = (int)((py-pr)/CELL_SIZE);  ymin = Math.max(ymin, 0);
    int ymax = (int)((py+pr)/CELL_SIZE);  ymax = Math.min(ymax, GRID_Y-1);
    
    int count = (xmax - xmin + 1) * (ymax - ymin + 1);
    if(HEAD_PTR + count > NEXT.length){
      HEAD_PTR += count; // prepare for reallocation
      return;
    }

    for(int y = ymin; y <= ymax ; y++){
      for(int x = xmin; x <= xmax ; x++){
        int gid = y * GRID_X + x;
        int new_head = HEAD_PTR++;
        int old_head = HEAD[gid]; HEAD[gid] = new_head; // xchange head pointer
        NEXT[new_head] = old_head;
        DATA[new_head] = object;
      }
    }
  }
  

  
  
  private void create(DwCollisionObject[] particles, int num_particles){
    for(int i = 0; i < num_particles; i++){
      insert(particles[i]);
    }
  }
  
  
  public void solveCollision(DwCollisionObject object){
    float pr = object.radCollision();
    float px = object.x();
    float py = object.y();
    
    px -= bounds[0];
    py -= bounds[1];
    
    int xmin = (int)((px-pr)/CELL_SIZE);  xmin = Math.max(xmin, 0);
    int xmax = (int)((px+pr)/CELL_SIZE);  xmax = Math.min(xmax, GRID_X-1);
    int ymin = (int)((py-pr)/CELL_SIZE);  ymin = Math.max(ymin, 0);
    int ymax = (int)((py+pr)/CELL_SIZE);  ymax = Math.min(ymax, GRID_Y-1);

    for(int y = ymin; y <= ymax ; y++){
      for(int x = xmin; x <= xmax ; x++){
        int gid = y * GRID_X + x;
        int head = HEAD[gid];
        while(head > 0){
          DwCollisionObject othr = DATA[head];
          object.update(othr);  
          head = NEXT[head];
        }
      }
    }
  }
  
  
  
  private void solveCollisions(DwCollisionObject[] particles, int num_particles){
    
    // reset states
    for(int i = 0; i < num_particles; i++){
      particles[i].resetCollisionPtr();
    }
    
    // solve collisions
    for(int i = 0; i < num_particles; i++){
      solveCollision(particles[i]);  
    }
  }
  
  
  public float[] bounds = new float[6];
  
  public void computeBounds(DwCollisionObject[] particles, int num_particles){ 
    float x_min = +Float.MAX_VALUE;
    float y_min = +Float.MAX_VALUE;
    float z_min=  +Float.MAX_VALUE;
    float x_max = -Float.MAX_VALUE;
    float y_max = -Float.MAX_VALUE;
    float z_max = -Float.MAX_VALUE;
    
    CELL_SIZE = 1;
    
    float r_sum = 0;
    
    for(int i = 0; i < num_particles; i++){
      float x = particles[i].x();
      float y = particles[i].y();
      float r = particles[i].radCollision();
      r_sum += r;
      
      if(x-r < x_min) x_min = x-r;
      if(x+r > x_max) x_max = x+r;
      if(y-r < y_min) y_min = y-r;
      if(y+r > y_max) y_max = y+r;
      
      // use max radius
//      if(r*2 > CELL_SIZE) CELL_SIZE = r*2; 
    }
    
    bounds[0] = x_min;
    bounds[1] = y_min;
    bounds[2] = z_min;
    bounds[3] = x_max;
    bounds[4] = y_max;
    bounds[5] = z_max;
    CELL_SIZE = (r_sum * 2) /particles.length;
  }
  

  
  
  public void updateCollisions(DwCollisionObject[] particles){
    updateCollisions(particles, particles.length);
  }
  
  public void updateCollisions(DwCollisionObject[] particles, int num_particles){

    // 0) prepare dimensions, size,
    computeBounds(particles, num_particles);
    int gx = (int) Math.ceil((bounds[3] - bounds[0])/CELL_SIZE)+1;
    int gy = (int) Math.ceil((bounds[4] - bounds[1])/CELL_SIZE)+1;
    int ppll_len = particles.length * 4 + 1; // just a guess
    
    // 1) resize if necessary
    resize(gx, gy, ppll_len);
    
    // 2) create per-pixel-linked-list (PPLL)
    create(particles, num_particles);
    
    // resize if necessary
    if(HEAD_PTR > NEXT.length){
      resize(gx, gy, HEAD_PTR);
      create(particles, num_particles);
    }
    
    // 3) solve collisions for each particle
    solveCollisions(particles, num_particles);
  }

  
  

  

  
  
}