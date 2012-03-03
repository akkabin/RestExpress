/*
 * Copyright 2009, Strategic Gains, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.strategicgains.restexpress;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;

import com.strategicgains.restexpress.domain.metadata.ServerMetadata;
import com.strategicgains.restexpress.exception.ExceptionMapping;
import com.strategicgains.restexpress.exception.ServiceException;
import com.strategicgains.restexpress.pipeline.DefaultRequestHandler;
import com.strategicgains.restexpress.pipeline.MessageObserver;
import com.strategicgains.restexpress.pipeline.PipelineBuilder;
import com.strategicgains.restexpress.pipeline.Postprocessor;
import com.strategicgains.restexpress.pipeline.Preprocessor;
import com.strategicgains.restexpress.plugin.Plugin;
import com.strategicgains.restexpress.response.DefaultResponseWrapper;
import com.strategicgains.restexpress.response.RawResponseWrapper;
import com.strategicgains.restexpress.response.ResponseWrapperFactory;
import com.strategicgains.restexpress.route.RouteDeclaration;
import com.strategicgains.restexpress.route.RouteResolver;
import com.strategicgains.restexpress.serialization.AliasingSerializationProcessor;
import com.strategicgains.restexpress.serialization.DefaultSerializationResolver;
import com.strategicgains.restexpress.serialization.SerializationProcessor;
import com.strategicgains.restexpress.serialization.SerializationResolver;
import com.strategicgains.restexpress.serialization.gson.GsonJsonProcessor;
import com.strategicgains.restexpress.serialization.text.DefaultTxtProcessor;
import com.strategicgains.restexpress.serialization.xstream.XstreamXmlProcessor;
import com.strategicgains.restexpress.util.Bootstraps;
import com.strategicgains.restexpress.util.DefaultShutdownHook;
import com.strategicgains.restexpress.util.LogLevel;
import com.strategicgains.restexpress.util.Resolver;

/**
 * Primary entry point to create a RestExpress service. All that's required is a
 * RouteDeclaration. By default: port is 8081, serialization format is JSON,
 * supported formats are JSON and XML.
 * 
 * @author toddf
 */
public class RestExpress
{
	private static final ChannelGroup allChannels = new DefaultChannelGroup(
	    "RestExpress");

	public static final int DEFAULT_PORT = 8081;
	public static final String DEFAULT_NAME = "RestExpress";
	private static final int DEFAULT_EXECUTOR_THREAD_COUNT = 0;

	private ServerBootstrap bootstrap;
	private String name;
	private int port;
	private RouteDeclaration routeDeclarations;
	private String defaultFormat;
	private boolean useTcpNoDelay = true;
	private boolean useKeepAlive = true;
	private boolean reuseAddress = true;
	private int soLinger = -1; // disabled by default
	private int receiveBufferSize = 262140; // Java default
	private int connectTimeoutMillis = 10000; // netty default
	private LogLevel logLevel = LogLevel.DEBUG; // Netty default
	private boolean useSystemOut;
	private ResponseWrapperFactory responseWrapperFactory;
	private boolean shouldHandleChunking = true;
	private boolean shouldUseCompression = true;
	private Integer maxChunkSize = null;
	
	// This controls the number of concurrent connections the application can handle.
	// Netty default is 2 * number of processors (or cores).
	private int workerThreadCount = 0;
	
	// This controls the number of concurrent requests the application can process.
	private int executorThreadCount = DEFAULT_EXECUTOR_THREAD_COUNT;

	Map<String, SerializationProcessor> serializationProcessors = new HashMap<String, SerializationProcessor>();
	private List<MessageObserver> messageObservers = new ArrayList<MessageObserver>();
	private List<Preprocessor> preprocessors = new ArrayList<Preprocessor>();
	private List<Postprocessor> postprocessors = new ArrayList<Postprocessor>();
	private Map<String, Class<?>> xmlAliases = new HashMap<String, Class<?>>();
	private Resolver<SerializationProcessor> serializationResolver;
	private ExceptionMapping exceptionMap = new ExceptionMapping();
	private List<Plugin> plugins = new ArrayList<Plugin>();

