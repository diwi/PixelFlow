package tests.streamlines;



import com.thomasdiewald.pixelflow.src.Fluid;
import com.thomasdiewald.pixelflow.src.PixelFlow;

import controlP5.Accordion;
import controlP5.Button;
import controlP5.ControlP5;
import controlP5.Group;
import controlP5.RadioButton;
import controlP5.Toggle;
import processing.core.*;
import processing.opengl.PGraphics2D;



public class Main_Streamlines extends PApplet {
  
  private class MyFluidData implements Fluid.FluidData{
    
    // update() is called during the fluid-simulation update step.
    @Override
    public void update(Fluid fluid) {
    
      float px, py, vx, vy, radius, vscale, r, g, b, intensity, temperature;
      
      // add impulse: density + temperature
      px = width-80;
      py = 30;
      radius = 60;
      r = 0.1f;
      g = 0.4f;
      b = 1.0f;
      vx = 0;
      vy = 50;
      intensity = 1.0f;
      fluid.addDensity(px, py, radius, r, g, b, intensity);
      temperature = 4;
      fluid.addTemperature(px, py, radius, temperature);
      
      // add impulse: density + velocit
      px = width/2;
      py = height/2;
      radius = 15;
      r = 1.0f;
      g = 0;
      b = 0.4f;
      vx = 0;
      vy = -50;
      fluid.addDensity(px, py, radius, r, g, b, intensity);
      temperature = -4;
      fluid.addVelocity(px, py, radius, vx, vy);
      

      boolean mouse_input = !cp5.isMouseOver() && mousePressed;
      
      // add impulse: density + velocity
      if(mouse_input && mouseButton == LEFT){
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        radius = 8;
        fluid.addDensity(px, py, radius, 1, 1, 1f, 1.0f);
        radius = 15;
        fluid.addVelocity(px, py, radius, vx, vy);
      }
      
      // add impulse: density + velocity
      if(mouse_input && mouseButton == RIGHT){
        vscale = 15;
        px     = mouseX;
        py     = height-mouseY;
        vx     = (mouseX - pmouseX) * +vscale;
        vy     = (mouseY - pmouseY) * -vscale;
        radius = 60;
        fluid.addDensity(px, py, radius, 1, 0.4f, 0f, 1f, 1);
        radius = 60;
        temperature = 5;
        fluid.addTemperature(px, py, radius, temperature);
      }
     
    }
  }
  
  
  int viewport_w = 800;
  int viewport_h = 800;
  int fluidgrid_scale = 1;
  
  int BACKGROUND_COLOR = 0;
  
  // Fluid simulation
  public Fluid fluid;

  // render targets
  PGraphics2D pg_fluid;
  //texture-buffer, for adding obstacles
  PGraphics2D pg_obstacles;


  public void settings() {
    size(viewport_w, viewport_h, P2D);
    smooth(4);
  }
  
  public void setup() {
    
    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
    context.printGL();
    
    
    // fluid simulation
    fluid = new Fluid(context, viewport_w, viewport_h, fluidgrid_scale);
    
    
    // set some simulation parameters
    fluid.param.dissipation_density     = 0.999f;
    fluid.param.dissipation_velocity    = 0.99f;
    fluid.param.dissipation_temperature = 0.50f;
    fluid.param.vorticity               = 0.10f;
    
    
    // interface for adding data to the fluid simulation
    MyFluidData cb_fluid_data = new MyFluidData();
    fluid.addCallback_FluiData(cb_fluid_data);
   
    
    // pgraphics for fluid
    pg_fluid = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_fluid.smooth(4);
    
    
    // pgraphics for obstacles
    pg_obstacles = (PGraphics2D) createGraphics(viewport_w, viewport_h, P2D);
    pg_obstacles.noSmooth();
    pg_obstacles.beginDraw();
    pg_obstacles.clear();

    // border-obstacle
    pg_obstacles.strokeWeight(20);
    pg_obstacles.stroke(64);
    pg_obstacles.noFill();
    pg_obstacles.rect(0, 0, pg_obstacles.width, pg_obstacles.height);
    pg_obstacles.endDraw();
    
    fluid.addObstacles(pg_obstacles);

    createGUI();
    
    frameRate(60);
  }
  
  
  
