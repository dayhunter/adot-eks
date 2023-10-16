# Using OpenTelemetry Agent for Automatic instrument

Please complete [Running Application on EKS](1-eks-app.md) before start this section.

## 1. Adding OpenTelemetry Agent to dockerfile

1.1 Update Dockerfile

```sh
cd ~/environment/instrumenting-java-apps-using-opentelemetry
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

3.1 Update `hello-app` deployment

```sh
cd ~/environment
sed -i -e s/\<AWS_REGION\>/${AWS_REGION}/g -e s/\<ACCOUNT_ID\>/${ACCOUNT_ID}/g ~/environment/adot-eks/workshop/2-eks-app-otel-agent/hello-app/deployment.yaml
kubectl apply -f ~/environment/adot-eks/workshop/2-eks-app-otel-agent/hello-app
```
##### Result Output
```
deployment.apps/hello-app configured
```

Deployment yaml file

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-app
  namespace: hello-app
  labels:
    app.kubernetes.io/created-by: eks-workshop
    app.kubernetes.io/type: app
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: hello-app
      app.kubernetes.io/instance: hello-app
      app.kubernetes.io/component: service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: hello-app
        app.kubernetes.io/instance: hello-app
        app.kubernetes.io/component: service
        app.kubernetes.io/created-by: eks-workshop
    spec:
      serviceAccountName: hello-app
      containers:
        - name: hello-app
          env:
            - name: OTEL_TRACES_EXPORTER
              value: logging
            - name: OTEL_METRICS_EXPORTER
              value: logging
          image: "<ACCOUNT_ID>.dkr.ecr.<AWS_REGION>.amazonaws.com/hello-app:latest"
          imagePullPolicy: Always
          ports:
            - name: http
              containerPort: 8080
              protocol: TCP
          resources:
            limits:
              memory: 1Gi
            requests:
              cpu: 250m
              memory: 1Gi
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
##### Result Output
```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
[otel.javaagent 2023-10-16 09:46:07:370 +0000] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 1.30.0-aws
[otel.javaagent 2023-10-16 09:46:08:227 +0000] [main] INFO io.opentelemetry.sdk.resources.Resource - Attempting to merge Resources with different schemaUrls. The resulting Resource will have no schemaUrl assigned. Schema 1: https://opentelemetry.io/schemas/1.21.0 Schema 2: https://opentelemetry.io/schemas/1.20.0

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.1.2)

2023-10-16T09:46:14.707Z  INFO 1 --- [           main] tutorial.buildon.aws.o11y.HelloApp       : Starting HelloApp v1.0 using Java 17 with PID 1 (/usr/src/app/target/hello-app-1.0.jar started by root in /usr/src/app)
2023-10-16T09:46:14.720Z  INFO 1 --- [           main] tutorial.buildon.aws.o11y.HelloApp       : No active profile set, falling back to 1 default profile: "default"
2023-10-16T09:46:17.750Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8888 (http)
2023-10-16T09:46:17.802Z  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2023-10-16T09:46:17.803Z  INFO 1 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.11]
2023-10-16T09:46:17.947Z  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2023-10-16T09:46:17.949Z  INFO 1 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 3064 ms
2023-10-16T09:46:19.547Z  INFO 1 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 1 endpoint(s) beneath base path '/actuator'
2023-10-16T09:46:19.816Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8888 (http) with context path ''
2023-10-16T09:46:19.853Z  INFO 1 --- [           main] tutorial.buildon.aws.o11y.HelloApp       : Started HelloApp in 6.431 seconds (process running for 12.818)
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

3.7 On `logging` tab, you will see both of `Metrics` and `Trace` show as below output:

