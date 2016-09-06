package tests.statePushPop;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;

import processing.core.*;

import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PJOGL;

public class Main_beginEndPushPop extends PApplet {

  // let's try 1,000,000 points
  int numPoints = 1000000;

  // pan, zoom and rotate
  float tx = 0, ty = 0;
  float sc = 1;
  float a = 0.0f;

  FloatBuffer vbuffer;
  FloatBuffer cbuffer;
  float[] projMatrix;
  float[] mvMatrix;

  PGraphicsOpenGL pg;
  PGL pgl;
  GL2 gl2;
  
  public void settings() {
    size(displayWidth / 2, displayHeight / 2, P3D);
//    PJOGL.profile = 2;
  }

  public void setup() {
 
 
    pg = (PGraphicsOpenGL) g;
    pgl = beginPGL();
    
//    System.out.println(gl2 = ((PJOGL) pgl).gl);
    
    GL gl = ((PJOGL) pgl).gl;
    GLContext ctx = gl.getContext();

    GLProfile profile =  gl.getGLProfile();
    
    
//    profile.
  
    gl2 = gl.getGL2();

    int vSize = (numPoints * 2);
    int cSize = (numPoints * 3);
    vSize = vSize << 2;
    cSize = cSize << 2;

    vbuffer = ByteBuffer.allocateDirect(vSize).order(ByteOrder.nativeOrder()).asFloatBuffer();
    cbuffer = ByteBuffer.allocateDirect(cSize).order(ByteOrder.nativeOrder()).asFloatBuffer();
    for (int i = 0; i < numPoints; i++) {
      // random x,y
      vbuffer.put(random(width));
      vbuffer.put(random(height));
      // random r,g,b
      cbuffer.put(random(1.0f));
      cbuffer.put(random(1.0f));
      cbuffer.put(random(1.0f));
    }
    vbuffer.rewind();
    cbuffer.rewind();

    gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
    gl2.glVertexPointer(2, GL2.GL_FLOAT, 0, vbuffer);

    gl2.glEnableClientState(GL2.GL_COLOR_ARRAY);
    gl2.glColorPointer(3, GL2.GL_FLOAT, 0, cbuffer);

    pg.endPGL();

    projMatrix = new float[16];
    mvMatrix = new float[16];
  }

  public void draw() {
    background(0);

    pgl = beginPGL();
    gl2 = ((PJOGL) pgl).gl.getGL2();

    copyMatrices();

    gl2.glPushMatrix();
    gl2.glTranslatef(width / 2, height / 2, 0);
    gl2.glScalef(sc, sc, sc);
    gl2.glRotatef(a, 0.0f, 0.0f, 1.0f);
    gl2.glTranslatef(-width / 2, -height / 2, 0);
    gl2.glTranslatef(tx, ty, 0);

    gl2.glPointSize(2.0f);

    gl2.glDrawArrays(GL2.GL_POINTS, 0, numPoints);

    gl2.glPopMatrix();

    endPGL();
  }

  public void mouseDragged() {
    float dx = (mouseX - pmouseX) / sc;
    float dy = (mouseY - pmouseY) / sc;
    float angle = radians(-a);
    float rx = cos(angle) * dx - sin(angle) * dy;
    float ry = sin(angle) * dx + cos(angle) * dy;
    tx += rx;
    ty += ry;
  }

  void copyMatrices() {
    gl2.glMatrixMode(GL2.GL_PROJECTION);

    projMatrix[0] = pg.projection.m00;
    projMatrix[1] = pg.projection.m10;
    projMatrix[2] = pg.projection.m20;
    projMatrix[3] = pg.projection.m30;

    projMatrix[4] = pg.projection.m01;
    projMatrix[5] = pg.projection.m11;
    projMatrix[6] = pg.projection.m21;
    projMatrix[7] = pg.projection.m31;

    projMatrix[8] = pg.projection.m02;
    projMatrix[9] = pg.projection.m12;
    projMatrix[10] = pg.projection.m22;
    projMatrix[11] = pg.projection.m32;

    projMatrix[12] = pg.projection.m03;
    projMatrix[13] = pg.projection.m13;
    projMatrix[14] = pg.projection.m23;
    projMatrix[15] = pg.projection.m33;

    gl2.glLoadMatrixf(projMatrix, 0);

    gl2.glMatrixMode(GL2.GL_MODELVIEW);
    mvMatrix[0] = pg.modelview.m00;
    mvMatrix[1] = pg.modelview.m10;
    mvMatrix[2] = pg.modelview.m20;
    mvMatrix[3] = pg.modelview.m30;

    mvMatrix[4] = pg.modelview.m01;
    mvMatrix[5] = pg.modelview.m11;
    mvMatrix[6] = pg.modelview.m21;
    mvMatrix[7] = pg.modelview.m31;

    mvMatrix[8] = pg.modelview.m02;
    mvMatrix[9] = pg.modelview.m12;
    mvMatrix[10] = pg.modelview.m22;
    mvMatrix[11] = pg.modelview.m32;

    mvMatrix[12] = pg.modelview.m03;
    mvMatrix[13] = pg.modelview.m13;
    mvMatrix[14] = pg.modelview.m23;
    mvMatrix[15] = pg.modelview.m33;
    gl2.glLoadMatrixf(mvMatrix, 0);
  }

  public static void main(String args[]) {
    PApplet.main(new String[] { Main_beginEndPushPop.class.getName() });
  }
}