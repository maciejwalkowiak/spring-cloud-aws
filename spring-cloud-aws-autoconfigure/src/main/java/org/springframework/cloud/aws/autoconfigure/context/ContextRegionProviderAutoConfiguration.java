/*
 * Copyright 2013-2019 the original author or authors.
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

package org.springframework.cloud.aws.autoconfigure.context;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.autoconfigure.context.properties.AwsRegionProperties;
import org.springframework.cloud.aws.core.config.AmazonWebserviceClientConfigurationUtils;
import org.springframework.cloud.aws.core.region.DefaultAwsRegionProviderChainDelegate;
import org.springframework.cloud.aws.core.region.StaticRegionProvider;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import static org.springframework.cloud.aws.context.config.support.ContextConfigurationUtils.REGION_PROVIDER_BEAN_NAME;

/**
 * Region auto configuration, based on <a
 * href=https://cloud.spring.io/spring-cloud-aws/spring-cloud-aws.html#_configuring_region>cloud.aws.region</a>
 * settings.
 *
 * @author Agim Emruli
 * @author Petromir Dzhunev
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@Import(ContextRegionProviderAutoConfiguration.Registrar.class)
@EnableConfigurationProperties(AwsRegionProperties.class)
public class ContextRegionProviderAutoConfiguration {

	String foo() {
		return null;
	}

	/**
	 * The prefix used for AWS region related properties.
	 */
	public static final String AWS_REGION_PROPERTIES_PREFIX = "cloud.aws.region";

	static class Registrar implements EnvironmentAware, ImportBeanDefinitionRegistrar {

		private Environment environment;

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			// TODO: refactor
			// Do not register region provider if already existing
			if (registry.containsBeanDefinition(REGION_PROVIDER_BEAN_NAME)) {
				return;
			}

			String staticRegion = this.environment
					.getProperty(AWS_REGION_PROPERTIES_PREFIX + ".static");

			AbstractBeanDefinition beanDefinition;

			if (StringUtils.hasText(staticRegion)) {
				BeanDefinitionBuilder builder = BeanDefinitionBuilder
						.genericBeanDefinition(StaticRegionProvider.class);
				builder.addConstructorArgValue(staticRegion);
				beanDefinition = builder.getBeanDefinition();
			}
			else {
				beanDefinition = BeanDefinitionBuilder
						.genericBeanDefinition(
								DefaultAwsRegionProviderChainDelegate.class)
						.getBeanDefinition();
			}

			BeanDefinitionReaderUtils.registerBeanDefinition(
					new BeanDefinitionHolder(beanDefinition, REGION_PROVIDER_BEAN_NAME),
					registry);
			AmazonWebserviceClientConfigurationUtils
					.replaceDefaultRegionProvider(registry, REGION_PROVIDER_BEAN_NAME);
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

	}

}