  // float buffer for pixel transfer from OpenGL to the host application
  float[] fluid_velocity;

 
  public void draw() {    

    // update simulation
    if(UPDATE_FLUID){
      fluid.update();
    }
    
    // clear render target
    pg_fluid.beginDraw();
    pg_fluid.background(BACKGROUND_COLOR);
    pg_fluid.endDraw();
    
    // render fluid stuff
    if(DISPLAY_FLUID_TEXTURES){
      // render: density (0), temperature (1), pressure (2), velocity (3)
      fluid.renderFluidTextures(pg_fluid, DISPLAY_fluid_texture_mode);
    }
    
    if(DISPLAY_FLUID_VECTORS){
      // render: velocity vector field
      fluid.renderFluidVectors(pg_fluid, 10);
    }
  
    // Transfer velocity data from the GPU to the host-application
    // This is in general a bad idea because such operations are very slow. So 
    // either do everything in shaders, and avoid memory transfer when possible, 
    // or do it very rarely. however, this is just an example for convenience.
    fluid_velocity = fluid.getVelocity(fluid_velocity);
   
      

    int px = mouseX;
    int py = mouseY;

    
    startCurveAt(px, py);
    
    
    int num_curves_x = 20;
    int num_curves_y = 20;
    
    int spacex = viewport_w /(num_curves_x+1);
    int spacey = viewport_h /(num_curves_y+1);
    
    for(int y = 0; y < num_curves_y; y++){
      for(int x = 0; x < num_curves_x; x++){
        px = spacex + x * spacex;
        py = spacey + y * spacey;
        
//        ellipse(px, py, 10, 10);
        startCurveAt(px, py);
      }
    }
    
    

       
    // RENDER
    // display textures
    image(pg_fluid    , 0, 0);
    image(pg_obstacles, 0, 0);
    
    
    

    
    
    
    
    
    
    
    
    // info
    String txt_fps = String.format(getClass().getName()+ "   [size %d/%d]   [frame %d]   [fps %6.2f]", fluid.fluid_w, fluid.fluid_h, fluid.simulation_step, frameRate);
    surface.setTitle(txt_fps);
  }
  
  public void startCurveAt(int px, int py){
    float[] vel = new float[2];
    float mag_sq = 0f;
    
//    PShape shp;
//    s = createShape();
    
    pg_fluid.beginDraw();
    pg_fluid.noStroke();
    pg_fluid.fill(255);
    pg_fluid.ellipse(px,  py,  2,  2);
    
    pg_fluid.strokeWeight(1);
    pg_fluid.stroke(255);
    pg_fluid.noFill();
    pg_fluid.beginShape();
    pg_fluid.vertex(px, py);
    
    int num_samples = 30;
    for(int i = 0; i < num_samples; i++){
      
      vel = getVelocityAt(px, py);
      if(vel == null){
        break;
      }
      
      vel[0] *= 2;
      vel[1] *= 2;
      
      mag_sq = vel[0]*vel[0] + vel[1]*vel[1];
      if(mag_sq < 1){
        break;
      }
      
      px = Math.round(px + vel[0]);
      py = Math.round(py + vel[1]);
      
  
      
      pg_fluid.vertex(px, py);
//      pg_fluid.curveVertex(px, py);
//      pg_fluid.quadraticVertex(px, py, px, py);
    }
    pg_fluid.endShape();
    pg_fluid.endDraw();
  }
  
  
  public float[] getVelocityAt(int px, int py){
 

    int px_view = px;
    int py_view = height - 1 - py; // invert y
    
    int px_grid = px_view/fluidgrid_scale;
    int py_grid = py_view/fluidgrid_scale;

    int w_grid  = fluid.tex_velocity.src.w;
    int h_grid  = fluid.tex_velocity.src.h;
    
    if(px_grid < 0 || px_grid >= w_grid) return null;
    if(py_grid < 0 || py_grid >= h_grid) return null;
    
    int PIDX    = py_grid * w_grid + px_grid;

    float[] vxy = new float[2];
    vxy[0] = +fluid_velocity[PIDX * 2 + 0];
    vxy[1] = -fluid_velocity[PIDX * 2 + 1]; // invert y
    return vxy;
  }

  
  
  
  
  
  boolean UPDATE_FLUID = true;
  
  boolean DISPLAY_FLUID_TEXTURES  = true;
  boolean DISPLAY_FLUID_VECTORS   = !true;
  boolean DISPLAY_PARTICLES       = !true;
  
  int     DISPLAY_fluid_texture_mode = 0;
  
  public void keyReleased(){
    if(key == 'p') fluid_togglePause(); // pause / unpause simulation
    if(key == '+') fluid_resizeUp();    // increase fluid-grid resolution
    if(key == '-') fluid_resizeDown();  // decrease fluid-grid resolution
    if(key == 'r') fluid_reset();       // restart simulation
    
    if(key == '1') DISPLAY_fluid_texture_mode = 0; // density
    if(key == '2') DISPLAY_fluid_texture_mode = 1; // temperature
    if(key == '3') DISPLAY_fluid_texture_mode = 2; // pressure
    if(key == '4') DISPLAY_fluid_texture_mode = 3; // velocity
    
    if(key == 'q') DISPLAY_FLUID_TEXTURES = !DISPLAY_FLUID_TEXTURES;
    if(key == 'w') DISPLAY_FLUID_VECTORS  = !DISPLAY_FLUID_VECTORS;
    if(key == 'e') DISPLAY_PARTICLES      = !DISPLAY_PARTICLES;
  }
  

