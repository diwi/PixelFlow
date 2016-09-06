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

// SOBEL 3x3      HORZ          VERT            TLBR            BLTR         +y
//                -1  0 +1      +1  +2  +1      -2  -1   0       0  +1  +2    |
//                -2  0 +2       0   0   0      -1   0  +1      -1   0  +1    |
//                -1  0 +1      -1  -2  -1       0  +1  +2      -2  -1   0    o- - - +x


#define HORZ 1
#define VERT 0
#define TLBR 0
#define BLTR 0

void main(){
  vec2 posn = gl_FragCoord.xy / wh;
  
  vec4 sobel = vec4(0);

#if HORZ 
  sobel += textureOffset(tex, posn, ivec2(-1,-1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(-1, 0)) * -2;
  sobel += textureOffset(tex, posn, ivec2(-1,+1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(+1,-1)) * +1;
  sobel += textureOffset(tex, posn, ivec2(+1, 0)) * +2;
  sobel += textureOffset(tex, posn, ivec2(+1,+1)) * +1;
#elif VERT
  sobel += textureOffset(tex, posn, ivec2(-1,-1)) * -1;
  sobel += textureOffset(tex, posn, ivec2( 0,-1)) * -2;
  sobel += textureOffset(tex, posn, ivec2(+1,-1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(-1,+1)) * +1;
  sobel += textureOffset(tex, posn, ivec2( 0,+1)) * +2;
  sobel += textureOffset(tex, posn, ivec2(+1,+1)) * +1;
#elif TLBR
  sobel += textureOffset(tex, posn, ivec2(-1, 0)) * -1;
  sobel += textureOffset(tex, posn, ivec2(-1,+1)) * -2;
  sobel += textureOffset(tex, posn, ivec2( 0,+1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(+1, 0)) * +1;
  sobel += textureOffset(tex, posn, ivec2(+1,-1)) * +2;
  sobel += textureOffset(tex, posn, ivec2( 0,-1)) * +1;
#elif BLTR
  sobel += textureOffset(tex, posn, ivec2(-1, 0)) * -1;
  sobel += textureOffset(tex, posn, ivec2(-1,-1)) * -2;
  sobel += textureOffset(tex, posn, ivec2( 0,-1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(+1, 0)) * +1;
  sobel += textureOffset(tex, posn, ivec2(+1,+1)) * +2;
  sobel += textureOffset(tex, posn, ivec2( 0,+1)) * +1;
#endif

  glFragColor = sobel;
  // glFragColor = vec4(abs(sobel.xyz), 1); // for UNSIGNED_BYTE
  // glFragColor = vec4(sobel.xyz * 0.5 + 0.5, 1); // for UNSIGNED_BYTE
}



