package com.thomasdiewald.pixelflow.java.rigid_origami;


import com.thomasdiewald.pixelflow.java.geometry.DwIndexedFaceSet;
import com.thomasdiewald.pixelflow.java.softbodydynamics.DwPhysics;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint;
import com.thomasdiewald.pixelflow.java.softbodydynamics.constraint.DwSpringConstraint3D;
import com.thomasdiewald.pixelflow.java.softbodydynamics.particle.DwParticle3D;

import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.core.PShape;

public class DwFoldingTile{
  
  static public abstract class TileData{
    public TileStyle style = new TileStyle();
    
    public int       VERTS_COUNT;
    public int       FACES_COUNT;
    public int       EDGES_COUNT;
    public int  []   HILO;
    public int  []   FACES_COL;
    public float[][] VERTS;
    public int  [][] FACES;
    public int  [][] EDGES;
    public int  [][] SPRINGS_R;
    public int  [][] SPRINGS_S;
    public float[][] TEX_COORDS; // normalized uv
    public float[]   BOUNDS_2D; // xmin, ymin, xmax, ymax
    
    public abstract int[] getVertsIDX(int[][] faces, int[] vid);
    
    public void finishDefinition(){
      computeBounds();
      createTexcoords();
      
      int col_idx_max = 0;
      for(int i = 0; i < FACES_COL.length; i++){
        col_idx_max = Math.max(col_idx_max, FACES_COL[i]);
      }
      
      style.RGB = new float[col_idx_max+1][3];
      
      int shade_max = 0;
      for(int i = 0; i < HILO.length; i++){
        shade_max = Math.max(shade_max, HILO[i]);
      }
      

    }
    
    public void computeBounds(){
      for(int i = 0; i < VERTS_COUNT; i++){
        float[] vert = VERTS[i];
        if(BOUNDS_2D[0] > vert[0]) BOUNDS_2D[0] = vert[0];
        if(BOUNDS_2D[1] > vert[1]) BOUNDS_2D[1] = vert[1];
        if(BOUNDS_2D[2] < vert[0]) BOUNDS_2D[2] = vert[0];
        if(BOUNDS_2D[3] < vert[1]) BOUNDS_2D[3] = vert[1];
      }
    }
    public void createTexcoords(){
      for(int i = 0; i < VERTS_COUNT; i++){
        float[] vert = VERTS[i];
        TEX_COORDS[i][0] = (vert[0] - BOUNDS_2D[0]) / (BOUNDS_2D[2] - BOUNDS_2D[0]);
        TEX_COORDS[i][1] = (vert[1] - BOUNDS_2D[1]) / (BOUNDS_2D[3] - BOUNDS_2D[1]);
      }
    }
  }
  
  
  static public class TileStyle{
    public PImage texture;
//    public int   fill_r, fill_g, fill_b;
    public int   stroke_r=0, stroke_g=0, stroke_b=0;
    public float stroke_w=0.5f;
    public boolean use_fold_shading = true;
    
//    private int[] COL = new int[3];
    private float[][] RGB = {{255,255,255}};
    private int[][] RGBS;

    public void createShading(float shade){
      float r, g, b;
//      r = fill_r; 
//      g = fill_g; 
//      b = fill_b;
      
      float lo_hi = 0.5f; // lo-hi mix value
      
      float slo = use_fold_shading ? shade : 1;
      float shi = 1;
      float smi = slo * (1f-lo_hi) + shi * lo_hi;
      
//      COL[0] = 0xFF000000 | (int)(r*slo) << 16 | (int)(g*slo) << 8 | (int)(b*slo);
//      COL[1] = 0xFF000000 | (int)(r*smi) << 16 | (int)(g*smi) << 8 | (int)(b*smi);
//      COL[2] = 0xFF000000 | (int)(r*shi) << 16 | (int)(g*shi) << 8 | (int)(b*shi);
      
      
      int num_colors = RGB.length;
      if(RGBS == null || RGBS.length != num_colors){
        RGBS = new int[num_colors][3];
      }
      for(int i = 0; i < num_colors; i++){
        r = RGB[i][0]; 
        g = RGB[i][1]; 
        b = RGB[i][2]; 
        
        RGBS[i][0] = 0xFF000000 | (int)(r*slo) << 16 | (int)(g*slo) << 8 | (int)(b*slo);
        RGBS[i][1] = 0xFF000000 | (int)(r*smi) << 16 | (int)(g*smi) << 8 | (int)(b*smi);
        RGBS[i][2] = 0xFF000000 | (int)(r*shi) << 16 | (int)(g*shi) << 8 | (int)(b*shi);
      } 
    }
    

    
    public TileStyle(){
    }
//    public TileStyle(int r, int g, int b){
//      setFillColor(r,g,b);
//    } 
    public void setTexture(PImage texture){
      this.texture = texture;
    }
    
