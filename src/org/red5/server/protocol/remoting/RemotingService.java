package org.red5.server.protocol.remoting;

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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.io.Deserializer;
import org.red5.server.io.Serializer;
import org.red5.server.io.amf.Input;
import org.red5.server.io.amf.Output;
import org.red5.server.service.ServiceInvoker;

public class RemotingService {

	private static Log log = LogFactory.getLog(RemotingService.class); 
	
	protected ServiceInvoker invoker; 
	protected Serializer serializer;
	protected Deserializer deserializer;
	
	public RemotingService(){
		serializer = new Serializer();
		deserializer = new Deserializer();
	}
	
	public ByteBuffer handleRequest(ByteBuffer in){
		log.debug("Handle request");
				
		ByteBuffer out = ByteBuffer.allocate(2048);
		out.setAutoExpand(true);
		skipHeaders(in); 
		List calls = decodeCalls(in);
		Iterator it = calls.iterator();
		out.putShort((short) 1); // write the version
		out.putShort((short) 0); // write the header count
		out.putShort((short) calls.size()); // write the number of bodies
		Output output = new Output(out);
		while(it.hasNext()){
			log.debug("Call");
			RemotingCall call = (RemotingCall) it.next();
			invoker.invoke(call);
			Output.putString(out,call.getClientResponse());
	   	   	Output.putString(out,"null");
	   	   	out.putInt(-1);
	   	   	serializer.serialize(output, call.getClientResult());
	   	}
		return out;
	}
	
	protected void skipHeaders(ByteBuffer in){
		log.debug("Skip headers");
		int version = in.getUnsignedShort(); // skip the version
		int count = in.getUnsignedShort();
		log.debug("Version: "+version);
		log.debug("Count: "+count);
		for(int i=0; i<count; i++){
			log.debug("Header: "+Input.getString(in));
			boolean required = in.get() == 0x01;
			log.debug("Required: "+required);
			in.skip(in.getInt());
		}
	}
	
    protected List decodeCalls(ByteBuffer in)  {
        log.debug("Decode calls");
        //in.getInt();
    		List calls = new LinkedList();
    		Input input = new Input(in);
        int count = in.getUnsignedShort();
        log.debug("Calls: "+count);
        int limit = in.limit();
        
        // Loop over all the body elements
        for (int i = 0; i < count; i++) {
            
        		in.limit(limit);
        	    input.reset();
        	    
            String serviceString = Input.getString(in);
            String clientCallback =  Input.getString(in);
            int length = in.getInt();
           
            // set the limit and deserialize
            in.limit(in.position()+length);
            Object value = deserializer.deserialize(input);
            
            String serviceName;
            String serviceMethod;
            int dotPos = serviceString.indexOf(".");
            if(dotPos!=-1){
            		serviceName = serviceString.substring(0, dotPos);
            		serviceMethod = serviceString.substring(dotPos+1, serviceString.length());
            } else {
            		serviceName = serviceString;
            		serviceMethod = "";
            }
            
            log.debug("Service: "+serviceName+" Method: "+serviceMethod);
            Object[] args = null;
            if(value instanceof Object[]){
            		args = (Object[]) value;
            } else {
            		args = new Object[]{value};
            }
            // Add the call to the list
            calls.add(new RemotingCall(serviceName, serviceMethod, args, clientCallback));   
        }

        return calls;
        
    }
}
