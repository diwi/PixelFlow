
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



// https://www.shadertoy.com/view/MsdGzl


// Created by inigo quilez - iq/2016
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0

// Pathtrace the scene. One path per pixel. Samples the sun light and the
// sky dome light at each vertex of the path.

// More info here: http://iquilezles.org/www/articles/simplepathtracing/simplepathtracing.htm


//------------------------------------------------------------------

float hash(float seed)
{
    return fract(sin(seed)*43758.5453 );
}

vec3 cosineDirection( in float seed, in vec3 nor)
{
    float u = hash( 78.233 + seed);
    float v = hash( 10.873 + seed);

    
    // Method 1 and 2 first generate a frame of reference to use with an arbitrary
    // distribution, cosine in this case. Method 3 (invented by fizzer) specializes 
    // the whole math to the cosine distribution and simplfies the result to a more 
    // compact version that does not depend on a full frame of reference.

    #if 0
        // method 1 by http://orbit.dtu.dk/fedora/objects/orbit:113874/datastreams/file_75b66578-222e-4c7d-abdf-f7e255100209/content
        vec3 tc = vec3( 1.0+nor.z-nor.xy*nor.xy, -nor.x*nor.y)/(1.0+nor.z);
        vec3 uu = vec3( tc.x, tc.z, -nor.x );
        vec3 vv = vec3( tc.z, tc.y, -nor.y );

        float a = 6.2831853 * v;
        return sqrt(u)*(cos(a)*uu + sin(a)*vv) + sqrt(1.0-u)*nor;
    #endif
	#if 0
    	// method 2 by pixar:  http://jcgt.org/published/0006/01/01/paper.pdf
    	float ks = (nor.z>=0.0)?1.0:-1.0;     //do not use sign(nor.z), it can produce 0.0
        float ka = 1.0 / (1.0 + abs(nor.z));
        float kb = -ks * nor.x * nor.y * ka;
        vec3 uu = vec3(1.0 - nor.x * nor.x * ka, ks*kb, -ks*nor.x);
        vec3 vv = vec3(kb, ks - nor.y * nor.y * ka * ks, -nor.y);
    
        float a = 6.2831853 * v;
        return sqrt(u)*(cos(a)*uu + sin(a)*vv) + sqrt(1.0-u)*nor;
    #endif
    #if 1
    	// method 3 by fizzer: http://www.amietia.com/lambertnotangent.html
        float a = 6.2831853 * v;
        u = 2.0*u - 1.0;
        return normalize( nor + vec3(sqrt(1.0-u*u) * vec2(cos(a), sin(a)), u) );
    #endif
}

//------------------------------------------------------------------

float maxcomp(in vec3 p ) { return max(p.x,max(p.y,p.z));}

float sdBox( vec3 p, vec3 b )
{
  vec3  di = abs(p) - b;
  float mc = maxcomp(di);
  return min(mc,length(max(di,0.0)));
}

float map( vec3 p )
{
    vec3 w = p;
    vec3 q = p;

    q.xz = mod( q.xz+1.0, 2.0 ) -1.0;
    
    float d = sdBox(q,vec3(1.0));
    float s = 1.0;
    for( int m=0; m<6; m++ )
    {
        float h = float(m)/6.0;

        p =  q - 0.5*sin( abs(p.y) + float(m)*3.0+vec3(0.0,3.0,1.0));

        vec3 a = mod( p*s, 2.0 )-1.0;
        s *= 3.0;
        vec3 r = abs(1.0 - 3.0*abs(a));

        float da = max(r.x,r.y);
        float db = max(r.y,r.z);
        float dc = max(r.z,r.x);
        float c = (min(da,min(db,dc))-1.0)/s;

        d = max( c, d );
   }

    
   float d1 = length(w-vec3(0.22,0.35,0.4)) - 0.09;
   d = min( d, d1 );

   float d2 = w.y + 0.22;
   d =  min( d,d2);

    
   return d;
}

//------------------------------------------------------------------

vec3 calcNormal( in vec3 pos )
{
    vec3 eps = vec3(0.0001,0.0,0.0);

    return normalize( vec3(
      map( pos+eps.xyy ) - map( pos-eps.xyy ),
      map( pos+eps.yxy ) - map( pos-eps.yxy ),
      map( pos+eps.yyx ) - map( pos-eps.yyx ) ) );
}


