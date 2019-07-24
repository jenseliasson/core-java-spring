package eu.arrowhead.common.dto;

import java.io.Serializable;
import java.util.List;

public class CloudListResponseDTO implements Serializable {

	//=================================================================================================
	// members
	
	private static final long serialVersionUID = -7716554887088548705L;

	private List<CloudResponseDTO> data;
	private long count;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------	
	public CloudListResponseDTO() {}
	
	//-------------------------------------------------------------------------------------------------	
	public CloudListResponseDTO(List<CloudResponseDTO> data, long count) {
		this.data = data;
		this.count = count;
	}

	//-------------------------------------------------------------------------------------------------	
	public List<CloudResponseDTO> getData() { return data; }
	public long getCount() { return count; }

	//-------------------------------------------------------------------------------------------------	
	public void setData(List<CloudResponseDTO> data) { this.data = data; }
	public void setCount(long count) { this.count = count; }	
}
