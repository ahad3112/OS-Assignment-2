import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Main {

    static List<Process> build(int[][] data) {
        List<Process> list = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            list.add(new Process("P" + (i + 1), data[i][0], data[i][1], data[i][2]));
        }
        return list;
    }

    static double avgWaiting(List<Process> ps) {
        double total = 0;
        for (Process p : ps) total += p.finish - p.arrival - p.burst;
        return total / ps.size();
    }

    static double avgTurnaround(List<Process> ps) {
        double total = 0;
        for (Process p : ps) total += p.finish - p.arrival;
        return total / ps.size();
    }

    static void runExperiments() {
        int[][] tc1 = { {0, 4, 2}, {1, 7, 3}, {3, 2, 1} };
        int[][] tc2 = { {0, 5, 4}, {1, 6, 2}, {4, 4, 3} };
        int[][] tc3 = { {0, 8, 1}, {1, 4, 2}, {2, 2, 3} };
        int[][][] cases = { tc1, tc2, tc3 };

        System.out.println("========== TEST CASE 1 (Q=3, CS=0) ==========");
        ACS acs1 = new ACS(build(tc1), 3, 0);
        acs1.run();
        acs1.printResults();

        System.out.println("\n========== TEST CASE 2 (Q=3, CS=1) ==========");
        ACS acs2 = new ACS(build(tc2), 3, 1);
        acs2.run();
        acs2.printResults();

        System.out.println("\n========== TEST CASE 3 (Q=2, CS=1) ==========");
        ACS acs3 = new ACS(build(tc3), 2, 1);
        acs3.run();
        acs3.printResults();

        System.out.println("\n========== GRAPH DATA (CSV) ==========");
        System.out.println("testcase,algo,cs,q,avgWait,avgTurn");
        for (int tc = 0; tc < cases.length; tc++) {
            for (int cs = 0; cs <= 3; cs++) {
                List<Process> sjfList = build(cases[tc]);
                new SJF(sjfList, cs).run();
                System.out.printf("TC%d,SJF,%d,-,%.2f,%.2f%n",
                        tc + 1, cs, avgWaiting(sjfList), avgTurnaround(sjfList));
                for (int q = 1; q <= 6; q++) {
                    List<Process> acsList = build(cases[tc]);
                    ACS acs = new ACS(acsList, q, cs);
                    acs.run();
                    System.out.printf("TC%d,ACS,%d,%d,%.2f,%.2f%n",
                            tc + 1, cs, q, avgWaiting(acsList), avgTurnaround(acsList));
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            runExperiments();
            return;
        }
        // MODERN LOOK (Nimbus)
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new SchedulerGUI().setVisible(true));
    }
}

class SchedulerGUI extends JFrame {

    static class Seg {
        String label;
        int start, end;
        boolean cs;

        Seg(String label, int start, int end, boolean cs) {
            this.label = label;
            this.start = start;
            this.end = end;
            this.cs = cs;
        }
    }

    static class ACSTrace extends ACS {
        List<Seg> segs = new ArrayList<>();
        Map<Process, Integer> prev = new HashMap<>();
        Process lastRan = null;

        ACSTrace(List<Process> ps, int q, int cs) {
            super(ps, q, cs);
            for (Process p : ps) prev.put(p, p.burst);
        }

        @Override
        void printState() {
            Process p = last;
            int len = prev.get(p) - p.remaining;
            prev.put(p, p.remaining);
            int start = time - len;
            if (lastRan != null && lastRan != p && cs > 0) {
                segs.add(new Seg("CS", start - cs, start, true));
            }
            segs.add(new Seg(p.name, start, time, false));
            lastRan = p;
            super.printState();
        }
    }

    static class SJFTrace extends SJF {
        List<Seg> segs = new ArrayList<>();

        SJFTrace(List<Process> ps, int cs) {
            super(ps, cs);
        }

        @Override
        Process getShortestProcess() {
            Process p = super.getShortestProcess();
            if (p != null) {
                if (last != null && last != p) {
                    if (cs > 0) segs.add(new Seg("CS", time, time + cs, true));
                } else {
                    Seg l = segs.isEmpty() ? null : segs.get(segs.size() - 1);
                    if (l != null && !l.cs && l.label.equals(p.name) && l.end == time) {
                        l.end = time + 1;
                    } else {
                        segs.add(new Seg(p.name, time, time + 1, false));
                    }
                }
            }
            return p;
        }
    }

    static class GanttPanel extends JPanel {
        static final int UNIT = 26, LEFT = 20, TOP = 30, H = 46;
        List<Seg> segs = new ArrayList<>();
        Map<String, Color> colors = new HashMap<>();

