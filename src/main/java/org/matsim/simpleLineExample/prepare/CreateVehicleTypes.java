package org.matsim.simpleLineExample.prepare;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.vehicles.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CreateVehicleTypes {
    private static final Logger log = Logger.getLogger(CreateVehicleTypes.class);
    private static final String VEH_FILE_NAME = "vehicles.xml";

    public static void main(String[] args) {
        CreateVehicleTypes.Input input = new CreateVehicleTypes.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Output directory: " + input.outputDir);

        writeVehiclesFile(create(), Paths.get(input.outputDir));

    }

    public static Vehicles create(){
        Vehicles veh = VehicleUtils.createVehiclesContainer();
        VehiclesFactory fac = VehicleUtils.getFactory();

        // pce = 0.125 => 8 people in a 7.5 x 3.5 rectangular
        veh.addVehicleType(createVehicleType(TransportMode.car, 1.875, 1.6, 0.125, fac));
        return veh;
    }

    public static VehicleType createVehicleType(String id, double length, double maxV, double pce, VehiclesFactory factory) {
        var vehicleType = factory.createVehicleType(Id.create(id, VehicleType.class));
        vehicleType.setNetworkMode(id);
        vehicleType.setPcuEquivalents(pce);
        vehicleType.setLength(length);
        vehicleType.setMaximumVelocity(maxV);
        vehicleType.setWidth(1.0);
        return vehicleType;

    }

    public static void writeVehiclesFile(Vehicles veh, Path path) {
        log.info("Writing vehicles to " + path.resolve(VEH_FILE_NAME));
        new MatsimVehicleWriter(veh).writeFile(path.resolve(VEH_FILE_NAME).toString());

        log.info("");
        log.info("Finished \uD83C\uDF89");
    }


    private static class Input {

        @Parameter(names = "-outputDir")
        private String outputDir;

    }

}
