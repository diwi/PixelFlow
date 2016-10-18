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

uniform sampler2D tex_velocity;
uniform sampler2D tex_source;
uniform sampler2D tex_obstacleC;

uniform vec2  wh_inv;
uniform float timestep;
uniform float rdx;
uniform float dissipation;

void main(){
  vec2 posn = gl_FragCoord.xy * wh_inv;
  
  float oC = texture(tex_obstacleC, posn).x;
  if (oC == 1.0) {
    glFragColor = vec4(0);
  } else {
    vec2 velocity = texture(tex_velocity, posn).xy;
    vec2 posn_back = posn - timestep * rdx * velocity * wh_inv;
    glFragColor = dissipation * texture(tex_source, posn_back);
  }
}

