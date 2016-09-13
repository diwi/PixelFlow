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

out vec2 glFragColor;

uniform vec2  wh;
uniform int   blend_mode;
uniform float mix_value;
uniform float multiplier;

uniform sampler2D tex_opticalflow;
uniform sampler2D tex_velocity_old;

void main(){

  vec2 posn = gl_FragCoord.xy / wh;
  vec2 data_opticalflow = texture(tex_opticalflow , posn).xy;
  vec2 data_velocity_old  = texture(tex_velocity_old, posn).xy;
  
  
  vec2 data_old = data_velocity_old;
  vec2 data_ext = data_opticalflow * multiplier;
  vec2 data_new = data_old;
  
  float len = length(data_ext);
  len = clamp(len, 0.0, 1.0);
  
  if (len > 0.0) {
    // REPLACE
    if(blend_mode == 0){
      glFragColor = data_ext;
    }
    // ADD
    if(blend_mode == 1){
      data_new = data_old + data_ext;
    }
    // MAX_MAGNITUDE
    if(blend_mode == 2){
      data_ext *= 15.0;
      if(length(data_old) > length(data_ext)){
        data_new = data_old;
      } else {
        data_new = mix(data_old, data_ext, mix_value);
      }
    }
  }
  
  
  
  
  
  
  
  
  
  
  
  
  /*
  // REPLACE
  if(blend_mode == 0){
    data_new = data_ext;
  }
  // ADD
  if(blend_mode == 1){
    data_new = data_src + data_ext;
  }
  // MAX_COMPONENT
  if(blend_mode == 2){
    data_new = max(data_src, data_ext);
  }
  // MAX_COMPONENT_OLD_INTENSITY
  if(blend_mode == 3){
    data_new = max(data_src, data_ext);
    data_new.a = data_src.a;
  }
  // MAX_COMPONENT_NEW_INTENSITY
  if(blend_mode == 4){
    data_new = max(data_src, data_ext);
    data_new.a = data_ext.a;
  }
  // MIX
  if(blend_mode == 5){
    data_new = mix(data_src, data_ext, mix_value);
  }
  // MIX_2
  if(blend_mode == 6){
    float mix_value_ = mix_value * data_ext.a;
    data_new = mix(data_src, data_ext, mix_value_);
  }*/
  glFragColor = data_new;
}

