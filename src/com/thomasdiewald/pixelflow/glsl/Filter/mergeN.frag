/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

#define TEX_LAYERS 0

out vec4 glFragColor;

uniform sampler2D tex_src[TEX_LAYERS];
uniform vec2      tex_mad[TEX_LAYERS];
uniform vec2      wh_rcp; 

void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;

  vec4 rgba = vec4(0.0);
  for(int i = 0; i < TEX_LAYERS; i++){
    rgba += texture(tex_src[i], posn) * tex_mad[i].x + tex_mad[i].y;
  }
         
  glFragColor = rgba;                      
}
