/**
 * 
 * PixelFlow | Copyright (C) 2017 Thomas Diewald - www.thomasdiewald.com
 * 
 * https://github.com/diwi/PixelFlow.git
 * 
 * A Processing/Java library for high performance GPU-Computing.
 * MIT License: https://opensource.org/licenses/MIT
 * 
 */



package FlowFieldParticles_DevDemo;

import java.util.Locale;


import com.jogamp.opengl.GL3;
import com.thomasdiewald.pixelflow.java.DwPixelFlow;
import com.thomasdiewald.pixelflow.java.antialiasing.FXAA.FXAA;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTexture;
import com.thomasdiewald.pixelflow.java.dwgl.DwGLTextureUtils;
import com.thomasdiewald.pixelflow.java.flowfieldparticles.DwFlowFieldParticles;
import com.thomasdiewald.pixelflow.java.imageprocessing.DwFlowField;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwFilter;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.DwLiquidFX;
import com.thomasdiewald.pixelflow.java.imageprocessing.filter.Merge;
import com.thomasdiewald.pixelflow.java.utils.DwUtils;

import controlP5.Accordion;
import controlP5.CColor;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;


public class FlowFieldParticles_DevDemo extends PApplet {
  
  //
  //
  // GPU FlowFieldParticle DevDemo.
  //
  //
  // Verlet Particle Simulation based on FlowFields.
  // 
  //
  // ___ FLOW FIELDS / SDF (SIGNED DISTANCE FIELDS) ___
  // A FlowField is simply a velocity texture which is used for iteratively updating 
  // particle-positions.
  // E.g. Applying a Sobel-filter (x/y gradients) onto a SDF results in a flowfield.
  // 
  //
  // ___ ACCELERATION ___
  // This is probably the most common use for velocity textures in particle
  // simulations, ... using the velocity for the update step.
  //
  //
  // ___ COLLISION ___
  // Particle-Collisions can be solved in a very elegant way, by rendering particles
  // into a R32F texture using a distance-to-center shading function and additive
  // blending. In a next step a flowfield from this texture is used for updating 
  // particles position in the next update step.
  // This is key to simulate millions of particles on the GPU.
  // 
  //
  // ___ COHESION ___
  // Same as for the collisions I was a bit surprised how well it works for
  // simulation particle-to-particle attraction.
  // 
  //
  // ___ OBSTACLES ___
  // Handling obstacles is more or less the same as particle-to-particle interaction
  // is handled.
  // A distance transform step is applied in the scene obstacles (edges) to get 
  // a local distance field from which a flow field can be generated.
  // The resulting velocity texture is used for collision detection.
  //
  //
  // author: Thomas Diewald, (C) 2017
  //
  //

  boolean START_FULLSCREEN = !true;

  int viewport_w = 1680;
  int viewport_h = 1024;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = 30;
  int gui_y = 30;
  
  
  PGraphics2D pg_checker;
  PGraphics2D pg_canvas;
  PGraphics2D pg_obstacles;
  PGraphics2D pg_spheres;
  PGraphics2D pg_particles;
  PGraphics2D pg_sprite;
  PGraphics2D pg_gravity;
  PGraphics2D pg_impulse;
//  DwGLTexture tex_impulse = new DwGLTexture();;
  
  FXAA antialiasing;
  PGraphics2D pg_aa;
  
  DwPixelFlow context;
  
  DwFlowFieldParticles particles;
  DwFlowField ff_acc;
  DwFlowField ff_impulse;
  
  DwLiquidFX liquidfx;

  public boolean APPLY_LIQUID_FX   = false;
  public boolean UPDATE_SCENE      = true;
  public boolean APPLY_OBSTACLES   = true;
  public boolean AUTO_SPAWN        = true;
  public boolean UPDATE_GRAVITY    = true;
  public boolean DISPLAY_DIST      = !true;
  public boolean DISPLAY_FLOW      = !true;
  public int     DISPLAY_TYPE_ID   = 0;
  public int     BACKGROUND_MODE   = 0;
  public int     PARTICLE_COLOR    = 1;
  
  float gravity = 1;

  MouseObstacle[] mobs = new MouseObstacle[5];

