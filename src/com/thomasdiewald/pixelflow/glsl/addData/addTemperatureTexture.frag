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

uniform vec2 wh;
uniform float intensity;
uniform sampler2D tex_temperature_old;
uniform sampler2D tex_temperature_src;

void main(){

  vec2  tc      = gl_FragCoord.xy / wh;
  float tOld    = texture(tex_temperature_old, tc).x;
  vec3  tNewRGB = texture(tex_temperature_src, tc).rgb;
  
  // default, just use current temperature
  glFragColor = tOld;
  
  // warm
  if(tNewRGB.r > 0.0){
    float tNew = tNewRGB.r * intensity;
    glFragColor = max(tNew, tOld);
  }
  
  // cold
  if(tNewRGB.b > 0.0){
    float tNew = tNewRGB.b * intensity;
    glFragColor = min(-tNew, tOld);
  }
}

