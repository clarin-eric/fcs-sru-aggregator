package eu.clarin.sru.fcs.aggregator.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import java.util.concurrent.TimeUnit;
import org.hibernate.validator.constraints.NotEmpty;

public class AggregatorConfiguration extends Configuration {

	@NotEmpty
	private String centerRegistryUrl;

	@NotEmpty
	private String weblichtUrl;

	@NotEmpty
	private String updateIntervalUnit;

	private int updateInterval;

	private int scanMaxDepth;

	@NotEmpty
	private String aggregatorFilePath;

	@JsonProperty
	public String getAggregatorFilePath() {
		return aggregatorFilePath;
	}

	@JsonProperty
	public String getCenterRegistryUrl() {
		return centerRegistryUrl;
	}

	@JsonProperty
	public int getScanMaxDepth() {
		return scanMaxDepth;
	}

	@JsonProperty
	public int getUpdateInterval() {
		return updateInterval;
	}

	@JsonProperty
	public String getUpdateIntervalUnit() {
		return updateIntervalUnit;
	}

	public TimeUnit getUpdateIntervalTimeUnit() {
		return TimeUnit.valueOf(updateIntervalUnit);
	}

	@JsonProperty
	public String getWeblichtUrl() {
		return weblichtUrl;
	}

	@JsonProperty
	public void setAggregatorFilePath(String aggregatorFilePath) {
		this.aggregatorFilePath = aggregatorFilePath;
	}

	@JsonProperty
	public void setCenterRegistryUrl(String centerRegistryUrl) {
		this.centerRegistryUrl = centerRegistryUrl;
	}

	@JsonProperty
	public void setScanMaxDepth(int scanMaxDepth) {
		this.scanMaxDepth = scanMaxDepth;
	}

	@JsonProperty
	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}

	@JsonProperty
	public void setUpdateIntervalUnit(String updateIntervalUnit) {
		this.updateIntervalUnit = updateIntervalUnit;
	}

	@JsonProperty
	public void setWeblichtUrl(String weblichtUrl) {
		this.weblichtUrl = weblichtUrl;
	}
}