	/**
	 * Create a new RestExpress service. By default, RestExpress uses port 8081.
	 * Supports JSON, and XML, providing JSEND-style wrapped responses. And
	 * displays some messages on System.out. These can be altered with the
	 * setPort(), noJson(), noXml(), noSystemOut(), and useRawResponses() DSL
	 * modifiers, respectively, as needed.
	 * 
	 * <p/>
	 * The default input and output format for messages is JSON. To change that,
	 * use the setDefaultFormat(String) DSL modifier, passing the format to use
	 * by default. Make sure there's a corresponding SerializationProcessor for
	 * that particular format. The Format class has the basics.
	 * 
	 * <p/>
	 * This DSL was created as a thin veneer on Netty functionality. The bind()
	 * method simply builds a Netty pipeline and uses this builder class to
	 * create it. Underneath the covers, RestExpress uses Google GSON for JSON
	 * handling and XStream for XML processing. However, both of those can be
	 * swapped out using the putSerializationProcessor(String,
	 * SerializationProcessor) method, creating your own instance of
	 * SerializationProcessor as necessary.
	 * 
	 * @param routes
	 *            a RouteDeclaration that declares the URL routes that this
	 *            service supports.
	 */
	public RestExpress(RouteDeclaration routes)
	{
		super();
		setRoutes(routes);
		setName(DEFAULT_NAME);
		setPort(DEFAULT_PORT);
		supportJson(true);
		supportXml();
		useSystemOut();
		useWrappedResponses();
	}

	/**
	 * Get the name of this RestExpress service.
	 * 
	 * @return a String representing the name of this service suite.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of this RestExpress service suite.
	 * 
	 * @param name
	 *            the name.
	 * @return the RestExpress instance to facilitate DSL-style method chaining.
	 */
	public RestExpress setName(String name)
	{
		this.name = name;
		return this;
	}

	/**
	 * Get the port that this RestExpress service suite is listening on.
	 * 
	 * @return the HTTP port.
	 */
	public int getPort()
	{
		return port;
	}

	/**
	 * Set the port that this RestExpress service suite should listen on. The
	 * default is 8081.
	 * 
	 * @param port
	 *            the HTTP port RestExpress will listen on.
	 * @return the RestExpress instance to facilitate DSL-style method chaining.
	 */
	public RestExpress setPort(int port)
	{
		this.port = port;
		return this;
	}

	public RouteDeclaration getRouteDeclarations()
	{
		return routeDeclarations;
	}

	/**
	 * Set the routes (URLs) that this RestExpress service suite supports.
	 * 
	 * @param routes
	 *            a RouteDeclaration
	 * @return the RestExpress instance to facilitate DSL-style method chaining.
	 */
	private RestExpress setRoutes(RouteDeclaration routes)
	{
		this.routeDeclarations = routes;
		return this;
	}

	public RestExpress putSerializationProcessor(String format,
	    SerializationProcessor processor)
	{
		serializationProcessors.put(format, processor);
		return this;
	}

	/* package protected */Map<String, SerializationProcessor> getSerializationProcessors()
	{
		return serializationProcessors;
	}

	public Resolver<SerializationProcessor> getSerializationResolver()
	{
		return serializationResolver;
	}

	public RestExpress setSerializationResolver(
	    Resolver<SerializationProcessor> serializationResolver)
	{
		this.serializationResolver = serializationResolver;
		return this;
	}

	public String getDefaultFormat()
	{
		return defaultFormat;
	}

	public RestExpress setDefaultFormat(String format)
	{
		if (format == null || format.trim().isEmpty()) return this;

		this.defaultFormat = format.toLowerCase();
		return this;
	}

