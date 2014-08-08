/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
