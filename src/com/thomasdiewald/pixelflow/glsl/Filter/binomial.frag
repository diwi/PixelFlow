/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
 
// binomial filter coefficients: N!/(N!(N-k)!)
//
// Pascall Triangle:
//    
//  [ 0]        1 |      1
//  [ 1]        2 |      1     1
//  [ 2]        4 |      1     2     1
//  [ 3]        8 |      1     3     3     1
//  [ 4]       16 |      1     4     6     4     1
//  [ 5]       32 |      1     5    10    10     5     1
//  [ 6]       64 |      1     6    15    20    15     6     1
//  [ 7]      128 |      1     7    21    35    35    21     7     1
//  [ 8]      256 |      1     8    28    56    70    56    28     8     1
//  [ 9]      512 |      1     9    36    84   126   126    84    36     9     1
//  [10]     1024 |      1    10    45   120   210   252   210   120    45    10     1
//  [11]     2048 |      1    11    55   165   330   462   462   330   165    55    11     1
//  [12]     4096 |      1    12    66   220   495   792   924   792   495   220    66    12     1
//  [13]     8192 |      1    13    78   286   715  1287  1716  1716  1287   715   286    78    13     1
//  [14]    16384 |      1    14    91   364  1001  2002  3003  3432  3003  2002  1001   364    91    14     1
//


#version 150

#define HORZ 0
#define VERT 0

#define BINOMIAL_3x3 0    // radius 1
#define BINOMIAL_5x5 0    // radius 2
#define BINOMIAL_7x7 0    // radius 3
#define BINOMIAL_9x9 0    // radius 4
#define BINOMIAL_11x11 0  // radius 5
#define BINOMIAL_13x13 0  // radius 6
#define BINOMIAL_15x15 0  // radius 7

out vec4 glFragColor;

uniform sampler2D	tex;
uniform vec2	    wh_rcp;

void main(){

  ivec2 pos = ivec2(gl_FragCoord.xy);
  vec4 blur = vec4(0.0);
  
  
#if HORZ 
  // #define getData(offset, weight) texelFetchOffset(tex, pos, 0, ivec2(offset, 0)) * weight
  #define getData(offset, weight) texture(tex, (gl_FragCoord.xy + vec2(offset, 0)) * wh_rcp) * weight
#endif
#if VERT 
  // #define getData(offset, weight) texelFetchOffset(tex, pos, 0, ivec2(0, offset)) * weight
  #define getData(offset, weight) texture(tex, (gl_FragCoord.xy + vec2(0, offset)) * wh_rcp) * weight
#endif
  
  
#if BINOMIAL_3x3
  blur += getData( 0,  2.0);
  blur += getData(+1,  1.0);
  blur += getData(-1,  1.0);
  blur /= 4.0;
#endif

#if BINOMIAL_5x5
  blur += getData( 0,  6.0);
  blur += getData(+1,  4.0);
  blur += getData(-1,  4.0);
  blur += getData(+2,  1.0);
  blur += getData(-2,  1.0);
  blur /= 16.0;
#endif

#if BINOMIAL_7x7
  blur += getData( 0, 20.0);
  blur += getData(+1, 15.0);
  blur += getData(-1, 15.0);
  blur += getData(+2,  6.0);
  blur += getData(-2,  6.0);
  blur += getData(+3,  1.0);
  blur += getData(-3,  1.0);
  blur /= 64.0;
#endif

#if BINOMIAL_9x9
  blur += getData( 0, 70.0);
  blur += getData(+1, 56.0);
  blur += getData(-1, 56.0);
  blur += getData(+2, 28.0);
  blur += getData(-2, 28.0);
  blur += getData(+3,  8.0);
  blur += getData(-3,  8.0);
  blur += getData(+4,  1.0);
  blur += getData(-4,  1.0);
  blur /= 256.0;
#endif

#if BINOMIAL_11x11
  blur += getData( 0, 252.0);
  blur += getData(+1, 210.0);
  blur += getData(-1, 210.0);
  blur += getData(+2, 120.0);
  blur += getData(-2, 120.0);
  blur += getData(+3,  45.0);
  blur += getData(-3,  45.0);
  blur += getData(+4,  10.0);
  blur += getData(-4,  10.0);
  blur += getData(+5,   1.0);
  blur += getData(-5,   1.0);
  blur /= 1024.0;
#endif

#if BINOMIAL_13x13
  blur += getData( 0, 924.0);
  blur += getData(+1, 792.0);
  blur += getData(-1, 792.0);
  blur += getData(+2, 495.0);
  blur += getData(-2, 495.0);
  blur += getData(+3, 220.0);
  blur += getData(-3, 220.0);
  blur += getData(+4,  66.0);
  blur += getData(-4,  66.0);
  blur += getData(+5,  12.0);
  blur += getData(-5,  12.0);
  blur += getData(+6,   1.0);
  blur += getData(-6,   1.0);
  blur /= 4096.0;
#endif

#if BINOMIAL_15x15
  blur += getData( 0, 3432.0);
  blur += getData(+1, 3003.0);
  blur += getData(-1, 3003.0);
  blur += getData(+2, 2002.0);
  blur += getData(-2, 2002.0);
  blur += getData(+3, 1001.0);
  blur += getData(-3, 1001.0);
  blur += getData(+4,  364.0);
  blur += getData(-4,  364.0);
  blur += getData(+5,   91.0);
  blur += getData(-5,   91.0);
  blur += getData(+6,   14.0);
  blur += getData(-6,   14.0);
  blur += getData(+7,    1.0);
  blur += getData(-7,    1.0);
  blur /= 16384.0;
#endif

  glFragColor = blur;
}


