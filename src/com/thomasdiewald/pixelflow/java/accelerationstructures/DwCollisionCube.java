/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.accelerationstructures;


public class DwCollisionCube{
  
  private float CELL_SIZE = 10f;
  private int   GRID_X; 
  private int   GRID_Y;
  private int   GRID_Z;
  private int               HEAD_PTR;
  private int[]             HEAD = new int[0];
  private int[]             NEXT = new int[0];
  private DwCollisionObject[] DATA = new DwCollisionObject[0];

  public DwCollisionCube(){
  }
  
  private void resize(int gx, int gy, int gz, int PPLL_size){
    
    // HEAD pointers
    if( (gx * gy * gz) > HEAD.length){
      HEAD = new int[gx * gy * gz];
//      System.out.println("CollisionGridAccelerator.resize -> HEAD: "+gx+", "+gy+", "+gz);
    }

    // NEXT pointers, DATA array
    if(PPLL_size > NEXT.length){
      int size_new = (int)(PPLL_size * 1.2f);
      NEXT = new int            [size_new];
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
    GRID_Z = gz;
  }
  
  
  
  private void create(DwCollisionObject[] particles, int num_particles){

    for(int i = 0; i < num_particles; i++){
      DwCollisionObject particle = particles[i];
      float pr = particle.radCollision();
      float px = particle.x();
      float py = particle.y();
      float pz = particle.z();
      
      px -= bounds[0];
      py -= bounds[1];
      pz -= bounds[2];
      
      int xmin = (int)((px-pr)/CELL_SIZE); // xmin = Math.max(xmin, 0);
      int xmax = (int)((px+pr)/CELL_SIZE); // xmax = Math.min(xmax, GRID_X-1);
      int ymin = (int)((py-pr)/CELL_SIZE); // ymin = Math.max(ymin, 0);
      int ymax = (int)((py+pr)/CELL_SIZE); // ymax = Math.min(ymax, GRID_Y-1);
      int zmin = (int)((pz-pr)/CELL_SIZE); // zmin = Math.max(zmin, 0);
      int zmax = (int)((pz+pr)/CELL_SIZE); // zmax = Math.min(zmax, GRID_Z-1);
      for(int z = zmin; z <= zmax ; z++){
        for(int y = ymin; y <= ymax ; y++){
          for(int x = xmin; x <= xmax ; x++){
            int gid = z * GRID_X * GRID_Y + y * GRID_X + x;
            int new_head = HEAD_PTR++;
            int old_head = HEAD[gid]; HEAD[gid] = new_head; // xchange head pointer
            if(new_head < NEXT.length){
              NEXT[new_head] = old_head;
              DATA[new_head] = particle;
            } else {
              // keep counting for reallocation size
            }
          }
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
      DwCollisionObject particle = particles[i];

      float pr = particle.radCollision();
      float px = particle.x();
      float py = particle.y();
      float pz = particle.z();
      
      px -= bounds[0];
      py -= bounds[1];
      pz -= bounds[2];
      
      int xmin = (int)((px-pr)/CELL_SIZE); // xmin = Math.max(xmin, 0);
      int xmax = (int)((px+pr)/CELL_SIZE); // xmax = Math.min(xmax, GRID_X-1);
      int ymin = (int)((py-pr)/CELL_SIZE); // ymin = Math.max(ymin, 0);
      int ymax = (int)((py+pr)/CELL_SIZE); // ymax = Math.min(ymax, GRID_Y-1);
      int zmin = (int)((pz-pr)/CELL_SIZE); // zmin = Math.max(zmin, 0);
      int zmax = (int)((pz+pr)/CELL_SIZE); // zmax = Math.min(zmax, GRID_Z-1);
      for(int z = zmin; z <= zmax ; z++){
        for(int y = ymin; y <= ymax ; y++){
          for(int x = xmin; x <= xmax ; x++){
            int gid = z * GRID_X * GRID_Y + y * GRID_X + x;
            int head = HEAD[gid];
            while(head > 0){
              DwCollisionObject othr = DATA[head];
              particle.update(othr);  
              head = NEXT[head];
            }
          }
        }
        
      }
        
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
      float z = particles[i].z();
      float r = particles[i].radCollision();
      r_sum += r;
      
      if(x-r < x_min) x_min = x-r;
      if(x+r > x_max) x_max = x+r;
      if(y-r < y_min) y_min = y-r;
      if(y+r > y_max) y_max = y+r;
      if(z-r < z_min) z_min = z-r;
      if(z+r > z_max) z_max = z+r;
      // use max radius
//      if(r > CELL_SIZE) CELL_SIZE = r; 
    }
    
    bounds[0] = x_min;
    bounds[1] = y_min;
    bounds[2] = z_min;
    bounds[3] = x_max;
    bounds[4] = y_max;
    bounds[5] = z_max;
//    CELL_SIZE *= 2;
    CELL_SIZE = (r_sum * 2) / particles.length;
  }
  

  
  
  public void updateCollisions(DwCollisionObject[] particles){
    updateCollisions(particles, particles.length);
  }
  
  public void updateCollisions(DwCollisionObject[] particles, int num_particles){

    // 0) prepare dimensions, size,
    computeBounds(particles, num_particles);
    int gx = (int) Math.ceil((bounds[3] - bounds[0] +1)/CELL_SIZE);
    int gy = (int) Math.ceil((bounds[4] - bounds[1] +1)/CELL_SIZE);
    int gz = (int) Math.ceil((bounds[5] - bounds[2] +1)/CELL_SIZE);
    int ppll_len = particles.length * 4 + 1; // just a guess
    
    // 1) resize if necessary
    resize(gx, gy, gz, ppll_len);
    
    // 2) create per-pixel-linked-list (PPLL)
    create(particles, num_particles);
    
    // resize if necessary
    if(HEAD_PTR > NEXT.length){
      resize(gx, gy, gz, HEAD_PTR);
      create(particles, num_particles);
    }
    
    // 3) solve collisions for each particle
    solveCollisions(particles, num_particles);
  }

  
}