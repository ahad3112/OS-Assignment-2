import java.util.List;

public class SJF {

    List<Process> processes;
    int cs;

    int time = 0;
    Process last = null;

    public SJF(List<Process> processes, int cs) {
        this.processes = processes;
        this.cs = cs;
    }

    
    Process getShortestProcess() {

        Process best = null;

        for (Process p : processes) {


            if (p.arrival <= time && p.remaining > 0) {

                if (best == null || p.remaining < best.remaining) {
                    best = p;
                }
            }
        }

        return best;
    }

    int nextArrival() {

        int next = -1;

        for (Process p : processes) {

            if (p.remaining > 0 && p.arrival > time) {

                if (next == -1 || p.arrival < next) {
                    next = p.arrival;
                }
            }
        }

        return next;
    }


    public void run() {

        while (true) {

            Process p = getShortestProcess();


            if (p == null) {

                int next = nextArrival();

                if (next == -1) {
                    break;
                }

                time = next;
                continue;
            }

            
            if (last != null && last != p) {

                time += cs;
                last = p;
                continue;
            }

            
            if (p.start == -1) {
                p.start = time;
            }

           
            p.remaining--;
            time++;

          
            if (p.remaining == 0) {
                p.finish = time;
            }

            last = p;
        }
    }

    
    public void printResults() {

        double totalTurnaround = 0;
        double totalWaiting = 0;

        System.out.println();
        System.out.println(
                "Process Arrival Burst Finish Turnaround Waiting Response"
        );

        for (Process p : processes) {

            int turnaround = p.finish - p.arrival;
            int waiting = turnaround - p.burst;
            int response = p.start - p.arrival;

            totalTurnaround += turnaround;
            totalWaiting += waiting;

            System.out.println(
                    p.name + " "
                    + p.arrival + " "
                    + p.burst + " "
                    + p.finish + " "
                    + turnaround + " "
                    + waiting + " "
                    + response
            );
        }

        System.out.println();

        System.out.println(
                "Average Turnaround Time: "
                + totalTurnaround / processes.size()
        );

        System.out.println(
                "Average Waiting Time: "
                + totalWaiting / processes.size()
        );
    }
}