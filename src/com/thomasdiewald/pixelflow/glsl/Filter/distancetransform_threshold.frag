/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

 
#version 150

out vec4 out_threshold;

uniform vec2 wh_rcp;
uniform vec4 colA = vec4(1.0);
uniform vec4 colB = vec4(0.0);
uniform float threshold = 0;
uniform isampler2D tex_dtnn;

void main(){
  ivec2 posi = ivec2(gl_FragCoord.xy);
   vec2 posn = gl_FragCoord.xy * wh_rcp;
  ivec2 dtnn = texture(tex_dtnn, posn).xy;
  float dist = length(vec2(dtnn - posi));
  float mixval = step(dist, threshold); // dist > threshold ? 0.0 : 1.0
  out_threshold = mix(colB, colA, mixval);
}



