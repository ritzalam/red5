import mx.events.ListEvent;

import org.red5.admin.connector.Red5Connector;
import org.red5.admin.connector.event.Red5Event;
import org.red5.admin.panels.Login;
import org.red5.utils.SharedObjectHandler;

[Bindable]
private var _footer:String;

[Bindable]
private var _error:String = "";

[Bindable]
private var _intervals:Array;

[Bindable]
private var _hosts:Array;

[Bindable]
private var _applications:Array;

[Bindable]
private var _scopes:Array;

[Bindable]
private var _users:Array;

[Bindable]
private var _streams:Array;

[Bindable]
private var _userstats:String;

[Bindable]
private var _selectedHost:String;

[Bindable]
private var _scope_stats:Array;

[Bindable]
private var _user_stats:Array;

[Bindable]
private var logoImg:Class;

[Bindable]
private var applicationImg:Class;

[Bindable]
private var applicationGoImg:Class;

[Bindable]
private var userImg:Class;

[Bindable]
private var userDeleteImg:Class;

private var _connector:Red5Connector;
private var sharedObject:SharedObjectHandler;
private var _selectedApp:String
private var _interval:Timer = null;
private var _selectedUser:Number

public function connect (): void {
	//
	logoImg = this.getStyle("Logo");
	applicationImg = this.getStyle("Application");
	applicationGoImg = this.getStyle("ApplicationGo");
	userImg = this.getStyle("User");
	userDeleteImg = this.getStyle("UserDelete");
	//
	_footer = "VMVersion: "+flash.system.System.vmVersion+" | Flash Player: "+flash.system.Capabilities.version+"";
	loggedInInfo.visible = false;
	_intervals = [1,5,10,20,30,60]
	_connector = Red5Connector.getInstance();
	_connector.connectServer()
}

private function initLogin ( event : Event ) : void {
	loginPanel.addEventListener(Login.CONNECTED,startApp);
	loginPanel.addEventListener(Login.CONNECTING,connectingRed5);
	loginPanel.addEventListener(Login.FAILED,failedConnection);
}

private function connectingRed5 ( event : Red5Event) : void {
	flowControll.selectedIndex = 1
}

private function startApp ( event : Red5Event) : void {
	loggedInInfo.visible = true;
	flowControll.selectedIndex = 2
	loadApplications()
	speedChange ( new ListEvent(ListEvent.CHANGE ) );
	_selectedHost = loginPanel.address.text
}

private function loadApplications() : void {
	var responder:Responder = new Responder(fillApplications,null);
	_connector.call ( "getApplications" , responder );
}

private function selectApplication ( event:ListEvent ) : void {
	_selectedApp = _applications[applist.selectedIndex].name;
	trace ( _selectedApp );
	var responder:Responder = new Responder(fillScopes,null);
	_connector.call ( "getScopes" , responder , _selectedApp );
	var responder2:Responder = new Responder(fillUsers,null);
	_connector.call ( "getConnections" , responder2 , _selectedApp );
	getAppStats();
}

public function fillScopes( scopes : Array ):void{
	_scopes = scopes;
}

public function fillUsers(apps:Array):void{
	_users = apps;
	if ( _selectedUser > apps.length && _selectedUser < apps.length ) {
		userList.selectedIndex = _selectedUser
	} 
}

public function fillApplications(apps:Array):void{
	_applications = apps;
	if ( _selectedApp != null ) {
		for (var i:Number = 0 ; i < _applications.length ; i++ ) {
			if ( _applications[i].name == _selectedApp ) {
				applist.selectedIndex = i;
			}
		}
	}
}

private function speedChange(event:ListEvent):void{
	if ( _interval != null ) {
		_interval.stop()
		_interval = null;
	}
	_interval = new Timer( _intervals[intervalSpeed.selectedIndex] * 1000 );
	_interval.addEventListener(TimerEvent.TIMER,refreshData);
	_interval.start()
}

private function refreshData(event:TimerEvent):void{
	loadApplications()
	switch ( statsTab.selectedIndex ) {
		case 0:
			if ( applist.selectedIndex >= 0 ) {
				selectApplication ( new ListEvent(ListEvent.CHANGE) );
			}
			break;
		case 1:
			if ( userList.selectedIndex >= 0 ) {
				selectUser ( new ListEvent(ListEvent.CHANGE) );
			}
			break;
	}
}

private function addAppListeners():void {
	applist.addEventListener(ListEvent.CHANGE,selectApplication);
	intervalSpeed.addEventListener(ListEvent.CHANGE,speedChange);	
}

private function addUserListeners():void {
	userList.addEventListener(ListEvent.CHANGE,selectUser);
}

private function selectUser ( event:ListEvent ) :void {
	var responder2:Responder = new Responder( showUserStatistics ,null);
	_connector.call ( "getUserStatistics" , responder2 , _users[userList.selectedIndex] );
	_selectedUser = userList.selectedIndex
	
}

public function killUser () : void {
	_connector.call ( "killUser" , null , _users[userList.selectedIndex] );
}

private function failedConnection ( event : Red5Event ) : void {
	flowControll.selectedIndex = 0;
	loggedInInfo.visible = false;
}

private function getAppStats():void{
	var name:String = _applications[applist.selectedIndex].name;
	var responder:Responder = new Responder(showStatistics,null);
	_connector.call ( "getStatistics" , responder , name );
}

/*	private function getScopeStats():void{
	var responder:Responder = new Responder(showStatistics,null);
	_connector.call ( "getStatistics" , responder , _scopes[scopelist.selectedIndex] );
}*/


private function showStatistics(data:Array):void{
	_scope_stats = data
}


private function showUserStatistics(data:Array):void{
	_user_stats = data;
}

private function logout(event:MouseEvent):void{
	_connector.close()
	flowControll.selectedIndex = 0;
}