  public void fluid_resizeUp(){
    fluid.resize(width, height, fluidgrid_scale = max(1, --fluidgrid_scale));
  }
  public void fluid_resizeDown(){
    fluid.resize(width, height, ++fluidgrid_scale);
  }
  public void fluid_reset(){
//    particle_system.reset();
    fluid.reset();
  }
  public void fluid_togglePause(){
    UPDATE_FLUID = !UPDATE_FLUID;
  }
  public void setDisplayMode(int val){
    DISPLAY_fluid_texture_mode = val;
    DISPLAY_FLUID_TEXTURES = DISPLAY_fluid_texture_mode != -1;
  }
  public void setDisplayVelocityVectors(int val){
    DISPLAY_FLUID_VECTORS = val != -1;
  }
  public void setDisplayParticles(int val){
    DISPLAY_PARTICLES = val != -1;
  }
  
  
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    Group group_fluid = cp5.addGroup("fluid controls")
//    .setPosition(20, 40)
    .setHeight(20).setWidth(180)
    .setBackgroundHeight(360)
    .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
    group_fluid.getCaptionLabel().align(LEFT, CENTER);
  
    Button breset = cp5.addButton("reset").setGroup(group_fluid).plugTo(this, "fluid_reset").setWidth(75);
    Button bplus  = cp5.addButton("+"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeUp").setWidth(25);
    Button bminus = cp5.addButton("-"    ).setGroup(group_fluid).plugTo(this, "fluid_resizeDown").setWidth(25).linebreak();
    
    float[] pxy = breset.getPosition();
    bplus .setPosition(pxy[0] + 75 + 10, pxy[1]);
    bminus.setPosition(pxy[0] + 75 + 25 + 20, pxy[1]);
    
    int px, py, oy;
    int sx = 100, sy = 14;
    
    cp5.addSlider("velocity").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.dissipation_velocity)
    .plugTo(fluid.param, "dissipation_velocity").linebreak();
    
    cp5.addSlider("density").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.dissipation_density)
    .plugTo(fluid.param, "dissipation_density").linebreak();
    
    cp5 .addSlider("temperature").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.dissipation_temperature)
    .plugTo(fluid.param, "dissipation_temperature").linebreak();
  
    cp5 .addSlider("vorticity").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.vorticity)
    .plugTo(fluid.param, "vorticity").linebreak();
        
    cp5.addSlider("iterations").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 80).setValue(fluid.param.num_jacobi_projection)
    .plugTo(fluid.param, "num_jacobi_projection").linebreak();
          
    cp5.addSlider("timestep").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 1).setValue(fluid.param.timestep)
    .plugTo(fluid.param, "timestep").linebreak();
        
    cp5.addSlider("gridscale").setGroup(group_fluid).setSize(sx, sy)
    .setRange(0, 50).setValue(fluid.param.gridscale)
    .plugTo(fluid.param, "gridscale").linebreak();
    
    RadioButton rb_setDisplayMode = cp5.addRadio("setDisplayMode").setGroup(group_fluid)
        .setPosition(10, 210).setSize(80,18)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(2)
        .addItem("Density"    ,0)
        .addItem("Temperature",1)
        .addItem("Pressure"   ,2)
        .addItem("Velocity"   ,3)
        .activate(0);
    for(Toggle toggle : rb_setDisplayMode.getItems()) toggle.getCaptionLabel().alignX(CENTER);
    
    RadioButton rb_setDisplayVelocityVectors = cp5.addRadio("setDisplayVelocityVectors").setGroup(group_fluid)
        .setPosition(10, 255).setSize(18,18)
        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
        .addItem("Velocity Vectors",0)
//        .activate(0)
        ;

//    RadioButton rb_setDisplayParticles = cp5.addRadio("setDisplayParticles").setGroup(group_fluid)
//        .setPosition(10, 280).setSize(18,18)
//        .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
//        .addItem("Particles",0)
////        .activate(0)
//        ;

    cp5.addNumberbox("BACKGROUND_COLOR").setGroup(group_fluid)
    .setPosition(10,310).setSize(80,18)
    .setMin(0).setMax(255)
    .setScrollSensitivity(1) .setValue(BACKGROUND_COLOR);
    
   
    group_fluid.close();
   
    
    Accordion accordion = cp5.addAccordion("acc")
        .setPosition(20,20)
        .setWidth(180)
        .addItem(group_fluid)
        ;

    accordion.setCollapseMode(Accordion.MULTI);
    accordion.open(0);
    
  }
  
  

  
  public static void main(String args[]) {
    PApplet.main(new String[] { Main_Streamlines.class.getName() });
  }
}