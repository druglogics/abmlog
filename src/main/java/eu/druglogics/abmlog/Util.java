package eu.druglogics.abmlog;

import eu.druglogics.gitsbe.model.BooleanEquation;
import eu.druglogics.gitsbe.model.BooleanModel;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.SplittableRandom;

/**
 * Class with useful static functions
 */
public class Util {

	public static void exportModel(BooleanModel booleanModel, boolean calculateAttractors,
								   String modelsDirectory) throws IOException {
		if (calculateAttractors) { // there is a .bnet file already created
			booleanModel.exportModelToGitsbeFile(modelsDirectory);
		} else { // no .bnet file created
			booleanModel.exportModelToGitsbeFile(modelsDirectory);
			booleanModel.exportModelToBoolNetFile(modelsDirectory);
		}
	}

	/**
	 * Get an {@link ArrayList} of indexes of the boolean equations of the initial model
	 * that have link operators (both activators and inhibitors).
	 */
	public static ArrayList<Integer> getLinkOperatorsIndexes(BooleanModel model) {
		ArrayList<Integer> res = new ArrayList<>();

		int index = 0;
		for (BooleanEquation booleanEquation : model.getBooleanEquations()) {
			String link = booleanEquation.getLink();
			if (link.equals("and") || link.equals("or")) {
				res.add(index);
			}
			index++;
		}

		return res;
	}

	/**
	 * Find the binary representation of a given decimal <i>number</i>, using the given
	 * amount of <i>digits</i>.
	 *
	 * @param number decimal number
	 * @param digits number of binary digits
	 * @return the binary representation
	 *
	 */
	public static String getBinaryRepresentation(long number, int digits) throws Exception {
		// with given digits we can reach up to:
		long max = (long) Math.pow(2, digits) - 1;

		if ((number > max) || (number < 0) || (digits < 1))
			throw new Exception("Number is negative or it cannot be represented "
				+ "with the given number of digits");

		char[] arr = Long.toBinaryString(number).toCharArray();
		StringBuilder sb = new StringBuilder();
		for (Character c : arr) {
			sb.append(c);
		}

		// add padding zeros if needed
		String res = sb.toString();
		if (res.length() < digits) {
			res = StringUtils.leftPad(sb.toString(), digits, "0");
		}

		return res;
	}

	/**
	 * Use this function to generate a (mutated) boolean model based on the binary representation of the
	 * <i>modelNumber</i> given, which indicates if the equations at the specific
	 * <i>indexes</i> will have an <b>and not (0)</b> or <b>or not (1)</b> link operator.
	 * <br>
	 * This function also does the calculation of the model's attractors and the exporting as well
	 * (depends on the available options).
	 *
	 */
	public static void genModel(BooleanModel booleanModel, String baseName, long modelNumber, ArrayList<Integer> indexes,
						 boolean calculateAttractors, String modelsDirectory) throws Exception {
		booleanModel.setModelName(baseName + "_" + modelNumber);

		String binaryRes = getBinaryRepresentation(modelNumber, indexes.size());

		int digitIndex = 0;
		for (char digit : binaryRes.toCharArray()) {
			int equationIndex = indexes.get(digitIndex);
			String link = booleanModel.getBooleanEquations().get(equationIndex).getLink();
			if ((digit == '0') && (link.equals("or")) || (digit == '1') && (link.equals("and"))) {
				booleanModel.changeLinkOperator(equationIndex);
			}
			digitIndex++;
		}

		if (calculateAttractors) {
			booleanModel.resetAttractors();
			booleanModel.calculateAttractors(modelsDirectory);
		}

		exportModel(booleanModel, calculateAttractors, modelsDirectory);
	}
}
