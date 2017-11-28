package Skylight.Skylight_BulletPhysics_Breakable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;

import com.thomasdiewald.pixelflow.java.sampling.DwSampling;

import bRigid.BBox;
import bRigid.BConvexHull;
import bRigid.BObject;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PMatrix3D;
import processing.core.PShape;
import wblut.geom.WB_Coord;
import wblut.geom.WB_CoordCollection;
import wblut.geom.WB_GeometryOp2D;
import wblut.geom.WB_Point;
import wblut.geom.WB_Polygon;
import wblut.geom.WB_Voronoi;
import wblut.geom.WB_VoronoiCell2D;

public class BreakableBody{
  
  PApplet papplet;
  MyBPhysics physics;
  PShape group_bulletbodies;
  
  BObject body;
  WB_Polygon boundary;
  float mass = 0;
  float thickness = 0;
  float mass_mult = 1f;
  float[] bcolor = {255,255,255};
  
  public BreakableBody(PApplet papplet, MyBPhysics physics, PShape group_bulletbodies){
    this.papplet = papplet;
    this.physics = physics;
    this.group_bulletbodies = group_bulletbodies;
  }
 
  
  public void remove(){
    if(body != null){
      body.rigidBody.setUserPointer(null);
      physics.removeBody(body);

      int idx = group_bulletbodies.getChildIndex(body.displayShape);
      group_bulletbodies.removeChild(idx);
    }
  }
  public void initBody(Vector3f dim, PMatrix3D matp5, float[] bcolor){
    initBody(dim, matp5, bcolor, 1);
  }

