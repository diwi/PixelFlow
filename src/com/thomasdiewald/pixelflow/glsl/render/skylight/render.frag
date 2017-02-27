/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
 #version 150

varying vec4 vertColor;
varying vec4 vertPosition;
varying vec3 vertNormal;


uniform mat4 projection;
uniform mat4 modelview;
uniform mat3 normalMatrix;

uniform vec2 wh;

uniform sampler2D tex_sky;
uniform sampler2D tex_sun;

uniform float mult_sun = 1.0;
uniform float mult_sky = 1.0;
uniform float gamma = 2.2;

void main(void) {

  vec3 shading = vertColor.rgb;

  vec2 fragCoordn = (gl_FragCoord.xy)/wh;
  float shading_SKY = texture2D(tex_sky, fragCoordn).r * mult_sky;
  float shading_SUN = texture2D(tex_sun, fragCoordn).r * mult_sun;

  shading *= (shading_SUN + shading_SKY);

  // apply some gamma correction
  shading = pow(shading, vec3(1.0/gamma));
  
  gl_FragColor = vec4(shading, 1);
  
  // vec3 normal = normalize(normalMatrix * vertNormal);


  
  
  
  // mat4 inv_projection = inverse(projection);
  // vec4 ndc = vec4(gl_FragCoord.xy / wh, gl_FragCoord.z, 1);
  // ndc.xyz = ndc.xyz * 2 - 1;
  
  // vec4 eye = inv_projection * ndc;
  // eye.xyz /= eye.w;
  
  // if( abs(eye.x - vertPosition.x) < 0.001){
    // gl_FragColor = vec4(1,0,0, 1);
  // }
  
  // {
    // vec4 p_clip = projection * vertPosition;
    // vec3 p_ndc  = p_clip.xyz / p_clip.w;
    // vec4 p_frag = vec4((p_ndc * 0.5 + 0.5) * vec3(wh, 1), 1.0 / p_clip.w);
    
    // if( abs(p_frag.w - gl_FragCoord.w) < 0.001){
      // gl_FragColor = vec4(1,1,0, 1);
    // }
  // }
  
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
  
  
  // float A = projection[0][0];
  // float F = projection[1][1];
  // float I = projection[2][0];
  // float J = projection[2][1];
  // float L = projection[2][3];
  
  // vec2 fragcoord = gl_FragCoord.xy;
  // vec2 p_ndc = (fragcoord / wh) * 2.0 - 1.0;
  // vec4 p_eye = vec4(0,0, L / gl_FragCoord.w, 1);
  // p_eye.xy = p_eye.z * (L * p_ndc - vec2(I, J)) / vec2(A, F);
  
  // if( length(p_eye-vertPosition) < 1){
    // gl_FragColor = vec4(abs(p_eye.xyz-vertPosition.xyz)*100, 1);
  // }
  
  // if( fract(gl_FragCoord).z <= 0.5){
    // gl_FragColor = vec4(1,0,0,1);
  // }
  
  // float kd_eye = dot(normal, normalize(-p_eye.xyz));
  // if( kd_eye <= 0.0){
    // gl_FragColor = vec4(1,0,0,1);
  // } else {
    // gl_FragColor.xyz *= 0.1;
  // }
  
}

