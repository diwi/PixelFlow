/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec4 out_frag;

uniform vec2 wh_rcp;
uniform sampler2D tex_velocity;

void main(){

  vec2 vel = texture(tex_velocity, gl_FragCoord.xy * wh_rcp).xy;
  
  float len = length(vel);
  len = min(len, 1.0);

  float r = 0.5 * (1.0 + vel.x);
  float g = 0.5 * (1.0 + vel.y);
  float b = 0.5 * (2.0 - (r + g));

  out_frag = vec4(r, g, b, len);
  // out_frag = vec4(len,len,len,1);
}


