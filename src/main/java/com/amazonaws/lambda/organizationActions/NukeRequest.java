package com.amazonaws.lambda.organizationActions;

public class NukeRequest {
	String region;
	String prefix;
	Boolean skipGuardDuty;
	Boolean skipSecHub;
		
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public Boolean getSkipGuardDuty() {
		return skipGuardDuty;
	}
	public void setSkipGuardDuty(Boolean skipGuardDuty) {
		this.skipGuardDuty = skipGuardDuty;
	}
	public Boolean getSkipSecHub() {
		return skipSecHub;
	}
	public void setSkipSecHub(Boolean skipSecHub) {
		this.skipSecHub = skipSecHub;
	}
	
}
