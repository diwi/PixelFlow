/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald (www.thomasdiewald.com)
 * 
 * src  - www.github.com/diwi/PixelFlow
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

package com.thomasdiewald.pixelflow.java.imageprocessing.filter;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;

import processing.opengl.PGraphicsOpenGL;
import processing.opengl.Texture;

/**
 * 
 * The Merge pass has several useful applications.
 * 
 * e.g. mixing/adding two or more textures based on weights (plus an additional scalar)
 * 
 * 
 * result_A = tex_A * mult_A + add_A
 * result_B = tex_B * mult_B + add_B
 * result_N = tex_N * mult_N + add_N
 * 
 * result = result_A + result_B + ... + result_N
 * 
 * 
 * where mult_A and add_A is called "mad_A" in the code ... "multiply and add"
 * 
 * 
 * e.g. this can be useful for the DoG operator where the rgb-values become negative
 * due to the difference [-mult, +mult] and instead of abs(-rgb) the values are shifted
 * by +0.5 (e.g. for 8bit unsigned byte textures)
 * mostly however, the "add_N" component will just be 0.0.
 * 
 * 
 * 
 * 
 * @author Thomas Diewald
 *
 */
public class Merge {
  
  
  public DwPixelFlow context;
  
  public DwGLSLProgram shader_merge1;
  public DwGLSLProgram shader_merge2;
  public DwGLSLProgram shader_merge3;
  public DwGLSLProgram shader_merge4;
  public DwGLSLProgram shader_merge5;
  public DwGLSLProgram shader_mergeN;
  
  
  public Merge(DwPixelFlow context){
    this.context = context;
  }
  
  
  public void apply(DwGLTexture dst, DwGLTexture[] tex_src, float[] tex_weights){
    apply(dst, alloc(tex_src, tex_weights));
  }
  
  public void apply(PGraphicsOpenGL dst, DwGLTexture[] tex_src, float[] tex_weights){
    apply(dst, alloc(tex_src, tex_weights));
  }
  
  public void apply(PGraphicsOpenGL dst, PGraphicsOpenGL[] tex_src, float[] tex_weights){
    apply(dst, alloc(tex_src, tex_weights));
  }
  
  public void apply(DwGLTexture dst, PGraphicsOpenGL[] tex_src, float[] tex_weights){
    apply(dst, alloc(tex_src, tex_weights));
  }
  

  public void apply(PGraphicsOpenGL dst, TexMad ... tex){
    if(tex == null) return;
    Texture tex_dst = dst.getTexture(); if(!tex_dst.available())  return;
    context.begin();
    context.beginDraw(dst);
    apply(tex_dst.glWidth, tex_dst.glHeight, tex);
    context.endDraw();
    context.end("Merge.apply");
  }
  
  public void apply(DwGLTexture dst, TexMad ... tex){
    if(tex == null) return;
    context.begin();
    context.beginDraw(dst);
    apply(dst.w, dst.h, tex);
    context.endDraw();
    context.end("Merge.apply");
  }
  

  
  private void apply(int w, int h, TexMad ... tex){
    int tex_layers = tex.length;
    switch(tex_layers){
      case 0: return;
      case 1: apply(w, h, tex[0]                                ); break;
      case 2: apply(w, h, tex[0], tex[1]                        ); break;
      case 3: apply(w, h, tex[0], tex[1], tex[2]                ); break;
      case 4: apply(w, h, tex[0], tex[1], tex[2], tex[3]        ); break;
      case 5: apply(w, h, tex[0], tex[1], tex[2], tex[3], tex[4]); break;
      default:
        {
          if(shader_mergeN == null){
            shader_mergeN = context.createShader((Object)(this+"TEX_LAYERS_N"), DwPixelFlow.SHADER_DIR+"Filter/merge.frag");
          }
          shader_mergeN.frag.setDefine("TEX_LAYERS", tex_layers);
          shader_mergeN.begin();
          shader_mergeN.uniform2f ("wh_rcp" , 1f/w,  1f/h);
          for(int i = 0; i < tex_layers; i++){
            shader_mergeN.uniform2f     ("mN["+i+"]", tex[i].mul, tex[i].add);
            shader_mergeN.uniformTexture("tN["+i+"]", tex[i].tex);
          }
          shader_mergeN.drawFullScreenQuad();
          shader_mergeN.end();
        }
        break;
    }

  }
  
  
  private void apply(int w, int h, TexMad t0){
    if(shader_merge1 == null){
      shader_merge1 = context.createShader((Object)(this+"TEX_LAYERS_1"), DwPixelFlow.SHADER_DIR+"Filter/merge.frag");
      shader_merge1.frag.setDefine("TEX_LAYERS", 1);  
    }
    shader_merge1.begin();
    shader_merge1.uniform2f     ("wh_rcp" , 1f/w,  1f/h);
    shader_merge1.uniform2f     ("m0", t0.mul, t0.add);
    shader_merge1.uniformTexture("t0", t0.tex);
    shader_merge1.drawFullScreenQuad();
    shader_merge1.end();
  }
  
  
  private void apply(int w, int h, TexMad t0, TexMad t1){
    if(shader_merge2 == null){
      shader_merge2 = context.createShader((Object)(this+"TEX_LAYERS_2"), DwPixelFlow.SHADER_DIR+"Filter/merge.frag");
      shader_merge2.frag.setDefine("TEX_LAYERS", 2);  
    }
    shader_merge2.begin();
    shader_merge2.uniform2f     ("wh_rcp" , 1f/w,  1f/h);
    shader_merge2.uniform2f     ("m0", t0.mul, t0.add);
    shader_merge2.uniform2f     ("m1", t1.mul, t1.add);
    shader_merge2.uniformTexture("t0", t0.tex);
    shader_merge2.uniformTexture("t1", t1.tex);
    shader_merge2.drawFullScreenQuad();
    shader_merge2.end();
  }
  