  public void settings() {
    if(START_FULLSCREEN){
      viewport_w = displayWidth;
      viewport_h = displayHeight;
      viewport_x = 0;
      viewport_y = 0;
      fullScreen(P2D);
    } else {
      viewport_w = (int) min(viewport_w, displayWidth  * 0.9f);
      viewport_h = (int) min(viewport_h, displayHeight * 0.9f);
      size(viewport_w, viewport_h, P2D);
    }
    PJOGL.profile = 3;
    smooth(0);
  }
  

  public void setup(){
    surface.setLocation(viewport_x, viewport_y);
    surface.setResizable(true);
    randomSeed(2);
    for(int i = 0; i < mobs.length; i++){
      float r = random(50,120);
      mobs[i] = new MouseObstacle(i, random(r, width-r), random(r,height-r), r);
    }

    context = new DwPixelFlow(this);
    context.print();
    context.printGL();

    antialiasing = new FXAA(context);
    
    liquidfx = new DwLiquidFX(context);
    
    particles = new DwFlowFieldParticles(context, 1024 * 1024 * 4);
    particles.param.size_display   = 10;
    particles.param.size_collision = particles.param.size_display;
    particles.param.size_cohesion  = 5;
    particles.param.velocity_damping  = 0.99f;
    
    ff_acc = new DwFlowField(context);
    ff_acc.param.blur_iterations = 0;
    ff_acc.param.blur_radius     = 1;
    
    
    ff_impulse = new DwFlowField(context);
    ff_impulse.param.blur_iterations = 1;
    ff_impulse.param.blur_radius     = 1;
    
    resizeScene();
    
    createSprite();
    
    createGUI();
    
    frameRate(1000);
  }
  


  public void resizeScene(){
    
    if(pg_canvas != null && width == pg_canvas.width && height == pg_canvas.height){
      return;
    }
    
    pg_aa = (PGraphics2D) createGraphics(width, height, P2D);
    pg_aa.smooth(0);
    pg_aa.textureSampling(5);
    
    pg_checker = (PGraphics2D) createGraphics(width, height, P2D);
    pg_checker.smooth(0);
    
    pg_canvas = (PGraphics2D) createGraphics(width, height, P2D);
    pg_canvas.smooth(0);
    
    pg_obstacles = (PGraphics2D) createGraphics(width, height, P2D);
    pg_obstacles.smooth(0);
    
    pg_spheres = (PGraphics2D) createGraphics(width, height, P2D);
    pg_spheres.smooth(0);
    
    pg_particles = (PGraphics2D) createGraphics(width, height, P2D);
    pg_particles.smooth(0);
    DwGLTextureUtils.changeTextureFormat(pg_particles, GL3.GL_RGBA16_SNORM, GL3.GL_RGBA, GL3.GL_FLOAT);

    pg_impulse = (PGraphics2D) createGraphics(width, height, P2D);
    pg_impulse.smooth(0);

    pg_gravity = (PGraphics2D) createGraphics(width, height, P2D);
    pg_gravity.smooth(0);
    
    pg_gravity.beginDraw();
    pg_gravity.blendMode(REPLACE);
    pg_gravity.background(0, 255, 0);
    pg_gravity.endDraw();
    
    setParticleColor(PARTICLE_COLOR);
    setCheckerboardBG(BACKGROUND_MODE);
  }
  

  
  


  
  //////////////////////////////////////////////////////////////////////////////
  //
  // DRAW
  //
  //////////////////////////////////////////////////////////////////////////////
  
  float impulse_max = 256;
  float impulse_mul = 15;
  float impulse_tsmooth = 0.90f;
  int   impulse_blur  = 0;
  
