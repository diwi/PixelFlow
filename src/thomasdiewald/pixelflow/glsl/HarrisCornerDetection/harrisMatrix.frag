/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec3 glFragColor;

uniform sampler2D tex_dx;
uniform sampler2D tex_dy;
uniform vec2 wh; 

void main(){

 vec2 posn = gl_FragCoord.xy / wh;
 
 float dx = texture(tex_dx, posn).x;
 float dy = texture(tex_dy, posn).x;
 
 glFragColor = vec3(dx*dx, dy*dy, dx*dy);
}