  private void apply(int w, int h, TexMad t0, TexMad t1, TexMad t2){
    if(shader_merge3 == null){
      shader_merge3 = context.createShader((Object)(this+"TEX_LAYERS_3"), DwPixelFlow.SHADER_DIR+"Filter/merge.frag");
      shader_merge3.frag.setDefine("TEX_LAYERS", 3);   
    }
    shader_merge3.begin();
    shader_merge3.uniform2f     ("wh_rcp" , 1f/w,  1f/h);
    shader_merge3.uniform2f     ("m0", t0.mul, t0.add);
    shader_merge3.uniform2f     ("m1", t1.mul, t1.add);
    shader_merge3.uniform2f     ("m2", t2.mul, t2.add);
    shader_merge3.uniformTexture("t0", t0.tex);
    shader_merge3.uniformTexture("t1", t1.tex);
    shader_merge3.uniformTexture("t2", t2.tex);
    shader_merge3.drawFullScreenQuad();
    shader_merge3.end();
  }
  
  private void apply(int w, int h, TexMad t0, TexMad t1, TexMad t2, TexMad t3){
    if(shader_merge4 == null){
      shader_merge4 = context.createShader((Object)(this+"TEX_LAYERS_4"), DwPixelFlow.SHADER_DIR+"Filter/merge.frag");
      shader_merge4.frag.setDefine("TEX_LAYERS", 4);
    }
    shader_merge4.begin();
    shader_merge4.uniform2f     ("wh_rcp" , 1f/w,  1f/h);
    shader_merge4.uniform2f     ("m0", t0.mul, t0.add);
    shader_merge4.uniform2f     ("m1", t1.mul, t1.add);
    shader_merge4.uniform2f     ("m2", t2.mul, t2.add);
    shader_merge4.uniform2f     ("m3", t3.mul, t3.add);  
    shader_merge4.uniformTexture("t0", t0.tex);
    shader_merge4.uniformTexture("t1", t1.tex);
    shader_merge4.uniformTexture("t2", t2.tex);
    shader_merge4.uniformTexture("t3", t3.tex);
    shader_merge4.drawFullScreenQuad();
    shader_merge4.end();
  }
  
  private void apply(int w, int h, TexMad t0, TexMad t1, TexMad t2, TexMad t3, TexMad t4){
    if(shader_merge5 == null){
      shader_merge5 = context.createShader((Object)(this+"TEX_LAYERS_5"), DwPixelFlow.SHADER_DIR+"Filter/merge.frag");
      shader_merge5.frag.setDefine("TEX_LAYERS", 5);
    }
    shader_merge5.begin();
    shader_merge5.uniform2f     ("wh_rcp" , 1f/w,  1f/h);
    shader_merge5.uniform2f     ("m0", t0.mul, t0.add);
    shader_merge5.uniform2f     ("m1", t1.mul, t1.add);
    shader_merge5.uniform2f     ("m2", t2.mul, t2.add);
    shader_merge5.uniform2f     ("m3", t3.mul, t3.add);
    shader_merge5.uniform2f     ("m4", t4.mul, t4.add);  
    shader_merge5.uniformTexture("t0", t0.tex);
    shader_merge5.uniformTexture("t1", t1.tex);
    shader_merge5.uniformTexture("t2", t2.tex);
    shader_merge5.uniformTexture("t3", t3.tex);
    shader_merge5.uniformTexture("t4", t4.tex);
    shader_merge5.drawFullScreenQuad();
    shader_merge5.end();
  }
  

  
  
  
  
  
  static public class TexMad {
    public int   tex = 0;
    public float mul = 0;
    public float add = 0;
    
    public TexMad(){
    }
    public TexMad(DwGLTexture tex, float mul, float add){
      set(tex, mul, add);
    }
    public TexMad(PGraphicsOpenGL pg, float mul, float add){
      set(pg, mul, add);
    } 
    public TexMad set(DwGLTexture tex, float mul, float add){
      this.tex = tex.HANDLE[0];
      this.mul = mul;
      this.add = add;
      return this;
    }
    public TexMad set(PGraphicsOpenGL pg, float mul, float add){
      Texture tex = pg.getTexture(); if(!tex.available()) return this;
      this.tex = tex.glName;
      this.mul = mul;
      this.add = add;
      return this;
    }
  }


  
  

  
  public TexMad[] alloc(DwGLTexture[] tex_src, float[] tex_weights){
    if((tex_src.length * 2) != tex_weights.length) return null;
    
    TexMad[] tex = new TexMad[tex_src.length];
    for(int i = 0; i < tex.length; i++){
      tex[i] = new TexMad(tex_src[i], tex_weights[i*2+0], tex_weights[i*2+1]);
    }
    return tex;
  }
  
  public TexMad[] alloc(PGraphicsOpenGL[] tex_src, float[] tex_weights){
    if((tex_src.length * 2) != tex_weights.length) return null;
    
    TexMad[] tex = new TexMad[tex_src.length];
    for(int i = 0; i < tex.length; i++){
      tex[i] = new TexMad(tex_src[i], tex_weights[i*2+0], tex_weights[i*2+1]);
    }
    return tex;
  }
  
  
  
  
  
}
