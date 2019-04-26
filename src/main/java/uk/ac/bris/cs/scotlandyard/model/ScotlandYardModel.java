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
import java.util.Iterator;

import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableListIterator;


import java.util.Arrays;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.Node;
import uk.ac.bris.cs.scotlandyard.ui.controller.TicketsCounter;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {

	
	final private List<Boolean> rounds;
	final private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> players = new ArrayList<ScotlandYardPlayer>();
	private Integer currentPlayerIndex = 0;
	private Integer currentRound = 0;
	private Set<Move> validMoves;
	private Integer lastMrXLocation = 0;
	private Set<Colour> winningPlayers = new HashSet<Colour>();
	private Set<Spectator> spectators = new HashSet<Spectator>();

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
		
		currentRound = NOT_STARTED;

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
		if(spectators.contains(requireNonNull(spectator))) throw new IllegalArgumentException("Spectator already registered");
		else spectators.add(spectator);
		// TODO
		//throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {

		if(spectators.contains(requireNonNull(spectator))) spectators.remove(spectator);
		else throw new IllegalArgumentException("Spectator is not registered");
		// TODO
		//throw new RuntimeException("Implement me");
		
	}

	private class PlayMoveVisitor implements MoveVisitor {
		private ScotlandYardPlayer player;

		PlayMoveVisitor() {
			player = players.get(currentPlayerIndex);
			currentPlayerIndex++;
			if(currentPlayerIndex == players.size()) currentPlayerIndex = 0;
		}
		
		public void visit(TicketMove move) {
			player.removeTicket(move.ticket());
			if(player.isDetective()) players.get(0).addTicket(move.ticket());
			player.location(move.destination());
			
			if(player.isMrX()) {
				if(rounds.get(currentRound)) lastMrXLocation = player.location();
				else {
					move = new TicketMove(move.colour(), move.ticket(), lastMrXLocation);
				}
				currentRound++;
			}

			for(Spectator s : spectators) {
				if(currentPlayerIndex == 1) s.onRoundStarted(ScotlandYardModel.this, currentRound);
				s.onMoveMade(ScotlandYardModel.this, move);
				if(isGameOver()) s.onGameOver(ScotlandYardModel.this, getWinningPlayers());
				else if(currentPlayerIndex == 0) s.onRotationComplete(ScotlandYardModel.this);
				
			}
			
		}

		public void visit(DoubleMove move) {
			player.removeTicket(DOUBLE);
			//Create spoof moves for annouce
			TicketMove fakeFirst = new TicketMove(move.colour(), move.firstMove().ticket(), lastMrXLocation);
			if(rounds.get(currentRound)) fakeFirst = move.firstMove();
			TicketMove fakeSecond = new TicketMove(move.colour(), move.secondMove().ticket(), fakeFirst.destination());
			if(rounds.get(currentRound + 1)) fakeSecond = move.secondMove();
			//Annnounce
			for(Spectator s : spectators) s.onMoveMade(ScotlandYardModel.this, new DoubleMove(move.colour(), fakeFirst, fakeSecond));
			this.visit(move.firstMove());
			this.visit(move.secondMove());
		}

		public void visit(PassMove move) {
			for(Spectator s : spectators) s.onMoveMade(ScotlandYardModel.this, move);
		}
	}

	public void accept(Move move) {
		if(move == null) {
			throw new NullPointerException("Move is null.");
		}
		if(!validMoves.contains(move)) {
			throw new IllegalArgumentException("Invalid move.");
		}

		move.visit(new PlayMoveVisitor());
		
		//At this time, currentPlayerIndex is the player about to move
		//currentRound is correct.
		//if(isGameOver()) {
		//	for(Spectator s : spectators) {
		//		s.onGameOver(this, getWinningPlayers());
		//	}
		//}
		if(currentPlayerIndex != 0) {
			startRotate();
		}

		
	}		

	private Set<Integer> getOccupiedLocations() {
		Set<Integer> occupied = new HashSet<Integer>();
		for(ScotlandYardPlayer player : players) {
			if(player.isDetective()) occupied.add(player.location());
		}
		return occupied;
	}

	private Set<TicketMove> genMovesFromNode(Node<Integer> n, ScotlandYardPlayer player) {
		Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(n);
		Set<TicketMove> moves = new HashSet<TicketMove>();
		Set<Integer> occupied = getOccupiedLocations();
		occupied.remove(player.location());

		//For all edges coming from the player node
		for(Edge<Integer, Transport> edge : edges) {
			Ticket ticket = Ticket.fromTransport(edge.data());
			Integer destination = edge.destination().value();

			//If unoccupied and has tickets, add the eddge
			if(!occupied.contains(destination)) {
				if(player.hasTickets(ticket, 1)) {
					moves.add(new TicketMove(player.colour(), ticket, destination));
				}
				if(player.hasTickets(SECRET, 1)) {
					moves.add(new TicketMove(BLACK, SECRET, destination));
				}
			}
		}

		return moves;
	}

	private Set<Move> genValidMoves() {
		ScotlandYardPlayer current = players.get(currentPlayerIndex);
		Node<Integer> firstNode = graph.getNode(current.location());
		Set<Move> moves = new HashSet<Move>();
		Set<TicketMove> singleMoves = new HashSet<TicketMove>();
		Set<DoubleMove> doubleMoves = new HashSet<DoubleMove>();

		// Generate all single moves from the player location
		singleMoves = genMovesFromNode(firstNode, current);
		if(singleMoves.isEmpty() && !current.isMrX()) {
			moves.add(new PassMove(current.colour()));
		}
		//Check conditions for double moves before generating them
		else if(current.isMrX() && current.hasTickets(DOUBLE) && getCurrentRound() < rounds.size() - 1) {
			//For each single move, look for second moves which can be made
			for (TicketMove firstMove : singleMoves) {
				Set<TicketMove> secondMoves = new HashSet<TicketMove>();
				Node<Integer> secondNode = graph.getNode(firstMove.destination());
	
				// Generate second moves from first move using new ticket count
				current.removeTicket(firstMove.ticket());
				secondMoves = genMovesFromNode(secondNode, current);
				current.addTicket(firstMove.ticket());
				
				//Add all double moves using the first move and second moves generated
				for(TicketMove secondMove : secondMoves) {
					doubleMoves.add(new DoubleMove(current.colour(), firstMove, secondMove));
				}
			}
		}
		moves.addAll(singleMoves);
		moves.addAll(doubleMoves);
		// Return all single and double moves
		return moves;
		// return Sets.union(singleMoves, doubleMoves);
	}

	@Override
	public void startRotate() {
		ScotlandYardPlayer current = players.get(currentPlayerIndex);
		if(isGameOver()) {
			if(currentRound == 0) throw new IllegalStateException("Game over at start of game");
			//for(Spectator s : spectators) s.onGameOver(this, getWinningPlayers());
		}
		else {
			validMoves = genValidMoves();
			current.player().makeMove(this, current.location(), validMoves, this);
		}
	}
	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableCollection(spectators);
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
		
		return Collections.unmodifiableSet(winningPlayers);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) 
	{
		if(colour == BLACK) return Optional.of(lastMrXLocation);
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

	private Boolean gameOverReturnAndSetPlayers(Boolean MrXWins) {
		winningPlayers = new HashSet<Colour>();
		if(MrXWins) winningPlayers.add(BLACK);
		else {
			for(ScotlandYardPlayer player : players) {
				if(player.isDetective()) winningPlayers.add(player.colour());
			}
		}
		return true;
	}

	@Override
	public boolean isGameOver() 
	{
		Boolean haveTickets = false;

		//For all detectives
		for(ScotlandYardPlayer player : players) {
			//In same location as MrXtestGameOverShouldNotifyGameOverOnce
			if(player.isDetective() && player.location() == players.get(0).location()) return gameOverReturnAndSetPlayers(false);
			for(Ticket t : Ticket.values()) {
				if(player.isDetective() && player.hasTickets(t, 1)) haveTickets = true;
			}
		}

		//No detectives have any tickets left
		if(!haveTickets) return gameOverReturnAndSetPlayers(true);

		//MrX meant to move next and start a new round
		if(currentPlayerIndex == 0) {
			//Current round is final round
			if(currentRound == rounds.size()) return gameOverReturnAndSetPlayers(true);
			//MrX cannot move next round
			if(genValidMoves().isEmpty()) return gameOverReturnAndSetPlayers(false);
		}
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		return players.get(this.currentPlayerIndex).colour();
	}

	@Override
	public int getCurrentRound() 
	{
		return currentRound;
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