  public void addImpulse(){
    
    // impulse center/velocity
    float mx = mouseX;
    float my = mouseY;
    float vx = (mouseX - pmouseX) * +impulse_mul;
    float vy = (mouseY - pmouseY) * -impulse_mul; // flip vertically
    // clamp velocity
    float vv_sq = vx*vx + vy*vy;
    float vv_sq_max = impulse_max*impulse_max;
    if(vv_sq > vv_sq_max){
      vx = impulse_max * vx / sqrt(vv_sq);
      vy = impulse_max * vy / sqrt(vv_sq);
    }
    // map velocity, to UNSIGNED_BYTE range
    final int mid = 127;
    vx = map(vx, -impulse_max, +impulse_max, 0, mid<<1);
    vy = map(vy, -impulse_max, +impulse_max, 0, mid<<1);
    // render "velocity"
    pg_impulse.beginDraw();
    pg_impulse.background(mid, mid, mid);
    pg_impulse.noStroke();
    if(mousePressed){
      pg_impulse.fill(vx, vy, mid);
      pg_impulse.ellipse(mx, my, 100, 100);
    }
    pg_impulse.endDraw();
    
    // create impulse texture
    ff_impulse.resize(width, height);
    {
      Merge.TexMad ta = new Merge.TexMad(ff_impulse.tex_vel, impulse_tsmooth, 0);
      Merge.TexMad tb = new Merge.TexMad(pg_impulse,  1, -mid/255f);
      DwFilter.get(context).merge.apply(ff_impulse.tex_vel, ta, tb);
      ff_impulse.blur(1, impulse_blur);
    }
  }
  
  
  
  
  public void particleSimulation(){

    int w = width;
    int h = height;

    // create acceleration texture
    ff_acc.resize(w, h);
    {
      float mul_gravity = UPDATE_GRAVITY ? -gravity/10f : 0;
      Merge.TexMad ta = new Merge.TexMad(ff_impulse.tex_vel, 1, 0);
      Merge.TexMad tb = new Merge.TexMad(pg_gravity, mul_gravity, 0);
      DwFilter.get(context).merge.apply(ff_acc.tex_vel, ta, tb);
    }

    // resize, create obstacles, update physics
    particles.resizeWorld(w, h);
    particles.createObstacleFlowField(pg_obstacles, BG, true);
    particles.update(ff_acc);
  }
  

  public void draw(){
    
    resizeScene();
    
    updateScene();
    
    autoSpawnParticles();

    addImpulse();
    
    particleSimulation();
  

    if(!DISPLAY_DIST)
    {
      if(DISPLAY_TYPE_ID == 0){
        // set pg_checker as background for blending
        DwFilter.get(context).copy.apply(pg_checker, pg_particles);
        particles.display(pg_particles);
      } else {
        float mult = 0.985f;
        DwFilter.get(context).multiply.apply(pg_particles, pg_particles, new float[]{mult, mult, mult, mult});
        particles.displayTrail(pg_particles);
      }
      
      if(APPLY_LIQUID_FX){
        liquidfx.param.base_LoD           = 1;
        liquidfx.param.base_blur_radius   = 1;
        liquidfx.param.base_threshold     = 0.6f;
        liquidfx.param.base_threshold_pow = 25;
        liquidfx.param.highlight_enabled  = true;
        liquidfx.param.highlight_LoD      = 1;
        liquidfx.param.highlight_decay    = 0.6f;
        liquidfx.param.sss_enabled        = true;
        liquidfx.param.sss_LoD            = 3;
        liquidfx.param.sss_decay          = 0.8f;
        liquidfx.apply(pg_particles);
      }

      pg_canvas.beginDraw(); 
      pg_canvas.blendMode(REPLACE);
      pg_canvas.image(pg_checker, 0, 0);
      pg_canvas.blendMode(BLEND);   
      pg_canvas.image(pg_obstacles, 0, 0);
      pg_canvas.image(pg_spheres, 0, 0);
      pg_canvas.image(pg_particles, 0, 0);
      pg_canvas.endDraw();
    }
    
    
    if(DISPLAY_DIST){
      int Z = DwGLTexture.SWIZZLE_0;
      int R = DwGLTexture.SWIZZLE_R;
      int G = DwGLTexture.SWIZZLE_G;
      int B = DwGLTexture.SWIZZLE_B;
      int A = DwGLTexture.SWIZZLE_A;
      int[] RGBA = {R,G,B,A};
      
      Merge.TexMad texA = new Merge.TexMad(particles.tex_obs_dist, 0.030f * particles.param.mul_obs, 0.0f);
      Merge.TexMad texB = new Merge.TexMad(particles.tex_col_dist, 0.500f * particles.param.mul_col, 0.0f);
      Merge.TexMad texC = new Merge.TexMad(particles.tex_coh_dist, 0.005f * particles.param.mul_coh, 0.0f);

      particles.tex_obs_dist.swizzle(new int[]{R, R, R, Z});
      particles.tex_col_dist.swizzle(new int[]{R, R, R, Z});
      particles.tex_coh_dist.swizzle(new int[]{R, Z, Z, Z});
      
      DwFilter.get(context).merge.apply(pg_canvas, texA, texB, texC);
      
      particles.tex_coh_dist.swizzle(RGBA);
      particles.tex_col_dist.swizzle(RGBA);
      particles.tex_obs_dist.swizzle(RGBA);
    }

    if(DISPLAY_FLOW){
      particles.ff_sum.param.line_spacing = 8;
      particles.ff_sum.param.line_width   = 0.8f;
      particles.ff_sum.param.line_scale   = 1.5f;
      particles.ff_sum.param.line_shading = 0;
      particles.ff_sum.displayPixel(pg_canvas);
      particles.ff_sum.displayLines(pg_canvas);
    }

    antialiasing.apply(pg_canvas, pg_aa);

    blendMode(REPLACE);
    image(pg_aa, 0, 0);
    blendMode(BLEND);
    
    info();
  }
  

