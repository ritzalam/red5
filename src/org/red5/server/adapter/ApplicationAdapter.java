package org.red5.server.adapter;

import static org.red5.server.api.ScopeUtils.isApp;
import static org.red5.server.api.ScopeUtils.isRoom;

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IBroadcastStreamService;
import org.red5.server.api.stream.IOnDemandStream;
import org.red5.server.api.stream.IOnDemandStreamService;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.api.stream.ISubscriberStreamService;
import org.red5.server.so.ScopeWrappingSharedObjectService;
import org.red5.server.stream.ScopeWrappingStreamManager;

public class ApplicationAdapter extends StatefulScopeWrappingAdapter
	implements ISharedObjectService, IBroadcastStreamService, IOnDemandStreamService, ISubscriberStreamService {

	protected static Log log =
        LogFactory.getLog(ApplicationAdapter.class.getName());

	protected ISharedObjectService sharedObjectService;
	protected ScopeWrappingStreamManager streamService;

	@Override
	public void setScope(IScope scope) {
		super.setScope(scope);
		sharedObjectService = new ScopeWrappingSharedObjectService(scope);
		streamService = new ScopeWrappingStreamManager(scope);
	}

	public boolean connect(IConnection conn, IScope scope) {
		if(isApp(scope)) return appConnect(conn);
		else if(isRoom(scope)) return roomConnect(conn);
		else return false;
	}

	public boolean start(IScope scope) {
		if(isApp(scope)) return appStart(scope);
		else if(isRoom(scope)) return roomStart(scope);
		else return false;
	}

	public void disconnect(IConnection conn, IScope scope) {
		if(isApp(scope)) appDisconnect(conn);
		else if(isRoom(scope)) roomDisconnect(conn);
	}

	public void stop(IScope scope) {
		if(isApp(scope)) appStop(scope);
		else if(isRoom(scope)) roomStop(scope);
	}

	public boolean join(IClient client, IScope scope) {
		if(isApp(scope)) return appJoin(client, scope);
		else if(isRoom(scope)) return roomJoin(client, scope);
		else return false;
	}

	public void leave(IClient client, IScope scope) {
		if(isApp(scope)) appLeave(client, scope);
		else if(isRoom(scope)) roomLeave(client, scope);
	}

	public boolean appStart(IScope app){
		log.debug("appStart: " + app);
		return true;
	}

	public void appStop(IScope app){
		// do nothing
		log.debug("appStop: " + app);
	}
	
	public boolean roomStart(IScope room){
		log.debug("roomStart: " + room);
		return true;
	}
	
	public void roomStop(IScope room){
		log.debug("roomStop: " + room);
		//	do nothing
	}
	
	public boolean appConnect(IConnection conn){
		log.debug("appConnect: "+conn);
		return true;
	}
	
	public boolean roomConnect(IConnection conn){
		log.debug("roomConnect: "+conn);
		return true;
	}
	
	public void appDisconnect(IConnection conn){
		// do nothing
		log.debug("appDisconnect: "+conn);
	}
	
	public void roomDisconnect(IConnection conn){
		// do nothing
		log.debug("roomDisconnect: "+conn);
	}
	
	public boolean appJoin(IClient client, IScope app){
		log.debug("appJoin: "+client+" >> "+app);
		return true;
	}
	
	public void appLeave(IClient client, IScope app){
		log.debug("appLeave: "+client+" << "+app);
	}
	
	public boolean roomJoin(IClient client, IScope room){
		log.debug("roomJoin: "+client+" >> "+room);
		return true;
	}
	
	public void roomLeave(IClient client, IScope room){
		log.debug("roomLeave: "+client+" << "+room);
	}
	
	/* Wrapper around ISharedObjectService */
	
	public boolean createSharedObject(String name, boolean persistent) {
		return sharedObjectService.createSharedObject(name, persistent);
	}

	public ISharedObject getSharedObject(String name) {
		return sharedObjectService.getSharedObject(name);
	}

	public Iterator<String> getSharedObjectNames() {
		return sharedObjectService.getSharedObjectNames();
	}

	public boolean hasSharedObject(String name) {
		return sharedObjectService.hasSharedObject(name);
	}
	
	/* Wrapper around the stream interfaces */
	
	public boolean hasBroadcastStream(String name) {
		return streamService.hasBroadcastStream(name);
	}

	public IBroadcastStream getBroadcastStream(String name) {
		return streamService.getBroadcastStream(name);
	}
	
	public Iterator<String> getBroadcastStreamNames() {
		return streamService.getBroadcastStreamNames();
	}
	
	public boolean hasOnDemandStream(String name) {
		return streamService.hasOnDemandStream(name);
	}

	public IOnDemandStream getOnDemandStream(String name) {
		return streamService.getOnDemandStream(name);
	}

	public ISubscriberStream getSubscriberStream(String name) {
		return streamService.getSubscriberStream(name);
	}
}