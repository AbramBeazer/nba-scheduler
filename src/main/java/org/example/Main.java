package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int GAMES_PER_SEASON = 82;

    private static final int YEAR = 2023;
    private static final int NEXT_YEAR = YEAR + 1;

    private static final LocalDate OPENING_DAY = LocalDate.of(YEAR, Month.OCTOBER, 1)
        .with(TemporalAdjusters.dayOfWeekInMonth(-2, DayOfWeek.TUESDAY));
    private static final LocalDate HALLOWEEN = LocalDate.of(YEAR, Month.OCTOBER, 31);
    private static final LocalDate CHRISTMAS_EVE = LocalDate.of(YEAR, Month.DECEMBER, 24);
    private static final LocalDate CHRISTMAS = LocalDate.of(YEAR, Month.DECEMBER, 25);
    private static final LocalDate SUPER_BOWL_SUNDAY = LocalDate.of(NEXT_YEAR, Month.FEBRUARY, 1)
        .with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY));

    public static void main(String[] args) {
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("nba.json")) {
            Objects.requireNonNull(stream, "Input stream cannot be null");
            League league = MAPPER.readValue(stream.readAllBytes(), League.class);

            List<Matchup> matchups = getDivisionMatchups(league);
            matchups.addAll(getConferenceMatchups(league));
            matchups.addAll(getInterConferenceMatchups(league));

            for (Matchup matchup : matchups) {
                System.out.println(matchup);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Matchup> getDivisionMatchups(League league) {
        List<Matchup> matchups = new ArrayList<>();
        for (Conference conf : league.getConferences()) {
            for (Division div : conf.getDivisions()) {
                List<Team> teams = div.getTeams();
                for (int i = 0; i < teams.size(); i++) {
                    Team away = teams.get(i);
                    for (int j = 0; j < teams.size(); j++) {
                        if (i != j) {
                            Team home = teams.get(j);
                            matchups.add(new Matchup(away, home));
                            matchups.add(new Matchup(away, home));
                        }
                    }
                }
            }
        }
        return matchups;
    }

    private static List<Matchup> getConferenceMatchups(League league) {
        List<Matchup> matchups = new ArrayList<>();
        final int offset = YEAR % 5;

        for (Conference conf : league.getConferences()) {
            List<Division> divisions = conf.getDivisions();
            for (int i = 0; i < divisions.size(); i++) {
                List<Team> teams = divisions.get(i).getTeams();
                for (int j = i + 1; j < divisions.size(); j++) {
                    List<Team> otherTeams = divisions.get(j).getTeams();
                    for (int k = 0; k < teams.size(); k++) {
                        for (int l = 0; l < otherTeams.size(); l++) {
                            matchups.add(new Matchup(teams.get(k), otherTeams.get(l)));
                            matchups.add(new Matchup(otherTeams.get(l), teams.get(k)));
                            if (l != (k + offset) % teams.size()) {
                                matchups.add(new Matchup(teams.get(k), otherTeams.get(l)));
                            }
                            if (l != (k + 2 + offset) % teams.size()) {
                                matchups.add(new Matchup(otherTeams.get(l), teams.get(k)));
                            }
                        }
                    }
                }
            }
        }
        return matchups;
    }

    private static List<Matchup> getInterConferenceMatchups(League league) {
        List<Matchup> matchups = new ArrayList<>();

        List<Team> teams = league.getConferences()
            .get(0)
            .getDivisions()
            .stream()
            .flatMap(div -> div.getTeams().stream())
            .collect(
                Collectors.toList());

        List<Team> otherTeams = league.getConferences()
            .get(1)
            .getDivisions()
            .stream()
            .flatMap(div -> div.getTeams().stream())
            .collect(
                Collectors.toList());

        for (Team team : teams) {
            for (Team otherTeam : otherTeams) {
                matchups.add(new Matchup(team, otherTeam));
                matchups.add(new Matchup(otherTeam, team));
            }
        }

        return matchups;
    }
}