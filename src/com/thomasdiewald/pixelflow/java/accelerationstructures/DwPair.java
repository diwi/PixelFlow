package com.thomasdiewald.pixelflow.java.accelerationstructures;

import com.thomasdiewald.pixelflow.java.geometry.DwHalfEdge;

/**
 * Integer Pair, can be used as for HashMap keys, 
 * e.g. for HalfEdge pair finding, etc...
 * or general, edge generation
 * http://stackoverflow.com/questions/7032961/java-how-to-use-a-pair-of-keys-for-hashmap
 * 
 * @author thomas diewald
 */
public class DwPair<T> {
  
  private final T a, b;
  private final int hashcode;
  
  public DwPair(final T a, final T b) { 
    this.a = a; 
    this.b = b; 
    this.hashcode = a.hashCode() + b.hashCode()*31;
  }
  
  @Override  
  public final int hashCode() { return hashcode; }
  
  @Override
  public final boolean equals(Object o) {
    return equals((DwPair<?>)o);
  }
  public final boolean equals(final DwPair<?> e){
    return  (a.equals(e.a) && b.equals(e.b));
  }
  
  
  // comfort methods
  public static final DwPair<Integer> AB(final DwHalfEdge.Edge e){  return new DwPair<Integer>( e.vert, e.next.vert ); }
  public static final DwPair<Integer> BA(final DwHalfEdge.Edge e){  return new DwPair<Integer>( e.next.vert, e.vert ); }
  
}