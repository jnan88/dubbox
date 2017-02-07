package com.alibaba.dubbo.rpc.protocol.jersey;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class MyApplication extends Application{

	private Set<Class<?>> classes = new HashSet<Class<?>>();
	
	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}

}
