/*
 * demoservice.js - a translation into JavaScript of the olfa demo DemoService class, a red5 example.
 *
 * @see http://developer.mozilla.org/en/docs/Core_JavaScript_1.5_Reference
 * @author Paul Gregoire
 */

importPackage(Packages.org.red5.server.api);
importPackage(Packages.org.springframework.core.io);
importPackage(Packages.org.apache.commons.logging);

importClass(java.io.File);
importClass(java.util.HashMap);
importClass(java.text.SimpleDateFormat);
importClass(Packages.org.springframework.core.io.Resource);
importClass(Packages.org.red5.server.api.Red5);

//class impersonator
function DemoService() {
	this.className = 'DemoService';
	log.debug('DemoService init');

    for (property in this) {
		try {
			print('>>' + property);
		} catch(e) {
			e.rhinoException.printStackTrace();
		}	
	}

	this.getListOfAvailableFLVs = function() {
		log.debug('getListOfAvailableFLVs');
		log.debug('Con local: ' + Red5.getConnectionLocal());
		var scope = Red5.getConnectionLocal().getScope();
		log.debug('Scope: ' + scope);
		var filesMap = new HashMap();
		var fileInfo;
		try {
			print('Getting the FLV files');
			var flvs = scope.getResources("streams/*.flv"); //Resource[]
			log.debug('Flvs: ' + flvs);
			log.debug('Number of flvs: ' + flvs.length);
			for (var i=0;i<flvs.length;i++) {
				var file = flvs[i];
				log.debug('file: ' + file);
				log.debug('java.io.File type: ' + (file == typeof(java.io.File)));
				log.debug('js type: ' + typeof(file));
				log.debug('file path: ' + file.path);
				log.debug('file url: ' + file.URL);
				var serverRoot = java.lang.System.getProperty('red5.root');
				log.debug('Red5 root: ' + serverRoot);
				var fso = new File(serverRoot + '/webapps/oflaDemo' + file.path);
				var flvName = fso.getName();
				log.debug('flvName: ' + flvName);
				log.debug('exist: ' + fso.exists());
				log.debug('readable: ' + fso.canRead());
				//loop thru props
				var flvBytes = 0;
				if ('length' in fso) {
					flvBytes = fso.length();
				} else {
					log.debug('Length not found');
				}
				log.debug('flvBytes: ' + flvBytes);
				var lastMod = '0';
				if ('lastModified' in fso) {
					lastMod = this.formatDate(new java.util.Date(fso.lastModified()));
				} else {
					log.debug('Last modified not found');
				}
	
				print('FLV Name: ' + flvName);
				print('Last modified date: ' + lastMod);
				print('Size: ' + flvBytes);
				print('-------');
				
				fileInfo = new HashMap(3);
				fileInfo.put("name", flvName);
				fileInfo.put("lastModified", lastMod);
				fileInfo.put("size", flvBytes);
				filesMap.put(flvName, fileInfo);
			}
		} catch (e) {
			log.debug('Error in getListOfAvailableFLVs: ' + e);
			//print('Exception: ' + e);
		}
		return filesMap;
	};

}

DemoService.prototype.formatDate = function(date) {
	log.debug('formatDate');
	//java 'thread-safe' date formatting
	return new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(date);
};	

