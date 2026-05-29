import com.comsol.model.*;

/**
 * The discretization of physical space into mesh for numerical calculations is done
 * inside the {@link Mesh} class.
 */
class Mesh {
    /**
     * Creates the mesh sequence, including custom meshes for beams, gap, vacuum, and PML.
     * The created mesh is used in {@link Tangot}.
     * <p>
     * Note that when creating a custom mesh ("m.automatic(false);"), a deliberate number
     * of mesh nodes are created under "Mesh 1". The exact number and type of mesh nodes
     * depends on the settings (like geometry) defined before creating the mesh. Therefore
     * when one changes the said settings (like geometry), there is different selection
     * of mesh nodes created. The mesh nodes can be any of the ones in the following list:
     * <ul>
     *     <li> Size (tag: "size") </li>
     *     <li> Identical Mesh (tag: "id1", "id2", ...) </li>
     *     <li> Free Tetrahedral (tag: "ftet1", "ftet2", ...) </li>
     *     <li> Mapped (tag: "map1", "map2", ...) </li>
     *     <li> Distribution (tag: "dis1", "dis2", ...) </li>
     *     <li> Copy (tag: "copy1", "copy2", ...) </li>
     *     <li> Swept (tag: "swe1", "swe2", ...) </li>
     * </ul>
     * It is unpredictable which selection of mesh nodes is created after each change. 
     * Therefore just the Size node from the predefined selection is used and applied
     * only newly created custom mesh nodes to the geometry 
     * (see {@link #createGapMesh(MeshSequence)},
     * {@link #createBeamMesh(MeshSequence)}, {@link #createVacuumMesh(MeshSequence)}
     * and {@link #createPMLMesh(MeshSequence, String)}). The predefined and unused
     * mesh nodes I decided to delete which is however not mandatory (I just wanted
     * to leave only the used nodes visible). Therefore the method responsible for 
     * deleting unused nodes, {@link #removePredefinedMeshNodes(MeshSequence)},
     * can be commented out and the code still works.
     *
     * @param component The COMSOL model component.
     * @return Updated model component with full meshing applied.
     */
    public static ModelNode createMesh(ModelNode component) {
        MeshSequence m = component.mesh().create("mesh1"); // m is now equal to component.mesh("mesh1")
        m.automatic(false); // makes the mesh user-controlled

        m = removePredefinedMeshNodes(m); // this line can be commented out, see documentation
        m = adjustGlobalSize(m, 2);
        m = createGapMesh(m);
        m = createBeamMesh(m);
        m = createVacuumMesh(m);
        m = createPMLMesh(m, "rectangle");
        
        // So far we have just created 2D mesh on the front of the geometry. 
        // Let's next sweep the 2D mesh over the entire geometry to create a 3D mesh.
        m = sweepMeshOver3D(m);

        m.run();

        return component;
    }

    /**
     * Removes predefined mesh nodes that are not needed for the custom mesh.
     * <p>
     * <h3>Note!</h3>
     * If at running this code you get something like "Unknown feature. - Tag: tagX",
     * you need to comment out the line m.feature().remove(tagX). If instead you get
     * something like "A problem occurred when building mesh feature 'Name X'.", you
     * need to uncomment the line m.feature().remove(tag) where tag is something like
     * namX.
     * 
     * @param m The MeshSequence object from the COMSOL model.
     * @return The updated MeshSequence with unnecessary mesh features removed.
     */
    public static MeshSequence removePredefinedMeshNodes(MeshSequence m) {
        // Change selection if complains when trying to run the code, see documentation:
        m.feature().remove("id1");
        m.feature().remove("id2");
        m.feature().remove("ftet1");
        //m.feature().remove("map1");
        //m.feature().remove("dis1");
        //m.feature().remove("dis2");
        //m.feature().remove("dis3");
        //m.feature().remove("dis4");
        //m.feature().remove("dis5");
        //m.feature().remove("copy1");
        //m.feature().remove("swe1");
        return m;
    }

