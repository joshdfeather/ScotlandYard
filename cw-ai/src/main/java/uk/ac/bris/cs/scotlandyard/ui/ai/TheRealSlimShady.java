package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class TheRealSlimShady implements Ai, Serializable {

	@Nonnull
	@Override
	public String name() {
		return "TheRealSlimShady";
	}

	@Nonnull
	@Override
	public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		// Initialise distances where 1st index is start location + 2nd index is end location
		List<List<Integer>> distances = new Distances().readFile();
		// Convert : Board -> GameState
		MyGameState x = MyGameState.convert(board);
		ImmutableList<Move> moves = x.getAvailableMoves().asList();
		// return move at index found using minimax
		int index = minimax(distances, true, 3, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, x);
		return moves.get(index);
	}

	public int minimax(List<List<Integer>> distances, Boolean MrXTurn, int source, int depth, int Alpha, int Beta,  MyGameState state) {
		// Terminates recursion at depth 0 or at a win state
		if (depth == 0) {
			return new Score().score(state, distances, state.getMrX());
		}
		if (!state.getWinner().isEmpty()) return new Score().score(state);
		// Maximising Player recursively picks best eval at each level
		if (MrXTurn) {
			int maxEval = Integer.MIN_VALUE;
			int i = 0, index = 0;
			for (Move m : state.getAvailableMoves()) {
				MyGameState next = (MyGameState) state.advance(m);
				int eval = minimax(distances,false, source, depth - 1, Alpha, Beta, next);
				// Update index at top level
				if (depth == source) {
					if (eval > maxEval) index = i;
				}
				Alpha = Integer.max(Alpha, eval);
				maxEval = Integer.max(eval, maxEval);
				// increment temporary index and Alpha-Beta prune if necessary
				i += 1;
				if (Beta <= Alpha) break;
			}
			// Return index or eval
			if (depth == source) return index;
			else return maxEval;
		}
		// Minimising Player picks move with worst eval at each level
		else {
			int minEval = Integer.MAX_VALUE;
			// Improve runtime by keeping moves that make MrX score worse
			for (Move m : remove(state, state.getAvailableMoves(), distances)) {
				MyGameState next = (MyGameState) state.advance(m);
				// Recurse with lower depth and MrX turn if all detectives moved
				int eval;
				if (next.getRemaining().contains(Piece.MrX.MRX)) {
					eval = minimax(distances, true, source, depth - 1, Alpha, Beta, next);
				} else {
					eval = minimax(distances, false, source, depth, Alpha, Beta, next);
				}
				minEval = Integer.min(minEval, eval);
				Beta = Integer.min(eval, Beta);
				// Alpha-Beta prune if necessary
				if (Beta <= Alpha) break;
			}
			return minEval;
		}
	}

	ImmutableSet<Move> remove(MyGameState state, ImmutableSet<Move> moves, List<List<Integer>> distances) {
		// Returns moves that provide a lower MrX score
		return ImmutableSet.copyOf(moves.stream().filter(m -> new Score().score(state.advance(m), distances, state.getMrX()) < new Score().score(state, distances, state.getMrX())).toList());
	}

}