package eu.arrowhead.common.dto;

import java.io.Serializable;

public class SystemRequestDTO implements Serializable {

	
	//=================================================================================================
	// members
	
	private static final long serialVersionUID = 3919207845374510215L;

	private Long systemId; // only used in update requests
	private String systemName;
	private String address;
	private Integer port;
	private String authenticationInfo;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	
	public Long getSystemId() { return systemId;}
	public String getSystemName() { return systemName; }
	public String getAddress() { return address; }
	public Integer getPort() { return port;	}
	public String getAuthenticationInfo() {	return authenticationInfo; }
	
	//-------------------------------------------------------------------------------------------------
	
	public void setSystemId(final Long systemId) { this.systemId = systemId; }
	public void setSystemName(final String systemName) { this.systemName = systemName; }
	public void setAddress(final String address) { this.address = address; }
	public void setPort(final Integer port) { this.port = port; }
	public void setAuthenticationInfo(final String authenticationInfo) { this.authenticationInfo = authenticationInfo;	}
	
}