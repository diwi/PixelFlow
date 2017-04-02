/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package com.thomasdiewald.pixelflow.java.rigid_origami;

import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSet;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.softbody.DwSoftBody3D;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PShape;


public abstract class DwFoldingModel{

  public String name;
  
  public DwIndexedFaceSet ifs;
  public DwFoldingTile[] tiles;
  public DwFoldingTile.TileData[] tile_defs;
  

  public abstract DwFoldingModel create(int num_x);
  public abstract DwFoldingModel create(int num_x, int num_y);

  
  public abstract void createNodeRadius(DwPhysics<DwParticle3D> physics, DwParticle3D[] particles);

  
  public void setName(String name){
    this.name = name;
  }
  
  public String getName(){
   return name;
  }
  
  public void createSprings(DwPhysics<DwParticle3D> physics, DwParticle3D[] particles, DwSpringConstraint.Param rigid, DwSpringConstraint.Param soft){ 
    for(int i = 0; i < tiles.length; i++){
      tiles[i].createSprings(physics, particles, rigid, soft);
    }
  }
  
  public void manipSprings(float spring_scale){
    for(int i = 0; i < tiles.length; i++){
      tiles[i].scaleSprings(spring_scale);
    }
  }
  
  public void setSpringScaleBounds(float springs_scale_min, float springs_scale_max){
    for(int i = 0; i < tiles.length; i++){
      tiles[i].setSpringScaleBounds(springs_scale_min, springs_scale_max);
    }
  }
  
  
  public void setTileDefinitions(DwFoldingTile.TileData ... tile_defs){
    this.tile_defs = tile_defs;
  }
  
  public void setTiles(DwFoldingTile[] ... tile_arrays){
    // count number of tiles
    int tiles_count = 0;
    for(DwFoldingTile[] tile_array : tile_arrays){
      tiles_count += tile_array.length;
    }
    
    // alloc tiles
    tiles = new DwFoldingTile[tiles_count];
    
    // copy to single array
    tiles_count = 0;
    for(DwFoldingTile[] tile_array : tile_arrays){
      System.arraycopy(tile_array, 0, tiles, tiles_count, tile_array.length);
      tiles_count += tile_array.length;
    }
  }
  

  
  public void computeDefaultNodeRadius(DwParticle3D[] particles, float collision_rad_scale){
    for(int i = 0; i < particles.length; i++){
      DwParticle3D particle = particles[i];
      int springs_count = particle.spring_count;
      DwSpringConstraint[] springs = particle.springs;
      
      int restlen_sum_count = 0;
      float restlen_avg = 0;
      float restlen_min = 0;
      for(int j = 0; j < springs_count; j++){
        if(springs[j].type == DwSpringConstraint.TYPE.STRUCT){
          float restlen_cur = springs[j].dd_rest * 0.5f;
          
          // restlen_sum
          restlen_avg += restlen_cur;
          restlen_sum_count++;
          
          // restlen_min
          if(restlen_min < restlen_cur){
            restlen_min = restlen_cur;
          }
        }
      }
      if(restlen_sum_count == 0){
        System.out.println("error, particle has no struct springs");
        continue;
      }
      restlen_avg /= restlen_sum_count;
      

      particle.setRadius(restlen_avg * collision_rad_scale);
//      particle.setRadiusCollision(restlen_avg * collision_rad_scale);
      
      DwParticle3D.MAX_RAD = Math.max(DwParticle3D.MAX_RAD, particle.rad);
    }
  }
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // FACE NORMALS
  //////////////////////////////////////////////////////////////////////////////
  
  
  public float[][] computeFaceNormals(float[][] normals){
    int       faces_count = ifs.getFacesCount();
    int  [][] faces       = ifs.getFaces();
    float[][] verts       = ifs.getVerts();
    
    if(normals == null || normals.length != faces_count){
      normals = new float[faces_count][3];
    }

    for(int i = 0; i < faces_count; i++){
      int[] face = faces[i];
      int i0 = face[0]; float[] v0 = verts[i0];
      int i1 = face[1]; float[] v1 = verts[i1];
      int i2 = face[2]; float[] v2 = verts[i2];

      float[] n = normals[i]; n[0] = n[1] = n[2] = 0;
      DwParticle3D.crossAccum(v0, v1, v2, n);
      
      float dd_sq = n[0]*n[0] + n[1]*n[1] + n[2]*n[2];
      float dd = 1f / (float) Math.sqrt(dd_sq);
      n[0] *= dd;
      n[1] *= dd;
      n[2] *= dd;
    }
    return normals;
  }
  
  
  
  public float[][] computeFaceNormals(float[][] normals, DwParticle3D[] particles){
    int     faces_count = ifs.getFacesCount();
    int[][] faces       = ifs.getFaces();
    
    if(normals == null || normals.length != faces_count){
      normals = new float[faces_count][3];
    }

    for(int i = 0; i < faces_count; i++){
      int[] face = faces[i];
      int i0 = face[0]; DwParticle3D v0 = particles[i0];
      int i1 = face[1]; DwParticle3D v1 = particles[i1];
      int i2 = face[2]; DwParticle3D v2 = particles[i2];

      float[] n = normals[i]; n[0] = n[1] = n[2] = 0;
      DwParticle3D.crossAccum(v0, v1, v2, n);
      
      float dd_sq = n[0]*n[0] + n[1]*n[1] + n[2]*n[2];
      float dd = 1f / (float) Math.sqrt(dd_sq);
      n[0] *= dd;
      n[1] *= dd;
      n[2] *= dd;
    }
    return normals;
  }


  
  

  
  
  //////////////////////////////////////////////////////////////////////////////
  // DISPLAY
  //////////////////////////////////////////////////////////////////////////////
  

  public void display(PGraphics pg){
    display(pg, null);
  }
  
  public void display(PGraphics pg, DwSoftBody3D softbody){
    pg.beginShape(PConstants.TRIANGLES);
    if(softbody != null){
      for(DwFoldingTile tile : tiles) tile.displayMesh(pg, softbody.particles);
    } else {
      for(DwFoldingTile tile : tiles) tile.displayMesh(pg, ifs);
    }
    pg.endShape();
  }
  
  public void display(PShape pg, DwSoftBody3D softbody){ 
    if(softbody != null){
      for(DwFoldingTile tile : tiles) tile.displayMesh(pg, softbody.particles);
    } else {
      for(DwFoldingTile tile : tiles) tile.displayMesh(pg, ifs);
    }
  }
  

  public void displayWireFrame(PGraphics pg, float strokeWeight){
    displayWireFrame(pg, null, strokeWeight);
  }
  
  public void displayWireFrame(PGraphics pg, DwSoftBody3D softbody, float strokeWeight){
//    PShape shp = pg.createShape();
    pg.beginShape(PConstants.LINES);
    pg.texture(null);
    if(softbody != null){
   
      for(DwFoldingTile tile : tiles){
        tile.DEF.style.stroke_w = strokeWeight;
        tile.displayWireframe(pg, softbody.particles);
      }
    } else {
      for(DwFoldingTile tile : tiles){
        tile.DEF.style.stroke_w = strokeWeight;
        tile.displayWireframe(pg, ifs);
      }
    }
    pg.endShape();
//    pg.shape(shp);
  }
  

}