  public void initBody(Vector3f dim, PMatrix3D matp5, float[] bcolor, float mass_mult){
    this.mass_mult = mass_mult;
    this.bcolor[0] = bcolor[0];
    this.bcolor[1] = bcolor[1];
    this.bcolor[2] = bcolor[2];

    float x_min = -(dim.x * 0.5f);
    float x_max = +(dim.x * 0.5f);
    float y_min = -(dim.y * 0.5f);
    float y_max = +(dim.y * 0.5f);
    
    ArrayList<WB_Point> pts = new  ArrayList<WB_Point>();
    pts.add(new WB_Point(x_min, y_min));
    pts.add(new WB_Point(x_max, y_min));
    pts.add(new WB_Point(x_max, y_max));
    pts.add(new WB_Point(x_min, y_max));
    
    boundary = new WB_Polygon(pts);
    thickness = dim.z;
    mass = dim.x * dim.y * dim.z * mass_mult;
    
    body = new BBox(papplet, mass, dim.x, dim.y, dim.z);
    
    asBulletTransform(matp5, transform);
    body.rigidBody.setWorldTransform(transform);
    body.rigidBody.getMotionState().setWorldTransform(transform);
    body.rigidBody.setUserPointer(this);
//    body.rigidBody.setRestitution(.01f);
//    body.rigidBody.setFriction(0.98f);
//    body.rigidBody.setDamping(0.2f, 0.2f);

    body.displayShape = papplet.createShape(PConstants.BOX, dim.x, dim.y, dim.z);
    body.displayShape.setFill(colorARGB(bcolor[0], bcolor[1], bcolor[2]));
    body.displayShape.setFill(true);
    body.displayShape.setStrokeWeight(1f);
    body.displayShape.setStroke(colorARGB(bcolor[0]*0.5f, bcolor[1]*0.5f, bcolor[2]*0.5f, 96));
    body.displayShape.setStroke(false);
    body.displayShape.setName("window| [wire]");

    group_bulletbodies.addChild(body.displayShape);
    physics.addBody(body);
  }
  
  
  PMatrix3D matp5     = new PMatrix3D();
  PMatrix3D matp5_inv = new PMatrix3D();
  Transform transform = new Transform();
  Matrix4f mat = new Matrix4f();
  Matrix4f mat_inv = new Matrix4f();
  public void createCellFracture(Vector3f fracture_center_world){

    if(body != null){

      transform = body.rigidBody.getMotionState().getWorldTransform(transform);
      mat = transform.getMatrix(mat);
      mat_inv.invert(mat);

      matp5.set(mat.m00, mat.m01, mat.m02, mat.m03, 
                mat.m10, mat.m11, mat.m12, mat.m13,
                mat.m20, mat.m21, mat.m22, mat.m23,
                mat.m30, mat.m31, mat.m32, mat.m33);
      matp5_inv = matp5.get();
      matp5_inv.invert();
    }
    
    
    float[] fracture_center_local = {0,0,0};
//    Vector3f fracture_center_local_ = new Vector3f();
    if(body != null && fracture_center_world != null){
      
//      mat_inv.transform(fracture_center_world, fracture_center_local_);
      
      float[] source = {fracture_center_world.x, fracture_center_world.y, fracture_center_world.z};
      fracture_center_local = matp5_inv.mult(source, fracture_center_local);
    }
    

    int num_voronoi_cells = 300;
    Random rand = new Random();
    
    // voronoi cell center
    List<WB_Point> points = new ArrayList<WB_Point>(num_voronoi_cells);

    for(int sample_idx = 0; sample_idx < num_voronoi_cells; sample_idx++){
      float r = 4 + 0.05f * (float) Math.pow(sample_idx, 1.5f);
//      float r = 4 + 8 * (float) Math.pow(sample_idx, 0.5f);
      
      float angle = sample_idx * (float) DwSampling.GOLDEN_ANGLE_R;
      float px = (float) (r * Math.cos(angle));
      float py = (float) (r * Math.sin(angle));

      float jitter = r * 0.15f;

      px += (rand.nextFloat() * 2 - 1) * jitter;
      py += (rand.nextFloat() * 2 - 1) * jitter;
      
      px += fracture_center_local[0];
      py += fracture_center_local[1];
//      px += fracture_center_local_.x;
//      py += fracture_center_local_.y;
      
      float idxn = sample_idx / (float)num_voronoi_cells;

      if(rand.nextFloat() < 0.8f * idxn) continue;
      
      WB_Point point = new WB_Point(px, py);
      if(WB_GeometryOp2D.contains2D(point, boundary)){
        points.add(point);
      } 
    }
    
    if(points.size() < 40){
      return;
    }

    // create voronoi
    List<WB_VoronoiCell2D> cells = WB_Voronoi.getClippedVoronoi2D(points, boundary, 0.00f);

    for (int i = 0; i < cells.size(); i++) {
      
      WB_VoronoiCell2D cell = cells.get(i);
      WB_Polygon cell_polygon = cell.getPolygon();
      int num_verts = cell_polygon.getNumberOfPoints();

      // compute center of mass
      float[][] pnts = new float[num_verts][2];
      for(int j = 0; j < num_verts; j++){
        WB_Point vtx = cell_polygon.getPoint(j);
        pnts[j][0] = vtx.xf(); 
        pnts[j][1] = vtx.yf();
      }
      
      // this one gives better results, than the voronoi center
//      DwBoundingDisk cell_bs = new DwBoundingDisk();
//      cell_bs.compute(pnts, pnts.length);
//      Vector3f center_of_mass = new Vector3f(cell_bs.pos[0], cell_bs.pos[1], 0f);
      
//      Vector3f center_of_mass = new Vector3f();
//      center_of_mass.x = cell_polygon.getCenter().xf();
//      center_of_mass.y = cell_polygon.getCenter().yf();
//      center_of_mass.z = cell_polygon.getCenter().zf();
      
      Vector3f center_of_mass = new Vector3f();
      center_of_mass.x = (cell.getGenerator().xf() + cell_polygon.getCenter().xf() ) * 0.5f;
      center_of_mass.y = (cell.getGenerator().yf() + cell_polygon.getCenter().yf() ) * 0.5f;
      center_of_mass.z = 0;
      
//      WB_Circle bounding_circle = WB_GeometryOp2D.getBoundingCircle2D(cell_points);
//      center_of_mass.x = bounding_circle.getCenter().xf();
//      center_of_mass.y = bounding_circle.getCenter().yf();
//      center_of_mass.z = 0;
      
      // create rigid body coords, center is at 0,0,0
      ObjectArrayList<Vector3f> vertices = new ObjectArrayList<Vector3f>(num_verts * 2);

      for(int j = 0; j < pnts.length; j++){
        float x = pnts[j][0] - center_of_mass.x;
        float y = pnts[j][1] - center_of_mass.y;
        vertices.add(new Vector3f(x, y, -thickness * 0.5f));
        vertices.add(new Vector3f(x, y, +thickness * 0.5f));
        cell_polygon.getPoint(j).set(x, y, 0);
      }
      
      
      // create rigid body
      float mass_new = (float) (cell.getArea() * thickness) * mass_mult;
      BConvexHull body_new = new MyBConvexHull(papplet, mass_new, vertices, new Vector3f(), true);
//
      // setup initial body transform-matrix
//      Matrix4f mat_translate = new Matrix4f();
//      mat_translate.setIdentity();
//      mat_translate.setTranslation(center_of_mass);
//
//      mat_translate.mul(mat);
//      Transform transform = new Transform(mat_translate);
      
      PMatrix3D mat_p5 = new PMatrix3D(matp5);
      mat_p5.translate(center_of_mass.x, center_of_mass.y, center_of_mass.z);
      Transform transform = asBulletTransform(mat_p5);

      // rigid-body properties
      body_new.rigidBody.getMotionState().setWorldTransform(transform);
      body_new.rigidBody.setWorldTransform(transform);
//      body_new.rigidBody.setRestitution(.01f);
//      body_new.rigidBody.setFriction(0.97f);
//      body_new.rigidBody.setDamping(0.2f, 0.2f);
      body_new.displayShape = createCellShape(cell_polygon, thickness);
      
      
      group_bulletbodies.addChild(body_new.displayShape);
      physics.addBody(body_new);
      
      
      BreakableBody bbody = new BreakableBody(papplet, physics, group_bulletbodies);
      bbody.body = body_new;
      bbody.boundary = cell_polygon;
      bbody.thickness = thickness;
      bbody.bcolor = bcolor;
      bbody.mass = mass_new;
      bbody.mass_mult = mass_mult;
      bbody.body.rigidBody.setUserPointer(bbody);
    }
    
    remove();
  }
  
  
  public PShape createCellShape(WB_Polygon polygon, float dimz){
    int num_points = polygon.getNumberOfPoints();
    float dimz_half = dimz*0.5f;
    
    PShape cell_top = papplet.createShape();
    cell_top.beginShape(PConstants.POLYGON);
    cell_top.normal(0, 0, -1);
    for(int i = 0; i < num_points; i++){
      WB_Point vtx = polygon.getPoint(i);
      cell_top.vertex(vtx.xf(), vtx.yf(), +dimz_half);
    }
    cell_top.endShape(PConstants.CLOSE);
    
    PShape cell_bot = papplet.createShape();
    cell_bot.beginShape(PConstants.POLYGON);
    cell_bot.normal(0, 0, -1);
    for(int i = 0; i < num_points; i++){
      WB_Point vtx = polygon.getPoint(i);
      cell_bot.vertex(vtx.xf(), vtx.yf(), -dimz_half);
    }
    cell_bot.endShape(PConstants.CLOSE);
    
    PShape cell_side = papplet.createShape();
    cell_side.beginShape(PConstants.QUADS);

    for(int i = 0; i <= num_points; i++){
      WB_Point v0 = polygon.getPoint((i+0)%num_points);
      WB_Point v1 = polygon.getPoint((i+1)%num_points);
      float v0x = v0.xf();
      float v0y = v0.yf();
      float v1x = v1.xf();
      float v1y = v1.yf();
      
      float dx = v1x - v0x;
      float dy = v1y - v0y;
      
      float nx = +dy;
      float ny = -dx;
      float nz = 0;
      float nn = (float) Math.sqrt(nx*nx + ny*ny);
      nx /= nn;  
      ny /= nn;  
      
      cell_side.normal(nx, ny, nz);
      cell_side.vertex(v0x, v0y, +dimz_half);
      cell_side.vertex(v0x, v0y, -dimz_half);
      cell_side.vertex(v1x, v1y, -dimz_half);
      cell_side.vertex(v1x, v1y, +dimz_half);
     
    }

    cell_side.endShape();
    
    
    PShape cell = papplet.createShape(PConstants.GROUP);
    cell.addChild(cell_top);
    cell.addChild(cell_bot);
    cell.addChild(cell_side);
    
    float r = bcolor[0];
    float g = bcolor[1];
    float b = bcolor[2];
    
    cell.setFill(colorARGB(r,g,b));
    cell.setFill(true);
    cell.setStrokeWeight(1f);
    cell.setStroke(colorARGB(r*0.5f,g*0.5f,b*0.5f, 96));
    cell.setStroke(false);
    
    cell.setName("[wire]");
    
    return cell;
  }
  
  
  

  
  

