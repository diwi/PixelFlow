// file: render.frag
// author: diewald

#version 150

out uvec2 fragColor;

uniform vec2 wh_rcp;
uniform sampler2D tex;

void main () {
  fragColor = uvec2(texture(tex, gl_FragCoord.xy * wh_rcp).rg * 65535.0);
}