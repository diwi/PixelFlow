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

uniform int   spawn_hi;
uniform float acc_mult = 1.0;
uniform float vel_mult = 1.0;
uniform vec2  acc_minmax;
uniform vec2  vel_minmax;
uniform ivec2 wh_position;
uniform vec2  wh_velocity_rcp;
uniform sampler2D tex_position;
uniform sampler2D tex_velocity;

#define UPDATE_VEL 1
#define UPDATE_ACC 1

void main(){

  // particle index, based on the current fragment position
  ivec2 tex_loc  = ivec2(gl_FragCoord.xy);
  int   particle_idx = tex_loc.y * wh_position.x + tex_loc.x;
  vec4  particle_pos = texelFetch(tex_position, tex_loc, 0);
  
  vec2 pos_cur = particle_pos.xy;
  vec2 pos_old = particle_pos.zw;

  if(particle_idx < spawn_hi){
  
#if UPDATE_VEL
    // velocity, clamped
    vec2 vel = (pos_cur - pos_old) / wh_velocity_rcp;
    float vel_len = length(vel);
    if(vel_len <= vel_minmax.x){
      vel *= 0.0;
    } else {
      vel *= clamp(vel_len - vel_minmax.x, 0, vel_minmax.y) / vel_len;
    }
    
    // update position, verlet integration
    pos_old = pos_cur;
    pos_cur += (vel * vel_mult) * wh_velocity_rcp;
#endif
  

#if UPDATE_ACC
    // acceleration, clamped
    vec2 acc = texture(tex_velocity, pos_cur).xy;
    float acc_len = length(acc);
    if(acc_len <= acc_minmax.x){
      acc *= 0.0;
    } else {
      acc *= clamp(acc_len - acc_minmax.x, 0, acc_minmax.y) / acc_len;
    }
    // update position
    pos_cur += (acc * acc_mult) * wh_velocity_rcp;
#endif

  }

  out_frag = vec4(pos_cur, pos_old);
}
