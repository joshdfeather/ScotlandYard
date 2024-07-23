package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;
import java.io.IOException;
import java.util.List;

public class ScoreTest {

    // Tests for score method used for winner
    @Test public void checkWinnerScoreIfSameLocation() throws IOException {
        Score score = new Score();
        GameSetup setup = new GameSetup(ScotlandYard.standardGraph(), ScotlandYard.STANDARD24MOVES.asList());
        Player MrX = new Player(Piece.MrX.MRX, ScotlandYard.defaultMrXTickets(), ScotlandYard.generateMrXLocation(4));
        ImmutableList<Player> detectives = ImmutableList.of(new Player(Piece.Detective.BLUE, ScotlandYard.defaultDetectiveTickets(), ScotlandYard.generateMrXLocation(4)));
        MyGameStateFactory factory = new MyGameStateFactory();
        Board.GameState state = factory.build(setup, MrX, detectives);
        assert(score.score(state) == -10000);
    }

    @Test public void checkWinnerScoreIfDifferentLocation() throws IOException {
        Score score = new Score();
        GameSetup setup = new GameSetup(ScotlandYard.standardGraph(), ScotlandYard.STANDARD24MOVES.asList());
        Player MrX = new Player(Piece.MrX.MRX, ScotlandYard.defaultMrXTickets(), ScotlandYard.generateMrXLocation(4));
        ImmutableList<Player> dets = ImmutableList.of(new Player(Piece.Detective.BLUE, ScotlandYard.defaultDetectiveTickets(), ScotlandYard.generateDetectiveLocations(1,2).get(0)));
        MyGameStateFactory factory = new MyGameStateFactory();
        Board.GameState state = factory.build(setup, MrX, dets);
        assert(score.score(state) != -10000);
    }

    // Tests for freedom and distance based score method
    @Test public void checkScore1() throws IOException {
        Score score = new Score();
        GameSetup setup = new GameSetup(ScotlandYard.standardGraph(), ScotlandYard.STANDARD24MOVES.asList());
        Player MrX = new Player(Piece.MrX.MRX, ScotlandYard.defaultMrXTickets(), ScotlandYard.generateMrXLocation(4));
        ImmutableList<Player> dets = ImmutableList.of(new Player(Piece.Detective.BLUE, ScotlandYard.defaultDetectiveTickets(), ScotlandYard.generateDetectiveLocations(1,2).get(0)));
        MyGameStateFactory factory = new MyGameStateFactory();
        Board.GameState state = factory.build(setup, MrX, dets);
        MyGameState s = MyGameState.convert(state);
        List<List<Integer>> distances = new Distances().readFile();
        // Detective is at 94 and MrX is at 51
        // Freedom = 5 and total = 4. Best route 51 -> 67 -> 79 -> 93 -> 94
        assert(score.score(s, distances, s.getMrX()) == 14);
    }

    @Test public void checkScore2() throws IOException {
        Score score = new Score();
        GameSetup setup = new GameSetup(ScotlandYard.standardGraph(), ScotlandYard.STANDARD24MOVES.asList());
        Player MrX = new Player(Piece.MrX.MRX, ScotlandYard.defaultMrXTickets(), ScotlandYard.generateMrXLocation(4));
        ImmutableList<Player> dets = ImmutableList.of(
                new Player(Piece.Detective.BLUE, ScotlandYard.defaultDetectiveTickets(), ScotlandYard.generateDetectiveLocations(1,2).get(0)),
                new Player(Piece.Detective.RED, ScotlandYard.defaultDetectiveTickets(), ScotlandYard.generateDetectiveLocations(6,2).get(0)));
        MyGameStateFactory factory = new MyGameStateFactory();
        Board.GameState state = factory.build(setup, MrX, dets);
        MyGameState s = MyGameState.convert(state);
        List<List<Integer>> distances = new Distances().readFile();
        // Detectives at 155, 94 and MrX is at 51
        // Freedom = 5 and total = 9. Best route 51 -> 67 -> 79 -> 93 -> 94 and 51 -> 67 -> 111 -> 153 -> 154 -> 155
        assert(score.score(s, distances, s.getMrX()) == 19);
    }
}
