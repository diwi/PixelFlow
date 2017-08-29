/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150 

out vec2 out_pos;

uniform sampler2D tex_position;
uniform sampler2D tex_velocity;

uniform vec2  wh_velocity_rcp;
uniform vec2  wh_position_rcp;
uniform float timestep;
uniform float rdx;
uniform float vel_scale;
uniform vec2  vel_minmax;


void main(){

  vec2 pos = texture(tex_position, gl_FragCoord.xy * wh_position_rcp).xy;
  vec2 vel = texture(tex_velocity, pos.xy).xy;

  // normalize velocity
  float vel_len = length(vel) + 0.0001;
  vel /= vel_len;
  
  // scale + clamp
  vel *= vel_scale * clamp(vel_len, vel_minmax.x, vel_minmax.y);
  
  out_pos = pos + rdx * timestep * vel * wh_velocity_rcp;
}
