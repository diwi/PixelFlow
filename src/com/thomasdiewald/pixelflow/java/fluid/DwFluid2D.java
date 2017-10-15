/**
 * 
 * PixelFlow | Copyright (C)  2016 Thomas Diewald - http://thomasdiewald.com
 * 
 * A Processing/Java library for high performance GPU-Computing (GLSL).
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package com.thomasdiewald.pixelflow.java.fluid;


import com.jogamp.opengl.GL2;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLSLProgram;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture.TexturePingPong;

import processing.opengl.PGraphics2D;
import processing.opengl.Texture;;


public class DwFluid2D{

  
  static public class Param{
    // buoyancy: temperature
    public boolean apply_buoyancy          = true;
    public float   temperature_ambient     = 0.0f;
    public float   fluid_buoyancy          = 1.0f;
    public float   fluid_weight            = 0.001f;
    
    // noise: vorticity
    public float   vorticity               = 0.0f;

    // dissipation/viscosity
    public float   dissipation_velocity    = 1;
    public float   dissipation_density     = 1;
    public float   dissipation_temperature = 1;
    
    // diffusion coeffs, ... not sure if necessary
    private float  diffusion_velocity      = 0.0f;
    private float  diffusion_density       = 0.0f;
    private float  diffusion_temperature   = 0.0f;
    
    // jacobi iterations
    public int     num_jacobi_projection   = 40;
    public int     num_jacobi_diffuse      = 20;
    
    // solution quality/performance
    public float   timestep                = 0.125f;
    public float   gridscale               = 1.0f;
  }


  // context, for several begin/end scopes, fbos, shaderFactory etc..
  public DwPixelFlow context;
  
  //update counter
  public  int   simulation_step;
  
  // dimension
  public  int   grid_scale;
  public  int   fluid_w, fluid_h; 
  public  int   viewp_w, viewp_h; 

  // fluid parameters
  public Param param = new Param();
   
  // textures
  public TexturePingPong tex_velocity    = new TexturePingPong();
  public TexturePingPong tex_density     = new TexturePingPong();
  public TexturePingPong tex_pressure    = new TexturePingPong();
  public TexturePingPong tex_temperature = new TexturePingPong();
  public TexturePingPong tex_obstacleC   = new TexturePingPong();
  public TexturePingPong tex_obstacleN   = new TexturePingPong();
  public DwGLTexture     tex_divergence  = new DwGLTexture();
  public DwGLTexture     tex_curl        = new DwGLTexture();
  
  // shaders ... will be created by GLScope, and also released there
  private DwGLSLProgram shader_obstacleBounds;
  private DwGLSLProgram shader_advect        ; 
  private DwGLSLProgram shader_buoyancy      ; 
  private DwGLSLProgram shader_divergence    ; 
  private DwGLSLProgram shader_jacobi        ; 
  private DwGLSLProgram shader_gradient      ; 
  private DwGLSLProgram shader_vorticityCurl ; 
  private DwGLSLProgram shader_vorticityForce;
  private DwGLSLProgram shader_renderFluid   ;
  private DwGLSLProgram shader_renderVelocityStreams;
  
  private DwGLSLProgram shader_addVelocityBlob   ; 
  private DwGLSLProgram shader_addDensityBlob    ; 
  private DwGLSLProgram shader_addTemperatureBlob; 
  private DwGLSLProgram shader_addDensityTexture ; 
  private DwGLSLProgram shader_addObstacleTexture; 
  

  // callbacks
  private FluidData CB_fluid_data;
  private Advect    CB_advect;

  public DwFluid2D(DwPixelFlow context){
    this.context = context;
    context.papplet.registerMethod("dispose", this);
    
    shader_obstacleBounds        = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/obstacleBounds.frag");
    shader_advect                = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/advect.frag"        ); 
    shader_buoyancy              = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/buoyancy.frag"      ); 
    shader_divergence            = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/divergence.frag"    ); 
    shader_jacobi                = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/jacobi.frag"        ); 
    shader_gradient              = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/gradient.frag"      ); 
    shader_vorticityCurl         = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/vorticityCurl.frag" ); 
    shader_vorticityForce        = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/vorticityForce.frag");
    shader_renderFluid           = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/renderFluid.frag");
    shader_renderVelocityStreams = context.createShader(DwPixelFlow.SHADER_DIR+"Fluid/renderVelocityStreams.vert", DwPixelFlow.SHADER_DIR+"Fluid/renderVelocityStreams.frag");
   
    shader_addVelocityBlob       = context.createShader(DwPixelFlow.SHADER_DIR+"addData/addVelocityBlob.frag"   );
    shader_addDensityBlob        = context.createShader(DwPixelFlow.SHADER_DIR+"addData/addDensityBlob.frag"    );
    shader_addTemperatureBlob    = context.createShader(DwPixelFlow.SHADER_DIR+"addData/addTemperatureBlob.frag");
    shader_addDensityTexture     = context.createShader(DwPixelFlow.SHADER_DIR+"addData/addDensityTexture.frag" );
    shader_addObstacleTexture    = context.createShader(DwPixelFlow.SHADER_DIR+"addData/addObstacleTexture.frag");
  }
  
  public DwFluid2D(DwPixelFlow context, int viewport_width, int viewport_height, int fluidgrid_scale){
    this(context);
    resize(viewport_width, viewport_height, fluidgrid_scale);
  }
 
  public void dispose(){
    release();
  }
  
  public void release(){                
    tex_velocity   .release();
    tex_density    .release();
    tex_temperature.release();
    tex_curl       .release();
    tex_divergence .release();
    tex_pressure   .release();
    tex_obstacleC  .release();
    tex_obstacleN  .release();
  }
  
  public void reset(){
    clearTextures(0.0f);
    simulation_step = 0;
  }
  
  private void clearTextures(float v){
    context.begin();
    tex_velocity   .clear(v);
    tex_density    .clear(v);
    tex_temperature.clear(v);
    tex_pressure   .clear(v);
    tex_obstacleC  .clear(v);
    tex_obstacleN  .clear(v);
    tex_curl       .clear(v);
    tex_divergence .clear(v);
    context.end("Fluid.clearTextures");
  }
  


  public boolean resize(int viewport_width, int viewport_height, int fluidgrid_scale) {
    
    grid_scale = Math.max(1, fluidgrid_scale);
    
    viewp_w = viewport_width;
    viewp_h = viewport_height;
    
    fluid_w = (int) Math.ceil(viewp_w / (float) grid_scale);
    fluid_h = (int) Math.ceil(viewp_h / (float) grid_scale);
    
    context.begin();

    boolean resized = false;
    resized |= tex_velocity   .resize(context, GL2.GL_RG16F  , fluid_w, fluid_h, GL2.GL_RG  , GL2.GL_FLOAT        , GL2.GL_LINEAR , 2,4);
    resized |= tex_density    .resize(context, GL2.GL_RGBA16F, fluid_w, fluid_h, GL2.GL_RGBA, GL2.GL_FLOAT        , GL2.GL_LINEAR , 4,4);
    resized |= tex_temperature.resize(context, GL2.GL_R16F   , fluid_w, fluid_h, GL2.GL_RED , GL2.GL_FLOAT        , GL2.GL_LINEAR , 1,4);
    resized |= tex_curl       .resize(context, GL2.GL_R16F   , fluid_w, fluid_h, GL2.GL_RED , GL2.GL_FLOAT        , GL2.GL_LINEAR , 1,4);
    resized |= tex_divergence .resize(context, GL2.GL_R16F   , fluid_w, fluid_h, GL2.GL_RED , GL2.GL_FLOAT        , GL2.GL_LINEAR , 1,4);
    resized |= tex_pressure   .resize(context, GL2.GL_R16F   , fluid_w, fluid_h, GL2.GL_RED , GL2.GL_FLOAT        , GL2.GL_LINEAR , 1,4);
    resized |= tex_obstacleC  .resize(context, GL2.GL_R8     , fluid_w, fluid_h, GL2.GL_RED , GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, 1,1);
    resized |= tex_obstacleN  .resize(context, GL2.GL_RGBA8  , fluid_w, fluid_h, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, GL2.GL_NEAREST, 4,1);

    if(resized){
      reset();
    }
    
    context.end("Fluid.resize");
    return resized;
  }
  
  

  
  public void update(float timestep) {   
    param.timestep = timestep;
    update();
  }

  public void update(){
    context.begin();
    
    // create cell-neighbor scalefactors
    createObstacleN();
    
    ////////////////////////////////////////////////////////////////////////////
    // 1) advect
    //    Advection is the process by which a fluid's velocity transports itself 
    //    and other quantities in the fluid.
    ////////////////////////////////////////////////////////////////////////////
    
    
    // some value mapping
    float v_diss = (float)Math.pow(param.dissipation_velocity   , 0.05f);
    float d_diss = (float)Math.pow(param.dissipation_density    , 0.05f);
    float t_diss = (float)Math.pow(param.dissipation_temperature, 0.05f);


    // velocity
    advect(tex_velocity.src, tex_velocity.src, tex_velocity.dst, v_diss);
    tex_velocity.swap();

    // density
    advect(tex_velocity.src, tex_density.src, tex_density.dst, d_diss);
    tex_density.swap();
    
    // temperature
    advect(tex_velocity.src, tex_temperature.src, tex_temperature.dst, t_diss);
    tex_temperature.swap();
    
    // who knows, maybe someone advects his own stuff
    if(CB_advect != null){
      context.end();
      CB_advect.update(this);
      context.begin();
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // 2) diffuse
    ////////////////////////////////////////////////////////////////////////////
    
    // not used atm, TODO
    diffuse(tex_velocity   , param.diffusion_velocity);
    diffuse(tex_density    , param.diffusion_density);
    diffuse(tex_temperature, param.diffusion_temperature);
    
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // 3) add forces
    ////////////////////////////////////////////////////////////////////////////
    
    
    // callback for updating any texture kind of texture data ... by the user
    if(CB_fluid_data != null){
      context.end();
      CB_fluid_data.update(this);
      context.begin();
    }
    
    
    // buoyancy
    if(param.apply_buoyancy){
      buoyancy(tex_velocity.src, tex_temperature.src, tex_density.src, tex_velocity.dst);
      tex_velocity.swap();
    }
    
    // vorticity confinement
    if(param.vorticity >= 0.0){
      vorticity(tex_velocity.src, tex_velocity.dst, param.vorticity);
      tex_velocity.swap();
    }
    

    ////////////////////////////////////////////////////////////////////////////
    // 4) projection
    ////////////////////////////////////////////////////////////////////////////
    
    // divergence
    // ... is the rate at which "density" exits a given region of space, and it 
    // measures the net change in velocity at a gridcell. 
    // the divergence of a vector field is a scalar field.
    divergence(tex_velocity.src, tex_divergence);
  
    // jacobi solver, pressure projection
    tex_pressure.src.clear(0);
    for (int i = 0; i < param.num_jacobi_projection; ++i) {
      jacobiPressure(tex_pressure.src, tex_divergence, tex_pressure.dst);
      tex_pressure.swap();
    }
    
    // subtract pressure-gradients (scalar-field) from intermediate velocities.
    gradient(tex_velocity.src, tex_pressure.src, tex_velocity.dst);
    tex_velocity.swap();
    
 
    simulation_step++;
    
    context.end("Fluid.updateFluid");
  }
  
  
  
  
  




  
  private void createObstacleN(){                                         
    context.beginDraw(tex_obstacleN.dst);
    shader_obstacleBounds.begin();
    shader_obstacleBounds.uniform2f     ("wh_inv"       , 1.0f/fluid_w, 1.0f/fluid_h); 
    shader_obstacleBounds.uniformTexture("tex_obstacleC", tex_obstacleC.src);
    shader_obstacleBounds.drawFullScreenQuad();
    shader_obstacleBounds.end();
    context.endDraw();
    context.errorCheck("Fluid.createObstacleN");
    tex_obstacleN.swap();
  }
  
 
  private void advect(DwGLTexture tex_velocity, DwGLTexture tex_source, DwGLTexture tex_dst, float dissipation){
    context.beginDraw(tex_dst);
    shader_advect.begin();
    shader_advect.uniform2f     ("wh_inv"       , 1.0f/fluid_w, 1.0f/fluid_h); 
    shader_advect.uniform1f     ("timestep"     , param.timestep          );
    shader_advect.uniform1f     ("rdx"          , 1.0f / param.gridscale        ); 
    shader_advect.uniform1f     ("dissipation"  , dissipation             );
    shader_advect.uniformTexture("tex_velocity" , tex_velocity );
    shader_advect.uniformTexture("tex_source"   , tex_source   );
    shader_advect.uniformTexture("tex_obstacleC", tex_obstacleC.src);
    shader_advect.drawFullScreenQuad();
    shader_advect.end();
    context.endDraw();    
    context.errorCheck("Fluid.advect");
  }
  
  
  private void diffuse(TexturePingPong tex, float diffuse){
    if( diffuse <= 0.000001f)return;
    
    float alpha = (param.gridscale * param.gridscale) / (param.timestep * diffuse);
    float rBeta = 1.0f / (4.0f + alpha);
    
    for (int i = 0; i < param.num_jacobi_diffuse; ++i) {
      jacobi(tex.src, tex.src, tex.dst, alpha, rBeta);
      tex.swap();
    }
  }
  
  
  private void buoyancy(DwGLTexture tex_velocity, DwGLTexture tex_temperature, DwGLTexture tex_density, DwGLTexture tex_dst){
    context.beginDraw(tex_dst);
    shader_buoyancy.begin();
    shader_buoyancy.uniform2f     ("wh_inv"             , 1.0f/fluid_w, 1.0f/fluid_h ); 
    shader_buoyancy.uniform1f     ("temperature_ambient", param.temperature_ambient);
    shader_buoyancy.uniform1f     ("timestep"           , param.timestep           );           
    shader_buoyancy.uniform1f     ("fluid_buoyancy"     , param.fluid_buoyancy     );     
    shader_buoyancy.uniform1f     ("fluid_weight"       , param.fluid_weight       );       
    shader_buoyancy.uniformTexture("tex_velocity"       , tex_velocity   );
    shader_buoyancy.uniformTexture("tex_temperature"    , tex_temperature);
    shader_buoyancy.uniformTexture("tex_density"        , tex_density    );
    shader_buoyancy.drawFullScreenQuad();
    shader_buoyancy.end();
    context.endDraw();
    context.errorCheck("Fluid.buoyancy");
  }
  
  
  private void vorticity(DwGLTexture tex_velocity, DwGLTexture tex_dst, float vorticity){
    context.beginDraw(tex_curl);
    shader_vorticityCurl.begin();
    shader_vorticityCurl.uniform2f     ("wh_inv"       , 1.0f/fluid_w, 1.0f/fluid_h); 
    shader_vorticityCurl.uniform1f     ("halfrdx"      , 0.5f / param.gridscale        );
    shader_vorticityCurl.uniformTexture("tex_velocity" , tex_velocity );
    shader_vorticityCurl.uniformTexture("tex_obstacleC", tex_obstacleC.src);
    shader_vorticityCurl.drawFullScreenQuad();
    shader_vorticityCurl.end();
    context.endDraw();
    context.errorCheck("Fluid.vorticity - Curl");
    
    context.beginDraw(tex_dst);
    shader_vorticityForce.begin();
    shader_vorticityForce.uniform2f     ("wh_inv"      , 1.0f/fluid_w, 1.0f/fluid_h);
    shader_vorticityForce.uniform1f     ("halfrdx"     , 0.5f / param.gridscale        );       
    shader_vorticityForce.uniform1f     ("timestep"    , param.timestep          );         
    shader_vorticityForce.uniform1f     ("vorticity"   , vorticity               );              
    shader_vorticityForce.uniformTexture("tex_velocity", tex_velocity);
    shader_vorticityForce.uniformTexture("tex_curl"    , tex_curl    );
    shader_vorticityForce.drawFullScreenQuad();
    shader_vorticityForce.end();
    context.endDraw();
    context.errorCheck("Fluid.vorticity - Force");
  }
  
  
  private void divergence(DwGLTexture tex_velocity, DwGLTexture tex_dst){
    context.beginDraw(tex_dst);
    shader_divergence.begin();
    shader_divergence.uniform2f     ("wh_inv"       , 1.0f/fluid_w, 1.0f/fluid_h);
    shader_divergence.uniform1f     ("halfrdx"      , 0.5f / param.gridscale);
    shader_divergence.uniformTexture("tex_velocity" , tex_velocity );
    shader_divergence.uniformTexture("tex_obstacleC", tex_obstacleC.src);
    shader_divergence.uniformTexture("tex_obstacleN", tex_obstacleN.src);
    shader_divergence.drawFullScreenQuad();
    shader_divergence.end();
    context.endDraw();
    context.errorCheck("Fluid.divergence");
  }
  
  
  private void jacobiPressure(DwGLTexture tex_pressure, DwGLTexture tex_divergence, DwGLTexture tex_dst){
    float alpha = -(param.gridscale * param.gridscale);
    float rBeta = 0.25f;

    jacobi(tex_pressure, tex_divergence, tex_dst, alpha, rBeta);
  }
  
  
  private void jacobi(DwGLTexture tex_x, DwGLTexture tex_b, DwGLTexture tex_dst, float alpha, float rBeta){
    context.beginDraw(tex_dst);
    shader_jacobi.begin();
    shader_jacobi.uniform2f     ("wh_inv"       , 1.0f/fluid_w, 1.0f/fluid_h);
    shader_jacobi.uniform1f     ("alpha"        , alpha);
    shader_jacobi.uniform1f     ("rBeta"        , rBeta);
    shader_jacobi.uniformTexture("tex_x"        , tex_x);
    shader_jacobi.uniformTexture("tex_b"        , tex_b);
    shader_jacobi.uniformTexture("tex_obstacleC", tex_obstacleC.src);
    shader_jacobi.uniformTexture("tex_obstacleN", tex_obstacleN.src);
    shader_jacobi.drawFullScreenQuad();
    shader_jacobi.end();
    context.endDraw();
    context.errorCheck("Fluid.jacobi");
  }
  

  private void gradient(DwGLTexture tex_velocity, DwGLTexture tex_pressure,  DwGLTexture tex_dst){
    context.beginDraw(tex_dst);
    shader_gradient.begin();
    shader_gradient.uniform2f     ("wh_inv"       , 1.0f/fluid_w, 1.0f/fluid_h);
    shader_gradient.uniform1f     ("halfrdx"      , 1.0f / param.gridscale);
//    shader_gradient.uniform1f     ("halfrdx"      , 0.5f / param.gridscale);
    shader_gradient.uniformTexture("tex_velocity" , tex_velocity );
    shader_gradient.uniformTexture("tex_pressure" , tex_pressure );
    shader_gradient.uniformTexture("tex_obstacleC", tex_obstacleC.src);
    shader_gradient.uniformTexture("tex_obstacleN", tex_obstacleN.src);  
    shader_gradient.drawFullScreenQuad();
    shader_gradient.end();
    context.endDraw();
    context.errorCheck("Fluid.gradient");
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  

  //////////////////////////////////////////////////////////////////////////////
  // RENDER
  //////////////////////////////////////////////////////////////////////////////
  
  public void renderFluidTextures(PGraphics2D dst, int display_mode){
    
    int w = dst.width;
    int h = dst.height;
    
    context.begin();
    context.beginDraw(dst);
    blendMode();
    shader_renderFluid.begin();
    shader_renderFluid.uniform2f     ("wh"             , w, h); 
    shader_renderFluid.uniform1i     ("display_mode"   , display_mode);
    shader_renderFluid.uniformTexture("tex_density"    , tex_density    .src);
    shader_renderFluid.uniformTexture("tex_temperature", tex_temperature.src);
    shader_renderFluid.uniformTexture("tex_velocity"   , tex_velocity   .src);
    shader_renderFluid.uniformTexture("tex_pressure"   , tex_pressure   .src);
    shader_renderFluid.uniformTexture("tex_obstacleC"  , tex_obstacleC  .src);
    shader_renderFluid.uniformTexture("tex_obstacleN"  , tex_obstacleN  .src);
    shader_renderFluid.drawFullScreenQuad();
    shader_renderFluid.end();
    context.endDraw();
    context.end("Fluid.renderFluidTextures");
  }
  
  

  public void renderFluidVectors(PGraphics2D dst, int spacing){

    int w = dst.width;
    int h = dst.height;
    
    int   lines_x    = Math.round(w / spacing);
    int   lines_y    = Math.round(h / spacing);
    int   num_lines  = lines_x * lines_y;
    float space_x    = w / (float) lines_x;
    float space_y    = h / (float) lines_y;
    float scale      = (space_x + space_y) * 0.35f;
    float line_width = 1.0f;
    
    context.begin();
    context.beginDraw(dst);
    blendMode();
    shader_renderVelocityStreams.begin();
    shader_renderVelocityStreams.uniform2f     ("wh"            , w, h);
    shader_renderVelocityStreams.uniform1i     ("display_mode"  , 0); // 0 or 1
    shader_renderVelocityStreams.uniform2i     ("num_lines"     , lines_x, lines_y);
    shader_renderVelocityStreams.uniform2f     ("spacing"       , space_x, space_y);
    shader_renderVelocityStreams.uniform1f     ("velocity_scale", scale);
    shader_renderVelocityStreams.uniformTexture("tex_velocity"  , tex_velocity.src);
    shader_renderVelocityStreams.drawFullScreenLines(num_lines, line_width);
    shader_renderVelocityStreams.end();
    context.endDraw();
    context.end("Fluid.renderFluidVectors");
  }
  
  
  public void blendMode(){
    context.gl.glEnable(GL2.GL_BLEND);
    context.gl.glBlendEquationSeparate(GL2.GL_FUNC_ADD, GL2.GL_FUNC_ADD);
    context.gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA, GL2.GL_ONE, GL2.GL_ONE);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // DATA TRANSFER: OPENGL <-> HOST APPLICATION
  //////////////////////////////////////////////////////////////////////////////
  
  
  // Transfer velocity data from the GPU to the host-application
  // This is in general a bad idea because such operations are very slow. So 
  // either do everything in shaders, and avoid memory transfer when possible, 
  // or do it very rarely. however, this is just an example for convenience.
  

  // GPU_DATA_READ == 0 --> [x0, y0, x1, y1, ...]
  // GPU_DATA_READ == 1 --> [x0, y0, x1, y1, ...]
  public float[] getVelocity(float[] data_F4, int x, int y, int w, int h){
    return getVelocity(data_F4, x, y, w, h, 0);
  }
  
  public float[] getVelocity(float[] data_F4, int x, int y, int w, int h, int buffer_offset){
    context.begin();
    data_F4 = tex_velocity.src.getFloatTextureData(data_F4, x, y, w, h, buffer_offset);
    context.end("Fluid.getVelocity");
    return data_F4;
  }
  
  // GPU_DATA_READ == 0 --> [x0, y0, x1, y1, ...]
  // GPU_DATA_READ == 1 --> [x0, y0, x1, y1, ...]
  public float[] getVelocity(float[] data_F4){
    context.begin();
    data_F4 = tex_velocity.src.getFloatTextureData(data_F4);
    context.end("Fluid.getVelocity");
    return data_F4;
  }
  
  
  
  /**
   * example code for filling the pixels of a pgraphics object
   * 
   * <pre><code>
   * float[] px_density = Fluid.getDensity(null);                                           
   * PGraphics2D pg = (PGraphics2D) createGraphics(Fluid.fluid_w, Fluid.fluid_h, P2D);    
   * pg.loadPixels();                                                                      
   * for(int i = 0, ii = 0; i < pg.pixels.length; i++){                                           
   *   float x = px_density[ii++];                                                            
   *   float y = px_density[ii++];                                                         
   *   float z = px_density[ii++];                                                            
   *   float w = px_density[ii++];                                                            
   *   w = constrain(w, 0, 1);                                                                
   *   int r = (int)(255 * x / w);                                                            
   *   int g = (int)(255 * y / w);                                                            
   *   int b = (int)(255 * z / w);                                                            
   *   int a = (int)(255 *     w);                                                            
   *   pg.pixels[i] = a << 24 | r << 16 | g << 8 | b;                                         
   * }                                                                                        
   * pg.updatePixels();  
   * </code></pre>                                                                                                              
   *                                         
   * @param data_F4
   * @return
   */
  public float[] getDensity(float[] data_F4, int x, int y, int w, int h){
    return getDensity(data_F4, x, y, w, h, 0);
  }
  
  public float[] getDensity(float[] data_F4, int x, int y, int w, int h, int buffer_offset){
    context.begin();
    float[] data = tex_density.src.getFloatTextureData(data_F4, x, y, w, h, buffer_offset);
    context.end("Fluid.getDensity");
    return data;
  }
  
  public float[] getDensity(float[] data_F4){
    context.begin();
    float[] data = tex_density.src.getFloatTextureData(data_F4);
    context.end("Fluid.getDensity");
    return data;
  }
  
  
  
  
  
  
 
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // ADD FLUID DATA: density, velocity, temperature, obsatcles, ect...
  //
  // These methods should not be used that much since each call is a bit expensive.
  // Instead, use the code, modify it, and create your own addWhatever routines.
  // the library comes with examples on how to do that
  //////////////////////////////////////////////////////////////////////////////
  

  
  public void addVelocity(float px, float py, float radius, float vx, float vy){
    addVelocity(px, py, radius, vx, vy, 2, 0.5f);
  }
  
  public void addVelocity(float px, float py, float radius, float vx, float vy, int blend_mode, float mix){
    context.begin();
    context.beginDraw(tex_velocity.dst);
    DwGLSLProgram shader = shader_addVelocityBlob;
    shader.begin();
    shader.uniform2f     ("wh_src"       , this.viewp_w, this.viewp_h );                                     
    shader.uniform2f     ("wh_dst"       , this.fluid_w, this.fluid_h );                                     
    shader.uniform1i     ("blend_mode"   , blend_mode);   
    shader.uniform1f     ("mix_value"    , mix);     
    shader.uniform2f     ("data.pos"     , px, py);                              
    shader.uniform1f     ("data.rad"     , radius);                                              
    shader.uniform2f     ("data.velocity", vx, vy);
    shader.uniformTexture("tex_src"      , tex_velocity.src);
    shader.drawFullScreenQuad();
    shader.end();  
    context.endDraw();
    context.end("Fluid.addVelocity");
    tex_velocity.swap();
  }
  
  public void addDensity(float px, float py, float radius, float r, float g, float b, float intensity){
    addDensity(px, py, radius, r,g,b,intensity, 2);
  }
  

  
  public void addDensity(float px, float py, float radius, float r, float g, float b, float intensity, int blend_mode){
    context.begin();
    context.beginDraw(tex_density.dst);
    DwGLSLProgram shader = shader_addDensityBlob;
    shader.begin();
    shader.uniform2f     ("wh_src"       , this.viewp_w, this.viewp_h );                                     
    shader.uniform2f     ("wh_dst"       , this.fluid_w, this.fluid_h );                                                                             
    shader.uniform2f     ("data.pos"     , px, py);                              
    shader.uniform1f     ("data.rad"     , radius);                                              
    shader.uniform4f     ("data.density" , r, g, b, intensity);
    shader.uniform1i     ("blend_mode"   , blend_mode);      
    shader.uniformTexture("tex_src"      , tex_density.src);
    shader.drawFullScreenQuad();
    shader.end();  
    context.endDraw();
    context.end("Fluid.addDensity");
    tex_density.swap();
  }
  
  public void addTemperature(float px, float py, float radius, float temperature){
    context.begin();
    context.beginDraw(tex_temperature.dst);
    DwGLSLProgram shader = shader_addTemperatureBlob;
    shader.begin();
    shader.uniform2f     ("wh_src"          , this.viewp_w, this.viewp_h );                                     
    shader.uniform2f     ("wh_dst"          , this.fluid_w, this.fluid_h );                                                                                 
    shader.uniform2f     ("data.pos"        , px, py);                              
    shader.uniform1f     ("data.rad"        , radius);                                              
    shader.uniform1f     ("data.temperature", temperature);
    shader.uniformTexture("tex_src"         , tex_temperature.src);
    shader.drawFullScreenQuad();
    shader.end();  
    context.endDraw();
    context.end("Fluid.addTemperature");
    tex_temperature.swap();
  }
  
 
  public void addDensity(PGraphics2D pg, float intensity_scale, int blend_mode, float mix){
    Texture tex = pg.getTexture(); if(!tex.available()) return;
//    int[] pg_tex_handle = new int[1];
    context.begin();
//    context.getGLTextureHandle(pg, pg_tex_handle);
    context.beginDraw(tex_density.dst);
    DwGLSLProgram shader = shader_addDensityTexture;
    shader.begin();
    shader.uniform2f  ("wh"             , fluid_w, fluid_h); 
    shader.uniform1f  ("intensity_scale", intensity_scale); 
    shader.uniform1i  ("blend_mode"     , blend_mode); 
    shader.uniform1f  ("mix_value"      , mix); 
    shader.uniformTexture("tex_density_old", tex_density.src);
    shader.uniformTexture("tex_density_src", tex.glName);
    shader.drawFullScreenQuad();
    shader.end();
    context.endDraw();
    context.end("Fluid.addDensity");
    tex_density.swap();
  }
  
  
  public void addObstacles(PGraphics2D pg){
    Texture tex = pg.getTexture(); if(!tex.available()) return;
//    int[] pg_tex_handle = new int[1];
    context.begin();   
//    context.getGLTextureHandle(pg, pg_tex_handle);
    context.beginDraw(tex_obstacleC.dst);
    DwGLSLProgram shader = shader_addObstacleTexture;
    shader.begin();
    shader.uniform2f     ("wh"     , fluid_w, fluid_h); 
    shader.uniformTexture("tex_src", tex.glName);
    shader.drawFullScreenQuad();
    shader.end();
    context.endDraw();
    context.end("Fluid.addObstacles");
    tex_obstacleC.swap();
  }

  

  
  
  

  
  //////////////////////////////////////////////////////////////////////////////
  // Interfaces
  //////////////////////////////////////////////////////////////////////////////
  
  public void addCallback_FluiData(FluidData cb_fluid_data){
    CB_fluid_data = cb_fluid_data;
  }  
  
  public void addCallback_Advect(Advect cb_advect){
    CB_advect = cb_advect;
  }
  
  static public interface FluidData{
    public void update(DwFluid2D fluid);
  }
  static public interface Advect{
    public void update(DwFluid2D fluid);
  }
  
  
  
  
}