    public void setFillColors(float[] ... colors){
      RGB = colors;
    }
    
//    public void setFillColor(int argb){
//      this.fill_r = (argb >> 16) & 0xFF;
//      this.fill_g = (argb >>  8) & 0xFF;
//      this.fill_b = (argb >>  0) & 0xFF;
//    }
//    public void setFillColor(int r, int g, int b){
//      this.fill_r = r; 
//      this.fill_g = g; 
//      this.fill_b = b;
//    }
//    public void setFillColor(float r, float g, float b){
//      this.fill_r = (int) Math.round(r); 
//      this.fill_g = (int) Math.round(g);  
//      this.fill_b = (int) Math.round(b); 
//    }
    public void setStrokeColor(int r, int g, int b){
      this.stroke_r = r; 
      this.stroke_g = g; 
      this.stroke_b = b;
    }
    public void setStrokeColor(float r, float g, float b){
      this.stroke_r = (int) Math.round(r); 
      this.stroke_g = (int) Math.round(g);  
      this.stroke_b = (int) Math.round(b); 
    }
    public void setStrokeWeight(float stroke_weight){
      this.stroke_w = stroke_weight;
    }
  }
  
  
  
  
  
  public final TileData DEF;
  
  public int[]   vidx; // vertex indices, computed based on the faces
  public int[][] faces;// faces, containing global vertex indices
  public float[]              springs_S_restlen;
  public DwSpringConstraint[] springs_S;
  
  // used for animation
  public float springs_scale_min = 0.025f;
  public float springs_scale_max = 1.000f;
  public float springs_scale     = -1;
  
  


  public DwFoldingTile(TileData DEF, DwIndexedFaceSet ifs, int verts_idx, int faces_idx, PMatrix3D mat){
    this.DEF = DEF;

    float[][] verts = new float[DEF.VERTS_COUNT][3];
    for(int i = 0; i < DEF.VERTS_COUNT; i++){
      mat.mult(DEF.VERTS[i], verts[i]);
    }
    
    int[][] faces = new int[DEF.FACES_COUNT][3];
    for(int i = 0; i < DEF.FACES_COUNT; i++){
      faces[i][0] = DEF.FACES[i][0] + verts_idx;
      faces[i][1] = DEF.FACES[i][1] + verts_idx;
      faces[i][2] = DEF.FACES[i][2] + verts_idx;
    }
    
    this.faces = faces;
      
    System.arraycopy(verts, 0, ifs.verts, verts_idx, DEF.VERTS_COUNT);
    System.arraycopy(faces, 0, ifs.faces, faces_idx, DEF.FACES_COUNT);
  }

  public int[] updateVIDX(){
    return (vidx = DEF.getVertsIDX(faces, vidx));
  }
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // SPRINGS
  //////////////////////////////////////////////////////////////////////////////
   
