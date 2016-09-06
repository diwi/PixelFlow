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


uniform vec2 wh_inv;
uniform sampler2D tex_obstacleC;

void main(){
  vec2 posn = gl_FragCoord.xy * wh_inv;
  
  glFragColor = vec4(0);
  
  // neighboring obstacles
  glFragColor.x = textureOffset(tex_obstacleC, posn, + ivec2(0,1)).x;
  glFragColor.y = textureOffset(tex_obstacleC, posn, - ivec2(0,1)).x;
  glFragColor.z = textureOffset(tex_obstacleC, posn, + ivec2(1,0)).x;
  glFragColor.w = textureOffset(tex_obstacleC, posn, - ivec2(1,0)).x;
}

