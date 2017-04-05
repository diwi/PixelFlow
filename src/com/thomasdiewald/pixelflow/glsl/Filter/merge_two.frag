/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec4 glFragColor;

uniform sampler2D tex_A;
uniform sampler2D tex_B;
uniform vec2      mad_A;
uniform vec2      mad_B;
uniform vec2      wh_rcp; 

void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
  vec4 fragA = texture(tex_A, posn) * mad_A.x + mad_A.y;
  vec4 fragB = texture(tex_B, posn) * mad_B.x + mad_B.y; 
  
  glFragColor = fragA + fragB;              
}
