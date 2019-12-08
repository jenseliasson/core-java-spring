package eu.arrowhead.common.database.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.database.entity.ChoreographerAction;

@Repository
public interface ChoreographerActionRepository extends RefreshableRepository<ChoreographerAction,Long> {

    //=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	public Optional<ChoreographerAction> findByActionName(final String actionName);
    public Optional<ChoreographerAction> findByActionNameAndNextAction(final String actionName, final ChoreographerAction nextAction);
}