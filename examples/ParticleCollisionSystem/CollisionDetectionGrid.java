package ParticleCollisionSystem;
public class CollisionDetectionGrid{
  
  private float CELL_SIZE;
  private int   GRID_X; 
  private int   GRID_Y;

  private int        HEAD_PTR;
  private int[]      HEAD = new int[0];
  private int[]      NEXT = new int[0];
  private Particle[] DATA = new Particle[0];
  
  ParticleSystem particlesystem;
  
  
  public CollisionDetectionGrid(ParticleSystem particlesystem){
    this.particlesystem = particlesystem;
  }
  
  
  public void updateCollisions(){


    Particle[] particles      = particlesystem.particles;
    int particle_count        = particlesystem.PARTICLE_COUNT;
    int particlesystem_size_x = particlesystem.size_x;
    int particlesystem_size_y = particlesystem.size_x;
    

    ////////////////////////////////////////////////////////////////////////////
    // 1) reAlloc if necessary
    ////////////////////////////////////////////////////////////////////////////
    CELL_SIZE = (Particle.MAX_RAD * 2) + 0.0001f;

    CELL_SIZE = Math.max(CELL_SIZE, 1);
    
    int gx = (int) Math.ceil(particlesystem_size_x/CELL_SIZE);
    int gy = (int) Math.ceil(particlesystem_size_y/CELL_SIZE);
    
    if( gx != GRID_X || gy != GRID_Y){
      GRID_X = gx;
      GRID_Y = gy;
      HEAD = new int[GRID_X * GRID_Y];
    }
    
    int PPLL_size = particle_count * 4 + 1;
    if(PPLL_size > NEXT.length){
      NEXT = new int     [PPLL_size];
      DATA = new Particle[PPLL_size];
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // 2) reset
    ////////////////////////////////////////////////////////////////////////////
    HEAD_PTR = 0;
    for(int i = 0; i < HEAD.length; i++) HEAD[i] = 0;
//    for(int i = 0; i < NEXT.length; i++) NEXT[i] = 0;
//    for(int i = 0; i < DATA.length; i++) DATA[i] = null; 
    
    for(int i = 0; i < particle_count; i++){
      particles[i].beforeCollisionDetection();
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // 3) create per-pixel-linked-list (PPLL)
    ////////////////////////////////////////////////////////////////////////////
    for(int i = 0; i < particle_count; i++){
      Particle particle = particles[i];
      float pr = particle.rad;
      float px = particle.x;
      float py = particle.y;
      
      int xmin = (int)((px-pr)/CELL_SIZE); xmin = Math.max(xmin, 0);
      int xmax = (int)((px+pr)/CELL_SIZE); xmax = Math.min(xmax, GRID_X-1);
      int ymin = (int)((py-pr)/CELL_SIZE); ymin = Math.max(ymin, 0);
      int ymax = (int)((py+pr)/CELL_SIZE); ymax = Math.min(ymax, GRID_Y-1);
      
//      int count = (1 + xmax - xmin)*(1 + ymax - ymin);
//      if(count > 4){
//        System.out.println("["+i+"] "+count + ":      "+xmin+", "+xmax + "     "+ymin+", "+ymax);
//      }
      
      for(int y = ymin; y <= ymax ; y++){
        for(int x = xmin; x <= xmax ; x++){
          int gid = y * GRID_X + x;
          int new_head = HEAD_PTR++;
          int old_head = HEAD[gid]; HEAD[gid] = new_head; // xchange head pointer
          NEXT[new_head] = old_head;
          DATA[new_head] = particle;
        }
      }
    }
    
    

    ////////////////////////////////////////////////////////////////////////////
    // 4) solve collisions for each particle
    ////////////////////////////////////////////////////////////////////////////
    for(int i = 0; i < particle_count; i++){
      Particle particle = particles[i];
      
      // to make sure, we don't do a collision test with ourself
      particle.tmp = particle; 
      
      float pr = particle.rad;
      float px = particle.x;
      float py = particle.y;
      
      int xmin = (int)((px-pr)/CELL_SIZE); xmin = Math.max(xmin, 0);
      int xmax = (int)((px+pr)/CELL_SIZE); xmax = Math.min(xmax, GRID_X-1);
      int ymin = (int)((py-pr)/CELL_SIZE); ymin = Math.max(ymin, 0);
      int ymax = (int)((py+pr)/CELL_SIZE); ymax = Math.min(ymax, GRID_Y-1);

      for(int y = ymin; y <= ymax ; y++){
        for(int x = xmin; x <= xmax ; x++){
          int gid = y * GRID_X + x;
          int head = HEAD[gid];
          while(head > 0){
            Particle othr = DATA[head];
            particle.updateCollision(othr);  
            head = NEXT[head];
          }
        }
      }
        
    }

  }

  
  }