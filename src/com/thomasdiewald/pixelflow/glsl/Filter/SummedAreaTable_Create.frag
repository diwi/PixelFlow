/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 

// Fast Summed-Area Table Generation and its Applications
// http://www.shaderwrangler.com/publications/sat/SAT_EG2005.pdf

#version 150

precision highp float;
precision highp int;

#define SAMPLES 8

out vec4 glFragColor;

uniform sampler2D	tex;
uniform ivec2 jump;

void main(){
  ivec2 pos = ivec2(gl_FragCoord.xy);
  vec4 sum = vec4(0);
  
  for(int i = 0; i < SAMPLES; i++){
    sum += texelFetch(tex, pos - i * jump, 0); 
  }
  glFragColor = sum;
}


