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
uniform float multiplier;
uniform int   blend_mode;
uniform float mix_value;

uniform sampler2D tex_opticalflow;
uniform sampler2D tex_density_old;
uniform sampler2D tex_movie;


void main(){

  vec2 posn = gl_FragCoord.xy / wh;
  vec2 data_opticalflow = texture(tex_opticalflow, posn).xy;
  vec4 data_density_old = texture(tex_density_old, posn);
  vec4 data_movie       = texture(tex_movie, posn);
   
  vec4 data_src = data_density_old;
  vec4 data_new = data_src;
  
  float len = length(data_opticalflow) * multiplier;
  len = clamp(len, 0.0, 1.0);
  // len = pow(len,4);
  
  // vec4 data_ext = vec4(len, len*0.5, 0, 1);
  // vec4 data_ext = vec4(len, len*0.1, 0, len*0.5);
  // data_ext = clamp(data_ext, vec4(0), vec4(1));


  // float mix_value_ = mix_value * data_ext.a;
  data_new = mix(data_src, data_movie, len);

  
  glFragColor = data_new;
}