        GanttPanel() {
            setBackground(Color.WHITE);
        }

        void setData(List<Seg> s, Map<String, Color> c) {
            segs = s;
            colors = c;
            int end = 0;
            for (Seg x : s) end = Math.max(end, x.end);
            setPreferredSize(new Dimension(LEFT * 2 + end * UNIT, 120));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g.getFontMetrics();
            TreeSet<Integer> marks = new TreeSet<>();

            for (Seg s : segs) {
                int x = LEFT + s.start * UNIT;
                int w = (s.end - s.start) * UNIT;
                g.setColor(s.cs ? Color.LIGHT_GRAY : colors.getOrDefault(s.label, Color.CYAN));
                g.fillRect(x, TOP, w, H);
                g.setColor(Color.BLACK);
                g.drawRect(x, TOP, w, H);
                String t = s.cs ? "CS" : s.label;
                int tw = fm.stringWidth(t);
                if (tw <= w - 2) g.drawString(t, x + (w - tw) / 2, TOP + H / 2 + fm.getAscent() / 2 - 2);
                marks.add(s.start);
                marks.add(s.end);
            }

            int lastX = -100;
            for (int m : marks) {
                int x = LEFT + m * UNIT;
                g.drawLine(x, TOP + H, x, TOP + H + 5);
                String t = String.valueOf(m);
                int tx = x - fm.stringWidth(t) / 2;
                if (tx > lastX + 4) {
                    g.drawString(t, tx, TOP + H + 20);
                    lastX = tx + fm.stringWidth(t);
                }
            }
        }
    }

    private static final int[][][] PRESETS = {
            {{0, 4, 2}, {1, 7, 3}, {3, 2, 1}},
            {{0, 5, 4}, {1, 6, 2}, {4, 4, 3}},
            {{0, 8, 1}, {1, 4, 2}, {2, 2, 3}}
    };
    private static final int[] PRESET_Q = {3, 3, 2};
    private static final int[] PRESET_CS = {0, 1, 1};

    // BRIGHT COLORS
    private static final Color[] PALETTE = {
            new Color(255, 99, 71),   // Bright Red
            new Color(30, 144, 255),  // Bright Blue
            new Color(50, 205, 50),   // Bright Green
            new Color(255, 165, 0),   // Bright Orange
            new Color(138, 43, 226),  // Bright Purple
            new Color(255, 20, 147),  // Bright Pink
            new Color(0, 206, 209),   // Bright Cyan
            new Color(255, 215, 0)    // Bright Gold
    };

