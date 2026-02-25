package uk.ac.warwick.dcs.sherlock.engine.executor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.dcs.sherlock.api.annotation.AdjustableParameter;
import uk.ac.warwick.dcs.sherlock.api.executor.IExecutor;
import uk.ac.warwick.dcs.sherlock.api.util.SherlockHelper;
import uk.ac.warwick.dcs.sherlock.api.util.Tuple;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Various executor utilities
 */
public class ExecutorUtils {

	public static final Logger logger = LoggerFactory.getLogger(IExecutor.class);

	/**
	 * does average of list
	 * @param scores list to average
	 * @return average
	 */
	public static float aggregateScores(Collection<Float> scores) {
		return (float) scores.stream().mapToDouble(x -> x).average().orElse(-1);
	}

	/**
	 * Returns all declared fields on a class and all of its superclasses, up to (but not including) Object.
	 * This allows @AdjustableParameter fields inherited from abstract base classes (e.g. PairwiseDetector)
	 * to be discovered when scanning a concrete subclass.
	 *
	 * @param clazz the class to scan
	 * @return list of all fields in the class hierarchy
	 */
	public static List<Field> getAllFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<>();
		while (clazz != null && clazz != Object.class) {
			fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
			clazz = clazz.getSuperclass();
		}
		return fields;
	}

	/**
	 * Populates the adjustables in an object
	 * @param instance object to populate
	 * @param params list of param references and values
	 * @param <T> type of the object to populate
	 */
	public static <T> void processAdjustableParameters(T instance, Map<String, Float> params) {
		getAllFields(instance.getClass()).stream().map(f -> new Tuple<>(f, f.getDeclaredAnnotationsByType(AdjustableParameter.class))).filter(x -> x.getValue().length == 1).forEach(x -> {
			String ref = SherlockHelper.buildFieldReference(x.getKey());
			boolean isInt = x.getKey().getType().equals(int.class);
			float val;

			if (params.containsKey(ref)) {
				val = params.get(ref);

				if (isInt && val % 1 != 0) {
					logger.error("Trying to assign a float value to integer adjustable parameter {}", ref);
					return;
				}

				if (val > x.getValue()[0].maximumBound() || val < x.getValue()[0].minimumBound()) {
					logger.error("Trying to assign an out of bounds value to adjustable parameter {}", ref);
					return;
				}
			}
			else {
				val = x.getValue()[0].defaultValue();
			}

			Field f = x.getKey();
			f.setAccessible(true);
			try {
				if (isInt) {
					int vali = (int) val;
					f.set(instance, vali);
				}
				else {
					f.set(instance, val);
				}
			}
			catch (IllegalAccessException | IllegalArgumentException | NullPointerException e) {
				logger.error("Could not set adjustable parameter", e);
			}
		});
	}

}
