package run;

import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.common.base.Preconditions;

public class RunScenario {
    public static void run(String configFile, boolean otfvis) {
        Config config = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
                new OTFVisConfigGroup());
        Controler controler = DrtControlerCreator.createControler(config, otfvis);
        controler.run();
    }

    public static void main(String[] args) {
        Preconditions.checkArgument(args.length == 1,
                "RunSav needs one arguments: path to the configuration file");
        RunScenario.run(args[0], false);
    }
}
