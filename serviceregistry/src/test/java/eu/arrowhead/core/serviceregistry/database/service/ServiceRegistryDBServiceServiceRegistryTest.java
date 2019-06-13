package eu.arrowhead.core.serviceregistry.database.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.junit4.SpringRunner;

import eu.arrowhead.common.database.entity.ServiceDefinition;
import eu.arrowhead.common.database.entity.ServiceRegistry;
import eu.arrowhead.common.database.entity.System;
import eu.arrowhead.common.database.repository.ServiceDefinitionRepository;
import eu.arrowhead.common.database.repository.ServiceRegistryRepository;
import eu.arrowhead.common.database.repository.SystemRepository;
import eu.arrowhead.common.dto.ServiceRegistryRequestDTO;
import eu.arrowhead.common.dto.ServiceSecurityType;
import eu.arrowhead.common.dto.SystemRequestDTO;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.core.serviceregistry.intf.ServiceInterfaceNameVerifier;

@RunWith (SpringRunner.class)
public class ServiceRegistryDBServiceServiceRegistryTest {
	
	//=================================================================================================
	// members
		
	@InjectMocks
	private ServiceRegistryDBService serviceRegistryDBService; 
	
	@Mock
	private ServiceRegistryRepository serviceRegistryRepository;
	
	@Mock
	private ServiceDefinitionRepository serviceDefinitionRepository;
	
	@Mock
	private SystemRepository systemRepository;
	
	@Spy
	private ServiceInterfaceNameVerifier interfaceNameVerifier;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	//Tests of getServiceReqistryEntries
		
	@Test (expected = InvalidParameterException.class)
	public void getServiceReqistryEntriesTest() {
		serviceRegistryDBService.getServiceReqistryEntries(0, 10, Direction.ASC, "notValid");
	}
		
	//-------------------------------------------------------------------------------------------------
	//Tests of removeServiceRegistryEntryById
		
	@Test (expected = InvalidParameterException.class)
	public void removeServiceRegistryEntryByIdTest() {
		when(serviceRegistryRepository.existsById(anyLong())).thenReturn(false);
		serviceRegistryDBService.removeServiceRegistryEntryById(1);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseNullParam() {
		serviceRegistryDBService.registerServiceResponse(null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseServiceDefinitionNull() {
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseServiceDefinitionEmpty() {
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition(" ");
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseProviderSystemNull() {
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition("service_definition");
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseProviderSystemNameNull() {
		final SystemRequestDTO sysDto = new SystemRequestDTO(); 
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition("service_definition");
		dto.setProviderSystem(sysDto);
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseProviderSystemNameEmpty() {
		final SystemRequestDTO sysDto = new SystemRequestDTO(); 
		sysDto.setSystemName(" ");
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition("service_definition");
		dto.setProviderSystem(sysDto);
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseProviderSystemAddressNull() {
		final SystemRequestDTO sysDto = new SystemRequestDTO(); 
		sysDto.setSystemName("system");
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition("service_definition");
		dto.setProviderSystem(sysDto);
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseProviderSystemAddressEmpty() {
		final SystemRequestDTO sysDto = new SystemRequestDTO(); 
		sysDto.setSystemName("system");
		sysDto.setAddress(" ");
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition("service_definition");
		dto.setProviderSystem(sysDto);
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class) 
	public void testRegisterServiceResponseProviderSystemPortNull() {
		final SystemRequestDTO sysDto = new SystemRequestDTO(); 
		sysDto.setSystemName("system");
		sysDto.setAddress("localhost");
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition("service_definition");
		dto.setProviderSystem(sysDto);
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class) 
	public void testRegisterServiceResponseEndOfValidityInvalid() {
		final String systemName = "system";
		final String address = "localhost";
		final int port = 1111;
		final String serviceDefinitionStr = "service_definition";
		
		final SystemRequestDTO sysDto = new SystemRequestDTO(); 
		sysDto.setSystemName(systemName);
		sysDto.setAddress(address);
		sysDto.setPort(port);
		
		final ServiceRegistryRequestDTO dto = new ServiceRegistryRequestDTO();
		dto.setServiceDefinition(serviceDefinitionStr);
		dto.setProviderSystem(sysDto);
		dto.setEndOfValidity("not a ZoneDateTime");
		
		when(serviceDefinitionRepository.findByServiceDefinition(any())).thenReturn(Optional.of(new ServiceDefinition(serviceDefinitionStr)));
		when(systemRepository.findBySystemNameAndAddressAndPort(any(), any(), anyInt())).thenReturn(Optional.of(new System(systemName, address, port, null)));
		
		serviceRegistryDBService.registerServiceResponse(dto);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testCreateServiceRegistryServiceDefinitionNull() {
		serviceRegistryDBService.createServiceRegistry(null, null, null, null, null, null, 1, null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testCreateServiceRegistryProviderNull() {
		serviceRegistryDBService.createServiceRegistry(new ServiceDefinition(), null, null, null, null, null, 1, null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = InvalidParameterException.class)
	public void testCreateServiceRegistryUniqueConstraintViolation() {
		when(serviceRegistryRepository.findByServiceDefinitionAndSystem(any(), any())).thenReturn(Optional.of(new ServiceRegistry()));
		serviceRegistryDBService.createServiceRegistry(new ServiceDefinition(), new System(), null, null, null, null, 1, null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testCreateServiceRegistryNotSecuredButAuthenticationInfoSpecified() {
		when(serviceRegistryRepository.findByServiceDefinitionAndSystem(any(), any())).thenReturn(Optional.empty());
		final System provider = new System();
		provider.setAuthenticationInfo("1234");
		serviceRegistryDBService.createServiceRegistry(new ServiceDefinition(), provider, null, null, ServiceSecurityType.NOT_SECURE, null, 1, null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testCreateServiceRegistrySecuredButAuthenticationInfoNotSpecified() {
		when(serviceRegistryRepository.findByServiceDefinitionAndSystem(any(), any())).thenReturn(Optional.empty());
		serviceRegistryDBService.createServiceRegistry(new ServiceDefinition(), new System(), null, null, ServiceSecurityType.CERTIFICATE, null, 1, null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testCreateServiceRegistryInterfacesListNull() {
		when(serviceRegistryRepository.findByServiceDefinitionAndSystem(any(), any())).thenReturn(Optional.empty());
		serviceRegistryDBService.createServiceRegistry(new ServiceDefinition(), new System(), null, null, ServiceSecurityType.NOT_SECURE, null, 1, null);
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testCreateServiceRegistryInterfacesListEmpty() {
		when(serviceRegistryRepository.findByServiceDefinitionAndSystem(any(), any())).thenReturn(Optional.empty());
		serviceRegistryDBService.createServiceRegistry(new ServiceDefinition(), new System(), null, null, ServiceSecurityType.NOT_SECURE, null, 1, Collections.<String>emptyList());
	}
	
	//-------------------------------------------------------------------------------------------------
	@Test(expected = IllegalArgumentException.class)
	public void testCreateServiceRegistryInvalidInterface() {
		when(serviceRegistryRepository.findByServiceDefinitionAndSystem(any(), any())).thenReturn(Optional.empty());
		serviceRegistryDBService.createServiceRegistry(new ServiceDefinition(), new System(), null, null, ServiceSecurityType.NOT_SECURE, null, 1, List.of("xml"));
	}
}