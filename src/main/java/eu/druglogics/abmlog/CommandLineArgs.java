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

	@Parameter(names = { "--attractors", "-a" }, description =
		"What kind of attractors to calculate. Can be only: `fixpoints` or `trapspaces`. "
			+ "If not specified, no attractors will be calculated", order = 1)
	private String attractors = null;

	@Parameter(names = { "--verbosity", "-v" }, description = "Logger verbosity (0 = nothing, " +
		"3 = Everything)", order = 2)
	private String verbosity = "3";

	@Parameter(names = { "--parallel", "-p" }, description = "Should we use all the available cores?",
		order = 3)
	private boolean parallel = false;

	public String getNetworkFile() {
		return networkFile;
	}

	public String getAttractors() {
		return attractors;
	}

	public String getVerbosity() {
		return verbosity;
	}

	public boolean getParallel() {
		return parallel;
	}
}
