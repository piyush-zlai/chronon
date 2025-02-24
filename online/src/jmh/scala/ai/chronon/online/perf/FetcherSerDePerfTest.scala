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
import java.util.Base64

// To build: sbt 'online/jmh:compile'
// To run: sbt 'online/jmh:run -i 10 -wi 5 -f1 ai.chronon.online.perf.FetcherSerDePerfTest'
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

  @Benchmark
  def benchmarkFeaturesJsonRoundTrip(): Unit = {
    val jsonString = jsonCodec.toString(featureMap.asJava)
    jsonString.size
    //println(s"##### Size of json string: ${jsonString.size}; Size of map ${featureMap.size}")
    val decodedMap = jsonCodec.fromString(jsonString, classOf[util.Map[String, Any]])
//    println(s"##### Size of map ${decodedMap.size}")
  }

  @Benchmark
  def benchmarkFeaturesToAvroRoundTrip(): Unit = {
    val avroBytes = avroCodec.encode(featureMap.asInstanceOf[Map[String, AnyRef]])
    //println(s"##### Size of avro bytes ${avroBytes.size}")
    val base64Bytes = Base64.getEncoder.encodeToString(avroBytes)
    //println(s"##### Size of base64 string: ${base64Bytes.size}; Size of map ${featureMap.size}")
    val decodedMap = avroCodec.decodeMap(Base64.getDecoder.decode(base64Bytes))
//    println(s"##### Size of map ${decodedMap.size}")
  }

  @Setup
  def setup(): Unit = {
    val (map, avroSchema) = FeatureGenerator.testData(Integer.valueOf(size))
    featureMap = map
    schema = avroSchema
    jsonCodec = Json.CODEC
    avroCodec = AvroCodec.of(schema.toString)
  }

  @TearDown
  def teardown(): Unit = {
    // Cleanup code that runs after each benchmark iteration
  }

}

