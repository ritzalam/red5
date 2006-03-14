package org.red5.server.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;

public class VideoCodecFactory {

	private Log log = LogFactory.getLog(VideoCodecFactory.class.getName());

	private List codecs = new ArrayList();
	
	public void setCodecs(List codecs) {
		this.codecs = codecs;
	}
	
	public IVideoStreamCodec getVideoCodec(ByteBuffer data) {
		IVideoStreamCodec result = null;
		Iterator it = this.codecs.iterator();
		while (it.hasNext()) {
			IVideoStreamCodec codec;
			IVideoStreamCodec storedCodec = (IVideoStreamCodec) it.next();
			// XXX: this is a bit of a hack to create new instances of the configured
			//      video codec for each stream
			try {
				codec = (IVideoStreamCodec) storedCodec.getClass().newInstance();
			} catch (Exception e) {
				log.error("Could not create video codec instance.", e);
				continue;
			}
			
			log.info("Trying codec " + codec);
			if (codec.canHandleData(data)) {
				result = codec;
				break;
			}
		}
		
		// No codec for this video data
		return result;
	}
}
