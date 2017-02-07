package com.alibaba.dubbo.rpc.protocol.jersey;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.proxy.WebResourceFactory;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.servlet.BootstrapListener;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.ServiceClassHolder;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

public class JerseyProtocol extends AbstractProxyProtocol {

	private static final int DEFAULT_PORT = 80;
	private final Map<String, JerseyServer> servers = new ConcurrentHashMap<String, JerseyServer>();
	private final List<Client> clients = Collections.synchronizedList(new LinkedList<Client>());
	private final JerseyServerFactory serverFactory = new JerseyServerFactory();

	@Override
	public int getDefaultPort() {
		return DEFAULT_PORT;
	}

	public void setHttpBinder(HttpBinder httpBinder) {
		serverFactory.setHttpBinder(httpBinder);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
		String addr = url.getIp() + ":" + url.getPort();
		final Class<T> implClass = ServiceClassHolder.getInstance().popServiceClass();
		JerseyServer server = servers.get(addr);
		if (server == null) {
			server = serverFactory.createServer(url.getParameter(Constants.SERVER_KEY, "jetty"));
			server.start(url);
			servers.put(addr, server);
		}
		String contextPath = getContextPath(url);
		if ("servlet".equalsIgnoreCase(url.getParameter(Constants.SERVER_KEY, "jetty"))) {
			ServletContext servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
			if (servletContext == null) {
				throw new RpcException("No servlet context found. Since you are using server='servlet', " + "make sure that you've configured " + BootstrapListener.class.getName() + " in web.xml");
			}
			String webappPath = servletContext.getContextPath();
			if (StringUtils.isNotEmpty(webappPath)) {
				webappPath = webappPath.substring(1);
				if (!contextPath.startsWith(webappPath)) {
					throw new RpcException("Since you are using server='servlet', " + "make sure that the 'contextpath' property starts with the path of external webapp");
				}
				contextPath = contextPath.substring(webappPath.length());
				if (contextPath.startsWith("/")) {
					contextPath = contextPath.substring(1);
				}
			}
		}
		server.deploy(implClass, impl, contextPath);

		final JerseyServer s = server;
		return new Runnable() {
			public void run() {
				// TODO due to dubbo's current architecture,
				// it will be called from registry protocol in the shutdown
				// process and won't appear in logs
				s.undeploy(implClass);
			}
		};
	}

	@Override
	protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		clients.add(client);
		WebTarget webTarget = client.target("http://" + url.getHost() + ":" + url.getPort() + "/" + getContextPath(url));
		return WebResourceFactory.newResource(type, webTarget);
	}

	@Override
	public void destroy() {
		super.destroy();
		for (Map.Entry<String, JerseyServer> entry : servers.entrySet()) {
			try {
				if (logger.isInfoEnabled()) {
					logger.info("Closing the rest server at " + entry.getKey());
				}
				entry.getValue().stop();
			} catch (Throwable t) {
				logger.warn("Error closing rest server", t);
			}
		}
		servers.clear();
	}

	protected String getContextPath(URL url) {
		int pos = url.getPath().lastIndexOf("/");
		return pos > 0 ? url.getPath().substring(0, pos) : "";
	}

}
