package com.thomasdiewald.pixelflow.java.softbodydynamics.softbody;

import java.util.Random;

import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;
import com.thomasdiewald.pixelflow.java.utils.DwStrokeStyle;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;
import processing.opengl.PGraphics2D;


public class DwSoftGrid3D extends DwSoftBody3D{
  
  // specific attributes for this body
  public int   nodes_x;
  public int   nodes_y;
  public int   nodes_z;
  public float nodes_r;
  
  private float tx_inv;
  private float ty_inv;
  private float tz_inv;
  
  
  public PGraphics2D texture_XYp = null;
  public PGraphics2D texture_XYn = null;
  public PGraphics2D texture_YZp = null;
  public PGraphics2D texture_YZn = null;
  public PGraphics2D texture_XZp = null;
  public PGraphics2D texture_XZn = null;
  
  public int bend_spring_mode = 0;
  public int bend_spring_dist = 3; // try other values, it affects the objects stiffness

  Random rand;
  
  public DwSoftGrid3D(){
  }

  public void create(DwPhysics<DwParticle3D> physics, int nx, int ny, int nz, float nr, float start_x, float start_y, float start_z){
 
    this.physics            = physics;
    this.rand               = new Random(0);
    this.collision_group_id = physics.getNewCollisionGroupId();
    this.nodes_offset       = physics.getParticlesCount();
    this.nodes_x            = nx;
    this.nodes_y            = ny;
    this.nodes_z            = nz;
    this.nodes_r            = nr;
    this.num_nodes          = nodes_x * nodes_y * nodes_z;
    this.particles          = new DwParticle3D[num_nodes];
    
    

    int normal_count_XY = nodes_x * nodes_y;
    int normal_count_YZ = nodes_y * nodes_z;
    int normal_count_XZ = nodes_x * nodes_z;
      
    normals = new float[6][][];
    normals[0] = new float[normal_count_XY][3];
    normals[1] = new float[normal_count_XY][3];
    normals[2] = new float[normal_count_YZ][3];
    normals[3] = new float[normal_count_YZ][3];
    normals[4] = new float[normal_count_XZ][3];
    normals[5] = new float[normal_count_XZ][3];
 
    
    // for textcoord normalization
    this.tx_inv = 1f/(float)(nodes_x-1);
    this.ty_inv = 1f/(float)(nodes_y-1);
    this.tz_inv = 1f/(float)(nodes_z-1);
    
    DwParticle3D.MAX_RAD = Math.max(DwParticle3D.MAX_RAD, nr);

    // temp variables
    int idx, idx_world;
    int x, y, z, ox, oy, oz;
    float px, py, pz;
    float rand_scale = 0.1f;
  
    // 1) init particles
    for(z = 0; z < nodes_z; z++){
      for(y = 0; y < nodes_y; y++){
        for(x = 0; x < nodes_x; x++){
          idx            = (z * nodes_x * nodes_y) + (y * nodes_x) + x;
          idx_world      = idx + nodes_offset;
          px             = start_x + x * nodes_r * 2 + (rand.nextFloat()*2-1) * rand_scale;
          py             = start_y + y * nodes_r * 2 + (rand.nextFloat()*2-1) * rand_scale;
          pz             = start_z + z * nodes_r * 2 + (rand.nextFloat()*2-1) * rand_scale;
          particles[idx] = new CustomParticle3D(idx_world, px, py, pz, nodes_r);
          particles[idx].setParamByRef(param_particle);
          particles[idx].setRadiusCollision(nodes_r * collision_radius_scale);
          particles[idx].collision_group = collision_group_id;
          if(self_collisions){
            particles[idx].collision_group = physics.getNewCollisionGroupId();
          }
        }
      }
    }
    
    
    ox = bend_spring_dist;
    oy = bend_spring_dist;
    oz = bend_spring_dist;
    
    // 2) create springs
    for(z = 0; z < nodes_z; z++){
      for(y = 0; y < nodes_y; y++){
        for(x = 0; x < nodes_x; x++){
          if(CREATE_STRUCT_SPRINGS){
            addSpring(x, y, z, -1, 0, 0, DwSpringConstraint.TYPE.STRUCT);
            addSpring(x, y, z, +1, 0, 0, DwSpringConstraint.TYPE.STRUCT);
            addSpring(x, y, z,  0,-1, 0, DwSpringConstraint.TYPE.STRUCT);
            addSpring(x, y, z,  0,+1, 0, DwSpringConstraint.TYPE.STRUCT);
            addSpring(x, y, z,  0, 0,-1, DwSpringConstraint.TYPE.STRUCT);
            addSpring(x, y, z,  0, 0,+1, DwSpringConstraint.TYPE.STRUCT);
          }
                    
          if(CREATE_SHEAR_SPRINGS){
            addSpring(x, y, z, -1,-1, -1, DwSpringConstraint.TYPE.SHEAR);
            addSpring(x, y, z, +1,-1, -1, DwSpringConstraint.TYPE.SHEAR);
            addSpring(x, y, z, -1,+1, -1, DwSpringConstraint.TYPE.SHEAR);
            addSpring(x, y, z, +1,+1, -1, DwSpringConstraint.TYPE.SHEAR);
            
            addSpring(x, y, z, -1,-1, +1, DwSpringConstraint.TYPE.SHEAR);
            addSpring(x, y, z, +1,-1, +1, DwSpringConstraint.TYPE.SHEAR);
            addSpring(x, y, z, -1,+1, +1, DwSpringConstraint.TYPE.SHEAR);
            addSpring(x, y, z, +1,+1, +1, DwSpringConstraint.TYPE.SHEAR);
          }
          
          if(CREATE_BEND_SPRINGS && bend_spring_dist > 0){
            // diagonal
            if(bend_spring_mode == 0){
              addSpring(x, y, z, -ox, -oy, -oz, DwSpringConstraint.TYPE.BEND);
              addSpring(x, y, z, +ox, -oy, -oz, DwSpringConstraint.TYPE.BEND);
              addSpring(x, y, z, -ox, +oy, -oz, DwSpringConstraint.TYPE.BEND);
              addSpring(x, y, z, +ox, +oy, -oz, DwSpringConstraint.TYPE.BEND);
              
              addSpring(x, y, z, -ox, -oy, +oz, DwSpringConstraint.TYPE.BEND);
              addSpring(x, y, z, +ox, -oy, +oz, DwSpringConstraint.TYPE.BEND);
              addSpring(x, y, z, -ox, +oy, +oz, DwSpringConstraint.TYPE.BEND);
              addSpring(x, y, z, +ox, +oy, +oz, DwSpringConstraint.TYPE.BEND);
            }
            
//            // orthogonal
//            if(bend_spring_mode == 1){
//              addSpring(x, y, z, -ox,   0, SpringConstraint3D.TYPE.BEND);
//              addSpring(x, y, z, +ox,   0, SpringConstraint3D.TYPE.BEND);
//              addSpring(x, y, z,   0, +oy, SpringConstraint3D.TYPE.BEND);
//              addSpring(x, y, z,   0, -oy, SpringConstraint3D.TYPE.BEND);
//            }
//            
//            // random, 'kind of' anisotropic
//            if(bend_spring_mode == 2){
//              for(int i = 0; i < 8; i++){
//                ox = (int) Math.round((rand.nextFloat()*2-1) * bend_spring_dist);
//                oy = (int) Math.round((rand.nextFloat()*2-1) * bend_spring_dist);
//
//                addSpring(x, y, z, ox, oy, SpringConstraint3D.TYPE.BEND);
//              }
//            }
          }
          
        }
      }
    }
    
    
    // add new particles to the physics-world
    physics.addParticles(particles, num_nodes);
  }
  
 
  public DwParticle getNode(int x, int y, int z){
    if(x <        0 || y <        0 || z <        0) return null;
    if(x >= nodes_x || y >= nodes_y || z >= nodes_z) return null;

    int idx = (z * nodes_x * nodes_y) + (y * nodes_x) + x;
    return particles[idx];
  }
  
