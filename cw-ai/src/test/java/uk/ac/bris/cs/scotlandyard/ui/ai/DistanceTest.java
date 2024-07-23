package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.io.IOException;
import java.util.List;

public  class DistanceTest {

    @Test
    public void distance1() {
        List<List<Integer>> distances = new Distances().readFile();
        assert(distances.get(50).get(94) == 4);
        // Best route 51 -> 67 -> 79 -> 93 -> 94
    }

    @Test
    public void distance2() {
        List<List<Integer>> distances = new Distances().readFile();
        assert(distances.get(50).get(155) == 5);
        // Best route 51 -> 67 -> 111 -> 153 -> 154 -> 155
    }

    @Test
    public void distance3() {
        List<List<Integer>> distances = new Distances().readFile();
        assert(distances.get(0).get(1) == 0);
    }

    @Test
    public void distance4() {
        List<List<Integer>> distances = new Distances().readFile();
        assert(distances.get(0).get(9) == 1);
        // Best route 1 -> 9
    }

    @Test
    public void distance5() {
        List<List<Integer>> distances = new Distances().readFile();
        assert(distances.get(85).get(158) == 3);
        // Best route 186 -> 116 -> 142 -> 158
    }

    @Test
    public void getMrXNullLocationTest() throws IOException {
        Score score = new Score();
        GameSetup setup = new GameSetup(ScotlandYard.standardGraph(), ScotlandYard.STANDARD24MOVES.asList());
        Player MrX = new Player(Piece.MrX.MRX, ScotlandYard.defaultMrXTickets(), ScotlandYard.generateMrXLocation(4));
        ImmutableList<Player> dets = ImmutableList.of(
                new Player(Piece.Detective.BLUE, ScotlandYard.defaultDetectiveTickets(), ScotlandYard.generateDetectiveLocations(1,2).get(0)));
        MyGameStateFactory factory = new MyGameStateFactory();
        Board.GameState state = factory.build(setup, MrX, dets);
        MyGameState s = MyGameState.convert(state);
        int i = 0;
        assert(score.getMrXLocation(s) == null);
    }
}
