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
package com.strategicgains.restexpress.domain;


/**
 * XLinkUtils.asLinks() calls a user-provided XLinkFactory to 
 * enable the caller to created alternate XLink instances.
 * 
 * @author toddf
 * @since Apr 19, 2011
 */
public interface XLinkFactory
{
	/**
	 * Create an XLink using the given id, optional rel, and href values.
	 * 
	 * @param id the id of the object being referenced.
	 * @param rel the relationship of the referred object to the referring object (e.g. "self" or "related"). May be null.
	 * @param href the link to the referred object.
	 * @return an XLink instance.
	 */
	public XLink create(String id, String rel, String href);
}
