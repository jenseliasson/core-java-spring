package eu.arrowhead.core.datamanager;

import java.util.*; 
import java.util.Vector;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
//import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.CoreDefaults;
import eu.arrowhead.common.CoreUtilities;
import eu.arrowhead.common.CoreUtilities.ValidatedPageParams;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.SenML;
import eu.arrowhead.common.dto.shared.EventPublishRequestDTO;
import eu.arrowhead.common.dto.shared.SubscriptionListResponseDTO;
import eu.arrowhead.common.dto.shared.SubscriptionRequestDTO;
import eu.arrowhead.common.dto.shared.SubscriptionResponseDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.common.exception.BadPayloadException;
import eu.arrowhead.core.datamanager.database.service.DataManagerDBService;
import eu.arrowhead.core.datamanager.service.DataManagerService;
import eu.arrowhead.core.datamanager.service.ProxyService;
import eu.arrowhead.core.datamanager.service.ProxyElement;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.Gson;

@Api(tags = { CoreCommonConstants.SWAGGER_TAG_ALL })
@CrossOrigin(maxAge = Defaults.CORS_MAX_AGE, allowCredentials = Defaults.CORS_ALLOW_CREDENTIALS, 
allowedHeaders = { HttpHeaders.ORIGIN, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION }
)
@RestController
@RequestMapping(CommonConstants.DATAMANAGER_URI)
public class DataManagerController {
	
	//=================================================================================================
	// members
	
	private static final String PATH_VARIABLE_ID = "id";

	private static final String EVENT_HANDLER_MGMT_URI =  CoreCommonConstants.MGMT_URI + "/subscriptions";
	private static final String EVENTHANLER_BY_ID_MGMT_URI = EVENT_HANDLER_MGMT_URI + "/{" + PATH_VARIABLE_ID + "}";
	
	private static final String GET_EVENT_HANDLER_MGMT_DESCRIPTION = "Return requested Subscription entries by the given parameters";
	private static final String GET_EVENT_HANDLER_MGMT_HTTP_200_MESSAGE = "Subscription entries returned";
	private static final String GET_EVENT_HANDLER_MGMT_HTTP_400_MESSAGE = "Could not retrieve Subscription entries";
	
	private static final String GET_EVENT_HANDLER_BY_ID_MGMT_DESCRIPTION = "Return requested Subscription entry by the given id";
	private static final String GET_EVENT_HANDLER_BY_ID_MGMT_HTTP_200_MESSAGE = "Subscription entriy returned";
	private static final String GET_EVENT_HANDLER_BY_ID_MGMT_HTTP_400_MESSAGE = "Could not retrieve Subscription entry";
	
	private static final String DELETE_EVENT_HANDLER_MGMT_DESCRIPTION = "Delete requested Subscription entry by the given id";
	private static final String DELETE_EVENT_HANDLER_MGMT_HTTP_200_MESSAGE = "Subscription entriy deleted";
	private static final String DELETE_EVENT_HANDLER_MGMT_HTTP_400_MESSAGE = "Could not delete Subscription entry";
	
	private static final String PUT_EVENT_HANDLER_MGMT_DESCRIPTION = "Update requested Subscription entry by the given id and parameters";
	private static final String PUT_EVENT_HANDLER_MGMT_HTTP_200_MESSAGE = "Updated Subscription entry returned";
	private static final String PUT_EVENT_HANDLER_MGMT_HTTP_400_MESSAGE = "Could not update Subscription entry";	
	
	private static final String POST_EVENT_HANDLER_SUBSCRIPTION_DESCRIPTION = "Subcribtion to the events specified in requested Subscription ";
	private static final String POST_EVENT_HANDLER_SUBSCRIPTION_HTTP_200_MESSAGE = "Successful subscription.";
	private static final String POST_EVENT_HANDLER_SUBSCRIPTION_HTTP_400_MESSAGE = "Unsuccessful subscription.";
	
	private static final String DELETE_EVENT_HANDLER_SUBSCRIPTION_DESCRIPTION = "Unsubcribtion from the events specified in requested Subscription ";
	private static final String DELETE_EVENT_HANDLER_SUBSCRIPTION_HTTP_200_MESSAGE = "Successful unsubscription.";
	private static final String DELETE_EVENT_HANDLER_SUBSCRIPTION_HTTP_400_MESSAGE = "Unsuccessful unsubscription.";
	
