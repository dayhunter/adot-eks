# AWS Distro for OpenTelemetry using EKS Add-Ons Installation

Welcome to the getting started guide for AWS Distro for OpenTelemetry (ADOT) using Elastic Kubernetes Service (EKS) add-ons. This guide shows you how to leverage Amazon EKS add-ons to install and manage ADOT within your Amazon EKS cluster.

<img src="./images/adot.png" width=80%/>

The ADOT Operator detects the presence of or changes for the `OpenTelemetryCollector` resource. For any such change, the ADOT Operator performs the following actions:

Verifies that all the required connections for these creation, update, or deletion requests to the Kubernetes API server are available.
Deploys ADOT Collector instances in the way the user expressed in the `OpenTelemetryCollector` resource configuration.
The diagram below shows how the Collector CR request flows from the customer to the ADOT Operator to deploy the ADOT Collector.

<img src="./images/adot_collector.png" width=80%/>

---

## 1. Check EKS ADOT add-ons

1.1 You can see ADOT add-on available versions.
```
aws eks describe-addon-versions --kubernetes-version 1.27 --addon-name adot \
    --query 'addons[].addonVersions[].{Version: addonVersion, Defaultversion: compatibilities[0].defaultVersion}' --output table
```
##### Result Output
```
------------------------------------------
|          DescribeAddonVersions         |
+-----------------+----------------------+
| Defaultversion  |       Version        |
+-----------------+----------------------+
|  True           |  v0.82.0-eksbuild.1  |
|  False          |  v0.80.0-eksbuild.2  |
|  False          |  v0.80.0-eksbuild.1  |
|  False          |  v0.78.0-eksbuild.2  |
|  False          |  v0.78.0-eksbuild.1  |
|  False          |  v0.76.1-eksbuild.1  |
|  False          |  v0.74.0-eksbuild.1  |
+-----------------+----------------------+
```

1.2 Determine the current add-ons installed on your cluster

```sh
EKS_CLUSTER_NAME=PetSite
eksctl get addon --cluster $EKS_CLUSTER_NAME
```
##### Result Output
```
2023-10-14 08:28:51 [ℹ]  Kubernetes version "1.27" in use by cluster "PetSite"
2023-10-14 08:28:51 [ℹ]  getting all addons
No addons found
```

1.3 You also can check via AWS Console

<img src="./images/eks_adot_add_on.png" width=80%/>

---

## 2. ADOT Operator

