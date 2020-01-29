# abmlog

This module's name is an acronym for: *All possible boolean models link operator generator*.

The only required input is a **single interactions file** which holds the network topology.
Using that file, we build a boolean model whose equations are based on the following format:
`A *= ( B ) or ... ) and not ( C ) or ... )`, translating thus lines from the *sif* file like: `B -> A` and `C -| A`).
Then we generate every possible boolean model out of the initial one, by changing the *link operator* (`and not` or 
`or not`) between the activator and inhibitor regulators in every possible permutation.

# Install

```
mvn clean install
```

# Examples

The below example will generate the models with no calculation of attractors:
```
java -cp target/abmlog-1.1.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif
```

The next example will generate the models and calculate the fixpoints (stable states):
```
java -cp target/abmlog-1.1.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=fixpoints
```

The below example will generate the models and calculate the minimal trapspaces):
```
java -cp target/abmlog-1.1.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=trapspaces
```

All attractors are calculated using the [BioLQM](https://github.com/colomoto/bioLQM) library.
The result models are saved in both **BoolNet** (.bnet) and **Gitsbe** (.gitsbe) formats.