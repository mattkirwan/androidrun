/**
 * 
 * AndroidRun, basic runner's android application. Calculates distance, speed
 * and other usefull values taken from GPS device.
 * 
 * Copyright (C) 2014 Bruno Vedder
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 *
 *
 * Most equation were taken from the wikipedia article:
 * https://en.wikipedia.org/wiki/Great-circle_distance
 *
 * https://en.wikipedia.org/wiki/Vincenty%27s_formulae
 *
 */

package fr.asterope;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


/**
 *
 * @author bruno
 */
public class WGS84
{

    public static final int GREAT_CIRCLE = 1;
    public static final int HAVERSINE = 2;
    public static final int VINCENTY = 3;
    public static final double EQUATOR_EARTH_RADIUS_KM = 6378.137;
    public static final double POLAR_EARTH_RADIUS_KM = 6356.752;
    public static final double AVERAGE_VOLUMIC_EARTH_RADIUS_KM = 6371.0;


    /**
     * Convert position (Latitude or Longitude) to Radian.
     *
     * @param posInDegree : Position in degree to convert to radian.
     * @return position in Radian.
     */
    static public double toRadian(double posInDegree)
    {
        return posInDegree * Math.PI / 180.0;
    }


    /**
     * Return a point that is the center of the two given points as parameters.
     *
     * @param p1
     * @param p2
     * @return center of the 2 points.
     */
    static WGS84Point getCenter(WGS84Point p1, WGS84Point p2)
    {
        double latCent = (p1.getLatitude() + p2.getLatitude()) / 2.0;
        double longCent = (p1.getLongitude() + p2.getLongitude()) / 2.0;
        double altCent = (p1.getAltitude() + p2.getAltitude()) / 2.0;
        return new WGS84Point(latCent, longCent, altCent);
    }


    /**
     * Convert position (Latitude or Longitude) to Degree.
     *
     * @param posInRadian : Position to convert to Degree
     * @return position in Degree.
     */
    static public double toDegre(double posInRadian)
    {
        return posInRadian / Math.PI * 180.0;
    }


    /**
     * Compute the shortest distance between points (lat1, long1) and (lat2,
     * long2) in the WGS84 geodesic system. Position are expected in Degree. It
     * uses the Great Circles algorithm.
     *
     * Note : The point MUSTN'T be antipodal, otherwise zero distance is
     * returned.
     *
     * @param lat1 : point 1 latitude.
     * @param long1: point 1 longitude.
     * @param lat2 : point 2 latitude.
     * @param long2: point 2 longitude.
     * @return distance in kilometer.
     */
    static double distance(double lat1, double long1, double lat2, double long2)
    {
        lat1 = WGS84.toRadian(lat1);
        long1 = WGS84.toRadian(long1);
        lat2 = WGS84.toRadian(lat2);
        long2 = WGS84.toRadian(long2);

        // Avoid NaN for position latitudes too close.
        if (Math.abs(lat1 - lat2) < 0.0000001)
        {
            return 0;
        }
        double dist = EQUATOR_EARTH_RADIUS_KM * Math.acos(Math.cos(lat1) * Math.cos(lat2) * Math.cos(long2 - long1) + Math.sin(lat1) * Math.sin(lat2));
        return dist;
    }


    /**
     * Compute the shortest distance between 2 WGPS84Points in the WGS84
     * geodesic system. Positions are expected in Degree. It uses the Great
     * Circles algorithm.
     *
     * Note : The point MUSTN'T be antipodal, otherwise zero distance is
     * returned.
     *
     * @param lat1 : source point.
     * @param long1: destination point.
     * @return distance in kilometer.
     */
    static double distance(WGS84Point src, WGS84Point dest)
    {
        double lat1 = src.getLatitude();
        double long1 = src.getLongitude();
        double lat2 = dest.getLatitude();
        double long2 = dest.getLongitude();

        return distance(lat1, long1, lat2, long2);
    }


    /**
     * Compute the shortest distance between points (lat1, long1) and (lat2,
     * long2) in the WGS84 geodesic system using Haversine formula. Position are
     * expected in Degree. This method is more accurate for short distance.
     *
     * Note : The point MUSTN'T be antipodal, otherwise zero distance is
     * returned.
     *
     * @param lat1 : point 1 latitude.
     * @param long1: point 1 longitude.
     * @param lat2 : point 2 latitude.
     * @param long2: point 2 longitude.
     * @return distance in kilometer.
     */
    static double haversineDistance(double lat1, double long1, double lat2, double long2)
    {
        lat1 = WGS84.toRadian(lat1);
        long1 = WGS84.toRadian(long1);
        lat2 = WGS84.toRadian(lat2);
        long2 = WGS84.toRadian(long2);

        double sinSquareLat = Math.sin((lat1 - lat2) / 2.0);
        sinSquareLat *= sinSquareLat;

        double cosLat = Math.cos(lat1) * Math.cos(lat2);

        double sinSquareLong = Math.sin((long1 - long2) / 2.0);
        sinSquareLong *= sinSquareLong;

        double dist = EQUATOR_EARTH_RADIUS_KM * 2.0 * Math.asin(Math.sqrt(sinSquareLat + cosLat * sinSquareLong));
        return dist;
    }


