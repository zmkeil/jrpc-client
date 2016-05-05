package com.example.rpc;

import com.example.rpc.policy.DefaultProtocolCtx;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public class Controller implements RpcController {
	
	private boolean isFailed;
	private String errorText;
	private IOBuf respMsg;	
	private Message resp;	
	private DefaultProtocolCtx protocolCtx;
	
	public Controller() {
		isFailed = false;
		errorText = null;
		respMsg = null;
		resp = null;
		protocolCtx = null;
	}

	@Override
	public String errorText() {
		return errorText;
	}

	@Override
	public boolean failed() {
		return isFailed;
	}

	@Override
	public boolean isCanceled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void notifyOnCancel(RpcCallback<Object> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFailed(String arg0) {
		errorText = arg0;
		isFailed = true;
	}

	@Override
	public void startCancel() {
		// TODO Auto-generated method stub
		
	}

	public IOBuf getRespMsg() {
		return respMsg;
	}

	public DefaultProtocolCtx getProtocolCtx() {
		if (protocolCtx == null) {
			protocolCtx = new DefaultProtocolCtx();
		}
		return protocolCtx;
	}
	
	public Message getResp() {
		return resp;
	}
	
	public void finalize() {
		return;
	}

	public void setRespMsg(IOBuf resp_msg) {
		this.respMsg = resp_msg;
	}

	public void setResp(Message resp) {
		this.resp = resp;
	}

}