	private static final String POST_EVENT_HANDLER_PUBLISH_DESCRIPTION = "Publish event"; 
	private static final String POST_EVENT_HANDLER_PUBLISH_HTTP_200_MESSAGE = "Publish event success"; 
	private static final String POST_EVENT_HANDLER_PUBLISH_HTTP_400_MESSAGE = "Publish event not success"; 

	private static final String POST_EVENT_HANDLER_PUBLISH_AUTH_UPDATE_DESCRIPTION = "Publish authorization change event "; 
	private static final String POST_EVENT_HANDLER_PUBLISH_AUTH_UPDATE_HTTP_200_MESSAGE = "Publish authorization change event success"; 
	private static final String POST_EVENT_HANDLER_PUBLISH_AUTH_UPDATE_HTTP_400_MESSAGE = "Publish authorization change event not success"; 
	
	private static final String NULL_PARAMETER_ERROR_MESSAGE = " is null.";
	private static final String NULL_OR_BLANK_PARAMETER_ERROR_MESSAGE = " is null or blank.";
	private static final String ID_NOT_VALID_ERROR_MESSAGE = " Id must be greater than 0. ";
	private static final String WRONG_FORMAT_ERROR_MESSAGE = " is in wrong format. ";

	@Value( CoreCommonConstants.$TIME_STAMP_TOLERANCE_SECONDS_WD )
	private long timeStampTolerance;
	
	private final Logger logger = LogManager.getLogger(DataManagerController.class);
	
	@Autowired
	DataManagerService dataManagerService;
	
	@Autowired
	ProxyService proxyService;

