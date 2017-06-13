// file: render2.frag
// author: diewald

#version 150

out vec4 fragColor;

uniform vec2 wh_rcp;
uniform sampler2D tex;


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
  vec2 val = decode(texture(tex, gl_FragCoord.xy * wh_rcp));
  fragColor = vec4(vec3(1.0 - val.g), 1);
}