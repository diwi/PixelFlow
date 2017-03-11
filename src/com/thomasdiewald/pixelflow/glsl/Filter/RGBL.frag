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
uniform vec2 wh; 
uniform vec3 luminance = vec3(0.2989, 0.5870, 0.1140);
uniform float gamma = 2.2;

void main(){
  vec3 rgb = texture(tex, gl_FragCoord.xy / wh).rgb;
  
  //rgb = pow(rgb, vec3(1.0/gamma));
  
  // float lum = dot(rgb, luminance);
  
  // lum = pow(lum, vec3(1.0/gamma));
  
  // glFragColor = vec4(rgb, lum);
  
  //glFragColor = texture(tex, gl_FragCoord.xy / wh);
  //glFragColor.a = dot(glFragColor.rgb, luminance);
  
  glFragColor = vec4(rgb, dot(rgb, luminance));
}





