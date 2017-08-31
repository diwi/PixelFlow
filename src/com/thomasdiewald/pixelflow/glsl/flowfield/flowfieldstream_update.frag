/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150 

out vec4 out_frag;

uniform vec2  wh_velocity_rcp;
uniform float timestep;
uniform float rdx;
uniform float dissipation;
uniform float	inertia;

uniform sampler2D tex_position;
uniform sampler2D tex_velocity;

void main(){

  // get previous pos/vel
  vec4 position = texelFetch(tex_position, ivec2(gl_FragCoord.xy), 0);
   
  vec2 pos = position.xy;
	vec2 vel = position.zw;

  // get new velocity
  vec2 vel_cur = texture(tex_velocity, pos).xy;

  // normalize velocity
  float vel_len = length(vel_cur);
  vel_cur = clamp(vel_len, 0.0, 1.0) * vel_cur / (vel_len + 0.000001);

  // update velocity
  vel = mix(vel_cur, vel, inertia) * dissipation;
  
  // update position
  pos += rdx * timestep * vel * wh_velocity_rcp;
  
  out_frag = vec4(pos, vel);
}
