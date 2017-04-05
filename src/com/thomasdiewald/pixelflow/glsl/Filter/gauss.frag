/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

#define PI (3.14159265)

out vec4 glFragColor;

uniform sampler2D	tex;
uniform vec2  wh_rcp; 
uniform int   radius;
uniform float sigma;
uniform ivec2 dir;

// void main(){
  // vec4 blur = texture(tex, gl_FragCoord.xy * wh_rcp);
  // float norm = 1;
  
  // for(int i = 1; i <= +radius; i++){
    // float coeff = exp(-0.5 * float(i) * float(i) / (sigma * sigma));
    // norm += coeff * 2.0;
    // blur += texture(tex, posn, (gl_FragCoord.xy + dir * i) * wh_rcp) * coeff;
    // blur += texture(tex, posn, (gl_FragCoord.xy - dir * i) * wh_rcp) * coeff;
  // }
  
  // glFragColor = blur / norm;
// }


// http://http.developer.nvidia.com/GPUGems3/gpugems3_ch40.html
void main(){

  vec3 coeff;  
  coeff.x = 1.0 / (sqrt(2.0 * PI) * sigma);  
  coeff.y = exp(-0.5 / (sigma * sigma));  
  coeff.z = coeff.y * coeff.y;  
  
  vec4 blur = vec4(0.0);
  float norm = 0.0;
  
  blur += texture(tex, gl_FragCoord.xy * wh_rcp) * coeff.x;
  norm += coeff.x;
  coeff.xy *= coeff.yz;  
  
  for(int i = 1; i <= radius; i++){
    blur += texture(tex, (gl_FragCoord.xy + dir * i) * wh_rcp) * coeff.x;
    blur += texture(tex, (gl_FragCoord.xy - dir * i) * wh_rcp) * coeff.x;
    norm += coeff.x * 2.0;
    coeff.xy *= coeff.yz;  
  }
  
  glFragColor = blur / norm;
}


