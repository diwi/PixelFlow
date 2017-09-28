
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



// https://www.shadertoy.com/view/4tXcRl


// Simple Greeble - Split4 by Jerome Liard, August 2017
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
// https://www.shadertoy.com/view/4tXcRl

// Manual unroll of so called shape grammar split (for "man made" tech detail)
// applied to a rough procedural reconstruction of the first few seconds of the death star trench sequence.
// Mouse to look around (2 camera paths)

// I was initially working on split/subdivided tiles for something else,
// but as it started to look familiar I pushed in the fan art direction.
//
// The tiles use several hardcoded quad split variants (all resulting in 4 children),
// and we recurse down 1 level on a couple of them (max 10 leaves).
//
// I find the lack of motion blur disturbing, but time was spent on other futile things instead.

#if 1
#define TIME_OFFSET 0.0
#define CAM2_TIME_OFFSET 0.0
#else
#define TIME_OFFSET 54.0
#define CAM2_TIME_OFFSET 7.0
#endif
#define CAMERA_REPEAT
// tile eval count (x1 x2 x4 (max)... to deal with cell boundaries), the more evals the less artifacts but it also depend on a ton of other things - use 1 or 2 really
#define NUM_TILE_EVALS 1
// warp first iteration with raytracing to reduce trace iteration count, affects lighting a bit
#define RAYTRACE_FIRST_MARCH_DISTANCE
// small trace optim that creates color disparity as a side effect (a bug promoted into a feature)
#define RAYMARCH_WITH_LOD
// allow proper shadows close to us, expensive and doesn't contribute that much... disable
//#define TRACED_SHADOW
// do the main cast shadow analytically
#define ANALYTICAL_SHADOW
// worry not, it's not screenspace
#define AO
// color tiles (debug)
//#define COLOR_TILES
#define STARFIELD
#define SUN
// scene type
//#define PRIMITIVES
#define SORT_OF_MOON
#define LASERS
// max marching iterations
#define MAX_ITERATIONS 140
// tweak for perfs vs quality etc
#define DBREAK 0.00125
#define TMAX 200.0
#define TFRAC 0.5

// threshold distance used by RAYMARCH_WITH_LOD and RAYTRACE_FIRST_MARCH_DISTANCE
#define ROUGH_SHELL_D 0.45

#define FLYING_SPEED 10.0
#define TRENCH_DEPTH 4.8
#define TRENCH_HALF_WIDTH 2.0

float PI = 3.141592654;

#ifndef FLT_MAX
#define FLT_MAX 1000000.0
#endif

vec3 RED	 = vec3( 1, 0, 0 );
vec3 GREEN	 = vec3( 0, 1, 0 );
vec3 BLACK	 = vec3( 0, 0, 0 );

vec2 xset( vec2 p, float v ) { return vec2( v, p.y );}
vec2 yset( vec2 p, float v ) { return vec2( p.x, v );}
vec3 xset( vec3 p, float v ) { return vec3( v, p.y, p.z );}
vec3 yset( vec3 p, float v ) { return vec3( p.x, v, p.z );}
vec3 zset( vec3 p, float v ) { return vec3( p.x, p.y, v );}

#define IMPL_SATURATE(type) type saturate( type x ) { return clamp( x, type(0.0), type(1.0) ); }
IMPL_SATURATE( float )
IMPL_SATURATE( vec2 )
IMPL_SATURATE( vec3 )
IMPL_SATURATE( vec4 )

float smoothstep_unchecked( float x ) { return ( x * x ) * ( 3.0 - x * 2.0 ); }
vec2 smoothstep_unchecked( vec2 x ) { return ( x * x ) * ( 3.0 - x * 2.0 ); }
vec3 smoothstep_unchecked( vec3 x ) { return ( x * x ) * ( 3.0 - x * 2.0 ); }
// cubicstep is a generic smoothstep where you can set in and out slopes
// slope at x=0 is df0
// slope at x=1 is df1
// smoothstep_unchecked(x) == cubicstep(x,0,0)
float cubicstep( float x, float df0, float df1 ) { float b = 3.0 - df1 - 2.0 * df0; float a = 1.0 - df0 - b; return ( ( a * x + b ) * x + df0 ) * x; }
// cubic polynomial
float cubic( vec4 c, float x ) { return ( ( c.x * x + c.y ) * x + c.z ) * x + c.w; }
float linearstep( float a, float b, float x ) { return saturate( ( x - a ) / ( b - a ) ); }
float smoothbump( float a, float r, float x ) { return 1.0 - smoothstep_unchecked( min( abs( x - a ), r ) / r ); }
vec2 perp( vec2 v ) { return vec2( -v.y, v.x ); }
float calc_angle( vec2 v ) { return atan( v.y, v.x ); }
float calc_angle( vec2 a, vec2 b ) { return calc_angle( vec2( dot( a, b ), dot( perp( a ), b ) ) ); }
vec3 contrast( vec3 x, vec3 s ) { return ( x - 0.5 ) * s + 0.5; }
float lensqr( vec2 v ) { return dot( v, v ); }
float lensqr( vec3 v ) { return dot( v, v ); }
float lensqr( vec4 v ) { return dot( v, v ); }
float pow2( float x ) { return x * x; }
float pow4( float x ) { x *= x; x *= x; return x; }
vec4 pow2( vec4 x ) { return x * x; }
vec4 pow4( vec4 x ) { x *= x; x *= x; return x; }
float pow5( float x ) { float x2 = x * x; return x2 * x2 * x; }
float maxcomp( float x ) { return x; }
float maxcomp( vec2 v ) { return max( v.x, v.y ); }
float maxcomp( vec3 v ) { return max( max( v.x, v.y ), v.z ); }
float mincomp( float x ) { return x; }
float mincomp( vec2 v ) { return min( v.x, v.y ); }
float mincomp( vec3 v ) { return min( min( v.x, v.y ), v.z ); }
float box( float x ) { return abs( x ) < 1.0 ? 1.0 : 0.0; }
vec3 chrominance( vec3 c ) { return c / max( c.r, max( c.g, c.b ) ); }
vec3 luminance( vec3 c ) { return vec3( dot( vec3( 0.2989, 0.5866, 0.1145 ), c ) ); }

// project this on line (O,d), d is assumed to be unit length for project_on_line1
// project_on_liney: d = 0,1,0
#define PROJECT_ON_LINE1(type) \
type project_on_liney( type P, type O ) { O.y += ( P - O ).y; return O; } /* d = vec3(0,1,0) */

PROJECT_ON_LINE1( vec2 )
PROJECT_ON_LINE1( vec3 )

#define DECL_BOUNDS( btype, type, booltype ) \
struct btype { type pmin; type pmax; }; \
/* min,max constructor*/ \
btype mkbounds( type amin, type amax ) { btype ret; ret.pmin = amin; ret.pmax = amax; return ret; } \
/*btype mkbounds_invalid() { btype ret; ret.pmin = FLT_MAX; ret.pmax = -FLT_MAX; return ret; }*/ \
type size( btype b ) { return b.pmax - b.pmin; } \
type center( btype b ) { return 0.5 * ( b.pmax + b.pmin ); } \
type closest( btype b, type p ) { return min( max( p, b.pmin ), b.pmax ); } \
bool inside( btype b, type p ) { return all_( /*booltype*/( p == closest( b, p ) ) ); }

bool all_( bool value ) { return value; } // not defined in glsl, apparently + can't use same name else some compilers barf
bool all_( bvec2 value ) { return all( value ); }
bool all_( bvec3 value ) { return all( value ); }

DECL_BOUNDS( bounds1, float, bool )
DECL_BOUNDS( bounds2, vec2, bvec2 )
DECL_BOUNDS( bounds3, vec3, bvec3 )

bounds3 mkbounds( bounds2 b, float height )
{
	bounds3 ret;
	ret.pmin = vec3( b.pmin.xy, 0 );
	ret.pmax = vec3( b.pmax.xy, height );
	return ret;
}
bounds2 xy( bounds3 b ) { return mkbounds( b.pmin.xy, b.pmax.xy  ); }
bounds2 xy( bounds2 b ) { return b; }

#define REPEAT_FUNCTIONS( type, btype ) \
type repeat( type x, type len ) { return len * fract( x * ( type( 1.0 ) / len ) ); }\
type repeat_mirror( type x, type len ) { return len * abs( type( -1.0 ) + 2.0 * fract( ( ( x * ( type( 1.0 ) / len ) ) - type( -1.0 ) ) * 0.5 ) ); }

REPEAT_FUNCTIONS( float, bounds1 )
REPEAT_FUNCTIONS( vec2, bounds2 )
REPEAT_FUNCTIONS( vec3, bounds3 )

#define TRI_FUNCTIONS(type,btype) \
/* y=1-x on 0,1, output 0,1 */ \
type tri0( type x ) { return abs( fract( x * 0.5 ) - type(0.5) ) * 2.0; } \
/* function returns 0 at x = half_width */ \
type tri_p( type x, type half_width, type half_period )	{ return half_width - repeat_mirror( x, half_period ); }

