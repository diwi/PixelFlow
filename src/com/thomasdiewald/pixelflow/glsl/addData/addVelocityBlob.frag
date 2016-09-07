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

struct Data{
  vec2  pos;
  float rad;
  vec2  velocity;
};

uniform vec2 wh_src;
uniform vec2 wh_dst;
uniform Data data;
uniform int blend_mode;
uniform float mix_value;

uniform sampler2D tex_src ;

void main(){

  // scale from viewport-size to fluid-size
  vec2 scale = wh_dst / wh_src;
  
  // scaled position/radius
  vec2  data_pos = data.pos * scale;
  float data_rad = data.rad * (scale.x + scale.y) * 0.5;

  // current data
  vec2 vOld = texture(tex_src, gl_FragCoord.xy / wh_dst).xy;
  vec2 vNew = data.velocity;

  float dist = distance(data_pos, gl_FragCoord.xy);
  if (dist < data_rad) {
    float dist_norm = 1.0 - clamp( dist / data_rad, 0.0, 1.0);

    // REPLACE
    if(blend_mode == 0){
      glFragColor = vNew;
    }
    // ADD
    if(blend_mode == 1){
      float falloff = clamp(sqrt(dist_norm*0.1), 0, 1);
      glFragColor = vOld + vNew * falloff;
    }
    // MAX_MAGNITUDE
    if(blend_mode == 2){
      vNew = vNew * dist_norm;
      if(length(vOld) > length(vNew)){
        glFragColor = vOld;
      } else {
        glFragColor = mix(vOld, vNew, mix_value);
      }
    }
  } else {
    glFragColor = vOld;
  }
}



