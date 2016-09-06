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

uniform sampler2D tex_velocity;
uniform sampler2D tex_temperature;
uniform sampler2D tex_density;

uniform vec2  wh_inv;
uniform float temperature_ambient;
uniform float timestep;
uniform float fluid_buoyancy;
uniform float fluid_weight;

void main(){
  vec2 posn = wh_inv * gl_FragCoord.xy;
  
  vec2  velocity    = texture(tex_velocity   , posn).xy;
  float temperature = texture(tex_temperature, posn).x;
  
  float dtemp = temperature - temperature_ambient;
  if (dtemp != 0.0) {
    float density = texture(tex_density, posn).a;
    float buoyancy_force = timestep * dtemp * fluid_buoyancy - density * fluid_weight;
    velocity += vec2(0, 1) * buoyancy_force;
  }
  
  glFragColor = velocity;
}

