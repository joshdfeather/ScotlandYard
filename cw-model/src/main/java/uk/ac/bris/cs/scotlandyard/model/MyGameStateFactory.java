package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Move.*;
import uk.ac.bris.cs.scotlandyard.model.Piece.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;

public final class MyGameStateFactory implements Factory<GameState> {

	// public method build returns a new MyGameState
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

	// MyGameState is hidden class of MyGameStateFactory instantiated in build.
	private final class MyGameState implements GameState {

		final private GameSetup setup;
		final private ImmutableSet<Piece> remaining;
		final private ImmutableList<LogEntry> log;
		final private Player mrX;
		final private List<Player> detectives;
		final private Set<Player> players;
		private ImmutableSet<Move> moves; //not final -> getWinner can edit this
		private ImmutableSet<Piece> winner; //not final -> winnerChecks can edit this

		// MyGameState constructor initialises declared attributes from above
		// starting checks to ensure valid attribute values occurs here also
		private MyGameState(
				GameSetup setup,
				ImmutableSet<Piece> remaining,
				ImmutableList<LogEntry> log,
				Player mrX,
				List<Player> detectives) {

			// Null parameters checks
			this.setup = Objects.requireNonNull(setup, "setup is null");
			this.remaining = Objects.requireNonNull(remaining, "remaining is null");
			this.log = Objects.requireNonNull(log, "log is null");
			this.mrX = Objects.requireNonNull(mrX, "mrX is null");
			this.detectives = Objects.requireNonNull(detectives, "detectives is null");

			// Detectives error checks
			for (Player detective : detectives) {
				if (detective.isMrX()) throw new IllegalArgumentException("many mrX");
				if (detective.tickets().get(Ticket.DOUBLE) > 0) throw new IllegalArgumentException("illegal double ticket");
				if (detective.tickets().get(Ticket.SECRET) > 0) throw new IllegalArgumentException("illegal secret ticket");
			}
			if (detectives.stream().map(Player::piece).distinct().count() != detectives.size()) {
				throw new IllegalArgumentException("identical pieces");
			}
			if (detectives.stream().map(Player::location).distinct().count() != detectives.size()) {
				throw new IllegalArgumentException("identical locations");
			}

			// Setup error checks
			if (setup.moves.isEmpty()) throw new IllegalArgumentException("moves is empty");
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("graph is empty");

			// mrX check
			if (!mrX.isMrX()) throw new IllegalArgumentException("no mrX");

			// Create mutable set of all players
			Set<Player> tempPlayers = new HashSet<>(detectives);
			tempPlayers.add(mrX);
			players = ImmutableSet.copyOf(tempPlayers);

			// Moves checks
			Set<Move> tempMoves = new HashSet<>();
			for (Player player : players) {
				if (remaining.contains(player.piece())) tempMoves.addAll(makeMoves(setup, detectives, player, player.location()));
			}
			if (remaining.contains(mrX.piece())) tempMoves.addAll(makeMoves(setup, detectives, mrX, mrX.location(), log));
			this.moves = ImmutableSet.copyOf(tempMoves);

			// Winner checks
			this.winner = ImmutableSet.of();
			if (isWinner(Detective.BLUE) || isStuck(MrX.MRX)) {
				this.winner = ImmutableSet.copyOf(detectives.stream().map(Player::piece).collect(Collectors.toSet()));
			}
			if (isWinner(MrX.MRX) || isStuck(Detective.BLUE)) {
				this.winner = ImmutableSet.of(this.mrX.piece());
			}
		}

		// True if a detective in same location as mrX
		private boolean isWinner(Detective p) {
			for (Player det : this.detectives) {
				if(det.location() == mrX.location()) return true;
			}
			return false;
		}

		// True if mrX fills his log
		private boolean isWinner(MrX p) {
			return(remaining.contains(MrX.MRX) && log.size() == setup.moves.size());
		}

		// True if all detectives have no tickets
		private boolean isStuck(Detective p) {
			for (Player det : this.detectives) {
				if (!det.tickets().values().stream().filter(v -> v != 0).collect(Collectors.toSet()).isEmpty())
					return false;
			}
			return true;
		}