	@Autowired
	DataManagerDBService dataManagerDBService;
	
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = "Return an echo message with the purpose of testing the core service availability", response = String.class, tags = { CoreCommonConstants.SWAGGER_TAG_CLIENT })
	@ApiResponses (value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@GetMapping(path = CommonConstants.ECHO_URI)
	public String echoService() {
		return "Got it!";
	}
	
	//-------------------------------------------------------------------------------------------------
	@GetMapping(value= "/historian")
	public String historianS(
			) {
		System.out.println("DataManager::Historian/");
		return "DataManager::Historian";
	}

	@GetMapping(value= "/historian/{system}")
	public String historianSystemGet(
		@PathVariable(value="system", required=true) String systemName
		) {
		System.out.println("DataManager:GET:Historian/"+systemName);
		return "DataManager::Historian/" + systemName;
	}

	@PutMapping(value= "/historian/{system}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String historianSystemPut(
		@PathVariable(value="system", required=true) String systemName
		) {
		System.out.println("DataManager:PUT:Historian/"+systemName);
		return "DataManager::Historian/" + systemName;
	}

	@GetMapping(value= "/historian/{system}/{service}")//CommonConstants.DM_HISTORIAN_URI)
	public String historianServiceGet(//RequestEntity<String> request /*
			//@RequestHeader("Content-Type", required=true) String contentType,
		@PathVariable(value="system", required=true) String systemName,
		@PathVariable(value="service", required=true) String serviceName
		) {
		System.out.println("DataManager:Get:Historian/"+systemName+"/"+serviceName);

		return "DataManager::Historian";
	}

	@PutMapping(value= "/historian/{system}/{service}", consumes = MediaType.APPLICATION_JSON_VALUE)//CommonConstants.DM_HISTORIAN_URI)
	public String historianServicePut(
		@PathVariable(value="system", required=true) String systemName,
		@PathVariable(value="service", required=true) String serviceName,
		@RequestBody Vector<SenML> sml
		) {
		System.out.println("DataManager:Put:Historian/"+systemName+"/"+serviceName);
		Iterator entry = sml.iterator();
		while (entry.hasNext()) { 
            	  System.out.println(entry.next().toString()); 
      		} 

		return "DataManager::Historian";
	}
	//-------------------------------------------------------------------------------------------------
	@ApiResponses (value = {
		@ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@GetMapping(value= "/proxy", produces=MediaType.APPLICATION_JSON_VALUE)
	public String proxyS() {
		System.out.println("DataManager::proxy/");
		Gson gson = new Gson();

		List<String> pes = ProxyService.getAllEndpoints();
		JsonObject answer = new JsonObject();
		JsonElement systemlist = gson.toJsonTree(pes);
		answer.add("systems", systemlist);

		String jsonStr = gson.toJson(answer);
		return jsonStr; //"DataManager::Proxy";
	}


	//-------------------------------------------------------------------------------------------------
	@ApiResponses (value = {
		@ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@GetMapping(value= "/proxy/{system}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String proxySystemGet(
			@PathVariable(value="system", required=true) String systemName
		) {
		System.out.println("DataManager:Get:proxy/"+systemName);

		List<ProxyElement> pes = ProxyService.getEndpoints(systemName);
		if (pes.size() == 0) {
			logger.debug("proxy GET to systemName: " + systemName + " not found");
			throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "System not found");
		}

		ArrayList<String> systems= new ArrayList<String>();
		for (ProxyElement pe: pes) {
			systems.add(pe.serviceName);
		}

		Gson gson = new Gson();
		JsonObject answer = new JsonObject();
		JsonElement servicelist = gson.toJsonTree(systems);
		answer.add("services", servicelist);
		String jsonStr = gson.toJson(answer);
		return jsonStr;
	}

	//-------------------------------------------------------------------------------------------------
	@ApiResponses (value = {
		@ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@PutMapping(value= "/proxy/{systemName}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String proxySystemPut(
			@PathVariable(value="systemName", required=true) String systemName,
			@RequestBody String requestBody
		) {
		System.out.println("DataManager:Put:proxy/"+systemName);
		JsonParser parser= new JsonParser();
		JsonObject obj = null;
		try {
			obj = parser.parse(requestBody).getAsJsonObject();
		} catch(Exception je){
			throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "malformed request");
		}

		String op = obj.get("op").getAsString();
		if(op.equals("list")){
			List<ProxyElement> pes = ProxyService.getEndpoints(systemName);
			if (pes.size() == 0) {
				logger.debug("proxy GET to systemName: " + systemName + " not found");
				throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "System not found");
			}

			ArrayList<String> systems= new ArrayList<String>();
			for (ProxyElement pe: pes) {
				systems.add(pe.serviceName);
			}

			Gson gson = new Gson();
			JsonObject answer = new JsonObject();
			JsonElement servicelist = gson.toJsonTree(systems);
			answer.add("services", servicelist);
			String jsonStr = gson.toJson(answer);
			return jsonStr;
		} else if(op.equals("create")){
			String srvName = obj.get("srvName").getAsString();
			String srvType = obj.get("srvType").getAsString();
			logger.info("Create Service: "+srvName+" of type: "+srvType+" for: " + systemName);

			/* check if service already exists */
			ArrayList<ProxyElement> services = ProxyService.getEndpoints(systemName);
			for (ProxyElement srv: services) {
				logger.info("PE: " + srv.serviceName);
				if(srv.serviceName.equals(srvName)){
					Gson gson = new Gson();
					JsonObject answer = new JsonObject();
					answer.addProperty("createResult", "Already exists");
					String jsonStr = gson.toJson(answer);
					//return Response.status(Status.CONFLICT).entity(jsonStr).type(MediaType.APPLICATION_JSON).build();
					throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "createResult: Already exists");
				}
			}
			logger.info("Not found, create it: " + srvName);

			/* create the service */
			boolean ret = ProxyService.addEndpoint(new ProxyElement(systemName, srvName));
			if (ret==true){
				throw new ResponseStatusException(org.springframework.http.HttpStatus.CREATED, "createResult: Created");
			} else { 
				throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "createResult: Already exists");
			}
		}

		return "";
		}

	//-------------------------------------------------------------------------------------------------
	@ApiResponses (value = {
		@ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@GetMapping(value= "/proxy/{system}/{service}")//CommonConstants.DM_PROXY_URI)
	public String proxyServiceGet(
			@PathVariable(value="system", required=true) String systemName,
			@PathVariable(value="service", required=true) String serviceName
			) {
			System.out.println("DataManager:Get:Proxy/"+systemName+"/"+serviceName);

			int statusCode = 0;
			ProxyElement pe = ProxyService.getEndpoint(serviceName);
			if (pe == null) {
				logger.info("proxy GET to serviceName: " + serviceName + " not found");
				//return Response.status(Status.NOT_FOUND).build();
				throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Service not found");
			}

			Iterator i = pe.msg.iterator();
			String senml = "";
			while (i.hasNext()) {
				senml += (i.next().toString())+"\n";
			}
			return senml;
			}

	//-------------------------------------------------------------------------------------------------
	@ApiResponses (value = {
		@ApiResponse(code = HttpStatus.SC_OK, message = CoreCommonConstants.SWAGGER_HTTP_200_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CoreCommonConstants.SWAGGER_HTTP_401_MESSAGE),
		@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CoreCommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@PutMapping(value= "/proxy/{system}/{service}")//CommonConstants.DM_PROXY_URI)
	public String proxyPut(
			@PathVariable(value="system", required=true) String systemName,
			@PathVariable(value="service", required=true) String serviceName,
			@RequestBody Vector<SenML> sml
			) {
		ProxyElement pe = ProxyService.getEndpoint(serviceName);
		if (pe == null) {
			throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Service not found");
		}

		//System.out.println("sml: "+ sml + "\t"+sml.toString());
		boolean statusCode = ProxyService.updateEndpoint(serviceName, sml);
		logger.info("putData/SenML returned with status code: " + statusCode + " from: " + sml.get(0).getBn() + " at: " + sml.get(0).getBt());

		String jsonret = "{\"rc\": 0}";
		return jsonret;
	}


	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = GET_EVENT_HANDLER_MGMT_DESCRIPTION, response = SubscriptionListResponseDTO.class, tags = { CoreCommonConstants.SWAGGER_TAG_MGMT })
	@ApiResponses(value = {
	@ApiResponse(code = HttpStatus.SC_OK, message = GET_EVENT_HANDLER_MGMT_HTTP_200_MESSAGE),
	@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = GET_EVENT_HANDLER_MGMT_HTTP_400_MESSAGE),
	@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
	@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@GetMapping(path = EVENT_HANDLER_MGMT_URI, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public SubscriptionListResponseDTO getSubscriptions(
	@RequestParam(name = CoreCommonConstants.REQUEST_PARAM_PAGE, required = false) final Integer page,
	@RequestParam(name = CoreCommonConstants.REQUEST_PARAM_ITEM_PER_PAGE, required = false) final Integer size,
	@RequestParam(name = CoreCommonConstants.REQUEST_PARAM_DIRECTION, defaultValue = CoreDefaults.DEFAULT_REQUEST_PARAM_DIRECTION_VALUE) final String direction,
	@RequestParam(name = CoreCommonConstants.REQUEST_PARAM_SORT_FIELD, defaultValue = CommonConstants.COMMON_FIELD_NAME_ID) final String sortField) {
	logger.debug("New getSubscriptions get request recieved with page: {} and item_per page: {}", page, size);

	//final ValidatedPageParams validParameters = CoreUtilities.validatePageParameters( page, size, direction, CommonConstants.EVENT_HANDLER_URI + EVENT_HANDLER_MGMT_URI );
	final SubscriptionListResponseDTO subscriptionsResponse = null; //dataManagerDBService.getSubscriptionsResponse( validParameters.getValidatedPage(), validParameters.getValidatedSize(), 

	logger.debug("Subscriptions  with page: {} and item_per page: {} retrieved successfully", page, size);
	return subscriptionsResponse;
	}

	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = GET_EVENT_HANDLER_BY_ID_MGMT_DESCRIPTION, response = SubscriptionResponseDTO.class, tags = { CoreCommonConstants.SWAGGER_TAG_MGMT })
	@ApiResponses(value = {
	@ApiResponse(code = HttpStatus.SC_OK, message = GET_EVENT_HANDLER_BY_ID_MGMT_HTTP_200_MESSAGE),
	@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = GET_EVENT_HANDLER_BY_ID_MGMT_HTTP_400_MESSAGE),
	@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
	@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@GetMapping(path = EVENTHANLER_BY_ID_MGMT_URI, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public SubscriptionResponseDTO getSubscriptionById(@PathVariable(value = PATH_VARIABLE_ID) final long id) {
	logger.debug("New getSubscriptionById get request recieved with id: {}", id);

	final String origin = CommonConstants.EVENT_HANDLER_URI + EVENTHANLER_BY_ID_MGMT_URI;
	if (id < 1) {
		throw new BadPayloadException(ID_NOT_VALID_ERROR_MESSAGE, HttpStatus.SC_BAD_REQUEST, origin);
		}
		
		final SubscriptionResponseDTO subscriptionResponse = dataManagerDBService.getSubscriptionByIdResponse( id );
		
		logger.debug("Subscription entry with id: {} successfully retrieved", id);
		
		return subscriptionResponse;
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = DELETE_EVENT_HANDLER_MGMT_DESCRIPTION, tags = { CoreCommonConstants.SWAGGER_TAG_MGMT })
	@ApiResponses(value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = DELETE_EVENT_HANDLER_MGMT_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = DELETE_EVENT_HANDLER_MGMT_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@DeleteMapping(path = EVENTHANLER_BY_ID_MGMT_URI, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public void deleteSubscription(@PathVariable(value = PATH_VARIABLE_ID) final long id) {
		logger.debug("New deleteSubscription delete request recieved with id: {}", id);
		
		final String origin = CommonConstants.EVENT_HANDLER_URI + EVENTHANLER_BY_ID_MGMT_URI;
		if (id < 1) {
			throw new BadPayloadException(ID_NOT_VALID_ERROR_MESSAGE, HttpStatus.SC_BAD_REQUEST, origin);
		}
		
		//dataManagerDBService.deleteSubscriptionResponse(id);
		
		logger.debug("Subscription entry with id: {} successfully deleted", id);
		
		return;
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = PUT_EVENT_HANDLER_MGMT_DESCRIPTION, response = SubscriptionResponseDTO.class, tags = { CoreCommonConstants.SWAGGER_TAG_MGMT })
	@ApiResponses(value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = PUT_EVENT_HANDLER_MGMT_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = PUT_EVENT_HANDLER_MGMT_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@PutMapping(path = EVENTHANLER_BY_ID_MGMT_URI, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public SubscriptionResponseDTO updateSubscription(
			@PathVariable(value = PATH_VARIABLE_ID) final long id,
			@RequestBody final SubscriptionRequestDTO request) {
		logger.debug("New updateSubscription put request recieved with id: {}", id);
		
		final String origin = CommonConstants.EVENT_HANDLER_URI + EVENTHANLER_BY_ID_MGMT_URI;
		if (id < 1) {
			throw new BadPayloadException(ID_NOT_VALID_ERROR_MESSAGE, HttpStatus.SC_BAD_REQUEST, origin);
		}
		
		//checkSubscriptionRequestDTO(request, origin);
		
		final SubscriptionResponseDTO response = dataManagerService.updateSubscriptionResponse(id, request);
		
		
		return response;
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = POST_EVENT_HANDLER_SUBSCRIPTION_DESCRIPTION, tags = { CoreCommonConstants.SWAGGER_TAG_CLIENT })
	@ApiResponses(value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = POST_EVENT_HANDLER_SUBSCRIPTION_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = POST_EVENT_HANDLER_SUBSCRIPTION_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@PostMapping(path = CommonConstants.OP_EVENT_HANDLER_SUBSCRIBE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public void subscribe(@RequestBody final SubscriptionRequestDTO request) {
		logger.debug("subscription started ...");
		
		final String origin = CommonConstants.EVENT_HANDLER_URI + CommonConstants.OP_EVENT_HANDLER_SUBSCRIBE;
		//checkSubscriptionRequestDTO( request, origin );
		
	    dataManagerService.subscribe( request );
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = DELETE_EVENT_HANDLER_SUBSCRIPTION_DESCRIPTION,  tags = { CoreCommonConstants.SWAGGER_TAG_CLIENT })
	@ApiResponses(value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = DELETE_EVENT_HANDLER_SUBSCRIPTION_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = DELETE_EVENT_HANDLER_SUBSCRIPTION_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@DeleteMapping(path = CommonConstants.OP_EVENT_HANDLER_UNSUBSCRIBE)
	@ResponseBody public void unsubscribe(
			@RequestParam(CommonConstants.OP_EVENT_HANDLER_UNSUBSCRIBE_REQUEST_PARAM_EVENT_TYPE) final String eventType,
			@RequestParam(CommonConstants.OP_EVENT_HANDLER_UNSUBSCRIBE_REQUEST_PARAM_SUBSCRIBER_SYSTEM_NAME) final String subscriberName,
			@RequestParam(CommonConstants.OP_EVENT_HANDLER_UNSUBSCRIBE_REQUEST_PARAM_SUBSCRIBER_ADDRESS) final String subscriberAddress,
			@RequestParam(CommonConstants.OP_EVENT_HANDLER_UNSUBSCRIBE_REQUEST_PARAM_SUBSCRIBER_PORT) final int subscriberPort) {
		logger.debug("unSubscription started ...");
		
		final String origin = CommonConstants.EVENT_HANDLER_URI + CommonConstants.OP_EVENT_HANDLER_SUBSCRIBE;
		//checkUnsubscribeParameters(eventType, subscriberName, subscriberAddress, subscriberPort, origin);
		
	    dataManagerService.unsubscribe( eventType, subscriberName, subscriberAddress, subscriberPort );
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = POST_EVENT_HANDLER_PUBLISH_DESCRIPTION, tags = { CoreCommonConstants.SWAGGER_TAG_CLIENT })
	@ApiResponses(value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = POST_EVENT_HANDLER_PUBLISH_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = POST_EVENT_HANDLER_PUBLISH_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@PostMapping(path = CommonConstants.OP_EVENT_HANDLER_PUBLISH, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public void publish(@RequestBody final EventPublishRequestDTO request) {
		logger.debug("publish started ...");
		
		final String origin = CommonConstants.EVENT_HANDLER_URI + CommonConstants.OP_EVENT_HANDLER_PUBLISH;
	//	checkEventPublishRequestDTO(request, origin);
		
		
	    dataManagerService.publishResponse(request);
	}
	
	//-------------------------------------------------------------------------------------------------
	@ApiOperation(value = POST_EVENT_HANDLER_PUBLISH_AUTH_UPDATE_DESCRIPTION, tags = { CoreCommonConstants.SWAGGER_TAG_PRIVATE })
	@ApiResponses(value = {
			@ApiResponse(code = HttpStatus.SC_OK, message = POST_EVENT_HANDLER_PUBLISH_AUTH_UPDATE_HTTP_200_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_BAD_REQUEST, message = POST_EVENT_HANDLER_PUBLISH_AUTH_UPDATE_HTTP_400_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_UNAUTHORIZED, message = CommonConstants.SWAGGER_HTTP_401_MESSAGE),
			@ApiResponse(code = HttpStatus.SC_INTERNAL_SERVER_ERROR, message = CommonConstants.SWAGGER_HTTP_500_MESSAGE)
	})
	@PostMapping(path = CommonConstants.OP_EVENT_HANDLER_PUBLISH_AUTH_UPDATE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public void publishSubscriberAuthorizationUpdate(@RequestBody final EventPublishRequestDTO request) {
		logger.debug("publishSubscriberAuthorizationUpdate started ...");
		
		final String origin = CommonConstants.EVENT_HANDLER_URI + CommonConstants.OP_EVENT_HANDLER_PUBLISH_AUTH_UPDATE;

		
	    dataManagerService.publishSubscriberAuthorizationUpdateResponse(request);
	}
	
	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------	
	private void checkSubscriptionRequestDTO( final SubscriptionRequestDTO request, final String origin) {
		logger.debug("checkSubscriptionRequestDTO started ...");
		
	}
	
	//-------------------------------------------------------------------------------------------------
	private void checkSystemRequestDTO(final SystemRequestDTO system, final String origin) {
		logger.debug("checkSystemRequestDTO started...");
	}
	
	//-------------------------------------------------------------------------------------------------
	private void checkEventPublishRequestDTO(final EventPublishRequestDTO request, final String origin) {
		logger.debug("checkEventPublishRequestDTO started ...");
		
		
	}
	
	//-------------------------------------------------------------------------------------------------
	// This method may CHANGE the content of EventPublishRequestDTO
	private void validateTimeStamp(final EventPublishRequestDTO request, final String origin) {
		logger.debug("validateTimeStamp started ...");
		
	}
	
	//-------------------------------------------------------------------------------------------------
	private void checkUnsubscribeParameters( final String eventType, final String subscriberName, final String subscriberAddress, final int subscriberPort, final String origin) {
		logger.debug("checkUnsubscribeParameters started...");
		
	}

}
