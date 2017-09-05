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

out vec4 out_pos_vel;

uniform int   spawn_lo;
uniform int   spawn_hi;
uniform ivec2 num_xy;
uniform ivec2 wh_position;

uniform sampler2D tex_position;

void main(){
  
  // prepare particle index, based on the current fragment position
  ivec2 tex_loc  = ivec2(gl_FragCoord.xy);
  int   particle_idx = tex_loc.y * wh_position.x + tex_loc.x;
  
  // check if the current fragment is one of those particles to spawn now
  if(particle_idx >= spawn_lo && particle_idx < spawn_hi){
    int idx = particle_idx - spawn_lo;
    int row = idx / num_xy.x;
    int col = idx - num_xy.x * row;
    vec2 pos = vec2(col + 0.5, row + 0.5) / vec2(num_xy);
    out_pos_vel = vec4(pos, pos);
  } else {
    out_pos_vel = texelFetch(tex_position, tex_loc, 0);
  }

}

