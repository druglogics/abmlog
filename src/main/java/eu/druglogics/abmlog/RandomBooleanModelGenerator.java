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
import java.util.SplittableRandom;

import static eu.druglogics.abmlog.Util.genModel;
import static eu.druglogics.abmlog.Util.getLinkOperatorsIndexes;
import static eu.druglogics.gitsbe.util.Util.*;

public class RandomBooleanModelGenerator {
	public String networkFile;
	public String resultsDirectory;
	public String modelsDirectory;
	public int verbosity;
	public int randomModelsNumber;
	public long seed;
	public Logger logger;
	public BooleanModel model;

	RandomBooleanModelGenerator() {
		// empty constructor
	}

	public static void main(String[] args) {
		RandomBooleanModelGenerator modelGenerator = new RandomBooleanModelGenerator();

		try {
			modelGenerator.initInputArgs(args);
			modelGenerator.initLogger();
			modelGenerator.initModel();
			modelGenerator.generateRandomModels();
		} catch (ParameterException parEx) {
			System.out.println("\nOptions preceded by an asterisk are required.");
			parEx.getJCommander().setProgramName("eu.druglogics.amblog.BooleanModelGenerator");
			parEx.usage();
		} catch (IOException | ConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			e.getMessage();
		}
	}

	public void initInputArgs(String[] args) throws ConfigurationException, IOException {
		RandomModelGenCommandLineArgs arguments = new RandomModelGenCommandLineArgs();
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

		randomModelsNumber = arguments.getRandomModelsNumber();
		if (randomModelsNumber < 0) {
			throw new ConfigurationException("Please provide a positive number for the number of " +
				"random models to generate");
		}

		seed = arguments.getSeed();
	}

	public void initLogger() throws IOException {
		String filenameOutput = "log";
		logger = new Logger(filenameOutput, resultsDirectory, verbosity, true);

		String[] argsMessage = { "\nInput parameters", "----------------",
			"Network File: " + networkFile };
		logger.outputLines(3, argsMessage);
	}

	public void initModel() throws Exception {
		GeneralModel generalModel = new GeneralModel(logger);

		generalModel.loadInteractionsFile(networkFile);
		generalModel.buildMultipleInteractions();

		String attractorTool = "biolqm_stable_states";
		this.model = new BooleanModel(generalModel, attractorTool, logger);
	}

	public void generateRandomModels() throws Exception {
		logger.outputHeader(3, "Random Model Generator");
		logger.outputHeader(3, "Total number of models to be generated: " + randomModelsNumber);

		String baseName = model.getModelName();
		ArrayList<Integer> indexes = getLinkOperatorsIndexes(model);
		long lastModelNumber = (long) Math.pow(2.0, indexes.size()); // 2^(#equations with link operators)

		SplittableRandom generator = new SplittableRandom(seed);
		for (int modelNumber = 1; modelNumber <= randomModelsNumber; modelNumber++) {
			long num = generator.nextLong(0, lastModelNumber);

			logger.outputStringMessage(3, "\nGenerating model No. " + num
				+ " (" + String.format("%.1f", ((float) modelNumber / randomModelsNumber * 100)) + "%)");
			genModel(model, baseName, num, indexes, true, modelsDirectory);
		}
	}
}
