package eu.arrowhead.core.choreographer.database.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.database.entity.ChoreographerAction;
import eu.arrowhead.common.database.entity.ChoreographerActionActionStepConnection;
import eu.arrowhead.common.database.entity.ChoreographerActionPlan;
import eu.arrowhead.common.database.entity.ChoreographerActionPlanActionConnection;
import eu.arrowhead.common.database.entity.ChoreographerActionStep;
import eu.arrowhead.common.database.entity.ChoreographerActionStepServiceDefinitionConnection;
import eu.arrowhead.common.database.entity.ChoreographerNextActionStep;
import eu.arrowhead.common.database.entity.ServiceDefinition;
import eu.arrowhead.common.database.repository.ChoreographerActionActionStepConnectionRepository;
import eu.arrowhead.common.database.repository.ChoreographerActionPlanActionConnectionRepository;
import eu.arrowhead.common.database.repository.ChoreographerActionPlanRepository;
import eu.arrowhead.common.database.repository.ChoreographerActionRepository;
import eu.arrowhead.common.database.repository.ChoreographerActionStepRepository;
import eu.arrowhead.common.database.repository.ChoreographerActionStepServiceDefinitionConnectionRepository;
import eu.arrowhead.common.database.repository.ChoreographerNextActionStepRepository;
import eu.arrowhead.common.database.repository.ServiceDefinitionRepository;
import eu.arrowhead.common.dto.internal.ChoreographerActionRequestDTO;
import eu.arrowhead.common.dto.internal.ChoreographerActionStepRequestDTO;
import eu.arrowhead.common.dto.internal.DTOConverter;
import eu.arrowhead.common.dto.shared.ChoreographerActionPlanResponseDTO;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class ChoreographerDBService {
	
	//=================================================================================================
	// members

    @Autowired
    private ServiceDefinitionRepository serviceDefinitionRepository;

    @Autowired
    private ChoreographerActionPlanRepository choreographerActionPlanRepository;

    @Autowired
    private ChoreographerActionRepository choreographerActionRepository;

    @Autowired
    private ChoreographerActionStepRepository choreographerActionStepRepository;

    @Autowired
    private ChoreographerActionStepServiceDefinitionConnectionRepository choreographerActionStepServiceDefinitionConnectionRepository;

    @Autowired
    private ChoreographerNextActionStepRepository choreographerNextActionStepRepository;

    @Autowired
    private ChoreographerActionActionStepConnectionRepository choreographerActionActionStepConnectionRepository;

    @Autowired
    private ChoreographerActionPlanActionConnectionRepository choreographerActionPlanActionConnectionRepository;

    private final Logger logger = LogManager.getLogger(ChoreographerDBService.class);
    
    //=================================================================================================
	// methods

    //-------------------------------------------------------------------------------------------------
    @Transactional(rollbackFor = ArrowheadException.class)
    public ChoreographerActionStep createChoreographerActionStepWithUsedService(final String stepName, final Set<String> usedServiceNames) {
        logger.debug("createChoreographerActionStep started...");

        try {
            if (Utilities.isEmpty(stepName)) {
                throw new InvalidParameterException("ActionStep name is null or blank.");
            }

            if (usedServiceNames == null || usedServiceNames.isEmpty()) {
                throw new InvalidParameterException("UsedService name is null or blank.");
            }

            final Optional<ChoreographerActionStep> choreographerActionStepOpt = choreographerActionStepRepository.findByName(stepName);
            choreographerActionStepOpt.ifPresent(choreographerActionStep -> {
                throw new InvalidParameterException("One or more ActionSteps with the given names already exist! ActionStep NAMES must be UNIQUE!");
            });
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }

        final List<ServiceDefinition> usedServices = new ArrayList<>(usedServiceNames.size());
        try {
            for (final String name : usedServiceNames) {
                final Optional<ServiceDefinition> serviceOpt = serviceDefinitionRepository.findByServiceDefinition(name);
                if (serviceOpt.isPresent()) {
                    usedServices.add(serviceOpt.get());
                } else {
                    logger.debug("Service Definition with name of " + name + " doesn't exist!");
                }
            }

            if (usedServices.size() != usedServiceNames.size()) {
                throw new InvalidParameterException("One or more of the Services given doesn't exist! Create ALL Services before usage.");
            }
            
            final ChoreographerActionStep stepEntry = choreographerActionStepRepository.save(new ChoreographerActionStep(stepName));
            for (final ServiceDefinition serviceDefinition : usedServices) {
            	final ChoreographerActionStepServiceDefinitionConnection connection =
            			choreographerActionStepServiceDefinitionConnectionRepository.save(new ChoreographerActionStepServiceDefinitionConnection(stepEntry, serviceDefinition));
            	stepEntry.getActionStepServiceDefinitionConnections().add(connection);
            }
            choreographerActionStepServiceDefinitionConnectionRepository.flush();
            
            return choreographerActionStepRepository.saveAndFlush(stepEntry);
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }

    //-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
    public ChoreographerActionStep addNextStepToChoreographerActionStep(final String stepName, final Set<String> nextActionStepNames) {
        logger.debug("addNextStepToChoreographerActionStep started...");

        if (Utilities.isEmpty(stepName)) {
        	throw new InvalidParameterException("Step name is empty or null.");
        }

        ChoreographerActionStep stepEntry;
        try {
        	final Optional<ChoreographerActionStep> choreographerActionStepOpt = choreographerActionStepRepository.findByName(stepName);
            if (choreographerActionStepOpt.isPresent()) {
                stepEntry = choreographerActionStepOpt.get();
            } else {
                throw new InvalidParameterException("The Choreographer Action Step doesn't exist!");
            }
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }

        final List<ChoreographerActionStep> nextActionSteps = new ArrayList<>(nextActionStepNames.size());
        try {
            for(final String nextActionStepName : nextActionStepNames) {
                final Optional<ChoreographerActionStep> actionStepOpt = choreographerActionStepRepository.findByName(nextActionStepName);
                if (actionStepOpt.isPresent()) {
                    nextActionSteps.add(actionStepOpt.get());
                } else {
                    throw new InvalidParameterException("Action Step with name of " + nextActionStepName + " doesn't exist!");
                }
            }
            
            for(final ChoreographerActionStep actionStep : nextActionSteps) {
            	final ChoreographerNextActionStep nextActionStep = choreographerNextActionStepRepository.save(new ChoreographerNextActionStep(stepEntry, actionStep));
            	stepEntry.getActionSteps().add(nextActionStep);
            }
            choreographerNextActionStepRepository.flush();
            
            return choreographerActionStepRepository.saveAndFlush(stepEntry);
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }

    //-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
    public ChoreographerAction createChoreographerAction(final String actionName, final List<ChoreographerActionStepRequestDTO> actionSteps) {
        logger.debug("createChoreographerAction started...");

        try {
            if (Utilities.isEmpty(actionName)) {
                throw new InvalidParameterException("Action name is null or blank.");
            }

            final Optional<ChoreographerAction> choreographerActionOpt = choreographerActionRepository.findByActionName(actionName);
            choreographerActionOpt.ifPresent(choreographerAction -> {
                throw new InvalidParameterException("One or more Actions with the given names already exist! Action NAMES must be UNIQUE!");
            });
            
            final ChoreographerAction action = new ChoreographerAction();
            action.setActionName(actionName);
            
            final ChoreographerAction actionEntry = choreographerActionRepository.save(action);
            if (actionSteps != null & !actionSteps.isEmpty()) {
            	for (final ChoreographerActionStepRequestDTO actionStep : actionSteps) {
            		final ChoreographerActionActionStepConnection connection = choreographerActionActionStepConnectionRepository.save(
            				new ChoreographerActionActionStepConnection(createChoreographerActionStepWithUsedService(actionStep.getActionStepName(), new HashSet<>(actionStep.getUsedServiceNames())),
            																										 actionEntry));
            		actionEntry.getActionActionStepConnections().add(connection);
            	}
            	
            	for (final ChoreographerActionStepRequestDTO actionStep : actionSteps) {
            		final List<String> nextActionStepNames = actionStep.getNextActionStepNames();
            		if (nextActionStepNames != null && !nextActionStepNames.isEmpty()) {
            			addNextStepToChoreographerActionStep(actionStep.getActionStepName(), new HashSet<>(nextActionStepNames));
            		}
            	}
            }
            choreographerActionActionStepConnectionRepository.flush();
            
            return choreographerActionRepository.saveAndFlush(actionEntry);
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }

    //-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
    public ChoreographerAction addNextActionToChoreographerAction(final String actionName, final String nextActionName) {
        logger.debug("addNextActionToChoreographerAction started...");

        if (Utilities.isEmpty(actionName)) {
        	throw new InvalidParameterException("Action name or next Action name is null or blank.");
        }


        ChoreographerAction choreographerAction;
        try {
        	final Optional<ChoreographerAction> choreographerActionOpt = choreographerActionRepository.findByActionName(actionName);
        	
            if (choreographerActionOpt.isPresent()) {
                choreographerAction = choreographerActionOpt.get();
            } else {
                throw new InvalidParameterException("Action with given Action Name of " + actionName + "doesn't exist!");
            }
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }

        try {
        	final Optional<ChoreographerAction> nextActionOpt = choreographerActionRepository.findByActionName(nextActionName);
            if (nextActionOpt.isPresent()) {
                choreographerAction.setNextAction(nextActionOpt.get());
            } else if (nextActionName != null) {
                throw new InvalidParameterException("Action with given Action Name of " + nextActionName + " doesn't exist!");
            }
            
            return choreographerActionRepository.saveAndFlush(choreographerAction);
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
    public ChoreographerActionPlan createChoreographerActionPlan(final String actionPlanName, final List<ChoreographerActionRequestDTO> actions) {
        logger.debug("createChoreographerActionPlan started...");

        try {
            if (Utilities.isEmpty(actionPlanName)) {
                throw new InvalidParameterException("ActionPlan name is null or blank!");
            }

            final Optional<ChoreographerActionPlan> choreographerActionPlanOpt = choreographerActionPlanRepository.findByActionPlanName(actionPlanName);
            choreographerActionPlanOpt.ifPresent(choreographerActionPlan -> {
                throw new InvalidParameterException("ActionPlan with given name already exists! ActionPlan NAMES must be UNIQUE!");
            });
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }

        try {
        	final ChoreographerActionPlan actionPlan = new ChoreographerActionPlan(actionPlanName);
        	final ChoreographerActionPlan actionPlanEntry = choreographerActionPlanRepository.save(actionPlan);
        	
            if (actions != null && !actions.isEmpty()) {
                for (final ChoreographerActionRequestDTO action : actions) {
                    final ChoreographerActionPlanActionConnection connection = choreographerActionPlanActionConnectionRepository.save(
                    		new ChoreographerActionPlanActionConnection(actionPlanEntry, createChoreographerAction(action.getActionName(), action.getActionSteps())));
                    actionPlanEntry.getActionPlanActionConnections().add(connection);
                }

                for (final ChoreographerActionRequestDTO action : actions) {
                    addNextActionToChoreographerAction(action.getActionName(), action.getNextActionName());
                }
            } else {
                throw new InvalidParameterException("ActionPlan doesn't have any actions or the action field is blank.");
            }
            choreographerActionPlanActionConnectionRepository.flush();
            
            return choreographerActionPlanRepository.saveAndFlush(actionPlanEntry);
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
    public ChoreographerActionPlan createChoreographerActionPlanWithExistingActions(final String actionPlanName, final List<ChoreographerActionRequestDTO> actions) {
        logger.debug("createChoreographerActionPlanWithExistingActions started...");

        try {
        	final Optional<ChoreographerActionPlan> choreographerActionPlanOpt = choreographerActionPlanRepository.findByActionPlanName(actionPlanName);
            choreographerActionPlanOpt.ifPresent(choreographerActionPlan -> {
                throw new InvalidParameterException("ActionPlan with given name already exists! ActionPlan NAMES must be UNIQUE!");
            });
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }

        try {
        	final ChoreographerActionPlan actionPlan = new ChoreographerActionPlan(actionPlanName);
        	final ChoreographerActionPlan actionPlanEntry = choreographerActionPlanRepository.save(actionPlan);
        	
        	final List<ChoreographerAction> choreographerActions = new ArrayList<>(actions.size());
            for (final ChoreographerActionRequestDTO action : actions) {
                final String nextActionName = action.getNextActionName();
                if (nextActionName != null) {
                    final Optional<ChoreographerAction> nextActionOptional = choreographerActionRepository.findByActionName(nextActionName);
                    if (nextActionOptional.isPresent()) {
                        final Optional<ChoreographerAction> actionOptional = choreographerActionRepository.findByActionNameAndNextAction(action.getActionName(), nextActionOptional.get());
                        if (actionOptional.isPresent()) {
                            choreographerActions.add(actionOptional.get());
                        } else {
                            throw new InvalidParameterException("One or more given Actions are not present in the database! Please create them first!");
                        }
                    } else {
                        throw new InvalidParameterException("The NextAction you defined for an Action doesn't match with the Action's initial NextAction!");
                    }
                } else {
                    final Optional<ChoreographerAction> actionOptional = choreographerActionRepository.findByActionName(action.getActionName());
                    actionOptional.ifPresent(choreographerActions::add);
                }
            }
            
            for (final ChoreographerAction action : choreographerActions) {
            	final ChoreographerActionPlanActionConnection connection = choreographerActionPlanActionConnectionRepository.save(new ChoreographerActionPlanActionConnection(actionPlanEntry, action));
            	actionPlanEntry.getActionPlanActionConnections().add(connection);
            }
            
            return choreographerActionPlanRepository.saveAndFlush(actionPlanEntry);
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }

    //-------------------------------------------------------------------------------------------------
	public Page<ChoreographerActionPlan> getChoreographerActionPlanEntries(final int page, final int size, final Direction direction, final String sortField) {
        logger.debug("getChoreographerActionPlanEntries started... ");

        final int validatedPage = page < 0 ? 0 : page;
        final int validatedSize = size <= 0 ? Integer.MAX_VALUE : size;
        final Direction validatedDirection = direction == null ? Direction.ASC : direction;
        final String validatedSortField = Utilities.isEmpty(sortField) ? CoreCommonConstants.COMMON_FIELD_NAME_ID : sortField.trim();

        if (!ChoreographerActionPlan.SORTABLE_FIELDS_BY.contains(validatedSortField)) {
            throw new InvalidParameterException("Sortable field with reference '" + validatedSortField + "' is not available");
        }

        try {
            return choreographerActionPlanRepository.findAll(PageRequest.of(validatedPage, validatedSize, validatedDirection, validatedSortField));
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }

    //-------------------------------------------------------------------------------------------------
	public List<ChoreographerActionPlanResponseDTO> getChoreographerActionPlanEntriesResponse(final int page, final int size, final Direction direction, final String sortField) {
        logger.debug("getChoreographerActionPlanEntriesResponse started...");

        final Page<ChoreographerActionPlan> choreographerActionPlanEntries = getChoreographerActionPlanEntries(page, size, direction, sortField);

        final List<ChoreographerActionPlanResponseDTO> actionPlanResponseDTOS = new ArrayList<>();
        for (final ChoreographerActionPlan actionPlan : choreographerActionPlanEntries) {
            actionPlanResponseDTOS.add(DTOConverter.convertChoreographerActionPlanToChoreographerActionPlanResponseDTO(actionPlan));
        }

        return actionPlanResponseDTOS;
    }

    //-------------------------------------------------------------------------------------------------
	public ChoreographerActionPlan getChoreographerActionPlanById(final long id) {
        logger.debug("getChoreographerActionPlanById started...");

        try {
            final Optional<ChoreographerActionPlan> actionPlanOpt = choreographerActionPlanRepository.findById(id);
            if (actionPlanOpt.isPresent()) {
                return actionPlanOpt.get();
            } else {
                throw new InvalidParameterException("Choreographer Action Plan with id of '" + id + "' doesn't exist!");
            }
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }

    //-------------------------------------------------------------------------------------------------
	public ChoreographerActionPlanResponseDTO getChoreographerActionPlanByIdResponse(final long id) {
        logger.debug("getChoreographerActionPlanByIdResponse started...");

        return DTOConverter.convertChoreographerActionPlanToChoreographerActionPlanResponseDTO(getChoreographerActionPlanById(id));
    }

    //-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
    public void removeActionPlanEntryById(final long id) {
        logger.debug("removeActionPlanEntryById started...");

        try {
            if (!choreographerActionPlanRepository.existsById(id)) {
                throw new InvalidParameterException("ActionPlan with id of '" + id + "' doesn't exist!");
            }
            choreographerActionPlanRepository.deleteById(id);
            choreographerActionPlanRepository.flush();
        } catch (final InvalidParameterException ex) {
            throw ex;
        } catch (final Exception ex) {
            logger.debug(ex.getMessage(), ex);
            throw new ArrowheadException(CoreCommonConstants.DATABASE_OPERATION_EXCEPTION_MSG);
        }
    }
}