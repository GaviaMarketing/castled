<p align="center">
  <img src="https://cdn.castled.io/logo/castled_original_on_black.svg" alt="Castled-logo" width="300" />
  <p align="center">Open source reverse-ETL platform to operationalise your data warehouse</p>
</p>

<p align="center">
  <a href="https://docs.castled.io/deploying-castled/deploy-local">
    <img src="https://cdn.castled.io/content/readme/deploy_locally.svg" alt="deply locally" />
  </a>
  <a href="https://docs.castled.io/deploying-castled/deploy-on-aws-ec2">
    <img src="https://cdn.castled.io/content/readme/deploy_aws.svg" alt="deploy on aws" />
  </a>
</p>

---

## Introduction

Castled is a [Reverse ETL](https://medium.com/castled/reverse-etl-opening-the-floodgates-for-operational-analytics-b09610c1120d) solution which enables you to make the valuable customer insights lying in your data warehouse actionable by moving them to sales, markerting and support tools of your choice. We currently support all major data warehouses incluing **Snowflake**, **BigQuery**, **Redshift** and **Postgres** as the data source and some of the most popular sales, marketing and support solutions such as **Salesforce**, **Hubspot**, **Intercom**, **Google Ads** etc as the destination.

<p align="center">
  <img src="https://cdn.castled.io/content/readme/castled_overview.svg" alt="Castled-logo" width="800" />
</p>

Some of our key offerings include

- **Incremental** or **Full** data syncs from your cloud data warehouse.
- Automated failure handling and recovery.
- **Reliability** and **Resiliance** at scale.
- **CFL(Castled Form Language)** is a Java Annotation based UI framework, which can support the most complex UI forms from backend, thereby removing the prerequisite of having a UI developer to add a new connector.
- **Kubernetes** ready for easy deployment.

## Getting Started

Quick link: https://oss-docs.castled.io/deploying-castled/deploy-local

```
git clone https://github.com/castledio/castled.git
cd castled
docker-compose up
```

You can access castled app at http://localhost:3000. Happy castling!!!

- Quick startup demo

<p align="center">
  <a href="https://www.loom.com/embed/71bf33acbb4a41cab7c96a3460a84e5f">
      <img style="max-width:600px;" src="https://cdn.loom.com/sessions/thumbnails/2a611aef2bfb454fa026cb1489d5a859-with-play.gif"/>
  </a>
</p>

- Creating a pipeline with Castled

<p align="center">
  <a href="https://www.loom.com/embed/17bd25ed06cd4ca7a7215440606e2041">
      <img style="max-width:600px;" src="https://cdn.loom.com/sessions/thumbnails/17bd25ed06cd4ca7a7215440606e2041-with-play.gif"/>
  </a>
</p>

## Documentation

You can access the documentation at: https://oss-docs.castled.io/

## Community Support

- [Discord](https://discord.gg/7aJ3DWP9pz) - For any discussions or help needed in getting started with Castled or just to hang out.
- [Github](https://github.com/castledio/castled) - If you need to raise any bugs or feature requests please use Github. We will get back to you in no time!

## Performance

The throughput/latency of a data pipeline depends mostly on the destination api limitations and/or rate limits. We do make the best effort to tune it to provide the most optimal throughput possible. But to give an indication of the throughput we support, publishing a bench mark we did to transfer upto 100 million records from BigQuery to Apache Kafka.

![kafka benchmark](https://cdn.castled.io/content/readme/kafka_benchmark_shaded.png)

## Contribute to us

We are a huge fan of the open source community and we value your contributions. We have spent an insane amount of time customizing our framework to reduce the amount of time required to built a new connector. Here is all you need to do to add your own custom connector.

1. Implement java interfaces ([ExternalAppConnector](https://github.com/castledio/castled/blob/main/connectors/src/main/java/io/castled/apps/ExternalAppConnector.java) and [DataSink](https://github.com/castledio/castled/blob/main/connectors/src/main/java/io/castled/apps/DataSink.java)) on the backend.

2. Customize the mapping configuration and app configuration on the UI using our java annotation based framework. Refer the [mapping configuration](https://github.com/castledio/castled/blob/main/connectors/src/main/java/io/castled/apps/connectors/customerio/CustomerIOAppSyncConfig.java) and [app configuration](https://github.com/castledio/castled/blob/main/connectors/src/main/java/io/castled/apps/connectors/customerio/CustomerIOAppConfig.java) of our _CustomerIO_ connector for more info.

## License

Refer our [license](https://github.com/castledio/castled/blob/main/LICENSE.md) file for queries regarding the licensing of our different modules.
