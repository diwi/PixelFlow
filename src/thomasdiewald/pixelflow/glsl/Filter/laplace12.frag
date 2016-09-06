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
uniform vec2      wh; 

// LAPLACE FILTER (highpass)
//                                  
//   0  1  0    1  1  1    1   2   1
//   1 -4  1    1 -8  1    2 -12   2
//   0  1  0    1  1  1    1   2   1
//                                  

void main(){
  vec2 posn = gl_FragCoord.xy / wh;
  
  vec4 laplace = texture(tex, posn) * +12;
  
  laplace += textureOffset(tex, posn, ivec2(-1, 0)) * -2.0;
  laplace += textureOffset(tex, posn, ivec2(+1, 0)) * -2.0;
  laplace += textureOffset(tex, posn, ivec2( 0,-1)) * -2.0;
  laplace += textureOffset(tex, posn, ivec2( 0,+1)) * -2.0;
  laplace += textureOffset(tex, posn, ivec2(-1,-1)) * -1.0;
  laplace += textureOffset(tex, posn, ivec2(+1,-1)) * -1.0;
  laplace += textureOffset(tex, posn, ivec2(-1,+1)) * -1.0;
  laplace += textureOffset(tex, posn, ivec2(+1,+1)) * -1.0;
  
  glFragColor = laplace;
  // glFragColor = vec4(laplace.xyz * 0.5 + 0.5, 1); // for UNSIGNED_BYTE
}