TRI_FUNCTIONS( float, bounds1 )
TRI_FUNCTIONS( vec2, bounds2 )
TRI_FUNCTIONS( vec3, bounds3 )

float spaced_tri( float x, float period, float half_width ) { period *= 0.5; x /= period; return max( 0.0, 1.0 + ( tri0( x ) - 1.0 ) * ( period / half_width ) ); }
float spaced_bumps( float x, float s, float r ) { return smoothstep_unchecked( spaced_tri( x, s, r ) ); }

// hash functions from David Hoskins's https://www.shadertoy.com/view/4djSRW

// *** Change these to suit your range of random numbers..

// *** Use this for integer stepped ranges, ie Value-Noise/Perlin noise functions.
#define HASHSCALE1 .1031
#define HASHSCALE3 vec3(.1031, .1030, .0973)
#define HASHSCALE4 vec4(1031, .1030, .0973, .1099)

// For smaller input rangers like audio tick or 0-1 UVs use these...
//#define HASHSCALE3 443.8975
//#define HASHSCALE3 vec3(443.897, 441.423, 437.195)
//#define HASHSCALE3 vec3(443.897, 441.423, 437.195, 444.129)

//----------------------------------------------------------------------------------------
//  2 out, 1 in...
vec2 hash21( float p )
{
	vec3 p3 = fract( vec3( p ) * HASHSCALE3 );
	p3 += dot( p3, p3.yzx  + 19.19 );
	return fract( ( p3.xx  + p3.yz  ) * p3.zy  );
}

//----------------------------------------------------------------------------------------
///  2 out, 2 in...
vec2 hash22( vec2 p )
{
	vec3 p3 = fract( vec3( p.xyx  ) * HASHSCALE3 );
	p3 += dot( p3, p3.yzx  + 19.19 );
	return fract( ( p3.xx  + p3.yz  ) * p3.zy  );
}

//----------------------------------------------------------------------------------------
///  3 out, 2 in...
vec3 hash32( vec2 p )
{
	vec3 p3 = fract( vec3( p.xyx ) * HASHSCALE3 );
	p3 += dot( p3, p3.yxz  + 19.19 );
	return fract( ( p3.xxy  + p3.yzz  ) * p3.zyx );
}

//----------------------------------------------------------------------------------------
// 4 out, 2 in...
vec4 hash42( vec2 p )
{
	vec4 p4 = fract( vec4( p.xyxy  ) * HASHSCALE4 );
	p4 += dot( p4, p4.wzxy  + 19.19 );
	return fract( ( p4.xxyz + p4.yzzw ) * p4.zywx );

}

vec2 V30 = vec2( 0.866025403, 0.5 );
vec2 V45 = vec2( 0.707106781, 0.707106781 );

vec2 unit_vector2( float angle ) { return vec2( cos( angle ), sin( angle ) ); }
vec2 rotate_with_unit_vector( vec2 p, vec2 cs ) { return vec2( cs.x * p.x - cs.y * p.y, cs.y * p.x + cs.x * p.y ); }
vec2 rotate_with_angle( vec2 p, float a_angle ) { return rotate_with_unit_vector( p, unit_vector2( a_angle ) ); }

// theta is angle with the z axis, range [0,pi].
// phi is angle with x vectors on z=0 plane, range [0,2pi].
vec3 zup_spherical_coords_to_vector( float theta, float phi )
{
	vec2 theta_vec = unit_vector2( theta );
	vec2 phi_vec = unit_vector2( phi );
	return vec3( theta_vec.y * phi_vec, theta_vec.x );
}

vec3 yup_spherical_coords_to_vector( float theta, float phi ) { return zup_spherical_coords_to_vector( theta, phi ).yzx; }

mat4 yup_spherical_offset( float theta, float phi )
{
	vec3 y = yup_spherical_coords_to_vector( theta, phi );
	vec3 z = yup_spherical_coords_to_vector( theta + PI * 0.5, phi );
	vec3 x = cross( y, z );
	return mat4( vec4( x, 0.0 ), vec4( y, 0.0 ), vec4( z, 0.0 ), vec4( 0, 0, 0, 1 ) );
}

// debug visualization
vec3 viridis_quintic( float x )
{
	x = saturate( x );
	vec4 x1 = vec4( 1.0, x, x * x, x * x * x ); // 1 x x2 x3
	vec4 x2 = x1 * x1.w * x; // x4 x5 x6 x7
	return vec3(
		dot( x1.xyzw, vec4( +0.280268003, -0.143510503, +2.225793877, -14.815088879 ) ) + dot( x2.xy, vec2( +25.212752309, -11.772589584 ) ),
		dot( x1.xyzw, vec4( -0.002117546, +1.617109353, -1.909305070, +2.701152864 ) ) + dot( x2.xy, vec2( -1.685288385, +0.178738871 ) ),
		dot( x1.xyzw, vec4( +0.300805501, +2.614650302, -12.019139090, +28.933559110 ) ) + dot( x2.xy, vec2( -33.491294770, +13.762053843 ) ) );
}

struct Ray { vec3 o; vec3 d; };

Ray get_view_ray( vec2 normalized_pos, float z, float aspect, float tan_half_fovy )
{
	Ray view_ray;
	view_ray.o = vec3( normalized_pos * vec2( aspect, 1.0 ) * tan_half_fovy, -1.0 ) * z;
	view_ray.d = normalize( view_ray.o );
	return view_ray;
}

vec2 get_screen_normalized_pos_from_view_pos( vec3 position, float aspect, float tan_half_fovy ) { return ( -position / position.z ).xy / ( vec2( aspect, 1.0 ) * tan_half_fovy ); }

mat4 lookat( vec3 eye, vec3 center, vec3 up )
{
	vec3 z = normalize( eye - center );
    vec3 x = normalize( cross( up, z ) );
	return mat4( vec4( x, 0.0 ), vec4( cross( z, x ), 0.0 ), vec4( z, 0.0 ), vec4( eye, 1.0 ) );
}

float opU( float d1, float d2 ) { return -max( -d1, -d2 ); }
float opS( float d1, float d2 ) { return max( -d2, d1 );}
float opI( float d1, float d2 ) { return max( d1, d2 ); }
float opI( float d1, float d2, float d3 ) { return max( max( d1, d2 ), d3 ); }

float sd_bounds_half_size( float p, float h ) { p = abs( p ) - h; return p; }
float sd_bounds_half_size( vec2 p, vec2 h ) { p = abs( p ) - h; return opI( p.x, p.y ); }
float sd_bounds_half_size( vec3 p, vec3 h ) { p = abs( p ) - h; return opI( p.x, p.y, p.z ); }
float sd_bounds_range( vec2 p, vec2 mi, vec2 ma ) { vec2 hmi = mi * 0.5; vec2 hma = ma * 0.5; return sd_bounds_half_size( p - ( hma + hmi ), hma - hmi ); }
float sd_bounds_range( float p, float mi, float ma ) { return sd_bounds_half_size( p - ( ( ma + mi ) * 0.5 ), ( ma - mi ) * 0.5 ); }
float sd_bounds_range( vec3 p, vec3 mi, vec3 ma ) { return sd_bounds_half_size( p - ( ( ma + mi ) * 0.5 ), ( ma - mi ) * 0.5 ); }
float sd_bounds( vec2 p, bounds2 b ) { return sd_bounds_range( p, b.pmin, b.pmax ); }
float sd_bounds( vec3 p, bounds3 b ) { return sd_bounds_range( p, b.pmin, b.pmax ); }

struct Plane2 { vec2 base; vec2 normal; };
struct Plane { vec3 base; vec3 normal; };

Plane2 mkplane2( vec2 base, vec2 normal ) { Plane2 plane; plane.base = base; plane.normal = normal; return plane; }
Plane mkplane( vec3 base, vec3 normal ) { Plane plane; plane.base = base; plane.normal = normal; return plane; }

// a few tediously hardcoded subdivisions that generates 4 children

#define SPLIT4_BOUNDS

struct Split4
{
#ifdef SPLIT4_BOUNDS
	bounds2 b00;
	bounds2 b01;
	bounds2 b10;
	bounds2 b11;
#endif
	vec4 d;
};

vec4 get_distances( vec2 p, Split4 split )
{
#if 1
	return split.d;
#else
// should be same result
	vec4 d;
	d.x = sd_bounds_range( p, split.b00.pmin, split.b00.pmax );
	d.y = sd_bounds_range( p, split.b01.pmin, split.b01.pmax );
	d.z = sd_bounds_range( p, split.b10.pmin, split.b10.pmax );
	d.w = sd_bounds_range( p, split.b11.pmin, split.b11.pmax );
	return d;
#endif
}

