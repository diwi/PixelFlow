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

out vec4 glFragColor;


uniform vec2  wh;
uniform float intensity_scale;
uniform int   blend_mode;
uniform float mix_value;

uniform sampler2D tex_density_src;
uniform sampler2D tex_density_old;

void main(){

  vec2 posn     = gl_FragCoord.xy / wh;
  vec4 density_src = texture(tex_density_src, posn);
  vec4 density_old = texture(tex_density_old, posn);
  
  density_src.a *= intensity_scale;
  
  vec4 density_new = vec4(0);
  
  // REPLACE
  if(blend_mode == 0){
    density_new = density_src;
  }
  // ADD
  if(blend_mode == 1){
    density_new = density_old + density_src;
  }
  // MAX_COMPONENT
  if(blend_mode == 2){
    density_new = max(density_old, density_src);
  }
  // MAX_COMPONENT_OLD_INTENSITY
  if(blend_mode == 3){
    density_new = max(density_old, density_src);
    density_new.a = density_old.a;
  }
  // MAX_COMPONENT_NEW_INTENSITY
  if(blend_mode == 4){
    density_new = max(density_old, density_src);
    density_new.a = density_src.a;
  }
  // MIX
  if(blend_mode == 5){
    density_new = mix(density_old, density_src, mix_value);
  }
  glFragColor = density_new;
}

