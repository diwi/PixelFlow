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
uniform float mix_value;
uniform vec2  wh; 

void main(){
  vec2 posn = gl_FragCoord.xy / wh;
  glFragColor = mix(texture(texA, posn), texture(texB, posn), mix_value);

}



