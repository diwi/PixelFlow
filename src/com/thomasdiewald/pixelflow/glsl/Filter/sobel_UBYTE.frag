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
uniform ivec2     dir; // horz (1,0) ... vert (0,1)

// SOBEL 3x3      horz          vert            diag 1          diag 2
//                +1  0 -1      +1  +2  +1      +2  +1   0       0  -1  -2
//                +2  0 -2       0   0   0      +1   0  -1      +1   0  -1
//                +1  0 -1      -1  -2  -1       0  -1  -2      +2  +1   0

void main(){
  vec2 posn = gl_FragCoord.xy / wh;
  
  vec4 sobel = vec4(0);
  
  sobel += texture(tex, posn + (-dir - dir.yx)/wh) * -1;   //ivec2(-1,-1)
  sobel += texture(tex, posn + (-dir         )/wh) * -2;   //ivec2(-1, 0)
  sobel += texture(tex, posn + (-dir + dir.yx)/wh) * -1;   //ivec2(-1,+1)
 
  sobel += texture(tex, posn + (+dir - dir.yx)/wh) * +1;    //ivec2(+1,-1)
  sobel += texture(tex, posn + (+dir         )/wh) * +2;    //ivec2(+1, 0)
  sobel += texture(tex, posn + (+dir + dir.yx)/wh) * +1;    //ivec2(+1,+1)
  
  // glFragColor = sobel;
  // glFragColor = vec4(abs(sobel.xyz), 1); // for UNSIGNED_BYTE
  glFragColor = vec4(sobel.xyz * 0.5 + 0.5, 1); // for UNSIGNED_BYTE
}



