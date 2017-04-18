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

attribute vec4 vertex;
attribute vec4 color;
attribute vec3 normal;

varying vec4 vertColor;
varying vec4 vertPosition;
varying vec3 vertNormal;

void main() {
  vertColor = color;
  vertPosition = modelview * vertex;
  // vertNormal = normalize(normalMatrix * normal);
  vertNormal = normal;
  gl_Position = projection * modelview * vertex;
}
