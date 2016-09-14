/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150 


// TWO_PI = PI * 2
#define TWO_PI 6.2831855
// GOLDEN_ANGLE_R =  PI * (3 - sqrt(5))
#define GOLDEN_ANGLE_R 2.3999631 


out vec4 glFragColor;



uniform int   spawn_lo;
uniform int   spawn_hi;
uniform vec2  spawn_origin;
uniform float spawn_radius;
uniform float noise;
uniform vec2  wh_particles;
uniform vec2  wh_viewport = vec2(1);

uniform sampler2D tex_particels;

void main(){
  
  // prepare particle index, based on the current fragment position
  ivec2 tex_loc  = ivec2(gl_FragCoord.xy);
  ivec2 tex_size = ivec2(wh_particles);
  int   particle_idx = tex_loc.y * tex_size.x + tex_loc.x;
  
  // particle data
  vec4 particle_data = vec4(0);
  
  // check if the current fragment is one of those particles to spawn now
  if(particle_idx >= spawn_lo && particle_idx < spawn_hi){
    // set new particle data
    float spawn_count = float(spawn_hi - spawn_lo);
    float spawn_idx   = float(particle_idx - spawn_lo);
    float spawn_idxn  = spawn_idx / spawn_count;
   
    // spawn fibonacci pattern for uniform distribution over a radial area
 
    float radius = spawn_radius * sqrt(spawn_idxn * spawn_idxn * spawn_idxn);
    // float radius = spawn_radius * sqrt(spawn_idxn); // uniform distribution
    
    float angle = spawn_idx * GOLDEN_ANGLE_R + noise;
    vec2 xy = vec2(cos(angle), sin(angle));
   
    particle_data.xy = spawn_origin + xy * radius;
    particle_data.xy /= wh_viewport; // normalize
  } else {
    // keep old particle data
    particle_data = texture(tex_particels, gl_FragCoord.xy/wh_particles);
  }
  
  glFragColor = particle_data;
}