  void info(){
    String txt_device = context.gl.glGetString(GL3.GL_RENDERER).trim().split("/")[0];
    String txt_app = getClass().getSimpleName();
    String txt_fps = String.format(Locale.ENGLISH, "[%s]   [%s]   [%d/%d]   [%7.2f fps]   [particles %,d] ", 
        txt_app, txt_device, 
        pg_canvas.width, 
        pg_canvas.height, 
        frameRate, particles.getCount()
        );

    fill(col_fg);
    noStroke();
    rect(0, height, 650, - 20);
    fill(255,128,0);
    text(txt_fps, 10, height-6);
    
    surface.setTitle(txt_fps);
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  //
  // SCENE
  //
  //////////////////////////////////////////////////////////////////////////////


  int[] BG      = { 0, 0, 0,  0};
  int[] FG      = {16,16,16,255};
  int[] FG_MOBS = {32,32,32,255};
  void setFill(PGraphicsOpenGL pg, int[] rgba){
    pg.fill(rgba[0], rgba[1], rgba[2], rgba[3]);
  }

  float rot   = 0;
  float slide = 0;
  
  void updateScene(){
    
    
    pg_spheres.beginDraw();
    pg_spheres.clear();
    pg_spheres.noStroke();
    pg_spheres.blendMode(BLEND);
    for(int i = 0; i < mobs.length; i++){
      mobs[i].draw(pg_spheres, FG_MOBS);
    }
    pg_spheres.endDraw();
    
   
    int w = pg_obstacles.width;
    int h = pg_obstacles.height;
    float wh = w/2f;
    // float hh = h/2f;
    
    pg_obstacles.beginDraw();
    pg_obstacles.clear();
    pg_obstacles.noStroke();
    pg_obstacles.blendMode(REPLACE);
    pg_obstacles.rectMode(CORNER);

    setFill(pg_obstacles, FG);
    pg_obstacles.rect(0, 0, w, h);
    setFill(pg_obstacles, BG);
    pg_obstacles.rect(25, 25, w-50, h-50);
      
    if(APPLY_OBSTACLES)
    {  
      if(UPDATE_SCENE){
        rot += 0.005f;
        slide += 0.004f;
      }

      pg_obstacles.rectMode(CENTER);
      int count = 10;
      float dy = h / (float) count;
      for(int i = 0; i < count; i++){
        float py = dy * 0.5f + i*dy;
        setFill(pg_obstacles, FG);
        pg_obstacles.rect(w-w/4f, py, 50, 50, 15);
  //      pg_obstacles.rect(  w/4f, py, 50, 50, 15);
      }
      
      pg_obstacles.pushMatrix();
      {
        float px = w/2 + sin(slide) * (4 * w/5f) * 0.5f;
        pg_obstacles.translate(px, h-250);
        setFill(pg_obstacles, FG);
        pg_obstacles.rect(0, 0, 50, 500);
      }
      pg_obstacles.popMatrix();
      
      pg_obstacles.pushMatrix();
      {
        pg_obstacles.translate(2 * w/7f, h/4f);
        pg_obstacles.rotate(sin(rot));
        setFill(pg_obstacles, FG);
        pg_obstacles.rect(0, 0, 400, 300, 50);
        setFill(pg_obstacles, BG);
        pg_obstacles.rect(0, 0, 350, 250, 25);
        pg_obstacles.rect(0, -70, 402, 40);
      }
      pg_obstacles.popMatrix();
  
      pg_obstacles.pushMatrix();
      {
        float dim = 2 * h/3f;
        pg_obstacles.translate(wh, h-dim/2);
        pg_obstacles.rotate(rot);
        setFill(pg_obstacles, FG);
        pg_obstacles.rect(0, 0, dim,  50);
        pg_obstacles.rect(0, 0,  50, dim, 10);
        setFill(pg_obstacles, BG);
        pg_obstacles.rect(0, 0,  100, 100);
      }
      pg_obstacles.popMatrix();
      
    }
    
    pg_obstacles.blendMode(BLEND);
//    for(int i = 0; i < mobs.length; i++){
//      mobs[i].draw(pg_obstacles, FG_MOBS);
//    }
    
    pg_obstacles.image(pg_spheres, 0, 0);
    
    pg_obstacles.endDraw();
    
  }
  

  
 
  public void autoSpawnParticles(){
    if(AUTO_SPAWN){
      float px = 2 * width/7f - 100;
      float py = height/4f;
      
      DwFlowFieldParticles.SpawnRadial sr = new DwFlowFieldParticles.SpawnRadial();
      sr.num(1);
      sr.dim(10, 10);
      sr.pos(px, height-1 - py);
      sr.vel(4, 4);
      particles.spawn(width, height, sr);

    }
    
    boolean IS_GUI = cp5.isMouseOver();
    
    if(!IS_GUI && mousePressed){     
      if(mouseButton == LEFT){
        int count = ceil(particles.getCount() * 0.01f);
        count = min(max(count, 1), 50000);  
        int radius = ceil(sqrt(count));
        spawn(radius, count);
      }
    }
   
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  //
  // GUI
  // Helper
  // Parameters
  // Interaction (Mouse, Keys)
  //
  //////////////////////////////////////////////////////////////////////////////
  
  static class MouseObstacle{
    int idx = 0;
    float px = 500;
    float py = 200;
    float r  = 60;
    float dx, dy;
    boolean moving = false;
    
    public MouseObstacle(int idx, float px, float py, float r){
      this.idx = idx;
      this.px = px;
      this.py = py;
      this.r = r;
    }
    void draw(PGraphics pg, int[] rgba){
      
      int cr = rgba[0]+2*(idx);
      int cg = rgba[1]+2*(idx);
      int cb = rgba[2]+2*(idx);
      int ca = 196;
      
      pg.noStroke();
      pg.fill(cr,cg,cb,ca);
      pg.ellipse(px, py, r*2, r*2);
    }
    public boolean inside(float mx, float my){
      dx = px - mx;
      dy = py - my;
      return (dx*dx + dy*dy) < (r*r);
    } 
    public void startMove(float mx, float my){
      moving = inside(mx, my);
    }
    public void move(float mx, float my){
      if(moving){
        px = mx + dx;
        py = my + dy;
      }
    }
    public void endMove(float mx, float my){
      moving = false;
    }
  }
  
  public void mousePressed(){
    if(mouseButton == RIGHT && !cp5.isMouseOver()){
      for(int i = 0; i < mobs.length; i++){
        mobs[i].startMove(mouseX, mouseY);
      }
    }
  }

  public void mouseDragged(){
    for(int i = 0; i < mobs.length; i++){
      mobs[i].move(mouseX, mouseY);
    }
  }
  
  public void mouseReleased(){
    for(int i = 0; i < mobs.length; i++){
      mobs[i].endMove(mouseX, mouseY);
    }
  }
  
  
  public void keyReleased(){
    if(key == 'r') reset();
    if(key == 'w') UPDATE_GRAVITY = !UPDATE_GRAVITY;
    if(key == 'e') UPDATE_SCENE = !UPDATE_SCENE;
    if(key == 'f') DISPLAY_FLOW = !DISPLAY_FLOW;
    if(key == 'd') DISPLAY_DIST = !DISPLAY_DIST;
//    if(key >= '1' && key <= '9') DISPLAY_ID = key - '1';
    if(key == 'h') toggleGUI(); 
  }
  
  public void toggleGUI(){
    if(cp5.isVisible()){
      cp5.hide();
    } else {
      cp5.show();
    }
  }
  
  
  public void spawn(int rad, int count){
    int vw = width;
    int vh = height;
    int px = mouseX;
    int py = mouseY; py = vh - 1 - py;
    
    DwFlowFieldParticles.SpawnRadial sr = new DwFlowFieldParticles.SpawnRadial();
    sr.num(count);
    sr.dim(rad, rad);
    sr.pos(px, py);
    
    particles.spawn(vw, vh, sr);
  }
  

  public void reset(){
    particles.reset();
  }

  public void set_size_display(float val){
    particles.param.size_display = val;
  }

  public void set_size_cohesion(float val){
    particles.param.size_cohesion = val;  
  }
  
  public void set_size_collision(float val){
    particles.param.size_collision = val;  
  }
  
  public void set_velocity_damping(float val){
    particles.param.velocity_damping = val;  
  }

  public void set_gravity(float val){
    gravity = val;
  }
  
  public void set_collision_steps(int val){
    particles.param.steps = val;
  }
  
  public void set_mul_acc(float val){
    particles.param.mul_acc = val;
  }
  public void set_mul_col(float val){
    particles.param.mul_col = val;
  }
  public void set_mul_coh(float val){
    particles.param.mul_coh = val;
  }
  public void set_mul_obs(float val){
    particles.param.mul_obs = val;
  }
  
  public void set_shader_collision_mult(float val){
    particles.param.shader_collision_mult = val;
  }

  

  static class SpriteParam {
    public int   size = 64;
    public float e1   = 2f;
    public float e2   = 1f;
    public float mult = 1f;
  }
  
  SpriteParam sprite_param = new SpriteParam();
  
  public void createSprite(){
    int size = sprite_param.size;
    particles.createSpriteTexture(size, sprite_param.e1, sprite_param.e2, sprite_param.mult);
   
    if(pg_sprite == null){
      int wh = gui_w-30;
      pg_sprite = (PGraphics2D) createGraphics(wh, wh, P2D);
      pg_sprite.smooth(0);
    }
    DwFilter.get(context).copy.apply(particles.param.tex_sprite, pg_sprite);
  }
  
  public void set_sprite_param_size(int val){
    sprite_param.size = val;
    createSprite();
  }
  public void set_sprite_param_e1(float val){
    sprite_param.e1 = val;
    createSprite();
  }
  public void set_sprite_param_e2(float val){
    sprite_param.e2 = val;
    createSprite();
  }
  public void set_sprite_param_mult(float val){
    sprite_param.mult = val;
    createSprite();
  }
  
  public void setParticleColor(int val){
    float r=1f, g=1f, b=1f, a=1f, s=1f;
    
    float[] ca = particles.param.col_A;
    
    switch(val){
      case 0: r = 0.10f; g = 0.50f; b = 1.00f; a = 10.0f; s = 0.50f;  break;
      case 1: r = 0.40f; g = 0.80f; b = 0.10f; a = 10.0f; s = 0.50f;  break;
      case 2: r = 0.80f; g = 0.40f; b = 0.10f; a = 10.0f; s = 0.50f;  break;
      case 3: r = 0.50f; g = 0.50f; b = 0.50f; a = 10.0f; s = 0.25f;  break;
      case 4: r = ca[0]; g = ca[1]; b = ca[2]; a =  1.0f; s = 1.00f;  break;
    }

    particles.param.col_A = new float[]{ r  , g  , b  , a };
    particles.param.col_B = new float[]{ r*s, g*s, b*s, 0 };
  }
  
  public void updateSelections(float[] val){
    int ID = 0;
    DISPLAY_DIST        = val[ID++] > 0;
    DISPLAY_FLOW        = val[ID++] > 0;
    UPDATE_GRAVITY      = val[ID++] > 0;
    UPDATE_SCENE        = val[ID++] > 0;
    AUTO_SPAWN          = val[ID++] > 0;
    APPLY_LIQUID_FX     = val[ID++] > 0;
    APPLY_OBSTACLES     = val[ID++] > 0;
  }
  
  public void setDisplayType(int val){
    DISPLAY_TYPE_ID = val;
  }

  public void setCheckerboardBG(int val){
    BACKGROUND_MODE = val;
    switch(BACKGROUND_MODE){ 
      case 0: pg_checker = DwUtils.createCheckerBoard(this, width, height, 128, color( 96, 0), color( 48, 0)); break;
      case 1: pg_checker = DwUtils.createCheckerBoard(this, width, height, 128, color(208, 0), color(160, 0)); break;
      case 2: pg_checker = DwUtils.createCheckerBoard(this, width, height, 128, color(208, 0), color( 48, 0)); break;
      case 3: pg_checker.beginDraw();  pg_checker.background(  0, 0); pg_checker.endDraw(); break;
      case 4: pg_checker.beginDraw();  pg_checker.background(255, 0); pg_checker.endDraw(); break;
    }
  }
  
  float mult_fg = 1f;
  float mult_active = 2f;
  float CR = 96;
  float CG = 16;
  float CB =  0;
  
  int col_bg    ;
  int col_fg    ;
  int col_active;
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    
    col_bg     = color(16);
    col_fg     = color(CR*mult_fg, CG*mult_fg, CB*mult_fg);
    col_active = color(CR*mult_active, CG*mult_active, CB*mult_active);
    
    int col_group = color(8,224);
    
    CColor theme = ControlP5.getColor();
    theme.setForeground(col_fg);
    theme.setBackground(col_bg);
    theme.setActive(col_active);

    cp5 = new ControlP5(this);
    cp5.setAutoDraw(true);
    
    int sx, sy, px, py;
    sx = 100; 
    sy = 14; 

    int dy_group = 20;
    int dy_item = 4;
    ////////////////////////////////////////////////////////////////////////////
    // GUI - FLUID
    ////////////////////////////////////////////////////////////////////////////
    Group group_particles = cp5.addGroup("particles");
    {
      group_particles.setHeight(20).setSize(gui_w, 450)
      .setBackgroundColor(col_group).setColorBackground(col_group);
      group_particles.getCaptionLabel().align(CENTER, CENTER);
      
      px = 15; py = 15;
      
      cp5.addButton("reset").setGroup(group_particles).plugTo(this, "reset"     ).setSize(80, 18).setPosition(px, py);

      
      {
        py += 1 * sy + dy_group;
        int count = 2;
        sx = (gui_w-30 - 2 * (count-1)) / count;
        RadioButton rb_type = cp5.addRadio("setDisplayType").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(count).plugTo(this, "setDisplayType")
          .setNoneSelectedAllowed(false)
          .addItem("Particles", 0)
          .addItem("Trails"   , 1)
          .activate(DISPLAY_TYPE_ID);
  
        for(Toggle toggle : rb_type.getItems()) toggle.getCaptionLabel().alignX(CENTER).alignY(CENTER);
        py += sy + dy_group;
      }

      cp5.addSlider("collision steps").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 3).setValue(particles.param.steps).plugTo(this, "set_collision_steps")
      .snapToTickMarks(true).setNumberOfTickMarks(4);
      py += sy + dy_group;
      
      cp5.addSlider("gravity").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(0f, 2f).setValue(gravity).plugTo(this, "set_gravity");
      py += sy + dy_item;
      
      cp5.addSlider("damping").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(0.95f, 1.00f).setValue(particles.param.velocity_damping).plugTo(this, "set_velocity_damping");
      py += sy + dy_group;

      
      cp5.addSlider("size display").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 50).setValue(particles.param.size_display).plugTo(this, "set_size_display");
      py += sy + dy_item;
      
      cp5.addSlider("size collision").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(1, 50).setValue(particles.param.size_collision).plugTo(this, "set_size_collision");
      py += sy + dy_item;
      
      cp5.addSlider("size cohesion").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(1, 50).setValue(particles.param.size_cohesion).plugTo(this, "set_size_cohesion");
      py += sy + dy_group;
      

      cp5.addSlider("mult collision").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(0.0f, 8.0f).setValue(particles.param.mul_col).plugTo(this, "set_mul_col");
      py += sy + dy_item;
      
      cp5.addSlider("mult cohesion").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(0.0f, 8.0f).setValue(particles.param.mul_coh).plugTo(this, "set_mul_coh");
      py += sy + dy_item;
      
      cp5.addSlider("mult obstacles").setGroup(group_particles).setSize(sx, sy).setPosition(px, py)
      .setRange(0.0f, 8.0f).setValue(particles.param.mul_obs).plugTo(this, "set_mul_obs");
      py += sy + dy_group;
      

      int ID = -1;
      cp5.addCheckBox("updateSelections").setGroup(group_particles).setSize(sy,sy).setPosition(px, py)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("DISPLAY DIST"       , ++ID).activate(DISPLAY_DIST        ? ID : 10)
          .addItem("DISPLAY FLOW"       , ++ID).activate(DISPLAY_FLOW        ? ID : 10)
          .addItem("UPDATE GRAVITY"     , ++ID).activate(UPDATE_GRAVITY      ? ID : 10)
          .addItem("UPDATE SCENE"       , ++ID).activate(UPDATE_SCENE        ? ID : 10)
          .addItem("AUTO SPAWN"         , ++ID).activate(AUTO_SPAWN          ? ID : 10)
          .addItem("APPLY LIQUID FX"    , ++ID).activate(APPLY_LIQUID_FX     ? ID : 10)
          .addItem("APPLY OBSTACLES"    , ++ID).activate(APPLY_OBSTACLES     ? ID : 10)
        ; 
    }
    
    
    
