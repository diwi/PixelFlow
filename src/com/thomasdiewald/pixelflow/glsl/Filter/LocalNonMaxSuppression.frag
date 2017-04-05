/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out float glFragColor;

uniform sampler2D tex_harrisCorner;
uniform vec2 wh_rcp; 

void main(){

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  float hC  = texture(tex_harrisCorner, posn).x;
  
  float maxima = hC
    * step(textureOffset(tex_harrisCorner, posn, ivec2( 0,+1)).x, hC)  // hT 
    * step(textureOffset(tex_harrisCorner, posn, ivec2( 0,-1)).x, hC)  // hB 
    * step(textureOffset(tex_harrisCorner, posn, ivec2(+1, 0)).x, hC)  // hR 
    * step(textureOffset(tex_harrisCorner, posn, ivec2(-1, 0)).x, hC)  // hL 
    * step(textureOffset(tex_harrisCorner, posn, ivec2(-1,+1)).x, hC)  // hTL
    * step(textureOffset(tex_harrisCorner, posn, ivec2(+1,+1)).x, hC)  // hTR
    * step(textureOffset(tex_harrisCorner, posn, ivec2(-1,-1)).x, hC)  // hBL
    * step(textureOffset(tex_harrisCorner, posn, ivec2(+1,-1)).x, hC); // hBR
  
  glFragColor = maxima;

}

