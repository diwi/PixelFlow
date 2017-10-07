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

uniform vec2  wh;
uniform float multiplier;
uniform int   blend_mode;
uniform float mix_value;

uniform sampler2D tex_ext;
uniform sampler2D tex_src; // current temperature

void main(){

  vec2 posn     = gl_FragCoord.xy / wh;
  vec4  data_ext = texture(tex_ext, posn);
  float data_src = texture(tex_src, posn).x;
  
  data_ext *= multiplier;
  
  float temperature = (data_ext.r + data_ext.g +data_ext.b) / 3.0;
  float data_new = 0.0;
  
  // REPLACE
  if(blend_mode == 0){
    data_new = temperature;
  }
  // ADD
  if(blend_mode == 1){
    data_new = data_src + temperature;
  }
  // MAX_COMPONENT
  if(blend_mode == 2){
    data_new = max(data_src, temperature);
  }
  // MIX
  if(blend_mode == 3){
    data_new = mix(data_src, temperature, mix_value);
  }
  // MIX_2
  if(blend_mode == 4){
    float mix_value_ = mix_value * (temperature > 0 ? 1.0 : 0.0);;
    data_new = mix(data_src, temperature, mix_value_);
  }
  glFragColor = data_new;
}

