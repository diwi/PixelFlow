/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
 
 
uniform mat4 projection;
uniform mat4 modelview;
// uniform mat3 normalMatrix;

// uniform mat4 mat_shadow;   // app
// uniform vec3 dir_light;    // app

attribute vec4 vertex;
// attribute vec4 color;
// attribute vec3 normal;

// varying vec4 vertColor;
// varying vec4 vertPosition;
// varying vec3 vertNormal;
// varying vec4 shadowCoord;
// varying float kd;

void main() {
  // vertColor = color;
  // Get vertex position in model view space
  // vertPosition = modelview * vertex;
  // Get normal direction in model view space
  // vertNormal = normalize(normalMatrix * normal);
  // vertNormal = normal;
  // Normal bias removes the shadow acne
  // shadowCoord = mat_shadow * (vertPosition + vec4(vertNormal, 0.0));
  // kd, diffuse shading, backfaces: kd < 0
  // kd = dot(dir_light, vertNormal);

  gl_Position = projection * modelview * vertex;
}