Split4 sd_Split_b_xxx( vec2 p, vec2 mi, vec2 ma, vec3 s )
{
//  -------------------------
//  |     |	    |     |     |
//  | b00 | b01 | b10 | b11 |
//  |     |	    |     |     |
//  -------------------------
//     x10     x0    x11

	float x0 = mix( mi.x, ma.x, s.x );
	float x10 = mix( mi.x, x0, s.y );
	float x11 = mix( x0, ma.x, s.z );

#if 0
// diagonal features
	float dx = linearstep( 0.4, 0.6, ( p.y - mi.y ) / ( ma.y - mi.y ) ) * ( ma.x - mi.x ) * 0.15;
	x0 += dx;
	x10 += dx;
	x11 += dx;
#endif

	Split4 split;
	//
#ifdef SPLIT4_BOUNDS
	split.b00 = mkbounds( mi, xset( ma, x10 ) );
	split.b01 = mkbounds( xset( mi, x10 ), xset( ma, x0 ) );
	split.b10 = mkbounds( xset( mi, x0 ), xset( ma, x11 ) );
	split.b11 = mkbounds( xset( mi, x11 ), ma );
#endif
	//
	float d = sd_bounds_range( p, mi, ma );
	float a = p.x - x0;
	float d0 = opI( a, d );
	float d1 = opI( -a, d );
	float b = p.x - x10;
	split.d.x = opI( d0, b );
	split.d.y = opI( d0, -b );
	float c = p.x - x11;
	split.d.z = opI( d1, c );
	split.d.w = opI( d1, -c );
	//
	return split;
}

Split4 sd_Split_b_xyy( vec2 p, vec2 mi, vec2 ma, vec3 s )
{
//     ---------------
//     | 	  |      |
//     | b01  |  b11 |
// y10 | -----|      |
//     | 	  |------| y11
//     | b00  |  b10 |
//     ---------------
//            x0

	float x0 = mix( mi.x, ma.x, s.x );
	float y10 = mix( mi.y, ma.y, s.y );
	float y11 = mix( mi.y, ma.y, s.z );

#if 1
// diagonal features
	float dx = linearstep( 0.4, 0.6, ( p.y - mi.y ) / ( ma.y - mi.y ) ) * ( ma.x - mi.x ) * 0.15;
	x0 += dx;
#endif

	Split4 split;
	//
#ifdef SPLIT4_BOUNDS
	split.b00 = mkbounds( mi, vec2( x0, y10 ) );
	split.b01 = mkbounds( yset( mi, y10 ), xset( ma, x0 ) );
	split.b10 = mkbounds( xset( mi, x0 ), yset( ma, y11 ) );
	split.b11 = mkbounds( vec2( x0, y11 ), ma );
#endif
	//
	float d = sd_bounds_range( p, mi, ma );
	float a = p.x - x0;
	float d0 = opI( a, d );
	float d1 = opI( -a, d );
	float b = p.y - y10;
	split.d.x = opI( d0, b );
	split.d.y = opI( d0, -b );
	float c = p.y - y11;
	split.d.z = opI( d1, c );
	split.d.w = opI( d1, -c );
	//
	return split;
}

Split4 sd_Split_b_xyx( vec2 p, vec2 mi, vec2 ma, vec3 s )
{
// 	   ------------------
//     | b01 |     |    |
// y10 | ----| b10 | b11|
//     | b00 |     |    |
// 	   ------------------
// 	        x0    x11

	float x0 = mix( mi.x, ma.x, s.x );

#if 0
// diagonal features (a bit too much)
	float dx = linearstep( 0.4, 0.6, ( p.y - mi.y ) / ( ma.y - mi.y ) ) * ( ma.x - mi.x ) * 0.15;
	x0 += dx;
#endif

	float y10 = mix( mi.y, ma.y, s.y );
	float x11 = mix( x0, ma.x, s.z );

#if 0
// diagonal feature (a bit too much)
	float dx = linearstep( 0.4, 0.6, ( p.y - mi.y ) / ( ma.y - mi.y ) ) * ( ma.x - mi.x ) * 0.15;
	x11 += dx;
#endif

	//
	Split4 split;
#ifdef SPLIT4_BOUNDS
	split.b00 = mkbounds( mi, vec2( x0, y10 ) );
	split.b01 = mkbounds( yset( mi, y10 ), xset( ma, x0 ) );
	split.b10 = mkbounds( xset( mi, x0 ), xset( ma, x11 ) );
	split.b11 = mkbounds( xset( mi, x11 ), ma );
#endif
	//
	float d = sd_bounds_range( p, mi, ma );
	float a = p.x - x0;
	float d0 = opI( a, d );
	float d1 = opI( -a, d );
	float b = p.y - y10;
	split.d.x = opI( d0, b );
	split.d.y = opI( d0, -b );
	float c = p.x - x11;
	split.d.z = opI( d1, c );
	split.d.w = opI( d1, -c );
	//
	return split;
}

// that one can't be expressed as a 2 levels split, but as an incomplete level 3 (we only store 4 bounds)
Split4 sd_Split_b_H( vec2 p, vec2 mi, vec2 ma, vec3 s )
{
//   --------------------
//   |     |  b10 |     |
//   |     |	  |     |
//   | b00 |------| b11 | y20
//   |     |	  |     |
//   |     |  b01 |     |
//   --------------------
//         x0    x10

	// note: we sort s.x and s.z, it make this function easier to use
	float x0 = mix( mi.x, ma.x, min( s.x, s.z ) );
	float y20 = mix( mi.y, ma.y, s.y );
	float x10 = mix( x0, ma.x, max( s.x, s.z ) );
	Split4 split;
	//
#ifdef SPLIT4_BOUNDS
	split.b00 = mkbounds( mi, xset( ma, x0 ) );
	split.b01 = mkbounds( xset( mi, x0 ), vec2( x10, y20 ) );
	split.b10 = mkbounds( vec2( x0, y20 ), xset( ma, x10 ) );
	split.b11 = mkbounds( xset( mi, x10 ), ma );
#endif
	//
	float d = sd_bounds_range( p, mi, ma );
	float d0 = opI( p.x - x0, d );
	float d1 = opI( -p.x + x0, d );
	float d20 = opI( p.x - x10, d1 );
	float d21 = opI( -p.x + x10, d1 );
	split.d.x = opI( d0, p.x - x0 );
	split.d.y = opI( d20, -p.y + y20 );
	split.d.z = opI( d20, p.y - y20 );
	split.d.w = opI( d21, -p.x + x10 );
	//
	return split;
}

struct TechTilesArgs
{
	vec4 height0, height10, height11;
	vec3 size0, size10, size11; //relative
	bool sub10, sub11; // recurse or not
};

// d stores 4 df for 4 boxes, h stored boxes height
float getDist4( float z, vec4 d, vec4 h )
{
	vec4 v = vec4( z ) - h;
	return opU( opU( opI( v.x, d.x ), opI( v.y, d.y ) ),
				opU( opI( v.z, d.z ), opI( v.w, d.w ) ) );
}

// 4 scopes output + recurse 2 of them
float sd_TechTilesTestsSub( vec3 p, int lod, float t, TechTilesArgs args, float e )
{
	vec4 d = vec4( FLT_MAX );
	vec4 heights = args.height0;

//	Split4 b = sd_Split_b_xxx( p.xy, vec2( 0, 0 ), vec2( 1, 1 ), args.size0 );
//	Split4 b = sd_Split_b_xyy( p.xy, vec2( 0, 0 ), vec2( 1, 1 ), args.size0 );
	Split4 b = sd_Split_b_xyx( p.xy, vec2( 0, 0 ), vec2( 1, 1 ), args.size0 );
//	Split4 b = sd_Split_b_H( p.xy,vec2( 0, 0 ), vec2( 1, 1 ), args.size0 );

	d = get_distances( p.xy, b ) + e;

#ifdef SPLIT4_BOUNDS
	if ( args.sub10 )
	{
		// do one more level
		Split4 b2 = sd_Split_b_xyy( p.xy, b.b01.pmin, b.b01.pmax, args.size10 );
		vec4 d2 = get_distances( p.xy, b2 ) + e;
		d.y = getDist4( p.z, d2, args.height10 );
		heights = max( heights, args.height10 );
	}
#endif

#ifdef SPLIT4_BOUNDS
	if ( args.sub11 )
	{
		// do one more level
		Split4 b2 = sd_Split_b_xxx( p.xy, b.b11.pmin, b.b11.pmax, args.size11 );
		vec4 d2 = get_distances( p.xy, b2 ) + e;
		d.w = getDist4( p.z, d2, args.height11 );
		heights = max( heights, args.height11 );
	}
#endif

	return getDist4( p.z, d, heights );
}

struct TechTilesArgs0
{
	float hmin;
	float hmax;
	float hdetail; // the height of sub detail
};

