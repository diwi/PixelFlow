// file: render.frag
// author: diewald

#version 150

out vec4 fragColor;

uniform vec2 wh_rcp;
uniform sampler2D tex;

void main () {
  vec2 val = texture(tex, gl_FragCoord.xy * wh_rcp).rg;
  fragColor = vec4(vec3(1.0 - val.g), 1);
}