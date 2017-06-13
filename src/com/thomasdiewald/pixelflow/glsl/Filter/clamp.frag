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

uniform sampler2D	tex;
uniform vec2 wh_rcp; 
uniform vec4 lo = vec4(0.0);
uniform vec4 hi = vec4(1.0);

void main(){
  glFragColor = clamp(texture(tex, gl_FragCoord.xy * wh_rcp), lo, hi);
}





