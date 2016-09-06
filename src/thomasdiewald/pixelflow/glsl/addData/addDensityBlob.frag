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

struct Data{
  vec2  pos;
  float rad;
  vec4  density;
};

uniform vec2 wh_src;
uniform vec2 wh_dst;
uniform Data data;
uniform int blend_mode;

uniform sampler2D tex_src ;


void main(){

  // scale from viewport-size to fluid-size
  vec2 scale = wh_dst / wh_src;
  
  // scaled position/radius
  vec2  data_pos = data.pos * scale;
  float data_rad = data.rad * (scale.x + scale.y) * 0.5;

  // current data
  vec4 dOld = texture(tex_src, gl_FragCoord.xy / wh_dst);
  vec4 dNew = data.density;

  float dist = distance(data_pos, gl_FragCoord.xy);
  if (dist < data_rad) {
    float dist_norm = 1.0 - clamp(dist / data_rad, 0.0, 1.0);

    // REPLACE
    if(blend_mode == 0){
      glFragColor = dNew;
    }
     // MIX_FALLOFF
    if(blend_mode == 1){
      float falloff = dist_norm * dist_norm;
      glFragColor = mix(dOld, dNew, falloff);
    }
    // MAX_FALLOFF
    if(blend_mode == 2){
      float falloff = sqrt(sqrt(dist_norm));
      glFragColor = max(dOld, dNew * falloff);
    }
    // MAX
    if(blend_mode == 3){
      glFragColor = max(dOld, dNew);
    }
    // NEW_RGB_OLD_A
    if(blend_mode == 4){
      glFragColor = vec4(dNew.rgb, dOld.a);
    }
    // OLD_NEW_AVG
    if(blend_mode == 5){
      glFragColor = (dNew + dOld) * 0.5;
    }
    // OLD_REDUCE
    if(blend_mode == 6){
      float falloff = dist_norm * dist_norm;
      vec4 dTmp = mix(dNew, dOld, 0.99); 
      glFragColor = mix(dOld, dTmp *0.99, falloff);
    }
  } else {
    glFragColor = dOld;
  }

}



