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

uniform sampler2D	tex;
uniform vec2  wh_rcp; 
uniform int   radius;
uniform float sigma_color;
uniform float sigma_space;

void main(){
  float sigma_color_sqinv = -0.5 / (sigma_color * sigma_color);
  float sigma_space_sqinv = -0.5 / (sigma_space * sigma_space);
  
  vec3 pC = texture(tex, gl_FragCoord.xy * wh_rcp).xyz;
  vec3 pS = vec3(0);
  float norm = 0;
  
  for(int y = -radius; y <= radius; y++){
  for(int x = -radius; x <= radius; x++){
  
    vec3 pN = texture(tex, (gl_FragCoord.xy + ivec2(x, y)) * wh_rcp).xyz;
    vec3 pD = pC - pN;
    
    float domain = (x*x + y*y) * sigma_space_sqinv;
    float range  = dot(pD, pD) * sigma_color_sqinv;
    float rd_exp = exp(domain + range);
    
    pS += pN * rd_exp;
    norm += rd_exp;
  }
  }
  
  glFragColor = vec4(pS / norm, 1);

}



