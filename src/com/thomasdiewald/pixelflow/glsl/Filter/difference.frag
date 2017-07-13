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

uniform vec2 wh_rcp; 
uniform sampler2D	texA;
uniform sampler2D	texB;

void main(){

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  vec4 dataA = texture(texA, posn);
  vec4 dataB = texture(texB, posn);
  glFragColor = abs(dataA - dataB) * 2.0;
}





