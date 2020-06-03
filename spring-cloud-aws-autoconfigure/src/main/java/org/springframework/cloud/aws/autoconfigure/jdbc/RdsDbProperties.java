package org.springframework.cloud.aws.autoconfigure.jdbc;

public class RdsDbProperties {
	private final String username;
	private final String password;
	private final String databaseName;
	private final boolean readReplicaSupport;

	public RdsDbProperties(String username, String password, String databaseName, boolean readReplicaSupport) {
		this.username = username;
		this.password = password;
		this.databaseName = databaseName;
		this.readReplicaSupport = readReplicaSupport;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public boolean isReadReplicaSupport() {
		return readReplicaSupport;
	}
}
