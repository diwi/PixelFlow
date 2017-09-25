package com.thomasdiewald.pixelflow.java.imageprocessing;

import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge.TexMad;

import processing.core.PConstants;
import processing.opengl.PGraphics2D;

public class DwBackgroundSubtraction{
  
  static public class Param{
    public int bg_frames      = 5;
    public int bg_blur_radius = 8;
    public int fg_blur_radius = 8;
  }
  
  public Param param = new Param();
  
  public DwPixelFlow context;

  // buffers
  public PGraphics2D pg_background;
  public PGraphics2D pg_diff;
  public PGraphics2D pg_tmp;


  protected int bg_frames_counter = 0;

  public DwBackgroundSubtraction(DwPixelFlow context, int w, int h){
    this.context = context;
    pg_background = initTexture(w, h);
    pg_diff       = initTexture(w, h);
    pg_tmp        = initTexture(w, h);
  }
  
  public void reset(){
    bg_frames_counter = 0;
  }

  public PGraphics2D initTexture(int w, int h){
    PGraphics2D pg = (PGraphics2D) context.papplet.createGraphics(w, h, PConstants.P2D);
    pg.smooth(0);
    pg.beginDraw();
    pg.textureWrap(PConstants.CLAMP);
    pg.clear();
    pg.endDraw();
    return pg;
  }

  public void apply(PGraphics2D pg_src, PGraphics2D pg_dst){
    DwFilter filter = DwFilter.get(context);

    // compute Background
//    if(bg_frames_counter < param.bg_frames){
//      filter.luminance.apply(pg_src, pg_background);
//      filter.gaussblur.apply(pg_background, pg_background, pg_tmp, 8);
//      bg_frames_counter = param.bg_frames;
//    }

    // compute background (intensity, blur)
    if(bg_frames_counter < param.bg_frames){
      
      PGraphics2D pg_background_tmp = pg_diff; // borrowing it

      filter.luminance.apply(pg_src, pg_background_tmp);
      filter.gaussblur.apply(pg_background_tmp, pg_background_tmp, pg_tmp, param.bg_blur_radius);

      float  mix  = bg_frames_counter / (bg_frames_counter + 1f);
      TexMad tm0 = new TexMad(pg_background    ,      mix, 0);
      TexMad tm1 = new TexMad(pg_background_tmp, 1f - mix, 0);
      filter.merge.apply(pg_background, tm0, tm1);     
      
      bg_frames_counter++;
    }

    pg_tmp.beginDraw();
    pg_tmp.clear();
    pg_tmp.endDraw();

    // compute current frame (intensity, blur)
    filter.luminance.apply(pg_src, pg_diff);
    filter.gaussblur.apply(pg_diff, pg_diff, pg_tmp, param.fg_blur_radius);

    //      // compute frame difference
    //      float mult     = 2f;
    //      float shift    = 0.1f;
    //      float[] madA = { +mult, shift * 0.5f};
    //      float[] madB = { -mult, shift * 0.5f};
    //      filter.merge.apply(pg_diff, pg_background, pg_diff, madA, madB);

    // compute frame difference abs(pg_background - pg_diff)
    filter.difference.apply(pg_diff, pg_background, pg_diff);

    filter.multiply.apply(pg_src, pg_dst, pg_diff);
    
  }



}