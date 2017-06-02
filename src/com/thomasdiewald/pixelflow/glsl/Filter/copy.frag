/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

#define FLIP_Y 0

#if FLIP_Y
  layout(origin_upper_left) in vec4 gl_FragCoord;
#endif

out vec4 glFragColor;

uniform sampler2D	tex;
uniform vec2 wh_rcp; 

void main(){
  glFragColor = texture(tex, gl_FragCoord.xy * wh_rcp);
  
  // vec2 posn = gl_FragCoord.xy * wh_rcp;
  // posn.y = 1.0 - posn.y;
  // glFragColor = texture(tex, posn);
}