	/**
	 * Tell RestExpress to support JSON in routes, incoming and outgoing. By
	 * default RestExpress supports JSON and is the default.
	 * 
	 * @param isDefault
	 *            true to make JSON the default format.
	 * @return the RestExpress instance.
	 */
	public RestExpress supportJson(boolean isDefault)
	{
		if (!getSerializationProcessors().containsKey(Format.JSON))
		{
			serializationProcessors
			    .put(Format.JSON, new GsonJsonProcessor());
		}

		if (isDefault)
		{
			setDefaultFormat(Format.JSON);
		}

		return this;
	}

	/**
	 * Tell RestExpress to support JSON in routes, incoming and outgoing. By
	 * default RestExpress supports JSON and is the default.
	 * 
	 * @return the RestExpress instance.
	 */
	public RestExpress supportJson()
	{
		return supportJson(false);
	}

	/**
	 * Tell RestExpress to not support JSON in routes, incoming or outgoing.
	 * Client must call setDefaultFormat(String) to set the default format to
	 * something else.
	 * 
	 * @return the RestExpress instance.
	 */
	public RestExpress noJson()
	{
		serializationProcessors.remove(Format.JSON);
		return this;
	}

	/**
	 * Tell RestExpress to support XML in routes, incoming and outgoing. By
	 * default RestExpress supports XML.
	 * 
	 * @param isDefault
	 *            true to make XML the default format.
	 * @return the RestExpress instance.
	 */
	public RestExpress supportXml(boolean isDefault)
	{
		if (!getSerializationProcessors().containsKey(Format.XML))
		{
			getSerializationProcessors().put(Format.XML,
			    new XstreamXmlProcessor());
		}

		if (isDefault)
		{
			setDefaultFormat(Format.XML);
		}

		return this;
	}

	/**
	 * Tell RestExpress to support XML in routes, incoming and outgoing. By
	 * default RestExpress supports XML.
	 * 
	 * @param isDefault
	 *            true to make XML the default format.
	 * @return the RestExpress instance.
	 */
	public RestExpress supportXml()
	{
		return supportXml(false);
	}

	/**
	 * Tell RestExpress to not support XML in routes, incoming or outgoing.
	 * 
	 * @return the RestExpress instance.
	 */
	public RestExpress noXml()
	{
		serializationProcessors.remove(Format.XML);
		return this;
	}

	public RestExpress supportChunking()
	{
		shouldHandleChunking = true;
		return this;
	}

	public RestExpress noChunking()
	{
		shouldHandleChunking = false;
		return this;
	}

	public RestExpress setMaxChunkSize(int size)
	{
		this.maxChunkSize = Integer.valueOf(size);
		return this;
	}

	public RestExpress supportCompression()
	{
		shouldUseCompression = true;
		return this;
	}

	public RestExpress noCompression()
	{
		shouldUseCompression = false;
		return this;
	}

	/**
	 * Tell RestExpress to support TXT format specifiers in routes, outgoing
	 * only at present.
	 * 
	 * @param isDefault
	 *            true to make TXT the default format.
	 * @return the RestExpress instance.
	 */
	public RestExpress supportTxt(boolean isDefault)
	{
		if (!getSerializationProcessors().containsKey(Format.TXT))
		{
			getSerializationProcessors().put(Format.TXT,
			    new DefaultTxtProcessor());
		}

		if (isDefault)
		{
			setDefaultFormat(Format.TXT);
		}

		return this;
	}

	/**
	 * Tell RestExpress to support TXT format specifier in routes, outgoing only
	 * at present.
	 * 
	 * @return the RestExpress instance.
	 */
	public RestExpress supportTxt()
	{
		return supportTxt(false);
	}

	public RestExpress addMessageObserver(MessageObserver observer)
	{
		if (!messageObservers.contains(observer))
		{
			messageObservers.add(observer);
		}

		return this;
	}

	public List<MessageObserver> getMessageObservers()
	{
		return messageObservers;
	}

