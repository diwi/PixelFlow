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

struct Data{
  vec2  pos;
  float rad;
  float temperature;
};

uniform vec2 wh_src;
uniform vec2 wh_dst;
uniform Data data;

uniform sampler2D tex_src ;


void main(){

  // scale from viewport-size to fluid-size
  vec2 scale = wh_dst / wh_src;
  
  // scaled position/radius
  vec2  data_pos = data.pos * scale;
  float data_rad = data.rad * (scale.x + scale.y) * 0.5;

  // current data
  float tOld = texture(tex_src, gl_FragCoord.xy / wh_dst).x;
  glFragColor = data.temperature;

  float dist = distance(data_pos, gl_FragCoord.xy);
  if (dist < data_rad) {
    glFragColor = data.temperature;
  } else {
    glFragColor = tOld;
  }
}