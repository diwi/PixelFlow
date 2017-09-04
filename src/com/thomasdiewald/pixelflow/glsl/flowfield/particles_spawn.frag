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


#define TWO_PI 6.2831855
// GOLDEN_ANGLE_R =  PI * (3 - sqrt(5))
#define GOLDEN_ANGLE_R 2.3999631 

out vec4 out_pos_vel;

uniform int   spawn_lo;
uniform int   spawn_hi;
uniform vec2  spawn_pos;
uniform float spawn_rad;
uniform float noise;
uniform ivec2 wh_position;
uniform vec2  wh_viewport_rcp = vec2(1.0);

uniform sampler2D tex_position;

void main(){
  
  // prepare particle index, based on the current fragment position
  ivec2 tex_loc  = ivec2(gl_FragCoord.xy);
  int   particle_idx = tex_loc.y * wh_position.x + tex_loc.x;
  
  // check if the current fragment is one of those particles to spawn now
  if(particle_idx >= spawn_lo && particle_idx < spawn_hi){
  
    // set new particle data
    float spawn_count = float(spawn_hi - spawn_lo);
    float spawn_idx   = float(particle_idx - spawn_lo);
    float spawn_idxn  = (spawn_idx+1) / spawn_count;
   
    // spawn fibonacci pattern for uniform distribution over a radial area
    // float radius = spawn_rad * sqrt(spawn_idxn * spawn_idxn * spawn_idxn);
    float radius = spawn_rad * sqrt(spawn_idxn); // uniform distribution

    float angle = spawn_idx * GOLDEN_ANGLE_R + noise;
    vec2 xy = vec2(cos(angle), sin(angle));
   
    vec2 pos = (spawn_pos + xy * radius) * wh_viewport_rcp;

    out_pos_vel = vec4(pos, pos);
  } else {
    out_pos_vel = texelFetch(tex_position, tex_loc, 0);
  }

}