	/**
	 * Add a Preprocessor instance that gets called before an incoming message
	 * gets processed. Preprocessors get called in the order in which they are
	 * added. To break out of the chain, simply throw an exception.
	 * 
	 * @param processor
	 * @return
	 */
	public RestExpress addPreprocessor(Preprocessor processor)
	{
		if (!preprocessors.contains(processor))
		{
			preprocessors.add(processor);
		}

		return this;
	}

	public List<Preprocessor> getPreprocessors()
	{
		return preprocessors;
	}

	/**
	 * Add a PostProcessor instance that gets call after an incoming message is
	 * processed. A Postprocessor is useful for augmenting or transforming the
	 * results. Postprocessors get called in the order in which they get added.
	 * However, they do NOT get called in the case of an exception or error
	 * within the route.
	 * 
	 * @param processor
	 * @return
	 */
	public RestExpress addPostprocessor(Postprocessor processor)
	{
		if (!postprocessors.contains(processor))
		{
			postprocessors.add(processor);
		}

		return this;
	}

	public List<Postprocessor> getPostprocessors()
	{
		return postprocessors;
	}

	public boolean shouldUseSystemOut()
	{
		return useSystemOut;
	}

	public RestExpress setUseSystemOut(boolean useSystemOut)
	{
		this.useSystemOut = useSystemOut;
		return this;
	}

	public RestExpress useSystemOut()
	{
		setUseSystemOut(true);
		return this;
	}

	public RestExpress noSystemOut()
	{
		setUseSystemOut(false);
		return this;
	}

	public boolean isUseTcpNoDelay()
	{
		return useTcpNoDelay;
	}

	public RestExpress setUseTcpNoDelay(boolean useTcpNoDelay)
	{
		this.useTcpNoDelay = useTcpNoDelay;
		return this;
	}

	public boolean isUseKeepAlive()
	{
		return useKeepAlive;
	}

	public RestExpress setUseKeepAlive(boolean useKeepAlive)
	{
		this.useKeepAlive = useKeepAlive;
		return this;
	}

	public LogLevel getLogLevel()
	{
		return logLevel;
	}

	public RestExpress setLogLevel(LogLevel logLevel)
	{
		this.logLevel = logLevel;
		return this;
	}

	public boolean isReuseAddress()
	{
		return reuseAddress;
	}

	public RestExpress setReuseAddress(boolean reuseAddress)
	{
		this.reuseAddress = reuseAddress;
		return this;
	}

	public int getSoLinger()
	{
		return soLinger;
	}

	public RestExpress setSoLinger(int soLinger)
	{
		this.soLinger = soLinger;
		return this;
	}

	public int getReceiveBufferSize()
	{
		return receiveBufferSize;
	}

	public RestExpress setReceiveBufferSize(int receiveBufferSize)
	{
		this.receiveBufferSize = receiveBufferSize;
		return this;
	}

	public int getConnectTimeoutMillis()
	{
		return connectTimeoutMillis;
	}

	public RestExpress setConnectTimeoutMillis(int connectTimeoutMillis)
	{
		this.connectTimeoutMillis = connectTimeoutMillis;
		return this;
	}

	/**
	 * 
	 * @param elementName
	 * @param theClass
	 * @return
	 */
	public RestExpress alias(String elementName, Class<?> theClass)
	{
		xmlAliases.put(elementName, theClass);
		return this;
	}

	public RestExpress useWrappedResponses()
	{
		responseWrapperFactory = new DefaultResponseWrapper();
		return this;
	}

	public RestExpress useRawResponses()
	{
		responseWrapperFactory = new RawResponseWrapper();
		return this;
	}

	public <T extends Exception, U extends ServiceException> RestExpress mapException(
	    Class<T> from, Class<U> to)
	{
		exceptionMap.map(from, to);
		return this;
	}

	public RestExpress setExceptionMap(ExceptionMapping mapping)
	{
		this.exceptionMap = mapping;
		return this;
	}

