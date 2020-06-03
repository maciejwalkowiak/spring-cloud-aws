package org.springframework.cloud.aws.autoconfigure.jdbc;

import java.util.Optional;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Region;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.aws.core.region.RegionProvider;

@ConfigurationProperties("cloud.aws.rds-config")
public class RdsProperties {

	private boolean enabled;

	private EndpointProperties endpoint;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public EndpointProperties getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(EndpointProperties endpoint) {
		this.endpoint = endpoint;
	}

	public Optional<EndpointConfiguration> endpointConfiguration(Optional<RegionProvider> regionProvider) {
		if (this.endpoint != null) {
			String region = this.endpoint.region != null ? this.endpoint.region
					: regionProvider.map(RegionProvider::getRegion).map(Region::getName).orElse(null);
			return Optional.of(new EndpointConfiguration(this.endpoint.url, region));
		}
		else {
			return Optional.empty();
		}
	}

	public static class EndpointProperties {

		private String url;

		private String region;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}

	}

}