    private final DefaultTableModel input =
            new DefaultTableModel(new String[]{"Process", "Arrival", "Burst", "Priority"}, 0);
    private final JTable inputTable = new JTable(input);
    private final DefaultTableModel results =
            new DefaultTableModel(new String[]{"Process", "Arrival", "Burst", "Finish", "Turnaround", "Waiting", "Response"}, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
    private final JSpinner csSpin = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
    private final JSpinner qSpin = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
    private final JSpinner agingSpin = new JSpinner(new SpinnerNumberModel(4, 1, 50, 1));
    private final JComboBox<String> algoBox = new JComboBox<>(new String[]{"ACS (3-tier)", "Preemptive SJF"});
    private final JComboBox<String> presetBox =
            new JComboBox<>(new String[]{"Custom", "TC1 (assignment)", "TC2 (assignment)", "TC3 (ours)"});
    private final JLabel avgLabel = new JLabel(" ");
    private final GanttPanel gantt = new GanttPanel();
    private final JTextArea log = new JTextArea();

    public SchedulerGUI() {
        setTitle("CPU Scheduler Simulator - SJF vs ACS");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        top.add(new JLabel("Test case:"));
        top.add(presetBox);
        top.add(new JLabel("Algorithm:"));
        top.add(algoBox);
        top.add(new JLabel("CS:"));
        top.add(csSpin);
        top.add(new JLabel("Q:"));
        top.add(qSpin);
        top.add(new JLabel("Aging interval:"));
        top.add(agingSpin);
        JButton runBtn = new JButton("Run");
        top.add(runBtn);
        add(top, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setBorder(BorderFactory.createTitledBorder("Processes (double-click a cell to edit)"));
        left.add(new JScrollPane(inputTable), BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        JButton addBtn = new JButton("Add process");
        JButton delBtn = new JButton("Remove selected");
        btns.add(addBtn);
        btns.add(delBtn);
        left.add(btns, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(340, 0));
        add(left, BorderLayout.WEST);

        JPanel resPanel = new JPanel(new BorderLayout());
        resPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        resPanel.add(new JScrollPane(new JTable(results)), BorderLayout.CENTER);
        resPanel.add(avgLabel, BorderLayout.SOUTH);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Gantt chart", new JScrollPane(gantt));
        tabs.addTab("Time-step log", new JScrollPane(log));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resPanel, tabs);
        split.setResizeWeight(0.35);
        add(split, BorderLayout.CENTER);

        addBtn.addActionListener(e -> input.addRow(new Object[]{"P" + (input.getRowCount() + 1), 0, 1, 1}));
        delBtn.addActionListener(e -> {
            int r = inputTable.getSelectedRow();
            if (r >= 0) input.removeRow(r);
        });
        runBtn.addActionListener(e -> onRun());
        presetBox.addActionListener(e -> loadPreset());
        algoBox.addActionListener(e -> {
            boolean acs = algoBox.getSelectedIndex() == 0;
            qSpin.setEnabled(acs);
            agingSpin.setEnabled(acs);
        });

        presetBox.setSelectedIndex(1);
        setSize(1100, 700);
        setLocationRelativeTo(null);
    }

    private void loadPreset() {
        int i = presetBox.getSelectedIndex();
        if (i == 0) return;
        input.setRowCount(0);
        int[][] data = PRESETS[i - 1];
        for (int r = 0; r < data.length; r++) {
            input.addRow(new Object[]{"P" + (r + 1), data[r][0], data[r][1], data[r][2]});
        }
        qSpin.setValue(PRESET_Q[i - 1]);
        csSpin.setValue(PRESET_CS[i - 1]);
    }

    private int num(int r, int c) {
        try {
            return Integer.parseInt(String.valueOf(input.getValueAt(r, c)).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Row " + (r + 1) + ": '" + input.getColumnName(c) + "' must be a whole number.");
        }
    }

    private List<Process> readProcesses() {
        if (inputTable.isEditing()) inputTable.getCellEditor().stopCellEditing();
        List<Process> list = new ArrayList<>();
        for (int r = 0; r < input.getRowCount(); r++) {
            String name = String.valueOf(input.getValueAt(r, 0)).trim();
            int a = num(r, 1), b = num(r, 2), p = num(r, 3);
            if (name.isEmpty()) throw new IllegalArgumentException("Row " + (r + 1) + ": name is empty.");
            if (a < 0) throw new IllegalArgumentException("Row " + (r + 1) + ": arrival must be 0 or more.");
            if (b < 1) throw new IllegalArgumentException("Row " + (r + 1) + ": burst must be 1 or more.");
            for (Process x : list) {
                if (x.name.equals(name)) throw new IllegalArgumentException("Duplicate process name: " + name);
            }
            list.add(new Process(name, a, b, p));
        }
        if (list.isEmpty()) throw new IllegalArgumentException("Add at least one process.");
        return list;
    }

    private void onRun() {
        List<Process> ps;
        try {
            ps = readProcesses();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int cs = (Integer) csSpin.getValue();
        int q = (Integer) qSpin.getValue();
        int aging = (Integer) agingSpin.getValue();

        List<Seg> segs;
        StringBuilder text = new StringBuilder();
        if (algoBox.getSelectedIndex() == 0) {
            ACSTrace acs = new ACSTrace(ps, q, cs);
            acs.agingInterval = aging;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintStream old = System.out;
            System.setOut(new PrintStream(buf, true));
            try {
                acs.run();
            } finally {
                System.setOut(old);
            }
            segs = acs.segs;
            text.append(buf);
        } else {
            SJFTrace sjf = new SJFTrace(ps, cs);
            sjf.run();
            segs = sjf.segs;
            text.append("Preemptive SJF - CPU timeline (CS = context switch)\n\n");
            for (Seg s : segs) {
                text.append(String.format("%3d - %-3d  %s%n", s.start, s.end, s.cs ? "CS" : s.label));
            }
        }

        results.setRowCount(0);
        Map<String, Color> colors = new HashMap<>();
        double tt = 0, tw = 0, tr = 0;
        for (int i = 0; i < ps.size(); i++) {
            Process p = ps.get(i);
            int turn = p.finish - p.arrival;
            int wait = turn - p.burst;
            int resp = p.start - p.arrival;
            tt += turn;
            tw += wait;
            tr += resp;
            results.addRow(new Object[]{p.name, p.arrival, p.burst, p.finish, turn, wait, resp});
            colors.put(p.name, PALETTE[i % PALETTE.length]);
        }
        avgLabel.setText(String.format("  Average Turnaround = %.2f     Average Waiting = %.2f     Average Response = %.2f",
                tt / ps.size(), tw / ps.size(), tr / ps.size()));

        gantt.setData(segs, colors);
        log.setText(text.toString());
        log.setCaretPosition(0);
    }
}