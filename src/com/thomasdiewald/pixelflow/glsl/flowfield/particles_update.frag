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

#define UPDATE_VEL 0
#define UPDATE_ACC 0


void limitLength(inout vec2 vel, in vec2 lohi){
  float vel_len = length(vel);
  if(vel_len <= lohi.x){
    vel *= 0.0;
  } else {
    vel *= clamp(vel_len - lohi.x, 0, lohi.y) / vel_len;
  }
}

void main(){

  // particle index, based on the current fragment position
  ivec2 tex_loc  = ivec2(gl_FragCoord.xy);
  int   particle_idx = tex_loc.y * wh_position.x + tex_loc.x;
  vec4  particle_pos = texelFetch(tex_position, tex_loc, 0);
  
  vec2 pos_cur = particle_pos.xy;
  vec2 pos_old = particle_pos.zw;

  if(particle_idx < spawn_hi){
  
#if UPDATE_VEL
    // velocity
    vec2 vel = (pos_cur - pos_old) / wh_velocity_rcp;
    // fix length
    limitLength(vel, vel_minmax);
    // update position, verlet integration
    pos_old = pos_cur;
    pos_cur += (vel * vel_mult) * wh_velocity_rcp;
#endif
  

#if UPDATE_ACC
    // acceleration
    vec2 acc = texture(tex_velocity, pos_cur).xy;
    // fix length
    limitLength(acc, acc_minmax);
    // update position
    pos_cur += (acc * acc_mult) * wh_velocity_rcp;
#endif

  }

  out_frag = vec4(pos_cur, pos_old);
}
