package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MyGameState implements Board.GameState {

    public static MyGameState convert(Board b) {
        return new MyGameState(
                b.getSetup(),
                ImmutableSet.of(Piece.MrX.MRX),
                b.getMrXTravelLog(),
                new Player(
                        Piece.MrX.MRX,
                        Arrays.stream(ScotlandYard.Ticket.values()).collect(ImmutableMap.toImmutableMap(Function.identity(), ticket -> b.getPlayerTickets(Piece.MrX.MRX).get().getCount(ticket))),
                        b.getAvailableMoves().asList().get(0).source()),
                b.getPlayers()
                       .stream()
                       .filter(Piece::isDetective)
                       .map(Piece.Detective.class::cast)
                       .map(detective -> new Player(
                               detective,
                               Arrays.stream(ScotlandYard.Ticket.values()).collect(ImmutableMap.toImmutableMap(Function.identity(), ticket -> b.getPlayerTickets(detective).get().getCount(ticket))),
                               b.getDetectiveLocation(detective).get()))
                       .collect(Collectors.toList()));
    }

    private GameSetup setup;
    private ImmutableSet<Piece> remaining;
    private ImmutableList<LogEntry> log;
    private Player mrX;
    private List<Player> detectives;
    private ImmutableSet<Move> moves; //not final -> getWinner can edit this
    private ImmutableSet<Piece> winner;
    private Set<Player> players;

    public MyGameState(
            final GameSetup setup,
            final ImmutableSet<Piece> remaining,
            final ImmutableList<LogEntry> log,
            final Player mrX,
            final List<Player> detectives) {

        // Null parameters checks
        this.setup = Objects.requireNonNull(setup, "setup is null");
        this.remaining = Objects.requireNonNull(remaining, "remaining is null");
        this.log = Objects.requireNonNull(log, "log is null");
        this.mrX = Objects.requireNonNull(mrX, "mrX is null");
        this.detectives = Objects.requireNonNull(detectives, "detectives is null");

        // Detectives error checks
        for (Player detective : detectives) {
            if (detective.isMrX()) throw new IllegalArgumentException("many mrX");
            if (detective.tickets().get(ScotlandYard.Ticket.DOUBLE) > 0) throw new IllegalArgumentException("illegal double ticket");
            if (detective.tickets().get(ScotlandYard.Ticket.SECRET) > 0) throw new IllegalArgumentException("illegal secret ticket");
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
        if (!mrX.isMrX()) throw new IllegalArgumentException("no mrX");

        // Create mutable set of all players
        players = new HashSet<>(detectives);
        players.add(mrX);

        // Single moves for all remaining players (detectives and mrX)
        Set<Move> tempMoves = new HashSet<>();
        for (Player player : players) {
            if (remaining.contains(player.piece())) tempMoves.addAll(makeSingleMoves(setup, detectives, player, player.location()));
        }

        // Double moves if mrX remaining
        if (remaining.contains(mrX.piece())) tempMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location(), log));
        this.moves = ImmutableSet.copyOf(tempMoves);

        // Winner checks
        this.winner = ImmutableSet.of();
        // Winner in current state (generic detective BLUE)
        if (isWinner(Piece.Detective.BLUE) || isStuck(Piece.MrX.MRX)) {
            this.winner = ImmutableSet.copyOf(detectives.stream().map(Player::piece).collect(Collectors.toSet()));
        }
        if (isWinner(Piece.MrX.MRX) || isStuck(Piece.Detective.BLUE)) {
            this.winner = ImmutableSet.of(this.mrX.piece());
        }
    }

    // True if a detective in same location as mrX
    private boolean isWinner(Piece.Detective p) {
        for (Player det : this.detectives) {
            if(det.location() == mrX.location()) return true;
        }
        return false;
    }

    // True if mrX fills his log
    private boolean isWinner(Piece.MrX p) {
        return(remaining.contains(Piece.MrX.MRX) && log.size() == setup.moves.size());
    }

    // True if all detectives have no tickets
    private boolean isStuck(Piece.Detective p) {
        for (Player det : this.detectives) {
            if (!det.tickets().values().stream().filter(v -> v != 0).collect(Collectors.toSet()).isEmpty())
                return false;
        }
        return true;
    }

    // True if mrX has no available moves
    private boolean isStuck(Piece.MrX p) {
        if (this.remaining.contains(p)) {
            return (this.moves.stream().filter(x -> x.commencedBy().isMrX()).collect(Collectors.toSet()).isEmpty());
        }
        return false;
    }

    // True if there is a detective in location inputted
    private static boolean isDetectiveThere(int destination, List<Player> detectives) {
        for (Player detective : detectives) if (detective.location() == destination) return true;
        return false;
    }

    private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
        Set<Move.SingleMove> SingleMoves = new HashSet();
        for(int destination : setup.graph.adjacentNodes(source)) {
            //  find out if destination is occupied by a detective
            //  if the location is occupied, don't add to the collection of moves to return
            if (!isDetectiveThere(destination, detectives) ) {
                for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
                    //  find out if the player has the required tickets (and secret ticket)
                    //  if it does, construct a SingleMove and add it the collection of moves to return
                    if (player.isMrX() && (player.tickets().get(ScotlandYard.Ticket.SECRET) > 0)) SingleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
                    if (player.tickets().get(t.requiredTicket()) > 0) SingleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
                }
            }
        }
        return SingleMoves;
    }

    private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source, ImmutableList<LogEntry> log) {
        Set<Move.DoubleMove> doubleMoves = new HashSet<>();
        if (player.has(ScotlandYard.Ticket.DOUBLE) && player.isMrX() && (log.size() < setup.moves.size() - 1)) {
            //two scenarios -> 1.both the same type of ticket -> 2.two different types of ticket
            Set<Move.SingleMove> firstMoves = makeSingleMoves(setup, detectives, player, source);
            for (Move.SingleMove first : firstMoves) {
                //filter (mrX can go back to his previous position)
                //filter (second moves must have tickets > 1) if (same ticket for both moves)
                Set<Move.SingleMove> secondMoves = makeSingleMoves(setup, detectives, player, first.destination)
                        .stream()
                        .filter(m -> (m.ticket != first.ticket || player.tickets().get(m.ticket) > 1))
                        .collect(Collectors.toSet());
                //combine first and second into a unified DoubleMove variable
                for (Move.SingleMove second : secondMoves) {
                    doubleMoves.add(new Move.DoubleMove(player.piece(), source, first.ticket, first.destination, second.ticket, second.destination));
                }
            }
        }
        return doubleMoves;
    }

    @Nonnull
    @Override
    public GameSetup getSetup() {
        return this.setup;
    }

    public ImmutableSet<Piece> getRemaining() {
        return this.remaining;
    }

    // Return player for a certain piece
    private Player getPlayer(Piece p) {
        for (Player det : detectives ){
            if(det.piece() == p) {
                return det;
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public ImmutableSet<Piece> getPlayers() {
        return ImmutableSet.copyOf(players.stream().map(Player::piece).collect(Collectors.toSet()));
    }

    @Nonnull
    @Override
    public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
        Player det = getPlayer(detective);
        if (det != null) return Optional.of(det.location());
        else return Optional.empty();
    }

    public Integer getMrX() {
        return this.mrX.location();
    }

    @Nonnull
    @Override
    public Optional<TicketBoard> getPlayerTickets(Piece piece) {
        // If mrX get mrX location
        if (mrX.piece() == piece){
            return Optional.of((ticket) -> mrX.tickets().get(ticket));
        }
        // Get player location
        Player det = getPlayer(piece);
        if (det != null) return Optional.of((ticket) -> det.tickets().get(ticket));
            // Otherwise is empty
        else return Optional.empty();
    }

    @Nonnull
    @Override
    public ImmutableList<LogEntry> getMrXTravelLog() {
        return this.log;
    }

    @Nonnull
    @Override
    public ImmutableSet<Piece> getWinner() {
        // Empty moves if winner found
        if (!this.winner.isEmpty()) this.moves = ImmutableSet.of();
        return this.winner;
    }

    @Nonnull
    @Override
    public ImmutableSet<Move> getAvailableMoves() { return this.moves; }

    @Nonnull
    public List<LogEntry> logHelper(ScotlandYard.Ticket t, int d, List<LogEntry> log) {
        // Update log depending on reveal state
        if (this.setup.moves.get(log.size())) log.add(LogEntry.reveal(t, d));
        else log.add(LogEntry.hidden(t));
        return log;
    }

    Set<Piece> stuckRemover(Set<Piece> remaining) {
        // Remove stuck detectives from remaining
        for (Player det : this.detectives) {
            if (det.tickets().values().stream().filter(v -> v != 0).collect(Collectors.toSet()).isEmpty()) {
                if (remaining.contains(det.piece())) remaining.remove(det.piece());
            }
        }
        return remaining;
    }

    @Nonnull
    public GameState doSingleMove(Move.SingleMove m) {
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
    @Nonnull
    public GameState doDoubleMove(Move.DoubleMove m) {
        // remaining is detectives
        Set<Piece> nextRemaining = new HashSet<>();
        for (Player det : detectives) nextRemaining.add(det.piece());
        List<Player> nextDetectives = this.detectives;
        //add two log entries
        List<LogEntry> nextLog = new ArrayList<>(this.log);
        nextLog = this.logHelper(m.ticket1, m.destination1, nextLog);
        nextLog = this.logHelper(m.ticket2, m.destination2, nextLog);
        // Updates mrX
        Player nextMrX = this.mrX;
        nextMrX = nextMrX.use(m.ticket1).at(m.destination1).use(m.ticket2).at(m.destination2).use(ScotlandYard.Ticket.DOUBLE);
        //return MyGameState
        return new MyGameState(this.setup, ImmutableSet.copyOf(nextRemaining), ImmutableList.copyOf(nextLog), nextMrX, nextDetectives);
    }

    @Nonnull
    @Override
    public GameState advance(Move move) {
        if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
        // Functions applied when visited
        Function<Move.SingleMove, GameState> smf = (this::doSingleMove);
        Function<Move.DoubleMove, GameState> dmf = (this::doDoubleMove);
        // Functional visitor to reveal underlying type
        Move.Visitor MoveVisitor = new Move.FunctionalVisitor(smf, dmf);
        return (GameState) move.accept(MoveVisitor);
    }
}
