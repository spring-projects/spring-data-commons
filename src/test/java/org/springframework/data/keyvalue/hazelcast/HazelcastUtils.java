/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.keyvalue.hazelcast;

import org.springframework.data.keyvalue.hazelcast.HazelcastKeyValueAdapter;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

/**
 * @author Christoph Strobl
 */
public class HazelcastUtils {

	static Config hazelcastConfig() {

		Config hazelcastConfig = new Config();
		hazelcastConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
		hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
		hazelcastConfig.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);

		return hazelcastConfig;
	}

	public static HazelcastKeyValueAdapter preconfiguredHazelcastKeyValueAdapter() {
		return new HazelcastKeyValueAdapter(Hazelcast.newHazelcastInstance(hazelcastConfig()));
	}

}
