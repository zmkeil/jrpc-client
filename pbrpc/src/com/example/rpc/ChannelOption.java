package com.example.rpc;

public class ChannelOption {
	public int connect_timeout = 5;
	public int send_timeout = 5;
	public int read_timeout = 5;
	public int max_retry_time = 3;	
	public String remote_addr;
	public int remote_port;
	
	public ChannelOption(String host, int port) {
		remote_addr = host;
		remote_port = port;
	}
}
