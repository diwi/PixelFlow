/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */


#version 150

#define LAPLACE_3x3_W4 0
#define LAPLACE_3x3_W8 0
#define LAPLACE_3x3_W12 0

// LAPLACE FILTER (highpass)
//                                  
//   0  1  0    1  1  1    1   2   1
//   1 -4  1    1 -8  1    2 -12   2
//   0  1  0    1  1  1    1   2   1
//    

out vec4 glFragColor;
uniform sampler2D	tex;
uniform vec2      mad = vec2(1.0, 0.0);
uniform vec2      wh_rcp; 

                             
void main(){
  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
  vec4 laplace = vec4(0.0);
  
#if LAPLACE_3x3_W4
  laplace += textureOffset(tex, posn, ivec2( 0, 0)) * + 4.0;
  laplace += textureOffset(tex, posn, ivec2(-1, 0)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(+1, 0)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2( 0,-1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2( 0,+1)) * - 1.0;
//laplace += textureOffset(tex, posn, ivec2(-1,-1)) * - 1.0;
//laplace += textureOffset(tex, posn, ivec2(+1,-1)) * - 1.0;
//laplace += textureOffset(tex, posn, ivec2(-1,+1)) * - 1.0;
//laplace += textureOffset(tex, posn, ivec2(+1,+1)) * - 1.0;
#endif

#if LAPLACE_3x3_W8
  laplace += textureOffset(tex, posn, ivec2( 0, 0)) * + 8.0;
  laplace += textureOffset(tex, posn, ivec2(-1, 0)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(+1, 0)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2( 0,-1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2( 0,+1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(-1,-1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(+1,-1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(-1,+1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(+1,+1)) * - 1.0;
#endif
  
#if LAPLACE_3x3_W12
  laplace += textureOffset(tex, posn, ivec2( 0, 0)) * +12.0;
  laplace += textureOffset(tex, posn, ivec2(-1, 0)) * - 2.0;
  laplace += textureOffset(tex, posn, ivec2(+1, 0)) * - 2.0;
  laplace += textureOffset(tex, posn, ivec2( 0,-1)) * - 2.0;
  laplace += textureOffset(tex, posn, ivec2( 0,+1)) * - 2.0;
  laplace += textureOffset(tex, posn, ivec2(-1,-1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(+1,-1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(-1,+1)) * - 1.0;
  laplace += textureOffset(tex, posn, ivec2(+1,+1)) * - 1.0;
#endif
 
  glFragColor = laplace * mad.x + mad.y;
}



