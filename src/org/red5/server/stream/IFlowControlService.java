package org.red5.server.stream;

import org.red5.server.api.IFlowControllable;

public interface IFlowControlService {
	public static final String KEY = "FlowControlService";
	
	void registerFlowControllable(IFlowControllable fc);
	void unregisterFlowControllable(IFlowControllable fc);
	void updateBWConfigure(IFlowControllable fc);
	void resetTokenBuckets(IFlowControllable fc);
	ITokenBucket getAudioTokenBucket(IFlowControllable fc);
	ITokenBucket getVideoTokenBucket(IFlowControllable fc);
}
