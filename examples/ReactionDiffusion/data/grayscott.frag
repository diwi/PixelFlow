// file: grayscott.frag
// author: diewald

#version 150

out vec2 fragColor;

uniform vec2 wh_rcp;
uniform sampler2D tex;

uniform float dA   = 1.0;
uniform float dB   = 0.5;
uniform float feed = 0.055;
uniform float kill = 0.062;
uniform float dt   = 1.0;

void main () {

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
  vec4 val = texture(tex, posn);
  
  vec4 laplace = -val;
  laplace += textureOffset(tex, posn, ivec2(-1, 0)) * + 0.20;
  laplace += textureOffset(tex, posn, ivec2(+1, 0)) * + 0.20;
  laplace += textureOffset(tex, posn, ivec2( 0,-1)) * + 0.20;
  laplace += textureOffset(tex, posn, ivec2( 0,+1)) * + 0.20;
  laplace += textureOffset(tex, posn, ivec2(-1,-1)) * + 0.05;
  laplace += textureOffset(tex, posn, ivec2(+1,-1)) * + 0.05;
  laplace += textureOffset(tex, posn, ivec2(-1,+1)) * + 0.05;
  laplace += textureOffset(tex, posn, ivec2(+1,+1)) * + 0.05;
  
  float nA = val.r + (dA * laplace.r - val.r * val.g * val.g + (feed * (1.0 - val.r))) * dt;
  float nB = val.g + (dB * laplace.g + val.r * val.g * val.g - ((kill + feed) * val.g)) * dt;
  
  fragColor = clamp(vec2(nA, nB), vec2(0), vec2(1));
}