    Group group_sprite = cp5.addGroup("sprite");
    {
      group_sprite.setHeight(20).setSize(gui_w, 380)
      .setBackgroundColor(col_group).setColorBackground(col_group);
      group_sprite.getCaptionLabel().align(CENTER, CENTER);
      
      px = 15; py = 15;

      cp5.addSlider("size").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py)
      .setRange(32, 128).setValue(sprite_param.size).plugTo(this, "set_sprite_param_size");
      py += sy + dy_item;

      cp5.addSlider("exp1").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 4).setValue(sprite_param.e1).plugTo(this, "set_sprite_param_e1");
      py += sy + dy_item;
      
      cp5.addSlider("exp2").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 4).setValue(sprite_param.e2).plugTo(this, "set_sprite_param_e2");
      py += sy + dy_item;
      
      cp5.addSlider("mult").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 4).setValue(sprite_param.mult).plugTo(this, "set_sprite_param_mult");
      py += sy + dy_group;
      
      cp5.addButton("sprite_img").setGroup(group_sprite).setPosition(px, py).setImage(pg_sprite).updateSize();
      
      
      py += gui_w-30 + dy_group;
      int count = 5;
      sx = (gui_w-30 - 2 * (count-1)) / count;
      RadioButton rb_colors = cp5.addRadio("setParticleColor").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(5).plugTo(this, "setParticleColor")
        .setNoneSelectedAllowed(false)
        .addItem("BLUE"   , 0)
        .addItem("GREEN"  , 1)
        .addItem("ORANGE" , 2)
        .addItem("GRAY"   , 3)
        .addItem("MONO"   , 4)
        .activate(PARTICLE_COLOR);

      for(Toggle toggle : rb_colors.getItems()) toggle.getCaptionLabel().alignX(CENTER).alignY(CENTER);
      

      py += sy + dy_item;
      count = 5;
      sx = (gui_w-30 - 2 * (count-1)) / count;
      RadioButton rb_bg = cp5.addRadio("setCheckerboardBG").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(count).plugTo(this, "setCheckerboardBG")
        .setNoneSelectedAllowed(false)
        .addItem("DARK"    , 0)
        .addItem("LIGHT"   , 1)
        .addItem("BW"      , 2)
        .addItem("BLACK"   , 3)
        .addItem("WHITE"   , 4)
        .activate(BACKGROUND_MODE);

      for(Toggle toggle : rb_bg.getItems()) toggle.getCaptionLabel().alignX(CENTER).alignY(CENTER);
      
      py += sy + dy_item;
      sx = 100;
      cp5.addSlider("collision_mult").setGroup(group_sprite).setSize(sx, sy).setPosition(px, py)
      .setRange(0, 1).setValue(particles.param.shader_collision_mult).plugTo(this, "set_shader_collision_mult")
      ;
      
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_particles)
      .addItem(group_sprite   )
      .open()
      ;

  }
  
  


  public static void main(String args[]) {
    PApplet.main(new String[] { FlowFieldParticles_DevDemo.class.getName() });
  }
  
  
}