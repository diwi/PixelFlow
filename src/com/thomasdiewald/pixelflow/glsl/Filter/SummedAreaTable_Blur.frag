/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

precision highp float;
precision highp int;

out vec4 glFragColor;

uniform sampler2D	tex_sat;
uniform vec2 wh_dst;
uniform vec2 wh_sat;
uniform int radius = 0;

void main(){
  ivec2 pos = ivec2(wh_sat * gl_FragCoord.xy / wh_dst);
  ivec4 bb  = ivec4(pos - radius - 1, pos + radius);
  
  bb = clamp(bb, ivec4(0), ivec4(wh_sat.xyxy - 1));
  
  float area = float((bb.z-bb.x)*(bb.w-bb.y)); //   |      |
  vec4 A = texelFetch(tex_sat, bb.xy, 0);      // --A------B
  vec4 B = texelFetch(tex_sat, bb.zy, 0);      //   |######|
  vec4 C = texelFetch(tex_sat, bb.zw, 0);      //   |######|
  vec4 D = texelFetch(tex_sat, bb.xw, 0);      // --D------C
                                      
  glFragColor = (A+C-B-D) / area;
}