  public void createSprings(DwPhysics<DwParticle3D> physics, DwParticle3D[] particles, DwSpringConstraint.Param rigid, DwSpringConstraint.Param soft){
    int[] vid = updateVIDX();
    
    int springs_R_count = DEF.SPRINGS_R.length;
    int springs_S_count = DEF.SPRINGS_S.length;
    
    // RIGID SPRINGS
    for(int i = 0; i < springs_R_count; i++){
      int[] spring = DEF.SPRINGS_R[i];
      int i0 = vid[spring[0]];
      int i1 = vid[spring[1]];
      DwParticle3D v0 = particles[i0]; 
      DwParticle3D v1 = particles[i1];
      DwSpringConstraint3D.addSpring(physics, v0, v1, rigid, DwSpringConstraint.TYPE.STRUCT);
    }
    
    // SOFT SPRINGS
    springs_S         = new DwSpringConstraint[springs_S_count];
    springs_S_restlen = new float             [springs_S_count];
    
    for(int i = 0; i < springs_S_count; i++){
      int[] spring =  DEF.SPRINGS_S[i];
      int i0 = vid[spring[0]];
      int i1 = vid[spring[1]];
      DwParticle3D v0 = particles[i0]; 
      DwParticle3D v1 = particles[i1];

      springs_S         [i] = DwSpringConstraint3D.addSpring(physics, v0, v1, soft, DwSpringConstraint.TYPE.BEND);
      springs_S_restlen [i] = springs_S[i].dd_rest;
    }
    
  }
  


  public void scaleSprings(float springs_scale_new){
    if(springs_scale == springs_scale_new){
      return;
    }
    springs_scale_new = Math.max(springs_scale_new, springs_scale_min);
    springs_scale_new = Math.min(springs_scale_new, springs_scale_max);
    
    if(springs_S != null){
      for(int i = 0; i < springs_S.length; i++){
        springs_S[i].setRestLength(springs_S_restlen[i] * springs_scale_new);
      }
    }
    springs_scale = springs_scale_new;
//    createShading();
    DEF.style.createShading(springs_scale);
  }
  

