package experimentalcode.shared.algorithm.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * KNNClassifier classifies instances based on the class distribution among the
 * k nearest neighbors in a database.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 * @param <L> the type of the ClassLabel the Classifier is assigning
 */
public class KNNClassifier<O extends DatabaseObject, D extends Distance<D>, L extends ClassLabel>
    extends DistanceBasedClassifier<O, D, L> {
    /**
     * OptionID for {@link experimentalcode.shared.algorithm.classifier.KNNClassifier#K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID(
        "knnclassifier.k",
        "The number of neighbors to take into account for classification."
    );

    /**
     * Parameter to specify the number of neighbors to take into account for classification,
     * must be an integer greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -knnclassifier.k} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(K_ID,
        new GreaterConstraint(0), 1);

    /**
     * Holds the value of @link #K_PARAM}.
     */
    protected int k;

    /**
     * Holds the database where the classification is to base on.
     */
    protected Database<O> database;

    /**
     * Provides a KNNClassifier,
     * adding parameter {@link #K_PARAM} to the option handler
     * additionally to parameters of super class.
     */
    public KNNClassifier() {
        super();
        // parameter k
        addOption(K_PARAM);
    }

    /**
     * Checks whether the database has the class labels set. Collects the class
     * labels available n the database. Holds the database to lazily classify
     * new instances later on.
     */
    public void buildClassifier(Database<O> database, ArrayList<L> labels) throws IllegalStateException {
        this.setLabels(labels);
        this.database = database;
    }

    /**
     * Provides a class distribution for the given instance. The distribution is
     * the relative value for each possible class among the k nearest neighbors
     * of the given instance in the previously specified database.
     */
    public double[] classDistribution(O instance) throws IllegalStateException {
        try {
            double[] distribution = new double[getLabels().size()];
            int[] occurences = new int[getLabels().size()];

            List<DistanceResultPair<D>> query = database.kNNQueryForObject(instance,
                k, getDistanceFunction());
            for (DistanceResultPair<D> neighbor : query) {
                // noinspection unchecked
                int index = Collections.binarySearch(getLabels(),
                    (AssociationID.CLASS.getType().cast(database.getAssociation(AssociationID.CLASS, neighbor.getID()))));
                if (index >= 0) {
                    occurences[index]++;
                }
            }
            for (int i = 0; i < distribution.length; i++) {
                distribution[i] = ((double) occurences[i])
                    / (double) query.size();
            }
            return distribution;
        }
        catch (NullPointerException e) {
            IllegalArgumentException iae = new IllegalArgumentException(e);
            iae.fillInStackTrace();
            throw iae;
        }
    }

    public Description getDescription() {
        return new Description(
            "kNN-classifier",
            "kNN-classifier",
            "Lazy classifier classifies a given instance to the majority class of the k-nearest neighbors.",
            "");
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameter
     * {@link #K_PARAM}.
     */
    @Override
    public List<String> setParameters(List<String> args) throws ParameterException {
        List<String> remainingParameters = super.setParameters(args);

        // parameter k
        k = K_PARAM.getValue();

        return remainingParameters;
    }

    public String model() {
        return "lazy learner - provides no model";
    }

    @Override
    protected Result runInTime(@SuppressWarnings("unused") Database<O> database) throws IllegalStateException {
      // TODO Implement sensible default behavior.
      return null;
    }

    @Override
    public Result getResult() {
      // TODO Add when runInTime was implemented.
      return null;
    }

}
