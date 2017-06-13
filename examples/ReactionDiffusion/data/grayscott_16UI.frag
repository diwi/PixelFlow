// file: grayscott.frag
// author: diewald

#version 150

out uvec2 fragColor;

uniform vec2 wh_rcp;
uniform usampler2D tex;

uniform float dA   = 1.0;
uniform float dB   = 0.5;
uniform float feed = 0.055;
uniform float kill = 0.062;
uniform float dt   = 1.0;

void main () {

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
  vec2 val = vec2(texture(tex, posn).rg) / 65535.0;
  
  vec2 laplace = -val;
  laplace += vec2(textureOffset(tex, posn, ivec2(-1, 0)).rg) / 65535.0 * 0.20;
  laplace += vec2(textureOffset(tex, posn, ivec2(+1, 0)).rg) / 65535.0 * 0.20;
  laplace += vec2(textureOffset(tex, posn, ivec2( 0,-1)).rg) / 65535.0 * 0.20;
  laplace += vec2(textureOffset(tex, posn, ivec2( 0,+1)).rg) / 65535.0 * 0.20;
  laplace += vec2(textureOffset(tex, posn, ivec2(-1,-1)).rg) / 65535.0 * 0.05;
  laplace += vec2(textureOffset(tex, posn, ivec2(+1,-1)).rg) / 65535.0 * 0.05;
  laplace += vec2(textureOffset(tex, posn, ivec2(-1,+1)).rg) / 65535.0 * 0.05;
  laplace += vec2(textureOffset(tex, posn, ivec2(+1,+1)).rg) / 65535.0 * 0.05;
  
  float nA = val.r + (dA * laplace.r - val.r * val.g * val.g + (feed * (1.0 - val.r))) * dt;
  float nB = val.g + (dB * laplace.g + val.r * val.g * val.g - ((kill + feed) * val.g)) * dt;
  
  fragColor = uvec2(clamp(vec2(nA, nB), vec2(0), vec2(1)) * 65535.0);
}