    /**
     * Compute the shortest distance between 2 WGPS84Points in the WGS84
     * geodesic system. Positions are expected in Degree.
     *
     * Note : The point MUSTN'T be antipodal, otherwise zero distance is
     * returned.
     *
     * @param lat1 : source point.
     * @param long1: destination point.
     * @return distance in kilometer.
     */
    static double haversineDistance(WGS84Point src, WGS84Point dest)
    {
        double lat1 = src.getLatitude();
        double long1 = src.getLongitude();
        double lat2 = dest.getLatitude();
        double long2 = dest.getLongitude();

        return haversineDistance(lat1, long1, lat2, long2);
    }


    /**
     * Compute the shortest distance between points (lat1, long1) and (lat2,
     * long2) in the WGS84 geodesic system using Vincenty formula. Position are
     * expected in Degree. This method is more accurate for short distance.
     *
     * @param lat1 : point 1 latitude.
     * @param long1: point 1 longitude.
     * @param lat2 : point 2 latitude.
     * @param long2: point 2 longitude.
     * @return distance in kilometer.
     */
    static double vincentyDistance(double lat1, double long1, double lat2, double long2)
    {

        lat1 = WGS84.toRadian(lat1);
        long1 = WGS84.toRadian(long1);
        lat2 = WGS84.toRadian(lat2);
        long2 = WGS84.toRadian(long2);

        double a = EQUATOR_EARTH_RADIUS_KM;
        double f = 1.0 / 298.257223563;
        double b = (1.0 - f) * a;
        double L = long2 - long1;

        double tanU1 = (1.0 - f) * Math.tan(lat1);
        double cosU1 = 1.0 / Math.sqrt((1.0 + tanU1 * tanU1));
        double sinU1 = tanU1 * cosU1;

        double tanU2 = (1.0 - f) * Math.tan(lat2);
        double cosU2 = 1.0 / Math.sqrt((1.0 + tanU2 * tanU2));
        double sinU2 = tanU2 * cosU2;

        double lambda = L;
        double lambdaPrime;
        double iterationLimit = 100.0;

        double cosSqalpha;
        double cossigma;
        double sinlambda;
        double sigma;
        double sinSqsigma;
        double sinsigma;
        double cos2sigmaM;
        double coslambda;
        double sinalpha;

        do
        {
            sinlambda = Math.sin(lambda);
            coslambda = Math.cos(lambda);
            sinSqsigma = (cosU2 * sinlambda) * (cosU2 * sinlambda) + (cosU1 * sinU2 - sinU1 * cosU2 * coslambda) * (cosU1 * sinU2 - sinU1 * cosU2 * coslambda);

            sinsigma = Math.sqrt(sinSqsigma);

            if (sinsigma == 0.0)
            {
                return 0.0;  // co-incident points
            }

            cossigma = sinU1 * sinU2 + cosU1 * cosU2 * coslambda;
            sigma = Math.atan2(sinsigma, cossigma);
            sinalpha = cosU1 * cosU2 * sinlambda / sinsigma;
            cosSqalpha = 1.0 - sinalpha * sinalpha;
            cos2sigmaM = cossigma - 2.0 * sinU1 * sinU2 / cosSqalpha;
            if (Double.isNaN(cos2sigmaM))
            {
                cos2sigmaM = 0.0;  // equatorial line: cosSqalpha=0 (§6)
            }
            double C = f / 16.0 * cosSqalpha * (4.0 + f * (4.0 - 3.0 * cosSqalpha));
            lambdaPrime = lambda;
            lambda = L + (1.0 - C) * f * sinalpha * (sigma + C * sinsigma * (cos2sigmaM + C * cossigma * (-1.0 + 2.0 * cos2sigmaM * cos2sigmaM)));
        }
        while (Math.abs(lambda - lambdaPrime) > 1e-12 && --iterationLimit > 0);

        if (iterationLimit == 0)
        {
            return -1.0;
        }

        double uSq = cosSqalpha * (a * a - b * b) / (b * b);
        double A = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)));
        double B = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)));
        double deltasigma = B * sinsigma * (cos2sigmaM + B / 4.0 * (cossigma * (-1.0 + 2.0 * cos2sigmaM * cos2sigmaM)
                - (B / 6.0 * cos2sigmaM * (-3.0 + 4.0 * sinsigma * sinsigma) * (-3.0 + 4.0 * cos2sigmaM * cos2sigmaM))));

        double s = b * A * (sigma - deltasigma);
        return s;
    }


    /**
     * Compute the shortest distance between 2 WGPS84Points in the WGS84
     * geodesic system. Positions are expected in Degree.
     *
     * @param lat1 : source point.
     * @param long1: destination point.
     * @return distance in kilometer.
     */
    static double vincentyDistance(WGS84Point src, WGS84Point dest)
    {
        double lat1 = src.getLatitude();
        double long1 = src.getLongitude();
        double lat2 = dest.getLatitude();
        double long2 = dest.getLongitude();

        return vincentyDistance(lat1, long1, lat2, long2);
    }


    /**
     * Convert : 121,136° to sexagesimal represntation : 121°8'9.6"
     *
     * @param degree
     * @return String containing Sexagesimal representation of given value.
     */
    static public String toSexagesimal(double degree)
    {
        int deg = (int) degree;
        double fPart = degree - deg;
        double minutes = fPart * 60.0;
        int min = (int) minutes;
        fPart = minutes - min;
        double sec = fPart * 60.0;

        // Keep 2 digits max.
        sec = Math.round(sec * 100);
        sec = sec / 100;

        String formated = deg + "°" + min + "'" + Double.toString(sec) + "\"";
        return formated;
    }


    /**
     * Read a List of postion from a text file, and return data as WGS84Point
     * list. Expected file format: Latitude1 Longitude1 in decimal degree. One
     * couple of value per line, no more.
     *
     * @param absoluteFilename
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    static public ArrayList<WGS84Point> loadPosition(String absoluteFilename) throws FileNotFoundException, IOException
    {
        ArrayList<WGS84Point> pos = new ArrayList<WGS84Point>(10000);

        // Open File and load couples of values.
        double d;
        FileReader fileReader = new FileReader(absoluteFilename);
        BufferedReader reader = null;

        try
        {
            reader = new BufferedReader(fileReader);
            String line = reader.readLine();

            while (line != null)
            {
                double lati, longi;

                Scanner s = new Scanner(line);
                lati = s.nextDouble();
                longi = s.nextDouble();
                pos.add(new WGS84Point(lati, longi));
                line = reader.readLine();
            }
        }
        catch (IOException e)
        {
            reader = null;
        }
        return pos;
    }


    /**
     * Compute integral distance over a track. Sum distance between each points.
     * Distance between each point can be calculated with different algorithm.
     *
     * @param points : Point measured along the track.
     * @param algorithm : Great-Circle, Haversine, Vincenty.
     * @return distance in kilometers.
     */
    public static double computeTrackDistance(ArrayList<WGS84Point> points, int algorithm)
    {
        double cumul = 0.0;

        WGS84Point start = points.get(0);
        double dist = 0.0;

        for (int i = 1; i < points.size(); i++)
        {
            WGS84Point dest = points.get(i);

            switch (algorithm)
            {
                case GREAT_CIRCLE:
                    dist += distance(start, dest);
                    break;

                case HAVERSINE:
                    dist += haversineDistance(start, dest);
                    break;

                case VINCENTY:
                    dist += WGS84.vincentyDistance(start, dest);
                    break;

                default:
                    dist = -1.0;
                    break;
            }

            cumul = cumul + dist;
            start = dest; // Destination becomes next start point.
        }
        return dist;
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        // TODO code application logic here
        double latitude = 44.88374;
        double rad = WGS84.toRadian(latitude);
        System.out.println(latitude + " to radian = " + rad);

        latitude = rad;
        double deg = WGS84.toDegre(latitude);
        System.out.println(latitude + " to degree = " + deg);

        WGS84Point paris = new WGS84Point(48.8588589, 2.3470599);
        WGS84Point laMure = new WGS84Point(44.9102669, 5.7860659);

        double distance = WGS84.distance(paris, laMure);
        double distanceHaversine = WGS84.haversineDistance(paris, laMure);
        double distanceVincenty = WGS84.vincentyDistance(laMure, paris);

        System.out.println("Paris -> La Mure = " + distance + " km(s).");
        System.out.println("Paris -> La Mure = " + distanceHaversine + " km(s) (Haversine).");
        System.out.println("Paris -> La Mure = " + distanceVincenty + " km(s) (Vincenty).");
        System.out.println("121.136° is " + WGS84.toSexagesimal(121.136) + " in sexagesimal representation.");

        String sourceFile = "/home/bruno/Data-Position.txt";

        ArrayList<WGS84Point> dataPos;

        try
        {
            dataPos = loadPosition(sourceFile);
            System.out.println((dataPos.size()) + " position read from " + sourceFile);

            for (int u = 1; u < 150; u++)
            {
                ArrayList<WGS84Point> underSampled = new ArrayList<WGS84Point>(dataPos.size());

                for (int i = 0; i < dataPos.size(); i += u)
                {
                    underSampled.add(dataPos.get(i));
                }
                double gc = computeTrackDistance(underSampled, GREAT_CIRCLE);
                double h = computeTrackDistance(underSampled, HAVERSINE);
                double vinc = computeTrackDistance(underSampled, VINCENTY);

                System.out.println(gc + "; " + h + "; " + vinc + ";");

            }
        }
        catch (IOException ex)
        {
            System.out.println("Got exception !");
        }

        WGS84Point p1 = new WGS84Point(2.0, 2.0, 2.0);
        WGS84Point p2 = new WGS84Point(1.0, 1.0, 1.0);

        WGS84Point center = WGS84.getCenter(p2, p1);
        System.out.println("Central point is " + center.toString());
    }
}
