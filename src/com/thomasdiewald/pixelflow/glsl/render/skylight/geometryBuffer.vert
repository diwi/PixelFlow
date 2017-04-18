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

// uniform mat4 mat_shadow;   // app
// uniform vec3 dir_light;    // app

in vec4 vertex;
in vec4 color;
in vec3 normal;

// out vec4 vertColor;
// out vec4 vertPosition;
out vec3 vertNormal;
// out vec4 shadowCoord;
// out float kd;

void main() {
  // vertColor = color;
  // Get vertex position in model view space
  // vertPosition = modelview * vertex;
  // Get normal direction in model view space
  // vertNormal = normalize(normalMatrix * normal);
  vertNormal = normal;
  // Normal bias removes the shadow acne
  // shadowCoord = mat_shadow * (vertPosition + vec4(vertNormal, 0.0));
  // kd, diffuse shading, backfaces: kd < 0
  // kd = dot(dir_light, vertNormal);

  gl_Position = projection * modelview * vertex;
}
