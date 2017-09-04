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

uniform float timestep = 1.0;
uniform float damping = 1.0;
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
  
  vec2 pos_cur = particle_pos.xy;
  vec2 pos_old = particle_pos.zw;

  if(particle_idx < spawn_hi){
  
    // acceleration
    vec2 acc = texture(tex_velocity, pos_cur).xy;
    
    // TODO: proper clamping, etc...

    // velocity
    vec2 vel = (pos_cur - pos_old) * damping;
    pos_old = pos_cur;

    // verlet integration
    pos_cur += vel + acc * 0.5 * timestep * timestep * wh_velocity_rcp;
  }

  out_frag = vec4(pos_cur, pos_old);
}