float sd_TechTilesTestsSub0( vec3 p, int lod, float t, Ray ray, vec2 index, TechTilesArgs0 targs )
{
	float d = FLT_MAX;

//	d = opI( p.z - 1.0, sd_bounds_range( p.xy, vec2( 0, 0 ), vec2( 1, 1 ) ) );
//	return d;

	float e0 = 0.0125 * 2.0;
	float e = e0 + t * 0.001; // else e becomes 0 as far as tracing is concerned... increases cost

	TechTilesArgs args;

	vec4 ha = hash42( index );
	vec4 hb = hash42( index + 100.0 );

	float rnd_type_and_rotation = ha.w;
	vec3 size0_hash = ha.xyz;
	vec4 height0_hash = hb;

	args.sub10 = rnd_type_and_rotation < 0.6;
	args.sub11 = rnd_type_and_rotation < 0.3;

	float rota = fract( rnd_type_and_rotation * 3.0 );
	if ( rota < 0.25 ) p.xy = p.yx;
	else if ( rota < 0.5 ) p.xy = vec2( 1.0 - p.y, p.x );

	float m1 = 0.15;
	args.size0 = m1 + ( 1.0 - m1 * 2.0 ) * size0_hash; // hash32 expensive

	args.size10 = vec3( 0.25, 0.5, 0.75 );
	args.height10 = vec4( 1.0 );
//	args.height10 = hash42( index + 80.0 ) * 0.25; // don't hash all... leave splits is interesting too

	args.size11 = vec3( 0.25, 0.5, 0.5 );
	args.height11 = vec4( 1.0 );
//	args.height11 = hash42( index + 85.0 ) * 0.25;

	args.height0 = mix( vec4( targs.hmin ), vec4( targs.hmax ), height0_hash );

	args.height10 = args.height0 + targs.hdetail * args.height10;
	args.height11 = args.height0 + targs.hdetail * args.height11;

	d = sd_TechTilesTestsSub( p, lod, t, args, e );

	// bevel
	d = opI( d, dot( p - vec3( 0, 0, 0.1 ), vec3( -V45.x, 0, V45.y ) ) );
	d = opI( d, dot( p - vec3( 0, 0, 0.1 ), vec3( 0, -V30.x, V30.y ) ) );

	return d;
}

// feature max height will be hscale*2
float sd_TechTiles( vec3 p, int lod, float t, Ray ray, TechTilesArgs0 targs, float e )
{
#if ( NUM_TILE_EVALS == 2 )

	// we do 2 evals in alternate checker patterns, "only" x2 cost and relatively clean
	// it still has corner cases (ha..ha..) but help in some situations

	float d = FLT_MAX;

	vec2 index0 = floor( p.xy );
	vec2 indexi = index0;
	float m = mod( indexi.x + indexi.y, 2.0 );

	vec2 dd;

	for ( int k = 0; k < 2; k += 1 )
	{
		vec3 p2 = p;
		vec2 index = index0;
		p2.xy = p.xy - index;

		if ( m == float( k ) )
		{
			vec2 offset = vec2( 0.0 );
			vec2 rp2 = p2.xy - 0.5;
			if ( abs( rp2.y ) > abs( rp2.x ) ) offset.y += rp2.y > 0.0 ? 1.0 : -1.0;
			else offset.x += rp2.x > 0.0 ? 1.0 : -1.0;
			index += offset;
			p2.xy -= offset;
		}

		float ddd = sd_TechTilesTestsSub0( p2, lod, t, ray, index, targs );
#if 0
		dd[k] = ddd; // gpu hangs on desktop (GTX 970)
#else
		if ( k == 0 ) dd.x = ddd;
		else  dd.y = ddd;
#endif

//		d = ddd; // compiler bug? doesn't work on laptop...
	}

	d = opU( dd.x, dd.y );

#else

	vec3 p2 = p;
	vec2 index = floor( p.xy );
	p2.xy = p.xy - index;
	return sd_TechTilesTestsSub0( p2, lod, t, ray, index, targs  ); // only 1 eval

#endif

}

float sd_DeathStarTrench( vec3 p, int lod, float t, Ray ray )
{
	float d = FLT_MAX;
	float hw = TRENCH_HALF_WIDTH;

#ifdef RAYMARCH_WITH_LOD
	d = opU( d, p.x + TRENCH_HALF_WIDTH );
	d = opU( d, -p.x + TRENCH_HALF_WIDTH );
	d = opU( d, p.z + TRENCH_DEPTH );
	d = opI( d, p.z );
	if ( abs( d ) > ROUGH_SHELL_D ) return d;
#endif

	TechTilesArgs0 targs;
	targs.hmin = 0.025;
	targs.hmax = 0.30;
	targs.hdetail = 0.05;

	// tiles for vertical walls
	TechTilesArgs0 targs_walls;
	targs_walls.hmin = 0.01;
	targs_walls.hmax = 0.22;
	targs_walls.hdetail = 0.05;

#if 1
	// the shallow trench rows perpendicular to the main one
	if ( mod( floor( p.y ), 7.0 ) == 0.0 )
	{
		targs.hmax *= 0.3;
		targs.hmin *= 0.3;
		targs_walls.hmax *= 0.3;
		targs_walls.hmin *= 0.3;
	}
#endif

	float h03 = 0.0; // surface trench clamp control
	float h00 = ( targs.hmax + targs.hdetail ); // wall tile top clamp control
	float h0 = h00;
	float h1 = ( targs_walls.hmax + targs_walls.hdetail );

	// inflate more for tracing
	h0 *= 2.0;
	h1 *= 2.0;

	bounds2 bsides = mkbounds( vec2( hw - h1, -TRENCH_DEPTH ), vec2( hw, h0 ) );
	bounds2 trench_xz = mkbounds( vec2( -hw, -TRENCH_DEPTH ), vec2( hw, 10 ) );

	float e = 0.15; // 51%
//	float e = 0.25; // 75%

	bool top_layer = ( p.z > 0.0 ) && ( p.z < h0 );
	bool bottom_layer = ( p.z < -TRENCH_DEPTH + h0 );

	bool top_layer2 = top_layer && ( abs( p.x ) > hw - h03 );
	bool bottom_layer2 = bottom_layer && ( abs( p.x ) <= hw );

	float hoffset = top_layer2 ? 0.0 : -TRENCH_DEPTH;

	targs.hmin += hoffset;
	targs.hmax += hoffset;

	{
		float d1 = sd_TechTiles( p, lod, t, ray, targs, e );
		d = opS( p.z, sd_bounds( p.xz, trench_xz ) );

		if ( top_layer2 || bottom_layer2 ) d = opU( d, d1 );
	}

	if ( inside( bsides, xset( p.xz, abs( p.x ) ) ) ) // perf culling
	{
		vec3 p2 = p.yzx;
		p2.z = abs( p2.z );
		p2.z -= hw;
		p2.z = -p2.z;
		p2.xy *= 2.0;

		float d1 = opI( p.z - TRENCH_DEPTH, sd_TechTiles( p2, lod, t, ray, targs_walls, e ) );
		d1 = opI( d1, p.z - h00 * 0.45 ); // clamp top
		d = opU( d1, d );
	}

	return d;
}

float sindecay( float x, vec3 args ) { return exp( -args.x * x ) * sin( x * args.y ) * args.z; }
float sindecay_derivative( float x, vec3 args ) { return ( args.y * cos( args.y * x ) - args.x * sin( args.y * x ) ) * exp( -args.x * x ) * args.z; }

// function used in overshoot steps, d is the derivative of the curve at x=0, only x>0 bit is used
float overshoot( float x, vec3 args, float df0 )
{
	if ( x > 1.0 ) return 1.0 - sindecay( x - 1.0, args );
	return cubicstep( x, df0, -sindecay_derivative( 0.0, args ) );
}

// first test, with an anticipation bump
float overshootstep1( float x, vec3 args )
{
	float df0 = 6.0;
	float s = 0.5;
	if ( x > 0.0 ) return 1.0 - ( 1.0 - overshoot( x, args, df0 ) ) * s;
	return 1.0 - ( 1.0 + ( 1.0 - cubicstep( max( x, -1.0 ) + 1.0, 0.0, df0 ) ) ) * s;
}

// dive_step_expin_a from y0+1.0 to 0.0
// df0 is the derivative at x=0 if we don't scale by 1/(1+y0)
// the real derivative at 0 is a/(a+df0)
// a controls the rate of ease in exp dive
// same as dive_step_expin_y0( x, df0, df0/a )
float overshootstep2( float x, float df0, float a, vec3 args )
{
	float y0 = df0 / a; // calculate y0 such that the derivative at x=0 becomes df0
	float y = x > 0.0 ? overshoot( x, args, df0 ) : -( 1.0 - exp( x * a ) ) * y0; // look there is a smiley in that calculation
	return ( y + y0 ) / ( 1.0 + y0 ); // the step goes from y0 to 1, normalize so it is 0 to 1
}

// like overshootstep2 but from -inf to 1 instead of 0 to 1 (steep turn)
float overshootstep3( float x, float df0, float a, vec3 args )
{
	float y0 = df0 / a; // calculate y0 such that the derivative at x=0 becomes df0
	float y = x > 0.0 ? overshoot( x, args, df0 ) : 1.0 - exp( -df0 * x );
	return ( y + y0 ) / ( 1.0 + y0 ); // the step goes from y0 to 1, normalize so it is 0 to 1
}

float oversteer( float x, float a, float b, float x0 ) { return exp( -pow2( x ) * 3.0 ) - exp( -pow2( x - x0 ) * 3.0 ) * 0.5; }

