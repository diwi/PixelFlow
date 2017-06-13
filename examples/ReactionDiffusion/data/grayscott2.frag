// file: grayscott2.frag
// author: diewald

#version 150

out vec4 fragColor;

uniform vec2 wh_rcp;
uniform sampler2D tex;

uniform float dA   = 1.0;
uniform float dB   = 0.5;
uniform float feed = 0.055;
uniform float kill = 0.062;
uniform float dt   = 1.0;

vec2 decode(vec4 dataf){
  ivec4 datai = ivec4(dataf * 255.0);
  float rg = (datai.r << 8 | datai.g) / 65535.0;
  float ba = (datai.b << 8 | datai.a) / 65535.0;
  return vec2(rg, ba);
}

vec4 encode(vec2 dataf){
  ivec2 datai = ivec2(dataf * 65535.0);
  int r = (datai.r >> 8) & 0xFF;
  int g = (datai.r     ) & 0xFF;
  int b = (datai.g >> 8) & 0xFF;
  int a = (datai.g     ) & 0xFF;
  return vec4(r,g,b,a) / 255.0;
}

void main () {

  vec2 posn = gl_FragCoord.xy * wh_rcp;
  
  vec2 val = decode(texture(tex, posn));
  
  vec2 laplace = -val;
  laplace += decode(textureOffset(tex, posn, ivec2(-1, 0))) * + 0.20;
  laplace += decode(textureOffset(tex, posn, ivec2(+1, 0))) * + 0.20;
  laplace += decode(textureOffset(tex, posn, ivec2( 0,-1))) * + 0.20;
  laplace += decode(textureOffset(tex, posn, ivec2( 0,+1))) * + 0.20;
  laplace += decode(textureOffset(tex, posn, ivec2(-1,-1))) * + 0.05;
  laplace += decode(textureOffset(tex, posn, ivec2(+1,-1))) * + 0.05;
  laplace += decode(textureOffset(tex, posn, ivec2(-1,+1))) * + 0.05;
  laplace += decode(textureOffset(tex, posn, ivec2(+1,+1))) * + 0.05;
  
  float nA = val.r + (dA * laplace.r - val.r * val.g * val.g + (feed * (1.0 - val.r))) * dt;
  float nB = val.g + (dB * laplace.g + val.r * val.g * val.g - ((kill + feed) * val.g)) * dt;

  fragColor = encode(clamp(vec2(nA, nB), vec2(0), vec2(1)));
}