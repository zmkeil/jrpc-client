package com.example.rpc.policy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import com.example.rpc.Controller;
import com.example.rpc.IOBuf;
import com.example.rpc.policy.NRpcProto.RpcMeta;
import com.example.rpc.policy.NRpcProto.RpcRequestMeta;
import com.example.rpc.policy.NRpcProto.RpcResponseMeta;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;

public class DefaultProtocol {
	
	private final static String TAG = "DefaultProtocol: ";
	
	private final static byte[] MAGIC_HEAD = "NRPC".getBytes();
	static {
		assert MAGIC_HEAD.length == 4;
	}
	
	public enum ParseResult {
		BAD_SCHEMA,
		BROKEN,
		NOT_ENOUGH_DATA,
		DONE
	}

	public static IOBuf PackRequest(MethodDescriptor method, Controller cntl, Message req) {
		RpcMeta.Builder metaBuilder = RpcMeta.newBuilder();
		
		RpcRequestMeta.Builder requestBuilder = RpcRequestMeta.newBuilder();
		requestBuilder.setServiceName(method.getService().getName());
		requestBuilder.setMethodName(method.getName());
		
		metaBuilder.setRequest(requestBuilder);
		RpcMeta meta = metaBuilder.build();
		int metaSize = meta.getSerializedSize();
		int bodySize = req.getSerializedSize();
		
		IOBuf reqMsg = new IOBuf();
		IOBuf.Output output = new IOBuf.Output(reqMsg);
		ByteBuffer head = ByteBuffer.allocate(12);
		head.order(ByteOrder.LITTLE_ENDIAN);
		// putInt() write 4 byte
		head = head.put(MAGIC_HEAD).putInt(metaSize).putInt(bodySize);
		output.write(head.array(), 0, 12);
				
		try {
			meta.writeTo(output);
		} catch (IOException e) {
			System.out.println(TAG + "meta searilize error");
			return null;
		}
		
		try {
			req.writeTo(output);
		} catch (IOException e) {
			System.out.println(TAG + "req searilize error");
			return null;
		}
		
		output.close();
		System.out.println(TAG + "pack request success");
		return reqMsg;
	}
	
	public static ParseResult ParseResponse(Controller cntl, boolean readEof) {
		IOBuf respMsg = cntl.getRespMsg();
		DefaultProtocolCtx protocolCtx = cntl.getProtocolCtx();
		
		// detect format
		if (protocolCtx.metaSize == -1) {
			if (!DefaultDetectFormat(respMsg, protocolCtx)) {
				return readEof ? ParseResult.BAD_SCHEMA : ParseResult.NOT_ENOUGH_DATA;
			}
		}
		RpcMeta meta = protocolCtx.meta;
		if (meta == null) {
			if (respMsg.length() < protocolCtx.metaSize) {
				return readEof ? ParseResult.BROKEN : ParseResult.NOT_ENOUGH_DATA;
			}
			IOBuf metaBuf = respMsg.cutn(protocolCtx.metaSize);
			try {
				protocolCtx.meta = RpcMeta.newBuilder().mergeFrom(
						metaBuf.array(), metaBuf.offset(), metaBuf.length()).build();				
			} catch (Exception e) {
				return ParseResult.BROKEN;
			}
		}
		
		if (respMsg.length() < protocolCtx.bodySize) {
			return readEof ? ParseResult.BROKEN : ParseResult.NOT_ENOUGH_DATA;
		}
		System.out.println(TAG + "parse meta done and enough data");
		return ParseResult.DONE;
	}
	
	public static void ProcessResponse(Controller cntl, Message resp) {
		IOBuf respMsg = cntl.getRespMsg();
		DefaultProtocolCtx protocolCtx = cntl.getProtocolCtx();
		
		RpcResponseMeta respMeta = protocolCtx.meta.getResponse();
		if ((respMeta != null) && (respMeta.hasErrorCode())
				&& (respMeta.getErrorCode() == NRpcProto.RPC_SERVICE_RESULT.RPC_SERVICE_FAILED)) {
			cntl.setFailed(respMeta.getErrorText());
		}
		
		IOBuf respBuf = respMsg.cutn(protocolCtx.bodySize);
		Message new_resp = null;
		try {
			new_resp = resp.newBuilderForType().mergeFrom(
					respBuf.array(), respBuf.offset(), respBuf.length()).build();				
		} catch (Exception e) {
			e.printStackTrace();
			cntl.setFailed("Parse resp failed");
			return;
		}
		
		cntl.setResp(new_resp);
		System.out.println(TAG + "parse response text done");
	}
	
	
	private static boolean DefaultDetectFormat(IOBuf respMsg,
			DefaultProtocolCtx protocolCtx) {
		if (respMsg.length() < 12) {
			return false;
		}
		
		ByteBuffer head = respMsg.fetch(12);
		head.order(ByteOrder.LITTLE_ENDIAN);
		byte[] magic = new byte[4];
		head.get(magic, 0, 4);
		if (!Arrays.equals(MAGIC_HEAD, magic)) {
			System.out.println(TAG + "magic not equals");
			return false;
		}
		
		protocolCtx.metaSize = head.getInt();
		protocolCtx.bodySize = head.getInt();
		return true;
	}


}
