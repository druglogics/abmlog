package eu.druglogics.abmlog;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

@Parameters(separators = "=")
public class RandomModelGenCommandLineArgs {

	@Parameter
	private List<String> parameters = new ArrayList<>();

	@Parameter(names = { "--file", "-f" }, required = true,
		description = "Input .sif network/topology file", order = 0)
	private String networkFile;

	@Parameter(names = { "--num", "-n"}, required = true,
		description = "How many random models should be generated?", order = 1)
	private Integer randomModelsNumber;

	@Parameter(names = { "--seed", "-s" },
		description = "Seed number for the random number generator", order = 2)
	private long seed = 0;

	@Parameter(names = { "--verbosity", "-v" }, description = "Logger verbosity (0 = nothing, " +
		"3 = Everything)", order = 3)
	private String verbosity = "3";

	public String getNetworkFile() {
		return networkFile;
	}

	public String getVerbosity() {
		return verbosity;
	}

	public Integer getRandomModelsNumber() {
		return randomModelsNumber;
	}

	public long getSeed() {
		return seed;
	}
}
