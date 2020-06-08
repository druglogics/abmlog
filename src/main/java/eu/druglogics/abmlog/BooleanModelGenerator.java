package eu.druglogics.abmlog;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import eu.druglogics.gitsbe.model.BooleanModel;
import eu.druglogics.gitsbe.model.GeneralModel;
import eu.druglogics.gitsbe.util.Logger;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.stream.IntStream;

import static eu.druglogics.abmlog.Util.genModel;
import static eu.druglogics.abmlog.Util.getLinkOperatorsIndexes;
import static eu.druglogics.gitsbe.util.Util.*;

public class BooleanModelGenerator {

	public String networkFile;
	public String resultsDirectory;
	public String modelsDirectory;
	public String attractors;
	public int maxDirSize;
	public int verbosity;
	public boolean parallel;
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

		parallel = arguments.getParallel();

		maxDirSize = arguments.getMaxDirSize();
	}

	public void initLogger() throws IOException {
		String filenameOutput = "log";
		logger = new Logger(filenameOutput, resultsDirectory, verbosity, true);

		String[] argsMessage = { "\nInput parameters", "----------------",
			"Network File: " + networkFile, "Attractors: " + attractors,
			"Verbosity: " + verbosity, "Parallel: " + parallel };
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

		String baseName = model.getModelName();

		boolean calculateAttractors = true;
		if (attractors == null) {
			calculateAttractors = false;
			logger.outputStringMessage(3, "Attractors will NOT be calculated for the generated models!");
		}

		ArrayList<Integer> indexes = getLinkOperatorsIndexes(model);
		long numOfModels = (long) Math.pow(2.0, indexes.size());

		logger.outputHeader(3, "Total number of models: " + numOfModels + " ("
			+ indexes.size() + " equations with link operators)");

		int cores = Runtime.getRuntime().availableProcessors();
		if (cores == 1) parallel = false;
		if (cores % 2 == 1) cores--; // so that we have an even number of cores

		if (parallel && cores < numOfModels) { // USE ALL CORES
			long range = numOfModels / cores;

			String COMMON_FORK_JOIN_POOL_PARALLELISM = "java.util.concurrent.ForkJoinPool.common.parallelism";
			System.setProperty(COMMON_FORK_JOIN_POOL_PARALLELISM, Integer.toString(cores - 1));
			logger.outputHeader(3, "Use parallelism with " + cores + " cores where each core " +
				"will handle " + range + " models");

			boolean finalCalculateAttractors = calculateAttractors;
			IntStream.range(0, cores).parallel().forEach(coreId -> {
				try {
					logger.outputStringMessage(3, "Model generating job assigned to Core No. " + (coreId + 1) + " started");
					Logger logger = new Logger("log_" + coreId, resultsDirectory, verbosity, true);
					BooleanModel booleanModel = new BooleanModel(model, logger);

					long startIndex = coreId * range;
					long endIndex = (coreId + 1) * range;
					logger.outputStringMessage(3, "This job will cover model ranging from "
						+ startIndex + " to " + (endIndex - 1) + " (inclusive)");

					long modelsDirSize = 0; // counts the number of models in a specific models dir
					long modelDirIndex = 0;
					String newModelsDirectory = new File(modelsDirectory + "/core_" + coreId
						+ "_" + modelDirIndex).getAbsolutePath();
					createDirectory(newModelsDirectory, logger);

					int index = 0;
					for (long modelNumber = startIndex; modelNumber < endIndex; modelNumber++) {
						if (modelsDirSize > maxDirSize - 1) { // create new models dir to avoid filesystem errors
							newModelsDirectory = new File(modelsDirectory + "/core_" + coreId
								+ "_" + (++modelDirIndex)).getAbsolutePath();
							createDirectory(newModelsDirectory, logger);

							modelsDirSize = 0;
						}

						logger.outputStringMessage(3, "\nGenerating model No. " + modelNumber
							+ " (" + String.format("%.1f", ((float) index / range * 100)) + "%)");
						genModel(booleanModel, baseName, modelNumber, indexes, finalCalculateAttractors, newModelsDirectory);

						index++;
						modelsDirSize++;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		} else { // USE ONE CORE
			long modelsDirSize = 0; // counts the number of models in a specific models dir
			long modelDirIndex = 0;
			String newModelsDirectory = modelsDirectory;
			for (long modelNumber = 0; modelNumber < numOfModels; modelNumber++) {
				if (modelsDirSize > maxDirSize - 1) { // create new models dir to avoid filesystem errors
					newModelsDirectory = new File(resultsDirectory + "/models_" + (++modelDirIndex)).getAbsolutePath();
					createDirectory(newModelsDirectory, this.logger);

					modelsDirSize = 0;
				}

				logger.outputStringMessage(3, "\nGenerating model No. " + modelNumber
					+ " (" + String.format("%.1f", ((float) modelNumber / numOfModels * 100)) + "%)");
				genModel(this.model, baseName, modelNumber, indexes, calculateAttractors, newModelsDirectory);

				modelsDirSize++;
			}
		}
	}
}
