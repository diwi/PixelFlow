/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


package Fluid2D.Fluid_VelocityEncoding;


/**
 * 
 * functions for encoding/decoding 2D-velocity
 * 
 * @author Thomas
 *
 */

// namespace Velocity
public class Velocity{
  
  static final public float TWO_PI = (float) (Math.PI * 2.0f);
  
  // namespace Polar
  static public class Polar{
    
    /**
     * converts an unnormalized vector to polar-coordinates.
     * 
     * @param  vx velocity x, unnormalized
     * @param  vy velocity y, unnormalized
     * @return {arc, mag}
     */
    static public float[] getArc(float vx, float vy){
      // normalize
      float mag_sq = vx*vx + vy*vy;
      if(mag_sq < 0.00001){
        return new float[]{0,0};
      }
      float mag = (float) Math.sqrt(mag_sq);
      vx /= mag;
      vy /= mag;
      
      float arc = (float) Math.atan2(vy, vx);
      if(arc < 0) arc += TWO_PI;
      return new float[]{arc, mag};
    }
    
    /**
     * encodes an unnormalized 2D-vector as an unsigned 32 bit integer.<br>
     *<br>
     * 0xMMMMAAAA (16 bit arc, 16 bit magnitude<br>
     * 
     * @param x    velocity x, unnormalized
     * @param y    velocity y, unnormalized
      * @return encoded polar coordinates
     */
    static public int encode_vX_vY(float vx, float vy){
      float[] arc_mag = getArc(vx, vy);
      int argb = encode_vA_vM(arc_mag[0], arc_mag[1]);
      return argb;
    }
    
    /**
     * encodes a vector, given in polar-coordinates, into an unsigned 32 bit integer.<br>
     *<br>
     * 0xMMMMAAAA (16 bit arc, 16 bit magnitude<br>
     * 
     * @param vArc
     * @param vMag
     * @return encoded polar coordinates
     */
    static public int encode_vA_vM(float vArc, float vMag){
      float  vArc_nor = vArc / TWO_PI;                           // [0, 1]
      int    vArc_I16 = (int)(vArc_nor * (0xFFFF - 1)) & 0xFFFF; // [0, 0xFFFF[
      int    vMag_I16 = (int)(vMag                   ) & 0xFFFF; // [0, 0xFFFF[
      return vMag_I16 << 16 | vArc_I16;                          // ARGB ... 0xAARRGGBB
    }

    /**
     * decodes a vector, given as 32bit encoded integer (0xMMMMAAAA) to a 
     * normalized 2d vector and its magnitude.
     * 
     * @param rgba 32bit encoded integer (0xMMMMAAAA)
     * @return {vx, vy, vMag}
     */
    static public float[] decode_ARGB(int rgba){
      int   vArc_I16 = (rgba >>  0) & 0xFFFF;            // [0, 0xFFFF[
      int   vMag_I16 = (rgba >> 16) & 0xFFFF;            // [0, 0xFFFF[
      float vArc     = TWO_PI * vArc_I16 / (0xFFFF - 1); // [0, TWO_PI]
      float vMag     = vMag_I16;
      float vx       = (float) Math.cos(vArc);
      float vy       = (float) Math.sin(vArc);
      return new float[]{vx, vy, vMag}; 
    }
  }
  
}