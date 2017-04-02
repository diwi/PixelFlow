/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

out vec4 glFragColor;

uniform sampler2D	tex;
uniform vec2 wh_rcp; 
uniform vec3 luminance = vec3(0.2989, 0.5870, 0.1140);
uniform float threshold = 0.5;
uniform int   exponent = 5;

void main(){

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  vec4 color = texture(tex, posn);
  float luma = dot(color.rgb, luminance);

  luma = min(1.0, 1.0 + (luma - threshold));

  glFragColor = pow(luma, exponent) * color;

}

