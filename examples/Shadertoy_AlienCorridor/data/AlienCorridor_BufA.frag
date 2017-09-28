
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



// https://www.shadertoy.com/view/4slyRs

float EPSILON = 0.002;
vec2 twist = vec2(2.0,7.0);
float planesDistance = 0.3;
vec4 bumpMapParams1 = vec4(2.0,7.0,0.01,-0.01);
vec4 bumpMapParams2 = vec4(2.0,3.0,-0.01,0.01);
vec4 heightMapParams = vec4(3.0,1.0,0.0,0.01);
vec4 heightInfluence = vec4(-0.025,-0.05,0.8,1.8);
float fogDensity = 0.2;
float fogDistance = 0.1;
vec3 groundColor1 = vec3(0.2,0.3,0.3);
vec3 groundColor2 = vec3(0.4,0.8,0.4);
vec3 columnColors = vec3(0.9,0.3,0.3);
vec4 ambient = vec4(0.2,0.3,0.4,0.0);
vec3 lightColor = vec3(0.4,0.7,0.7);
vec4 fogColor = vec4(0.0,0.1,0.5,1.0);
vec3 rimColor = vec3(1.0,0.75,0.75);

float pi = 3.14159265359;

mat2 rot(float a) 
{
    vec2 s = sin(vec2(a, a + pi/2.0));
    return mat2(s.y,s.x,-s.x,s.y);
}

float smin( float a, float b, float k )
{
    float h = clamp( 0.5+0.5*(b-a)/k, 0.0, 1.0 );
    return mix( b, a, h ) - k*h*(1.0-h);
}

float sphere(vec3 pos, float radius, vec3 scale)
{
    return length(pos*scale)-radius;
}

float heightmap(vec2 uv)
{
    return heightMapParams.x*texture(iChannel0, (uv + iTime*heightMapParams.zw)*heightMapParams.y).x;
}

float bumpmap(vec2 uv)
{
    float b1 = bumpMapParams1.x*(1.0 - texture(iChannel0, (uv + iTime*bumpMapParams1.zw)*bumpMapParams1.y).x);
    float b2 = bumpMapParams2.x*(1.0-texture(iChannel0, (uv + iTime*bumpMapParams2.zw)*bumpMapParams2.x).x);
    return b1+b2;
}

float distfunc(vec3 pos)
{
    vec3 p2 = pos;
    p2.x += sin(p2.z*3.0 + p2.y*5.0)*0.15;
    p2.xy *= rot(floor(p2.z*2.0)*twist.y);
    pos.xy *= rot(pos.z*twist.x);
    
    float h = heightmap(pos.xz)*heightInfluence.x;
    
    vec3 columnsrep = vec3(0.75,1.0,0.5);
    vec3 reppos = (mod(p2 + vec3(iTime*0.01 + sin(pos.z*0.5),0.0,0.0),columnsrep)-0.5*columnsrep);
    
    float columnsScaleX = 1.0 + sin(p2.y*20.0*sin(p2.z) + iTime*5.0 + pos.z)*0.15;
    float columnsScaleY = (sin(iTime + pos.z*4.0)*0.5+0.5);
    
    float columns = sphere(vec3(reppos.x, pos.y+0.25, reppos.z), 0.035, vec3(columnsScaleX,columnsScaleY,columnsScaleX));
    float corridor = planesDistance - abs(pos.y) + h;
    float d = smin(corridor, columns, 0.25); 
           
    return d;
}

float rayMarch(vec3 rayDir, vec3 cameraOrigin)
{
    const int MAX_ITER = 50;
	const float MAX_DIST = 30.0;
    
    float totalDist = 0.0;
    float totalDist2 = 0.0;
	vec3 pos = cameraOrigin;
	float dist = EPSILON;
    vec3 col = vec3(0.0);
    float glow = 0.0;
    
    for(int j = 0; j < MAX_ITER; j++)
	{
		dist = distfunc(pos);
		totalDist = totalDist + dist;
		pos += dist*rayDir;
        
        if(dist < EPSILON || totalDist > MAX_DIST)
		{
			break;
		}
	}
    
    return totalDist  ;
}

