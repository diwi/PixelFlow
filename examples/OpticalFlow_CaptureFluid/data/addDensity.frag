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

void main(){

  vec2 posn = gl_FragCoord.xy / wh;
  vec2 data_opticalflow = texture(tex_opticalflow, posn).xy;
  vec4 data_density_old = texture(tex_density_old, posn);
  
  vec4 data_src = data_density_old;
  vec4 data_new = data_src;
  
  float len = length(data_opticalflow) * multiplier;
  // len = clamp(len, 0.0, 1.0);
  // len = pow(len,4);
  
  // vec4 data_ext = vec4(len, len*0.5, 0, 1);
  vec4 data_ext = vec4(len, len*0.1, 0, len*0.5);
  data_ext = clamp(data_ext, vec4(0), vec4(1));

  if(len > 0.0){
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
    }
  } 
  glFragColor = data_new;
}

