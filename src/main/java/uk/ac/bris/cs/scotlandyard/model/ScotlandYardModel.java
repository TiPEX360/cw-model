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
import java.util.Map;
import java.util.HashMap;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableListIterator;

import java.util.Arrays;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	final private List<Boolean> rounds;
	final private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players = new ArrayList<ScotlandYardPlayer>();
	private Integer currentPlayerIndex = 0;
	private Set<Move> validMoves;

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

		for(PlayerConfiguration config : configurations) {
			//For all players
			if(colours.contains(config.colour)) {
				throw new InvalidParameterException("Player of this colour already exists.");
			}
			colours.add(config.colour);

			if(locations.contains(config.location)) {
				throw new InvalidParameterException("Player is already at this location.");
			}
			locations.add(config.location);
			
			for(Ticket t : Ticket.values())  {
				if(!config.tickets.containsKey(t)) {
					throw new InvalidParameterException("Player is missing ticket.");
				}
			}

			//For detectives only
			if(config.colour != BLACK) {
				if(config.tickets.getOrDefault(DOUBLE, 0) > 0) {
					throw new InvalidParameterException("Detective contains double ticket.");
				}
				if(config.tickets.getOrDefault(SECRET, 0) > 0) {
					throw new InvalidParameterException("Detective contains secret ticket.");
				}
			}
			this.players.add(new ScotlandYardPlayer(config.player, config.colour, config.location, config.tickets));
		}
		validMoves = new HashSet<Move>();
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

	public void accept(Move m) {
		if(m == null) {
			throw new NullPointerException("Move is null.");
		}
		if(!validMoves.contains(m)) {
			throw new IllegalArgumentException("Invalid move.");
		}

		//If TicketMove
		//If PassMove
		//If double move
	}

	private Set<TicketMove> genMovesFromNode(Node<Integer> n, ScotlandYardPlayer player) {
		Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(n);
		Set<TicketMove> moves = new HashSet<TicketMove>();
		for(Edge<Integer, Transport> edge : edges) {
			if(player.hasTickets(Ticket.fromTransport(edge.data()))) {
				moves.add(new TicketMove(player.colour(), Ticket.fromTransport(edge.data()), edge.destination().value()));
			}
			else if(player.hasTickets(SECRET)) {
				moves.add(new TicketMove(BLACK, SECRET, edge.destination().value()));
			}
		}

		return moves;
	}

	private Set<Move> genValidMoves() {
		ScotlandYardPlayer current = players.get(currentPlayerIndex);
		Node<Integer> node = graph.getNode(current.location());
		Set<TicketMove> singleMoves = new HashSet<TicketMove>();
		Set<DoubleMove> doubleMoves = new HashSet<DoubleMove>();

		singleMoves = genMovesFromNode(node, current);
		for (TicketMove firstMove : singleMoves) {
			Set<TicketMove> secondMoves = new HashSet<TicketMove>();
			current.removeTicket(firstMove.ticket());
			secondMoves = genMovesFromNode(node, current);
			current.addTicket(firstMove.ticket());
			
			for(TicketMove secondMove : secondMoves) {
				doubleMoves.add(new DoubleMove(current.colour(), firstMove, secondMove));
			}
		}

		return Sets.union(singleMoves, doubleMoves);
	}

	@Override
	public void startRotate() {
		ScotlandYardPlayer current = players.get(currentPlayerIndex);
		validMoves = genValidMoves();
		System.out.println(validMoves.toString());
		current.player().makeMove(this, current.location(), validMoves, this);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		List<Colour> playerColours = new ArrayList<Colour>();
		for(ScotlandYardPlayer player : players) {
			playerColours.add(player.colour());
		}
		return Collections.unmodifiableList(playerColours);
	}
	
	public Set<Colour> getWinningPlayers() {
		return Collections.unmodifiableSet(new HashSet<Colour>());
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) 
	{
		if(colour == BLACK) return Optional.of((Integer)0);
		for(ScotlandYardPlayer player : players) {
			if(colour == player.colour()) {
				return Optional.of((Integer)player.location());
			}
		}
		return Optional.empty();
	}	

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) 
	{	
		for(ScotlandYardPlayer player : players)
			if(player.colour()==colour)
				return Optional.of(player.tickets().getOrDefault(ticket, 0));
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() 
	{
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		return players.get(this.currentPlayerIndex).colour();
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
