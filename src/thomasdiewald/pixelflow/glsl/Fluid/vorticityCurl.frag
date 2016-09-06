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


out float glFragColor;

uniform sampler2D tex_velocity;
uniform sampler2D tex_obstacleC;
// uniform sampler2D tex_obstacleN;

uniform vec2  wh_inv;
uniform float halfrdx;

void main(){

  vec2 posn = wh_inv * gl_FragCoord.xy;
  
  if (texture(tex_obstacleC, posn).x == 1.0) { glFragColor = 0.0; return; }
  
  // velocity
  vec2 vT = textureOffset(tex_velocity, posn, + ivec2(0,1)).xy;
  vec2 vB = textureOffset(tex_velocity, posn, - ivec2(0,1)).xy;
  vec2 vR = textureOffset(tex_velocity, posn, + ivec2(1,0)).xy;
  vec2 vL = textureOffset(tex_velocity, posn, - ivec2(1,0)).xy;
  vec2 vC = texture      (tex_velocity, posn              ).xy;
      
  glFragColor = halfrdx * ((vT.x - vB.x) - (vR.y - vL.y));
}


