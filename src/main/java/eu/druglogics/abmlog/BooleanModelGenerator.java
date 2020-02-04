package eu.druglogics.abmlog;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import eu.druglogics.gitsbe.model.BooleanEquation;
import eu.druglogics.gitsbe.model.BooleanModel;
import eu.druglogics.gitsbe.model.GeneralModel;
import eu.druglogics.gitsbe.util.FileDeleter;
import eu.druglogics.gitsbe.util.Logger;
import org.apache.commons.lang3.StringUtils;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.stream.IntStream;

import static eu.druglogics.gitsbe.util.Util.*;

public class BooleanModelGenerator {

	public String networkFile;
	public String resultsDirectory;
	public String modelsDirectory;
	public String attractors;
	public int verbosity;
	public int export;
	public boolean parallel;
	public Logger logger;
	public BooleanModel model;

	private final String COMMON_FORK_JOIN_POOL_PARALLELISM = "java.util.concurrent.ForkJoinPool.common.parallelism";

	BooleanModelGenerator() {
		// empty constructor
	}

	public static void main(String[] args) {
		BooleanModelGenerator modelGenerator = new BooleanModelGenerator();

		try {
			modelGenerator.initInputArgs(args);
			modelGenerator.initLogger();
			modelGenerator.initModel();
			modelGenerator.generateModels();
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

		export = arguments.getExport();
		if ((export != 0) && (export != 1))
			throw new ConfigurationException("Export can only be: 0 or 1");

		if ((export == 1) && (attractors == null))
			throw new ConfigurationException("Cannot have export of models with 1 or more attractors "
				+ "without calculating these! Please specify an `attractors` option or set `export` to 0 "
				+ "(all models)");

		parallel = arguments.getParallel();
	}

	public void initLogger() throws IOException {
		String filenameOutput = "log";
		logger = new Logger(filenameOutput, resultsDirectory, verbosity, true);

		String[] argsMessage = { "\nInput parameters", "----------------",
			"Network File: " + networkFile, "Attractors: " + attractors, "Verbosity: " + verbosity,
			"Export: " + export, "Parallel: " + parallel };
		logger.outputLines(3, argsMessage);
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

	public void generateModels() throws Exception {
		logger.outputHeader(3, "Model Generation, Attractor Calculation and Export");

		FileDeleter fileDeleter = new FileDeleter(modelsDirectory);
		String baseName = model.getModelName();

		boolean calculateAttractors = true;
		if (attractors == null) {
			calculateAttractors = false;
			logger.outputStringMessage(3, "Attractors will NOT be calculated for the generated models!");
		}

		ArrayList<Integer> indexes = getLinkOperatorsIndexes();
		int numOfModels = (int) Math.pow(2.0, indexes.size());

		logger.outputHeader(3, "Total number of models: " + numOfModels + " ("
			+ indexes.size() + " equations with link operators)");

		int cores = Runtime.getRuntime().availableProcessors();
		if (cores == 1) parallel = false;
		if (cores % 2 == 1) cores--; // so that we have an even number of cores

		if (parallel && cores < numOfModels) { // USE ALL CORES
			int range = numOfModels / cores;

			System.setProperty(COMMON_FORK_JOIN_POOL_PARALLELISM, Integer.toString(cores - 1));
			logger.outputHeader(3, "Use parallelism with " + cores + " cores where each core " +
				"will handle " + range + " models");

			boolean finalCalculateAttractors = calculateAttractors;
			IntStream.range(0, cores).parallel().forEach(coreId -> {
				try {
					logger.outputStringMessage(3, "Model generating job assigned to Core No. " + (coreId + 1) + " started");
					Logger logger = new Logger("log_" + coreId, resultsDirectory, verbosity, true);
					BooleanModel booleanModel = new BooleanModel(model, logger);

					int startIndex = coreId * range;
					int endIndex = (coreId + 1) * range;
					logger.outputStringMessage(3, "This job will cover model ranging from "
						+ startIndex + " to " + (endIndex - 1) + " (inclusive)");
					int index = 0;
					for (int modelNumber = startIndex; modelNumber < endIndex; modelNumber++) {
						logger.outputStringMessage(3, "\nGenerating model No. " + modelNumber
							+ " (" + String.format("%.1f", ((float) index / range * 100)) + "%)");
						genModel(booleanModel, baseName, modelNumber, indexes, finalCalculateAttractors, fileDeleter, logger);
						index++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} else { // USE ONE CORE
			for (int modelNumber = 0; modelNumber < numOfModels; modelNumber++) {
				logger.outputStringMessage(3, "\nGenerating model No. " + modelNumber
					+ " (" + String.format("%.1f", ((float) modelNumber / numOfModels * 100)) + "%)");
				genModel(this.model, baseName, modelNumber, indexes, calculateAttractors, fileDeleter, this.logger);
			}
		}
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
	public void genModel(BooleanModel booleanModel, String baseName, int modelNumber, ArrayList<Integer> indexes,
						 boolean calculateAttractors, FileDeleter fileDeleter, Logger logger) throws Exception {
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

		exportModel(booleanModel, calculateAttractors, fileDeleter, logger);
	}

	public void exportModel(BooleanModel booleanModel, boolean calculateAttractors, FileDeleter fileDeleter, Logger logger) throws IOException {
		if (calculateAttractors) { // there is a .bnet file already created
			if (export == 0 || (export == 1  && booleanModel.hasAttractors())) {
				booleanModel.exportModelToGitsbeFile(modelsDirectory);
			} else if ((export == 1) && (!booleanModel.hasAttractors())) {
				fileDeleter.activate();
				FileDeleter.deleteFilesMatchingPattern(logger, booleanModel.getModelName());
				fileDeleter.disable();
			}
		} else { // no .bnet file created, export should be 0 (all models exported)
			booleanModel.exportModelToGitsbeFile(modelsDirectory);
			booleanModel.exportModelToBoolNetFile(modelsDirectory);
		}
	}

	/**
	 * Get an {@link ArrayList} of indexes of the boolean equations of the initial model
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