  public void setSpringScaleBounds(float springs_scale_min, float springs_scale_max){
    this.springs_scale_min = springs_scale_min;
    this.springs_scale_max = springs_scale_max;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // DISPLAY
  //////////////////////////////////////////////////////////////////////////////


  public void displayMesh(PGraphics pg, DwIndexedFaceSet ifs){
//    pg.beginShape(PConstants.TRIANGLES);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(DEF.style.texture);
    pg.noStroke();
    int     s0,s1,s2;
    int     i0,i1,i2;
    float[] t0,t1,t2;
    float[] v0,v1,v2;
    for(int i = 0; i < DEF.FACES_COUNT; i++){
      i0 = faces[i][0];  v0 = ifs.verts[i0];
      i1 = faces[i][1];  v1 = ifs.verts[i1];
      i2 = faces[i][2];  v2 = ifs.verts[i2];
      
      i0 = DEF.FACES[i][0]; s0 = DEF.HILO[i0];  t0 = DEF.TEX_COORDS[i0];
      i1 = DEF.FACES[i][1]; s1 = DEF.HILO[i1];  t1 = DEF.TEX_COORDS[i1];
      i2 = DEF.FACES[i][2]; s2 = DEF.HILO[i2];  t2 = DEF.TEX_COORDS[i2];
      
      int ci = DEF.FACES_COL[i];
      
      if(DEF.style.texture != null){
        DwDisplayUtils.vertex(pg, v0, t0); 
        DwDisplayUtils.vertex(pg, v1, t1); 
        DwDisplayUtils.vertex(pg, v2, t2); 
      } else {
//        pg.fill(DEF.style.COL[s0]); DwDisplayUtils.vertex(pg, v0);
//        pg.fill(DEF.style.COL[s1]); DwDisplayUtils.vertex(pg, v1);
//        pg.fill(DEF.style.COL[s2]); DwDisplayUtils.vertex(pg, v2);
        pg.fill(DEF.style.RGBS[ci][s0]); DwDisplayUtils.vertex(pg, v0);
        pg.fill(DEF.style.RGBS[ci][s1]); DwDisplayUtils.vertex(pg, v1);
        pg.fill(DEF.style.RGBS[ci][s2]); DwDisplayUtils.vertex(pg, v2);
      }
    }
//    pg.endShape();
  } 
  
  public void displayMesh(PShape pg, DwIndexedFaceSet ifs){
  //  pg.beginShape(PConstants.TRIANGLES);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(DEF.style.texture);
    pg.noStroke();
    int     s0,s1,s2;
    int     i0,i1,i2;
    float[] t0,t1,t2;
    float[] v0,v1,v2;
    for(int i = 0; i < DEF.FACES_COUNT; i++){
      i0 = faces[i][0];  v0 = ifs.verts[i0];
      i1 = faces[i][1];  v1 = ifs.verts[i1];
      i2 = faces[i][2];  v2 = ifs.verts[i2];
      
      i0 = DEF.FACES[i][0]; s0 = DEF.HILO[i0];  t0 = DEF.TEX_COORDS[i0];
      i1 = DEF.FACES[i][1]; s1 = DEF.HILO[i1];  t1 = DEF.TEX_COORDS[i1];
      i2 = DEF.FACES[i][2]; s2 = DEF.HILO[i2];  t2 = DEF.TEX_COORDS[i2];
      
      int ci = DEF.FACES_COL[i];
      
      if(DEF.style.texture != null){
        DwDisplayUtils.vertex(pg, v0, t0); 
        DwDisplayUtils.vertex(pg, v1, t1); 
        DwDisplayUtils.vertex(pg, v2, t2); 
      } else {
  //      pg.fill(DEF.style.COL[s0]); DwDisplayUtils.vertex(pg, v0);
  //      pg.fill(DEF.style.COL[s1]); DwDisplayUtils.vertex(pg, v1);
  //      pg.fill(DEF.style.COL[s2]); DwDisplayUtils.vertex(pg, v2);
        pg.fill(DEF.style.RGBS[ci][s0]); DwDisplayUtils.vertex(pg, v0);
        pg.fill(DEF.style.RGBS[ci][s1]); DwDisplayUtils.vertex(pg, v1);
        pg.fill(DEF.style.RGBS[ci][s2]); DwDisplayUtils.vertex(pg, v2);
      }
    }
  //  pg.endShape();
  } 

  
  public void displayMesh(PGraphics pg, DwParticle3D[] particles){
//    pg.beginShape(PConstants.TRIANGLES);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(DEF.style.texture);
    pg.noStroke();
    int          s0,s1,s2;
    int          i0,i1,i2;
    float[]      t0,t1,t2;
    DwParticle3D v0,v1,v2;
    for(int i = 0; i < DEF.FACES_COUNT; i++){
      i0 = faces[i][0];  v0 = particles[i0];  if(v0.all_springs_deactivated) continue;
      i1 = faces[i][1];  v1 = particles[i1];  if(v1.all_springs_deactivated) continue;
      i2 = faces[i][2];  v2 = particles[i2];  if(v2.all_springs_deactivated) continue;
      
      i0 = DEF.FACES[i][0]; s0 = DEF.HILO[i0];  t0 = DEF.TEX_COORDS[i0];
      i1 = DEF.FACES[i][1]; s1 = DEF.HILO[i1];  t1 = DEF.TEX_COORDS[i1];
      i2 = DEF.FACES[i][2]; s2 = DEF.HILO[i2];  t2 = DEF.TEX_COORDS[i2];
      
      int ci = DEF.FACES_COL[i];
      
      if(DEF.style.texture != null){
        DwDisplayUtils.vertex(pg, v0, t0); 
        DwDisplayUtils.vertex(pg, v1, t1); 
        DwDisplayUtils.vertex(pg, v2, t2); 
      } else {
//        pg.fill(DEF.style.COL[s0]); DwDisplayUtils.vertex(pg, v0);
//        pg.fill(DEF.style.COL[s1]); DwDisplayUtils.vertex(pg, v1);
//        pg.fill(DEF.style.COL[s2]); DwDisplayUtils.vertex(pg, v2);
 
        pg.fill(DEF.style.RGBS[ci][s0]); DwDisplayUtils.vertex(pg, v0);
        pg.fill(DEF.style.RGBS[ci][s1]); DwDisplayUtils.vertex(pg, v1);
        pg.fill(DEF.style.RGBS[ci][s2]); DwDisplayUtils.vertex(pg, v2);
      }
    }
//    pg.endShape();
  }
  
  
  
  public void displayMesh(PShape pg, DwParticle3D[] particles){
  //  pg.beginShape(PConstants.TRIANGLES);
    pg.textureMode(PConstants.NORMAL);
    pg.texture(DEF.style.texture);
    pg.noStroke();
    int          s0,s1,s2;
    int          i0,i1,i2;
    float[]      t0,t1,t2;
    DwParticle3D v0,v1,v2;
    for(int i = 0; i < DEF.FACES_COUNT; i++){
      i0 = faces[i][0];  v0 = particles[i0];  if(v0.all_springs_deactivated) continue;
      i1 = faces[i][1];  v1 = particles[i1];  if(v1.all_springs_deactivated) continue;
      i2 = faces[i][2];  v2 = particles[i2];  if(v2.all_springs_deactivated) continue;
      
      i0 = DEF.FACES[i][0]; s0 = DEF.HILO[i0];  t0 = DEF.TEX_COORDS[i0];
      i1 = DEF.FACES[i][1]; s1 = DEF.HILO[i1];  t1 = DEF.TEX_COORDS[i1];
      i2 = DEF.FACES[i][2]; s2 = DEF.HILO[i2];  t2 = DEF.TEX_COORDS[i2];
      
      int ci = DEF.FACES_COL[i];
      
      if(DEF.style.texture != null){
        DwDisplayUtils.vertex(pg, v0, t0); 
        DwDisplayUtils.vertex(pg, v1, t1); 
        DwDisplayUtils.vertex(pg, v2, t2); 
      } else {
  //      pg.fill(DEF.style.COL[s0]); DwDisplayUtils.vertex(pg, v0);
  //      pg.fill(DEF.style.COL[s1]); DwDisplayUtils.vertex(pg, v1);
  //      pg.fill(DEF.style.COL[s2]); DwDisplayUtils.vertex(pg, v2);
  
        pg.fill(DEF.style.RGBS[ci][s0]); DwDisplayUtils.vertex(pg, v0);
        pg.fill(DEF.style.RGBS[ci][s1]); DwDisplayUtils.vertex(pg, v1);
        pg.fill(DEF.style.RGBS[ci][s2]); DwDisplayUtils.vertex(pg, v2);
      }
    }
  //  pg.endShape();
  }
  
  
  
  
  
  
  
  

  public void displayWireframe(PGraphics pg, DwParticle3D[] particles){
    pg.strokeWeight(DEF.style.stroke_w);
    pg.stroke(DEF.style.stroke_r, DEF.style.stroke_g, DEF.style.stroke_b);
    int[] vid = updateVIDX();
    for(int i = 0; i < DEF.EDGES_COUNT; i++){
      int[] edge = DEF.EDGES[i];
      int i0 = vid[edge[0]];
      int i1 = vid[edge[1]];
      DwParticle3D v0 = particles[i0]; 
      DwParticle3D v1 = particles[i1];
      if(v0.all_springs_deactivated || v1.all_springs_deactivated) continue;
      DwDisplayUtils.line(pg, v0, v1);
    }
  }
  
  
  public void displayWireframe(PGraphics pg, DwIndexedFaceSet ifs){
    pg.strokeWeight(DEF.style.stroke_w);
    pg.stroke(DEF.style.stroke_r, DEF.style.stroke_g, DEF.style.stroke_b);
    int[] vid = updateVIDX();
    for(int i = 0; i < DEF.EDGES_COUNT; i++){
      int[] edge = DEF.EDGES[i];
      int i0 = vid[edge[0]];
      int i1 = vid[edge[1]];
      float[] v0 = ifs.verts[i0]; 
      float[] v1 = ifs.verts[i1];
      DwDisplayUtils.line(pg, v0, v1);
    }
  }
  

}
