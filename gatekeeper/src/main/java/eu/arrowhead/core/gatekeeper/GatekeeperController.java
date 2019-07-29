package eu.arrowhead.core.gatekeeper;

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.CloudListResponseDTO;
import eu.arrowhead.common.dto.CloudRequestDTO;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.core.gatekeeper.database.service.GatekeeperDBService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@CrossOrigin(maxAge = Defaults.CORS_MAX_AGE, allowCredentials = Defaults.CORS_ALLOW_CREDENTIALS, 
allowedHeaders = { HttpHeaders.ORIGIN, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION }
)
@RestController
@RequestMapping(CommonConstants.GATEKEEPER_URI)
public class GatekeeperController {
	
	//=================================================================================================
	// members

	private static final String PATH_VARIABLE_ID = "id";
	private static final String ID_NOT_VALID_ERROR_MESSAGE = "Id must be greater than 0.";
	
	private static final String GATEKEEPER_MGMT_CLOUDS_URI = CommonConstants.MGMT_URI + "/clouds";
	private static final String GATEKEEPER_MGMT_CLOUDS_BY_ID_URI = GATEKEEPER_MGMT_CLOUDS_URI + "/{" + PATH_VARIABLE_ID + "}";
	
	private static final String POST_GATEKEEPER_MGMT_CLOUDS_HTTP_201_MESSAGE = "Cloud(s) created";
	private static final String POST_GATEKEEPER_MGMT_CLOUDS_HTTP_400_MESSAGE = "Could not create Cloud(s)";
	private static final String PUT_GATEKEEPER_MGMT_CLOUDS_HTTP_201_MESSAGE = "Cloud(s) updated";
	private static final String PUT_GATEKEEPER_MGMT_CLOUDS_HTTP_400_MESSAGE = "Could not update Cloud(s)";
	private static final String DELETE_GATEKEEPER_MGMT_CLOUDS_HTTP_200_MESSAGE = "Cloud removed";
	private static final String DELETE_GATEKEEPER_MGMT_CLOUDS_HTTP_400_MESSAGE = "Could not remove Cloud";
	
	private final Logger logger = LogManager.getLogger(GatekeeperController.class);
	