		// True if mrX has no available moves
		private boolean isStuck(MrX p) {
			if (this.remaining.contains(p)) {
				return (this.moves.stream().filter(x -> x.commencedBy().isMrX()).collect(Collectors.toSet()).isEmpty());
			}
			return false;
		}

		// True if there is a detective at the entered location
		private static boolean isDetectiveThere(int destination, List<Player> detectives) {
			for (Player detective : detectives) if (detective.location() == destination) return true;
			return false;
		}

		// Returns set of SingleMoves that the player can take
		private static Set<SingleMove> makeMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			Set<SingleMove> SingleMoves = new HashSet();
			// For each unoccupied adjacent node, find valid moves
			for(int destination : setup.graph.adjacentNodes(source)) {
				if (!isDetectiveThere(destination, detectives) ) {
					for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
						//  Find out if the player has the required tickets
						if (player.isMrX() && (player.tickets().get(Ticket.SECRET) > 0)) SingleMoves.add(new SingleMove(player.piece(), source, Ticket.SECRET, destination));
						if (player.tickets().get(t.requiredTicket()) > 0) SingleMoves.add(new SingleMove(player.piece(), source, t.requiredTicket(), destination));
					}
				}
			}
			return SingleMoves;
		}

		// Returns a set of DoubleMoves that the player can take (MrX only)
		private static Set<DoubleMove> makeMoves(GameSetup setup, List<Player> detectives, Player player, int source, ImmutableList<LogEntry> log) {
			Set<DoubleMove> doubleMoves = new HashSet<>();
			// ensure valid player, ticket and it is not the last move of the game
			if (player.has(Ticket.DOUBLE) && player.isMrX() && (log.size() < setup.moves.size() - 1)) {
				Set<SingleMove> firstMoves = makeMoves(setup, detectives, player, source);
				for (SingleMove first : firstMoves) {
					Set<SingleMove> secondMoves = makeMoves(setup, detectives, player, first.destination)
							.stream()
							.filter(m -> (m.ticket != first.ticket || player.tickets().get(m.ticket) > 1))
							.collect(Collectors.toSet());
					// combine first and second SingleMove into a unified DoubleMove
					for (SingleMove second : secondMoves) {
						doubleMoves.add(new DoubleMove(player.piece(), source, first.ticket, first.destination, second.ticket, second.destination));
					}
				}
			}
			return doubleMoves;
		}

		// Getter method that returns the encapsulated setup
		@Nonnull
		@Override
		public GameSetup getSetup() {
			return this.setup;
		}

		// Getter method that returns the specific player for given piece
		private Player getPlayer(Piece p) {
			for (Player det : this.detectives ){
				if(det.piece() == p) {
					return det;
				}
			}
			return null;
		}

		// Getter method that returns corresponding set of pieces for players
		@Nonnull
		@Override
		public ImmutableSet<Piece> getPlayers() {
			return ImmutableSet.copyOf(this.players.stream().map(Player::piece).collect(Collectors.toSet()));
		}

		// Getter method that returns the location for an entered detective piece, held within an optional container
		@Nonnull
		@Override
		public Optional<Integer> getDetectiveLocation(Detective detective) {
			Player det = getPlayer(detective);
			if (det != null) return Optional.of(det.location());
			else return Optional.empty();
		}

		// Getter method that returns a TicketBoard of piece entered, held within an optional container
		@Nonnull
		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			// If mrX get mrX location
			if (this.mrX.piece() == piece){
				return Optional.of((ticket) -> this.mrX.tickets().get(ticket));
			}
			// Get player location
			Player det = getPlayer(piece);
			if (det != null) return Optional.of((ticket) -> det.tickets().get(ticket));
			// Otherwise is empty
			else return Optional.empty();
		}

		// Getter method that returns MrX's encapsulated travel log
		@Nonnull
		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() {
			return this.log;
		}

		// Getter method that returns a set of encapsulated winner(s)
		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			// Empty moves if winner found
			if (!this.winner.isEmpty()) this.moves = ImmutableSet.of();
			return this.winner;
		}

		// Getter method that returns a set of encapsulated moves
		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() { return this.moves; }

		// Helper function that returns a List of log entries handling reveal moves.
		@Nonnull
		public List<LogEntry> logHelper(Ticket t, int d, List<LogEntry> nextLog) {
			// Update log depending on reveal state
			if (this.setup.moves.get(nextLog.size())) nextLog.add(LogEntry.reveal(t, d));
			else nextLog.add(LogEntry.hidden(t));
			return nextLog;
		}

		// Helper function that returns a set, without the stuck detectives present
		private Set<Piece> stuckRemover(Set<Piece> nextRemaining) {
			// Remove stuck detectives from remaining
			for (Player det : this.detectives) {
				if (det.tickets().values().stream().filter(v -> v != 0).collect(Collectors.toSet()).isEmpty()) {
					if (this.remaining.contains(det.piece())) nextRemaining.remove(det.piece());
				}
			}
			return nextRemaining;
		}

		// Returns a new GameState, where a SingleMove m has been carried out.
		@Nonnull
		public GameState doMove(SingleMove m) {
			Set<Piece> nextRemaining;
			Player nextMrX;
			List<Player> nextDetectives;
			//assign previous log which is later made immutable
			List<LogEntry> nextLog = new ArrayList<>(this.log);
			if (m.commencedBy().isMrX()) {
				//remaining is detectives and they are unchanged
				nextRemaining = this.detectives.stream().map(Player::piece).collect(Collectors.toSet());
				nextDetectives = this.detectives;
				// Updates mrX and log
				nextMrX = this.mrX.use(m.ticket).at(m.destination);
				nextLog = this.logHelper(m.ticket, m.destination, nextLog);
			}
			else {
				//if detective, remaining is filtered out by commenced by
				nextRemaining = this.remaining.stream().filter(p -> p != m.commencedBy()).collect(Collectors.toSet());
				//filter out detectives with no tickets from remaining
				nextRemaining = stuckRemover(nextRemaining);
				//if no more detectives, set remaining to mrX...
				if (nextRemaining.isEmpty()) nextRemaining.add(mrX.piece());
				// updates mrX so he receives detectives ticket...
				nextMrX = this.mrX.give(m.ticket);
				// Updates detectives with new object
				Player nextDet = Objects.requireNonNull(getPlayer(m.commencedBy()).use(m.ticket).at(m.destination));
				nextDetectives = this.detectives.stream().filter(d -> d.piece() != m.commencedBy()).collect(Collectors.toList());
				nextDetectives.add(nextDet);
			}
			return new MyGameState(this.setup, ImmutableSet.copyOf(nextRemaining), ImmutableList.copyOf(nextLog), nextMrX, nextDetectives);
		}

		// Returns a new GameState, where a DoubleMove m has been carried out.
		@Nonnull
		public GameState doMove(DoubleMove m) {
			// remaining is detectives
			Set<Piece> nextRemaining = new HashSet<>();
			for (Player det : this.detectives) nextRemaining.add(det.piece());
			List<Player> nextDetectives = this.detectives;
			// add two log entries
			List<LogEntry> nextLog = new ArrayList<>(this.log);
			nextLog = this.logHelper(m.ticket1, m.destination1, nextLog);
			nextLog = this.logHelper(m.ticket2, m.destination2, nextLog);
			// Updates mrX
			Player nextMrX = this.mrX;
			nextMrX = nextMrX.use(m.ticket1).at(m.destination1).use(m.ticket2).at(m.destination2).use(Ticket.DOUBLE);
			//return MyGameState
			return new MyGameState(this.setup, ImmutableSet.copyOf(nextRemaining), ImmutableList.copyOf(nextLog), nextMrX, nextDetectives);
		}

		// Returns a new GameState, based on a valid selected move
		//  Functions are assigned, smf and dmf (single move function, double move function) to overloaded doMove methods
		// Visitor pattern means functional visitor calls smf/dmf when visit() method called
		@Nonnull
		@Override
		public GameState advance(Move move) {
			if(!this.moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			// Functions applied when visited
			Function<SingleMove, GameState> smf = (this::doMove);
			Function<DoubleMove, GameState> dmf = (this::doMove);
			// Functional visitor to reveal underlying type
			Visitor MoveVisitor = new FunctionalVisitor(smf, dmf);
			return (GameState) move.accept(MoveVisitor);
		}
	}

}
