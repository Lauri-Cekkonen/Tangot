import com.comsol.model.*;

class IdentifyEntities {
    /**
     * Identifies all domains and frontal x-faces and creates selections out of them
     * (used later in {@link Tangot} for material, physics and meshing).
     *
     * @param component The COMSOL model component.
     * @return Updated model component with domain and face selections.
     */
    public static ModelNode identifyBoundaryFacesAndDomains(ModelNode component) {
        component = identifySourceAndDestinationFaces(component);
        component = identifyPMLDomainAndFrontalFace(component);
        component = identifyVacuumDomainAndFrontalFace(component);
        component = identifyBeamsDomainSourceAndDestinationFaces(component);
        component = identifyGapDomainAndFace(component);
        return component;
    }

    /**
     * Identifies source and destination boundary faces of the full geometry.
     * 
     * @param component The COMSOL model component.
     * @return Updated model component with domain and face selections.
     */
    public static ModelNode identifySourceAndDestinationFaces(ModelNode component) {
        String[] xLim1 = {"-Inf", "0"};
        String[] yLim = {"-Inf", "Inf"};
        String[] zLim = yLim;
        component = createBoxSelection(component, "sourceFace", 2, "inside", xLim1, yLim, zLim);
        component.selection("sourceFace").label("source face of entire geometry");
        
        String[] xLim2 = {"0", "Inf"};
        component = createBoxSelection(component, "destinationFace", 2, "inside", xLim2, yLim, zLim);
        component.selection("destinationFace").label("destination face of entire geometry");

        SelectionFeature union = component.selection().create("sourceAndDestinationFaces", "Union"); // union is now equal to component.selection("sourceAndDestinationFaces")
        union.label("source and destination faces of entire geometry");
        union.set("entitydim", 2);
        union.set("input", new String[]{"sourceFace", "destinationFace"});
        return component;
    }

    /**
     * Identifies the domains and frontal x-faces that represent PML (Perfectly Matched Layer) regions.
     *
     * @param component The COMSOL model component.
     * @return Updated ModelNode with PML domain and face selection.
     */
    public static ModelNode identifyPMLDomainAndFrontalFace(ModelNode component) {
        // Identify the frontal x-face of PML
        String[] xLimFace = {"-Inf", "0"};
        String[] yLim1 = {"vac_y/2 - small_distance", "Inf"};
        String[] zLim1 = {"-Inf", "Inf"};
        component = createBoxSelection(component, "PMLFace1", 2, "inside", xLimFace, yLim1, zLim1);

        String[] yLim2 = {"-Inf", "-vac_y/2 + small_distance"};
        String[] zLim2 = {"-Inf", "Inf"};
        component = createBoxSelection(component, "PMLFace2", 2, "inside", xLimFace, yLim2, zLim2);

        String[] yLim3 = {"-Inf", "Inf"};
        String[] zLim3 = {"vac_z/2 - small_distance", "Inf"};
        component = createBoxSelection(component, "PMLFace3", 2, "inside", xLimFace, yLim3, zLim3);

        String[] yLim4 = {"-Inf", "Inf"};
        String[] zLim4 = {"-Inf", "-vac_z/2 + small_distance"};
        component = createBoxSelection(component, "PMLFace4", 2, "inside", xLimFace, yLim4, zLim4);

        SelectionFeature unionFace = component.selection().create("PMLFace", "Union"); // unionFace is now equal to component.selection("PMLFace")
        unionFace.label("PML front face");
        unionFace.set("entitydim", 2);
        unionFace.set("input", new String[]{"PMLFace1", "PMLFace2", "PMLFace3", "PMLFace4"});

        // Identify the entire PML
        String[] xLimDomain = {"-Inf", "Inf"};
        component = createBoxSelection(component, "PMLDomain1", 3, "inside", xLimDomain, yLim1, zLim1);
        component = createBoxSelection(component, "PMLDomain2", 3, "inside", xLimDomain, yLim2, zLim2);
        component = createBoxSelection(component, "PMLDomain3", 3, "inside", xLimDomain, yLim3, zLim3);
        component = createBoxSelection(component, "PMLDomain4", 3, "inside", xLimDomain, yLim4, zLim4);
        
        SelectionFeature unionDomain = component.selection().create("PMLDomain", "Union"); // unionDomain is now equal to component.selection("PMLDomain")
        unionDomain.label("PML domain");
        unionDomain.set("entitydim", 3);
        unionDomain.set("input", new String[]{"PMLDomain1", "PMLDomain2", "PMLDomain3", "PMLDomain4"});
        return component;
    }

    /**
     * Identifies the domain and frontal x-face of the vacuum region (non-PML and non-gap).
     *
     * @param component The COMSOL model component.
     * @return Updated model component with vacuum domain and face selection.
     */
    public static ModelNode identifyVacuumDomainAndFrontalFace(ModelNode component) {
        String[] xLimFace = {"-Inf", "0"};
        String[] yLim = {"-small_distance", "small_distance"};
        String[] zLim = {"beam_z/2 + small_distance", "vac_z/2 - small_distance"};
        component = createBoxSelection(component, "vacuumFace", 2, "intersects", xLimFace, yLim, zLim);
        component.selection("vacuumFace").label("vacuum face");

        String[] xLimDomain = {"-Inf", "Inf"};
        component = createBoxSelection(component, "vacuumDomain", 3, "intersects", xLimDomain, yLim, zLim);
        component.selection("vacuumFace").label("vacuum domain");
        return component;
    }

