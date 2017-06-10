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
uniform sampler2D	tex_src;
uniform sampler2D	tex_mul;

void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;
  glFragColor = texture(tex_src, posn) * texture(tex_mul, posn);
}





