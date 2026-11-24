import java.util.ArrayList;
import java.util.List;

public class ACS {
    List<Process> processes;
    int quantum;
    int cs;
    int agingInterval = 4;

    List<Process> tier1 = new ArrayList<>();
    List<Process> tier2 = new ArrayList<>();

    int time = 0;
    Process last = null;

    public ACS(List<Process> processes, int quantum, int cs) {
        this.processes = processes;
        this.quantum = quantum;
        this.cs = cs;
    }

    void addArrivals() {
        for (Process p : processes) {
            if (p.arrival <= time && !tier1.contains(p) && p.remaining > 0 && p.start == -1) {
                tier1.add(p);
            }
        }
    }

    void contextSwitch(Process p) {
        if (last != null && last != p) {
            time += cs;
        }
        last = p;
    }

    int effective(Process p) {
        return p.priority - (time - p.tier2Entry) / agingInterval;
    }

    String list1() {
        if (tier1.isEmpty()) {
            return "Empty";
        }
        String s = "";
        for (Process p : tier1) {
            s += p.name + "(rem " + p.remaining + ", pri " + p.priority + ") ";
        }
        return s;
    }

    String list2() {
        if (tier2.isEmpty()) {
            return "Empty";
        }
        String s = "";
        for (Process p : tier2) {
            s += p.name + "(rem " + p.remaining + ", pri " + effective(p) + ") ";
        }
        return s;
    }

    void printState() {
        System.out.println("Time " + time);
        System.out.println("  Tier 1: " + list1());
        System.out.println("  Tier 2: " + list2());
    }

    Process pickFromTier2() {
        Process best = tier2.get(0);
        for (Process p : tier2) {
            if (effective(p) < effective(best)) {
                best = p;
            }
        }
        tier2.remove(best);
        return best;
    }

    int nextArrival() {
        int next = -1;
        for (Process p : processes) {
            if (p.remaining > 0 && p.start == -1) {
                if (next == -1 || p.arrival < next) {
                    next = p.arrival;
                }
            }
        }
        return next;
    }

    public void run() {
        while (true) {
            addArrivals();

            if (!tier1.isEmpty()) {
                Process p = tier1.remove(0);

                contextSwitch(p);

                if (p.start == -1) {
                    p.start = time;
                }

                addArrivals();

                int run = Math.min(quantum, p.remaining);
                time += run;
                p.remaining -= run;

                if (p.remaining == 0) {
                    p.finish = time;
                } else {
                    p.tier2Entry = time;
                    tier2.add(p);
                }

                addArrivals();

                System.out.println(p.name + " ran until " + time);
                printState();
            } else if (!tier2.isEmpty()) {
                Process p = pickFromTier2();

                contextSwitch(p);

                if (p.start == -1) {
                    p.start = time;
                }

                time += p.remaining;
                p.remaining = 0;
                p.finish = time;

                System.out.println(p.name + " ran until " + time);
                printState();
            } else {
                int next = nextArrival();
                if (next == -1) {
                    break;
                }
                time = next;
            }
        }
    }

    public void printResults() {
        double totalTurnaround = 0;
        double totalWaiting = 0;

        System.out.println();
        System.out.println("Process Arrival Finish Turnaround Waiting Response");

        for (Process p : processes) {
            int turnaround = p.finish - p.arrival;
            int waiting = turnaround - p.burst;
            int response = p.start - p.arrival;

            totalTurnaround += turnaround;
            totalWaiting += waiting;

            System.out.println(p.name + " " + p.arrival + " " + p.finish + " " + turnaround + " " + waiting + " " + response);
        }

        System.out.println("Average Turnaround Time: " + totalTurnaround / processes.size());
        System.out.println("Average Waiting Time: " + totalWaiting / processes.size());
    }
}