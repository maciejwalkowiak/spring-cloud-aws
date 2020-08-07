/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.aws.it.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.autoconfigure.jdbc.AmazonRdsDatabaseAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Agim Emruli
 */
@SpringBootTest(
		classes = BootDataSourceFactoryBeanAwsTest.BootDataSourceFactoryBeanAwsTestConfig.class,
		properties = {
				"cloud.aws.credentials.access-key=${aws-integration-tests.accessKey}",
				"cloud.aws.credentials.secret-key=${aws-integration-tests.secretKey}" })
class BootDataSourceFactoryBeanAwsTest extends DataSourceFactoryBeanAwsTest {

	@SpringBootApplication
	@PropertySource({ "classpath:Integration-test-config.properties",
			"file://${els.config.dir}/access.properties" })
	static class BootDataSourceFactoryBeanAwsTestConfig {

	}

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void usesAutoConfiguration() {
		assertThat(applicationContext.getBean(AmazonRdsDatabaseAutoConfiguration.class)).isNotNull();
	}

}
