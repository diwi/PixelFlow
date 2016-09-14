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

out vec4 glFragColor;

uniform sampler2D tex_particles;
uniform sampler2D tex_velocity;
uniform sampler2D tex_obstacles;

uniform vec2  wh_fluid;
uniform vec2  wh_particles;
uniform float timestep ;
uniform float rdx;
uniform float dissipation;
uniform float	inertia;


void main(){
  vec4 particle_data = texture(tex_particles, gl_FragCoord.xy / wh_particles);
   
  vec2 pos_old = particle_data.xy;
	vec2 dir_old = particle_data.zw;
  
  float obstacle = texture(tex_obstacles, pos_old).x;
  if( obstacle > 0.0 
   || pos_old.x < 0.0 || pos_old.x > 1.0 
   || pos_old.y < 0.0 || pos_old.y > 1.0 
   ) 
  {
    // step back
    pos_old -= rdx * timestep * dir_old / wh_fluid;
    glFragColor = vec4(pos_old, dir_old);
  } 
  else 
  {
    vec2 dir_cur = texture(tex_velocity, pos_old).xy;
    vec2 dir_new = mix(dir_cur, dir_old, inertia) * dissipation;

    vec2 pos_new = pos_old + rdx * timestep * dir_new / wh_fluid;
    glFragColor = vec4(pos_new, dir_new);
  }

}