  public DwParticle3D getNode3D(int x, int y, int z){
    if(x <        0 || y <        0 || z <        0) return null;
    if(x >= nodes_x || y >= nodes_y || z >= nodes_z) return null;

    int idx = (z * nodes_x * nodes_y) + (y * nodes_x) + x;
    return particles[idx];
  }
  
  
  public void addSpring(int ax, int ay, int az, int offx, int offy, int offz, DwSpringConstraint.TYPE type){
    int bx = ax + offx;
    int by = ay + offy;
    int bz = az + offz;
    
    // clamp offset to grid-bounds
    if(bx < 0) bx = 0; else if(bx > nodes_x-1) bx = nodes_x-1;
    if(by < 0) by = 0; else if(by > nodes_y-1) by = nodes_y-1;
    if(bz < 0) bz = 0; else if(bz > nodes_z-1) bz = nodes_z-1;
    
    int ia = (az * nodes_x * nodes_y) + (ay * nodes_x) + ax;
    int ib = (bz * nodes_x * nodes_y) + (by * nodes_x) + bx;

//    DwSpringConstraint3D.addSpring(particles[ia], particles[ib], param_spring, type);
    DwSpringConstraint3D.addSpring(physics, particles[ia], particles[ib], param_spring, type);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // NORMALS
  //////////////////////////////////////////////////////////////////////////////
  
  private void computeNormalsXY(float[][] normals_ref, int iz){
    for(int iy = 0; iy < nodes_y; iy++){
      for(int ix = 0; ix < nodes_x; ix++){
        int idx = iy * nodes_x + ix;
        DwParticle3D pC = getNode3D(ix  , iy  , iz  );
        DwParticle3D pT = getNode3D(ix  , iy-1, iz  );
        DwParticle3D pB = getNode3D(ix  , iy+1, iz  );
        DwParticle3D pL = getNode3D(ix-1, iy  , iz  );
        DwParticle3D pR = getNode3D(ix+1, iy  , iz  );
        computeNormals(normals_ref[idx], pC, pT, pR, pB, pL);
      }
    }
  }
  
  private void computeNormalsYZ(float[][] normals_ref, int ix){
    for(int iz = 0; iz < nodes_z; iz++){
      for(int iy = 0; iy < nodes_y; iy++){
        int idx = iz * nodes_y + iy;
        DwParticle3D pC = getNode3D(ix  , iy  , iz  );
        DwParticle3D pT = getNode3D(ix  , iy  , iz-1);
        DwParticle3D pB = getNode3D(ix  , iy  , iz+1);
        DwParticle3D pL = getNode3D(ix  , iy-1, iz  );
        DwParticle3D pR = getNode3D(ix  , iy+1, iz  );
        computeNormals(normals_ref[idx], pC, pT, pR, pB, pL);
      }
    }
  }
  
  private void computeNormalsXZ(float[][] normals_ref, int iy){
    for(int iz = 0; iz < nodes_z; iz++){
      for(int ix= 0; ix < nodes_x; ix++){
        int idx = iz * nodes_x + ix;
        DwParticle3D pC = getNode3D(ix  , iy  , iz  );
        DwParticle3D pT = getNode3D(ix  , iy  , iz-1);
        DwParticle3D pB = getNode3D(ix  , iy  , iz+1);
        DwParticle3D pL = getNode3D(ix-1, iy  , iz  );
        DwParticle3D pR = getNode3D(ix+1, iy  , iz  );
        computeNormals(normals_ref[idx], pC, pT, pR, pB, pL);
      }
    }
  }
  

  public float[][][] normals;
  public float normal_dir = -1f;
  
//  private void computeNormals(float[] n, VerletParticle3D pC, 
//                                         VerletParticle3D pT,
//                                         VerletParticle3D pR,
//                                         VerletParticle3D pB,
//                                         VerletParticle3D pL)
//  {
//    n[0] = n[1] = n[2] = 0;
//    VerletParticle3D.crossAccum(pC, pT, pR, n);
//    VerletParticle3D.crossAccum(pC, pR, pB, n);
//    VerletParticle3D.crossAccum(pC, pB, pL, n);
//    VerletParticle3D.crossAccum(pC, pL, pT, n);
//
//    float dd_sq  = n[0]*n[0] +  n[1]*n[1] +  n[2]*n[2];
//    float dd_inv = normal_dir * 1f/(float)(Math.sqrt(dd_sq)+0.000001f);
//
//    n[0] *= dd_inv;
//    n[1] *= dd_inv;
//    n[2] *= dd_inv;  
//  }
  
  private void computeNormals(float[] n, DwParticle3D pC, DwParticle3D ... pN){
    n[0] = n[1] = n[2] = 0;
    
    for(int i = 0; i < pN.length-1; i++){
      DwParticle3D.crossAccum(pC, pN[i], pN[i+1], n);
    }
    DwParticle3D.crossAccum(pC, pN[pN.length-1], pN[0], n);
    
    float dd_sq  = n[0]*n[0] +  n[1]*n[1] +  n[2]*n[2];
    float dd_inv = normal_dir/(float)(Math.sqrt(dd_sq)+0.000001f);
    
    n[0] *= dd_inv;
    n[1] *= dd_inv;
    n[2] *= dd_inv;  
  }
  
  
  

  @Override
  public void computeNormals(){
    computeNormalsXY(normals[0],         0);
    computeNormalsXY(normals[1], nodes_z-1);
    computeNormalsYZ(normals[2],         0);
    computeNormalsYZ(normals[3], nodes_x-1);
    computeNormalsXZ(normals[4],         0);
    computeNormalsXZ(normals[5], nodes_y-1);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // DISPLAY
  //////////////////////////////////////////////////////////////////////////////

  private DwParticle3D lastp;
  private boolean degenerated = false;
  
  private final void vertex(PGraphics pg, DwParticle3D p, float[] n, float tu, float tv){
    if(p.all_springs_deactivated){
      degenerated = true;
      if(lastp != null){
        pg.vertex(lastp.cx,lastp.cy,lastp.cz, 0, 0);
      }
    } else {
      if(degenerated){
        pg.vertex(p.cx,p.cy,p.cz, 0, 0);
        pg.vertex(p.cx,p.cy,p.cz, 0, 0);
        degenerated = false;
      }
      pg.normal(n[0], n[1], n[2]); 
      pg.vertex(p.cx, p.cy, p.cz, tu, tv);
      lastp = p;
    }
  }
  
  private void displayGridXY(PGraphics pg, float[][] normals, int iz, PGraphics2D tex){
    pg.beginShape(PConstants.TRIANGLE_STRIP);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(tex);
    int ix, iy;
    for(iy = 0; iy < nodes_y-1; iy++){
      for(ix = 0; ix < nodes_x; ix++){
        vertex(pg, getNode3D(ix, iy+0, iz), normals[(iy+0)*nodes_x+ix], ix * tx_inv, (iy+0) * ty_inv);
        vertex(pg, getNode3D(ix, iy+1, iz), normals[(iy+1)*nodes_x+ix], ix * tx_inv, (iy+1) * ty_inv);
      }
      ix -= 1; vertex(pg, getNode3D(ix, iy+1, iz), normals[(iy+1)*nodes_x+ix], 0, 0);
      ix  = 0; vertex(pg, getNode3D(ix, iy+1, iz), normals[(iy+1)*nodes_x+ix], 0, 0);
    }
    pg.endShape();
  }

  
  private void displayGridYZ(PGraphics pg, float[][] normals, int ix, PGraphics2D tex){
    pg.beginShape(PConstants.TRIANGLE_STRIP);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(tex);
    int iz, iy;
    for(iz = 0; iz < nodes_z-1; iz++){
      for(iy = 0; iy < nodes_y; iy++){
        vertex(pg, getNode3D(ix, iy, iz+0), normals[(iz+0)*nodes_y+iy], iy * ty_inv, (iz+0) * tz_inv);
        vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_y+iy], iy * ty_inv, (iz+1) * tz_inv);
      }
      iy -= 1; vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_y+iy], 0, 0);
      iy  = 0; vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_y+iy], 0, 0);
    }
    pg.endShape();
  }
  
  
  private void displayGridXZ(PGraphics pg, float[][] normals, int iy, PGraphics2D tex){
    pg.beginShape(PConstants.TRIANGLE_STRIP);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(tex);
    int iz, ix;
    for(iz = 0; iz < nodes_z-1; iz++){
      for(ix = 0; ix < nodes_x; ix++){
        vertex(pg, getNode3D(ix, iy, iz+0), normals[(iz+0)*nodes_x+ix], ix * tx_inv, (iz+0) * tz_inv);
        vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_x+ix], ix * tx_inv, (iz+1) * tz_inv);
      }
      ix -= 1; vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_x+ix], 0, 0);
      ix  = 0; vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_x+ix], 0, 0);
    }
    pg.endShape();
  }
  
  
  
  
  
  
  
  
  
  private final void vertex(PShape pg, DwParticle3D p, float[] n, float tu, float tv){
    if(p.all_springs_deactivated){
      degenerated = true;
      if(lastp != null){
        pg.vertex(lastp.cx,lastp.cy,lastp.cz, 0, 0);
      }
    } else {
      if(degenerated){
        pg.vertex(p.cx,p.cy,p.cz, 0, 0);
        pg.vertex(p.cx,p.cy,p.cz, 0, 0);
        degenerated = false;
      }
      pg.normal(n[0], n[1], n[2]); 
      pg.vertex(p.cx, p.cy, p.cz, tu, tv);
      lastp = p;
    }
  }
  
  private void displayGridXY(PShape pg, float[][] normals, int iz, PGraphics2D tex){
    pg.beginShape(PConstants.TRIANGLE_STRIP);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(tex);
    pg.fill(material_color);
    int ix, iy;
    for(iy = 0; iy < nodes_y-1; iy++){
      for(ix = 0; ix < nodes_x; ix++){
        vertex(pg, getNode3D(ix, iy+0, iz), normals[(iy+0)*nodes_x+ix], ix * tx_inv, (iy+0) * ty_inv);
        vertex(pg, getNode3D(ix, iy+1, iz), normals[(iy+1)*nodes_x+ix], ix * tx_inv, (iy+1) * ty_inv);
      }
      ix -= 1; vertex(pg, getNode3D(ix, iy+1, iz), normals[(iy+1)*nodes_x+ix], 0, 0);
      ix  = 0; vertex(pg, getNode3D(ix, iy+1, iz), normals[(iy+1)*nodes_x+ix], 0, 0);
    }
    pg.endShape();
  }

  
  private void displayGridYZ(PShape pg, float[][] normals, int ix, PGraphics2D tex){
    pg.beginShape(PConstants.TRIANGLE_STRIP);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(tex);
    pg.fill(material_color);
    int iz, iy;
    for(iz = 0; iz < nodes_z-1; iz++){
      for(iy = 0; iy < nodes_y; iy++){
        vertex(pg, getNode3D(ix, iy, iz+0), normals[(iz+0)*nodes_y+iy], iy * ty_inv, (iz+0) * tz_inv);
        vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_y+iy], iy * ty_inv, (iz+1) * tz_inv);
      }
      iy -= 1; vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_y+iy], 0, 0);
      iy  = 0; vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_y+iy], 0, 0);
    }
    pg.endShape();
  }
  
  
  private void displayGridXZ(PShape pg, float[][] normals, int iy, PGraphics2D tex){
    pg.beginShape(PConstants.TRIANGLE_STRIP);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(tex);
    pg.fill(material_color);
    int iz, ix;
    for(iz = 0; iz < nodes_z-1; iz++){
      for(ix = 0; ix < nodes_x; ix++){
        vertex(pg, getNode3D(ix, iy, iz+0), normals[(iz+0)*nodes_x+ix], ix * tx_inv, (iz+0) * tz_inv);
        vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_x+ix], ix * tx_inv, (iz+1) * tz_inv);
      }
      ix -= 1; vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_x+ix], 0, 0);
      ix  = 0; vertex(pg, getNode3D(ix, iy, iz+1), normals[(iz+1)*nodes_x+ix], 0, 0);
    }
    pg.endShape();
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  private final void normal(PGraphics pg, DwParticle3D p, float[] n, float nlen){
    if(p.all_springs_deactivated) return;
    pg.vertex(p.cx          , p.cy          , p.cz          );
    pg.vertex(p.cx+n[0]*nlen, p.cy+n[1]*nlen, p.cz+n[2]*nlen);
  }
  
  
  private void displayNormalsXY(PGraphics pg, float[][] normals, int iz, float nlen){
    pg.beginShape(PConstants.LINES);
    for(int iy = 0; iy < nodes_y; iy++){
      for(int ix = 0; ix < nodes_x; ix++){
        normal(pg, getNode3D(ix, iy, iz), normals[iy * nodes_x + ix], nlen);
      }
    }
    pg.endShape();
  }
  
  private void displayNormalsYZ(PGraphics pg, float[][] normals, int ix, float nlen){
    pg.beginShape(PConstants.LINES);
    for(int iz = 0; iz < nodes_z; iz++){
      for(int iy = 0; iy < nodes_y; iy++){
        normal(pg, getNode3D(ix, iy, iz), normals[iz * nodes_y + iy], nlen);
      }
    }
    pg.endShape();
  }
  
  private void displayNormalsXZ(PGraphics pg, float[][] normals, int iy, float nlen){
    pg.beginShape(PConstants.LINES);
    for(int iz = 0; iz < nodes_z; iz++){
      for(int ix = 0; ix < nodes_x; ix++){
        normal(pg, getNode3D(ix, iy, iz), normals[iz * nodes_x + ix], nlen);
      }
    }
    pg.endShape();
  }
  

  
  private PShape createShape(PGraphics pg){
    PShape shp = pg.createShape(PConstants.GROUP);

    PShape[] shp_grid = new PShape[6];
    for(int i = 0; i < shp_grid.length; i++){
      shp_grid[i] = pg.createShape();
      shp.addChild(shp_grid[i]);
    }
    
    shp_grid[0].setName("gridXYp");
    shp_grid[1].setName("gridXYn");
    shp_grid[2].setName("gridYZp");
    shp_grid[3].setName("gridYZn");
    shp_grid[4].setName("gridXZp");
    shp_grid[5].setName("gridXZn");

                    displayGridXY(shp_grid[0], normals[0], 0        , texture_XYp);
    if(nodes_z > 1) displayGridXY(shp_grid[1], normals[1], nodes_z-1, texture_XYn);
                    displayGridYZ(shp_grid[2], normals[2], 0        , texture_YZp);
    if(nodes_x > 1) displayGridYZ(shp_grid[3], normals[3], nodes_x-1, texture_YZn);
                    displayGridXZ(shp_grid[4], normals[4], 0        , texture_XZp);
    if(nodes_y > 1) displayGridXZ(shp_grid[5], normals[5], nodes_y-1, texture_XZn);
    return shp;
  }
  
  
  
  @Override
  public void createShapeMesh(PGraphics pg){
    PShape shp = createShape(pg);
    shp.setStroke(false);
    setShapeMesh(pg.parent, shp);
  }


  @Override
  public void createShapeWireframe(PGraphics pg, DwStrokeStyle style){
    PShape shp = createShape(pg);
    
    shp.setTexture(null);
    shp.setFill(false);
    shp.setStroke(true);
    shp.setStroke(style.stroke_color);
    shp.setStrokeWeight(style.stroke_weight);
    
    setShapeWireframe(pg.parent, shp);
  }
  
   
  @Override
  public void displayNormals(PGraphics pg){
                    displayNormalsXY(pg, normals[0], 0        ,  display_normal_length);
    if(nodes_z > 1) displayNormalsXY(pg, normals[1], nodes_z-1, -display_normal_length);
                    displayNormalsYZ(pg, normals[2], 0        ,  display_normal_length);
    if(nodes_x > 1) displayNormalsYZ(pg, normals[3], nodes_x-1, -display_normal_length);
                    displayNormalsXZ(pg, normals[4], 0        , -display_normal_length); // y inverted
    if(nodes_y > 1) displayNormalsXZ(pg, normals[5], nodes_y-1, +display_normal_length); // y inverted
  }


}
  
  
  
 
  