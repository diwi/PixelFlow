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

uniform float velocity_mult = 1.0;
uniform int   spawn_hi;
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
    if(acc_len < 0.05){
      acc = vec2(0.0);
      acc_len = 0.0;
    }
    if(acc_len > 1.0){
      acc /= acc_len;
    }
    
    // TODO: proper clamping, etc...
    
    // update position
    particle_pos.xy += acc * 0.5 * velocity_mult * wh_velocity_rcp;
  }

  out_frag = particle_pos;
}
