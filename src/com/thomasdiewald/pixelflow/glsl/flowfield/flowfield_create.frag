/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

#version 150


out vec2 out_flow;

uniform vec2 wh_rcp;
uniform sampler2D tex_src;

void main(){

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
// #define TEX_OFF(x,y) textureOffset(tex_src, posn, ivec2(x, y))
#define TEX_OFF(x,y) texture(tex_src, posn+vec2(x, y)*wh_rcp)

  // vec4 dh = TEX_OFF(-1, 0) - TEX_OFF(+1, 0);
  // vec4 dv = TEX_OFF( 0,-1) - TEX_OFF( 0,+1);
  
  
  // sobel filter for gradient
  
  // TL T TR
  //  L o  R
  // BL B BR
  
  vec4  L = TEX_OFF(-1, 0);
  vec4  R = TEX_OFF(+1, 0);
  vec4  T = TEX_OFF( 0,-1);
  vec4  B = TEX_OFF( 0,+1);
  
  vec4 TL = TEX_OFF(-1,-1);
  vec4 TR = TEX_OFF(+1,-1);
  vec4 BL = TEX_OFF(-1,+1);
  vec4 BR = TEX_OFF(+1,+1);
  
  vec4 dh = vec4(0);
  vec4 dv = vec4(0);
  
  dh += TL * +1;   dh += TR * -1;
  dh +=  L * +2;   dh +=  R * -2;
  dh += BL * +1;   dh += BR * -1;
  
  dv += TL * +1;   dv += BL * -1;
  dv += T  * +2;   dv += B  * -2;
  dv += TR * +1;   dv += BR * -1;
  
  out_flow = vec2(dh.x, dv.x);
  
  
  float len = length(out_flow);
  out_flow /= (len + 0.000001);
}





