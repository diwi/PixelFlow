/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec4 glFragColor;

uniform sampler2D tex_harrisCorner;
uniform vec2 wh; 
// uniform float threshold;

void main(){

  vec2 posn = gl_FragCoord.xy / wh;
  float hC  = texture(tex_harrisCorner, posn).x;
  hC = clamp(hC, 0, 1);
  glFragColor = vec4(hC, 0, 0, hC);
}

