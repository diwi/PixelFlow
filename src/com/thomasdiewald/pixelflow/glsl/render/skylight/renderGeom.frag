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

uniform vec2 wh;
uniform sampler2D tex_geombuffer;
uniform sampler2D tex_sky;
uniform sampler2D tex_sun;

uniform float mult_sun = 1.0;
uniform float mult_sky = 1.0;
uniform float gamma = 2.2;

void main(void) {

  vec2 fragcoordn = gl_FragCoord.xy / wh;
  vec4 geom = texture(tex_geombuffer, fragcoordn);
  
  // reconstruct vertex position (p_eye) from depth (p_eye.z)
  vec2 AF = vec2(mat_projection[0].x, mat_projection[1].y);
  vec2 IJ = vec2(mat_projection[2].xy);
  float L = mat_projection[2].w;
  
  // p_eye (vertex position in eye-space)
  float eye_z = L * geom.w;
  vec2 p_ndc = fragcoordn * 2.0 - 1.0;
  vec4 p_eye = vec4((L * p_ndc - IJ) / AF, 1, 1); p_eye.xyz *= eye_z;
  
  // angle/orientation/diffuse shading
  // vec3 p_eye_normal = geom.xyz;
  // float kd_eye = dot(normalize(p_eye_normal), normalize(-p_eye.xyz));
  // float kd_sky = dot(p_eye_normal, dir_light);

  
  vec3 shading = vec3(1);

  float shading_SKY = texture(tex_sky, fragcoordn).r * mult_sky;
  float shading_SUN = texture(tex_sun, fragcoordn).r * mult_sun;

  shading *= (shading_SUN + shading_SKY);

  // apply some gamma correction
  shading = pow(shading, vec3(1.0/gamma));
  glFragColor = vec4(shading, 1);
  
  // glFragColor = vec4(0,0,0, 1);
  
  // if(kd_eye < 0 ){
    // glFragColor = vec4(1,0,0, 1);
  // }
}

