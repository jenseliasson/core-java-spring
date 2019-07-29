package eu.arrowhead.core.gatekeeper.database.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.database.entity.Cloud;
import eu.arrowhead.common.database.entity.CloudGatekeeper;
import eu.arrowhead.common.database.repository.CloudGatekeeperRepository;
import eu.arrowhead.common.database.repository.CloudRepository;
import eu.arrowhead.common.database.service.CommonDBService;
import eu.arrowhead.common.dto.CloudListResponseDTO;
import eu.arrowhead.common.dto.CloudRequestDTO;
import eu.arrowhead.common.dto.CloudResponseDTO;
import eu.arrowhead.common.dto.DTOConverter;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class GatekeeperDBService {

	//=================================================================================================
	// members
	
	@Autowired
	private CloudRepository cloudRepository;
	
	@Autowired
	private CloudGatekeeperRepository cloudGatekeeperRepository;
	
	@Autowired
	private	CommonDBService commonDBService;
	
	private final Logger logger = LogManager.getLogger(GatekeeperDBService.class);
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public CloudListResponseDTO getCloudsResponse(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getCloudsResponse started...");
		
		final Page<Cloud> entries = getClouds(page, size, direction, sortField);
		return DTOConverter.convertCloudListToCloudListResponseDTO(entries);
	}
	
	//-------------------------------------------------------------------------------------------------
	public Page<Cloud> getClouds(final int page, final int size, final Direction direction, final String sortField) {
		logger.debug("getClouds started...");
		
		final int validatedPage = page < 0 ? 0 : page;
		final int validatedSize = size <= 0 ? Integer.MAX_VALUE : size; 		
		final Direction validatedDirection = direction == null ? Direction.ASC : direction;
		final String validatedSortField = Utilities.isEmpty(sortField) ? CommonConstants.COMMON_FIELD_NAME_ID : sortField.trim();
		
		if (!Cloud.SORTABLE_FIELDS_BY.contains(validatedSortField)) {
			throw new InvalidParameterException("Sortable field with reference '" + validatedSortField + "' is not available");
		}
		
		try {
			return cloudRepository.findAll(PageRequest.of(validatedPage, validatedSize, validatedDirection, validatedSortField));
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public CloudResponseDTO getCloudByIdResponse(final long id) {
		logger.debug("getCloudByIdResponse started...");
		
		return DTOConverter.convertCloudToCloudResponseDTO(getCloudById(id));
	}
	
	//-------------------------------------------------------------------------------------------------
	public Cloud getCloudById(final long id) {
		logger.debug("getCloudById started...");
		
		try {
			
			final Optional<Cloud> find = cloudRepository.findById(id);
			if (find.isEmpty()) {
				throw new InvalidParameterException("Cloud with id of '" + id + "' not exists");
			} else {
				return find.get();
			}
			
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public CloudListResponseDTO registerBulkCloudsWithGatekeepersResponse(final List<CloudRequestDTO> validatedDtoList) {
		logger.debug("registerBulkCloudsWithGatekeepersResponse started...");
		
		final List<Cloud> entries = registerBulkCloudsWithGatekeepers(validatedDtoList);
		return DTOConverter.convertCloudListToCloudListResponseDTO(new PageImpl<Cloud>(entries));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Cloud> registerBulkCloudsWithGatekeepers(final List<CloudRequestDTO> validatedDtoList) {
		logger.debug("registerBulkCloudsWithGatekeepers started...");
	
		try {
			
			if (validatedDtoList == null || validatedDtoList.isEmpty()) {
				throw new InvalidParameterException("List of cloudRequestDTO is null or empty");
			}
			final List<CloudRequestDTO> dtoMarkedAsSecureOwnCloud = new ArrayList<>();
			final List<CloudRequestDTO> dtoMarkedAsInsecureOwnCloud = new ArrayList<>();
			for (final CloudRequestDTO dto : validatedDtoList) {
				
				if (dto.getOwnCloud() && dto.getSecure()) {
					dtoMarkedAsSecureOwnCloud.add(dto);
				}
				if (dto.getOwnCloud() && !dto.getSecure()) {
					dtoMarkedAsInsecureOwnCloud.add(dto);
				}
			}
			
			validateOwnCloudRegistrationRequest(dtoMarkedAsSecureOwnCloud, dtoMarkedAsInsecureOwnCloud);
			
			final List<Cloud> cloudsToSave = new ArrayList<>(validatedDtoList.size());
			final Map<String, CloudGatekeeper> gatekeepersByCloudOperatorAndName = new HashMap<>();
			final Set<String> dtoOperatorAndNameCombinations = new HashSet<>();
			final Set<String> dtoAddressPortUriCombinations = new HashSet<>();
			for (final CloudRequestDTO dto : validatedDtoList) {
				
				//Creating cloud
				if (dtoOperatorAndNameCombinations.contains(dto.getOperator() +  dto.getName())) {
					throw new InvalidParameterException("More than one CloudRequestDTO have the following operator and name combination :" + dto.getOperator() + ", " + dto.getName());
				}
				checkUniqueConstraintOfCloudTable(dto.getOperator(), dto.getName());
				final Cloud cloud = new Cloud(dto.getOperator(), dto.getName(), dto.getSecure(), dto.getNeighbor(), dto.getOwnCloud());
				
				//Creating gatekeeper
				if (dtoAddressPortUriCombinations.contains(dto.getAddress() + dto.getPort() + dto.getServiceUri())) {
					throw new InvalidParameterException("More than one CloudRequestDTO have the following address, port and serviceUri combination :" + dto.getAddress() + ", " + dto.getPort() + ", " + dto.getServiceUri());
				}
				checkUniqueConstraintOfCloudGatekeeperTable(null, dto.getAddress(), dto.getPort(), dto.getServiceUri());
				final CloudGatekeeper cloudGatekeeper = new CloudGatekeeper(cloud, dto.getAddress(), dto.getPort(), dto.getServiceUri(), dto.getAuthenticationInfo());
				gatekeepersByCloudOperatorAndName.put(cloud.getOperator() + cloud.getName(), cloudGatekeeper);
				
				cloudsToSave.add(cloud);
				dtoOperatorAndNameCombinations.add(dto.getOperator() + dto.getName());
				dtoAddressPortUriCombinations.add(dto.getAddress() + dto.getPort() + dto.getServiceUri());
			}
			
			final List<Cloud> savedClouds = cloudRepository.saveAll(cloudsToSave);
			cloudRepository.flush();
			
			for (final Cloud savedCloud : savedClouds) {
				final CloudGatekeeper cloudGatekeeper = gatekeepersByCloudOperatorAndName.get(savedCloud.getOperator() + savedCloud.getName());
				cloudGatekeeper.setCloud(savedCloud);
				savedCloud.setGatekeeper(cloudGatekeeper);
			}
			
			cloudGatekeeperRepository.saveAll(gatekeepersByCloudOperatorAndName.values());
			cloudGatekeeperRepository.flush();
			
			return savedClouds;
			
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public CloudListResponseDTO updateCloudsWithGatekeepersResponse(final List<CloudRequestDTO> validatedDtoList) {
		logger.debug("updateCloudsWithGatekeepersResponse started...");
		
		final List<Cloud> entries = updateCloudsWithGatekeepers(validatedDtoList);
		return DTOConverter.convertCloudListToCloudListResponseDTO(new PageImpl<Cloud>(entries));
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Cloud> updateCloudsWithGatekeepers(final List<CloudRequestDTO> validatedDtoList) {
		logger.debug("updateBulkCloudsWithGatekeepers started...");
		final List<Cloud> updatedClouds = new ArrayList<>(validatedDtoList.size());
		
		try {
			
			if (validatedDtoList == null || validatedDtoList.isEmpty()) {
				throw new InvalidParameterException("List of cloudRequestDTO is null or empty");
			}
			
			final Set<String> cloudUniqueContraintsFromDTOs = new HashSet<>();
			final Set<String> gatekeeperUniqueContraintsFromDTOs = new HashSet<>();			
			for (final CloudRequestDTO dto : validatedDtoList) {
				if(dto.getId() == null || dto.getId() < 1) {
					throw new InvalidParameterException("DTO has invalid id: " + dto);
				}
				
				final Optional<Cloud> existingCloud = cloudRepository.findById(dto.getId());
				if (existingCloud.isEmpty()) {
					throw new InvalidParameterException("No cloud exists with the following id: " + dto.getId());
				}
				
				//Own cloud can't be updated and a not own cloud can't be updated for being own cloud as this is managed automatically by the ApplicationInitListener
				if (existingCloud.get().getOwnCloud()) {
					throw new InvalidParameterException("Own cloud can't be updated as it is managed automatically by the ApplicationInitListener. Own cloud: " + existingCloud.get());
				}
				if(dto.getOwnCloud()) {
					throw new InvalidParameterException("A not own cloud can't be updated for being an own cloud as it is managed automatically by the ApplicationInitListener. Cloud: "+ existingCloud.get());
				}
				
				//Checking unique constraints
				if(cloudUniqueContraintsFromDTOs.contains(dto.getOperator() + dto.getName())) {
					throw new InvalidParameterException("DTO list contains cloud unique constraint violation (oprator, name): " + dto.getOperator() + ", " + dto.getName());
				}
				if(gatekeeperUniqueContraintsFromDTOs.contains(dto.getAddress() + dto.getPort() + dto.getServiceUri())) {
					throw new InvalidParameterException("DTO list contains gatekeeper unique constraint violation (address, port, serviceUri): " + dto.getAddress() + ", " + dto.getPort() + ", " + dto.getServiceUri());
				}
				if(!dto.getOperator().equalsIgnoreCase(existingCloud.get().getOperator()) || !dto.getName().equalsIgnoreCase(existingCloud.get().getName())) {
					checkUniqueConstraintOfCloudTable(dto.getOperator(), dto.getName());					
				}
				if (!dto.getAddress().equalsIgnoreCase(existingCloud.get().getGatekeeper().getAddress())
						|| dto.getPort() != existingCloud.get().getGatekeeper().getPort()
						|| !dto.getServiceUri().equalsIgnoreCase(existingCloud.get().getGatekeeper().getServiceUri())) {
					
					checkUniqueConstraintOfCloudGatekeeperTable(null, dto.getAddress(), dto.getPort(), dto.getServiceUri());
				}
				
				//Update cloud
				final Cloud cloudToBeUpdated = existingCloud.get();
				final CloudGatekeeper gatekeeperToBeUpdated = existingCloud.get().getGatekeeper();
				
				gatekeeperToBeUpdated.setAddress(dto.getAddress());
				gatekeeperToBeUpdated.setPort(dto.getPort());
				gatekeeperToBeUpdated.setServiceUri(dto.getServiceUri());
				gatekeeperToBeUpdated.setAuthenticationInfo(dto.getAuthenticationInfo());
				gatekeeperUniqueContraintsFromDTOs.add(dto.getAddress() + dto.getPort() + dto.getServiceUri());
				cloudGatekeeperRepository.saveAndFlush(gatekeeperToBeUpdated);
				
				cloudToBeUpdated.setOperator(dto.getOperator());
				cloudToBeUpdated.setName(dto.getName());
				cloudToBeUpdated.setSecure(dto.getSecure());
				cloudToBeUpdated.setNeighbor(dto.getNeighbor());
				cloudUniqueContraintsFromDTOs.add(dto.getOperator() + dto.getName());
				updatedClouds.add(cloudRepository.saveAndFlush(cloudToBeUpdated));				
			}
			
			return updatedClouds;
			
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void removeCloudById(final long id) {
		try {

			if(cloudRepository.existsById(id)) {
				cloudRepository.deleteById(id);
			}
			
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	public CloudGatekeeper getGatekeeperByCloud(final Cloud cloud) {
		logger.debug("getGatekeeperByCloud started...");
		
		try {
			
			final Optional<CloudGatekeeper> gatekeeperOpt = cloudGatekeeperRepository.findByCloud(cloud);
			if (gatekeeperOpt.isEmpty()) {
				throw new InvalidParameterException("Gatekeeper with cloud: " + cloud + " not exists.");
			} else {
				return gatekeeperOpt.get();
			}
			
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public CloudGatekeeper registerGatekeeper(final Cloud cloud, final String address, final int port, final String serviceUri, final String authenticationInfo) {
		logger.debug("registerGatekeeper started...");
		
		try {
			
			Assert.isTrue(cloud != null, "Cloud is null.");
			Assert.isTrue(!Utilities.isEmpty(address), "Address is null or empty.");
			Assert.isTrue(!Utilities.isEmpty(serviceUri), "ServiceUri is null or empty.");
						
			if (isPortOutOfValidRange(port)) {
				throw new InvalidParameterException("Port must be between " + CommonConstants.SYSTEM_PORT_RANGE_MIN + " and " + CommonConstants.SYSTEM_PORT_RANGE_MAX + ".");
			}
			
			if (cloud.getSecure() && Utilities.isEmpty(authenticationInfo)) {
				throw new InvalidParameterException("Gatekeeper without or with blank authenticationInfo cannot be registered for a secured cloud. Cloud: " + cloud);
			}
			
			final String validatedAddress = address.toLowerCase().trim();
			final String validatedServiceUri = serviceUri.trim();
			
			checkUniqueConstraintOfCloudGatekeeperTable(cloud, validatedAddress, port, validatedServiceUri);
			
			final CloudGatekeeper gatekeeper = new CloudGatekeeper(cloud, validatedAddress, port, validatedServiceUri, authenticationInfo);
			return cloudGatekeeperRepository.saveAndFlush(gatekeeper);			
			
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException(ex.getMessage());
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public CloudGatekeeper updateGatekeeper(final CloudGatekeeper gatekeeper, final String address, final int port, final String serviceUri, final String authenticationInfo) {
		logger.debug("registerGatekeeper started...");
		
		try {
			
			Assert.isTrue(gatekeeper != null, "Gatekeeper is null.");
			Assert.isTrue(!Utilities.isEmpty(address), "Address is null or empty.");
			Assert.isTrue(!Utilities.isEmpty(serviceUri), "ServiceUri is null or empty.");					
			
			if (isPortOutOfValidRange(port)) {
				throw new InvalidParameterException("Port must be between " + CommonConstants.SYSTEM_PORT_RANGE_MIN + " and " + CommonConstants.SYSTEM_PORT_RANGE_MAX + ".");
			}
						
			if (gatekeeper.getCloud().getSecure() && Utilities.isEmpty(authenticationInfo)) {
				throw new InvalidParameterException("Gatekeeper without or with blank authenticationInfo cannot be registered for a secured cloud. Cloud: " + gatekeeper.getCloud());
			}
								
			final String validatedAddress = address.toLowerCase().trim();
			final String validatedServiceUri = serviceUri.trim();
			
			if(!gatekeeper.getAddress().equals(validatedAddress) || gatekeeper.getPort() != port || !gatekeeper.getServiceUri().equals(validatedServiceUri)) {
				checkUniqueConstraintOfCloudGatekeeperTable(null, validatedAddress, port, validatedServiceUri);			
			}
			
			gatekeeper.setAddress(validatedAddress);
			gatekeeper.setPort(port);
			gatekeeper.setServiceUri(validatedServiceUri);
			gatekeeper.setAuthenticationInfo(authenticationInfo);
			
			return cloudGatekeeperRepository.saveAndFlush(gatekeeper);
			
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException(ex.getMessage());
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}		
	}
	
	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void removeGatekeeper(final long id) {
		try {

			if(cloudGatekeeperRepository.existsById(id)) {
				cloudGatekeeperRepository.deleteById(id);
			}
			
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//=================================================================================================
	// assistant methods
	
	//-------------------------------------------------------------------------------------------------
	private void checkUniqueConstraintOfCloudGatekeeperTable(final Cloud cloud, final String address, final int port, final String serviceUri) {
		logger.debug("checkUniqueConstraintOfCloudGatekeeperTable started...");
		
		try {
			
			if (cloud != null) {
				final Optional<CloudGatekeeper> gatekeeperOpt = cloudGatekeeperRepository.findByCloud(cloud);
				if (gatekeeperOpt.isPresent()) {
					throw new InvalidParameterException("Gatekeeper with cloud: " + cloud + " already exists.");
				} 
				
			} 			
			
			final Optional<CloudGatekeeper> gatekeeperOpt = cloudGatekeeperRepository.findByAddressAndPortAndServiceUri(address, port, serviceUri);
			if (gatekeeperOpt.isPresent()) {
				throw new InvalidParameterException("Gatekeeper with address: " + address + ", port: " + port + ", serviceUri: " + serviceUri + " already exists.");
			}
			
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private void checkUniqueConstraintOfCloudTable(final String operator, final String name) {
		logger.debug("checkUniqueConstraintOfCloudTable started...");		
		
		try {
			
			final Optional<Cloud> cloudOpt = cloudRepository.findByOperatorAndName(operator, name);
			if (cloudOpt.isPresent()) {
				throw new InvalidParameterException("Cloud wit operator: " + operator + " and name : " + "already exists");
			}
			
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
	
	//-------------------------------------------------------------------------------------------------
	private boolean isPortOutOfValidRange(final int port) {
		logger.debug("isPortOutOfValidRange started...");
		return port < CommonConstants.SYSTEM_PORT_RANGE_MIN || port > CommonConstants.SYSTEM_PORT_RANGE_MAX;
	}
	
	//-------------------------------------------------------------------------------------------------
	private void validateOwnCloudRegistrationRequest(final List<CloudRequestDTO> dtoMarkedAsSecureOwnCloud, final List<CloudRequestDTO> dtoMarkedAsInsecureOwnCloud) {
		logger.debug("validateOwnCloudRegistrationRequest started...");
		
		try {
		
			if (!dtoMarkedAsSecureOwnCloud.isEmpty()) {
				try {
					commonDBService.getOwnCloud(true);	
					throw new InvalidParameterException("Secure own cloud already exists.");
				} catch (final DataNotFoundException ex) {
					if (dtoMarkedAsSecureOwnCloud.size() > 1) {
						throw new InvalidParameterException("More than one CloudRequestDTO marked as secure own cloud.");
					}
				}
			}
			
			if (!dtoMarkedAsInsecureOwnCloud.isEmpty()) {
				try {
					commonDBService.getOwnCloud(false);
					throw new InvalidParameterException("Insecure own cloud already exists.");
				} catch (final DataNotFoundException ex) {
					if (dtoMarkedAsInsecureOwnCloud.size() > 1) {
						throw new InvalidParameterException("More than one CloudRequestDTO marked as insecure own cloud.");
					}
				}
				
			}
			
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.debug(ex.getMessage(), ex);
			throw new ArrowheadException(CommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
		}
	}
}
