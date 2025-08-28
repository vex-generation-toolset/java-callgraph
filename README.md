# icr-java-cg-transition

## Docker Build and Run
To build docker image, run:
```sh
cd icr-java-cg-transition
mvn -DskipTests clean install
cd Docker && sudo ./build_image.sh -i icr-cg:latest
```
Next, run the docker container:
```sh
docker run --rm -v /home/nhasan/ORExtras/JavaSource/sagan:/workspace/source \
    -v /home/nhasan/ORExtras/JavaSource/Result/sagan:/workspace/result \
    icr-cg:latest
```