    /**
     * Adjusts the global mesh size settings which can be copyed to distinct mesh elements.
     *
     * @param m The MeshSequence object to modify.
     * @param meshElementSize An integer from 1 to 9 representing mesh fineness (lower = finer).
     * @return The updated MeshSequence with adjusted size settings.
     */
    public static MeshSequence adjustGlobalSize(MeshSequence m, int meshElementSize) {
        //m.create("size", "Size"); // if did not exist, you could create with this line
        m.feature("size").label("Custom global size");
        m.feature().move("size", 0); // just moves size up in the model tree
        m.feature("size").set("hauto", meshElementSize);
        return m;
    }

    /**
     * Creates mesh for the frontal x-face of the PML region using either rectangle or quadrilateral elements.
     * <p>
     * "PMLFace" is defined in {@link IdentifyEntities#identifyPMLDomainAndFrontalFace(ModelNode)}.
     * 
     * @param m The MeshSequence to which the mesh feature is added.
     * @param meshType The type of mesh to create: "rectangle" or "quadrilateral".
     * @return The updated MeshSequence with PML mesh feature added.
     */
    public static MeshSequence createPMLMesh(MeshSequence m, String meshType) {
        if (meshType == "rectangle") {
            MeshFeature map2 = m.create("map2", "Map"); // map2 is now equal to m.feature("map2")
            m.feature().move("map2", 4); // just moves map2 up in the model tree, for some reason this must be done, otherwise generates unvisible freeQuad element an sweeps over them
            map2.label("PML mesh");
            map2.selection().named("PMLFace");
            //mapPML.selection().set(1, 4, 7, 11, 17, 34, 37, 40);

            MeshFeature map2_sizePML = map2.feature().copy("sizePML", "mesh1/size"); // map2_sizePML is now equal to map2.feature("sizePML")
            map2_sizePML.label("PML mesh size");
            map2_sizePML.set("hgrad", 1.03); // after copying, one can change. Copying makes anyways custom: true and sets element parameters active
            //map2_sizePML.selection().geom("geom1"); // ???
            //m.run("map2"); // the entire mesh will be run by the end of createMesh() method
        }

        else if (meshType == "quadrilateral") {
            MeshFeature fq1 = m.create("fq1", "FreeQuad"); // fq1 is now equal to m.feature("fq1")
            fq1.label("PML mesh");
            fq1.selection().named("PMLFace");
            //fq1.selection().set(1, 4, 7, 11, 17, 34, 37, 40);

            MeshFeature fq1_sizePML = fq1.create("sizePML", "Size"); // if you don't copy, you must create; fq1_sizePML is now equal to fq1.feature("sizePML")
            fq1_sizePML.set("hauto", 6);
            //fq1_sizePML.selection().geom("geom1"); // ???
            //m.run("fq1"); // the entire mesh will be run by the end of createMesh() method
        }
        return m;
    }

    /**
     * Creates a triangular mesh for the frontal x-faces of both beams in the geometry.
     * <p>
     * "beamsSourceFace" is defined in {@link IdentifyEntities#identifyBeamsDomainSourceAndDestinationFaces(ModelNode)}.
     *
     * @param m The MeshSequence to which the mesh is added.
     * @return The updated MeshSequence with the beam mesh configured.
     */
    public static MeshSequence createBeamMesh(MeshSequence m) {
        MeshFeature ftri2 = m.create("ftri2", "FreeTri"); // ftri2 is now equal to m.feature("ftri2")
        m.feature().move("ftri2", 2); // just moves ftri2 up in the model tree
        ftri2.label("beam mesh");
        ftri2.selection().named("beamsSourceFace");
        //ftri2.selection().set(21, 29);

        MeshFeature ftri2_sizeBeams = ftri2.create("sizeBeams", "Size"); // if you don't copy, you must create; ftri2_sizeBeams is now equal to ftri2.feature("sizeBeams")
        ftri2_sizeBeams.label("beam mesh size");
        ftri2_sizeBeams.set("custom", true);
        ftri2_sizeBeams.set("hmaxactive", true);
        ftri2_sizeBeams.set("hmax", "10*mesh_max");
        ftri2_sizeBeams.set("hgradactive", true);
        ftri2_sizeBeams.set("hgrad", "1.1");
        //ftri2_sizeBeams.selection().geom("geom1"); // ???
        //m.run("ftri2"); // the entire mesh will be run by the end of this method
        return m;
    }
    