struct CameraRet { vec3 eye; float roll; float pitch; vec4 debug_color; float exposure; };

CameraRet init_cam()
{
	CameraRet cam;
	cam.pitch = 0.0;
	cam.roll = 0.0;
	cam.exposure = 1.0;
	cam.debug_color = vec4( 0.0 );
	return cam;
}

mat4 look_around_mouse_control( mat4 camera, float pitch, float tan_half_fovy )
{
	float mouse_ctrl = 1.0;
	vec2 mm_offset = vec2( 0.0, pitch );
	vec2 mm = vec2( 0.0, 0.0 );

#ifndef EXTRA_3D_CAMERA
	if ( iMouse.z > 0.0 )
		mm = ( iMouse.xy - iResolution.xy * 0.5 ) / ( min( iResolution.x, iResolution.y ) * 0.5 );
#endif

	mm.x = -mm.x;
	mm = sign( mm ) * pow( abs( mm ), vec2( 0.9 ) );
	mm *= PI * tan_half_fovy * mouse_ctrl;
	mm += mm_offset;
	return camera * yup_spherical_offset( mm.y, mm.x );
}

// awkwardly reproduce a camera path similar to the one in the movie
CameraRet get_camera1_movie_dive_path( float t )
{
	CameraRet cam = init_cam();

	// approach curves

	// turn curve
	float x = overshootstep3( t * 0.55 - 1.4, 2.2, 6.0, vec3( 1.1, 3.0, 0.5 ) ) - 1.0;

	// descent curve
	float zz = 1.0 - overshootstep2( t * 1.0 - 2.0, 0.8, 4.0, vec3( 1.0, 1.0, 0.4 ) );
	float z = -TRENCH_DEPTH * 0.75 + zz * 5.0;

	cam.eye = vec3( -x, t * FLYING_SPEED, z );

	float pp = ( 1.0 - smoothstep( 0.0, 2.4, t ) );
	cam.pitch = -PI * 0.18 * pp;
//	cam.debug_color = vec4( GREEN.rgb, pp * 0.5 );

	float rr = oversteer( ( t - 2.7 ), 0.3, 0.3, 1.0 );
	cam.roll -= -rr * PI * 0.2;
//	cam.debug_color = vec4( RED.rgb, rr*0.5 );

	cam.eye.z += 0.2 * sin( t * 2.0 );

	cam.eye.x += ( 0.4 * TRENCH_HALF_WIDTH * sin( t * 1.0 ) ); // left right amplitude
//	cam.roll -= 0.2 * cos( t * 1.0 + 0.25 ); // steering roll (anticipates, derivative)
	cam.roll += 0.05 * sin( t * 2.0 ); // noise roll

	// occasional jumps
	cam.eye.z += ( 1.0 - ( 1.0 + cos( PI * spaced_tri( t * 0.2, 4.0, 0.5 ) ) ) * 0.5 ) * 7.0;

	cam.exposure = mix( 0.57, 0.17, smoothstep( -1.0, 2.0, cam.eye.z ) );
	return cam;
}

mat4 get_camera1_movie_dive( mat4 camera, float tan_half_fovy, float t, inout vec4 debug_color, inout float exposure )
{
	vec3 center = get_camera1_movie_dive_path( t + 0.05 ).eye;
	CameraRet cam = get_camera1_movie_dive_path( t );
	vec3 eye = cam.eye;
	vec3 up = vec3( 0, 0, 1 );
	up.xz = rotate_with_angle( up.xz, cam.roll );
	camera = lookat( eye, center, up );
	exposure = cam.exposure;
	debug_color = cam.debug_color;
	return look_around_mouse_control( camera, cam.pitch, tan_half_fovy );
}

CameraRet get_camera2_path( float t )
{
	t *= 0.4; // slow down
	CameraRet cam = init_cam();
	cam.exposure = 0.35;
	cam.roll -= cos( t + 0.25 ) * 0.175;
	cam.pitch -= PI * 0.2;
	float xpos_max = 2.5;
	float ypos = t * FLYING_SPEED;
	xpos_max += ( 1.0 + sin( t * 0.05 ) * 0.5 ) * 2.0;
	cam.eye = vec3( xpos_max * sin( t ), ypos, 2.75 );
	return cam;
}

mat4 get_camera2( mat4 camera, float tan_half_fovy, float t, inout vec4 debug_color, inout float exposure )
{
	vec3 center = get_camera2_path( t + 0.02 ).eye;
	CameraRet cam = get_camera2_path( t );
	vec3 eye = cam.eye;
	vec3 up = vec3( 0, 0, 1 );
	up.xz = rotate_with_angle( up.xz, cam.roll );
	camera = lookat( eye, center, up );
	exposure = cam.exposure;
	debug_color = cam.debug_color;
	return look_around_mouse_control( camera, cam.pitch - PI * 0.08, tan_half_fovy );
}

// plane base 0,0,pz plane normal 0,0,nz
vec2 intersect_plane2_nz( Ray ray, float pz, float nz )
{
	float epsilon = 1e-4;
	float vdotn = ray.d.z * nz;
	float d = ( ray.o.z - pz ) * nz;
	float t = ( abs( vdotn ) <= epsilon ? FLT_MAX : -d / vdotn );
	return vec2( t, d );
}

// plane base 0,0,px plane normal 0,0,nx
vec2 intersect_plane2_nx( Ray ray, float px, float nx )
{
	float epsilon = 1e-4;
	float vdotn = ray.d.x * nx;
	float d = ( ray.o.x - px ) * nx;
	float t = ( abs( vdotn ) <= epsilon ? FLT_MAX : -d / vdotn );
	return vec2( t, d );
}

// turn t (result of intersect_plane* functions) into a negative space range
vec2 mm3( vec2 t ) { return t.y * t.x > 0.0 ? vec2( t.x, FLT_MAX ) : vec2( -FLT_MAX, t.x ); }

// intersect 2 negative space ranges, assume convex
vec2 mm4( vec2 a, vec2 b ) { return vec2( max( a.x, b.x ), min( a.y, b.y ) ); }

// raytrace to get a better first march distance
float warpTrace( Ray ray )
{
	vec2 t0 = intersect_plane2_nz( ray, ROUGH_SHELL_D, 1.0 );
	vec2 t1 = intersect_plane2_nx( ray, -TRENCH_HALF_WIDTH + ROUGH_SHELL_D, 1.0 );
	vec2 t2 = intersect_plane2_nx( ray, TRENCH_HALF_WIDTH - ROUGH_SHELL_D, -1.0 );
	vec2 t3 = intersect_plane2_nz( ray, -TRENCH_DEPTH + ROUGH_SHELL_D, 1.0 );
	float d = opU( opU( opI( t0.y, t1.y ), opI( t0.y, t2.y ) ), t3.y );
	if ( d < 0.0 ) return 0.0; // we are inside... do nothing and gtfo
	vec2 r0 = mm3( t0 );
	vec2 r01 = mm4( r0, mm3( t1 ) );
	vec2 r02 = mm4( r0, mm3( t2 ) );
	vec3 dd = vec3( FLT_MAX );
	if ( r01.x != FLT_MAX && r01.y > r01.x && r01.x > 0.0 ) dd.x = r01.x;
	if ( r02.x != FLT_MAX && r02.y > r02.x && r02.x > 0.0 ) dd.y = r02.x;
	if ( t3.x != FLT_MAX && t3.x > 0.0 ) dd.z = t3.x;
	return mincomp( dd );
}

float sd_Scene( vec3 p, int lod, float t, Ray ray )
{
	float d1 = FLT_MAX;
	float d2 = FLT_MAX;
#ifdef PRIMITIVES
// was for simple check lighting (need disable shadow hacks)
	d1 = opU( sd_bounds_range( p, -vec3( 1. ), vec3( 1. ) ), length( p - vec3( 0., 3., 0. ) ) - 1. );
#endif
#ifdef SORT_OF_MOON
	d2 = sd_DeathStarTrench( p, lod, t, ray );
#endif
	return opU( d1, d2 );
}

struct TraceOutput
{
	float t; // ray travel distance
	float num_iterations;
	float dist; // "hit" point distance to surface
	float shadow;
};

vec3 sd_SceneGrad( vec3 p, int lod, TraceOutput to, Ray ray )
{
	// if p is far away the epsilon will vanish in the addition and normal calculation will be broken
	// this was to keep the gradient working even at large-ish distances...
//	vec3 h = max( vec3( 0.006 ), abs( p ) * 1e-6 );
	vec3 h = vec3( 0.001 );
	vec3 n = normalize( vec3( sd_Scene( p + vec3( h.x, 0.0, 0.0 ), lod, to.t, ray ),
							  sd_Scene( p + vec3( 0.0, h.y, 0.0 ), lod, to.t, ray ),
							  sd_Scene( p + vec3( 0.0, 0.0, h.z ), lod, to.t, ray ) ) - to.dist ); // to.dist == sd_Scene( p, lod, to.t, ray ), our last eval 
	// if the normal is backfacing, our point p is likely behind an occluded object (a thin object or an edge we accidentally traced through)
	// this creates distracting salt noise that makes certain lighting components unstable (fresnel)
	// if we care the simplest hack to do that attenuates the artifacts in our scene is to negate
	return dot( n, ray.d ) > 0.0 ? -n : n;
}

