# abmlog

This module's name is an acronym for: *All possible boolean models link operator generator*.

The only required input is a **single interactions file** which holds the network topology.
Using that file, we build a boolean model whose equations are based on the following format:
`A *= ( B ) or ... ) and not ( C ) or ... )`, translating thus lines from the *sif* file like: `B -> A` and `C -| A`).
Then we generate every possible boolean model out of the initial one, by changing the *link operator* (`and not` or 
`or not`) between the activator and inhibitor regulators in every possible permutation.

# Install

First, install Gitsbe (**link to be provided upon publication**).

```
git clone https://github.com/druglogics/abmlog.git
mvn clean install
```

# Examples

Get the list of all **provided user options**:
```shell script
java -cp target/abmlog-1.4.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator
```

The example below will generate all the possible boolean models and export them with **no calculation of attractors**:
```
java -cp target/abmlog-1.4.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif
```

The next example will generate the models and also calculate the **fixpoints (stable states)**:
```
java -cp target/abmlog-1.4.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=fixpoints
```

The examples above **use only 1 core** to generate the models, but this job can be **parallelized** by giving an extra parameter `--parallel` (so that all available cores are used):
```
java -cp target/abmlog-1.4.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=fixpoints --parallel
```

The example below will generate the models and also calculate the **minimal trapspaces**:
```
java -cp target/abmlog-1.4.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=trapspaces
```

All attractors are calculated using the [BioLQM](https://github.com/colomoto/bioLQM) library.
The result models are saved in both **BoolNet** (.bnet) and **Gitsbe** (.gitsbe) formats.

## Output 

The output consists of a `results_<network_file>_<date>` directory which holds the `log` files and a `models` directory where the models are saved. Note that we split the `models` directory to several **if the amount of models exceeds 100000** thus avoiding filesystem issues that may arise. For large models, always try to use a machine with as many cores as possible (and the `--parallel` option of course) as well as check that the number of *inodes* (for Linux systems) is enough to store the total amount of models that will be generated (this information is outputed on the first lines of the main `log` file).
 