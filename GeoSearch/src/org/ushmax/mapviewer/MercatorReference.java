package org.ushmax.mapviewer;

import android.graphics.Point;
import android.graphics.PointF;

public class MercatorReference {
  public static void toGeo(int x, int y, int zoom, PointF result) {
    double px = x;
    double py = y;
    final double scale = Math.pow(2, zoom);

    px /= scale;
    py /= scale;

    double lng = 180 * (px / 128 - 1.0f);
    double lat_rad = 2 * (Math.atan(Math.exp(
        Math.PI * (1 - py / 128))) - Math.PI / 4);
    double lat = lat_rad * 180 / Math.PI;
    result.x = (float)lat;
    result.y = (float)lng;
  }

  public static void fromGeo(float lat, float lng, int zoom, Point result) {
    double px = 128 * (1.0 + lng / 180);
    // Mercator projection
    double lat_rad = lat * Math.PI / 180;
    double py = 128 * (1 - (
        Math.log(Math.tan(lat_rad / 2 + Math.PI / 4)) / Math.PI
    ));
    final double scale = Math.pow(2, zoom);

    px *= scale;
    py *= scale;

    result.x = (int)px;
    result.y = (int)py;
  }
 
  public static float metersToPixels(float distance, float lat, float lng, int zoom) {
    double equator = 2 * Math.PI * MyMath.RADIUS_EARTH_METERS;
    double lat_rad = lat * Math.PI / 360;
    double dist_lng = distance / (equator * Math.cos(lat_rad)); 
    float px = (float) (256 * dist_lng);
    px *= Math.pow(2, zoom);
    return px;
  }
}