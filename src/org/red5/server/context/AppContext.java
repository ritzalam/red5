package org.red5.server.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.flv.FLVServiceImpl;
import org.red5.server.SharedObjectPersistence;
import org.red5.server.SharedObjectRamPersistence;
import org.red5.server.service.ServiceInvoker;
import org.red5.server.stream.StreamManager;
import org.springframework.beans.BeansException;

public class AppContext 
	extends GenericRed5Context {
	
	public static final String APP_CONFIG = "app.xml";
	public static final String APP_SERVICE_NAME = "appService";
	public static final String SO_PERSISTENCE_NAME = "sharedObjectPersistence";
	public static final String STREAM_MANAGER_NAME = "streamManager";
	
	protected String appPath;
	protected String appName;
	protected HostContext host;
		
	protected static Log log =
        LogFactory.getLog(AppContext.class.getName());
	
	public AppContext(HostContext host, String appName, String appPath) throws BeansException {
		super(host, appPath, appPath + "/" + APP_CONFIG ); 
		this.appName = appName;
		this.appPath = appPath;
	}
	
	public void initialize() {
		
		super.initialize();
		
		StreamManager streamManager = null;
		if(!this.containsBean(STREAM_MANAGER_NAME)){
			streamManager = new StreamManager();
			this.getBeanFactory().registerSingleton(STREAM_MANAGER_NAME, streamManager);
			
			//streamManager.initialize();
			streamManager.setApplicationContext(this);
			// this will need to be refactored
			streamManager.setFlvService(new FLVServiceImpl());
		} else {
			streamManager = (StreamManager) this.getBean(STREAM_MANAGER_NAME);
		}
		
		BaseApplication app = null;
		if(!this.containsBean(APP_SERVICE_NAME)){
			app = new BaseApplication();
			app.setApplicationContext(this);
			this.getBeanFactory().registerSingleton(APP_SERVICE_NAME, app);
			app.setStreamManager(streamManager);
			app.initialize();
		} else {
			app = (BaseApplication) this.getBean(APP_SERVICE_NAME);
			app.setApplicationContext(this);
			app.setStreamManager(streamManager);
			app.initialize();
		}
		
		SharedObjectPersistence soPersistence = null;
		if(!this.containsBean(SO_PERSISTENCE_NAME)){
			soPersistence = new SharedObjectRamPersistence();
			soPersistence.setApplicationContext(this);
			app.setSharedObjectPersistence(soPersistence);
		} else {
			soPersistence = (SharedObjectPersistence) this.getBean(SO_PERSISTENCE_NAME);
			soPersistence.setApplicationContext(this);
			app.setSharedObjectPersistence(soPersistence);
		}
	}
	
	public ServiceInvoker getServiceInvoker(){
		return (ServiceInvoker) getBean(ServiceInvoker.SERVICE_NAME);
	}
}
