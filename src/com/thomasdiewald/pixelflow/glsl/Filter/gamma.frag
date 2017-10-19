/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec4 glFragColor;

uniform sampler2D	tex;
uniform float gamma = 2.2;
uniform vec2  wh_rcp; 

void main(){
  vec4 rgba = texture(tex, gl_FragCoord.xy * wh_rcp);
  rgba.xyz = pow(rgba.xyz, vec3(1.0/gamma));
  glFragColor = rgba;
  
}



