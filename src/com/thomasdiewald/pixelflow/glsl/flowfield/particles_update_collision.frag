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
uniform vec2  acc_minmax;
uniform ivec2 wh_position;
uniform vec2  wh_velocity_rcp;
uniform sampler2D tex_position;
uniform sampler2D tex_velocity;


void main(){

  // particle index, based on the current fragment position
  ivec2 tex_loc  = ivec2(gl_FragCoord.xy);
  int   particle_idx = tex_loc.y * wh_position.x + tex_loc.x;
  vec4  particle_pos = texelFetch(tex_position, tex_loc, 0);

  if(particle_idx < spawn_hi){
    // acceleration
    vec2 acc = texture(tex_velocity, particle_pos.xy).xy;
    float acc_len = length(acc);
    if(acc_len <= acc_minmax.x){
      acc *= 0.0;
    } else {
      acc *= clamp(acc_len - acc_minmax.x, 0, acc_minmax.y) / acc_len;
    }
    
    
    // update position
    particle_pos.xy += acc * acc_mult * wh_velocity_rcp;
  }

  out_frag = particle_pos;
}
