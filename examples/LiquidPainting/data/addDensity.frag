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

uniform sampler2D tex_ext;
uniform sampler2D tex_src;

void main(){

  vec2 posn     = gl_FragCoord.xy / wh;
  vec4 data_ext = texture(tex_ext, posn);
  vec4 data_src = texture(tex_src, posn);
  
  data_ext *= multiplier;
  vec4 data_new = vec4(0);
  
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
  glFragColor = data_new;
}

