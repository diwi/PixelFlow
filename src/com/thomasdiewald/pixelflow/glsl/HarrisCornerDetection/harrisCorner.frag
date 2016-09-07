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

uniform sampler2D tex_harrisMatrix;
uniform vec2 wh; 
uniform float harrisK = 0.04; // 0.04 – 0.15
uniform float scale = 1.0;


// https://en.wikipedia.org/wiki/Corner_detection
//
// Harris Corner measure, Reponse
//
// Ix  ... gradient in X
// Iy  ... gradient in Y
// Ixx ... Ix * Ix
// Iyy ... Iy * Iy
// Ixy ... Ix * Iy
//
// R = det(A) - K(Trace^2(A))
// R = Ix^2 * Iy^2 - Ixy^2 - K(Ix^2 + Iy^2)^2
// R = Ixx * Iyy - Ixy * Ixy - K(Ixx + Iyy)^2
//
// Nobles's Corner measure, Response
// R = det(A) / (Trace(A) + e)



#define NOBLE 0 // 0 ... noble, 1 ... harris

void main(){
  
  vec2 posn = gl_FragCoord.xy / wh;
  vec3 harris_matrix = texture(tex_harrisMatrix, posn).xyz;
  
  float Ixx = harris_matrix.x; // Ix * Ix
  float Iyy = harris_matrix.y; // Iy * Iy
  float Ixy = harris_matrix.z; // Ix * Iy
  float Ixxyy_sum = Ixx + Iyy;
  
#if (NOBLE == 1)
  float corner_response = harrisK * (Ixx * Iyy - (Ixy * Ixy)) / (Ixxyy_sum + 0.0001); // using harrisK as a scalefactor to avoid glsl warnings (lazyness)
#else
  float corner_response = (Ixx * Iyy - (Ixy * Ixy)) - harrisK * (Ixxyy_sum * Ixxyy_sum);
#endif

  glFragColor = corner_response * scale;
}








