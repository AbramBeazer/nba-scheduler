package org.example;

import java.util.Objects;

public class Team {

    private String location;
    private String name;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return location + " " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Team team = (Team) o;
        return location.equals(team.location) && name.equals(team.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, name);
    }
}