```
[otel.javaagent 2023-10-16 09:47:08:887 +0000] [PeriodicMetricReader-1] INFO io.opentelemetry.exporter.logging.LoggingMetricExporter - metric: ImmutableMetricData{resource=Resource{schemaUrl=https://opentelemetry.io/schemas/1.21.0, attributes={cloud.account.id="538334289408", cloud.availability_zone="us-east-2b", cloud.platform="aws_ec2", cloud.provider="aws", cloud.region="us-east-2", container.id="06ec5738bdd180dfd557ec2567adc64d21575e0465c5dbc52730cffca182b6af", host.arch="amd64", host.id="i-0fd91a6a91c5f8f20", host.image.id="ami-03412347265cf2f1a", host.name="ip-11-0-224-192.us-east-2.compute.internal", host.type="t3.medium", os.description="Linux 5.10.192-183.736.amzn2.x86_64", os.type="linux", process.command_args=[/usr/local/openjdk-17/bin/java, -javaagent:opentelemetry-javaagent-all.jar, -jar, target/hello-app-1.0.jar], process.executable.path="/usr/local/openjdk-17/bin/java", process.pid=1, process.runtime.description="Oracle Corporation OpenJDK 64-Bit Server VM 17+35-2724", process.runtime.name="OpenJDK Runtime Environment", process.runtime.version="17+35-2724", service.name="hello-app", telemetry.auto.version="1.30.0-aws", telemetry.sdk.language="java", telemetry.sdk.name="opentelemetry", telemetry.sdk.version="1.30.1"}}, instrumentationScopeInfo=InstrumentationScopeInfo{name=io.opentelemetry.runtime-telemetry-java8, version=1.30.0-alpha, schemaUrl=null, attributes={}}, name=process.runtime.jvm.buffer.limit, description=Total capacity of the buffers in this pool, unit=By, type=LONG_SUM, data=ImmutableSumData{points=[ImmutableLongPointData{startEpochNanos=1697449568783149139, epochNanos=1697449628793414731, attributes={pool="direct"}, value=8556, exemplars=[]}, ImmutableLongPointData{startEpochNanos=1697449568783149139, epochNanos=1697449628793414731, attributes={pool="mapped"}, value=0, exemplars=[]}, ImmutableLongPointData{startEpochNanos=1697449568783149139, epochNanos=1697449628793414731, attributes={pool="mapped - 'non-volatile memory'"}, value=0, exemplars=[]}], monotonic=false, aggregationTemporality=CUMULATIVE}}
2023-10-16T09:47:12.106Z  INFO 1 --- [nio-8888-exec-1] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring DispatcherServlet 'dispatcherServlet'
2023-10-16T09:47:12.109Z  INFO 1 --- [nio-8888-exec-1] o.s.web.servlet.DispatcherServlet        : Initializing Servlet 'dispatcherServlet'
2023-10-16T09:47:12.118Z  INFO 1 --- [nio-8888-exec-1] o.s.web.servlet.DispatcherServlet        : Completed initialization in 3 ms
2023-10-16T09:47:12.223Z  INFO 1 --- [nio-8888-exec-1] t.buildon.aws.o11y.HelloAppController    : The response is valid.
[otel.javaagent 2023-10-16 09:47:12:338 +0000] [http-nio-8888-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'HelloAppController.hello' : 652d069f40fa8ee6d83edc40735b38dd 9db71fa9f335d549 INTERNAL [tracer: io.opentelemetry.spring-webmvc-6.0:1.30.0-alpha] AttributesMap{data={thread.id=25, thread.name=http-nio-8888-exec-1}, capacity=128, totalAddedValues=2}
[otel.javaagent 2023-10-16 09:47:12:348 +0000] [http-nio-8888-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /hello' : 652d069f40fa8ee6d83edc40735b38dd d20e43b96d3fea2b SERVER [tracer: io.opentelemetry.tomcat-10.0:1.30.0-alpha] AttributesMap{data={thread.id=25, http.target=/hello, net.sock.peer.addr=0:0:0:0:0:0:0:1, net.sock.peer.port=34082, user_agent.original=curl/7.64.0, net.host.name=localhost, thread.name=http-nio-8888-exec-1, net.host.port=8888, http.status_code=200, net.sock.host.addr=0:0:0:0:0:0:0:1, http.route=/hello, http.method=GET, http.scheme=http, net.protocol.version=1.1, net.protocol.name=http}, capacity=128, totalAddedValues=15}
```

Congratulations!! You have completed this section. Please continue on [AWS Distro for OpenTelemetry using EKS Add-Ons Installation](3-eks-adot-add-on.md)

---

## References
- [Containers Zero to One Workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/613d57e0-6ae0-4fdc-bdb8-dac3930c2ec9/en-US)
---