/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
 
#version 150

#define PER_CHANNEL 0

out vec4 out_frag;

uniform vec2 wh_rcp;
uniform sampler2D tex_src;
uniform sampler2D tex_minmax;

void main(){
  vec4 rgba_min = texelFetch(tex_minmax, ivec2(0,0), 0);
  vec4 rgba_max = texelFetch(tex_minmax, ivec2(1,0), 0);
  vec4 rgba = texture(tex_src, gl_FragCoord.xy * wh_rcp);
  
#if (PER_CHANNEL==1)

   out_frag = (rgba - rgba_min) / (rgba_max - rgba_min);
   
#else 

  float lo = min(min(rgba_min.x, rgba_min.y), rgba_min.z);
  float hi = max(max(rgba_max.x, rgba_max.y), rgba_max.z);
  out_frag = (rgba - lo) / (hi - lo);
  
#endif

}





