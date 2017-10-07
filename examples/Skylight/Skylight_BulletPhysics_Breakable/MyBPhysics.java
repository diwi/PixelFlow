package Skylight.Skylight_BulletPhysics_Breakable;

import bRigid.BInterface;
import bRigid.BObject;
import bRigid.BPhysics;

public class MyBPhysics extends BPhysics{
  
  
  public void updateBehaviors1(){
    if (behaviors != null) {
      for (BObject o : rigidBodies) {
        for (BInterface b : behaviors) {
          b.apply(this, o);
        }
      }
    }
  }
  
  private void updateObjects1() {
    for (BObject o : rigidBodies) {
      if (o.behaviors != null) {
        for (BInterface b : o.behaviors) {
          b.apply(this, o);
        }
      }
    }
  }
  
  public void update(float frameRate){
    for (int i = 0; i < stepSimulation; i++) {
      updateBehaviors1();
      updateObjects1();
      // float stepTime = getDeltaTimeMicroseconds();
      // world.stepSimulation(stepTime / 1000000f);
      world.stepSimulation(1.0f / 400, 100, 1.0f / 200.0f);
    }
  
  }
  
  @Override
  public void update() {
    super.update();
  }
}