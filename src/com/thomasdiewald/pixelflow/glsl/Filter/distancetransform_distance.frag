/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

 
#version 150

out float out_distance;

uniform isampler2D tex_dtnn;

void main(){
  ivec2 pos = ivec2(gl_FragCoord.xy);
  ivec2 dtnn = texelFetch(tex_dtnn, pos, 0).xy;
  out_distance = length(vec2(dtnn - pos));
}



