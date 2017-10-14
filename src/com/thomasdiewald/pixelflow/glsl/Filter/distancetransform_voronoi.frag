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

uniform vec2 wh_rcp;
uniform vec2 wh_src_rcp;
uniform vec2 wh_dtnn_rcp;
uniform float dist_norm;

void main(){

  // ivec2 posi_dtnn = ivec2(gl_FragCoord.xy);
  // ivec2 dtnn = texelFetch(tex_dtnn, posi_dtnn, 0).xy;
  // vec4 data = texelFetch(tex_src, dtnn, 0);
 
  // normalized fraglocation
  vec2 posn = (gl_FragCoord.xy) * wh_rcp;
   
  // un-normalized fraglocation in dtnn texture space
  ivec2 posi_dtnn = ivec2(posn / wh_dtnn_rcp);
  ivec2 dtnn = texelFetch(tex_dtnn, posi_dtnn, 0).xy;
  
  // un-normalized fraglocation in src texture space
  ivec2 posi_src = ivec2((dtnn+0.5) * wh_dtnn_rcp / wh_src_rcp);
  vec4 data = texelFetch(tex_src, posi_src, 0);
  
  // shade by distance
  float dtnn_dist = length(vec2(dtnn - posi_dtnn)) * dist_norm;
  data *= sqrt(dtnn_dist);

  glFragColor = data;
  glFragColor.a = 1.0;
  
}





