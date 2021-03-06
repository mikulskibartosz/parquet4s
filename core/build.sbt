libraryDependencies ++= {
  val parquetVersion = "1.10.1"
  val sparkVersion = "2.4.0"
  val hadoopVersion = "2.9.2"
  Seq(
    "org.apache.parquet" % "parquet-hadoop" % parquetVersion,
    "org.apache.hadoop" % "hadoop-client" % hadoopVersion,
    "com.chuusai" %% "shapeless" % "2.3.3",

    // tests
    "org.scalamock" %% "scalamock" % "4.1.0" % "test",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test,it",
    "org.apache.spark" %% "spark-core" % sparkVersion % "it"
      exclude(org = "org.apache.hadoop", name = "hadoop-client"),
    "org.apache.spark" %% "spark-sql" % sparkVersion % "it"
      exclude(org = "org.apache.hadoop", name = "hadoop-client"),
    "ch.qos.logback" % "logback-classic" % "1.2.3" % "test,it"
  )
}
