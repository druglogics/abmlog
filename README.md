# abmlog

This module's name is an acronym for: *All possible boolean models link operator generator*.

The only required input is a **single interactions file** which describes the network topology.
Using that file, we build a boolean model whose equations are based on the following format:
`A *= ( B or C or ... ) and not ( D or E or ... )`, translating thus lines from the *sif* file like e.g. `B -> A` and `D -| A`.
Then we generate every possible boolean model out of the initial one, by changing the *link operator* (`and not` or 
`or not`) between the activator and inhibitor regulators in every possible permutation.

For models that have a large number of equations with a link operator, making thus the generation of all possible link operator models infeasible/untractable, we provide a *random* model generator.

# Install

First, [install Gitsbe](https://druglogics.github.io/druglogics-doc/gitsbe-install.html). 
Then run:

```
git clone https://github.com/druglogics/abmlog.git
mvn clean install
```

# Examples

We now provide some examples using the `BooleanModelGenerator` and `RandomBooleanModelGenerator`.

Get the list of all **provided user options**:
```
java -cp target/abmlog-1.5.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator
```

The example below will generate all the possible boolean models and export them with **no calculation of attractors**:
```
java -cp target/abmlog-1.5.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif
```

The next example will generate the models and also calculate the **fixpoints (stable states)**:
```
java -cp target/abmlog-1.5.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=fixpoints
```

The examples above **use only 1 core** to generate the models, but this job can be **parallelized** by giving an extra parameter `--parallel` (so that all available cores are used):
```
java -cp target/abmlog-1.5.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=fixpoints --parallel
```

The example below will generate the models and also calculate the **minimal trapspaces**:
```
java -cp target/abmlog-1.5.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=trapspaces
```

---

For a boolean model that has e.g. 23 boolean equations with a link operator, generating all *2^23* possible link operator permutated models might be very challenging task (time-wise, space-wise, etc.).
Another case is that we may just want a *sample* out of the pool of all possible models.
These use cases are covered by a simple random boolean model generator that produces structurally different models based on link operator mutations.

Generating 100 models from the input network:

```
java -cp target/abmlog-1.5.0-jar-with-dependencies.jar eu.druglogics.abmlog.RandomBooleanModelGenerator --file=test/network.sif --num=100
```

All attractors are calculated using the [BioLQM](https://github.com/colomoto/bioLQM) library.
The result models are saved in both **BoolNet** (.bnet) and **Gitsbe** (.gitsbe) formats.

## Output 

For both model generators, the output consists of a `results_<network_file>_<date>` directory which holds the `log` file(s) and a `models` directory where the models are saved.

In the case of the `BooleanModelGenerator`, we split the `models` directory to several **if the amount of models exceeds 100000** thus avoiding filesystem issues that may arise.
For large models, always try to use a machine with as many cores as possible (and the `--parallel` option of course) as well as check that the number of *inodes* (for Linux systems) is enough to store the total amount of models that will be generated (this information is outputed on the first lines of the main `log` file).

