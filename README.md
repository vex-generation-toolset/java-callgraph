# java-callgraph

## Prerequisites
1. Java 17
```sh
sudo apt install openjdk-17-jdk
```
2. [maven 3.9.11](https://maven.apache.org/install.html)
3. [Eclipse IDE for Eclipse Committers (2023-12)](https://www.eclipse.org/downloads/packages/release/2023-12/r/eclipse-ide-eclipse-committers)

## Clone the repository:
```
git clone git@github.com:vex-generation-toolset/java-callgraph.git
```

## Build (maven):
```sh
cd java-callgraph
./mvnw clean verify
```

## Import into Eclipse

1. Launch Eclipse and create (or open) a workspace.
2. Go to **File ▸ Import… ▸ Maven ▸ Existing Maven Projects**.
3. Browse to the project root directory (`java-callgraph`) and finish the import.
4. In the Project Explorer, open `target-platform.target`.

    * The Target Definition editor opens.
    * Click **Set as Active Target Platform** (upper right corner).

## Run applicaton from eclipse
To run `java-callgraph` on a java project, first create a `config.json` inside eclipse directory:
The structure of the `config.json` file should be like this:
```json
{
    "source" : "/path/to/source",
    "result" : "/path/to/result",
    "summaries" : "/path/to/java-callgraph/summaries",
    "debug" : true
}
```
Here
- `source` is the path of the java project we want to analyze and build callgraph for.
- `result` indicates the directory where the generated callgraph will be stored in json format.
- `summaries` path to the summaries directory inside the project (predefined).
- `debug` field can be used to get additional logs.

After creating `config.json`, go to eclipse editor.
- In the `Package Explorer` right click on the project `org.openrafactory.callgraph`.
- From the menu, `Run as` -> `Eclipse Application`.
- If any warning popup is shown, ignore and click `Continue`.
Then the run will start and logs will be shown in the `console` tab.

## Testing

To run a particular test suite (like CallGraphTestSuite):
- Open the test suite class in eclipse editor.
- Right click on the file name from `Package Explorer`.
- From the menu, hover on `Run as` and select `Run Configurations...`.
- A `Run Configurations` will open up and the test suite will be selected under `Junit Plug-in Test`.
- From the `Test` tab, select `JUnit 3` as `Test runner` and untick `Run in UI thread`.
- Next in the `Main` tab, select `Run an application` and from the drop-down select `[No Application] - Headless Mode`.
- Now click `Run` button to run the test suite.

The above steps is only necessary for running a test suite for the first time. For subsequent run, you can just select the test suite
configuration to run that test suite.

## Docker Build and Run
To build docker image, run (may require `sudo`):
```sh
cd java-callgraph
./build_image.sh -i java-cg:latest
```

Next, run the docker container. The source code needs to be mounted at the `/workspace/source` directory.
The call-graph is generated at the `/workspace/result` directory. Mount a directory at `/workspace/result` for the call-graph to persist. 
```sh
sudo docker run --rm -v <path/to/source>:/workspace/source \
    -v <path/to/result>:/workspace/result \
    java-cg:latest
```
