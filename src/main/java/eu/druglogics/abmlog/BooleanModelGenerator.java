package eu.druglogics.abmlog;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import eu.druglogics.gitsbe.model.BooleanEquation;
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

import static eu.druglogics.gitsbe.util.Util.createDirectory;
import static eu.druglogics.gitsbe.util.Util.getFileExtension;

public class BooleanModelGenerator {

	private String networkFile;
	private String workDirectory;
	private String attractors;
	private int verbosity;
	private Logger logger;

	private BooleanModelGenerator() {
		// empty constructor
	}

	public static void main(String[] args) {
		BooleanModelGenerator modelGenerator = new BooleanModelGenerator();

		try {
			modelGenerator.initInputArgs(args);
			modelGenerator.initLogger();

			BooleanModel booleanModel = modelGenerator.initModel();

			modelGenerator.printTotalNumberOfModels(booleanModel);

			ArrayList<BooleanModel> models = modelGenerator.genModels(booleanModel);

			modelGenerator.calculateModelAttractors(models);
			modelGenerator.exportModels(models);
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

	private void initInputArgs(String[] args) throws ConfigurationException, ParameterException, IOException {
		CommandLineArgs arguments = new CommandLineArgs();
		JCommander.newBuilder().addObject(arguments).build().parse(args);

		networkFile = new File(arguments.getNetworkFile()).getAbsolutePath();
		if (!getFileExtension(networkFile).equals(".sif"))
			throw new ConfigurationException("The network file should be a `.sif`");

		String verbosityLevel = arguments.getVerbosity();
		verbosity = (verbosityLevel == null) ? 3 : Integer.parseInt(verbosityLevel);

		// infer the input dir from .sif file and create a new work dir
		String inputDirectory = new File(networkFile).getParent();

		DateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss");
		String dateStr = dateFormat.format(Calendar.getInstance().getTime());
		workDirectory = new File(inputDirectory + "/results_" + dateStr).getAbsolutePath();
		createDirectory(workDirectory);

		attractors = arguments.getAttractors();

		if ((attractors != null) &&
			(!attractors.equals("fixpoints")) &&
			(!attractors.equals("trapspaces"))) {
			throw new ConfigurationException("Attractors can only be: `fixpoints` or `trapspaces` or absent");
		}
	}

	private void initLogger() throws IOException {
		String filenameOutput = "log";
		logger = new Logger(filenameOutput, workDirectory, verbosity, true);
	}

	private BooleanModel initModel() throws Exception {
		GeneralModel generalModel = new GeneralModel(logger);

		generalModel.loadInteractionsFile(networkFile);
		generalModel.buildMultipleInteractions();

		String attractorTool;
		if (attractors == null || attractors.equals("fixpoints"))
			attractorTool = "biolqm_stable_states";
		else
			attractorTool = "biolqm_trapspaces";

		return new BooleanModel(generalModel, attractorTool, logger);
	}

	private void printTotalNumberOfModels(BooleanModel model) {
		int count = 0;

		for (BooleanEquation booleanEquation : model.getBooleanEquations()) {
			String link = booleanEquation.getLink();
			if (link.equals("and") || link.equals("or")) {
				count++;
			}
		}

		logger.outputHeader(3, "Total number of models: " + (int) Math.pow(2.0, count));
	}

	private ArrayList<BooleanModel> genModels(BooleanModel booleanModel) throws Exception {
		logger.outputHeader(3, "Model Generation");
		ArrayList<BooleanModel> modelList = new ArrayList<>();

		String baseName = booleanModel.getModelName();
		int modelNumber = 0;
		booleanModel.setModelName(baseName + "_" + modelNumber);
		modelList.add(booleanModel);
		logger.outputStringMessage(3, "Adding initial model No. " + modelNumber);

		int index = 0;
		for (BooleanEquation booleanEquation : booleanModel.getBooleanEquations()) {
			String link = booleanEquation.getLink();
			if (link.equals("and") || link.equals("or")) {
				ArrayList<BooleanModel> newModels = new ArrayList<>();

				for (BooleanModel model: modelList) {
					BooleanModel newModel = new BooleanModel(model, logger);
					modelNumber++;
					newModel.setModelName(baseName + "_" + modelNumber);
					newModel.changeLinkOperator(index);
					logger.outputStringMessage(3, "Adding model No. " + modelNumber);
					newModels.add(newModel);
				}

				modelList.addAll(newModels);
			}
			index++;
		}

		return modelList;
	}

	private void calculateModelAttractors(ArrayList<BooleanModel> models) throws Exception {
		if (attractors != null) {
			logger.outputHeader(3, "Attractor calculation");

			for (BooleanModel model : models) {
				model.calculateAttractors(workDirectory);
			}
		}
	}

	private void exportModels(ArrayList<BooleanModel> models) throws IOException {
		logger.outputHeader(3, "Model Export");

		String modelsDirectory = new File(workDirectory + "/models").getAbsolutePath();
		createDirectory(modelsDirectory, logger);

		for (BooleanModel model : models) {
			model.exportModelToGitsbeFile(modelsDirectory);
			model.exportModelToBoolNetFile(modelsDirectory);
		}
	}
}
