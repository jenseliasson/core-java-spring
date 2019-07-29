package eu.arrowhead.common.dto;

import java.io.Serializable;

public class CloudRequestDTO implements Serializable {

	//=================================================================================================
	// members
	
	private static final long serialVersionUID = 7397917411719621910L;
	
	private Long id;
	private String operator;
	private String name;
	private String address;
	private Integer port;
	private String serviceUri;
	private String authenticationInfo;
	private Boolean secure;
	private Boolean neighbor;
	private Boolean ownCloud;
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Long getId() { return id; }
	public String getOperator() { return operator; }
	public String getName() { return name; }
	public String getAddress() { return address; }
	public Integer getPort() { return port; }
	public String getServiceUri() { return serviceUri; }
	public String getAuthenticationInfo() { return authenticationInfo; }
	public Boolean getSecure() { return secure; }
	public Boolean getNeighbor() { return neighbor; }
	public Boolean getOwnCloud() { return ownCloud; }
	
	//-------------------------------------------------------------------------------------------------
	public void setId(final Long id) { this.id = id; }
	public void setOperator(final String operator) { this.operator = operator; }
	public void setName(final String name) { this.name = name; }
	public void setAddress(final String address) { this.address = address; }
	public void setPort(final Integer port) { this.port = port; }
	public void setServiceUri(final String serviceUri) { this.serviceUri = serviceUri; }
	public void setAuthenticationInfo(final String authenticationInfo) { this.authenticationInfo = authenticationInfo; }
	public void setSecure(final Boolean secure) { this.secure = secure; }
	public void setNeighbor(final Boolean neighbor) { this.neighbor = neighbor; }
	public void setOwnCloud(final Boolean ownCloud) { this.ownCloud = ownCloud; }
}