    /**
     * Creates a triangular mesh for the frontal x-face of the non-PML vacuum region excluding the gap.
     * <p>
     * "vacuumFace" is defined in {@link IdentifyEntities#identifyVacuumDomainAndFrontalFace(ModelNode)}.
     *
     * @param m The MeshSequence object to modify.
     * @return The updated MeshSequence with vacuum face mesh added.
     */
    public static MeshSequence createVacuumMesh(MeshSequence m) {
        MeshFeature ftri3 = m.create("ftri3", "FreeTri"); // ftri3 is now equal to m.feature("ftri3")
        m.feature().move("ftri3", 3); // just moves ftri3 up in the model tree
        ftri3.label("vacuum mesh");
        ftri3.selection().named("vacuumFace");
        //ftri3.selection().set(14);

        MeshFeature ftri3_sizeVacuum = ftri3.create("sizeVacuum", "Size"); // if you don't copy, you must create; ftri3_sizeVacuum is now equal to ftri3.feature("sizeVacuum")
        ftri3_sizeVacuum.label("vacuum mesh size");
        ftri3_sizeVacuum.set("custom", true);
        ftri3_sizeVacuum.set("hmaxactive", true);
        ftri3_sizeVacuum.set("hmax", "1e-6");
        ftri3_sizeVacuum.set("hgradactive", true);
        ftri3_sizeVacuum.set("hgrad", "1.2");
        //ftri3_sizeVacuum.selection().geom("geom1"); // ???
        //m.run("ftri3"); // the entire mesh will be run by the end of this method
        return m;
    }

    /**
     * Creates a triangular mesh for the gap region between the beams on the frontal x-face.
     * <p>
     * "gapFace" is defined in {@link IdentifyEntities#identifyVacuumDomainAndFrontalFace(ModelNode)}.
     *
     * @param m The MeshSequence to which the mesh feature is added.
     * @return The updated MeshSequence with the gap mesh applied.
     */
    public static MeshSequence createGapMesh(MeshSequence m) {
        MeshFeature ftri4 = m.create("ftri4", "FreeTri"); // ftri4 is now equal to m.feature("ftri4")
        m.feature().move("ftri4", 1); // just moves ftri4 up in the model tree
        ftri4.label("gap mesh");
        ftri4.selection().named("gapFace");
        //ftri4.selection().set(25);

        MeshFeature ftri4_sizeGap = ftri4.create("sizeGap", "Size"); // if you don't copy, you must create; ftri4_sizeGap is now equal to ftri4.feature("sizeGap")
        ftri4_sizeGap.label("gap mesh size");
        ftri4_sizeGap.set("custom", true);
        ftri4_sizeGap.set("hmaxactive", true);
        ftri4_sizeGap.set("hmax", "mesh_max");
        //ftri4_sizeGap.selection().geom("geom1"); // ???
        return m;
    }

    /**
     * Sweeps the 2D mesh from the frontal x-face through the full 3D model volume,
     * generating a full 3D mesh based on the initial surface mesh.
     * <p>
     * "sourceFace" and "destinationFace" are defined in {@link IdentifyEntities#identifySourceAndDestinationFaces(ModelNode)}.
     *
     * @param m The MeshSequence containing all mesh features.
     * @return The updated MeshSequence with the sweep operation configured.
     */
    public static MeshSequence sweepMeshOver3D(MeshSequence m) {
        MeshFeature swe2 = m.create("swe2", "Sweep"); // swe2 is now equal to m.feature("swe2")
        m.feature().move("swe2", 5); // just moves swe2 up in the model tree
        swe2.label("mesh sweep");
        swe2.selection("sourceface").named("sourceFace");
        swe2.selection("targetface").named("destinationFace");
        swe2.set("facemethod", "quad"); // quadrilateral is also the default
        //m.run("swe2"); // the entire mesh will be run by the end of this method
        return m;
    }
}
