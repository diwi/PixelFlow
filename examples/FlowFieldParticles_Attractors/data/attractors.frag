/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

 
#version 150

#define ATTRACTOR_NUM 1

out vec2 out_vel;

uniform float attractor_mult = 1.0;
uniform float attractor_mass[ATTRACTOR_NUM];
uniform vec2  attractor_pos[ATTRACTOR_NUM];

void main(){
  vec2 field_pos = gl_FragCoord.xy;
  
  vec2 vel_sum = vec2(0.0);
  
  for(int i = 0; i < ATTRACTOR_NUM; i++){
    vec2 vel_i = attractor_pos[i] - field_pos;
    float dist = dot(vel_i, vel_i);
    if(dist != 0.0){
      dist = sqrt(dist);
      vel_i /= dist;
 
      // dist = max(1.0, dist);
      vel_sum += vel_i * attractor_mult * attractor_mass[i] / dist;
    }
 
    
  }

  out_vel = vel_sum;
}
