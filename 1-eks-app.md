# Running Application on EKS

Please complete [Environment Setup](0-environment-setup.md) before start this section.

## 1. Create an ECR Repository

1.1 In the AWS Management Console, navigate to `Amazon ECR`.

<img src="./images/navigate_to_ecr.png" width=80%/>

1.2 Click on `Get Started` to create a repository.

<img src="./images/ecr.png" width=80%/>

1.3 Create an ECR repository `hello-app`. 

<img src="./images/erc_create_repo_1.png" width=80%/>

1.4 Under Image scan setting, enable Scan on push, and enable KMS encryption.

<img src="./images/erc_create_repo_2.png" width=80%/>

1.5 Once the repository is created, select the repository and click `View push commands`

<img src="./images/erc_repo_created.png" width=80%/>

You'd see the commands to authenticate, tag, and push your container immages to the ECR repository.

<img src="./images/erc_repo_push.png" width=80%/>

---

## 2. Clone application and build image

2.1 Clone sample application

```sh
git clone https://github.com/build-on-aws/instrumenting-java-apps-using-opentelemetry.git -b build-on-aws-tutorial
```

2.2 Create Dockerfile

```sh
cd ~/environment/instrumenting-java-apps-using-opentelemetry
touch Dockerfile
```

2.3 Update content in docker file

```sh
FROM maven:3.8.1-openjdk-17-slim

VOLUME /tmp
ADD . /usr/src/app
WORKDIR /usr/src/app

RUN mvn clean package -DskipTests

ENTRYPOINT [ "java", "-jar", "target/hello-app-1.0.jar" ]
```

2.4 Build to docker image

```sh
docker build -t hello-app:latest .
```

## 3. Tag and push your container image to the ECR repository

In your Cloud9 environment, follow the instructions from the push commands.

3.1 Authenticate your Docker client to your registry

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

3.2 Tag your `hello-app` image built in the previous section

```sh
docker tag hello-app:latest ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/hello-app:latest
```

3.3 Push the image to ECR
```sh
docker push ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/hello-app:latest
```

## 4. Deploy application

4.1 Create `hello-app` namespace

```sh
cd ~/environment
kubectl apply -f ~/environment/adot-eks/workshop/1-eks-app/namespace.yaml 
```

4.2 Create `hello-app` pod and service

```sh
sed -i -e s/\<AWS_REGION\>/${AWS_REGION}/g -e s/\<ACCOUNT_ID\>/${ACCOUNT_ID}/g ~/environment/adot-eks/workshop/1-eks-app/hello-app/deployment.yaml
kubectl apply -f ~/environment/adot-eks/workshop/1-eks-app/hello-app
```
##### Result Output
```
deployment.apps/hello-app created
service/hello-app created
serviceaccount/hello-app created
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

4.3 Check that application is ready with the following command

```sh
kubectl get po -n hello-app
```
##### Result Output
```
NAME                         READY   STATUS    RESTARTS   AGE
hello-app-5887979795-8ldn7   1/1     Running   0          10s
```

4.4 Check `hello-app` log

```sh
export HELLO_APP_POD_NAME=$(kubectl get pods -n hello-app -o jsonpath='{.items[].metadata.name}')
kubectl logs -f ${HELLO_APP_POD_NAME} -n hello-app
```
##### Result Output
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.1.2)

2023-10-16T09:30:11.432Z  INFO 1 --- [           main] tutorial.buildon.aws.o11y.HelloApp       : Starting HelloApp v1.0 using Java 17 with PID 1 (/usr/src/app/target/hello-app-1.0.jar started by root in /usr/src/app)
2023-10-16T09:30:11.438Z  INFO 1 --- [           main] tutorial.buildon.aws.o11y.HelloApp       : No active profile set, falling back to 1 default profile: "default"
2023-10-16T09:30:13.966Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8888 (http)
2023-10-16T09:30:13.983Z  INFO 1 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2023-10-16T09:30:13.983Z  INFO 1 --- [           main] o.apache.catalina.core.StandardEngine    : Starting Servlet engine: [Apache Tomcat/10.1.11]
2023-10-16T09:30:14.172Z  INFO 1 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2023-10-16T09:30:14.173Z  INFO 1 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 2555 ms
2023-10-16T09:30:15.481Z  INFO 1 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 1 endpoint(s) beneath base path '/actuator'
2023-10-16T09:30:15.612Z  INFO 1 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8888 (http) with context path ''
2023-10-16T09:30:15.641Z  INFO 1 --- [           main] tutorial.buildon.aws.o11y.HelloApp       : Started HelloApp in 5.137 seconds (process running for 6.566)
```

4.5 Open `new Terminal`

<img src="./images/cloud9_new_terminal.png" width=80%/>

4.6 Access to `hello-app` pod and invoke the API

```sh
export HELLO_APP_POD_NAME=$(kubectl get pods -n hello-app -o jsonpath='{.items[].metadata.name}')
kubectl exec -it ${HELLO_APP_POD_NAME}  -n hello-app -- sh
```

4.7 Invoke API inside Pod `sh`
```sh
curl -X GET http://localhost:8888/hello
```
##### Result Output
```
{"message":"Hello World"}
```

4.8 On `logging` tab, you will see output message `The response is valid.` as below

```
2023-10-16T09:31:27.604Z  INFO 1 --- [nio-8888-exec-1] t.buildon.aws.o11y.HelloAppController    : The response is valid.
```

Congratulations!! You have completed this section. Please continue on [Using OpenTelemetry Agent for Automatic instrument](2-eks-app-otel-agent.md)

---

## References
- [Containers Zero to One Workshop](https://catalog.us-east-1.prod.workshops.aws/workshops/613d57e0-6ae0-4fdc-bdb8-dac3930c2ec9/en-US)
- [Instrumenting Java Applications with OpenTelemetry](https://community.aws/tutorials/instrumenting-java-apps-using-opentelemetry)
---