package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Random RANDOM = new Random();
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

            List<Team> teams = league.getConferences()
                .stream()
                .flatMap(conference -> conference.getDivisions()
                    .stream()
                    .flatMap(division -> division.getTeams().stream()))
                .collect(
                    Collectors.toList());

            List<Game> games = scheduleGames(matchups, teams);

            for (Game game : games) {
                System.out.println(game);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Game> scheduleGames(List<Matchup> matchups, List<Team> teams) {
        Collections.shuffle(matchups);
        List<Game> games = new ArrayList<>();

        final List<Matchup> reservedMatchups = reserveMatchups(matchups, teams);

        Set<Team> playedYesterday = new HashSet<>();
        Set<Team> playedTwoDaysAgo = new HashSet<>();

        LocalDate today = OPENING_DAY;
        while (!matchups.isEmpty()) {
            Set<Team> playedToday = new HashSet<>();
            final int maxGamesToday = getNumberOfGamesToday(today);

            int matchupIndex = 0;
            int gamesToday = 0;
            while (matchupIndex < matchups.size() && gamesToday < maxGamesToday) {

                Matchup match = matchups.get(matchupIndex);

                if (canPlayToday(match.getAway(), playedToday, playedYesterday, playedTwoDaysAgo)
                    && canPlayToday(match.getHome(), playedToday, playedYesterday, playedTwoDaysAgo)) {

                    matchups.remove(matchupIndex);
                    games.add(new Game(today, match));
                    playedToday.add(match.getAway());
                    playedToday.add(match.getHome());
                    gamesToday++;
                } else {
                    matchupIndex++;
                }
            }

            playedTwoDaysAgo = playedYesterday;
            playedYesterday = playedToday;
            today = today.plusDays(1);
        }

        LocalDate lastDayOfRegularSeason = today.plusDays(1);
        for (Matchup matchup : reservedMatchups) {
            games.add(new Game(lastDayOfRegularSeason, matchup));
        }

        return games;
    }

    private static boolean canPlayToday(
        Team team,
        Set<Team> playedToday,
        Set<Team> playedYesterday,
        Set<Team> playedTwoDaysAgo) {
        return !playedTwiceInLastTwoDays(team, playedYesterday, playedTwoDaysAgo) && !playedToday.contains(team);
    }

    private static boolean playedTwiceInLastTwoDays(Team team, Set<Team> playedYesterday, Set<Team> playedTwoDaysAgo) {
        return playedYesterday.contains(team) && playedTwoDaysAgo.contains(team);
    }

    private static int getNumberOfGamesToday(LocalDate today) {
        if (today.equals(OPENING_DAY)) {
            return randomBetweenInclusive(2, 3);
        } else if (today.equals(CHRISTMAS_EVE)) {
            return 0;
        } else if (today.equals(CHRISTMAS)) {
            return randomBetweenInclusive(5, 6);
        } else if (today.equals(SUPER_BOWL_SUNDAY)) {
            return 2;
        } else {
            return randomBetweenInclusive(5, 10);
        }
    }

    private static List<Matchup> reserveMatchups(List<Matchup> matchups, List<Team> teams) {
        List<Matchup> reservedMatchups = new ArrayList<>();
        for (int i = 0; i < teams.size(); i = i + 2) {
            final Team away = teams.get(i);
            final Team home = teams.get(i + 1);
            Matchup match = matchups.stream()
                .filter(m -> m.getAway().equals(away) && m.getHome().equals(home))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    "Should have been able to find matchup between any two teams at start of process."));
            reservedMatchups.add(match);
        }
        if (!matchups.removeAll(reservedMatchups)) {
            throw new RuntimeException("Could not remove reserved matchups from master list.");
        }
        return reservedMatchups;
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

        Collections.shuffle(matchups);
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

        Collections.shuffle(matchups);
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

        Collections.shuffle(matchups);
        return matchups;
    }

    private static int randomBetweenInclusive(int least, int greatest) {
        return RANDOM.nextInt((greatest + 1) - least) + least;
    }
}