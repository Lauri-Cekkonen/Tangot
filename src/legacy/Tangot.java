import java.util.List; // for the List used in TupleLike class and instances
import java.util.ArrayList; // also for the List used in TupleLike class and instances

import com.comsol.model.*;
import com.comsol.model.util.*;
import com.comsol.model.physics.MultiphysicsCoupling;
import com.comsol.model.physics.Physics;
import com.comsol.model.physics.PhysicsFeature;

/**
 * Tangot is a Java class that builds and solves a COMSOL model for 
 * coupled piezoelectric-electrostatic simulations using the COMSOL Java API.
 *
 * This model includes setup of geometry, materials, meshing, physics,
 * and simulation study for eigenfrequency analysis.
 *
 * <h3>Dependencies</h3>
 * <ul>
 *   <li>COMSOL Multiphysics Java API</li>
 *   <li>TupleLike class for returning multiple values</li>
 * </ul>
 */
public class Tangot {
    public static void main(String[] args) {
        try {
            run(args);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static Model run(String[] args) throws java.io.IOException  {
        // geometry parameters, all in nanonmeters:
        String beam_x = "50";
        String beam_y = "100";
        String beam_z = "100";
        String gapSize = "50";
        String surroundingVacuumThickness = "500";
        String thicknessOfPMLZone = "300";

        // mesh parameter in nanometers:
        String maximumElementSize = "20";

        // parameter for identifying boundaries, also in nanometers:
        String smallDistance = "5";

        // String whatIsComputed = "dispersion";
        boolean customMaterial = true;
        
        double numberOfEigenvalues = 3;

        // dispersion relation and parametric sweep parameters:
        /*double kxMinimum = 1e-9; // in units of 1/m, lower bound for parametric sweep
        double kxMaximum = Math.PI/Double.parseDouble(beam_x); // in units of 1/m, upper bound for parametric sweep
        int N = 3; // the amount of discrete kx points in the discretization
        String fileName1 = "dispersion_data_1.txt"; // possible file formats: .txt, .csv, .dat
        String fileName2 = "dispersion_data_2.txt"; // possible file formats: .txt, .csv, .dat*/

        // surface integral related parameters
        String kx1 = "5e6 [1/m]";
        String kx2 = "3932368.319941144 [1/m]";
        String fields1FileName = "fields_data_1.txt";
        String fields2FileName = "fields_data_2.txt";
        String integralsFileName = "integrations_front.csv";

        String[] parameters = {
            beam_x, 
            beam_y, 
            beam_z, 
            gapSize, 
            surroundingVacuumThickness, 
            thicknessOfPMLZone, 
            maximumElementSize, 
            smallDistance
        };

        String pathToFile = ".\\data";
        String separator = "\\";

        Model model = ModelUtil.create("Model");
        TupleLike modelAndGeometryArguments = setParameters(model, parameters);
        model = modelAndGeometryArguments.getModel();
        List<String[]> geometryArguments = modelAndGeometryArguments.getStringArrays();
        model = createCustomParameters(model);

        ModelNode component = model.component().create("comp1", true); // component is now equal to model.component("comp1")
        component = createGeometry(component, geometryArguments);
        component = IdentifyEntities.identifyBoundaryFacesAndDomains(component);

        if (customMaterial) {
            component = setCustomMaterials(component);
        }
        else {
            component = setMaterials(component);
        }

        component = selectPMLElements(component);
        component = createPhysics(component);
        component = Mesh.createMesh(component);

        TupleLike studyReturn = createStudy(model, numberOfEigenvalues);
        //model = performCustomParametricSweep(studyReturn.getModel(), studyReturn.getStudy(), studyReturn.getStudyFeature(), kxMinimum, kxMaximum, N, pathToFile, separator, fileName1);
        //model = createAndPerformParametricSweep(studyReturn.getModel(), studyReturn.getStudy(), studyReturn.getStudyFeature(), kxMinimum, kxMaximum, N, pathToFile, separator, fileName2);
        model = computeAndExportFieldsAndIntegrals(studyReturn.getModel(), studyReturn.getStudy(), studyReturn.getStudyFeature(), kx1, kx2, pathToFile, separator, fields1FileName, fields2FileName, integralsFileName);

        // to save a .mph file corresponding to the model created and solved in this java code, uncomment the following two lines
        String modelName = "modelCorrespondingToCurrentCode.mph";
        model.save(pathToFile+separator+modelName);

        return model;
    }

    /**
     * Sets all model parameters from the provided list and prepares geometry arguments.
     * <p>
     * About position of objects: note that in {@link #createGeometry(ModelNode, List)}
     * we set the center of the objects to origin
     *
     * @param model The COMSOL model to modify.
     * @param parameters An array of Strings representing model parameters.
     * @return TupleLike object containing the model and a list of geometry parameter arrays.
     */
    public static TupleLike setParameters(Model model, String[] parameters) {
        model.param().set("beam_x", parameters[0] + " [nm]");
        model.param().set("beam_y", parameters[1] + " [nm]");
        model.param().set("beam_z", parameters[2] + " [nm]");
        model.param().set("gap", parameters[3] + " [nm]");
        model.param().set("vac_x", "beam_x");
        model.param().set("vac_y", "2*" + parameters[4] + " [nm] + gap + 2*beam_y");
        model.param().set("vac_z", "2*" + parameters[4] + " [nm] + beam_z");
        model.param().set("kx", "3182810.8861448877 [1/m]");
        model.param().set("frequency", "2e9 [Hz]");
        model.param().set("small_distance", parameters[7] + " [nm]");
        model.param().set("PML", parameters[5] + " [nm]");
        model.param().set("mesh_max", parameters[6] + " [nm]");
        model.param().set("kx", "1 [1/m]");

        List<String[]> geometryArguments = new ArrayList<>();

        String[] sizeVacuum = new String[]{"vac_x", "vac_y+2*PML", "vac_z+2*PML"};
        geometryArguments.add(sizeVacuum);
        String[] positionVacuum = new String[]{"0", "0", "0"}; // see documentation
        geometryArguments.add(positionVacuum);

        String[] sizeBeamsPlusGap = new String[]{"beam_x", "2*beam_y + gap", "beam_z"};
        geometryArguments.add(sizeBeamsPlusGap);
        String[] positionBeams = new String[]{"0", "0", "0"}; // see documentation
        geometryArguments.add(positionBeams);

        String[] sizeGap = new String[]{"beam_x", "gap", "beam_z"};
        geometryArguments.add(sizeGap);
        String[] positionGap = new String[]{"0", "0", "0"}; // see documentation
        geometryArguments.add(positionGap);

        TupleLike returnValue = new TupleLike(model, geometryArguments);

        return returnValue;
    }

    public static Model createCustomParameters(Model model) {
        ModelParamGroup param2 = model.param().create("par2"); // param2 is now equal to model.param("par2")
        param2.label("Custom material parameters");
        param2.set("density", "5680.0");
        param2.descr("density", "");
        param2.set("eps11", "9.428317549544083");
        param2.descr("eps11", "");
        param2.set("eps12", "-0.0306715245465573");
        param2.descr("eps12", "");
        param2.set("eps13", "0.8227037726986098");
        param2.descr("eps13", "");
        param2.set("eps22", "8.551071077294361");
        param2.descr("eps22", "");
        param2.set("eps23", "-0.028729448806399455");
        param2.descr("eps23", "");
        param2.set("eps33", "9.320611386111363");
        param2.descr("eps33", "");
        param2.set("c11", "200251086280.71985");
        param2.descr("c11", "");
        param2.set("c12", "112586596642.55672");
        param2.descr("c12", "");
        param2.set("c13", "115184074854.26183");
        param2.descr("c13", "");
        param2.set("c14", "-96972330.69394074");
        param2.descr("c14", "");
        param2.set("c15", "952017673.7023336");
        param2.descr("c15", "");
        param2.set("c16", "-35492524.06116751");
        param2.descr("c16", "");
        param2.set("c22", "209674493074.3486");
        param2.descr("c22", "");
        param2.set("c23", "113629303080.71628");
        param2.descr("c23", "");
        param2.set("c24", "341857753.73371106");
        param2.descr("c24", "");
        param2.set("c25", "-7964618673.163111");
        param2.descr("c25", "");
        param2.set("c26", "364966921.4238069");
        param2.descr("c26", "");
        param2.set("c33", "200174471489.86212");
        param2.descr("c33", "");
        param2.set("c34", "12809026.737818267");
        param2.descr("c34", "");
        param2.set("c35", "-366802533.971801");
        param2.descr("c35", "");
        param2.set("c36", "-54360116.7118981");
        param2.descr("c36", "");
        param2.set("c44", "43456418466.37287");
        param2.descr("c44", "");
        param2.set("c45", "-317763451.63223654");
        param2.descr("c45", "");
        param2.set("c46", "-899338398.2482905");
        param2.descr("c46", "");
        param2.set("c55", "52544876607.11972");
        param2.descr("c55", "");
        param2.set("c56", "-343697354.56883514");
        param2.descr("c56", "");
        param2.set("c66", "43338679504.04221");
        param2.descr("c66", "");
        param2.set("e11", "-0.010439907664325173");
        param2.descr("e11", "");
        param2.set("e12", "-0.41670861611372684");
        param2.descr("e12", "");
        param2.set("e13", "0.5540986261079477");
        param2.descr("e13", "");
        param2.set("e14", "-0.03624339965916182");
        param2.descr("e14", "");
        param2.set("e15", "0.7098425961706647");
        param2.descr("e15", "");
        param2.set("e16", "-0.026463905156560462");
        param2.descr("e16", "");
        param2.set("e21", "-0.024094438724269106");
        param2.descr("e21", "");
        param2.set("e22", "0.039010793861862826");
        param2.descr("e22", "");
        param2.set("e23", "-0.01934955039800981");
        param2.descr("e23", "");
        param2.set("e24", "-0.32676692889793224");
        param2.descr("e24", "");
        param2.set("e25", "-0.036243399659161825");
        param2.descr("e25", "");
        param2.set("e26", "-0.3488559752132653");
        param2.descr("e26", "");
        param2.set("e31", "0.6462862845119531");
        param2.descr("e31", "");
        param2.set("e32", "-0.39032324055664364");
        param2.descr("e32", "");
        param2.set("e33", "-0.13705123504546196");
        param2.descr("e33", "");
        param2.set("e34", "-0.02171901683030117");
        param2.descr("e34", "");
        param2.set("e35", "0.6219512670084092");
        param2.descr("e35", "");
        param2.set("e36", "-0.03624339965916182");
        param2.descr("e36", "");
        return model;
    }

    /**
     * Creates the 3D geometry based on size and position arguments defined in {@link #setParameters(Model, String[])}
     *
     * @param component The COMSOL model component to operate on.
     * @param geometryArguments A list of size and position arrays for each geometry block.
     * @return The updated model component with geometry created.
     */
    public static ModelNode createGeometry(ModelNode component, List<String[]> geometryArguments) {
        GeomSequence g = component.geom().create("geom1", 3); // g is now equal to component.geom("geom1")
        g.geomRep("comsol"); // the alternative "cadps" requires a licence for certain actions
        
        GeomFeature vacuumBox = g.create("blk1", "Block"); // vacuumBox is now equal to g.feature("blk1")
        vacuumBox.label("vacuum"); // name
        vacuumBox.set("size", geometryArguments.get(0));
        vacuumBox.set("pos", geometryArguments.get(1));
        vacuumBox.set("base", "center"); // puts center to origin, other geometries need to have this too!
        vacuumBox.setIndex("layer", "PML", 0);
        vacuumBox.set("layerfront", true);
        vacuumBox.set("layerback", true);
        vacuumBox.set("layertop", true);
        vacuumBox.set("layerbottom", true);
        //g.run("blk1");

        GeomFeature beamBox = g.create("blk2", "Block"); // beamBox is now equal to g.feature("blk2")
        beamBox.label("beams"); // name
        beamBox.set("size", geometryArguments.get(2));
        beamBox.set("pos", geometryArguments.get(3));
        beamBox.set("base", "center");
        //g.run("blk2");

        // Relic from the time when the beams where defined separately:
        //g.feature().duplicate("blk3", "blk2");
        //g.feature("blk3").label("beam_2"); // name
        //g.feature("blk3").set("pos", new String[]{"0", "-(beam_y+gap)/2", "0"});
        //g.feature("blk3").set("base", "center");
        //g.run("blk3");

        GeomFeature gapBox = g.create("blk4", "Block"); // gapBox is now equal to g.feature("blk4")
        gapBox.label("gap"); // name
        gapBox.set("size", geometryArguments.get(4));
        gapBox.set("pos", geometryArguments.get(5));
        gapBox.set("base", "center");
        //g.run("blk4");

        g.run();
        return component;
    }

    public static ModelNode setMaterials(ModelNode component) {
        Material vacuumMaterial = component.material().create("mat1", "Common"); // vacuumMaterial is now equal to component.material("mat1")
        vacuumMaterial.label("Perfect vacuum");
        vacuumMaterial.propertyGroup("def").set("density", "");
        vacuumMaterial.propertyGroup("def").set("relpermeability", "");
        vacuumMaterial.propertyGroup("def").set("relpermittivity", "");
        vacuumMaterial.propertyGroup("def").set("electricconductivity", "");
        vacuumMaterial.propertyGroup("def").set("density", "0[kg/m^3]");
        vacuumMaterial.propertyGroup("def")
          .set("relpermeability", new String[]{"1", "0", "0", "0", "1", "0", "0", "0", "1"});
        vacuumMaterial.propertyGroup("def")
          .set("relpermittivity", new String[]{"1", "0", "0", "0", "1", "0", "0", "0", "1"});
        vacuumMaterial.propertyGroup("def")
          .set("electricconductivity", new String[]{"0[S/m]", "0", "0", "0", "0[S/m]", "0", "0", "0", "0[S/m]"});
        vacuumMaterial.selection().all(); // is also default, note that gets overriden in beams when a new material is selected for beams next

        Material beamsMaterial = component.material().create("mat2", "Common"); // beamsMaterial is now equal to component.material("mat2")
        beamsMaterial.propertyGroup().create("StrainCharge", "Strain-charge form");
        beamsMaterial.propertyGroup().create("StressCharge", "Stress-charge form");
        beamsMaterial.label("Zinc Oxide");
        beamsMaterial.set("family", "custom");
        beamsMaterial.set("customspecular", new double[]{0.7843137254901961, 1, 1});
        beamsMaterial.set("diffuse", "custom");
        beamsMaterial.set("customdiffuse", new double[]{0.7843137254901961, 0.7843137254901961, 0.7843137254901961});
        beamsMaterial.set("ambient", "custom");
        beamsMaterial.set("customambient", new double[]{0.7843137254901961, 0.7843137254901961, 0.7843137254901961});
        beamsMaterial.set("noise", true);
        beamsMaterial.set("fresnel", 0.9);
        beamsMaterial.set("roughness", 0.1);
        beamsMaterial.set("diffusewrap", 0);
        beamsMaterial.set("reflectance", 0);
        beamsMaterial.propertyGroup("def")
          .set("relpermittivity", new String[]{"8.5446", "0", "0", "0", "8.5446", "0", "0", "0", "10.204"});
        beamsMaterial.propertyGroup("def").set("density", "5680[kg/m^3]");
        beamsMaterial.propertyGroup("StrainCharge")
          .set("sE", new String[]{"7.86e-012[1/Pa]", "-3.43e-012[1/Pa]", "-2.21e-012[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "-3.43e-012[1/Pa]", "7.86e-012[1/Pa]", "-2.21e-012[1/Pa]", "0[1/Pa]",
                                "0[1/Pa]", "0[1/Pa]", "-2.21e-012[1/Pa]", "-2.21e-012[1/Pa]", "6.94e-012[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]",
                                "0[1/Pa]", "2.36e-011[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "2.36e-011[1/Pa]", "0[1/Pa]",
                                "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "0[1/Pa]", "2.26e-011[1/Pa]"});
        beamsMaterial.propertyGroup("StrainCharge")
          .set("dET", new String[]{"0[C/N]", "0[C/N]", "-5.43e-012[C/N]", "0[C/N]", "0[C/N]", "-5.43e-012[C/N]", "0[C/N]", "0[C/N]", "1.167e-011[C/N]", "0[C/N]",
                                "-1.134e-011[C/N]", "0[C/N]", "-1.134e-011[C/N]", "0[C/N]", "0[C/N]", "0[C/N]", "0[C/N]", "0[C/N]"});
        beamsMaterial.propertyGroup("StrainCharge")
          .set("epsilonrT", new String[]{"9.16", "0", "0", "0", "9.16", "0", "0", "0", "12.64"});
        beamsMaterial.propertyGroup("StressCharge")
          .set("cE", new String[]{"2.09714e+011[Pa]", "1.2114e+011[Pa]", "1.05359e+011[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "1.2114e+011[Pa]", "2.09714e+011[Pa]", "1.05359e+011[Pa]", "0[Pa]",
                                "0[Pa]", "0[Pa]", "1.05359e+011[Pa]", "1.05359e+011[Pa]", "2.11194e+011[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "0[Pa]",
                                "0[Pa]", "4.23729e+010[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "4.23729e+010[Pa]", "0[Pa]",
                                "0[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "0[Pa]", "4.42478e+010[Pa]"});
        beamsMaterial.propertyGroup("StressCharge")
          .set("eES", new String[]{"0[C/m^2]", "0[C/m^2]", "-0.567005[C/m^2]", "0[C/m^2]", "0[C/m^2]", "-0.567005[C/m^2]", "0[C/m^2]", "0[C/m^2]", "1.32044[C/m^2]", "0[C/m^2]",
                                "-0.480508[C/m^2]", "0[C/m^2]", "-0.480508[C/m^2]", "0[C/m^2]", "0[C/m^2]", "0[C/m^2]", "0[C/m^2]", "0[C/m^2]"});
        beamsMaterial.propertyGroup("StressCharge")
          .set("epsilonrS", new String[]{"8.5446", "0", "0", "0", "8.5446", "0", "0", "0", "10.204"});
        beamsMaterial.selection().named("beamsDomain");
        return component;
    }

    public static ModelNode setCustomMaterials(ModelNode component) {
        Material vacuumMaterial = component.material().create("mat3", "Common");
        vacuumMaterial.selection().all(); // is also default
        vacuumMaterial.label("Vacuum");
        vacuumMaterial.propertyGroup("def").set("relpermittivity", new String[]{"1"});

        Material beamsMaterial = component.material().create("mat4", "Common");
        beamsMaterial.selection().named("beamsDomain");
        beamsMaterial.label("Aluminium Nitride (rotated)");
        MaterialModel stressChargeTensors = beamsMaterial.propertyGroup().create("StressCharge", "Stress-charge_form");
        stressChargeTensors.set("cE", new String[]{"c11", "c12", "c22", "c13", "c23", "c33", "c14", "c24", "c34", "c44",
                                "c15", "c25", "c35", "c45", "c55", "c16", "c26", "c36", "c46", "c56",
                                "c66"});
        stressChargeTensors.set("eES", new String[]{"e11", "e21", "e31", "e12", "e22", "e32", "e13", "e23", "e33", "e14",
                                "e24", "e34", "e15", "e25", "e35", "e16", "e26", "e36"});
        stressChargeTensors.set("epsilonrS", new String[]{"eps11", "eps12", "eps22", "eps13", "eps23", "eps33"});
        beamsMaterial.propertyGroup("def").set("density", new String[]{"density"});
        return component;
    }

    /**
     * Creates and assigns the PML region coordinate system.
     * <p>
     * "PMLDomain" is defined in {@link IdentifyEntities#identifyPMLDomainAndFrontalFace(ModelNode)}.
     *
     * @param component The COMSOL model component.
     * @return Updated model component with PML coordinate system assigned.
     */
    public static ModelNode selectPMLElements(ModelNode component) {
        Coordsys c = component.coordSystem().create("pml1", "PML"); // c is now equal to component.coordSystem("pml1")
        c.selection().named("PMLDomain");
        c.set("wavelengthSourceType", "userDefined");
        c.set("typicalWavelength", "2*pi/kx");
        c.set("PMLfactor", "3");
        c.set("PMLgamma", "1");
        return component;
    }
    
    /**
     * Defines the physics interfaces (electrostatics and solid mechanics) and their coupling for the simulation.
     * <p>
     * "solid" is defined in {@link #createSolidMechanics(ModelNode)} and "es" in {@link #createElectrostatics(ModelNode)}.
     *
     * @param component The COMSOL model component.
     * @return Updated model component with physics features added.
     */
    public static ModelNode createPhysics(ModelNode component) {
        component = createElectrostatics(component);
        component = createSolidMechanics(component);

        MultiphysicsCoupling piezoEffect = component.multiphysics().create("pze1", "PiezoelectricEffect", 3); // piezoEffect is now equal to component.multiphysics("pze1")
        piezoEffect.set("Solid_physics", "solid");
        piezoEffect.set("Electrostatics_physics", "es");
        return component;
    }

    /**
     * Defines the electrostatics physics feature and adds to it piezoelectric charge conservation that applies for the beams.
     * <p>
     * Note that when creating the electrostatic physics ("Physics electro = ..."), three default nodes are created:
     * <ul>
     *     <li> Charge Conservation node (tag: "ccn1") which creates the time-independent PDE (poisson's equation) </li>
     *     <li> Zero Charge node (tag: "zc1") which is an approximation of saying that the charge is zero inside the domain 
     *         <ul>
     *             <li> More precisely: when the net (free) charge is zero in a region, the electric field (D-field) goes to zero further away from the charges.
     *                  Thus the electric field component normal to the surface surrounding the region goes to zero, and therefore far away from the charges
     *                  we have approximately n.D = 0. </li>
     *         </ul>
     *     </li>
     *     <li> Initial Values node (tag: init1) which sets the (real part of) electric potential to zero at initial time </li>
     * </ul>
     * With "electro.selection()..." one can choose which elements these default features apply to.
     * <p>
     * "beamsDomain"is defined in {@link IdentifyEntities#identifyBeamsDomainSourceAndDestinationFaces(ModelNode)} and
     * <p>
     * "sourceAndDestinationFaces" in {@link IdentifyEntities#identifySourceAndDestinationFaces(ModelNode)}.
     * 
     * @param component The COMSOL model component.
     * @return Updated model component with electrostatics features added.
     */
    public static ModelNode createElectrostatics(ModelNode component) {
        Physics electro = component.physics().create("es", "Electrostatics", "geom1"); // electro is now component.physics("es")
        electro.selection().all(); // (is also default) the electric potential vibrates everywhere (both in beams and vacuum)
        electro.prop("ShapeProperty").set("order_electricpotential", 2); // 2 corresponds to quadratic Lagrange elements (just Lagrange elements available), is also default
        electro.feature("ccn1").set("epsilonr_mat", "from_mat"); // set relative permittivity, is also default
        // Alternatively, you could set the relative permitivity with the following lines:
        //electro.feature("ccn1").set("epsilonr_mat", "userdef");
        //electro.feature("ccn1").set("epsilonr_mat", "userdef");

        PhysicsFeature chargeConservation = electro.create("ccnp1", "ChargeConservationPiezo"); // chargeConservation is now electro.feature("ccnp1")
        chargeConservation.selection().named("beamsDomain"); // just the beams are piezoelectric; this overrides the default charge conservation in beams
        chargeConservation.featureInfo("info").set("root.comp1.es.ccnp1.weak$1", new String[]{"es.weak*es.d", "4"}); // default weak expression

        PhysicsFeature electroBloch = electro.create("pc1", "PeriodicCondition", 2); // esBloch is now equal to electro.feature("pc1")
        electroBloch.selection().named("sourceAndDestinationFaces"); // this overrides boundary condition "Zero Charge" where they overlap
        electroBloch.set("PeriodicType", "Floquet");
        electroBloch.set("kFloquet", new String[]{"kx", "0", "0"});
        return component;
    }

    /**
     * Defines the solid mechanics physics feature and adds to it piezoelectric material model (everything is just applied for the beams).
     * <p>
     * Note that when creating solid mechanics ("Physics structural = ..."), three default nodes are created:
     * <ul>
     *     <li> Linear Elastic Material node (tag: "lemm1") which creates the time-independent PDE (vector field wave equation from linear continuum mechanics).
     *         <ul>
     *              <li> COMSOL also has Nonlinear Elastic Material node but that requires a separate license
     *                   (either Nonlinear Structural Materials Module or the Geomechanics Module). </li>
     *         </ul>
     *     </li>
     *     <li> Free node (tag: "free1") which let's the boundaries (that are not overriden later) to move freely </li>
     *     <li> Initial Values node (tag: "init1") which sets the initial displacement and velocity fields to zero. </li>
     * </ul>
     * With "structural.selection()..." one can choose which elements these default features apply to.
     * <p>
     * "beamsDomain" and "beamsSourceAndDestinationFaces" are defined in {@link IdentifyEntities#identifyBeamsDomainSourceAndDestinationFaces(ModelNode)}.
     * 
     * @param component The COMSOL model component.
     * @return Updated model component with solid mechanics features added.
     */
    public static ModelNode createSolidMechanics(ModelNode component) {
        Physics structural = component.physics().create("solid", "SolidMechanics", "geom1"); // structural is now component.physics("solid")
        structural.selection().named("beamsDomain"); // just the beams vibrate mechanically, not the vacuum around
        structural.prop("ShapeProperty").set("order_displacement", "2s"); // "2s" corresponds to quadratic serendipity elements, is also default
        structural.feature("init1").set("u", new int[]{0, 0, 0}); // initial displacement, also the default
        structural.feature("init1").set("ut", new int[]{0, 0, 0});  // initial velocity, also the default

        PhysicsFeature piezoMaterial = structural.create("pzm1", "PiezoelectricMaterialModel"); // piezoMaterial is now structural.feature("pzm1")
        piezoMaterial.selection().named("beamsDomain"); // just the beams are piezoelectric
        //piezoMaterial.selection().all(); // this would also work insted of the line above, because solid mechanics is set to apply just to the beams
        piezoMaterial.set("ConstitutiveRelation", "StressCharge"); // (is also the default) constitutive equations:
        // T = c:S - e.E
        // D = e:S - ε.E
        piezoMaterial.set("cE_mat", "from_mat"); // is also default
        piezoMaterial.set("eES_mat", "from_mat"); // is also default
        piezoMaterial.set("epsilonrS_mat", "from_mat"); // is also default
        piezoMaterial.set("rho_mat", "from_mat"); // is also default
        // Alternatively you can set the material parameters with the following (edited) lines:
        //piezoMaterial.set("cE_mat", "userdef");
        //piezoMaterial.set("cE", new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        //piezoMaterial.set("eES_mat", "userdef");
        //piezoMaterial.set("eES", new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        //piezoMaterial.set("epsilonrS_mat", "userdef");
        //piezoMaterial.set("epsilonrS", new int[]{1, 0, 0, 0, 1, 0, 0, 0, 1});
        //piezoMaterial.set("rho_mat", "userdef");
        //piezoMaterial.set("rho", 0);
        piezoMaterial.featureInfo("info").set("root.comp1.solid.pzm1.weak$1", new String[]{"-solid.Sl11*test(solid.el11)-2*solid.Sl12*test(solid.el12)-2*solid.Sl13*test(solid.el13)-solid.Sl22*test(solid.el22)-2*solid.Sl23*test(solid.el23)-solid.Sl33*test(solid.el33)", "4"});


        PhysicsFeature structuralBloch = structural.create("pc2", "PeriodicCondition", 2); // structuralBloch is now equal to structural.feature("pc2")
        structuralBloch.selection().named("beamsSourceAndDestinationFaces"); // this overrides boundary condition "Free" where they overlap
        structuralBloch.set("PeriodicType", "Floquet");
        structuralBloch.set("kFloquet", new String[]{"kx", "0", "0"});
        return component;
    }

    /**
     * Creates the simulation study for eigenfrequency analysis.
     * <p>
     * "/physics/es" is defined in {@link #createElectrostatics(ModelNode)},
     * <p>
     * "/physics/solid" in {@link #createSolidMechanics(ModelNode)} and
     * <p>
     * "/multiphysics/pze1" in {@link #createPhysics(ModelNode)}.
     *
     * @param model The COMSOL model to operate on.
     * @param numberOfEigenvalues Number of eigenfrequencies to compute.
     * @return The TupleLike object where
     * <ul>
     *      <li> Model is the model with the study configured. </li>
     *      <li> Study is the configured study. </li>
     *      <li> StudyFeature is the eigenfrequency study step. </li>
     * </ul>
     */
    public static TupleLike createStudy(Model model, double numberOfEigenvalues) {
        Study s = model.study().create("std1"); // s is now equal to model.study("std1")
        //s.label("Eigenfrequency (parametric sweep via java code)");
        //s.setGenPlots(false);
        //s.setGenConv(false);

        // Create eigenfrequency study that solves at single point numberOfEigenvalues amount of eigenfrequencies
        StudyFeature eigFreq = s.create("eig", "Eigenfrequency"); // eig is now equal to s.feature("eig")
        eigFreq.set("eigsolver", "arpack"); // ARPACK is originally written in FORTRAN
        eigFreq.set("chkeigregion", true);
        eigFreq.set("conrad", "1");
        eigFreq.set("conradynhm", "1");
        eigFreq.set("storefact", false);
        eigFreq.set("linpsolnum", "auto");
        eigFreq.set("solnum", "auto");
        eigFreq.set("notsolnum", "auto");
        eigFreq.set("outputmap", new String[]{});
        eigFreq.set("ngenAUX", "1");
        eigFreq.set("goalngenAUX", "1");
        eigFreq.set("ngenAUX", "1");
        eigFreq.set("goalngenAUX", "1");
        eigFreq.setSolveFor("/physics/es", true);
        eigFreq.setSolveFor("/physics/solid", true);
        eigFreq.setSolveFor("/multiphysics/pze1", true);
        eigFreq.set("neigs", numberOfEigenvalues);

        TupleLike studyReturn = new TupleLike(model, s, eigFreq);

        return studyReturn;
    }

    public static TupleLike createSolver(Model model, Study study, StudyFeature studyFeature, String tagForSolver, String labelForSolver) {
        String studyTag = study.tag();
        String studyFeatureTag = studyFeature.tag();
        
        SolverSequence solver = model.sol().create(tagForSolver); // solver is now equal to model.sol(tagForSolver)
        solver.study(studyTag);
        solver.label(labelForSolver);

        SolverFeature compileEquations = solver.create("st1", "StudyStep"); // compileEquations is now equal to solver.feature("st1")
        compileEquations.set("study", studyTag);
        compileEquations.set("studystep", studyFeatureTag);

        SolverFeature dependentVariables = solver.create("v1", "Variables"); // dependentVariables is now equal to solver.feature("v1")
        dependentVariables.set("control", studyFeatureTag);

        SolverFeature eigenvalueSolver = solver.create("e1", "Eigenvalue"); // eigenValues is now equal to solver.feature("e1")
        eigenvalueSolver.set("eigvfunscale", "maximum");
        eigenvalueSolver.set("eigvfunscaleparam", "5.36E-12");
        eigenvalueSolver.set("control", studyFeatureTag);
        eigenvalueSolver.feature("aDef").set("cachepattern", true);

        solver.attach(studyTag);

        TupleLike solverReturn = new TupleLike(model, solver);

        return solverReturn;
    }

    /**
     * Creates and configures solver. These nodes will appear below "Study 1 > Solver Configurations" in COMSOL GUI.
     * 
     * @param model The COMSOL model to operate on.
     * @return The model with the solution configured.
     */
    public static Model performCustomParametricSweep(Model model, Study study, StudyFeature studyFeature, double kxMin, double kxMax, int N, String pathToFolder, String separator, String fileName) {
        TupleLike modelAndSolver = createSolver(model, study, studyFeature, "eigsol1", "Last solution in code sweep");
        model = modelAndSolver.getModel();
        SolverSequence solver = modelAndSolver.getSolver();

        // Perform parametric sweep
        double kxCurrent = kxMin;
        double dkx = (kxMax - kxMin)/(N - 1);

        // initial step
        model.param().set("kx", String.valueOf(kxCurrent) + " [1/m]");
        solver.runAll(); // creates Solution 1 containing the results (tag: "dset1")

        DatasetFeature dataset = model.result().dataset().create("dsetlastsol1", "Solution");
        dataset.label("Parametric solution (sweep in code)");
        dataset.set("solution", solver.tag());

        TupleLike tableAndExportReturn = setUpTable(model, dataset, "1", pathToFolder, separator, fileName);
        NumericalFeature evaluation = tableAndExportReturn.getNumericalFeature();
        ExportFeature exportTable = tableAndExportReturn.getExportFeature();

        exportTable.set("header", false); // comment this line out if you want header after every step

        // Perform rest of the steps and update the table in each step
        for (int i = 1; i < N; i++) {
            kxCurrent = kxMin + i*dkx;
            model.param().set("kx", String.valueOf(kxCurrent) + " [1/m]");
            solver.runAll();

            // update table
            evaluation.set("table", "tbl1");
            evaluation.setResult(); // alternative "appendResult()" would put them column after column instead of row after row
            exportTable.run();
        }

        model.result().dataset().remove("dset1"); // remove default dataset
        
        return model;
    }

    public static Model createAndPerformParametricSweep(Model model, Study study, StudyFeature studyFeature, double kxMin, double kxMax, int N, String pathToFolder, String separator, String fileName) {
        TupleLike modelAndSolver = createSolver(model, study, studyFeature, "eigsol2", "Last solution in COMSOL sweep");
        model = modelAndSolver.getModel();
        SolverSequence solver = modelAndSolver.getSolver();
        
        double dkx = (kxMax - kxMin)/(N - 1);
        String parametricSweepRange = "range(" + String.valueOf(kxMin) + ", " + String.valueOf(dkx) + ", " + String.valueOf(kxMax) + ")";

        String studyTag = study.tag();
        StudyFeature paramSweep = model.study(studyTag).create("param", "Parametric"); // paramSweep is now equal to model.study(studyTag).feature("param")
        paramSweep.setIndex("pname", "kx", 0);
        paramSweep.setIndex("plistarr", parametricSweepRange, 0);

        BatchFeature batch = model.batch().create("p1", "Parametric"); // batch is now equal to model.batch("p1")
        batch.study(studyTag);

        BatchFeature batchSolSeq = batch.create("so1", "Solutionseq"); // batchSolSeq is now equal to batch.feature("so1")
        batchSolSeq.set("seq", solver.tag());
        batchSolSeq.set("store", "on");
        batchSolSeq.set("clear", "on");
        batchSolSeq.set("psol", "none");
        batch.set("pname", new String[]{"kx"});
        batch.set("plistarr", new String[]{parametricSweepRange});
        batch.set("sweeptype", "sparse");
        batch.set("probesel", "all");
        batch.set("probes", new String[]{});
        batch.set("plot", "off");
        batch.set("err", "on");
        batch.attach(studyTag);
        batch.set("control", paramSweep.tag());

        SolverSequence paramSolution = model.sol().create("paramsol2"); // paramSolution is now equal to model.sol("paramsol2")
        paramSolution.study(studyTag);
        paramSolution.label("COMSOL sweep solutions");
        batchSolSeq.set("psol", paramSolution.tag());
        batch.run("compute");

        DatasetFeature dataset = model.result().dataset().create("dsetparam2", "Solution");
        dataset.label("Parametric solution (sweep in COMSOL)");
        dataset.set("solution", paramSolution.tag()); // if you here instead had "solver.tag()", then just the values of the last kx point would get saved

        model.result().dataset().remove("dset1"); // remove default dataset
        model.result().dataset().remove("dset2"); // remove default dataset

        TupleLike tableAndExportReturn = setUpTable(model, dataset, "2", pathToFolder, separator, fileName);

        return tableAndExportReturn.getModel();
    }

    public static TupleLike setUpTable(Model model, DatasetFeature dataset, String numberForTag, String pathToFolder, String separator, String fileName) {
        NumericalFeature evaluation = model.result().numerical().create("gev" + numberForTag, "EvalGlobal"); // evaluation is now equal to model.result().numerical("gev1")
        evaluation.set("data", dataset.tag());
        evaluation.setIndex("looplevelinput", "all", 0); // all eigenvalues included instead of "first" or "last"
        evaluation.setIndex("expr", "kx", 0);
        evaluation.setIndex("descr", "Wave number kx", 0);
        evaluation.setIndex("expr", "real(freq)", 1);
        evaluation.setIndex("descr", "Real part of eigenfrequency", 1);

        TableFeature table = model.result().table().create("tbl" + numberForTag, "Table"); // table is now equal to model.result().table("tbl1")
        table.comments("Global Evaluation 1");

        evaluation.set("table", "tbl1");
        evaluation.setResult();

        ExportFeature exportTable = model.result().export().create("export_tbl" + numberForTag, "Table"); // exportTable is now equal to model.result().export("export_tbl1")
        exportTable.set("source", "table");
        exportTable.set("table", "tbl1");
        exportTable.set("header", true);
        exportTable.set("filename", pathToFolder + separator + fileName);
        exportTable.set("ifexists", "append");
        exportTable.run();
        
        TupleLike tableAndExportReturn = new TupleLike(model, evaluation, exportTable);
        
        return tableAndExportReturn;
    }

    public static Model computeAndExportFieldsAndIntegrals(Model model, Study study, StudyFeature studyFeature, String kx1, String kx2, String pathToFile, String separator, String fileName1, String fileName2, String fileName3) {
        // solve at kx1
        TupleLike modelAndSolver1 = createSolver(model, study, studyFeature, "kx1solver", "Solutions at kx1 to be integrated or exported as fields");
        model = modelAndSolver1.getModel();
        SolverSequence solver1 = modelAndSolver1.getSolver();

        model.param().set("kx", kx1);
        solver1.runAll(); // creates a dataset containing the results (tag: "dset1")...

        DatasetFeature dataset1 = model.result().dataset().create("dsetkx1", "Solution"); // ...but we create a new dataset to store the results
        dataset1.label("Solution at kx1");
        dataset1.set("solution", solver1.tag());

        // solve at kx2
        TupleLike modelAndSolver2 = createSolver(model, study, studyFeature, "kx2solver", "Solutions at kx2 to be integrated or exported as fields");
        model = modelAndSolver2.getModel();
        SolverSequence solver2 = modelAndSolver2.getSolver();

        model.param().set("kx", kx2);
        solver2.runAll(); // creates a dataset containing the results (tag: "dset2")...

        DatasetFeature dataset2 = model.result().dataset().create("dsetkx2", "Solution"); // ...but we create a new dataset to store the results
        dataset2.label("Solution at kx2");
        dataset2.set("solution", solver2.tag());

        exportFields(model, dataset1, "1", 2,  pathToFile, separator, fileName1);
        exportFields(model, dataset2, "2", 2,  pathToFile, separator, fileName2);

        model = performIntegration(model, dataset1.tag(), dataset2.tag(), pathToFile, separator, fileName3);

        model.result().dataset().remove("dset1"); // remove default dataset
        model.result().dataset().remove("dset2"); // remove default dataset

        return model;
    }

    public static Model exportFields(Model model, DatasetFeature dataset, String numberForTag, int whichEigenfrequency, String pathToFile, String separator, String fileName) {
        ExportFeature fieldsExport = model.result().export().create("fieldsExport" + numberForTag, "Data"); // fieldsExport is now equal to model.result().export("fieldsExportX")
        fieldsExport.set("data", dataset.tag());
        fieldsExport.setIndex("looplevelinput", "manual", 0); // alternatively you can select all frequencies by writing "all" instead of "manual"
        fieldsExport.setIndex("looplevel", new int[]{whichEigenfrequency}, 0); // instead of whichEigenfrequency you can also have 1, 2, 3, etc.
        //fieldsExport.setIndex("expr", "kx", 0); // example for how to insert one value to one row
        fieldsExport.set("expr", new String[]{"u", "v", "w", "V", "solid.SXX", "solid.SYY", "solid.SZZ", "solid.SYZ", "solid.SXZ", "solid.SXY", 
            "es.DX", "es.DY", "es.DZ", "kx", "freq", "solid.el11", "solid.el22", "solid.el33", "solid.el23", "solid.el13", 
            "solid.el12", "es.Ex", "es.Ey", "es.Ez"});
        fieldsExport.set("unit", new String[]{"m", "m", "m", "V", "N/m^2", "N/m^2", "N/m^2", "N/m^2", "N/m^2", "N/m^2", 
            "C/m^2", "C/m^2", "C/m^2", "1/m", "Hz", "1", "1", "1", "1", "1", 
            "1", "V/m", "V/m", "V/m"});
        fieldsExport.set("descr", new String[]{"Displacement field, X-component", "Displacement field, Y-component", "Displacement field, Z-component", "Electric potential", "Second Piola\u2013Kirchhoff stress, XX-component", "Second Piola\u2013Kirchhoff stress, YY-component", "Second Piola\u2013Kirchhoff stress, ZZ-component", "Second Piola\u2013Kirchhoff stress, YZ-component", "Second Piola\u2013Kirchhoff stress, XZ-component", "Second Piola\u2013Kirchhoff stress, XY-component", 
            "Electric displacement field, X-component", "Electric displacement field, Y-component", "Electric displacement field, Z-component", "", "Frequency", "Strain tensor, local coordinate system, 11-component", "Strain tensor, local coordinate system, 22-component", "Strain tensor, local coordinate system, 33-component", "Strain tensor, local coordinate system, 23-component", "Strain tensor, local coordinate system, 13-component", 
            "Strain tensor, local coordinate system, 12-component", "Electric field, x-component", "Electric field, y-component", "Electric field, z-component"});
        fieldsExport.set("filename", pathToFile + separator + fileName);
        fieldsExport.set("ifexists", "overwrite"); // is also default
        fieldsExport.set("pattern", "lagrange"); // where the points are evaluated, alternative is "gauss" for Gauss points
        fieldsExport.set("smooth", "internal");
        //fieldsExport.run(); // uncomment this line if you want to get the export saved to the file
        return model;
    }

    public static Model performIntegration(Model model, String kx1DataTag, String kx2DataTag, String pathToFile, String separator, String fileName) {
        TupleLike modelAndSurface1 = setUpSurfaceWithData(model, "surf1", "Beam 1 front, kx1", kx1DataTag, "beamFace1");
        TupleLike modelAndSurface2 = setUpSurfaceWithData(modelAndSurface1.getModel(), "surf2", "Beam 1 front, kx2", kx2DataTag, "beamFace1");
        TupleLike modelAndSurface3 = setUpSurfaceWithData(modelAndSurface2.getModel(), "surf3", "Beam 2 front, kx1", kx1DataTag, "beamFace2");
        TupleLike modelAndSurface4 = setUpSurfaceWithData(modelAndSurface3.getModel(), "surf4", "Beam 2 front, kx2", kx2DataTag, "beamFace2");
        TupleLike modelAndSurface5 = setUpSurfaceWithData(modelAndSurface4.getModel(), "surf5", "Beam 1 back, kx1", kx1DataTag, "beamFace3");
        TupleLike modelAndSurface6 = setUpSurfaceWithData(modelAndSurface5.getModel(), "surf6", "Beam 1 back, kx2", kx2DataTag, "beamFace3");
        TupleLike modelAndSurface7 = setUpSurfaceWithData(modelAndSurface6.getModel(), "surf7", "Beam 2 back, kx1", kx1DataTag, "beamFace4");
        TupleLike modelAndSurface8 = setUpSurfaceWithData(modelAndSurface7.getModel(), "surf8", "Beam 2 back, kx2", kx2DataTag, "beamFace4");
        
        TupleLike modelAndJoin1 = joinSurfacesWithData(modelAndSurface8.getModel(), "join1", "Beam 1 front", modelAndSurface1.getDatasetFeature().tag(), modelAndSurface2.getDatasetFeature().tag(), 1, 1);
        TupleLike modelAndJoin2 = joinSurfacesWithData(modelAndJoin1.getModel(), "join2", "Beam 2 front", modelAndSurface3.getDatasetFeature().tag(), modelAndSurface4.getDatasetFeature().tag(), 1, 1);
        TupleLike modelAndJoin3 = joinSurfacesWithData(modelAndJoin2.getModel(), "join3", "Beam 1 back", modelAndSurface5.getDatasetFeature().tag(), modelAndSurface6.getDatasetFeature().tag(), 1, 1);
        TupleLike modelAndJoin4 = joinSurfacesWithData(modelAndJoin3.getModel(), "join4", "Beam 2 back", modelAndSurface7.getDatasetFeature().tag(), modelAndSurface8.getDatasetFeature().tag(), 1, 1);
        model = modelAndJoin4.getModel();

        model.result().param().set("omega", "2*pi*frequency");
        model.result().param().set("c1", "0.26");
        model.result().param().set("c2", "-0.135");

        EvaluationGroupFeature integration = model.result().evaluationGroup().create("eg1", "EvaluationGroup");
        integration.label("Integration");

        EvaluationFeature integrationOverBeam1Front = integration.create("int1", "IntSurface");
        integrationOverBeam1Front.set("intvolume", true);
        integrationOverBeam1Front.set("data", modelAndJoin1.getDatasetFeature().tag());
        integrationOverBeam1Front.setIndex("expr", "0.5*(-data1(solid.SX)*conj(data1(solid.u_tX))-data1(solid.SXY)*conj(data1(solid.u_tY))-data1(solid.SXZ)*conj(data1(solid.u_tZ)))+0.5*data1(V)*conj(data1(es.iomega*es.DX))", 0);
        integrationOverBeam1Front.setIndex("descr", "P11", 0);
        integrationOverBeam1Front.setIndex("expr", "0.5*(-data2(solid.SX)*conj(data2(solid.u_tX))-data2(solid.SXY)*conj(data2(solid.u_tY))-data2(solid.SXZ)*conj(data2(solid.u_tZ)))+0.5*data2(V)*conj(data2(es.iomega*es.DX))", 1);
        integrationOverBeam1Front.setIndex("descr", "P12", 1);
        integrationOverBeam1Front.setIndex("expr", "0.5*(-data1(solid.SX)*conj(data2(solid.u_tX))-data1(solid.SXY)*conj(data2(solid.u_tY))-data1(solid.SXZ)*conj(data2(solid.u_tZ)))+0.5*data1(V)*conj(data2(es.iomega*es.DX))", 2);
        integrationOverBeam1Front.setIndex("descr", "P1112", 2);
        integrationOverBeam1Front.setIndex("expr", "0.5*(-data2(solid.SX)*conj(data1(solid.u_tX))-data2(solid.SXY)*conj(data1(solid.u_tY))-data2(solid.SXZ)*conj(data1(solid.u_tZ)))+0.5*data2(V)*conj(data1(es.iomega*es.DX))", 3);
        integrationOverBeam1Front.setIndex("descr", "P1211", 3);

        EvaluationFeature integrationOverBeam2Front = integration.create("int2", "IntSurface");
        integrationOverBeam2Front.set("intvolume", true);
        integrationOverBeam2Front.set("data", "join2");
        integrationOverBeam2Front.setIndex("expr", "0.5*(-data1(solid.SX)*conj(data1(solid.u_tX))-data1(solid.SXY)*conj(data1(solid.u_tY))-data1(solid.SXZ)*conj(data1(solid.u_tZ)))+0.5*data1(V)*conj(data1(es.iomega*es.DX))", 0);
        integrationOverBeam2Front.setIndex("descr", "P21", 0);
        integrationOverBeam2Front.setIndex("expr", "0.5*(-data2(solid.SX)*conj(data2(solid.u_tX))-data2(solid.SXY)*conj(data2(solid.u_tY))-data2(solid.SXZ)*conj(data2(solid.u_tZ)))+0.5*data2(V)*conj(data2(es.iomega*es.DX))", 1);
        integrationOverBeam2Front.setIndex("descr", "P22", 1);
        integrationOverBeam2Front.setIndex("expr", "0.5*(-data1(solid.SX)*conj(data2(solid.u_tX))-data1(solid.SXY)*conj(data2(solid.u_tY))-data1(solid.SXZ)*conj(data2(solid.u_tZ)))+0.5*data1(V)*conj(data2(es.iomega*es.DX))", 2);
        integrationOverBeam2Front.setIndex("descr", "P2122", 2);
        integrationOverBeam2Front.setIndex("expr", "0.5*(-data2(solid.SX)*conj(data1(solid.u_tX))-data2(solid.SXY)*conj(data1(solid.u_tY))-data2(solid.SXZ)*conj(data1(solid.u_tZ)))+0.5*data2(V)*conj(data1(es.iomega*es.DX))", 3);
        integrationOverBeam2Front.setIndex("descr", "P2221", 3);

        integration.run();

        TableFeature table = model.result().table().create("intTable", "Table");
        integration.copyToTable(table.tag());
        ExportFeature exportTable = model.result().export().create("tbl1", "Table");
        exportTable.set("filename", pathToFile + separator + fileName);
        exportTable.set("ifexists", "overwrite"); // is also default
        exportTable.run();

        return model;
    }

    public static TupleLike setUpSurfaceWithData(Model model, String surfaceTag, String labelForSurface, String whichDataTag, String whichSurfaceTag) {
        DatasetFeature surface = model.result().dataset().create(surfaceTag, "Surface");
        surface.label(labelForSurface);
        surface.set("data", whichDataTag);
        surface.selection().named(whichSurfaceTag);

        TupleLike modelAndSurfaceWithData = new TupleLike(model, surface);
        return modelAndSurfaceWithData;
    }

    /**
     * The idea is that the eigenfrequency corresponding to whichEigenfreq1 would have the same numerical value
     * as the eigenfrequency corresponding to whichEigenfreq2.
     * @param model
     * @param joinTag
     * @param labelForJoin
     * @param surf1Tag
     * @param surf2Tag
     * @param whichEigenfreq1 must be 1, 2, 3, ...
     * @param whichEigenfreq2 must be 1, 2, 3, ...
     * @return
     */
    public static TupleLike joinSurfacesWithData(Model model, String joinTag, String labelForJoin, String surf1Tag, String surf2Tag, int whichEigenfreq1, int whichEigenfreq2) {
        DatasetFeature join = model.result().dataset().create(joinTag, "Join");
        join.label(labelForJoin);
        // set first data to be joined
        join.set("data", surf1Tag);
        join.set("solutions", "one"); // we select just one eigenfrequency to the join
        join.set("solnum", whichEigenfreq1); // this is the selected eigenfrequency

        // set second data to be joined
        join.set("data2", surf2Tag);
        join.set("solutions2", "one"); // we select just one eigenfrequency to the join
        join.set("solnum2", whichEigenfreq2); // this is the selected eigenfrequency

        join.set("method", "explicit"); // this means that the precises formula must be stated explicitly with data1(...) and data2(...)
        
        TupleLike modelAndJoin = new TupleLike(model, join);
        return modelAndJoin;
    }

    /**
     * Exports simulation results to a text file.
     *
     * @param model The computed model with results.
     * @param pathToFileName Full file path to export results to (including name and .txt extension).
     * @return The model after export operation.
     */
    public static Model saveDataToTXTFile(Model model, String pathToFileName) {
        ExportFeature table = model.result().export().create("tbl1", "Table"); // table is now equal to model.result().export("tbl1")
        table.set("source", "evaluationgroup");
        table.set("filename", pathToFileName);
        table.run();
        return model;
    }
}