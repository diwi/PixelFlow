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

#define TEX_LAYERS 0

uniform sampler2D tex_src[TEX_LAYERS];
uniform float tex_weights[TEX_LAYERS];

uniform vec2 wh_rcp; 

void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;

  vec4 rgba = vec4(0.0);
  for(int i = 0; i < TEX_LAYERS; i++){
    rgba += tex_weights[i] * texture(tex_src[i], posn);
  }
         
  glFragColor = clamp(rgba, vec4(0.0), vec4(1.0));                      
}
