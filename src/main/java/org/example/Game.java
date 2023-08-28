package org.example;

import java.time.LocalDate;

public class Game {

    private LocalDate date;
    private Matchup matchup;

    public Game(LocalDate date, Matchup matchup) {
        this.date = date;
        this.matchup = matchup;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Matchup getMatchup() {
        return matchup;
    }

    public void setMatchup(Matchup matchup) {
        this.matchup = matchup;
    }

    @Override
    public String toString() {
        return date.toString() + " -- " + matchup.toString();
    }
}
