package run;

import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

public class TrialRun {
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "data/MATSim_AV/config_sc1.xml");
		
		DrtConfigGroup drtConfig = (DrtConfigGroup) config.getModules().get("drt");
		
		//drtConfig.setDrtInsertionSearchParams(new DrtInsertionSearchParams());
		
		
	}

}
