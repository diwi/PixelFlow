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


in vec2 velocity;
in float line_domain;

uniform float velocity_scale;

void main(){

  // get velocity
  float len = velocity_scale * length(velocity) * 0.1;

  // glFragColor = vec4(len, len * 0.1, 0, line_domain);
  
  glFragColor = vec4(len, len * 0.1, 1 - len*0.5, line_domain + 0.3);
   // glFragColor = vec4(0.5-len, len*0.1, 1, line_domain);
   
   
  // float r = 0.5 * (1.0 + velocity.x / (len + 0.0001f));
  // float g = 0.5 * (1.0 + velocity.y / (len + 0.0001f));
  // float b = 0.5 * (2.0 - (r + g));
  // glFragColor = vec4(r,g,b,1);
}

