
#version 150

#define SAMPLER0 sampler2D // sampler2D, sampler3D, samplerCube
#define SAMPLER1 sampler2D // sampler2D, sampler3D, samplerCube
#define SAMPLER2 sampler2D // sampler2D, sampler3D, samplerCube
#define SAMPLER3 sampler2D // sampler2D, sampler3D, samplerCube

uniform SAMPLER0 iChannel0; // image/buffer/sound    Sampler for input textures 0
uniform SAMPLER1 iChannel1; // image/buffer/sound    Sampler for input textures 1
uniform SAMPLER2 iChannel2; // image/buffer/sound    Sampler for input textures 2
uniform SAMPLER3 iChannel3; // image/buffer/sound    Sampler for input textures 3

uniform vec3  iResolution;           // image/buffer          The viewport resolution (z is pixel aspect ratio, usually 1.0)
uniform float iTime;                 // image/sound/buffer    Current time in seconds
uniform float iTimeDelta;            // image/buffer          Time it takes to render a frame, in seconds
uniform int   iFrame;                // image/buffer          Current frame
uniform float iFrameRate;            // image/buffer          Number of frames rendered per second
uniform vec4  iMouse;                // image/buffer          xy = current pixel coords (if LMB is down). zw = click pixel
uniform vec4  iDate;                 // image/buffer/sound    Year, month, day, time in seconds in .xyzw
uniform float iSampleRate;           // image/buffer/sound    The sound sample rate (typically 44100)
uniform float iChannelTime[4];       // image/buffer          Time for channel (if video or sound), in seconds
uniform vec3  iChannelResolution[4]; // image/buffer/sound    Input texture resolution for each channel



// https://www.shadertoy.com/view/4ttXzj


/**
 Just fooling around basicly. Some sort of bloodstream. 
*/


// http://iquilezles.org/www/articles/smin/smin.htm
float smin( float a, float b, float k )
{
    float h = clamp( 0.5+0.5*(b-a)/k, 0.0, 1.0 );
    return mix( b, a, h ) - k*h*(1.0-h);
}

float cells(vec2 uv){  // Trimmed down.
    uv = mix(sin(uv + vec2(1.57, 0)), sin(uv.yx*1.4 + vec2(1.57, 0)), .75);
    return uv.x*uv.y*.3 + .7;
}

/*
float cells(vec2 uv)
{
    float sx = cos(uv.x);
    float sy = sin(uv.y);
    sx = mix(sx, cos(uv.y * 1.4), .75);
    sy = mix(sy, sin(uv.x * 1.4), .75);
    return .3 * (sx * sy) + .7;
}
*/

const float BEAT = 4.0;
float fbm(vec2 uv)
{
    
    float f = 200.0;
    vec2 r = (vec2(.9, .45));    
    vec2 tmp;
    float T = 100.0 + iTime * 1.3;
    T += sin(iTime * BEAT) * .1;
    // layers of cells with some scaling and rotation applied.
    for (int i = 1; i < 8; ++i)
    {
        float fi = float(i);
        uv.y -= T * .5;
        uv.x -= T * .4;
        tmp = uv;
        
        uv.x = tmp.x * r.x - tmp.y * r.y; 
        uv.y = tmp.x * r.y + tmp.y * r.x; 
        float m = cells(uv);
        f = smin(f, m, .07);
    }
    return 1. - f;
}

vec3 g(vec2 uv)
{
    vec2 off = vec2(0.0, .03);
    float t = fbm(uv);
    float x = t - fbm(uv + off.yx);
    float y = t - fbm(uv + off);
    float s = .0025;
    vec3 xv = vec3(s, x, 0);
    vec3 yv = vec3(0, y, s);
    return normalize(cross(xv, -yv)).xzy;
}

vec3 ld = normalize(vec3(1.0, 2.0, 3.));

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
	vec2 uv = fragCoord.xy / iResolution.xy;
    uv -= vec2(0.5);  
    float a = iResolution.x / iResolution.y;
    uv.y /= a;
    vec2 ouv = uv;
    float B = sin(iTime * BEAT);
    uv = mix(uv, uv * sin(B), .035);
    vec2 _uv = uv * 25.;
    float f = fbm(_uv);
    
    // base color
    fragColor = vec4(f);
    fragColor.rgb *= vec3(1., .3 + B * .05, 0.1 + B * .05);
    
    vec3 v = normalize(vec3(uv, 1.));
    vec3 grad = g(_uv);
    
    // spec
    vec3 H = normalize(ld + v);
    float S = max(0., dot(grad, H));
    S = pow(S, 4.0) * .2;
    fragColor.rgb += S * vec3(.4, .7, .7);
    // rim
    float R = 1.0 - clamp(dot(grad, v), .0, 1.);
    fragColor.rgb = mix(fragColor.rgb, vec3(.8, .8, 1.), smoothstep(-.2, 2.9, R));
    // edges
    fragColor.rgb = mix(fragColor.rgb, vec3(0.), smoothstep(.45, .55, (max(abs(ouv.y * a), abs(ouv.x)))));
    
    // contrast
    fragColor = smoothstep(.0, 1., fragColor);
}










