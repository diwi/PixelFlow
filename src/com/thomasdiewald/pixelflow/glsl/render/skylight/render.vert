/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
#version 150
 
uniform mat4 projection;
uniform mat4 modelview;
uniform mat3 normalMatrix;

in vec4 vertex;
in vec4 color;
in vec3 normal;

out vec4 vertColor;
out vec4 vertPosition;
out vec3 vertNormal;

void main() {
  vertColor = color;
  vertPosition = modelview * vertex;
  // vertNormal = normalize(normalMatrix * normal);
  vertNormal = normal;
  gl_Position = projection * modelview * vertex;
}
