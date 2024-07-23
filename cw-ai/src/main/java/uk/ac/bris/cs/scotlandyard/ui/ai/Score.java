package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Piece;
import java.util.*;

public class Score {

    public int score(Board.GameState state) {
        // Return high score if MrX wins and low if loses
        if (state.getWinner().contains(Piece.Detective.BLUE)) return -10000;
        else return 10000;
    }

    public int score(Board.GameState state, List<List<Integer>> distances, int MrXLocation) {
        if (state.getWinner().contains(Piece.Detective.BLUE)) return -10000;
        else if (state.getWinner().contains(Piece.MrX.MRX)) return 10000;
        // Get distances from current MrX location and detective locations
        List<Integer> MrXDistances = distances.get(MrXLocation - 1);
        List<Integer> locations = getDetectiveLocations(state);
        // Calculate total distance and closest
        int total = 0;
        int closest = Integer.MAX_VALUE;
        for (int location: locations) {
            int distance = distances.get(MrXLocation - 1).get(location);
            total += distance;
            closest = Integer.min(closest, distance);
        }
        // Find last MrX location and freedom
        Integer lastMrX = getMrXLocation(state);
        if (lastMrX != null) total += MrXDistances.get(lastMrX);
        int freedom = state.getSetup().graph.adjacentNodes(MrXLocation).stream().filter(l -> !locations.contains(l)).toList().size();
        int score = total + (freedom * 2);
        if (closest == 1) return score - 1000;
        // Return weighted score based on number of moves and total distance
        return score;
    }

    Integer getMrXLocation(Board.GameState state){
        // Traverses backwards through travel log to find location at last reveal
        Integer round = state.getMrXTravelLog().size();
        ImmutableList<Boolean> moves = state.getSetup().moves;
        while (round > 0) {
            if (moves.get(round - 1)) return state.getMrXTravelLog().get(round - 1).location().get();
            round -= 1;
        }
        // If not yet revealed return null
        return null;
    }

    List<Integer> getDetectiveLocations(Board.GameState state) {
        // Return list of detective locations
        return state.getPlayers()
                .stream()
                .filter(p -> p != Piece.MrX.MRX)
                .map(Piece.Detective.class::cast)
                .map(d -> state.getDetectiveLocation(d).get())
                .toList();
    }
}
