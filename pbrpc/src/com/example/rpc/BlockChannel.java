package com.example.rpc;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Vector;

import com.example.rpc.policy.DefaultProtocol;
import com.example.rpc.policy.DefaultProtocol.ParseResult;
import com.google.protobuf.BlockingRpcChannel;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;


public class BlockChannel implements BlockingRpcChannel {

	private final String TAG = "BChannel: ";
	private ChannelOption _option;	
	
	public BlockChannel(ChannelOption option) {
		super();
		_option = option;
	}
	
	@Override
	public Message callBlockingMethod(MethodDescriptor method,
			RpcController controller, Message req, Message resp/*just for type*/)
			throws ServiceException {
		Controller cntl = (Controller)controller;
		IOBuf req_msg = DefaultProtocol.PackRequest(method, cntl, req);
		
		RpcThread thread = new RpcThread(this, cntl, req_msg, resp);
		thread.start();
		
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Message new_resp = cntl.getResp();
		cntl.finalize();
		return new_resp;
	}
	
	public ChannelOption getOption() {
		return _option;
	}	
	
	
	class RpcThread extends Thread {
		
		private static final int MAX_RESPONSE_LEN = 1024;
		private BlockChannel channel;
		private Controller cntl;
		private IOBuf req_msg;
		private Message resp;
		
		public RpcThread(BlockChannel channel, Controller cntl, IOBuf req_msg, Message resp) {
			this.channel = channel;
			this.cntl = cntl;
			this.req_msg = req_msg;
			this.resp = resp;
		}

		@Override
        public void run() {
			Socket socket = new Socket();
			SocketAddress remote = new InetSocketAddress(channel.getOption().remote_addr,
					channel.getOption().remote_port);
			try {
				socket.connect(remote, 60000);
			} catch (UnknownHostException e) {
				System.out.println(TAG + "unkown host");
				e.printStackTrace();
				return;
			} catch (ConnectException e) {
				System.out.println(TAG + "remote don't listen on port");
				e.printStackTrace();
				return;
			} catch (SocketTimeoutException e) {
				System.out.println(TAG + "connect timeout 1 min");
				e.printStackTrace();
				return;
			} catch (IOException e) {
				System.out.println(TAG + "connect unkown error");
				e.printStackTrace();
				return;
			}
			
			// send request
			DataOutputStream os = null;
			try {
				os = new DataOutputStream(socket.getOutputStream());
			} catch (IOException e) {
				System.out.println(TAG + "socket getOutputStream error");
				e.printStackTrace();
			}
			byte[] req_buf = req_msg.array();
			/*for (int i = 0; i < req_msg.length() + 10; i++) {
				String hex = Integer.toHexString(req_buf[i] & 0xFF);
				System.out.print(hex.toUpperCase() + " ");
			}
			System.out.println(" ");*/
			
			try {
				os.write(req_buf, req_msg.offset(), req_msg.length());
				os.flush();
			} catch (IOException e2) {
				System.out.println(TAG + "socket send request error");
				e2.printStackTrace();
			}

			// recv response
			InputStream resp_buf = null;
			try {
				resp_buf = socket.getInputStream();
			} catch (IOException e) {
				System.out.println(TAG + "socket getInputStream error");
				e.printStackTrace();
			}
			try {
				socket.setSoTimeout(5000);
			} catch (SocketException e1) {
				e1.printStackTrace();
			}
			
			IOBuf resp_msg = new IOBuf();
			IOBuf.Output resp_msg_out = new IOBuf.Output(resp_msg);
			cntl.setRespMsg(resp_msg);
			byte[] b = new byte[MAX_RESPONSE_LEN];
			int offset = 0;
			int len = MAX_RESPONSE_LEN;			
			boolean readEof = false;
			while (true) {			
				// recv response
				int read_len;
				try {
					read_len = resp_buf.read(b, offset, len);
				} catch (SocketTimeoutException e) {
					System.out.println(TAG + "socket read timeout");
					e.printStackTrace();
					break;
				} catch (IOException e) {
					System.out.println(TAG + "socket read error");
					e.printStackTrace();
					break;
				}
				if (read_len == -1) {
					readEof = true;
				}				
				resp_msg_out.write(b, offset, read_len);
				resp_msg_out.flush();
				offset += read_len;
				len -= read_len;
				if (len <= 0) {
					System.out.println(TAG + "response is too big");
					break;
				}
				
				// parse response
				ParseResult ret = DefaultProtocol.ParseResponse(cntl, readEof);
				if (ret == ParseResult.NOT_ENOUGH_DATA) {
					System.out.println(TAG + "not enough data");
					continue;
				}
				if ((ret == ParseResult.BAD_SCHEMA) || (ret == ParseResult.BROKEN)) {
					System.out.println(TAG + "bad response");
					cntl.setFailed("bad response");
				}
				break;
			}
			resp_msg_out.close();
			if (!cntl.failed()) {
				DefaultProtocol.ProcessResponse(cntl, resp);
			}
			return;
		}
	}

}
