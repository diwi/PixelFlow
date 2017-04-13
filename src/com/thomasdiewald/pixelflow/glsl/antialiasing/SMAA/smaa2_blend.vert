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

 
#version 150

precision mediump float;
precision mediump int;
                       
uniform vec2 wh_rcp;

#define SMAA_PIXEL_SIZE wh_rcp
#define SMAA_ONLY_COMPILE_VS 1
#define SMAA_GLSL_3 1 
#define SMAA_PRESET_ULTRA 1

#include "SMAA.h"

noperspective out vec2 texcoord;
noperspective out vec2 pixcoord;
noperspective out vec4 offset[3];

void main(){
  int x = ((gl_VertexID<<1) & 2) - 1;
  int y = ((gl_VertexID   ) & 2) - 1;
  gl_Position = vec4(x,y,0,1);
  texcoord = gl_Position.xy * 0.5 + 0.5;

  SMAABlendingWeightCalculationVS(gl_Position, gl_Position, texcoord, pixcoord, offset);
}