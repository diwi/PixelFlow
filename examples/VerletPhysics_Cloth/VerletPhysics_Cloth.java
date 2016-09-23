package VerletPhysics_Cloth;




import java.util.ArrayList;

import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics2D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies2D.SoftBody2D;
import com.thomasdiewald.pixelflow.java.verletphysics.softbodies2D.SoftGrid;

import controlP5.Accordion;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.*;

public class VerletPhysics_Cloth extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  int gui_w = 200;
  int gui_x = viewport_w - gui_w;
  int gui_y = 0;
  
  // physics simulation
  VerletPhysics2D physics;
  
  // cloth parameters
  VerletParticle2D.Param param_cloth1 = new VerletParticle2D.Param();
  VerletParticle2D.Param param_cloth2 = new VerletParticle2D.Param();
  
  // cloth objects
  SoftGrid cloth1 = new SoftGrid();
  SoftGrid cloth2 = new SoftGrid();
  
  // list, that wills store the cloths
  ArrayList<SoftBody2D> softbodies = new ArrayList<SoftBody2D>();

  // 0 ... default: particles, spring
  // 1 ... tension
  int DISPLAY_MODE = 0;
  
  // entities to display
  boolean DISPLAY_PARTICLES      = true;
  boolean DISPLAY_SPRINGS_STRUCT = true;
  boolean DISPLAY_SPRINGS_SHEAR  = true;
  boolean DISPLAY_SPRINGS_BEND   = true;
  
  // first thing to do, inside draw()
  boolean NEED_REBUILD = true;
  
  // just for the window title-info
  int NUM_SPRINGS;
  int NUM_PARTICLES;
  

  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(4);
  }
  


  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
