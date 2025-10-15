package run;

import org.matsim.run.RunMatsim;

public class RunBase {

    public static void main(String[] args) {
        new RunMatsim(args).run();
    }
}
