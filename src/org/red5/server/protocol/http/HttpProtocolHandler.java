package org.red5.server.protocol.http;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.mina.io.IoSession;

/**
 * A simplistic HTTP protocol handler that replies back the URL and headers
 * which a client requested.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev: 264677 $, $Date: 2005-08-30 11:44:35 +0900 $
 */
public class HttpProtocolHandler extends HttpStreamIoHandler
{
    protected void processStreamIo( IoSession session, InputStream in,
                                    OutputStream out )
    {
        // You *MUST* execute stream I/O logic in a separate thread.
        new Worker( in, out ).start();
    }
    
    private static class Worker extends Thread
    {
        private final InputStream in;
        private final OutputStream out;
        
        public Worker( InputStream in, OutputStream out )
        {
            setDaemon( true );
            this.in = in;
            this.out = out;
        }
        
        public void run()
        {
            String url;
            Map headers = new TreeMap();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader( this.in ) );
            PrintWriter out = new PrintWriter(
                    new BufferedWriter( new OutputStreamWriter( this.out ) ) );
            
            try
            {
                // Get request URL.
                url = in.readLine().split( " " )[1];
                
                // Read header
                String line;
                while ( ( line = in.readLine() ) != null && !line.equals( "" ) )
                {
                    String[] tokens = line.split(": ");
                    headers.put( tokens[0], tokens[1] );
                }
                
                // Write header
                out.println( "HTTP/1.0 200 OK" );
                out.println( "Content-Type: text/html" );
                out.println( "Server: MINA Example" );
                out.println();
                
                // Write content
                out.println( "<html><head></head><body>" );
                out.println( "<h3>Request Summary for: " + url + "</h3>" );
                out.println( "<table border=\"1\"><tr><th>Key</th><th>Value</th></tr>" );
                
                Iterator it = headers.entrySet().iterator();
                while( it.hasNext() )
                {
                    Entry e = ( Entry ) it.next();
                    out.println( "<tr><td>" + e.getKey() + "</td><td>" + e.getValue() + "</td></tr>" );
                }
                
                out.println( "</table>" );
                out.println( "</body></html>" );
                
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
            finally
            {
                out.flush();
                out.close();
                try
                {
                    in.close();
                }
                catch( IOException e )
                {
                }
            }
        }
    }
}