  public Transform asBulletTransform(PMatrix3D mat_p5){
    mat_tmp.setRow(0, mat_p5.m00, mat_p5.m01, mat_p5.m02, mat_p5.m03);
    mat_tmp.setRow(1, mat_p5.m10, mat_p5.m11, mat_p5.m12, mat_p5.m13);
    mat_tmp.setRow(2, mat_p5.m20, mat_p5.m21, mat_p5.m22, mat_p5.m23);
    mat_tmp.setRow(3, mat_p5.m30, mat_p5.m31, mat_p5.m32, mat_p5.m33);
    return new Transform(mat_tmp);
  }
  
  Matrix4f mat_tmp = new Matrix4f();
  public Transform asBulletTransform(PMatrix3D mat_p5, Transform transform){
    if(transform == null) transform = new Transform();
    mat_tmp.setRow(0, mat_p5.m00, mat_p5.m01, mat_p5.m02, mat_p5.m03);
    mat_tmp.setRow(1, mat_p5.m10, mat_p5.m11, mat_p5.m12, mat_p5.m13);
    mat_tmp.setRow(2, mat_p5.m20, mat_p5.m21, mat_p5.m22, mat_p5.m23);
    mat_tmp.setRow(3, mat_p5.m30, mat_p5.m31, mat_p5.m32, mat_p5.m33);
    transform.set(mat_tmp);
    return transform;
  }
  
  
  static protected int colorARGB(float r, float g, float b){
    int ir = Math.round(clamp(r, 0, 255));
    int ig = Math.round(clamp(g, 0, 255));
    int ib = Math.round(clamp(b, 0, 255));
    int ia = 255;
    return ia << 24 | ir << 16 | ig << 8 | ib;
  }
  
  static protected int colorARGB(float r, float g, float b, float a){
    int ir = Math.round(clamp(r, 0, 255));
    int ig = Math.round(clamp(g, 0, 255));
    int ib = Math.round(clamp(b, 0, 255));
    int ia = Math.round(clamp(a, 0, 255));
    return ia << 24 | ir << 16 | ig << 8 | ib;
  }
  
  static final public float clamp(float val, float lo, float hi){
    return (val < lo) ? lo : (val > hi) ? hi : val;
  }
  
  
  

  
}
