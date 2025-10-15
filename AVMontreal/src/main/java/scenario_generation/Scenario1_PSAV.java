package scenario_generation;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.households.Household;

public class Scenario1_PSAV {

	public static void main(String[] args) {
		String inputPlansFile = "data/5 p/prepared_population.xml.gz";
		String outputPlansFile = "data/scn1_psav/population_sce1.xml.gz";


		String outputVehiclesFile = "data/scn1_psav/private_vehicles_sce1.xml.gz";

		// Create Scenario + Population
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population population = scenario.getPopulation();
		


		// Read the plans
		population = PopulationUtils.readPopulation(inputPlansFile);

		// --- Step 1: identify motorized households ---
		//Set<Id<Household>> motorHouseholdIds = new HashSet<>();
		Map<Id<Household>, Id<Link>> householdHomeLink = new HashMap<>(); 
		// Map householdId -> linkHome

		for (Person person : population.getPersons().values()) {
			// The "car availability" and "householdId" might come from person attributes or plan attributes.
			// In your Python you used person[0][2] and person[0][5], which correspond to some XML attribute ordering.
			// Here, we assume they are stored as attributes or inside the person’s attributes.

			String carAvail = PersonUtils.getCarAvail(person);
			Id<Household> hhIdObj = Id.create(Long.toString((long) person.getAttributes().getAttribute("household_id")), Household.class);
			if (carAvail == null || hhIdObj == null) {
				continue;
			}

			// We also need to find the home link: assume first activity in plan is home and has linkId
			Id<Link> homeLink = null;
			if (person.getSelectedPlan() != null) {
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						// You may check if this is the “home” activity by type or index

						homeLink = act.getLinkId();
						break;

					}
				}
			}

			//TODO: Check the logic, seems like if there are multiple person in a household and the first 
			//activity link id of these persons do not match, then this logic will just take the first person's
			//first activity link id as the household link id. Oct 14, 2025; Ashraf. 
			if (!"never".equalsIgnoreCase(carAvail)) {

				if (homeLink != null && !householdHomeLink.containsKey(hhIdObj)) {
					householdHomeLink.put(hhIdObj, homeLink);
				}
			}
		}

		System.out.println("Number of PAV: " + householdHomeLink.size());

		// --- Step 2: modify each person’s legs and assign subpopulation ---
		for (Person person : population.getPersons().values()) {
			Id<Household> hhId = Id.create(Long.toString((long) person.getAttributes().getAttribute("household_id")),Household.class);

			boolean isMotorized = (hhId != null && householdHomeLink.containsKey(hhId));

			// assign subpopulation attribute
			if (isMotorized) {
				PopulationUtils.putSubpopulation(person, "private");
				PersonUtils.setCarAvail(person, "never");
			} else {
				PopulationUtils.putSubpopulation(person, "no_private");

			}

			// modify legs
			if (person.getSelectedPlan() != null) {
				for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						String mode = leg.getMode();
						if (isMotorized) {
							// if motorized household
							if ("car".equals(mode)) {
								leg.setMode("private_AV");
								// if route is link-to-link and has same start_link/end_link?
								if (leg.getRoute() != null) {
									Id<Link> start = leg.getRoute().getStartLinkId();
									Id<Link> end = leg.getRoute().getEndLinkId();
									if (start.equals(end)) {
										leg.setMode("walk");
										// remove the route or clear it
										leg.setRoute(null);
									}
								}
							} else if ("car_passenger".equals(mode)) {
								leg.setMode("private_AV");
								if (leg.getRoute() != null) {
									Id<Link> start = leg.getRoute().getStartLinkId();
									Id<Link> end = leg.getRoute().getEndLinkId();
									if (start.equals(end)) {
										leg.setMode("walk");
										leg.setRoute(null);
									}
								}
							}
							// else leave other modes unchanged
						} else {
							// non-motorized household: convert all car or car_passenger to walk
							if ("car".equals(mode) || "car_passenger".equals(mode)) {
								leg.setMode("walk");
								leg.setRoute(null);
							}
						}
					}
				}
			}
		}

		// --- Step 3: write out modified plans ---
		PopulationWriter writer = new PopulationWriter(population);
		writer.write(outputPlansFile);

		System.out.println("Wrote modified plans to " + outputPlansFile);
		
		// --- Step 2: Build DVRP fleet from householdHomeLink ---
        FleetSpecificationImpl fleet = new FleetSpecificationImpl();

        int i = 0;
        for (Map.Entry<Id<Household>, Id<Link>> entry : householdHomeLink.entrySet()) {
            Id<Household> hhId = entry.getKey();
            Id<Link> startLinkId = entry.getValue();

            Id<DvrpVehicle> vehId = Id.create("taxi_" + hhId.toString(), DvrpVehicle.class);

            DvrpVehicleSpecification spec = ImmutableDvrpVehicleSpecification.newBuilder()
                    .id(vehId)
                    .startLinkId(startLinkId)
                    .capacity(1)
                    .serviceBeginTime(0.0)
                    .serviceEndTime(24.0 * 3600.0)
                    .build();

            fleet.addVehicleSpecification(spec);
            i++;
        }
        
        // --- Step 3: Write as DVRP vehicle file ---
        new FleetWriter(fleet.getVehicleSpecifications().values().stream(), new IntegerLoadType("AV")).write(outputVehiclesFile);

        System.out.println("✅ Wrote " + i + " DVRP vehicles to: " + outputVehiclesFile);

		
	}
}


