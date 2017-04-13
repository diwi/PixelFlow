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
#define SMAA_ONLY_COMPILE_VS 0
#define SMAA_GLSL_3 1 
#define SMAA_PRESET_ULTRA 1

#include "SMAA.h"

noperspective in vec2 texcoord;
noperspective in vec4 offset[3];

uniform sampler2D tex_color;

out     vec4      glFragColor;

void main(){
  // glFragColor = SMAALumaEdgeDetectionPS(texcoord, offset, tex_color);
  glFragColor = SMAAColorEdgeDetectionPS(texcoord, offset, tex_color);
  // glFragColor = SMAADepthEdgeDetectionPS(texcoord, offset, tex_depth);
}