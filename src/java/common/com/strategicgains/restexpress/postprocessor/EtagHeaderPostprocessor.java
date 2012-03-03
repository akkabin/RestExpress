package com.strategicgains.restexpress.postprocessor;
/*
    Copyright 2011, Strategic Gains, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.ETAG;

import com.strategicgains.restexpress.Request;
import com.strategicgains.restexpress.Response;
import com.strategicgains.restexpress.pipeline.Postprocessor;
import com.strategicgains.util.date.DateAdapter;
import com.strategicgains.util.date.HttpHeaderTimestampAdapter;

/**
 * If the response body is non-null, adds an ETag header.  ETag is
 * computed from the body object's hash code combined with the hash
 * code of the resulting format.
 * 
 * @author toddf
 * @since Oct 5, 2011
 */
public class EtagHeaderPostprocessor
implements Postprocessor
{
	DateAdapter fmt = new HttpHeaderTimestampAdapter();

	@Override
	public void process(Request request, Response response)
	{
		if (!request.isMethodGet()) return;
		if (!response.hasBody()) return;

		Object body = response.getBody();

		if (!response.hasHeader(ETAG))
		{
			String format = request.getFormat() == null ? request.getResolvedRoute().getDefaultFormat() : request.getFormat();
			response.addHeader(ETAG, String.valueOf(body.hashCode() ^ format.hashCode()));
		}

		// TODO: this should be in a LastModifiedHeaderPostprocessor
//		if (!response.hasHeader(LAST_MODIFIED) && body.getClass().isAssignableFrom(Timestamped.class))
//		{
//			response.addHeader(LAST_MODIFIED, fmt.format(((Timestamped) body).getUpdatedAt()));
//		}
	}
}
