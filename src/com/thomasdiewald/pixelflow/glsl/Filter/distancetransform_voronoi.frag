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

uniform  sampler2D tex_src;
uniform isampler2D tex_dtnn;

uniform float dist_norm;

void main(){
  ivec2 pos = ivec2(gl_FragCoord.xy);
  ivec2 dtnn = texelFetch(tex_dtnn, pos, 0).xy;
  vec4  data = texelFetch(tex_src, dtnn, 0);
  
  // shade by distance
  float dis = length(vec2(dtnn - pos)) * dist_norm;
  // data *= (1.0 - dis*dis);
  // data *= (1.0 - dis)* 1.5f; 
  data *= sqrt(dis) * 1.4f;
  
  glFragColor = data;
  glFragColor.a = 1.0;
}





