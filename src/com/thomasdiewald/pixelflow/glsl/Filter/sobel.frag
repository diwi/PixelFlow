/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

#define SOBEL_3x3_HORZ 0
#define SOBEL_3x3_VERT 0
#define SOBEL_3x3_TLBR 0
#define SOBEL_3x3_BLTR 0

out vec4 glFragColor;
uniform sampler2D	tex;
uniform vec2      wh_rcp;
uniform vec2      mad = vec2(1.0, 0.0);

// SOBEL 3x3      HORZ          VERT            TLBR            BLTR         +y
//                -1  0 +1      +1  +2  +1      -2  -1   0       0  +1  +2    |
//                -2  0 +2       0   0   0      -1   0  +1      -1   0  +1    |
//                -1  0 +1      -1  -2  -1       0  +1  +2      -2  -1   0    o- - - +x

void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
  vec4 sobel = vec4(0);

#if SOBEL_3x3_HORZ
  sobel += textureOffset(tex, posn, ivec2(-1,-1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(-1, 0)) * -2;
  sobel += textureOffset(tex, posn, ivec2(-1,+1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(+1,-1)) * +1;
  sobel += textureOffset(tex, posn, ivec2(+1, 0)) * +2;
  sobel += textureOffset(tex, posn, ivec2(+1,+1)) * +1;
#elif SOBEL_3x3_VERT
  sobel += textureOffset(tex, posn, ivec2(-1,-1)) * -1;
  sobel += textureOffset(tex, posn, ivec2( 0,-1)) * -2;
  sobel += textureOffset(tex, posn, ivec2(+1,-1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(-1,+1)) * +1;
  sobel += textureOffset(tex, posn, ivec2( 0,+1)) * +2;
  sobel += textureOffset(tex, posn, ivec2(+1,+1)) * +1;
#elif SOBEL_3x3_TLBR
  sobel += textureOffset(tex, posn, ivec2(-1, 0)) * -1;
  sobel += textureOffset(tex, posn, ivec2(-1,+1)) * -2;
  sobel += textureOffset(tex, posn, ivec2( 0,+1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(+1, 0)) * +1;
  sobel += textureOffset(tex, posn, ivec2(+1,-1)) * +2;
  sobel += textureOffset(tex, posn, ivec2( 0,-1)) * +1;
#elif SOBEL_3x3_BLTR
  sobel += textureOffset(tex, posn, ivec2(-1, 0)) * -1;
  sobel += textureOffset(tex, posn, ivec2(-1,-1)) * -2;
  sobel += textureOffset(tex, posn, ivec2( 0,-1)) * -1;
  sobel += textureOffset(tex, posn, ivec2(+1, 0)) * +1;
  sobel += textureOffset(tex, posn, ivec2(+1,+1)) * +2;
  sobel += textureOffset(tex, posn, ivec2( 0,+1)) * +1;
#endif

  glFragColor = sobel * mad.x + mad.y;
}



