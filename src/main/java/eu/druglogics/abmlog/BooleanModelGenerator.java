package eu.druglogics.abmlog;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import eu.druglogics.gitsbe.model.BooleanEquation;
import eu.druglogics.gitsbe.model.BooleanModel;
import eu.druglogics.gitsbe.model.GeneralModel;
import eu.druglogics.gitsbe.util.Logger;
import org.apache.commons.lang3.StringUtils;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static eu.druglogics.gitsbe.util.Util.*;

public class BooleanModelGenerator {

	public String networkFile;
	public String resultsDirectory;
	public String modelsDirectory;
	public String attractors;
	public int verbosity;
	public Logger logger;

	public BooleanModel model;

	BooleanModelGenerator() {
		// empty constructor
	}

	public static void main(String[] args) {
		BooleanModelGenerator modelGenerator = new BooleanModelGenerator();

		try {
			modelGenerator.initInputArgs(args);
			modelGenerator.initLogger();
			modelGenerator.initModel();
			modelGenerator.genModels();
		} catch (ParameterException parEx) {
			System.out.println("\nOptions preceded by an asterisk are required.");
			parEx.getJCommander().setProgramName("eu.druglogics.amblog.BooleanModelGenerator");
			parEx.usage();
		}
		catch (IOException | ConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			e.getMessage();
		}
	}

	public void initInputArgs(String[] args) throws ConfigurationException, ParameterException, IOException {
		CommandLineArgs arguments = new CommandLineArgs();
		JCommander.newBuilder().addObject(arguments).build().parse(args);

		networkFile = new File(arguments.getNetworkFile()).getAbsolutePath();
		if (!getFileExtension(networkFile).equals(".sif"))
			throw new ConfigurationException("The network file should be a `.sif`");

		String verbosityLevel = arguments.getVerbosity();
		verbosity = (verbosityLevel == null) ? 3 : Integer.parseInt(verbosityLevel);

		// infer the input dir from .sif file and create a new 'results' dir and a 'models' dir where the generated files will be stored
		String inputDirectory = new File(networkFile).getParent();

		DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
		String dateStr = dateFormat.format(Calendar.getInstance().getTime());
		resultsDirectory = new File(inputDirectory + "/results_"
			+ removeExtension(new File(networkFile).getName()) + "_" + dateStr).getAbsolutePath();
		createDirectory(resultsDirectory);

		modelsDirectory = new File(resultsDirectory + "/models").getAbsolutePath();
		createDirectory(modelsDirectory);

		attractors = arguments.getAttractors();

		if ((attractors != null) &&
			(!attractors.equals("fixpoints")) &&
			(!attractors.equals("trapspaces"))) {
			throw new ConfigurationException("Attractors can only be: `fixpoints` or `trapspaces` or absent");
		}
	}

	public void initLogger() throws IOException {
		String filenameOutput = "log";
		logger = new Logger(filenameOutput, resultsDirectory, verbosity, true);
	}

	public void initModel() throws Exception {
		GeneralModel generalModel = new GeneralModel(logger);

		generalModel.loadInteractionsFile(networkFile);
		generalModel.buildMultipleInteractions();

		String attractorTool;
		if (attractors == null || attractors.equals("fixpoints"))
			attractorTool = "biolqm_stable_states";
		else
			attractorTool = "biolqm_trapspaces";

		this.model = new BooleanModel(generalModel, attractorTool, logger);
	}

	public void genModels() throws Exception {
		logger.outputHeader(3, "Model Generation, Attractor Calculation and Export");

		boolean calculateAttractors = true;
		if (attractors == null) {
			calculateAttractors = false;
			logger.outputStringMessage(3, "Attractors will NOT be calculated for the generated models!");
		}

		ArrayList<Integer> indexes = getLinkOperatorsIndexes();
		int numOfModels = (int) Math.pow(2.0, indexes.size());

		logger.outputHeader(3, "Total number of models: " + numOfModels + " ("
			+ indexes.size() + " equations with link operators)");

		String baseName = model.getModelName();
		for(int modelNumber = 0; modelNumber < numOfModels; modelNumber++) {
			model.setModelName(baseName + "_" + modelNumber);

			logger.outputStringMessage(3, "\nGenerating model No. " + modelNumber);

			String binaryRes = getBinaryRepresentation(modelNumber, indexes.size());

			int digitIndex = 0;
			for(char digit: binaryRes.toCharArray()) {
				int equationIndex = indexes.get(digitIndex);
				String link = model.getBooleanEquations().get(equationIndex).getLink();
				if ((digit == '0') && (link.equals("or")) || (digit == '1') && (link.equals("and"))) {
					model.changeLinkOperator(equationIndex);
				}
				digitIndex++;
			}

			if (calculateAttractors) {
				model.resetAttractors();
				model.calculateAttractors(modelsDirectory);
			}

			model.exportModelToGitsbeFile(modelsDirectory);
		}
	}

	/**
	 * Get an {@link ArrayList} of indexes of the boolean equations of the model
	 * that have link operators (both activators and inhibitors).
	 */
	public ArrayList<Integer> getLinkOperatorsIndexes() {
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
	public String getBinaryRepresentation(int number, int digits) throws Exception {
		// with given digits we can reach up to:
		int max = (int) Math.pow(2, digits) - 1;

		if ((number > max) || (number < 0) || (digits < 1))
			throw new Exception("Number is negative or it cannot be represented "
				+ "with the given number of digits");

		char[] arr = Integer.toBinaryString(number).toCharArray();
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
}