TraceOutput traceScene( Ray ray, int lod, float shadow_sharpness, float tmax, bool warp_trace, float max_iterations )
{
	TraceOutput to;
	to.t = 0.0;
	to.num_iterations = 0.0;
	to.dist = 0.0;
	to.shadow = 1.0;

#ifdef RAYTRACE_FIRST_MARCH_DISTANCE
	if ( warp_trace ) { to.t += warpTrace( ray ); if ( to.t == FLT_MAX ) return to; } // jump close to first hit
#endif

	for ( int i = 0; i < MAX_ITERATIONS; ++i )
	{
		float d = sd_Scene( ray.o + to.t * ray.d, lod, to.t, ray );
		to.dist = d;
		if ( ( abs( to.dist ) <= DBREAK * to.t ) || to.t > tmax ) break;
		to.shadow = min( to.shadow, shadow_sharpness * to.dist / to.t ); // iq's awesome trick http://www.iquilezles.org/www/material/nvscene2008/rwwtt.pdf for shadows
		to.t += to.dist * TFRAC;
		to.num_iterations += 1.0;
		if ( to.num_iterations >= max_iterations ) break;
	}

	to.shadow = max( 0.0, to.shadow ); // fixes some artifacts
	return to;
}

float star_glare( float x, float e, float c ) { return exp2( -pow( x, e ) * c ); }

// black body color adapted from Fabrice Neyret's https://www.shadertoy.com/view/4tdGWM
// T absolute temperature (K), m1 is a 0,1 param that controls output scale
vec3 black_body( float T, float m1 )
{
	float m = .01 + 5. * m1;
	vec3 f = vec3( 1. ) + 0.5 * vec3( 0., 1., 2. );
	float Trcp = 1. / T;
	return ( 10. / m * ( f * f * f ) ) / ( exp( ( 19e+3 * f * Trcp ) ) - 1. );  // Planck law
}

// assumes x 0,1 maps to 24e+2, 30e+3
#define FF 24.
// vaguely use this distribution there https://en.wikipedia.org/wiki/Stellar_classification
float Tprob2( float x ) { return exp( -x * FF ); }
float Tprob2_int( float x ) { return -exp( -x * FF ) * ( 1.0 / FF ); }
float Tprob2_int_inv( float x ) { return -log( -x * FF ) * ( 1.0 / FF ); }
#if 0
float cdf( float x ) { return Tprob2_int( x ) - Tprob2_int( 0. );}
float cdf_inv( float x ) { return Tprob2_int_inv( x + Tprob2_int( 0. ) ); }
#else
float cdf( float x ) { return Tprob2_int( x ) - ( -1. / FF ); } // hardcode the function
float cdf_inv( float x ) { return Tprob2_int_inv( x + ( -1. / FF ) ); }
#endif

vec3 star_color( float x ) { return black_body( mix( 24e+2, 30e+3, x ), 1. ); }

#ifdef STARFIELD
// stars are clamped on their cell borders (we don't bother iterating neighbours)
vec3 starfield( vec3 viewvec )
{
	vec2 num = vec2( 1.0, 2.0 ) * 250.0;
	float theta = acos( viewvec.z );
	float phi = calc_angle( viewvec.xy );
	if ( phi < 0.0 ) phi += 2.0 * PI;
	vec2 sc = vec2( theta, phi );
	vec2 scd = vec2( 1.0, 2.0 ) * PI / num;
	vec2 sci = floor( sc / scd ); // if ( mod( sci.x + sci.y, 2.0 ) == 1.0 ) return RED;
	vec2 scf = ( sc - sci * scd ) / scd; //	return vec3( scf, 0.0 ); // view cells
//	vec4 r = vec4(0.5); // aligned stars
	vec4 r = hash42( sci ); //	return vec3( r ); // color cell by hash
	vec2 scc = ( ( sci + 0.5 ) * scd ); // cell center
	vec2 r2 = hash22( sci ); //	return vec3( r2.y ); // debug // x: discard probability, y: color distribution uniform input
	if ( r2.x > sin( scc.x ) ) return BLACK; // randomly decimate in sin theta fashion for something uniform-ish
//	if ( r2.x > sin( scc.x ) ) return RED; // visualize the decimation
	vec2 sc2 = ( ( sci + r.xy ) * scd ); // cell center
	vec3 v = zup_spherical_coords_to_vector( sc2.x, sc2.y ); // return v;
	v = normalize( v ); // we also need to renormalize already normal vector here else broken on GTX1060 laptop
	float c = 1.0 - dot( v, viewvec ); // return vec3( 1.0 - smoothstep( 0.0, 1.0, c ) ); // debug: show the full disks (max glow radius) // 0->2
	vec3 col = star_color( cdf_inv( r2.y * cdf( 1. ) ) ); // return vec3( chrominance( col ) );
#if 1
	c *= 1e+6;
	col = mix( col, vec3( maxcomp( col ) ), 0.5 ); // after all those efforts we lerp towards monotone... :-]
	float s = smoothstep( 512.0, 1280.0, iResolution.x );
	return star_glare( c * 0.008, 0.8, 40.0 + s * 40.0 ) * col * 20.;
//	return star_glare( c * 0.01, mix( 0.4, 0.8, r.w ), mix( 30.0, 60.0, r.z ) ) * col * 120.; // manual glow;
#else
// with larger glares and plausible star colors it starts to look like space photos a bit (maybe)
	c *= 6e+4;
	return star_glare( c, mix( 0.3, 0.8, r.w ), mix( 3.0, 6.0, r.z ) ) * col * 80.; // manual glow;
#endif
}
#endif

void build_onb( vec3 z, vec3 x0, out vec3 x, out vec3 y ) { y = normalize( cross( x0, z ) ); x = normalize( cross( z, y ) ); }

vec3 sunval( float sun_dp, vec2 p, vec3 sun_color, vec3 sun_color_c )
{
	float r = ( ( -sun_dp + 1. ) * 0.5 ) * 300.0;
	float f = 0.;
#if 1
// flares
	float a = calc_angle( p );
	float da = 2.0 * PI / 6.0;
	float a0 = floor( a * ( 1.0 / da ) ) * da;
	vec2 va0 = perp( unit_vector2( a0 ) );
	vec2 va1 = -perp( unit_vector2( a0 + da ) );
	vec2 d = vec2( dot( va0, p ), dot( va1, p ) );
	float f0 = opU( d.x, d.y );
	f = f0 * 8.;
	f += 1.0 - exp( -r * 0.01 ) * 0.85;
	f = 1. / ( 0.01 + f * 50. );
	f *= 5.0;
#endif
	return 40. * sun_color * star_glare( max( 0.0, r - 0.17 ) * 0.075, 0.35, 24.0 ) + f * sun_color_c;
}

// I used a bit of pbr reference to see what happens (don't worry it all deteriorates into sad hacks pretty quickly)
// https://learnopengl.com/#!PBR/Theory
// http://graphicrants.blogspot.jp/
// alpha = roughness * roughness
float D_blinn_phong( float m_dot_n, float alpha ) { float alpha_sqr = alpha * alpha; return pow( m_dot_n, ( 2. / alpha_sqr ) - 2. ) / ( PI * alpha_sqr ); }
float D_beckmann( float m_dot_n, float alpha )
{
	float alpha_sqr = alpha * alpha;
	float m_dot_n_sqr = m_dot_n * m_dot_n;
	return exp( ( m_dot_n_sqr - 1. ) / ( alpha_sqr * m_dot_n_sqr ) ) / ( PI * alpha_sqr * m_dot_n_sqr * m_dot_n_sqr );
}
// Trowbridge-Reitz
float D_GGX( float m_dot_n, float alpha ) { float alpha_sqr = alpha * alpha; return alpha_sqr / ( PI * pow2( pow2( m_dot_n ) * ( alpha_sqr - 1. ) + 1. ) ); }
float D_GGXaniso( vec3 m, vec3 n, float alpha_x, float alpha_y, vec3 x, vec3 y ) { return 1. / ( PI * alpha_x * alpha_y * pow2( pow2( dot( x, m ) / alpha_x ) + pow2( dot( y, m ) / alpha_y ) + pow2( dot( n, m ) ) ) ); }
float G_implicit( float n_dot_l, float n_dot_v ) { return n_dot_l * n_dot_v; }
float G_neumann( float n_dot_l, float n_dot_v ) { return n_dot_l * n_dot_v / max( n_dot_l, n_dot_v ); }
float G_cooktorrance( float n_dot_l,
					  float n_dot_v,
					  float n_dot_h,
					  float v_dot_h )
{
	return min( 1.,
				min( 2. * n_dot_h * n_dot_v / v_dot_h,
					 2. * n_dot_h * n_dot_l / v_dot_h ) );
}
float G_kelemen( float n_dot_l, float n_dot_v, float v_dot_h ) { return n_dot_l * n_dot_v / pow2( v_dot_h ); }
float F_none( float v_dot_h, float F0 ) { return F0; }
float F_schlick( float v_dot_h, float F0 ) { return F0 + ( 1. - F0 ) * pow5( 1. - v_dot_h ); }
float F_cooktorrance( float v_dot_h, float F0 )
{
	float F0_sqrt = sqrt( F0 );
	float mu = ( 1. + F0_sqrt ) / ( 1. - F0_sqrt );
	float c = v_dot_h;
	float g = sqrt( mu * mu + c * c - 1. );
	return 0.5 * pow2( ( g - c ) / ( g + c ) ) * ( 1. + pow2( ( ( g + c ) * c - 1. ) / ( ( g - c ) * c + 1. ) ) );
}
float G_smith_beckmann( float n_dot_v, float alpha ) { float c = n_dot_v / ( alpha * sqrt( 1. - n_dot_v * n_dot_v ) ); return c < 1.6 ? ( ( 3.535 * c + 2.181 * c * c ) / ( 1. + 2.276 * c + 2.577 * c * c ) ) : 1.; }
float G_smith_GGX( float n_dot_v, float alpha ) { float alpha_sqr = alpha * alpha; return 2. * n_dot_v / ( n_dot_v + sqrt( alpha_sqr + ( 1. - alpha_sqr ) * n_dot_v * n_dot_v ) ); }
float G_smith_schlick_beckmann( float n_dot_v, float alpha ) { float k = alpha * sqrt( 2. / PI ); return n_dot_v / ( n_dot_v * ( 1. - k ) + k ); }

