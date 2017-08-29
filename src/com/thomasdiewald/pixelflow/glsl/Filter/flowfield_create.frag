/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

#version 150


out vec2 out_flow;

uniform vec2 wh_rcp;
uniform sampler2D tex_src;

void main(){

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
#define TEX_OFF(x,y) textureOffset(tex_src, posn, ivec2(x, y))

  vec4 dh1 = TEX_OFF(-1, 0) - TEX_OFF(+1, 0);
  vec4 dv1 = TEX_OFF( 0,-1) - TEX_OFF( 0,+1);
  
  // vec4 dh2 = TEX_OFF(-2, 0) - TEX_OFF(+2, 0);
  // vec4 dv2 = TEX_OFF( 0,-2) - TEX_OFF( 0,+2);
  
  out_flow = vec2(dh1.x, dv1.x);
}





