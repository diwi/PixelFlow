/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

#define EPSILON 0.000001

out vec4 glFragColor;

uniform sampler2D	tex;
uniform vec2 wh_rcp; 
uniform vec4 threshold_val = vec4(0.0f);
uniform vec4 threshold_pow = vec4(5.0f);
uniform vec4 threshold_mul = vec4(1.0f);

void main(){

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  vec4 color = texture(tex, posn);
  
  // if(color.r  < threshold_val.r){
     // color.r /= threshold_val.r;
     // color.r  = pow(color.r, threshold_pow.r) * threshold_mul.r;
     // color.r *= threshold_val.r;
  // }
  
  // if(color.g  < threshold_val.g){
     // color.g /= threshold_val.g;
     // color.g  = pow(color.g, threshold_pow.g) * threshold_mul.g;
     // color.g *= threshold_val.g;
  // }
  
  // if(color.b  < threshold_val.b){
     // color.b /= threshold_val.b;
     // color.b  = pow(color.b, threshold_pow.b) * threshold_mul.b;
     // color.b *= threshold_val.b;
  // }
  
  // if(color.a < threshold_val.a){
    // color.a /= threshold_val.a;
    // color.a  = pow(color.a, threshold_pow.a) * threshold_mul.a;
    // color.a *= threshold_val.a;
  // }
  
  // glFragColor = color;
  

  vec4 color_th = color;
  // remap to normalized range [0, threshold_val] -> [0, 1]
  color_th /= threshold_val + EPSILON; // !div by zero!
  // scale (exponentially)
  color_th  = pow(color_th, threshold_pow) * threshold_mul;
  // remap back [0, 1] -> [0, threshold_val]
  color_th *= threshold_val + EPSILON;
  
  glFragColor = mix(color_th, color, step(threshold_val, color));
}

