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

void main(void) {
  // gl_FragColor = vec4(gl_FragCoord.z, 1.0/gl_FragCoord.w, 0, 0);
  glFragColor = vec4(gl_FragCoord.z, 0, 0, 0);
}

