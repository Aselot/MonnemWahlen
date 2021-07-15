package ew;

public class Poll {
    private int id;
    private int seats;
    private String name;


    public Poll(int id, int seats, String name) {
        this.id = id;
        this.seats = seats;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
