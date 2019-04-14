package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.DOUBLE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.SECRET;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableListIterator;

import java.util.Arrays;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {

	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<PlayerConfiguration> players;
	private Colour currentPlayer;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {
		
		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);

		if(this.rounds.isEmpty()) {
			throw new IllegalArgumentException("Rounds is empty.");
		}

		if(this.graph.isEmpty()) {
			throw new IllegalArgumentException("Graph is empty.");
		}

		if(mrX == null) {
			throw new NullPointerException("Mrx is empty");
		}

		if(!mrX.colour.isMrX()) {
			throw new InvalidParameterException("MrX not black.");
		}
		
		List<PlayerConfiguration> configurations = new ArrayList<PlayerConfiguration>();
		configurations.add(mrX);
		configurations.add(firstDetective);

		//Make sure colours and locations are not repeated.
		for(PlayerConfiguration detective : restOfTheDetectives) {
			configurations.add(detective);
		}
		
		Set<Colour> colours = new HashSet<Colour>();
		Set<Integer> locations = new HashSet<Integer>();


		for(PlayerConfiguration player : configurations) {
			//All player validation
			if(colours.contains(player.colour)) {
				throw new InvalidParameterException("Player of this colour already exists.");
			}
			colours.add(player.colour);

			if(locations.contains(player.location)) {
				throw new InvalidParameterException("Player is already at this location.");
			}
			locations.add(player.location);
			
			for(Ticket t : Ticket.values())  {
				if(!player.tickets.containsKey(t)) {
					throw new InvalidParameterException("Player is missing ticket.");
				}
			}

			//Detective only validation
			if(player.colour != BLACK) {
				if(player.tickets.getOrDefault(DOUBLE, 0) > 0) {
					throw new InvalidParameterException("Detective contains double ticket.");
				}
				if(player.tickets.getOrDefault(SECRET, 0) > 0) {
					throw new InvalidParameterException("Detective contains secret ticket.");
				}
			}
		}
		this.players = configurations;
		this.currentPlayer = BLACK;
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
		
	}

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> playerColours = new ArrayList<Colour>();
		for(PlayerConfiguration player : players) {
			playerColours.add(player.colour);
		}

		return Collections.unmodifiableList(playerColours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		return Collections.unmodifiableSet(new HashSet<Colour>());
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) 
	{
		if(colour == BLACK) return Optional.of((Integer)0);
		for(PlayerConfiguration player : players) {
			if(colour == player.colour) {
				return Optional.of((Integer)player.location);
			}
		}
		return Optional.empty();
	}	

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) 
	{	
		for(PlayerConfiguration player : players)
			if(player.colour==colour)
				return Optional.of(player.tickets.getOrDefault(ticket, 0));
		return Optional.empty();
	}

	@Override
	
	public boolean isGameOver() 
	{
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		return currentPlayer;
	}

	@Override
	public int getCurrentRound() 
	{
		return NOT_STARTED;
	
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(this.rounds);
	}

	@Override
	public ImmutableGraph<Integer, Transport> getGraph() {
		return new ImmutableGraph<Integer, Transport>(this.graph);
	}
}
