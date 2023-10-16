This workshop built on top of 
- [One Observability Workshop](https://catalog.workshops.aws/observability/en-US) https://catalog.workshops.aws/observability/en-US
- [Instrumenting Java Applications with OpenTelemetry](https://community.aws/tutorials/instrumenting-java-apps-using-opentelemetry)https://community.aws/tutorials/instrumenting-java-apps-using-opentelemetry

# AWS Distro for OpenTelemetry on EKS

Welcome to `AWS Distro for OpenTelemetry on EKS` workshop. This workshop aims to provide developers and DevOps engineers with hands-on practice to setup AWS Distro for OpenTelemetry Add-ons then trace and me. We start from containerizing an applicaiton, to deploying a practical containerized application on AWS. We will explore how to `Auto` and `Manual` Instrumentation for `Traces` and `Metrics` and integrate with `AWS X-Ray` and `Amazon CloudWatch Metrics`

---
## Takeaways
By the end of this workshop you will have learned the following:

- Understand how to install `AWS Distro for OpenTelemetry` by using `EKS Add-Ons`
- Understand how to coonfig `OpenTelemetry Collector`
- Understand how to set up `Auto` Instrumentation for `Traces` to `AWS X-Ray`
- Understand how to set up `Auto` Instrumentation for `Metrics` to `Amazon CloudWatch Metrics`
- Understand how to set up `Manual` Instrumentation for `Traces` to `AWS X-Ray`
- Understand how to set up `Manual` Instrumentation for `Metrics` to `Amazon CloudWatch Metrics`

---

## Table of contents

Prerequisite: [Environment Setup](0-environment-setup.md)

1. [Running Application on EKS](1-eks-app.md)
2. [Using OpenTelemetry Agent for Automatic instrument](2-eks-app-otel-agent.md)
3. [AWS Distro for OpenTelemetry using EKS Add-Ons Installation](3-eks-adot-add-on.md)
4. [Automatic Instrumentation (Traces to AWS X-Ray)](4-auto-trace-x-ray.md)
5. [Automatic Instrumentation (Metrics to CloudWatch Metrics)](5-auto-metrics-cloudwatch.md)
6. [Environment Variable](6-environment-variable.md)
7. [Manual Instrumentation (Traces to AWS X-Ray)](7-manual-trace-x-ray.md)
8. [Manual Instrumentation (Metrics to CloudWatch Metrics](8-manual-metrics-cloudwatch.md)
9. [Tracing with Micronaut Framework](9-tracing-with-micronaut.md)

---
