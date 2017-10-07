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

uniform float line_uv;

void main(){

  float alpha = 1.0-line_uv + 0.1;
  alpha = clamp(alpha, 0, 1);
  // glFragColor = vec4(1.0, 1.0-line_uv, 1.0-line_uv, alpha);
  glFragColor = vec4(line_uv, line_uv*0.5f, 0, alpha);
  

  // float len = line_uv;
  // glFragColor = vec4(len, 0.5, 1-len, len * 0.5 + 0.5);
  // glFragColor = vec4(0, sqrt(len)*0.9, len*len, (len) * 0.8 + 0.2);
  

}


