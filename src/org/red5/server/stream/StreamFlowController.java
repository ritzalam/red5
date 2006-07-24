package org.red5.server.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IFlowControllable;

public class StreamFlowController {
	
	private static final Log log = LogFactory.getLog(StreamFlowController.class);

	private static final int FIXED_CHANGE = 1024 * 4;
	
	public boolean adaptBandwidthForFlow(IStreamFlow flow, IFlowControllable controllable){
		
		
		IBandwidthConfigure parentBwConf = controllable.getParentFlowControllable().getBandwidthConfigure();
		IBandwidthConfigure bwConf = controllable.getBandwidthConfigure();
		if(bwConf == null){
			bwConf = parentBwConf.clone();
			controllable.setBandwidthConfigure(bwConf);
		}
		boolean change = false;
		final int bufferTime = flow.getBufferTime();
		long bw = bwConf.getOverallBandwidth();
		if(bufferTime > flow.getMaxTimeBuffer()){
			if(flow.isBufferTimeIncreasing()){ 
				if(bw > flow.getDataBitRate()) bw = flow.getDataBitRate();
				bw -= computeChange(bw);
				change = true;
				//log.info("<<");
			} 
		} else if(bufferTime < flow.getMinTimeBuffer()){
			if(!flow.isBufferTimeIncreasing()){	
				if(bw < flow.getDataBitRate()) bw = flow.getDataBitRate();
				bw += computeChange(bw) * 2;
				change = true;
				//log.info(">>");
			} 
		} else if(bufferTime < ( flow.getMinTimeBuffer() + ((flow.getMaxTimeBuffer() - flow.getMinTimeBuffer()) / 1.5) )){
			if(!flow.isBufferTimeIncreasing()){ 
				if(bw < flow.getDataBitRate() * 0.5) bw = (int) (flow.getDataBitRate() * 1.2);
				bw += computeChange(bw) / 2;
				change = true;
				//log.info(">");
			} 
		} else if(bufferTime < flow.getMaxTimeBuffer()){
			if(flow.isBufferTimeIncreasing()){
				if(bw > flow.getDataBitRate()*1.5) bw = (int) (flow.getDataBitRate() * 1.2);
				bw -= computeChange(bw) / 2;
				change = true;
				//log.info("<");
			} 
		} else {
			//log.info("GOOD!");
		}
		
		//change = false;
		if(change){
			
			if(bw > parentBwConf.getOverallBandwidth() ) bw = parentBwConf.getOverallBandwidth();
			else if(bw < FIXED_CHANGE) bw = FIXED_CHANGE;
			
			bwConf.setOverallBandwidth(bw);
			controllable.setBandwidthConfigure(bwConf);
		}
		
		log.debug("bw: "+Math.round(bw/1000)+" buf: "+(bufferTime + flow.getZeroToStreamTime()));
		
		return change;
	}
	
	int computeChange(long bw){
		return FIXED_CHANGE;
	}
	
}