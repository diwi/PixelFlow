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

uniform vec2 wh;
uniform sampler2D tex_src;

uniform int blend_mode;

void main(){


  vec4 src = texture(tex_src, gl_FragCoord.xy / wh);
  
  glFragColor = 0.0;
  if(src.x > 0.0 ) glFragColor = 1.0;
  if(src.y > 0.0 ) glFragColor = 1.0;
  if(src.z > 0.0 ) glFragColor = 1.0;
  if(src.a > 0.0 ) glFragColor = 1.0;
  
}

