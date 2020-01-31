# abmlog

This module's name is an acronym for: *All possible boolean models link operator generator*.

The only required input is a **single interactions file** which holds the network topology.
Using that file, we build a boolean model whose equations are based on the following format:
`A *= ( B ) or ... ) and not ( C ) or ... )`, translating thus lines from the *sif* file like: `B -> A` and `C -| A`).
Then we generate every possible boolean model out of the initial one, by changing the *link operator* (`and not` or 
`or not`) between the activator and inhibitor regulators in every possible permutation.

# Install

```
git clone https://github.com/druglogics/abmlog.git
mvn clean install
```

# Examples

Get the list of all **provided user options**:
```shell script
java -cp target/abmlog-1.2.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator
```

The below example will generate all the possible boolean models and export them with **no calculation of attractors**:
```
java -cp target/abmlog-1.2.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif
```

The next example will generate the models, calculate the **fixpoints (stable states)** and export them all:
```
java -cp target/abmlog-1.2.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=fixpoints
```

The below example will generate the models and calculate the **minimal trapspaces**, but will only export those that have at least 1 trapspace (there can be models that have only the *trivial* trapspace, e.g. all nodes are dashes):
```
java -cp target/abmlog-1.2.0-jar-with-dependencies.jar eu.druglogics.abmlog.BooleanModelGenerator --file=test/test.sif --attractors=trapspaces --export=1
```

All attractors are calculated using the [BioLQM](https://github.com/colomoto/bioLQM) library.
The result models are saved in both **BoolNet** (.bnet) and **Gitsbe** (.gitsbe) formats.
