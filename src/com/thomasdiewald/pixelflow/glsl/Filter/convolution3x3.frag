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
uniform vec2      wh_rcp; 
uniform float     kernel[9];
          
// kernel: 0 1 2
//         3 4 5
//         6 7 8  

void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
  vec4 convolution = vec4(0);
  
  convolution += textureOffset(tex, posn, ivec2(-1,-1)) * kernel[0];
  convolution += textureOffset(tex, posn, ivec2( 0,-1)) * kernel[1];
  convolution += textureOffset(tex, posn, ivec2(+1,-1)) * kernel[2];
  
  convolution += textureOffset(tex, posn, ivec2(-1, 0)) * kernel[3];
  convolution += texture      (tex, posn              ) * kernel[4];
  convolution += textureOffset(tex, posn, ivec2(+1, 0)) * kernel[5];
  
  convolution += textureOffset(tex, posn, ivec2(-1,+1)) * kernel[6];
  convolution += textureOffset(tex, posn, ivec2( 0,+1)) * kernel[7];
  convolution += textureOffset(tex, posn, ivec2(+1,+1)) * kernel[8];
  
  glFragColor = convolution;
  glFragColor.a = 1.0;
}



