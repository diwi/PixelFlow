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

#define TWO_PI 6.2831855


uniform vec2  wh;
uniform float multiplier;
uniform int   blend_mode;
uniform float mix_value;

uniform sampler2D tex_ext;
uniform sampler2D tex_src;

vec3 decode_ARGB(int rgba){
  int   vArc_I16 = (rgba >>  0) & 0xFFFF;            // [0, 0xFFFF[
  int   vMag_I16 = (rgba >> 16) & 0xFFFF;            // [0, 0xFFFF[
  float vArc     = TWO_PI * vArc_I16 / (0xFFFF - 1); // [0, TWO_PI]
  float vMag     = vMag_I16;
  float vx       = cos(vArc);
  float vy       = sin(vArc);
  return vec3(vx, vy, vMag); 
}



void main(){

  vec2 posn     = gl_FragCoord.xy / wh;
  vec4 data_ext = texture(tex_ext, posn);
  vec4 data_src = texture(tex_src, posn);
  
  ivec4 data = ivec4(data_ext * 255);
  int argb = data.a << 24 | data.r << 16 | data.g << 8 | data.b;
  
  vec3 vxym = decode_ARGB(argb);
  vec2 vExt = vxym.xy * vxym.z * multiplier;
  
  vec2 vOld = data_src.xy;
  vec2 vNew = vec2(0);

  // REPLACE
  if(blend_mode == 0){
    vNew = vExt;
  }
  // ADD
  if(blend_mode == 1){
    vNew = vOld + vExt;
  }
  // MIX
  if(blend_mode == 2){
    vNew = mix(vOld, vExt, mix_value);
  }
  // MAX_MAGNITUDE_MIX
  if(blend_mode == 3){
    if(length(vOld) > length(vNew)){
      glFragColor = vOld;
    } else {
      glFragColor = mix(vOld, vNew, mix_value);
    }
  }
  // MAX_MAG
  if(blend_mode == 4){
    vec2 vSum = vOld + vExt;
    if(length(vExt) >= length(vSum)){
      vNew = vExt;
    } else {
      vNew = vOld;
    }
  }
  if(blend_mode == 5){
    vNew = (length(vExt) > 0.0) ? vExt : vOld;
  }
  if(blend_mode == 6){
    if(length(vExt) > 0.0){
      vNew = mix(vOld, vExt, mix_value);
    } else {
      vNew =  vOld;
    }
  }
  glFragColor = vNew;
}




