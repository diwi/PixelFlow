/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */

#version 150

out vec4 out_frag;

uniform float e1 = 1.0;
uniform float e2 = 1.0;
uniform float mult = 1.0;
uniform vec2  wh_rcp;

void main(){
  vec2 posn = (gl_FragCoord.xy * wh_rcp) * 2.0 - 1.0;
  float falloff = min(1.0, length(posn));
  falloff  = pow(falloff, e1);
  falloff  = 1.0 - falloff;
  falloff  = pow(falloff, e2);
  falloff *= mult;
  // falloff  = clamp(falloff, 0, 1);
  out_frag = vec4(falloff);
}





