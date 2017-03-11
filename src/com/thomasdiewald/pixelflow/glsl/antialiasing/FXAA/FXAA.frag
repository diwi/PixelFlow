#version 150


#ifdef GL_ARB_gpu_shader5
  #extension GL_ARB_gpu_shader5 : enable
#else
  #extension GL_EXT_gpu_shader4 : enable
  #define FXAA_FAST_PIXEL_OFFSET 0
#endif

#define FXAA_PC 1
#define FXAA_GLSL_130 1
#define FXAA_GREEN_AS_LUMA 0
// 10 to 15 - default medium dither (10=fastest, 15=highest quality)
// 20 to 29 - less dither, more expensive (20=fastest, 29=highest quality)
// 39       - no dither, very expensive 
#define FXAA_QUALITY__PRESET 39

#include "FXAA3_11.h"

uniform sampler2D	tex;
uniform vec2	    wh_rcp; // vec2(1/w, 1/h)
out     vec4      glFragColor;

void main( void ){

  // Amount of sub-pixel aliasing removal. Can effect sharpness.
  //   1.00 - upper limit (softer)
  //   0.75 - default amount of filtering
  //   0.50 - lower limit (sharper, less sub-pixel aliasing removal)
  //   0.25 - almost off
  //   0.00 - completely off
	FxaaFloat fxaaQualitySubpix = 0.75;

  // The minimum amount of local contrast required to apply algorithm.
  //   0.333 - too little (faster)
  //   0.250 - low quality
  //   0.166 - default
  //   0.125 - high quality 
  //   0.063 - overkill (slower)
	FxaaFloat fxaaQualityEdgeThreshold = 0.125;

  // Trims the algorithm from processing darks.
  //   0.0833 - upper limit (default, the start of visible unfiltered edges)
  //   0.0625 - high quality (faster)
  //   0.0312 - visible limit (slower)
  // Special notes when using FXAA_GREEN_AS_LUMA,
  //   Likely want to set this to zero.
  //   As colors that are mostly not-green
  //   will appear very dark in the green channel!
  //   Tune by looking at mostly non-green content,
  //   then start at zero and increase until aliasing is a problem.
	FxaaFloat fxaaQualityEdgeThresholdMin = 0.0;
  
	glFragColor = FxaaPixelShader(
      wh_rcp * gl_FragCoord.xy
    , vec4(0.0)
    , tex
    , tex
    , tex
    , wh_rcp
    , vec4(0.0)
    , vec4(0.0)
    , vec4(0.0)
    , fxaaQualitySubpix
    , fxaaQualityEdgeThreshold
    , fxaaQualityEdgeThresholdMin
    , 0.0
    , 0.0
    , 0.0
    , vec4(0.0)
  );
  
  glFragColor.a = 1;
}
