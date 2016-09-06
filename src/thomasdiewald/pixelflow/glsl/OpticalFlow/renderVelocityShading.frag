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


uniform vec2 wh;
uniform sampler2D tex_velocity;


void main(){

  vec2 posn = gl_FragCoord.xy / wh;
  vec2 flow = texture(tex_velocity, posn).xy;
  
  float mag = length(flow) * 1;
  mag = clamp(mag, 0.0, 1.0);
  
  
  // glFragColor = vec4(0, 0, 0, 1.0-mag);
  // glFragColor = vec4(mag, 0, 0, 1);
  
  
  float len = mag;
  float r = 0.5 * (1.0 + flow.x / (len + 0.0001f));
  float g = 0.5 * (1.0 + flow.y / (len + 0.0001f));
  float b = 0.5 * (2.0 - (r + g));
  glFragColor = vec4(r,g,b,len);
  
}

