//package com.thomasdiewald.pixelflow.java.accelerationstructures;
//
//import com.thomasdiewald.pixelflow.java.geometry.DwHalfEdge;
//
///**
// * Integer Pair, can be used as for HashMap keys, 
// * e.g. for HalfEdge pair finding, etc...
// * or general, edge generation
// * http://stackoverflow.com/questions/7032961/java-how-to-use-a-pair-of-keys-for-hashmap
// * 
// * @author thomas diewald
// */
//public class DwIntegerPair {
//  
//  private final Integer a, b;
//  private final int hashcode;
//  
//  public DwIntegerPair(final Integer a, final Integer b) { 
//    this.a = a; 
//    this.b = b; 
//    this.hashcode = a.hashCode() + b.hashCode()*31;
//  }
//  
//  @Override  
//  public final int hashCode() { return hashcode; }
//  
//  @Override
//  public final boolean equals(Object o) {
//    return equals((DwIntegerPair)o);
//  }
//  public final boolean equals(final DwIntegerPair e){
//    return  (a.equals(e.a) && b.equals(e.b));
//  }
//  
//  
//  // comfort methods
//  public static final DwIntegerPair AB(final DwHalfEdge.Edge e){  return new DwIntegerPair( e.vert, e.next.vert ); }
//  public static final DwIntegerPair BA(final DwHalfEdge.Edge e){  return new DwIntegerPair( e.next.vert, e.vert ); }
//}