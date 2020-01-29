package eu.druglogics.abmlog;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

@Parameters(separators = "=")
public class CommandLineArgs {

	@Parameter
	private List<String> parameters = new ArrayList<>();

	@Parameter(names = { "--file", "-f" }, required = true,
		description = "Input .sif network/topology file", order = 0)
	private String networkFile;

	@Parameter(names = { "--attractors", "-a"}, description =
		"What kind of attractors to calculate. Can be only: `fixpoints` or `trapspaces`."
			+ " If not specified, only stable states (fixpoints) will be calculated for the generated "
			+ "models", order = 1)
	private String attractors;

	@Parameter(names = { "--verbosity", "-v" }, description = "Logger verbosity (0 = nothing, " +
		"3 = Everything)", order = 2)
	private String verbosity;

	public String getNetworkFile() {
		return networkFile;
	}

	public String getAttractors() {
		return attractors;
	}

	public String getVerbosity() {
		return verbosity;
	}
}
