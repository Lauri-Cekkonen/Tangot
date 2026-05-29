import java.util.List; // for the List used in TupleLike class
import com.comsol.model.*;

/**
 * A simple container class to return both a COMSOL {@link Model}
 * and a list of geometry-related string arrays from a method.
 * <p>
 * Used to emulate tuple-like behavior since Java does not natively support returning multiple values.
 */
public class TupleLike {
    private Model model;
    private List<String[]> geometryArguments;
    private Study study;
    private StudyFeature studyFeature;
    private SolverSequence solver;
    private NumericalFeature evaluation;
    private ExportFeature exportTable;
    private DatasetFeature surface;

    /**
     * Constructs a new TupleLike object.
     *
     * @param model The COMSOL {@link Model} instance.
     * @param geometryArguments A list of String arrays representing geometry parameters.
     */
    public TupleLike(Model model, List<String[]> geometryArguments) {
        this.model = model;
        this.geometryArguments = geometryArguments;
    }

    public TupleLike(Model model, Study study, StudyFeature studyFeature) {
        this.model = model;
        this.study = study;
        this.studyFeature = studyFeature;
    }

    public TupleLike(Model model, SolverSequence solver) {
        this.model = model;
        this.solver = solver;
    }

    public TupleLike(Model model, NumericalFeature numerical, ExportFeature export) {
        this.model = model;
        this.evaluation = numerical;
        this.exportTable = export;
    }

    public TupleLike(Model model, DatasetFeature surface) {
        this.model = model;
        this.surface = surface;
    }

    /**
     * Gets the {@link Model} object stored in this tuple.
     *
     * @return The COMSOL model instance.
     */
    public Model getModel() {
        return this.model;
    }

    /**
     * Gets the list of geometry-related string arrays.
     *
     * @return A {@link List} of string arrays representing geometry arguments.
     */
    public List<String[]> getStringArrays() {
        return this.geometryArguments;
    }

    public Study getStudy() {
        return this.study;
    }

    public StudyFeature getStudyFeature() {
        return this.studyFeature;
    }

    public SolverSequence getSolver() {
        return this.solver;
    }

    public NumericalFeature getNumericalFeature() {
        return this.evaluation;
    }

    public ExportFeature getExportFeature() {
        return this.exportTable;
    }

    public DatasetFeature getDatasetFeature() {
        return this.surface;
    }
}
