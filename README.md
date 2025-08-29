# java-callgraph

## Docker Build and Run
To build docker image, run (may require `sudo`):
```sh
cd java-callgraph
mvn -DskipTests clean package
cd Docker && ./build_image.sh -i java-cg:latest
```
Next, run the docker container. The source code needs to be mounted at the `/workspace/source` directory.
The call-graph is generated at the `/workspace/result` directory. Mount a directory at `/workspace/result` for the call-graph to persist. 
```sh
docker run --rm -v <path/to/source>:/workspace/source \
    -v <path/to/result>:/workspace/result \
    java-cg:latest
```
