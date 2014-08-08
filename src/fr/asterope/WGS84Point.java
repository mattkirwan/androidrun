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

/**
 * This class is a storage for basic GPS WGS84 point. It coordinates are
 * represented in DECIMAL W/E notation e.g. : latitude = 48.85341 N longitude =
 * 2.3488 E
 *
 * Altitude is stored in meter.
 *
 * @author bruno
 */
public class WGS84Point
{

    private double latitude = 0.0;
    private double longitude = 0.0;
    private double altitude = 0.0;


    /**
     * Constructor.
     *
     * @param lati is latitude in decimal degree.
     * @param longi is longitude in decimal degree.
     * @param alti is altitude in meters.
     */
    public WGS84Point(double lati, double longi, double alti)
    {
        latitude = lati;
        longitude = longi;
        altitude = alti;
    }


    /**
     * Constructor with no altitude. Point altitude will be set to 0.
     *
     * @param lati is latitude in decimal degree.
     * @param longi is longitude in decimal degree.
     */
    public WGS84Point(double lati, double longi)
    {
        this(lati, longi, 0.0);
    }


    /**
     * Constructor with default parameters.
     */
    public WGS84Point()
    {
        this(0.0, 0.0, 0.0);
    }


    /**
     * Return a String representation of the point. Altitude isn't shown.
     * @return
     */
    @Override
    public String toString()
    {
        return latitude + " N / " + longitude + " E";
    }


    /**
     * Latitude getter. Value is returned in decimal degree.
     *
     * @return Latitude.
     */
    public double getLatitude()
    {
        return latitude;
    }


    /**
     * Latitude setter: Value is expected in decimal degree.
     *
     * @param latitude in decimal degree.
     */
    public void setLatitude(double latitude)
    {
        this.latitude = latitude;
    }


    /**
     * Longitude getter. Value is returned in decimal degree.
     *
     * @return Longitude.
     */
    public double getLongitude()
    {
        return longitude;
    }


    /**
     * Longitude setter: Value is expected in decimal degree.
     *
     * @param longitude in decimal degree.
     */
    public void setLongitude(double longitude)
    {
        this.longitude = longitude;
    }


    /**
     * Altitude getter. Value is returned in meters.
     *
     * @return Altitude.
     */
    public double getAltitude()
    {
        return altitude;
    }


    /**
     * Altitude setter: Value is expected in meters.
     *
     * @param altitude in meters.
     */
    public void setAltitude(double altitude)
    {
        this.altitude = altitude;
    }
}
