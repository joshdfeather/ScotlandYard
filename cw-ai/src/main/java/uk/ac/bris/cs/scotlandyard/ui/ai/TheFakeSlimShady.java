package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class TheFakeSlimShady implements Ai, Serializable {

    // returns name of Detective AI
    @Nonnull
    @Override
    public String name() {
        return "TheFakeSlimShady";
    }

    // returns the move selected by the AI
    public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
        // Read list of all distances from file t.tmp
        FileInputStream fis;
        ObjectInputStream ois;
        List<List<Integer>> distances;
        try {
            fis = new FileInputStream("cw-ai/src/main/java/uk/ac/bris/cs/scotlandyard/ui/ai/t.tmp");
            ois = new ObjectInputStream(fis);
            distances = (List<List<Integer>>) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Piece Moving = board.getAvailableMoves().asList().get(0).commencedBy();
        return pickDetectiveMove((Board.GameState) board, Moving, distances);
    }

    // algorithm to select detective move
    public Move pickDetectiveMove(Board.GameState state, Piece piece, List<List<Integer>> distances) {
        // Filter moves for relevant piece
        List<Move> moves = state.getAvailableMoves().stream().filter(m -> m.commencedBy() == piece).toList();
        // Find MrX location from previous reveal
        Integer MrXLocation = new Score().getMrXLocation(state);
        // Choose random moves before MrX first reveal
        if (MrXLocation == null) {
            Random rand = new Random();
            return moves.get(rand.nextInt(moves.size()));
        }
        // Otherwise choose move closest to last reveal
        else {
            Move chosen = null;
            Integer min = 99999;
            for (Move m : moves) {
                Board.GameState x = state.advance(m);
                Integer detectiveLocation = x.getDetectiveLocation((Piece.Detective) piece).get();
                Integer distance = distances.get(detectiveLocation - 1).get(MrXLocation);
                if (distance < min) {
                    min = distance;
                    chosen = m;
                }
            }
            return chosen;
        }
    }
}
