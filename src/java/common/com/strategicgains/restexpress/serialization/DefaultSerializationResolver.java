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

package com.strategicgains.restexpress.serialization;

import java.util.HashMap;
import java.util.Map;

import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.exception.BadRequestException;

/**
 * @author toddf
 * @since Nov 20, 2009
 */
public class DefaultSerializationResolver
implements SerializationResolver
{
	private Map<String, SerializationProcessor> processors = new HashMap<String, SerializationProcessor>();
	private String defaultFormat;
	
	public DefaultSerializationResolver()
	{
		super();
	}
	
	public DefaultSerializationResolver(Map<String, SerializationProcessor> processors, String defaultFormat)
	{
		super();
		this.processors.putAll(processors);
		this.defaultFormat = defaultFormat;
	}
	
	public SerializationProcessor put(String format, SerializationProcessor processor)
	{
		return processors.put(format, processor);
	}
	
	public void setDefaultFormat(String format)
	{
		this.defaultFormat = format;
	}

	@Override
	public SerializationProcessor resolve(Request request)
	{
		SerializationProcessor processor = null;

		processor = resolveViaRequestFormat(request);
		
		if (processor != null)
		{
			return processor;
		}

//		processor = resolveViaAcceptHeaders(request);
//		
//		if (processor != null)
//		{
//			return processor;
//		}

		processor = getDefault();
		
		if (processor == null)
		{
			throw new BadRequestException("No serialization processor found for Accept Headers");
		}
		
		return processor;
	}

    @Override
    public SerializationProcessor getDefault()
    {
		return resolveViaSpecifiedFormat(defaultFormat);
    }

	private SerializationProcessor resolveViaRequestFormat(Request request)
	{
		return resolveViaSpecifiedFormat(request.getFormat());
	}
	
//	private SerializationProcessor resolveViaAcceptHeaders(Request request)
//	{
//		List<String> acceptHeaders = getAcceptHeaders(request);
//		SerializationProcessor processor = null;
//
//		for (String acceptHeader : acceptHeaders)
//		{
//			processor = processors.get(acceptHeader);
//			
//			if (processor != null)
//			{
//				break;
//			}
//		}
//		
//		return processor;
//	}
	
	private SerializationProcessor resolveViaSpecifiedFormat(String format)
	{
		if (format == null || format.trim().isEmpty())
		{
			return null;
		}
		
		return processors.get(format);
	}

	/**
     * @param request
     * @return
     */
//    private List<String> getAcceptHeaders(Request request)
//    {
//	    // TODO retrieve the accept headers from the request as a list.
//    	return Collections.emptyList();
//    }
}
