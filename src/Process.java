public class Process {
    String name;
    int arrival;
    int burst;
    int priority;
    int remaining;
    int start = -1;
    int finish;
    int tier2Entry;

    public Process(String name, int arrival, int burst, int priority) {
        this.name = name;
        this.arrival = arrival;
        this.burst = burst;
        this.priority = priority;
        this.remaining = burst;
    }
}