The ADOT Operator uses admission webhooks to mutate and validate the Collector Custom Resource (CR) requests. The [cert-manager](https://cert-manager.io/docs/) will generate a self-signed certificate.

2.1 Install cert-manager with the command:

```sh
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.8.2/cert-manager.yaml
```
##### Result Output
```
namespace/cert-manager created
customresourcedefinition.apiextensions.k8s.io/certificaterequests.cert-manager.io created
customresourcedefinition.apiextensions.k8s.io/certificates.cert-manager.io created
.
.
.
mutatingwebhookconfiguration.admissionregistration.k8s.io/cert-manager-webhook created
validatingwebhookconfiguration.admissionregistration.k8s.io/cert-manager-webhook created
```

2.2 Check that cert-manager is ready with the following command:

```sh
kubectl get pod -n cert-manager
```
##### Result Output
```
NAME                                       READY   STATUS    RESTARTS   AGE
cert-manager-7cd8798b85-brssl              1/1     Running   0          5m57s
cert-manager-cainjector-8699cf859b-4slck   1/1     Running   0          5m57s
cert-manager-webhook-85f6989b69-gxfdf      1/1     Running   0          5m57s
```

2.3 Apply the necessary permissions for ADOT to your cluster with the command:

```sh
kubectl apply -f https://amazon-eks.s3.amazonaws.com/docs/addons-otel-permissions.yaml
```
##### Result Output
```
namespace/opentelemetry-operator-system created
clusterrole.rbac.authorization.k8s.io/eks:addon-manager-otel created
clusterrolebinding.rbac.authorization.k8s.io/eks:addon-manager-otel created
role.rbac.authorization.k8s.io/eks:addon-manager created
rolebinding.rbac.authorization.k8s.io/eks:addon-manager created
```

2.4 Install the ADOT Operator into your Amazon EKS cluster using the command:

```
aws eks create-addon --addon-name adot --cluster-name $EKS_CLUSTER_NAME
```
##### Result Output
```
{
    "addon": {
        "addonName": "adot",
        "clusterName": "PetSite",
        "status": "CREATING",
        "addonVersion": "v0.82.0-eksbuild.1",
        "health": {
            "issues": []
        },
        "addonArn": "arn:aws:eks:us-east-2:273168336574:addon/PetSite/adot/68c5974a-5e58-17b7-bf67-020134a7e20e",
        "createdAt": "2023-10-14T14:26:21.681000+00:00",
        "modifiedAt": "2023-10-14T14:26:21.706000+00:00",
        "tags": {}
    }
}
```
The status field value will be CREATING until complete.

Verify that ADOT is installed and running with the command:

```sh
aws eks describe-addon --addon-name adot --cluster-name $EKS_CLUSTER_NAME
```

##### Result Output
```
{
    "addon": {
        "addonName": "adot",
        "clusterName": "PetSite",
        "status": "ACTIVE",
        "addonVersion": "v0.82.0-eksbuild.1",
        "health": {
            "issues": []
        },
        "addonArn": "arn:aws:eks:us-east-2:273168336574:addon/PetSite/adot/68c5974a-5e58-17b7-bf67-020134a7e20e",
        "createdAt": "2023-10-14T14:26:21.681000+00:00",
        "modifiedAt": "2023-10-14T14:26:41.186000+00:00",
        "tags": {}
    }
}
```

You'll see "status": "ACTIVE" when creation is complete.

2.5 You also can check via AWS Console

<img src="./images/eks_adot_add_on_installed.png" width=80%/>
---

## 3. ADOT Collector

Once the ADOT EKS Add-On is running, you can deploy the ADOT Collector into your EKS cluster. The ADOT Collector can be deployed in one of four modes: Deployment, Daemonset, StatefulSet, and Sidecar. Each mode is briefly described below.

3.1 Use your IAM role to launch the ADOT Collector
You can associate your IAM role to your EKS service account using IRSA. Your service account can then provide AWS permissions to the containers you run in any pod that use that service account.

```sh
eksctl create iamserviceaccount \
    --name adot-collector \
    --namespace otel \
    --cluster $EKS_CLUSTER_NAME \
    --attach-policy-arn arn:aws:iam::aws:policy/AmazonPrometheusRemoteWriteAccess \
    --attach-policy-arn arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess \
    --attach-policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy \
    --approve \
    --override-existing-serviceaccounts
```

**Note:**
you only need to attach the policies for that service:
arn:aws:iam::aws:policy/AmazonPrometheusRemoteWriteAccess grants write access to the Prometheus service.
arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess grants write access to the AWS X-Ray service.
arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy grants access to write the CloudWatch service.

3.2 Deploy the ADOT Collector

```sh
cd ~/environment/workshop/3-eks-adot-add-on/
sed -i -e s/AWS_REGION/$AWS_REGION/g otel-collector-config.yaml
kubectl apply -f otel-collector-config.yaml
```
##### Result Output
```sh
opentelemetrycollector.opentelemetry.io/my-adot-collector created
```

3.3 Opentelemetry Configuration Details

<img src="./images/otel_collector.png" width=80%/>

```yaml
apiVersion: opentelemetry.io/v1alpha1
kind: OpenTelemetryCollector # Custom Resource Definition
metadata:
  name: adot
  namespace: otel
spec:
  image: public.ecr.aws/aws-observability/aws-otel-collector:latest
  mode: deployment # daemonset, statefulset, sidecar
  serviceAccount: adot-collector # Service account for use with Collector
  config: |
    receivers:
      otlp:
        protocols:
          grpc:
            endpoint: 0.0.0.0:4317
    processors:

    exporters:
      awsxray:
        region: us-east-2

    service:
      pipelines:
        traces:
          receivers: [otlp]
          processors: []
          exporters: [awsxray]
```

**Note** This collector we will be using on next section. to send trace data to AWS X-Ray

3.4 Describe Opentelemetry Collector Pod

```sh
export OTEL_COLLECTOR_POD_NAME=$(kubectl get pods -n otel -o jsonpath='{.items[].metadata.name}')
kubectl describe pod $OTEL_COLLECTOR_POD_NAME -n otel
```

```
Name:             my-adot-collector-collector-f8f976c4-lr7x9
Namespace:        otel
Priority:         0
Service Account:  adot-collector
Node:             ip-11-0-226-54.us-east-2.compute.internal/11.0.226.54
Start Time:       Sat, 14 Oct 2023 15:04:16 +0000
Labels:           app.kubernetes.io/component=opentelemetry-collector
                  app.kubernetes.io/instance=otel.my-adot-collector
                  app.kubernetes.io/managed-by=opentelemetry-operator
                  app.kubernetes.io/name=my-adot-collector-collector
                  app.kubernetes.io/part-of=opentelemetry
                  app.kubernetes.io/version=latest
                  pod-template-hash=f8f976c4
Annotations:      opentelemetry-operator-config/sha256: f47327fdfe6c3a16880e7bef550db9d5231e1ba257cec62cbc3febdc587944e1
                  prometheus.io/path: /metrics
                  prometheus.io/port: 8888
                  prometheus.io/scrape: true
Status:           Running
IP:               11.0.218.116
IPs:
  IP:           11.0.218.116
Controlled By:  ReplicaSet/my-adot-collector-collector-f8f976c4
Containers:
  otc-container:
    Container ID:  containerd://f63699cfc816502f1621f4ca493409cf68e541f88f988bc18457a165062ef5fd
    Image:         public.ecr.aws/aws-observability/aws-otel-collector:latest
    Image ID:      public.ecr.aws/aws-observability/aws-otel-collector@sha256:598b4a2c32ab3b528ebaf9926c4845168ca4d0f2a28940cbd37c21da90954aae
    Ports:         8888/TCP, 4317/TCP, 4318/TCP
    Host Ports:    0/TCP, 0/TCP, 0/TCP
    Args:
      --config=/conf/collector.yaml
    State:          Running
      Started:      Sat, 14 Oct 2023 15:04:19 +0000
    Ready:          True
    Restart Count:  0
    Environment:
      POD_NAME:                     my-adot-collector-collector-f8f976c4-lr7x9 (v1:metadata.name)
      AWS_STS_REGIONAL_ENDPOINTS:   regional
      AWS_DEFAULT_REGION:           us-east-2
      AWS_REGION:                   us-east-2
      AWS_ROLE_ARN:                 arn:aws:iam::273168336574:role/eksctl-PetSite-addon-iamserviceaccount-otel-a-Role1-XD0bcqklFJpn
      AWS_WEB_IDENTITY_TOKEN_FILE:  /var/run/secrets/eks.amazonaws.com/serviceaccount/token
    Mounts:
      /conf from otc-internal (rw)
      /var/run/secrets/eks.amazonaws.com/serviceaccount from aws-iam-token (ro)
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-99pqz (ro)
Conditions:
  Type              Status
  Initialized       True 
  Ready             True 
  ContainersReady   True 
  PodScheduled      True 
Volumes:
  aws-iam-token:
    Type:                    Projected (a volume that contains injected data from multiple sources)
    TokenExpirationSeconds:  86400
  otc-internal:
    Type:      ConfigMap (a volume populated by a ConfigMap)
    Name:      my-adot-collector-collector
    Optional:  false
  kube-api-access-99pqz:
    Type:                    Projected (a volume that contains injected data from multiple sources)
    TokenExpirationSeconds:  3607
    ConfigMapName:           kube-root-ca.crt
    ConfigMapOptional:       <nil>
    DownwardAPI:             true
QoS Class:                   BestEffort
Node-Selectors:              <none>
Tolerations:                 node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
                             node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
Events:
  Type    Reason     Age   From               Message
  ----    ------     ----  ----               -------
  Normal  Scheduled  8s    default-scheduler  Successfully assigned otel/my-adot-collector-collector-f8f976c4-lr7x9 to ip-11-0-226-54.us-east-2.compute.internal
  Normal  Pulling    7s    kubelet            Pulling image "public.ecr.aws/aws-observability/aws-otel-collector:latest"
  Normal  Pulled     5s    kubelet            Successfully pulled image "public.ecr.aws/aws-observability/aws-otel-collector:latest" in 2.054673412s (2.054687894s including waiting)
  Normal  Created    5s    kubelet            Created container otc-container
  Normal  Started    5s    kubelet            Started container otc-container
```

Congratulations!! You have completed this section. Please continue on [Running Application on EKS](2_eks_app.md)

---

## References
- [Managing Amazon EKS add-ons](https://docs.aws.amazon.com/eks/latest/userguide/managing-add-ons.html)
- [Getting Started with AWS Distro for OpenTelemetry using EKS Add-Ons](https://aws-otel.github.io/docs/getting-started/adot-eks-add-on)
- [Collector Configuration for AWS X-Ray](https://aws-otel.github.io/docs/getting-started/adot-eks-add-on/config-xray)
- [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/)
---