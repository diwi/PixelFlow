// file: render.frag
// author: diewald

#version 150

out vec4 fragColor;

uniform vec2 wh_rcp;
uniform usampler2D tex;

void main () {
  vec2 val = vec2(texture(tex, gl_FragCoord.xy * wh_rcp).rg) / 65535.0;
  fragColor = vec4(vec3(1.0 - val.g), 1);
}