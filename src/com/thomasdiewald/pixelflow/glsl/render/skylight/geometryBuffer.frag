/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
// PERSPECTIVE projection matrix
//
//   A  .  I  .
//   .  F  J  .
//   .  .  K  O
//   .  .  L  .
//
// vertex' = projection * vertex
// vx' = A * vx + I * vz
// vy' = F * vy + J * z
// vz' =          K * vz + O
// vw' =          L * vz

// ORTHOGONAL projection matrix
//
//   A  .  .  M
//   .  F  .  N
//   .  .  K  O
//   .  .  .  P
//
// vertex' = projection * vertex
// vx' = A * vx + M * vw
// vy' = F * vy + N * vw
// vz' = K * vz + O * vw
// vw' =          P * vw
  
// TRANSFORMATION : p_eye -> p_clip -> p_ndc -> p_frag
//
// 1) CLIP coordinates: [-w,+w]
//    p_clip = projection * p_eye
//    
//    p_clip.x = A * p_eye.x + I * p_eye.z
//    p_clip.y = F * p_eye.y + J * p_eye.z
//    p_clip.z =               K * p_eye.z + O
//    p_clip.w =               L * p_eye.z
//
// 2) NDC coordinates: [-1,+1]
//    p_ndc.xyz = p_clip.xyz / p_clip.w ...
//
// 3) SCREEN coordinates: [-1,+1]
//    p_frag.xyz  = (p_ndc.xyz * 0.5 + 0.5) * vec3(w, h, 1);
//    p_frag.w    = 1.0 / p_clip.w


// TRANSFORMATION : p_eye <- p_ndc <- p_frag
// 1) NDC coordinates: [-1,+1]
//    p_ndc.xyz = (gl_FragCoord.xyz / vec3(w, h, 1)) * 2.0 - 1.0;
//
// 2) EYE coordinates 
//    p_eye.z = L / gl_FragCoord.w
//    p_eye.x = p_eye.z * (L * p_ndc.x - I) / A
//    p_eye.y = p_eye.z * (L * p_ndc.y - J) / F
//  





#version 150

// in vec4 vertPosition;
in vec3 vertNormal;

out vec4 glFragColor;

// uniform mat4 projection;
// uniform mat4 modelview;
uniform mat3 normalMatrix;


vec2 endcodeNormal2F(in vec3 n3){
  return n3.xy / sqrt(n3.z*8.0f+8.0f) + 0.5f; // vec3[0,1] -> vec2[0,1]
}

vec3 decodeNormal3f(in vec2 n2){
  n2 = n2*4.0-2.0;
  float f = dot(n2, n2);
  float g = sqrt(1.0-f*0.25f);
  return vec3( n2*g, 1.0-f*0.50f );
}

void main(void) {
  // transform vertex normal to eye-space
  vec3 vert_normal = normalize(normalMatrix * vertNormal);
  
  if(!gl_FrontFacing) vert_normal = -vert_normal;
  
  // vec3 vert_normal = normalize(vertNormal);
  // glFragColor = vec4(vert_normal, gl_FragCoord.z);
  glFragColor = vec4(vert_normal, 1.0 / gl_FragCoord.w);
  // glFragColor = vec4(endcodeNormal2F(vert_normal), 1.0, 1.0 / gl_FragCoord.w);
  
  
  // glFragColor = vec4(1.0 / gl_FragCoord.w);
  // glFragColor.a = 1;
}

