package ai.chronon.online.perf

import org.apache.avro.Schema
import org.openjdk.jmh.annotations._
import io.vertx.core.json.JsonObject
import java.util.concurrent.TimeUnit
import io.vertx.core.json.Json
import io.vertx.core.spi.json.JsonCodec
import java.util
import scala.collection.JavaConverters._
import ai.chronon.online.AvroCodec
import org.apache.fury.Fury
import org.apache.fury.config.Language
import java.util.Base64

// To build: sbt 'online/jmh:compile'
// To run: sbt 'online/jmh:run -i 3 -wi 3 -f1 ai.chronon.online.perf.FetcherSerDePerfTest'
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class FetcherSerDePerfTest {

  private var featureMap: Map[String, Any] = _
  private var schema: Schema = _

  @Param(Array("100", "200", "250", "500", "1000"))
  private var size: String = _

  private var jsonCodec: JsonCodec = _
  private var avroCodec: AvroCodec = _
  private var furyCodec: Fury = _

  @Benchmark
  def benchmarkFeaturesJsonRoundTrip(): Unit = {
    val jsonString = jsonCodec.toString(featureMap.asJava)
    val decodedMap = jsonCodec.fromString(jsonString, classOf[util.Map[String, Any]])
  }

  @Benchmark
  def benchmarkFeaturesToAvroBase64RoundTrip(): Unit = {
    val avroBytes = avroCodec.encode(featureMap.asInstanceOf[Map[String, AnyRef]])
    val base64Bytes = Base64.getEncoder.encodeToString(avroBytes)
    val decodedMap = avroCodec.decodeMap(Base64.getDecoder.decode(base64Bytes))
  }

  @Benchmark
  def benchmarkFeaturesToAvroRoundTrip(): Unit = {
    val avroBytes = avroCodec.encode(featureMap.asInstanceOf[Map[String, AnyRef]])
    val decodedMap = avroCodec.decodeMap(avroBytes)
  }

  @Benchmark
  def benchmarkFeaturesToFuryRoundTrip(): Unit = {
    val furyBytes = furyCodec.serialize(featureMap.asInstanceOf[Map[String, AnyRef]])
    val decodedMap = furyCodec.deserialize(furyBytes).asInstanceOf[Map[String, AnyRef]]
  }

  @Setup
  def setup(): Unit = {
    val (map, avroSchema) = FeatureGenerator.testData(Integer.valueOf(size))
    featureMap = map
    schema = avroSchema
    jsonCodec = Json.CODEC
    avroCodec = AvroCodec.of(schema.toString)
    furyCodec = Fury.builder.withLanguage(Language.JAVA).requireClassRegistration(false).build
  }

  @TearDown
  def teardown(): Unit = {
    // Cleanup code that runs after each benchmark iteration
  }

}

