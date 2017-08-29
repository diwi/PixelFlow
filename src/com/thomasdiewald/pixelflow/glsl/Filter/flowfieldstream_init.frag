/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150 

out vec2 out_pos;

uniform vec2 wh_rcp;

void main(){
  out_pos = gl_FragCoord.xy * wh_rcp;
}

