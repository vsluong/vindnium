package com.brianstempin.vindiniumclient.bot.advanced.murderbot;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.AdvancedGameState;
import com.brianstempin.vindiniumclient.bot.advanced.Mine;
import com.brianstempin.vindiniumclient.bot.advanced.Vertex;
import com.brianstempin.vindiniumclient.dto.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Claims mines if it's the closest one
 */
public class GreedyAlgorithm implements Decision<AdvancedMurderBot.GameContext, BotMove> {

    private final static Logger logger = LogManager.getLogger(EnRouteLootingDecisioner.class);

    private final Decision<AdvancedMurderBot.GameContext, BotMove> noGoodMineDecisioner;

    public GreedyAlgorithm(Decision<AdvancedMurderBot.GameContext, BotMove> noGoodMineDecisioner) {
        this.noGoodMineDecisioner = noGoodMineDecisioner;
    }

    @Override
    public BotMove makeDecision(AdvancedMurderBot.GameContext context) {
        GameState.Position myPosition = context.getGameState().getMe().getPos();
        Map<GameState.Position, Vertex> boardGraph = context.getGameState().getBoardGraph();


    	//maximum distance to snag the low-hanging fruit
    	int max = 20;
    	//my id
    	int me = context.getGameState().getMe().getId();
    	Mine bestMine = null;
    	int bestDistance = Integer.MAX_VALUE;
    	//look through all mines
    	for(Mine m : context.getGameState().getMines().values())
    	{
    		//don't consider mines that are mine
    		if( m.getOwner() != null && m.getOwner().getId() == me )
    			continue;
    		//get distance to mine
    		AdvancedMurderBot.DijkstraResult currentDijkstraResult =
    				context.getDijkstraResultMap().get(m.getPosition());
    		if(currentDijkstraResult == null)
    			continue;
    		int distance = currentDijkstraResult.getDistance();
    		//ignore if mine is too far
    		if(distance > max)
    			continue;
    		
    		//see if anyone else is too close
    		if(BotUtils.getHeroesAround(context.getGameState(), context.getDijkstraResultMap(), distance).size() > 0) {
                logger.info("Mine found, but another hero is too close.");
                continue;
            }
    		
    		if(distance <= bestDistance)
    		{
    			bestDistance = distance;
    			bestMine = m;
    		}
    	}
    	
    	if(bestMine != null)
    	{
    		AdvancedMurderBot.DijkstraResult currentDijkstraResult =
    				context.getDijkstraResultMap().get(bestMine.getPosition());
    		//if found a nice mine
    		GameState.Position nextPosition = bestMine.getPosition();
    		while(null != currentDijkstraResult && currentDijkstraResult.getDistance() > 1) {
                nextPosition = currentDijkstraResult.getPrevious();
                currentDijkstraResult = context.getDijkstraResultMap().get(nextPosition);
            }
    		logger.info("Getting a free Mine.");
    		return BotUtils.directionTowards(currentDijkstraResult.getPrevious(), nextPosition);
    	}
    	
        // Nope.
        logger.info("No easy mines.");
        return noGoodMineDecisioner.makeDecision(context);
    }
}