vec3 add_light_contrib( vec3 albedo, vec3 l, vec3 n, vec3 v, float Li, float dwi, float kdiffuse, float kspecular )
{
	float F0 = 0.08;
	float roughness = 0.25;
	float alpha = roughness * roughness;
	vec3 h = normalize( l + v );
	float eps = 1e-4; // else divides by zero
	float n_dot_l = max( eps, dot( n, l ) );
	float n_dot_v = max( eps, dot( n, v ) );
	float n_dot_h = max( eps, dot( n, h ) );
	float v_dot_h = max( eps, dot( h, v ) );
	float l_dot_h = max( eps, dot( l, h ) );

//	float D = D_blinn_phong( n_dot_h, alpha ); // https://en.wikipedia.org/wiki/Blinn%E2%80%93Phong_shading_model
//	float D = D_beckmann( n_dot_h, alpha );
	float D = D_GGX( n_dot_h, alpha ); // n_dot_h should probably be clamped to >=0

//	float G = G_implicit( n_dot_l, n_dot_v );
	float G = G_neumann( n_dot_l, n_dot_v );
//	float G = G_cooktorrance( n_dot_l, n_dot_v, n_dot_h, v_dot_h );
//	float G = G_kelemen( n_dot_l, n_dot_v, v_dot_h );
//	float G = G_smith_beckmann( n_dot_v, alpha ) *
//	          G_smith_beckmann( n_dot_l, alpha );
//	float G = G_smith_GGX( n_dot_v, alpha ) *
//	          G_smith_GGX( n_dot_l, alpha );
//	float G = G_smith_schlick_beckmann( n_dot_v, alpha ) *
//	          G_smith_schlick_beckmann( n_dot_l, alpha );

//	float F = F_none( n_dot_v, F0 );
	float F = F_schlick( n_dot_v, F0 );

	return  ( ( kdiffuse * albedo * ( 1.0 / PI ) + kspecular * ( D * F * G ) / ( 4. * n_dot_l * n_dot_v ) ) ) * Li * n_dot_l * dwi;
}

// e = eye pos, v = vader vector, p = lit point, n = normal, l = sun direction
vec3 shade( in vec3 e, in vec3 v, mat4 cam, in vec3 p, in vec3 n, vec3 l
			, float traced_shadow, float sun_shadow, float first_bounce
			, TraceOutput to, float ao, float exposure )
{
	vec3 col = vec3( 0. );
	bool sky = to.t > TMAX;

//	return sky ? BLACK : ( vec3( 1.0 ) + n ) * 0.5; // normal debug color
//	return vec3( 1.0 - exp( -to.t * 0.15 ) ); // distance to hit point
//	return viridis_quintic( to.num_iterations * ( 1.0 / float( MAX_ITERATIONS ) ) ); // visualize the number of iterations
//	return viridis_quintic( to.dist / ( DBREAK * to.t ) );
//	return viridis_quintic( to.dist );
//	return viridis_quintic( ao );

	vec3 sun_color = black_body( 19000.0, 0.8 );
	vec3 sun_color_c = chrominance( sun_color );
	vec3 albedo = vec3( 0.85, 0.85, 1 );
#ifdef COLOR_TILES
	albedo = mix( vec3( 0.2 ), vec3( 1.0 ), hash32( floor( p.xy ) ) );
#endif

	if ( !sky )
	{
		float iter = to.num_iterations * ( 1.0 / float( MAX_ITERATIONS ) );
		float fog_start = 30.0;
		float fog = exp2( -max( to.t - fog_start, 0.0 ) * 0.02 );
		float ao_z = 1.0 - saturate( abs( p.z * ( 1.0 / TRENCH_DEPTH ) ) ); // vertical occlusion
		float kdiffuse = 0.75;
		float kspecular = 1.0;
		float sunI = 40.0;
		float kambient = 0.25;
		float shadow = min( traced_shadow, sun_shadow );
#if 1
		col += add_light_contrib( albedo, l, n, -v
								  , sunI
								  , 1.0
								  , kdiffuse * mix( 0.2, 1.0, ao_z ) * mix( 0.025, 1.0, shadow )
								  , kspecular * shadow * ao_z ); // main sun light
#endif
#if 1
		vec3 l2 = normalize( vec3( 5, -5.5, 10 ) );
		col += add_light_contrib( albedo, l2, n, -v
								  , 8.0 * sunI / 40.0
								  , 1.0
								  , kdiffuse * mix( 0.2, 1.0, ao_z )
								  , kspecular * shadow * ao_z ); // dummy secondary source
#endif
#if 1
		col += add_light_contrib( albedo, reflect( l, vec3( -1., 0., 0. ) ), n, -v
								  , 0.7 * sunI / 40.0 // we could evaluate...
								  , 1.0
								  , kdiffuse * first_bounce
								  , kspecular * first_bounce ); // first bounce
#endif
#if 1
		col += PI * vec3( kambient * ao_z * ao ); // ambient
#endif
		col *= fog * ao * ao * ( 1.0 - iter * 0.7 ); // pffuwahaha
	}
	else
	{
		col = vec3( 0.0 );

#ifdef SUN
		vec3 sx, sy;
		build_onb( l, cam[0].xyz, sx, sy ); // we want the flares to be viewspaceish
		col += sunval( dot( l, v ), vec2( dot( v, sx ), dot( v, sy ) ), sun_color, sun_color_c );
#endif

#ifdef STARFIELD
		col += starfield( v );
#endif
	}

//	return col;
	return vec3( 1. ) - exp2( -col * exposure );
}

// black -> green -> white gradient
vec3 laser_heatmap( float u ) { float r = 0.5; vec3 c = vec3( smoothbump( r * float( 2.0 ), r, u ) ); c.g += smoothbump( r * float( 1.0 ), r, u ); return c; }

#define LASER_LEN 1.5
#define LASER_LEN_RCP (1.0/(LASER_LEN))
#define LASER_SPEED (60.0)
// spawn at player + that
#define LASER_SPAWN_DISTANCE (40.0)
#define LASER_PERIOD (LASER_SPAWN_DISTANCE*2.0/(FLYING_SPEED+LASER_SPEED))

// for lasers capsules...
vec2 sphere_trace( vec2 O, vec2 d, float radius, vec2 C )
{
	float tp = dot( C - O, d ); // P = project C on line (O,d)
	vec2 P = O + d * tp;
	float h_sqr = lensqr( P - C );
	float radius_sqr = radius * radius;
	if ( h_sqr > radius_sqr ) return vec2( FLT_MAX, FLT_MAX ); // ray missed the sphere
//	bool start_inside = lensqr( O - C ) <= radius_sqr; // start inside the sphere?
	float dt = sqrt( radius_sqr - h_sqr ); // distance from P to In (near hit) and If (far hit)
//	if ( start_inside )	return vec2(FLT_MAX,tp+dt);	// order In->O->If // record only far hit If
//	if ( tp < 0.0 )	return vec2(FLT_MAX,FLT_MAX); // order In->If->O // O is outside the sphere and beyhond If, no hit
	return vec2( tp - dt, tp + dt ); // record 2 hits In, If
}

