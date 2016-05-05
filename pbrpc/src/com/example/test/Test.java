package com.example.test;


import com.example.rpc.BlockChannel;
import com.example.rpc.ChannelOption;
import com.example.rpc.Controller;
import com.google.protobuf.ServiceException;

import com.example.test.EchoServiceBean.EchoRequest;
import com.example.test.EchoServiceBean.EchoResponse;
import com.example.test.EchoServiceBean.EchoService;
import com.example.test.EchoServiceBean.EchoService.BlockingInterface;
import com.example.test.EchoServiceBean.Person;

public class Test {

	public static void main(String[] args) {		
		ChannelOption option = new ChannelOption("112.126.80.217", 8899);		
        BlockChannel bchannel = new BlockChannel(option);
        BlockingInterface bstub = EchoService.newBlockingStub(bchannel);
        EchoResponse response = null;
        
        for (int i = 0; i < 3; i++) {
    		Controller cntl = new Controller();
	        try {
	        	EchoRequest request = EchoRequest.newBuilder().setMsg("hello world: " + i)
	    				.addPersons(Person.newBuilder().setType(Person.Type.MAN).setId(32452).setNumber(2010).build()).build();
	        	response = bstub.echo(cntl, request);
			} catch (ServiceException e) {
				e.printStackTrace();
			}
	        if (!cntl.failed()) {
	        	System.out.println("resp msg: " + response.getRes());
	        } else {
	        	System.out.println(cntl.errorText());
	        }
	        System.out.println("------>>");
	        try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
	}
	
}
