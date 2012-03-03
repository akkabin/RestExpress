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
package com.strategicgains.restexpress.response;

import com.strategicgains.restexpress.Response;
import com.strategicgains.restexpress.domain.ResultWrapper;

/**
 * Wraps the out bound Response body in a JSEND-style object.
 * 
 * @author toddf
 * @since Feb 10, 2011
 */
public class DefaultResponseWrapper
implements ResponseWrapperFactory
{
	@Override
	public Object wrap(Response response)
	{
		return ResultWrapper.fromResponse(response);
	}
}