// laser code is total bloat, we raytrace infinite cylinders and integrate something
// volumetric along a fixed number of steps between the 2 intersections, when they exist
vec3 lasers( Ray view_ray, float hs, float time, float t0 )
{
	float fade = 1.0 - smoothstep( -TRENCH_DEPTH * 0.05, 0.0, view_ray.o.z );
	float pos = FLYING_SPEED * time; // camera pos
	float laser_period = LASER_PERIOD * FLYING_SPEED;
	float offset = hs * 5.0;
	float nth = floor( ( pos - offset ) / laser_period );
	float y0 = offset + nth * laser_period + LASER_SPAWN_DISTANCE;
	float yy_t = ( pos - ( offset + nth * laser_period ) ) * ( 1.0 / FLYING_SPEED );
	float laser_pos = y0 - yy_t * LASER_SPEED;
	vec2 orig = hash21( nth * hs );
	float w2 = TRENCH_HALF_WIDTH * 0.8;
	orig = mix( vec2( -w2, -TRENCH_DEPTH * 0.9 ), vec2( w2, -TRENCH_DEPTH * 0.3 ), orig );
	float r = 0.085;
	vec3 color = BLACK;

	//todo: case when we are inside the laser
	float vzx1len = length( view_ray.d.zx );
	vec2 vzx1 = view_ray.d.zx / vzx1len;
	vec2 st = sphere_trace( view_ray.o.zx, vzx1, r, vec3( orig.x, 0, orig.y ).zx );
	if ( st.x == FLT_MAX || // no hit
		 st.y < 0.0 ) // hitting behind the camera
					  // we don't intersect the laser cylinder, gtfo
		return color;

	vec3 cacc = vec3( 0.0);
	float aacc = 0.0;

	float vzx1len_rcp = 1.0 / vzx1len;
	float t00 = st.x * vzx1len_rcp;
	float t11 = st.y * vzx1len_rcp;

#define LASER_ITER 20.0
	for ( float i = 0.0; i <= LASER_ITER; i += 1.0 )
	{
		float x = i * ( 1.0 / LASER_ITER );
		float t = t00 + ( t11 - t00 ) * cubicstep(  x, 2.5, 2.5 ); // smaller steps near the center to better capture the gradient
		vec3 p = view_ray.o + t * view_ray.d;
		vec3 vzx = p - project_on_liney( p, vec3( orig.x, 0, orig.y ) );
		vec3 pl = p;
		pl.y -= laser_pos; // local
		float ri = length( vzx );
		if ( pl.y > LASER_LEN - r ) ri = length( pl - vec3( orig.x, LASER_LEN - r, orig.y ) );
		else if ( pl.y < -LASER_LEN + r ) ri = length( pl - vec3( orig.x, -LASER_LEN + r, orig.y ) );
		float ddx = 1.0 - saturate( ri * ( 1.0 / r ) );
		float xxx = pl.y * LASER_LEN_RCP;
		ddx *= box( xxx ) * fade;
		vec3 emission = laser_heatmap( ddx ) * 0.2;
		float opacity = saturate( ddx * 0.1 );
		cacc += ( 1.0 - aacc ) * emission;
		aacc += ( 1.0 - aacc ) * opacity; // http://developer.download.nvidia.com/books/HTML/gpugems/gpugems_ch39.html
	}

	color = cacc;
	color = cacc * aacc;
	return color;
}

float penumbra( float x ) { x = max( x, 0.0 ); x = 1.0 - exp2( -x * x * 8.0 ); return x; }

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
	fragColor = vec4( 0., 0., 0., 1 );
	float aspect = ( iResolution.x / iResolution.y );
	vec2 uv = ( fragCoord.xy + vec2( 0.5, 0.5 ) ) / iResolution.xy;

//	fragColor.xyz = star_color( uv.x ).xyz; return;
//	fragColor.xyz = laser_heatmap( uv.x ); return;

	float tan_half_fovy = 0.6;
	float znear = 0.1;
	vec4 debug_color = vec4( 1.0, 1.0, 1.0, 0.0 );
	float time = iTime + TIME_OFFSET;
	float fade = 1.0;
	float exposure = 1.0;
	mat4 camera;

#ifdef EXTRA_3D_CAMERA
	camera[0] = iCamera[0];
	camera[1] = iCamera[1];
	camera[2] = iCamera[2];
	camera[3] = iCamera[3];
	tan_half_fovy = iTanHalfFovy;
	exposure = iExposure;
#else
	{
		float time_slice = 30.0;
#ifdef CAMERA_REPEAT
		float camera_select = mod( floor( time / time_slice ), 2.0 );
		float time_slice_r = 0.25;
		fade = 1.0 - min( spaced_bumps( time, time_slice, time_slice_r ), 1.0 - box( time / time_slice_r ) );
		time = mod( time, time_slice );
#else
		float camera_select = 0.0;
#endif
		if ( camera_select == 0.0 ) camera = get_camera1_movie_dive( camera, tan_half_fovy, time, debug_color, exposure );
		else camera = get_camera2( camera, tan_half_fovy, time + CAM2_TIME_OFFSET, debug_color, exposure );
	}
#endif

	Ray view_ray = get_view_ray( ( uv - vec2( 0.5 ) ) * 2.0, znear, aspect, tan_half_fovy );

	view_ray.o = camera[3].xyz;
	view_ray.d = ( camera * vec4( view_ray.d, 0.0 ) ).xyz;
	view_ray.d = normalize( view_ray.d ); // have to renormalize this already normalized (yey!) vector here else lasers are broken (only on laptop GTX1060)

	TraceOutput to = traceScene( view_ray, 0, 15.0, TMAX, true, float( MAX_ITERATIONS ) );

	vec3 l = normalize( vec3( -8, 0, 5.2 ) ); // careful with z, long shadows make the tracing slower
	vec3 p = view_ray.o + to.t * view_ray.d;
	vec3 n = sd_SceneGrad( p, 0, to, view_ray );

	float ao = 1.0;

#ifdef AO
	{
		// http://www.iquilezles.org/www/material/nvscene2008/rwwtt.pdf
		float delta = 0.1;
		float a = 0.0;
		float b = 1.0;
		for ( int i = 0; i < 5; i++ )
		{
//			if ( to.t > 30 ) break;
			float fi = float( i );
			float d = sd_Scene( p + delta * fi * n, 0, to.t, view_ray );
			a += ( delta * fi  - d ) * b;
			b *= 0.5;
		}
		ao = saturate( 1.0 - 1.2 * a );
		// note: had to lower DBREAK inorder to avoid ugly patterns when far away from surface
	}
#endif

	bool sky = to.t > TMAX;

	float sun_shadow = 1.0; // shadow
	float first_bounce = 0.0; // first light bounce (illuminates the bottom left corner)
	float traced_shadow = 1.0;

	if ( !sky )
	{
#ifdef TRACED_SHADOW
		float expensive_shadow_dist = 40.0;
		if ( to.t < expensive_shadow_dist )
		{
			Ray sray;
			sray.o = p + n * 0.1 * 1.0;
			sray.d = l;

			TraceOutput tos = traceScene( sray, 0, 15.0, 10.0, false, 20.0 );
			traced_shadow = tos.shadow;
			traced_shadow += smoothstep( expensive_shadow_dist - 2.0, expensive_shadow_dist, to.t );
			traced_shadow = saturate( traced_shadow );
		}
#endif
		vec2 l2 = normalize( l.xz );
		vec2 l2r = reflect( l2, vec2( -1, 0 ) );
		float so = 0.0; // shadow plane offset from z=0
		Plane2 pl1 = mkplane2( vec2( -TRENCH_HALF_WIDTH, so ), -perp( l2 ) );
		Plane2 pl2 = mkplane2( vec2( TRENCH_HALF_WIDTH, so ), -perp( l2r ) );
		Plane2 pl3 = pl2;
		pl3.normal = -pl3.normal;
		pl3.base.y += ( l2.y / l2.x ) * 2.0 * TRENCH_HALF_WIDTH;

		if ( ( abs( p.x ) < TRENCH_HALF_WIDTH ) && ( p.z < so ) )
		{
#ifdef ANALYTICAL_SHADOW
			sun_shadow = penumbra( dot( p.xz - pl1.base, pl1.normal ) );
#endif
			first_bounce = min( penumbra( dot( p.xz - pl2.base, pl2.normal ) ),
								penumbra( dot( p.xz - pl3.base, pl3.normal ) ) );
			first_bounce *= step( p.x, TRENCH_HALF_WIDTH - 0.5 ) * step( p.z, 0.0 );
		}
	}

	fragColor.rgb = shade( view_ray.o, view_ray.d, camera, p, n, l
						   , traced_shadow, sun_shadow, first_bounce, to, ao, exposure );
#ifdef LASERS
	fragColor.rgb += lasers( view_ray, 0.0, time, to.t );
	fragColor.rgb += lasers( view_ray, 1.0, time, to.t );
	fragColor.rgb += lasers( view_ray, -1.0, time, to.t );
	fragColor.rgb += lasers( view_ray, -0.5, time, to.t );
	fragColor.rgb += lasers( view_ray, +0.5, time, to.t );
//	fragColor.rgb += lasers( view_ray, +0.8, time, to.t );
#endif
	fragColor.rgb *= fade;
	fragColor.rgb = mix( fragColor.rgb, debug_color.rgb, debug_color.a );
}











