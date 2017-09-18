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

uniform vec2 mad;
uniform  sampler2D tex_FG;
uniform isampler2D tex_dtnn;

void main(){
  ivec2 pos  = ivec2(gl_FragCoord.xy);
  
  float FG   = texelFetch(tex_FG, pos, 0).x * 2.0 - 1.0; // -1.0, +1.0
  ivec2 dtnn = texelFetch(tex_dtnn, pos, 0).xy;
  float dist = length(vec2(dtnn - pos));

  out_distance = dist * FG * mad.x + mad.y;
  
  out_distance = max(0, out_distance);
}



