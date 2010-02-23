package org.ushmax.mapviewer;

public class MyMath {
  public static final float DEG2RAD = (float)(Math.PI / 180.0);
  public static final float RAD2DEG = (float)(180.0 / Math.PI);

  public static final float PI = (float)Math.PI;
  public static final float PI_2 = PI / 2.0f;
  public static final float PI_4 = PI / 4.0f;

  public static final int RADIUS_EARTH_METERS = 6378140;

  public static double gudermannInverse(double aLatitude){
    return Math.log(Math.tan(PI_4 + (DEG2RAD * aLatitude / 2)));
  }

  public static double gudermann(double y){
    return RAD2DEG * Math.atan(Math.sinh(y));
  }

  // REQUIRES: divisor > 0
  public static int div(int number,final int divisor) {
    if (number >= 0) {
      return number / divisor;
    }
    return number / divisor - 1;
  }

  // REQUIRES: modulus > 0
  public static int mod(int number, final int modulus){
    if(number >= 0) {
      return number % modulus;
    }
    return number % modulus + modulus;
  }
}
