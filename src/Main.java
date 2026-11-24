import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Process> list = new ArrayList<>();
        list.add(new Process("P1", 0, 4, 2));
        list.add(new Process("P2", 1, 7, 3));
        list.add(new Process("P3", 3, 2, 1));

        ACS acs = new ACS(list, 3, 0);
        acs.run();
        acs.printResults();
    }
}