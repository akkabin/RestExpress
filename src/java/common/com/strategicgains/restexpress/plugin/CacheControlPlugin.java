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
package com.strategicgains.restexpress.plugin;

import com.strategicgains.restexpress.RestExpress;
import com.strategicgains.restexpress.postprocessor.CacheHeaderPostprocessor;
import com.strategicgains.restexpress.postprocessor.DateHeaderPostprocessor;
import com.strategicgains.restexpress.postprocessor.EtagHeaderPostprocessor;

/**
 * @author toddf
 * @since Dec 23, 2011
 */
public class CacheControlPlugin
extends AbstractPlugin
{
	@Override
	public CacheControlPlugin register(RestExpress server)
	{
		if (isRegistered()) return this;

		super.register(server);

		server
			.addPostprocessor(new DateHeaderPostprocessor())
			.addPostprocessor(new CacheHeaderPostprocessor())
			.addPostprocessor(new EtagHeaderPostprocessor());

		return this;
	}

    @Override
    public void bind(RestExpress server)
    {
    }
}
