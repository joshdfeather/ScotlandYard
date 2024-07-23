package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.Set;

public final class MyModelFactory implements Factory<Model> {

    @Nonnull
    @Override
    public Model build(GameSetup setup,
                       Player mrX,
                       ImmutableList<Player> detectives) {
        // return a new MyModel with appropriate parameters
        return new MyModel(setup, mrX, detectives);
    }

    // hidden class MyModel
    private final class MyModel implements Model {
        private final Set<Observer> spectators;
        private Board.GameState board; // not final -> reassigned in advance
        private final GameSetup setup;
        private final Player mrX;
        private final ImmutableList<Player> detectives;

        private MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives) {
            this.setup = setup;
            this.mrX = mrX;
            this.detectives = detectives;
            this.board = new MyGameStateFactory().build(this.setup, this.mrX, this.detectives);
            this.spectators = new HashSet<>();
        }

        // Getter method that returns encapsulated board attribute
        @Nonnull
        @Override
        public Board getCurrentBoard() {
            return this.board;
        }

        // Setter method that adds an observer to the encapsulated set of spectators
        @Override
        public void registerObserver(Observer observer) {
            if (observer == null) throw new NullPointerException("null observer");
            else if (this.spectators.contains(observer)) throw new IllegalArgumentException("already spectator");
            else this.spectators.add(observer);
        }

        // Setter method that removes an observer from the encapsulated set of spectators
        @Override
        public void unregisterObserver(Observer observer) {
            if (observer == null) throw new NullPointerException("null observer");
            else if (!this.spectators.contains(observer)) throw new IllegalArgumentException("no such observer");
            else this.spectators.remove(observer);
        }

        // Getter method that returns an ImmutableSet of Observers
        @Nonnull
        @Override
        public ImmutableSet<Observer> getObservers() {
            return ImmutableSet.copyOf(this.spectators);
        }

        // Advance the GameState and inform observers on changes
        @Override
        public void chooseMove(@Nonnull Move move) {
            this.board = this.board.advance(move);
            for (Observer o : this.spectators) {
                if (!this.board.getWinner().isEmpty()) o.onModelChanged(this.board, Observer.Event.GAME_OVER);
                else o.onModelChanged(this.board, Observer.Event.MOVE_MADE);
            }
        }
    }
}
