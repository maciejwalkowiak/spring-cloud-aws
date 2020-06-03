package org.springframework.cloud.aws.autoconfigure.jdbc;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.aws.jdbc.config.annotation.RdsInstanceConfigurer;
import org.springframework.cloud.aws.jdbc.rds.AmazonRdsDataSourceFactoryBean;

/**
 * Bean post processor for RDS instance configurer.
 */
public class RdsInstanceConfigurerBeanPostProcessor
		implements BeanPostProcessor, BeanFactoryAware {

	private RdsInstanceConfigurer rdsInstanceConfigurer;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof AmazonRdsDataSourceFactoryBean
				&& this.rdsInstanceConfigurer != null) {
			((AmazonRdsDataSourceFactoryBean) bean).setDataSourceFactory(
					this.rdsInstanceConfigurer.getDataSourceFactory());
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ListableBeanFactory) {
			Collection<RdsInstanceConfigurer> configurer = ((ListableBeanFactory) beanFactory)
					.getBeansOfType(RdsInstanceConfigurer.class).values();

			if (configurer.isEmpty()) {
				return;
			}

			if (configurer.size() > 1) {
				throw new IllegalStateException(
						"Only one RdsInstanceConfigurer may exist");
			}

			this.rdsInstanceConfigurer = configurer.iterator().next();
		}
	}

}
