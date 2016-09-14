/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

precision mediump float;
precision mediump int;

out vec4 glFragColor;

in vec2 velocity;
uniform float point_size;

void main(){

  vec2 pc = abs(gl_PointCoord * 2.0 - 1.0); // abs[-1, 1]
  
  // 1) round + falloff with radius
  float falloff = 1.0 - clamp(length(pc), 0.0, 1.0);
  falloff = pow(falloff, 0.3);
  
  // 2) round + evenly shaded 
  // if(length(pc) > 1.0) falloff = 0.0; else falloff = 1.0;

  float len = length(velocity) * 0.035;
  glFragColor = vec4(len, 0.5, 1-len, len * 0.5 + 0.5);
  glFragColor.a *= falloff;
}


