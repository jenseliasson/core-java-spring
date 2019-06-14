package eu.arrowhead.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ServiceQueryResultDTO implements Serializable {

	//=================================================================================================
	// members
	
	private static final long serialVersionUID = -1822444510232108526L;
	
	private List<ServiceRegistryResponseDTO> serviceQueryData = new ArrayList<>();

	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public List<ServiceRegistryResponseDTO> getServiceQueryData() { return serviceQueryData; }

	//-------------------------------------------------------------------------------------------------
	public void setServiceQueryData(final List<ServiceRegistryResponseDTO> serviceQueryData) { this.serviceQueryData = serviceQueryData; }
}