	/**
	 * Return the number of requested NIO/HTTP-handling worker threads.
	 * 
	 * @return the number of requested worker threads.
	 */
	public int getWorkerThreadCount()
	{
		return workerThreadCount;
	}

	/**
	 * Set the number of NIO/HTTP-handling worker threads.  This
	 * value controls the number of simultaneous connections the
	 * application can handle.
	 * 
	 * The default (if this value is not set, or set to zero) is
	 * the Netty default, which is 2 times the number of processors
	 * (or cores).
	 * 
	 * @param value the number of desired NIO worker threads.
	 * @return the RestExpress instance.
	 */
	public RestExpress setWorkerThreadCount(int value)
	{
		this.workerThreadCount = value;
		return this;
	}
	
	/**
	 * Returns the number of background request-handling (executor) threads.
	 * 
	 * @return the number of executor threads.
	 */
	public int getExecutorThreadCount()
	{
		return executorThreadCount;
	}
	
	/**
	 * Set the number of background request-handling (executor) threads.
	 * This value controls the number of simultaneous requests that the
	 * application can handle.  For longer-running requests, a higher number
	 * may be indicated.
	 * 
	 * For VERY short-running requests, a value of zero will cause no
	 * background threads to be created, causing all processing to occur in
	 * the NIO worker.
	 * 
	 * @param value the number of executor threads to create.
	 * @return the RestExpress instance.
	 */
	public RestExpress setExecutorThreadCount(int value)
	{
		this.executorThreadCount = value;
		return this;
	}

	/**
	 * The last call in the building of a RestExpress server, bind() causes
	 * Netty to bind to the listening address and process incoming messages.
	 * 
	 * @return Channel
	 */
	public Channel bind()
	{
		// Configure the server.
		if (workerThreadCount == 0)
		{
			bootstrap = Bootstraps.createServerNioBootstrap();
		}
		else
		{
			bootstrap = Bootstraps.createServerNioBootstrap(workerThreadCount);
		}

		// Set up the event pipeline factory.
		DefaultRequestHandler requestHandler = new DefaultRequestHandler(
		    createRouteResolver(), createSerializationResolver());
		requestHandler.setResponseWrapperFactory(responseWrapperFactory);

		// Add MessageObservers to the request handler here, if desired...
		requestHandler.addMessageObserver(messageObservers
		    .toArray(new MessageObserver[0]));

		requestHandler.setExceptionMap(exceptionMap);

		// Add pre/post processors to the request handler here...
		addPreprocessors(requestHandler);
		addPostprocessors(requestHandler);

		PipelineBuilder pf = new PipelineBuilder().addRequestHandler(
		    new LoggingHandler(getLogLevel().getNettyLogLevel()))
		    .addRequestHandler(requestHandler);
		
		if (getExecutorThreadCount() > 0)
		{
			ExecutionHandler executionHandler = new ExecutionHandler(
	             new OrderedMemoryAwareThreadPoolExecutor(getExecutorThreadCount(), 0, 0));
			pf.setExecutionHandler(executionHandler);
		}

		if (shouldHandleChunking)
		{
			pf.handleChunked();

			if (maxChunkSize != null)
			{
				pf.maxChunkSize(maxChunkSize.intValue());
			}
		}

		if (shouldUseCompression)
		{
			pf.useCompression();
		}

		bootstrap.setPipelineFactory(pf);
		setBootstrapOptions();

		// Bind and start to accept incoming connections.
		if (shouldUseSystemOut())
		{
			System.out.println("Starting " + getName() + " Server on port "
			    + port);
		}

		Channel channel = bootstrap.bind(new InetSocketAddress(getPort()));
		allChannels.add(channel);
		bindPlugins();
		return channel;
	}

	private void setBootstrapOptions()
	{
		bootstrap.setOption("child.tcpNoDelay", isUseTcpNoDelay());
		bootstrap.setOption("child.keepAlive", isUseKeepAlive());
		bootstrap.setOption("reuseAddress", isReuseAddress());
		bootstrap.setOption("child.soLinger", getSoLinger());
		bootstrap.setOption("connectTimeoutMillis", getConnectTimeoutMillis());
		bootstrap.setOption("receiveBufferSize", getReceiveBufferSize());
	}

