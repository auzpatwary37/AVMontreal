package run;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.common.base.Preconditions;

public class RunSav {
    public static void run(String configUrl, String output, String vehiclesFile, boolean otfvis) {
        Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup(),
                new OTFVisConfigGroup());
        var taxiConfigs = MultiModeTaxiConfigGroup.get(config).getModalElements();
        for (TaxiConfigGroup taxiConfig : taxiConfigs){
            //taxiConfig.setTaxisFile(vehiclesFile);
            taxiConfig.taxisFile = vehiclesFile;
        }
        config.controller().setOutputDirectory(output);
        TaxiControlerCreator.createControler(config, otfvis).run();
    }

    public static void main(String[] args) {
        Preconditions.checkArgument(args.length == 3,
                "RunSav needs three arguments: path to the configuration file, output folder, vehicle file");
        RunSav.run(args[0], args[1], args[2],false);
    }

}
