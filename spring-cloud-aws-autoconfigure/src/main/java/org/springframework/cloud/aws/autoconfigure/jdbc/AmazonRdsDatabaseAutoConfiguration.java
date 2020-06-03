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

package org.springframework.cloud.aws.autoconfigure.jdbc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.aws.context.config.xml.GlobalBeanDefinitionUtils;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.cloud.aws.jdbc.datasource.TomcatJdbcDataSourceFactory;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsReadReplicaAwareDataSourceFactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * @author Agim Emruli
 * @author Alain Sahli
 * @author Maciej Walkowiak
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@Import(AmazonRdsDatabaseAutoConfiguration.Registrar.class)
@ConditionalOnClass(com.amazonaws.services.rds.AmazonRDSClient.class)
@EnableConfigurationProperties(RdsProperties.class)
@ConditionalOnProperty(name = "cloud.aws.rds-config.enabled", havingValue = "true", matchIfMissing = true)
public class AmazonRdsDatabaseAutoConfiguration {

	private static final String AWS_RDS_CLIENT_BEAN_NAME = "amazonRDSClient";

	@Autowired
	private RdsProperties rdsProperties;

	@Bean
	public RdsInstanceConfigurerBeanPostProcessor rdsInstanceConfigurerBeanPostProcessor() {
		return new RdsInstanceConfigurerBeanPostProcessor();
	}

	@Bean
	@ConditionalOnMissingBean(AmazonRDSClient.class)
	AmazonRDSClient amazonRDSClient(Optional<RegionProvider> regionProvider,
			Optional<ClientConfiguration> clientConfiguration, Optional<AWSCredentialsProvider> credentialsProvider) {
		AmazonRDSClientBuilder builder = AmazonRDSClientBuilder.standard();
		clientConfiguration.ifPresent(builder::withClientConfiguration);
		credentialsProvider.ifPresent(builder::withCredentials);
		regionProvider.ifPresent(it -> builder.withRegion(it.getRegion().getName()));
		rdsProperties.endpointConfiguration(regionProvider).ifPresent(builder::withEndpointConfiguration);
		return (AmazonRDSClient) builder.build();
	}

	/**
	 * Registrar for Amazon RDS.
	 */
	public static class Registrar implements EnvironmentAware, ImportBeanDefinitionRegistrar {

		private static final String PREFIX = "cloud.aws.rds";

		private ConfigurableEnvironment environment;

		@Override
		public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
				BeanDefinitionRegistry registry) {
			Map<String, RdsDbProperties> dbInstanceConfigurations = getDbInstanceConfigurations();
			for (Map.Entry<String, RdsDbProperties> dbInstanceEntry : dbInstanceConfigurations.entrySet()) {
				registerDataSource(registry, dbInstanceEntry.getKey(), dbInstanceEntry.getValue().getPassword(),
						dbInstanceEntry.getValue().isReadReplicaSupport(), dbInstanceEntry.getValue().getUsername(),
						dbInstanceEntry.getValue().getDatabaseName());
			}
		}

		@Override
		public void setEnvironment(Environment environment) {
			Assert.isInstanceOf(ConfigurableEnvironment.class, environment,
					"Amazon RDS auto configuration requires a configurable environment");
			this.environment = (ConfigurableEnvironment) environment;
		}

		protected void registerDataSource(BeanDefinitionRegistry beanDefinitionRegistry, String dbInstanceIdentifier,
				String password, boolean readReplica, String userName, String databaseName) {
			BeanDefinitionBuilder datasourceBuilder = getBeanDefinitionBuilderForDataSource(readReplica);

			// Constructor (mandatory) args
			datasourceBuilder.addConstructorArgReference(AWS_RDS_CLIENT_BEAN_NAME);
			datasourceBuilder.addConstructorArgValue(dbInstanceIdentifier);
			datasourceBuilder.addConstructorArgValue(password);

			// optional args
			datasourceBuilder.addPropertyValue("username", userName);
			datasourceBuilder.addPropertyValue("databaseName", databaseName);

			String resourceResolverBeanName = GlobalBeanDefinitionUtils
					.retrieveResourceIdResolverBeanName(beanDefinitionRegistry);
			datasourceBuilder.addPropertyReference("resourceIdResolver", resourceResolverBeanName);

			datasourceBuilder.addPropertyValue("dataSourceFactory",
					BeanDefinitionBuilder.rootBeanDefinition(TomcatJdbcDataSourceFactory.class).getBeanDefinition());

			beanDefinitionRegistry.registerBeanDefinition(dbInstanceIdentifier, datasourceBuilder.getBeanDefinition());
		}

		private BeanDefinitionBuilder getBeanDefinitionBuilderForDataSource(boolean readReplicaEnabled) {
			BeanDefinitionBuilder datasourceBuilder;
			if (readReplicaEnabled) {
				datasourceBuilder = BeanDefinitionBuilder
						.rootBeanDefinition(AmazonRdsReadReplicaAwareDataSourceFactoryBean.class);
			}
			else {
				datasourceBuilder = BeanDefinitionBuilder.rootBeanDefinition(AmazonRdsDataSourceFactoryBean.class);
			}
			return datasourceBuilder;
		}

		@SuppressWarnings("unchecked")
		private Map<String, RdsDbProperties> getDbInstanceConfigurations() {
			Map<String, Object> subProperties = Binder.get(this.environment)
					.bind(PREFIX, Bindable.mapOf(String.class, Object.class)).orElseGet(Collections::emptyMap);
			Map<String, RdsDbProperties> dbConfigurationMap = new HashMap<>(subProperties.keySet().size());
			for (Entry<String, Object> subProperty : subProperties.entrySet()) {
				String instanceName = subProperty.getKey();
				Map<String, String> value = (Map<String, String>) subProperty.getValue();

				dbConfigurationMap.put(instanceName, new RdsDbProperties(value.get("username"), value.get("password"),
						value.get("databaseName"), Boolean.parseBoolean(value.get("readReplicaSupport"))));
			}
			return dbConfigurationMap;
		}

	}

}
