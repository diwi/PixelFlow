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

uniform sampler2D	texA;
uniform sampler2D	texB;
uniform vec2 multiplier;
uniform vec2  wh; 

void main(){
  vec2 posn = gl_FragCoord.xy / wh;
  vec4 dataA = texture(texA, posn) * multiplier.x;
  vec4 dataB = texture(texB, posn) * multiplier.y;
}



