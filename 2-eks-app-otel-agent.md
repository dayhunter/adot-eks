# Using OpenTelemetry Agent for Automatic instrument

## 1. Adding OpenTelemetry Agent to dockerfile

1.1 Update Dockerfile

```sh
cd instrumenting-java-apps-using-opentelemetry
```

1.2 Update content in `Dockerfile`

```sh
FROM maven:3.8.1-openjdk-17-slim

VOLUME /tmp
ADD . /usr/src/app
WORKDIR /usr/src/app

RUN mvn clean package -DskipTests

RUN curl -L https://github.com/aws-observability/aws-otel-java-instrumentation/releases/download/v1.30.0/aws-opentelemetry-agent.jar --output opentelemetry-javaagent-all.jar

ENTRYPOINT [ "java", "-javaagent:opentelemetry-javaagent-all.jar", "-jar", "target/hello-app-1.0.jar" ]
```

1.3 Build to docker image

```sh
docker build -t hello-app:latest .
```

## 2. Tag and push your container image to the ECR repository

In your Cloud9 environment, follow the instructions from the push commands.

2.1 Authenticate your Docker client to your registry

```sh
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
```
##### Result Output
```
WARNING! Your password will be stored unencrypted in /home/ec2-user/.docker/config.json.
Configure a credential helper to remove this warning. See
https://docs.docker.com/engine/reference/commandline/login/#credentials-store

Login Succeeded
```

2.2 Tag your `hello-app` image built in the previous section

```sh
docker tag hello-app:latest ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/hello-app:latest
```

2.3 Push the image to ECR
```sh
docker push ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/hello-app:latest
```

## 3. Deploy application

3.1 Create `hello-app` pod and service

```sh
kubectl apply -f ~/environment/workshop/2-eks-app-otel-agent/hello-app
```
##### Result Output
```
deployment.apps/hello-app configured
service/hello-app unchanged
serviceaccount/hello-app unchanged
```

3.2 Check that application is ready with the following command

```sh
kubectl get po -n hello-app
```
##### Result Output
```
NAME                         READY   STATUS    RESTARTS   AGE
hello-app-5887979795-8ldn7   1/1     Running   0          10s
```

3.3 Check `hello-app` log

```sh
export HELLO_APP_POD_NAME=$(kubectl get pods -n hello-app -o jsonpath='{.items[].metadata.name}')
kubectl logs -f ${HELLO_APP_POD_NAME} -n hello-app
```

3.4 Open `new Terminal`

<img src="./images/cloud9_new_terminal.png" width=80%/>

3.5 Access to `hello-app` pod and invoke the API

```sh
export HELLO_APP_POD_NAME=$(kubectl get pods -n hello-app -o jsonpath='{.items[].metadata.name}')
kubectl exec -it ${HELLO_APP_POD_NAME}  -n hello-app -- sh
```

3.6 Invoke API inside Pod `sh`
```sh
curl -X GET http://localhost:8888/hello
```
##### Result Output
```
{"message":"Hello World"}
```

3.7 On `logging` tab, you will see output as below

```
[otel.javaagent 2023-10-15 10:24:18:040 +0000] [PeriodicMetricReader-1] INFO io.opentelemetry.exporter.logging.LoggingMetricExporter - metric: ImmutableMetricData{resource=Resource{schemaUrl=https://opentelemetry.io/schemas/1.21.0, attributes={cloud.account.id="273168336574", cloud.availability_zone="us-east-2b", cloud.platform="aws_ec2", cloud.provider="aws", cloud.region="us-east-2", container.id="93b3751859bcff98b7c188af6e9ce6f02e5e2d1249950ea8680fbae63e747d7a", host.arch="amd64", host.id="i-04f1db0dd4bc261ae", host.image.id="ami-03412347265cf2f1a", host.name="ip-11-0-226-54.us-east-2.compute.internal", host.type="t3.medium", os.description="Linux 5.10.192-183.736.amzn2.x86_64", os.type="linux", process.command_args=[/usr/local/openjdk-17/bin/java, -javaagent:opentelemetry-javaagent-all.jar, -jar, target/hello-app-1.0.jar], process.executable.path="/usr/local/openjdk-17/bin/java", process.pid=1, process.runtime.description="Oracle Corporation OpenJDK 64-Bit Server VM 17+35-2724", process.runtime.name="OpenJDK Runtime Environment", process.runtime.version="17+35-2724", service.name="hello-app", telemetry.auto.version="1.30.0-aws", telemetry.sdk.language="java", telemetry.sdk.name="opentelemetry", telemetry.sdk.version="1.30.1"}}, instrumentationScopeInfo=InstrumentationScopeInfo{name=io.opentelemetry.runtime-telemetry-java8, version=1.30.0-alpha, schemaUrl=null, attributes={}}, name=process.runtime.jvm.buffer.limit, description=Total capacity of the buffers in this pool, unit=By, type=LONG_SUM, data=ImmutableSumData{points=[ImmutableLongPointData{startEpochNanos=1697365397949943552, epochNanos=1697365457959440633, attributes={pool="direct"}, value=8556, exemplars=[]}, ImmutableLongPointData{startEpochNanos=1697365397949943552, epochNanos=1697365457959440633, attributes={pool="mapped"}, value=0, exemplars=[]}, ImmutableLongPointData{startEpochNanos=1697365397949943552, epochNanos=1697365457959440633, attributes={pool="mapped - 'non-volatile memory'"}, value=0, exemplars=[]}], monotonic=false, aggregationTemporality=CUMULATIVE}}
2023-10-15T10:24:23.662Z  INFO 1 --- [nio-8888-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2023-10-15T10:24:23.665Z  INFO 1 --- [nio-8888-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2023-10-15T10:24:23.668Z  INFO 1 --- [nio-8888-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 2 ms
2023-10-15T10:24:23.769Z  INFO 1 --- [nio-8888-exec-1] t.buildon.aws.o11y.HelloAppController    : The response is valid.
[otel.javaagent 2023-10-15 10:24:23:871 +0000] [http-nio-8888-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'HelloAppController.hello' : 652bbdd7e83f8d62edac21f875655018 01d9392b326d940a INTERNAL [tracer: io.opentelemetry.spring-webmvc-6.0:1.30.0-alpha] AttributesMap{data={thread.id=26, thread.name=http-nio-8888-exec-1}, capacity=128, totalAddedValues=2}
[otel.javaagent 2023-10-15 10:24:23:881 +0000] [http-nio-8888-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /hello' : 652bbdd7e83f8d62edac21f875655018 2976ccb4bb1487f6 SERVER [tracer: io.opentelemetry.tomcat-10.0:1.30.0-alpha] AttributesMap{data={thread.id=26, http.target=/hello, net.sock.peer.addr=0:0:0:0:0:0:0:1, net.sock.peer.port=51808, user_agent.original=curl/7.64.0, net.host.name=localhost, thread.name=http-nio-8888-exec-1, net.host.port=8888, http.status_code=200, net.sock.host.addr=0:0:0:0:0:0:0:1, http.route=/hello, http.method=GET, http.scheme=http, net.protocol.version=1.1, net.protocol.name=http}, capacity=128, totalAddedValues=15}
```

Congratulations!! You have completed this section. Please continue on [Running Application on EKS](3-eks-add-on.md)

---

## References
- [Containers Zero to One Workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/613d57e0-6ae0-4fdc-bdb8-dac3930c2ec9/en-US)
---