	/**
	 * Used in main() to install a default JVM shutdown hook and shut down the
	 * server cleanly. Calls shutdown() when JVM termination detected. To
	 * utilize your own shutdown hook(s), install your own shutdown hook(s) and
	 * call shutdown() instead of awaitShutdown().
	 */
	public void awaitShutdown()
	{
		Runtime.getRuntime().addShutdownHook(new DefaultShutdownHook(this));
		boolean interrupted = false;

		do
		{
			try
			{
				Thread.sleep(300);
			}
			catch (InterruptedException e)
			{
				interrupted = true;
			}
		}
		while (!interrupted);
	}

	/**
	 * Releases all resources associated with this server so the JVM can
	 * shutdown cleanly. Call this method to finish using the server. To utilize
	 * the default shutdown hook in main() provided by RestExpress, call
	 * awaitShutdown() instead.
	 */
	public void shutdown()
	{
		ChannelGroupFuture future = allChannels.close();
		future.awaitUninterruptibly();
		bootstrap.getFactory().releaseExternalResources();
	}

	/**
	 * @return
	 */
	private RouteResolver createRouteResolver()
	{
		RouteDeclaration routeDeclarations = getRouteDeclarations();
		routeDeclarations.setDefaultFormat(getDefaultFormat());
		routeDeclarations.setSupportedFormats(getSupportedFormats());
		return new RouteResolver(routeDeclarations.createRouteMapping());
	}

	/**
	 * @return
	 */
	private List<String> getSupportedFormats()
	{
		List<String> supportedFormats = new ArrayList<String>();

		for (String format : serializationProcessors.keySet())
		{
			supportedFormats.add(format);
		}

		return supportedFormats;
	}

	/**
	 * @return
	 */
	public ServerMetadata getRouteMetadata()
	{
		ServerMetadata m = new ServerMetadata();
		m.setName(getName());
		m.setPort(getPort());
		m.setDefaultFormat(getDefaultFormat());
		m.addAllSupportedFormats(getSerializationProcessors().keySet());
		m.addAllRoutes(getRouteDeclarations().getMetadata());
		return m;
	}

	public RestExpress registerPlugin(Plugin plugin)
	{
		if (!plugins.contains(plugin))
		{
			plugins.add(plugin);
			plugin.register(this);
		}

		return this;
	}

	private void bindPlugins()
	{
		for (Plugin plugin : plugins)
		{
			plugin.bind(this);
		}
	}

	/**
	 * @return
	 */
	private SerializationResolver createSerializationResolver()
	{
		DefaultSerializationResolver resolver = new DefaultSerializationResolver();
		resolver.setDefaultFormat(getDefaultFormat());

		for (Entry<String, SerializationProcessor> entry : getSerializationProcessors()
		    .entrySet())
		{
			if (entry.getKey().equals(Format.XML))
			{
				setXmlAliases((AliasingSerializationProcessor) entry.getValue());
			}

			resolver.put(entry.getKey(), entry.getValue());
		}

		return resolver;
	}

	/**
	 * @param value
	 */
	private void setXmlAliases(AliasingSerializationProcessor value)
	{
		for (Entry<String, Class<?>> entry : xmlAliases.entrySet())
		{
			value.alias(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * @param requestHandler
	 */
	private void addPreprocessors(DefaultRequestHandler requestHandler)
	{
		for (Preprocessor processor : getPreprocessors())
		{
			requestHandler.addPreprocessor(processor);
		}
	}

	/**
	 * @param requestHandler
	 */
	private void addPostprocessors(DefaultRequestHandler requestHandler)
	{
		for (Postprocessor processor : getPostprocessors())
		{
			requestHandler.addPostprocessor(processor);
		}
	}
}
