package com.example.rpc.policy;

import com.example.rpc.policy.NRpcProto.RpcMeta;

public class DefaultProtocolCtx {

	public int metaSize;
	public int bodySize;
	public RpcMeta meta;
	
	public DefaultProtocolCtx() {
		metaSize = -1;
		bodySize = -1;
		meta = null;
	}

}