    /**
     * Identifies the domain and frontal x-face of the source and destination faces of the beams.
     *
     * @param component The COMSOL model component.
     * @return Updated ModelNode with beam domain and face selections.
     */
    public static ModelNode identifyBeamsDomainSourceAndDestinationFaces(ModelNode component) {
        String[] xLimSourceFace = {"-Inf", "0"};
        String[] yLimBeam1 = {"gap/2+small_distance", "gap/2 + beam_y - small_distance"};
        String[] zLim = {"-beam_z/2 + small_distance", "beam_z/2 - small_distance"};
        component = createBoxSelection(component, "beamFace1", 2, "intersects", xLimSourceFace, yLimBeam1, zLim);

        String[] yLimBeam2 = {"-(gap/2 + beam_y - small_distance)", "-gap/2 - small_distance"};
        component = createBoxSelection(component, "beamFace2", 2, "intersects", xLimSourceFace, yLimBeam2, zLim);

        SelectionFeature unionSourceFace = component.selection().create("beamsSourceFace", "Union"); // unionSourceFace is now equal to component.selection("beamsSourceFace")
        unionSourceFace.label("source face of beams");
        unionSourceFace.set("entitydim",2);
        unionSourceFace.set("input", new String[]{"beamFace1", "beamFace2"});

        String[] xLimDestinationFace = {"0", "Inf"};
        component = createBoxSelection(component, "beamFace3", 2, "intersects", xLimDestinationFace, yLimBeam1, zLim);
        component = createBoxSelection(component, "beamFace4", 2, "intersects", xLimDestinationFace, yLimBeam2, zLim);

        SelectionFeature unionDestinationFace = component.selection().create("beamsDestinationFace", "Union"); // unionDestinationFace is now equal to component.selection("beamsDestinationFace")
        unionDestinationFace.label("destination face of beams");
        unionDestinationFace.set("entitydim",2);
        unionDestinationFace.set("input", new String[]{"beamFace3", "beamFace4"});

        SelectionFeature unionFaces = component.selection().create("beamsSourceAndDestinationFaces", "Union"); // unionFaces is now equal to component.selection("beamsSourceAndDestinationFaces")
        unionFaces.label("source and destination face of beams");
        unionFaces.set("entitydim",2);
        unionFaces.set("input", new String[]{"beamsSourceFace", "beamsDestinationFace"});

        String[] xLimDomain = {"-Inf", "Inf"};
        component = createBoxSelection(component, "beamDomain1", 3, "intersects", xLimDomain, yLimBeam1, zLim);
        component = createBoxSelection(component, "beamDomain2", 3, "intersects", xLimDomain, yLimBeam2, zLim);

        SelectionFeature unionDomain = component.selection().create("beamsDomain", "Union"); // unionDomain is now equal to component.selecction("beamsDomain")
        unionDomain.label("beams domain");
        unionDomain.set("entitydim", 3);
        unionDomain.set("input", new String[]{"beamDomain1", "beamDomain2"});
        return component;
    }

    /**
     * Identifies the domain and frontal x-face of the gap between beams.
     *
     * @param component The COMSOL model component.
     * @return Updated ModelNode with gap domain and face selection.
     */
    public static ModelNode identifyGapDomainAndFace(ModelNode component) {
        String[] xLimFace = {"-Inf", "0"};
        String[] yLim = {"-small_distance", "small_distance"};
        String[] zLim = yLim;
        component = createBoxSelection(component, "gapFace", 2, "intersects", xLimFace, yLim, zLim);
        component.selection("gapFace").label("gap front face");

        String[] xLimDomain = {"-Inf", "Inf"};
        component = createBoxSelection(component, "gapDomain", 3, "intersects", xLimDomain, yLim, zLim);
        component.selection("gapDomain").label("gap domain");
        return component;
    }

    /**
     * Creates a box selection in the model geometry for a given tag and limits.
     *
     * @param component The COMSOL model component.
     * @param tag A unique name for the selection.
     * @param N Entity dimension for identified entities (2 = face, 3 = volume).
     * @param selectionType Either "inside" or "intersects".
     * @param xLim X-limits of the box.
     * @param yLim Y-limits of the box.
     * @param zLim Z-limits of the box.
     * @return Updated model component with the created box selection.
     */
    public static ModelNode createBoxSelection(ModelNode component, String tag, int N, String selectionType, String[] xLim, String[] yLim, String[] zLim) {
        SelectionFeature box = component.selection().create(tag, "Box"); // box is now equal to component.selection(tag)
        box.set("entitydim", N); // we select N dimensional entities...
        box.set("condition", selectionType); // ...that are entirely inside or intersecting the box
        box.set("xmin", xLim[0]);
        box.set("xmax", xLim[1]);
        box.set("ymin", yLim[0]);
        box.set("ymax", yLim[1]);
        box.set("zmin", zLim[0]);
        box.set("zmax", zLim[1]);
        return component;
    }
}