//    context.printGL();
    
    physics = new VerletPhysics2D();

    physics.param.GRAVITY = new float[]{ 0, 0.2f };
    physics.param.bounds  = new float[]{ 0, 0, viewport_w-gui_w, height };
    physics.param.iterations_collisions = 4;
    physics.param.iterations_springs    = 4;
    
    // Parameters for Cloth1 particles
    param_cloth1.DAMP_BOUNDS          = 0.40f;
    param_cloth1.DAMP_COLLISION       = 0.99999f;
    param_cloth1.DAMP_VELOCITY        = 0.991f; 
    param_cloth1.DAMP_SPRING_decrease = 0.999999f;    
    param_cloth1.DAMP_SPRING_increase = 0.0005999999f;
    
    // Parameters for Cloth2 particles
    param_cloth2.DAMP_BOUNDS          = 0.40f;
    param_cloth2.DAMP_COLLISION       = 0.99999f;
    param_cloth2.DAMP_VELOCITY        = 0.991f; 
    param_cloth2.DAMP_SPRING_decrease = 0.999999f;    
    param_cloth2.DAMP_SPRING_increase = 0.0005999999f;
    
    // initial cloth building parameters, both cloth start the same
    cloth1.CREATE_STRUCT_SPRINGS = true;
    cloth1.CREATE_SHEAR_SPRINGS  = true;
    cloth1.CREATE_BEND_SPRINGS   = true;
    cloth1.bend_spring_mode      = 0;
    cloth1.bend_spring_dist      = 3;
    
    cloth2.CREATE_STRUCT_SPRINGS = true;
    cloth2.CREATE_SHEAR_SPRINGS  = true;
    cloth2.CREATE_BEND_SPRINGS   = true;
    cloth2.bend_spring_mode      = 0;
    cloth2.bend_spring_dist      = 3;

    createGUI();
    
    frameRate(600);
  }
  
  
  
  public void initBodies(){
    
    physics.reset();
    
    softbodies.clear();
    
    softbodies.add(cloth1);
    softbodies.add(cloth2);
    
    cloth1.setParticleColor(color(255, 180,   0, 128));
    cloth2.setParticleColor(color(  0, 180, 255, 128));
    
    VerletParticle2D.Param[] cloth_params = {param_cloth1, param_cloth2};
    
    // both cloth are of the same size
    int nodex_x = 30;
    int nodes_y = 30;
    int nodes_r = 8;
    int nodes_start_x = 0;
    int nodes_start_y = 80;
    
    int   num_cloth = softbodies.size();
    float cloth_width = 2 * nodes_r * (nodex_x-1);
    float spacing = ((viewport_w - gui_w) - num_cloth * cloth_width) / (float)(num_cloth+1);  
    
    // create all cloth in the list
    for(int i = 0; i < num_cloth; i++){
      nodes_start_x += spacing + cloth_width * i;
      SoftGrid cloth = (SoftGrid) softbodies.get(i);
      cloth.create(physics, cloth_params[i], nodex_x, nodes_y, nodes_r, nodes_start_x, nodes_start_y);
      cloth.getNode(              0, 0).enable(false, false, false); // fix node to current location
      cloth.getNode(cloth.nodes_x-1, 0).enable(false, false, false); // fix node to current location
      cloth.createShape(this);
      softbodies.add(cloth);
    }
    

//    SpringConstraint.makeAllSpringsBidirectional(physics.getParticles());
    
    NUM_SPRINGS   = SpringConstraint.getSpringCount(physics.getParticles(), true);
    NUM_PARTICLES = physics.getParticlesCount();
  }


  
  
  
  public void draw() {

    if(NEED_REBUILD){
      initBodies();
      NEED_REBUILD = false;
    }
    
    updateMouseInteractions();

    // update physics simulation
    physics.update(1);
    
    // render
    background(DISPLAY_MODE == 0 ?  255 : 92);
    
    // 1) particles
    if(DISPLAY_PARTICLES){
      for(SoftBody2D body : softbodies){
        body.use_particles_color = (DISPLAY_MODE == 0);
        body.drawParticles(this.g);
      }
    }
    
    // 2) springs
    for(SoftBody2D body : softbodies){
      if(DISPLAY_SPRINGS_BEND  ) body.drawSprings(this.g, SpringConstraint.TYPE.BEND  , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_SHEAR ) body.drawSprings(this.g, SpringConstraint.TYPE.SHEAR , DISPLAY_MODE);
      if(DISPLAY_SPRINGS_STRUCT) body.drawSprings(this.g, SpringConstraint.TYPE.STRUCT, DISPLAY_MODE);
    }

    // interaction stuff
    if(DELETE_SPRINGS){
      fill(255,64);
      stroke(0);
      strokeWeight(1);
      ellipse(mouseX, mouseY, DELETE_RADIUS*2, DELETE_RADIUS*2);
    }


    // some info, windows title
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  
  
  
  
  
  // this resets all springs and particles, to some of its initial states
  // can be used after deactivating springs with the mouse
  public void repairAllSprings(){
    SpringConstraint.makeAllSpringsUnidirectional(physics.getParticles());
    for(SoftBody2D body : softbodies){
      for(VerletParticle2D pa : body.particles){
        pa.setCollisionGroup(body.collision_group_id);
        pa.setRadiusCollision(pa.rad());
      }
    }
  }
  
  
  // update all springs rest-lengths, based on current particle position
  // the effect is, that the body keeps the current shape
  public void applySpringMemoryEffect(){
    for(SoftBody2D body : softbodies){
      for(VerletParticle2D pa : body.particles){
        for(int i = 0; i < pa.spring_count; i++){
          pa.springs[i].updateRestlength();
        }
      }
    }
  }
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 
  VerletParticle2D particle_mouse = null;
  
  public VerletParticle2D findNearestParticle(float mx, float my){
    return findNearestParticle(mx, my, Float.MAX_VALUE);
  }
  
  public VerletParticle2D findNearestParticle(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    VerletParticle2D[] particles = physics.getParticles();
    VerletParticle2D particle = null;
    for(int i = 0; i < particles.length; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq =  dx*dx + dy*dy;
      if( dd_sq < dd_min_sq){
        dd_min_sq = dd_sq;
        particle = particles[i];
      }
    }
    return particle;
  }
  
  public ArrayList<VerletParticle2D> findParticlesWithinRadius(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    VerletParticle2D[] particles = physics.getParticles();
    ArrayList<VerletParticle2D> list = new ArrayList<VerletParticle2D>();
    for(int i = 0; i < particles.length; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq =  dx*dx + dy*dy;
      if(dd_sq < dd_min_sq){
        list.add(particles[i]);
      }
    }
    return list;
  }
  
  
  public void updateMouseInteractions(){
    if(cp5.isMouseOver()) return; 
    
    // deleting springs/constraints between particles
    if(DELETE_SPRINGS){
      ArrayList<VerletParticle2D> list = findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
      for(VerletParticle2D tmp : list){
        SpringConstraint.deactivateSprings(tmp);
        tmp.collision_group = physics.getNewCollisionGroupId();
        tmp.rad_collision = tmp.rad;
      }
    } else {
      if(particle_mouse != null) particle_mouse.moveTo(mouseX, mouseY, 0.2f);
    }
  }
  
  
  boolean DELETE_SPRINGS = false;
  float   DELETE_RADIUS = 20;

  public void mousePressed(){
    boolean mouseInteraction = !cp5.isMouseOver();
    if(mouseInteraction){
      if(mouseButton == RIGHT ) DELETE_SPRINGS = true; 
      if(!DELETE_SPRINGS){
        particle_mouse = findNearestParticle(mouseX, mouseY, 100);
        if(particle_mouse != null) particle_mouse.enable(false, false, false);
      }
    }
  }
  
  public void mouseReleased(){
    if(!DELETE_SPRINGS && particle_mouse != null){
      if(mouseButton == LEFT  ) particle_mouse.enable(true, true, true);
      if(mouseButton == CENTER) particle_mouse.enable(true, false, false);
      particle_mouse = null;
    }
    if(mouseButton == RIGHT ) DELETE_SPRINGS = false;
  }
  
  public void keyReleased(){
    if(key == 'r') initBodies();
    if(key == 's') repairAllSprings();
    if(key == 'm') applySpringMemoryEffect();
    if(key == '1') DISPLAY_MODE = 0;
    if(key == '2') DISPLAY_MODE = 1;
    if(key == 'p') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
  }

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  //////////////////////////////////////////////////////////////////////////////
  // GUI STUFF
  //////////////////////////////////////////////////////////////////////////////
  
  
  public void cloth1_CREATE_SPRING_TYPE  (float[] val){
    cloth1.CREATE_STRUCT_SPRINGS = (val[0] > 0);
    cloth1.CREATE_SHEAR_SPRINGS  = (val[1] > 0);
    cloth1.CREATE_BEND_SPRINGS   = (val[2] > 0);
    NEED_REBUILD = true;
  }
  public void cloth1_BEND_SPRING_MODE(int val){
    cloth1.bend_spring_mode = val;
    NEED_REBUILD = true;
  }
  public void cloth1_BEND_SPRING_LEN(int val){
    cloth1.bend_spring_dist = val;
    NEED_REBUILD = true;
  }
  
  
  
  public void cloth2_CREATE_SPRING_TYPE  (float[] val){
    cloth2.CREATE_STRUCT_SPRINGS = (val[0] > 0);
    cloth2.CREATE_SHEAR_SPRINGS  = (val[1] > 0);
    cloth2.CREATE_BEND_SPRINGS   = (val[2] > 0);
    NEED_REBUILD = true;
  }
  public void cloth2_BEND_SPRING_MODE(int val){
    cloth2.bend_spring_mode = val;
    NEED_REBUILD = true;
  }

  public void cloth2_BEND_SPRING_LEN(int val){
    cloth2.bend_spring_dist = val;
    NEED_REBUILD = true;
  }
  
  
  public void setDisplayMode(int val){
    DISPLAY_MODE = val;
  }
  
  public void setDisplayTypes(float[] val){
    DISPLAY_PARTICLES      = (val[0] > 0);
    DISPLAY_SPRINGS_STRUCT = (val[1] > 0);
    DISPLAY_SPRINGS_SHEAR  = (val[2] > 0);
    DISPLAY_SPRINGS_BEND   = (val[3] > 0);
  }
  
  public void setGravity(float val){
    physics.param.GRAVITY[1] = val;
  }
  
  
  
  
  
  ControlP5 cp5;
  
  public void createGUI(){
    cp5 = new ControlP5(this);
    
    int sx, sy, px, py, oy;
    
    sx = 100; sy = 14; oy = (int)(sy*1.4f);
    
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - CLOTH1
    ////////////////////////////////////////////////////////////////////////////
    Group group_physics = cp5.addGroup("global");
    {
      group_physics.setHeight(20).setSize(gui_w, height)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_physics.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
      
      int bsx = (gui_w-40)/3;
      cp5.addButton("rebuild").setGroup(group_physics).plugTo(this, "initBodies"             ).setSize(bsx, 18).setPosition(px, py);
      cp5.addButton("repair" ).setGroup(group_physics).plugTo(this, "repairAllSprings"       ).setSize(bsx, 18).setPosition(px+=bsx+10, py);
      cp5.addButton("memory" ).setGroup(group_physics).plugTo(this, "applySpringMemoryEffect").setSize(bsx, 18).setPosition(px+=bsx+10, py);
      
      px = 10; 
      cp5.addSlider("gravity").setGroup(group_physics).setSize(sx, sy).setPosition(px, py+=(int)(oy*1.5f))
          .setRange(0, 7).setValue(physics.param.GRAVITY[1]).plugTo(this, "setGravity");
      
      cp5.addSlider("iter: springs").setGroup(group_physics).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 50).setValue(physics.param.iterations_springs).plugTo( physics.param, "iterations_springs");
      
      cp5.addSlider("iter: collisions").setGroup(group_physics).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 10).setValue(physics.param.iterations_collisions).plugTo( physics.param, "iterations_collisions");
      
      cp5.addRadio("setDisplayMode").setGroup(group_physics).setSize(sy,sy).setPosition(px, py+=(int)(oy*1.4f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("colored",0)
          .addItem("tension",1)
          .activate(DISPLAY_MODE);
      
      cp5.addCheckBox("setDisplayTypes").setGroup(group_physics).setSize(sy,sy).setPosition(px, py+=(int)(oy*2.4f))
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("PARTICLES", 0).activate(DISPLAY_PARTICLES      ? 0 : 5)
          .addItem("STRUCT "  , 1).activate(DISPLAY_SPRINGS_STRUCT ? 1 : 5)
          .addItem("SHEAR"    , 2).activate(DISPLAY_SPRINGS_SHEAR  ? 2 : 5)
          .addItem("BEND"     , 3).activate(DISPLAY_SPRINGS_BEND   ? 3 : 5);
    }
    
    

    ////////////////////////////////////////////////////////////////////////////
    // GUI - CLOTH1
    ////////////////////////////////////////////////////////////////////////////
    Group group_cloth1 = cp5.addGroup("cloth 1");
    {
      Group group_cloth = group_cloth1;
      
      group_cloth.setHeight(20).setSize(gui_w, 210)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_cloth.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
       
      cp5.addSlider("C1.velocity").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py)
          .setRange(0, 1).setValue(param_cloth1.DAMP_VELOCITY).plugTo(param_cloth1, "DAMP_VELOCITY");
      
      cp5.addSlider("C1.contraction").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(param_cloth1.DAMP_SPRING_decrease).plugTo(param_cloth1, "DAMP_SPRING_decrease");
      
      cp5.addSlider("C1.expansion").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(param_cloth1.DAMP_SPRING_increase).plugTo(param_cloth1, "DAMP_SPRING_increase");

      cp5.addCheckBox("cloth1_CREATE_SPRING_TYPE").setGroup(group_cloth).setSize(sy,sy).setPosition(px, py+=oy)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("C1.struct springs", 0).activate(cloth1.CREATE_STRUCT_SPRINGS ? 0 : 2)
          .addItem("C1.shear springs" , 1).activate(cloth1.CREATE_SHEAR_SPRINGS  ? 1 : 2)
          .addItem("C1.bend springs"  , 2).activate(cloth1.CREATE_BEND_SPRINGS   ? 2 : 2)
          ;
      
      cp5.addRadio("cloth1_BEND_SPRING_MODE").setGroup(group_cloth).setSize(sy,sy).setPosition(px, py+=oy*3)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("C1.bend springs: diagonal",0)
          .addItem("C1.bend springs: ortho"   ,1)
          .addItem("C1.bend springs: random"  ,2)
          .activate(cloth1.bend_spring_mode);
      
      cp5.addSlider("C1.bend spring len").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy*3)
          .setRange(0, 50).setValue(cloth1.bend_spring_dist).plugTo(this, "cloth1_BEND_SPRING_LEN");

    }
    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - CLOTH 2
    ////////////////////////////////////////////////////////////////////////////
    Group group_cloth2 = cp5.addGroup("cloth 2");
    {
      Group group_cloth = group_cloth2;
      
      group_cloth.setHeight(20).setSize(gui_w, 210)
      .setBackgroundColor(color(16, 180)).setColorBackground(color(16, 180));
      group_cloth.getCaptionLabel().align(CENTER, CENTER);
      
      px = 10; py = 15;
       
      cp5.addSlider("C2.velocity").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py)
          .setRange(0, 1).setValue(param_cloth2.DAMP_VELOCITY).plugTo(param_cloth2, "DAMP_VELOCITY");
      
      cp5.addSlider("C2.contraction").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(param_cloth2.DAMP_SPRING_decrease).plugTo(param_cloth2, "DAMP_SPRING_decrease");
      
      cp5.addSlider("C2.expansion").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy)
          .setRange(0, 1).setValue(param_cloth2.DAMP_SPRING_increase).plugTo(param_cloth2, "DAMP_SPRING_increase");

      cp5.addCheckBox("cloth2_CREATE_SPRING_TYPE").setGroup(group_cloth).setSize(sy,sy).setPosition(px, py+=oy)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("C2.struct springs", 0).activate(cloth1.CREATE_STRUCT_SPRINGS ? 0 : 2)
          .addItem("C2.shear springs" , 1).activate(cloth1.CREATE_SHEAR_SPRINGS  ? 1 : 2)
          .addItem("C2.bend springs"  , 2).activate(cloth1.CREATE_BEND_SPRINGS   ? 2 : 2)
          ;
      
      cp5.addRadio("cloth2_BEND_SPRING_MODE").setGroup(group_cloth).setSize(sy,sy).setPosition(px, py+=oy*3)
          .setSpacingColumn(2).setSpacingRow(2).setItemsPerRow(1)
          .addItem("C2.bend springs: diagonal",0)
          .addItem("C2.bend springs: ortho"   ,1)
          .addItem("C2.bend springs: random"  ,2)
          .activate(cloth2.bend_spring_mode);
      
      cp5.addSlider("C2.bend spring len").setGroup(group_cloth).setSize(sx, sy).setPosition(px, py+=oy*3)
      .setRange(0, 50).setValue(cloth2.bend_spring_dist).plugTo(this, "cloth2_BEND_SPRING_LEN");
    }

    
    ////////////////////////////////////////////////////////////////////////////
    // GUI - ACCORDION
    ////////////////////////////////////////////////////////////////////////////
    cp5.addAccordion("acc").setPosition(gui_x, gui_y).setWidth(gui_w).setSize(gui_w, height)
      .setCollapseMode(Accordion.MULTI)
      .addItem(group_cloth1)
      .addItem(group_cloth2)
      .addItem(group_physics)
      .open(0, 1, 2);
   
  }
  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  
  public static void main(String args[]) {
    PApplet.main(new String[] { VerletPhysics_Cloth.class.getName() });
  }
}