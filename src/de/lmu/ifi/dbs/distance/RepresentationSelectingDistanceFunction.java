package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Distance function for multirepresented objects that selects one represenation and
 * computes the distances only within the selected representation.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RepresentationSelectingDistanceFunction<O extends DatabaseObject<O>, M extends MultiRepresentedObject<O>, D extends Distance<D>> extends AbstractDistanceFunction<M, D> {
  /**
   * A pattern defining a comma.
   */
  public static final Pattern SPLIT = Pattern.compile(",");

  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for distance functions.
   */
  public static final String DISTANCE_FUNCTIONS_P = "distancefunctions";

  /**
   * Description for parameter distance functions.
   */
  public static final String DISTANCE_FUNCTIONS_D = "\"<classname_1,...,classname_n>a comma separated list of the distance functions to determine the distance between objects within one representation (default: " + DEFAULT_DISTANCE_FUNCTION + ")";

  /**
   * The index of the current representation.
   */
  private int currentRepresentationIndex = -1;

  /**
   * The list of distance functions for each representation.
   */
  private List<DistanceFunction<O, D>> distanceFunctions;

  /**
   * The default distance function.
   */
  private DistanceFunction<O, D> defaultDistanceFunction;

  /**
   * Provides a Distance function for multirepresented objects that selects one
   * represenation and computes the distances only within the selected representation.
   */
  public RepresentationSelectingDistanceFunction() {
    super();
    parameterToDescription.put(DISTANCE_FUNCTIONS_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTIONS_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Sets the currently selected representation for which the distances will be computed.
   *
   * @param index the index of the representation to be selected
   */
  public void setCurrentRepresentationIndex(int index) {
    this.currentRepresentationIndex = index;
  }

  /**
   * @see DistanceFunction#valueOf(String)
   */
  public D valueOf(String pattern) throws IllegalArgumentException {
    return getDistanceFunctionForCurrentRepresentation().valueOf(pattern);
  }

  /**
   * @see DistanceFunction#infiniteDistance()
   */
  public D infiniteDistance() {
    return getDistanceFunctionForCurrentRepresentation().infiniteDistance();
  }

  /**
   * @see DistanceFunction#nullDistance()
   */
  public D nullDistance() {
    return getDistanceFunctionForCurrentRepresentation().nullDistance();
  }

  /**
   * @see DistanceFunction#undefinedDistance()
   */
  public D undefinedDistance() {
    return getDistanceFunctionForCurrentRepresentation().undefinedDistance();
  }

  /**
   * @see DistanceFunction#distance(DatabaseObject, DatabaseObject)
   */
  public D distance(M o1, M o2) {
    O object1 = o1.getRepresentation(currentRepresentationIndex);
    O object2 = o2.getRepresentation(currentRepresentationIndex);

    return getDistanceFunctionForCurrentRepresentation().distance(object1, object2);
  }

  /**
   * @see DistanceFunction#description()
   */
  public String description() {
    return "Distance function for multirepresented objects that selects one represenation and " +
           "computes the distances only within the selected representation.";
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

    // distance functions
    if (optionHandler.isSet(DISTANCE_FUNCTIONS_P)) {
      String distanceFunctions = optionHandler.getOptionValue(DISTANCE_FUNCTIONS_P);
      String[] distanceFunctionsClasses = SPLIT.split(distanceFunctions);
      if (distanceFunctionsClasses.length == 0) {
        throw new IllegalArgumentException("No distance functions specified.");
      }
      this.distanceFunctions = new ArrayList<DistanceFunction<O, D>>(distanceFunctionsClasses.length);
      for (String distanceFunctionClass : distanceFunctionsClasses) {
        //noinspection unchecked
        this.distanceFunctions.add(Util.instantiate(DistanceFunction.class, distanceFunctionClass));
      }

      for (DistanceFunction<O, D> distanceFunction : this.distanceFunctions) {
        remainingParameters = distanceFunction.setParameters(remainingParameters);
      }
      return remainingParameters;
    }
    else {
      //noinspection unchecked
      defaultDistanceFunction = Util.instantiate(DistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);
      return defaultDistanceFunction.setParameters(remainingParameters);
    }
  }

  /**
   * Returns the distance function for the currently selected representation.
   *
   * @return the distance function for the currently selected representation
   */
  private DistanceFunction<O, D> getDistanceFunctionForCurrentRepresentation() {
    if (currentRepresentationIndex < 0)
      throw new IllegalStateException("Wrong representation set, current index = " + currentRepresentationIndex);

    if (distanceFunctions.size() > 0) {
      if (currentRepresentationIndex > distanceFunctions.size())
        throw new IllegalStateException("Wrong representation set, current index = " + currentRepresentationIndex);
      return distanceFunctions.get(currentRepresentationIndex);
    }

    else return defaultDistanceFunction;
  }

}
