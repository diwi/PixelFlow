/**
 * 
 * PixelFlow | Copyright (C) 2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */
 
 
#version 150


out vec4 glFragColor;


uniform mat4 projection;
uniform mat4 modelview;
uniform mat3 normalMatrix;

uniform mat4 mat_projection;
// uniform mat4 mat_screen_to_eye; // screen-space (view) -> eye-space
// uniform mat4 mat_shadow_modelview;
uniform mat4 mat_shadow;        // eye-space -> screen-space (shadowmap)
// uniform mat3 mat_shadow_normal;
uniform mat3 mat_shadow_normal_modelview;
uniform mat3 mat_shadow_normal_projection;
uniform vec3 dir_light;




uniform float shadow_bias_mag = 0;
uniform vec2 wh;
uniform vec2 wh_shadow;
uniform sampler2D tex_shadow;
uniform sampler2D tex_src;
uniform float pass_mix;
// uniform int singlesided = 1;

uniform sampler2D tex_geombuffer;

vec2 endcodeNormal2F(in vec3 n3){
  return n3.xy / sqrt(n3.z*8.0f+8.0f) + 0.5f; // vec3[0,1] -> vec2[0,1]
}

vec3 decodeNormal3f(in vec2 n2){
  n2 = n2*4.0-2.0;
  float f = dot(n2, n2);
  float g = sqrt(1.0-f*0.25f);
  return vec3( n2*g, 1.0-f*0.50f );
}

float getShadow(vec4 p_frag_shadow){
  //return step(p_frag_shadow.z, texture(tex_shadow, p_frag_shadow.xy).r);

  // if(p_frag_shadow.x < 0 || p_frag_shadow.x > 1) return 0.0;
  // if(p_frag_shadow.y < 0 || p_frag_shadow.y > 1) return 0.0;
  if(p_frag_shadow.z < 0 || p_frag_shadow.z > 2) return 0.0;
  return step(p_frag_shadow.z, texelFetch(tex_shadow, ivec2(p_frag_shadow.xy*wh_shadow), 0).r);
}


void main(void) {

  vec2 fragcoordn = (gl_FragCoord.xy) / wh;
  vec4 geom = texelFetch(tex_geombuffer, ivec2(gl_FragCoord.xy), 0);
  
  // transform vertex normal to eye-space
  // vec3 vert_normal = normalize(normalMatrix * vertNormal);
  // vec3 vert_screen = gl_FragCoord.xyz;

  // vec3 vert_screen = vec3(gl_FragCoord.xy, geom.w);
  // vec3 p_eye_normal = geom.xyz;
  // vec3 vert_normal = decodeNormal3f(geom.xy);

  // transform fragcoord from camera-screen-space to eye-space to shadowmap-screen-space
  // vec4 screen = vec4(vert_screen, 1);
  // vec4 eye    = mat_screen_to_eye * screen; eye.xyz /= eye.w; eye.w = 1;
  // vec4 p_frag_shadow = mat_shadow * (eye + vec4(vert_normal, 0.0)); p_frag_shadow.xyz /= p_frag_shadow.w;
  
  
  // reconstruct vertex position (p_eye) from depth (p_eye.z)
  vec2 AF = vec2(mat_projection[0].x, mat_projection[1].y);
  vec2 IJ = vec2(mat_projection[2].xy);
  float L = mat_projection[2].w;
  
  // p_eye (vertex position in eye-space)
  float eye_z = L * geom.w;
  vec2 p_ndc = fragcoordn * 2.0 - 1.0;
  vec4 p_eye = vec4((L * p_ndc - IJ) / AF, 1, 1); p_eye.xyz *= eye_z;
  
  // vertex normal
  vec3 p_eye_normal = geom.xyz;
  // switch normal direction if needed for single sided surfaces
  // if(singlesided == 1){
    // p_eye_normal *= sign(dot(p_eye_normal, -p_eye.xyz));
  // }
  

  // normal bias
  // shadowmap eye space has bounds of the unitsphere.
  // so the normal bias can be of constant length, only affected by shadowmap resolution
  vec3 p_normal_bias = p_eye_normal;
  p_normal_bias = mat_shadow_normal_modelview  * p_normal_bias;  // to eyespace
  p_normal_bias = normalize(p_normal_bias) * shadow_bias_mag;          // constant length
  p_normal_bias = mat_shadow_normal_projection * p_normal_bias;  // to screenspace
  
  // vertex position in shadow map screenspace + bias
  vec4 p_frag_shadow = (mat_shadow * p_eye) + vec4(p_normal_bias, 0);
  p_frag_shadow.xyz /= p_frag_shadow.w;
  
  // diffuse shading
  float kd_sky = dot(p_eye_normal, dir_light);
 
  float shading_cur = 0.0;
  if(kd_sky > 0.0){
    shading_cur = getShadow(p_frag_shadow) * kd_sky;
  }
  
  // average
  float shading_old = texelFetch(tex_src, ivec2(gl_FragCoord.xy), 0).r;
  float shading_new = mix(shading_cur, shading_old, pass_mix);
  glFragColor = vec4(shading_new);
}

