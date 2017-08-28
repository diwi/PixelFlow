/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
 
// Min/Max

#version 150

#define MODE 0

out vec4 out_frag;

uniform vec2 wh_rcp;
uniform sampler2D texA;
uniform sampler2D texB;

void main(){
  vec2 pos = gl_FragCoord.xy * wh_rcp;
  
#if (MODE_MIN == 0)
  out_frag = min(texture(texA, pos), texture(texB, pos));
#else
  out_frag = max(texture(texA, pos), texture(texB, pos));
#endif

}