float intersect( in vec3 ro, in vec3 rd )
{
    float res = -1.0;
    float tmax = 16.0;
    float t = 0.01;
    for(int i=0; i<128; i++ )
    {
        float h = map(ro+rd*t);
        if( h<0.0001 || t>tmax ) break;
        t +=  h;
    }
    
    if( t<tmax ) res = t;

    return res;
}

float shadow( in vec3 ro, in vec3 rd )
{
    float res = 0.0;
    
    float tmax = 12.0;
    
    float t = 0.001;
    for(int i=0; i<80; i++ )
    {
        float h = map(ro+rd*t);
        if( h<0.0001 || t>tmax) break;
        t += h;
    }

    if( t>tmax ) res = 1.0;
    
    return res;
}


vec3 sunDir = normalize(vec3(-0.3,1.3,0.1));
vec3 sunCol =  6.0*vec3(1.0,0.8,0.6);
vec3 skyCol =  4.0*vec3(0.2,0.35,0.5);


vec3 calculateColor(vec3 ro, vec3 rd, float sa )
{
    const float epsilon = 0.0001;

    vec3 colorMask = vec3(1.0);
    vec3 accumulatedColor = vec3(0.0);

    float fdis = 0.0;
    for( int bounce = 0; bounce<3; bounce++ ) // bounces of GI
    {
        //rd = normalize(rd);
       
        //-----------------------
        // trace
        //-----------------------
        float t = intersect( ro, rd );
        if( t < 0.0 )
        {
            if( bounce==0 ) return mix( 0.05*vec3(0.9,1.0,1.0), skyCol, smoothstep(0.1,0.25,rd.y) );
            break;
        }

        if( bounce==0 ) fdis = t;

        vec3 pos = ro + rd * t;
        vec3 nor = calcNormal( pos );
        vec3 surfaceColor = vec3(0.4)*vec3(1.2,1.1,1.0);

        //-----------------------
        // add direct lighitng
        //-----------------------
        colorMask *= surfaceColor;

        vec3 iColor = vec3(0.0);

        // light 1        
        float sunDif =  max(0.0, dot(sunDir, nor));
        float sunSha = 1.0; if( sunDif > 0.00001 ) sunSha = shadow( pos + nor*epsilon, sunDir);
        iColor += sunCol * sunDif * sunSha;
        // todo - add back direct specular

        // light 2
        vec3 skyPoint = cosineDirection( sa + 7.1*float(iFrame) + 5681.123 + float(bounce)*92.13, nor);
        float skySha = shadow( pos + nor*epsilon, skyPoint);
        iColor += skyCol * skySha;


        accumulatedColor += colorMask * iColor;

        //-----------------------
        // calculate new ray
        //-----------------------
        //float isDif = 0.8;
        //if( hash(sa + 1.123 + 7.7*float(bounce)) < isDif )
        {
           rd = cosineDirection(76.2 + 73.1*float(bounce) + sa + 17.7*float(iFrame), nor);
        }
        //else
        {
        //    float glossiness = 0.2;
        //    rd = normalize(reflect(rd, nor)) + uniformVector(sa + 111.123 + 65.2*float(bounce)) * glossiness;
        }

        ro = pos;
   }

   float ff = exp(-0.01*fdis*fdis);
   accumulatedColor *= ff; 
   accumulatedColor += (1.0-ff)*0.05*vec3(0.9,1.0,1.0);

   return accumulatedColor;
}

mat3 setCamera( in vec3 ro, in vec3 rt, in float cr )
{
	vec3 cw = normalize(rt-ro);
	vec3 cp = vec3(sin(cr), cos(cr),0.0);
	vec3 cu = normalize( cross(cw,cp) );
	vec3 cv = normalize( cross(cu,cw) );
    return mat3( cu, cv, -cw );
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    float sa = hash( dot( fragCoord, vec2(12.9898, 78.233) ) + 1113.1*float(iFrame) );
    
    vec2 of = -0.5 + vec2( hash(sa+13.271), hash(sa+63.216) );
    vec2 p = (-iResolution.xy + 2.0*(fragCoord+of)) / iResolution.y;

    vec3 ro = vec3(0.0,0.0,0.0);
    vec3 ta = vec3(1.5,0.7,1.5);

    mat3  ca = setCamera( ro, ta, 0.0 );
    vec3  rd = normalize( ca * vec3(p,-1.3) );

    vec3 col = texture( iChannel0, fragCoord/iResolution.xy ).xyz;
    if( iFrame==0 ) col = vec3(0.0);
    
    col += calculateColor( ro, rd, sa );

    fragColor = vec4( col, 1.0 );
}