//Taken from https://www.shadertoy.com/view/Xds3zN
mat3 setCamera( in vec3 ro, in vec3 ta, float cr )
{
	vec3 cw = normalize(ta-ro);
	vec3 cp = vec3(sin(cr), cos(cr),0.0);
	vec3 cu = normalize( cross(cw,cp) );
	vec3 cv = normalize( cross(cu,cw) );
    return mat3( cu, cv, cw );
}

vec3 calculateNormals(vec3 pos)
{
	vec2 eps = vec2(0.0, EPSILON*1.0);
	vec3 n = normalize(vec3(
	distfunc(pos + eps.yxx) - distfunc(pos - eps.yxx),
	distfunc(pos + eps.xyx) - distfunc(pos - eps.xyx),
	distfunc(pos + eps.xxy) - distfunc(pos - eps.xxy)));
    
	return n;
}

//Taken from https://www.shadertoy.com/view/XlXXWj
vec3 doBumpMap(vec2 uv, vec3 nor, float bumpfactor)
{
   
    const float eps = 0.001;
    float ref = bumpmap(uv); 
    
    vec3 grad = vec3(bumpmap(vec2(uv.x-eps, uv.y))-ref, 0.0, bumpmap(vec2(uv.x, uv.y-eps))-ref); 
             
    grad -= nor*dot(nor, grad);          
                      
    return normalize( nor + grad*bumpfactor );
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{ 
    vec3 cameraOrigin = vec3(0.0, 0.0, iTime*-0.1);
    vec3 cameraTarget = cameraOrigin + vec3(0.0, 0.0, 1.0);;
    
	vec2 screenPos = (fragCoord.xy/iResolution.xy)*2.0-1.0;
	screenPos.x *= iResolution.x/iResolution.y;
    
	mat3 cam = setCamera(cameraOrigin, cameraTarget, 0.0 );
    
    vec3 rayDir = cam* normalize( vec3(screenPos.xy,2.0) );
    rayDir.xy *= rot(iTime*0.1);
    float dist = rayMarch(rayDir, cameraOrigin);
   
    vec3 pos = cameraOrigin + dist*rayDir;
    vec2 uv = pos.xy * rot(pos.z*twist.x);
    float h = heightmap(vec2(uv.x, pos.z));
    vec3 n = calculateNormals(pos);
    vec3 bump = doBumpMap(vec2(uv.x, pos.z), n, 3.0);
    float m = smoothstep(-0.15,0.2, planesDistance - abs(uv.y) + h*heightInfluence.y + sin(iTime)*0.05);
    vec3 color = mix(mix(groundColor1, groundColor2, smoothstep(heightInfluence.z,heightInfluence.w,h)), columnColors, m);
    float fog = dist*fogDensity-fogDistance;
    float heightfog = pos.y;
    float rim = (1.0-max(0.0, dot(-normalize(rayDir), bump)));
    vec3 lightPos = pos - (cameraOrigin + vec3(0.0,0.0,1.0));
    vec3 lightDir = -normalize(lightPos);
    float lightdist = length(lightPos);
    float atten = 1.0 / (1.0 + lightdist*lightdist*3.0);
    float light = max(0.0, dot(lightDir, bump));
   	vec3 r = reflect(normalize(rayDir), bump);
    float spec = clamp (dot (r, lightDir),0.0,1.0);
    float specpow = pow(spec,20.0);
    vec3 c = color*(ambient.xyz + mix(rim*rim*rim, rim*0.35+0.65, m)*rimColor + lightColor*(light*atten*2.0 + specpow*1.5));
    vec4 res = mix(vec4(c, rim), fogColor, clamp(fog+heightfog,0.0,1.0));

    
	fragColor = res;
}
