package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;

/**
 * The LimitEigenPairFilter marks all eigenpairs having an (absolute) eigenvalue
 * below the specified threshold (relative or absolute) as weak eigenpairs, the
 * others are marked as strong eigenpairs.
 * 
 * @author Elke Achtert 
 */
// todo parameter comments
public class LimitEigenPairFilter extends AbstractParameterizable implements EigenPairFilter {
  /**
   * OptionID for {@link #ABSOLUTE_FLAG}
   */
  public static final OptionID EIGENPAIR_FILTER_ABSOLUTE = OptionID.getOrCreateOptionID("pca.filter.absolute",
      "Flag to mark delta as an absolute value."
  );

  /**
   * OptionID for {@link #DELTA_PARAM}
   */
  public static final OptionID EIGENPAIR_FILTER_DELTA = OptionID.getOrCreateOptionID("pca.filter.delta",
      "The threshold for strong Eigenvalues. If not otherwise specified, delta " +
      "is a relative value w.r.t. the (absolute) highest Eigenvalues and has to be " +
      "a double between 0 and 1. To mark delta as an absolute value, use " +
      "the option -" + EIGENPAIR_FILTER_ABSOLUTE.getName() + "."
  );

  /**
   * "absolute" Flag
   */
  private final Flag ABSOLUTE_FLAG = new Flag(EIGENPAIR_FILTER_ABSOLUTE);

	/**
	 * The default value for delta.
	 */
	public static final double DEFAULT_DELTA = 0.01;

  /**
   * Parameter delta
   */
  private final DoubleParameter DELTA_PARAM = new DoubleParameter(EIGENPAIR_FILTER_DELTA,
      new GreaterEqualConstraint(0), DEFAULT_DELTA);

	/**
	 * Threshold for strong eigenpairs, can be absolute or relative.
	 */
	private double delta;

	/**
	 * Indicates whether delta is an absolute or a relative value.
	 */
	private boolean absolute;

	/**
	 * Provides a new EigenPairFilter that marks all eigenpairs having an
	 * (absolute) eigenvalue below the specified threshold (relative or
	 * absolute) as weak eigenpairs, the others are marked as strong eigenpairs.
	 */
  public LimitEigenPairFilter() {
		super();

    addOption(DELTA_PARAM);
    addOption(ABSOLUTE_FLAG);

    // Conditional Constraint:
    // delta must be >= 0 and <= 1 if it's a relative value
		// Since relative or absolute is dependent on the absolute flag this is a
		// global constraint!
		List<ParameterConstraint<Number>> cons = new Vector<ParameterConstraint<Number>>();
    // TODO: Keep the constraint here - applies to non-conditional case as well, and is set above.
		ParameterConstraint<Number> aboveNull = new GreaterEqualConstraint(0);
		cons.add(aboveNull);
		ParameterConstraint<Number> underOne = new LessEqualConstraint(1);
		cons.add(underOne);

		GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<Number, Double>(DELTA_PARAM, cons, ABSOLUTE_FLAG, false);
		optionHandler.setGlobalParameterConstraint(gpc);
	}

	public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
		StringBuffer msg = new StringBuffer();
		if (logger.isDebugging()) {
			msg.append("delta = ").append(delta);
		}

		// determine limit
		double limit;
		if (absolute) {
			limit = delta;
		} else {
			double max = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < eigenPairs.size(); i++) {
				EigenPair eigenPair = eigenPairs.getEigenPair(i);
				double eigenValue = Math.abs(eigenPair.getEigenvalue());
				if (max < eigenValue) {
					max = eigenValue;
				}
			}
			limit = max * delta;
		}
		if (logger.isDebugging()) {
			msg.append("\nlimit = ").append(limit);
		}

		// init strong and weak eigenpairs
		List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
		List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

		// determine strong and weak eigenpairs
		for (int i = 0; i < eigenPairs.size(); i++) {
			EigenPair eigenPair = eigenPairs.getEigenPair(i);
			double eigenValue = Math.abs(eigenPair.getEigenvalue());
			if (eigenValue >= limit) {
				strongEigenPairs.add(eigenPair);
			} else {
				weakEigenPairs.add(eigenPair);
			}
		}
		if (logger.isDebugging()) {
			msg.append("\nstrong EigenPairs = ").append(strongEigenPairs);
			msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
			logger.debugFine(msg.toString());
		}

		return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
	}

    /**
     * Calls the super method
     * and sets additionally the values of the flag
     * {@link #ABSOLUTE_FLAG} and the parameter {@link #DELTA_PARAM}.
     */
    @Override
    public List<String> setParameters(List<String> args) throws ParameterException {
		List<String> remainingParameters = super.setParameters(args);

		// absolute
		absolute = ABSOLUTE_FLAG.isSet();

		// delta
		delta = DELTA_PARAM.getValue();
		if (absolute && DELTA_PARAM.tookDefaultValue()) {
			throw new WrongParameterValueException("Illegal parameter setting: " + "Flag " + ABSOLUTE_FLAG.getName() + " is set, " + "but no value for "
					+ DELTA_PARAM.getName() + " is specified.");
		}

		return remainingParameters;
	}

  @Override
  public String shortDescription() {
		StringBuffer description = new StringBuffer();
		description.append(PercentageEigenPairFilter.class.getName());
		description.append(" filters all eigenpairs, which are lower than a given value.\n");
		return description.toString();
	}

}
