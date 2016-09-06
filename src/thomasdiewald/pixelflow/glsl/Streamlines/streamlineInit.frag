/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150 

// precision mediump float;
// precision mediump int;

out vec4 glFragColor;

uniform vec2 wh;

void main(){
   glFragColor = vec4(gl_FragCoord.xy / wh, 0, 0);
}