	@Autowired
	private GatekeeperDBService gatekeeperDBService;
	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = "Return an echo message with the purpose of testing the core service availability", response = String.class)
	@ApiResponses (value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = CommonConstants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@GetMapping(path = CommonConstants.ECHO_URI)
	public String echoService() {
		return "Got it!";
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = "Create the requested Cloud entries", response = CloudListResponseDTO.class)
	@ApiResponses (value = {
			@ApiResponse(code = HttpStatus.SC_CREATED, message = POST_GATEKEEPER_MGMT_CLOUDS_HTTP_201_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = POST_GATEKEEPER_MGMT_CLOUDS_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@PostMapping(path = GATEKEEPER_MGMT_CLOUDS_URI, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(value = org.springframework.http.HttpStatus.CREATED)
	@ResponseBody public CloudListResponseDTO registerClouds(@RequestBody final List<CloudRequestDTO> request) {
		logger.debug("registerClouds started...");
		
		if (request == null || request.isEmpty()) {
			throw new BadPayloadException("CloudRequestDTO is empty.", HttpStatus.SC_BAD_REQUEST, CommonConstants.GATEKEEPER_URI + GATEKEEPER_MGMT_CLOUDS_URI);
		}
		
		for (final CloudRequestDTO dto : request) {
			validateCloudRequestDTO(dto, CommonConstants.GATEKEEPER_URI + GATEKEEPER_MGMT_CLOUDS_URI);
		}
		
		final CloudListResponseDTO registeredEntries = gatekeeperDBService.registerBulkCloudsWithGatekeepersResponse(request);
		
		logger.debug("registerClouds has been finished.");
		return registeredEntries;
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = "Update the requested Cloud entries", response = CloudListResponseDTO.class)
	@ApiResponses (value = {
			@ApiResponse(code = HttpStatus.SC_CREATED, message = PUT_GATEKEEPER_MGMT_CLOUDS_HTTP_201_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = PUT_GATEKEEPER_MGMT_CLOUDS_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@PutMapping(path = GATEKEEPER_MGMT_CLOUDS_URI, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public CloudListResponseDTO updateClouds(@RequestBody final List<CloudRequestDTO> request) {
		logger.debug("updateClouds started...");
		
		if (request == null || request.isEmpty()) {
			throw new BadPayloadException("CloudRequestDTO is empty.", HttpStatus.SC_BAD_REQUEST, CommonConstants.GATEKEEPER_URI + GATEKEEPER_MGMT_CLOUDS_URI);
		}
		
		for (final CloudRequestDTO dto : request) {
			validateCloudRequestDTO(dto, CommonConstants.GATEKEEPER_URI + GATEKEEPER_MGMT_CLOUDS_URI);
		}
		
		final CloudListResponseDTO updatedEntries = gatekeeperDBService.updateCloudsWithGatekeepersResponse(request);
		
		logger.debug("updateClouds has been finished.");
		return updatedEntries;
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = "Remove the requested Cloud entry with its Gatekeeper")
	@ApiResponses (value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = DELETE_GATEKEEPER_MGMT_CLOUDS_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = DELETE_GATEKEEPER_MGMT_CLOUDS_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@DeleteMapping(path = GATEKEEPER_MGMT_CLOUDS_BY_ID_URI)
	public void removeCloudById(@PathVariable(value = PATH_VARIABLE_ID) final long id) {
		logger.debug("removeCloud started...");
		
		if (id < 1) {
			throw new BadPayloadException(ID_NOT_VALID_ERROR_MESSAGE, HttpStatus.SC_BAD_REQUEST, CommonConstants.GATEKEEPER_URI + GATEKEEPER_MGMT_CLOUDS_BY_ID_URI);
		}
		
		gatekeeperDBService.removeCloudById(id);
		logger.debug("removeCloud has been finished.");
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private void validateCloudRequestDTO(final CloudRequestDTO dto, final String origin) {
		logger.debug("validateCloudRequestDTO started...");
		
		if (dto.getId() != null && dto.getId() < 1) {
			throw new BadPayloadException(ID_NOT_VALID_ERROR_MESSAGE, HttpStatus.SC_BAD_REQUEST, origin);
		}
		
		final boolean isOperatorInvalid = Utilities.isEmpty(dto.getOperator());
		final boolean isNameInvalid = Utilities.isEmpty(dto.getName());
		final boolean isAddressInvalid = Utilities.isEmpty(dto.getAddress());
		final boolean isPortInvalid = dto.getPort() == null || isPortOutOfValidRange(dto.getPort());
		final boolean isServiceUriInvalid = Utilities.isEmpty(dto.getServiceUri());
		final boolean isSecureFlagMissing = dto.getSecure() == null;
		final boolean isNeighborFlagMissing = dto.getNeighbor() == null;
		final boolean isOwnCloudFlagMissing = dto.getOwnCloud() == null;
		
		if (isOperatorInvalid || isNameInvalid || isAddressInvalid || isPortInvalid || isServiceUriInvalid) {
			String exceptionMessage = "Following parameters are not valid:";
			exceptionMessage = isOperatorInvalid ? exceptionMessage + " operator is empty," : exceptionMessage;
			exceptionMessage = isNameInvalid ? exceptionMessage + " name is empty," : exceptionMessage;
			exceptionMessage = isAddressInvalid ? exceptionMessage + " address is empty," : exceptionMessage;
			exceptionMessage = isPortInvalid ? exceptionMessage + " port is missing or out of valid range," : exceptionMessage;
			exceptionMessage = isServiceUriInvalid ? exceptionMessage + " serviceUri is empty," : exceptionMessage;
			exceptionMessage = exceptionMessage.substring(0, exceptionMessage.length() - 1);
			
			throw new BadPayloadException(exceptionMessage, HttpStatus.SC_BAD_REQUEST, origin);
		}
		
		dto.setOperator(dto.getOperator().toLowerCase().trim());
		dto.setName(dto.getName().toLowerCase().trim());
		dto.setAddress(dto.getAddress().toLowerCase().trim());
		dto.setServiceUri(dto.getServiceUri().trim());
		if (isSecureFlagMissing) {
			dto.setSecure(false);
		}
		if (isNeighborFlagMissing) {
			dto.setNeighbor(false);
		}
		if (isOwnCloudFlagMissing) {
			dto.setOwnCloud(false);
		}
		
		if(dto.getSecure() && Utilities.isEmpty(dto.getAuthenticationInfo())) {
			throw new BadPayloadException("Gatekeeper without or with blank authenticationInfo cannot be registered for a secured cloud.", HttpStatus.SC_BAD_REQUEST, origin);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private boolean isPortOutOfValidRange(final int port) {
		logger.debug("isPortOutOfValidRange started...");
		return port < CommonConstants.SYSTEM_PORT_RANGE_MIN || port > CommonConstants.SYSTEM_PORT_RANGE_MAX;
	}
}