/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150 

precision mediump float;
precision mediump int;

out vec2 glFragColor;

uniform sampler2D tex_particles;
uniform sampler2D tex_velocity;
uniform sampler2D tex_obstacles;

uniform vec2  wh_fluid;
uniform vec2  wh_particles;
uniform float timestep ;
uniform float rdx;
uniform float velocity_scale;
uniform float velocity_min;

void main(){
  vec4 particle_data = texture(tex_particles, gl_FragCoord.xy / wh_particles);
  vec2 pos_old = particle_data.xy;
  vec2 dir_new = texture(tex_velocity, pos_old).xy;

  float len = length(dir_new);
  vec2 dir_new_min = velocity_scale * max(velocity_min, len) * dir_new/(len + 0.0001);
  
  glFragColor = pos_old + rdx * timestep * dir_new_min / wh_fluid;
}
