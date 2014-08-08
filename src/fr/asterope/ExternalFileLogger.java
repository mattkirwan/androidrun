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
 */

package fr.asterope;

import android.os.Environment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 *
 * @author bruno
 */
public class ExternalFileLogger
{

    private File logFile = null;
    private boolean logFileReady = false;
    private String filename = null;


    /**
     * Constructor
     *
     * @param filename : filename of the logfile.
     */
    public ExternalFileLogger(String fileName)
    {
        filename = fileName;
        generateLogFile();
    }


    /**
     * Get Handle on the logFile, if possible. Maintains the flag logFileReady
     * accordingly.
     */
    private void generateLogFile()
    {
        if (isExternalStorageWritable() == true)
        {
            try
            {
                logFile = new File(Environment.getExternalStorageDirectory(), filename);
                logFileReady = true;
            }
            catch (Exception e)
            {
                logFileReady = false;
            }
        }
        else
        {
            logFileReady = false;
        }
    }


    /**
     * Perform write operation into the log file. If logfile is not created,
     * try to create it. Date and time are prepended to the message.
     * 
     * @param msg: message to log.
     * @throws java.io.FileNotFoundException
     */
    public void write(String msg) throws FileNotFoundException, IOException
    {
        if (logFileReady == false)
        {
            generateLogFile();
        }
        
        if (logFileReady == true)
        {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String now = sdf.format(new Date());

            FileOutputStream outFile = new FileOutputStream(logFile, true);
            OutputStreamWriter ofw = new OutputStreamWriter(outFile);
            ofw.write(now + " : " + msg + "\n");
            ofw.close();
        }
    }


    /**
     * Perform write in the logFile, without exception.
     *
     * @param msg
     * @return true if write terminates successfully, false otherwise.
     */
    public boolean safeWrite(String msg)
    {
        try
        {
            write(msg);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }


    /**
     * Returns true if an external storage is mounted.
     *
     * @return
     */
    static public boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
