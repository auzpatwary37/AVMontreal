package run;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;



public class RunPsav {
    public static void run(String configFile, String output, String populationFile, String vehiclesFile, boolean otfvis) {
        Config config = ConfigUtils.loadConfig(configFile, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
        
        MultiModeDrtConfigGroup drt = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
        
        
        
        
        config.plans().setInputFile(populationFile);
        config.removeModule("ev");
        config.scoring().getOrCreateModeParams("private_AV");
        
        
        var drtConfigs = MultiModeDrtConfigGroup.get(config).getModalElements();
        for (DrtConfigGroup drtConfig : drtConfigs){
            //drtConfig.setVehiclesFile(vehiclesFile);
            
            drtConfig.setVehiclesFile(vehiclesFile);
        }
        
        config.controller().setOutputDirectory(output);
        config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
        Controler controler = DrtControlerCreator.createControler(config, otfvis);
        controler.run();
    }
    
    public static void main(String[] args) {
    	run("data/scn1_psav/config_with_calibrated_parameters.xml",
    			"data/scn1_psav/output",
    			"population_sce1.xml.gz",
    			"data/scn1_psav/private_vehicles_sce1.xml.gz",
    			true);
    }

}
