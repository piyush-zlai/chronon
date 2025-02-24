package ai.chronon.online.perf

import ai.chronon.online.AvroConversions
import ai.chronon.api.{BooleanType, DataType, IntType, LongType, StringType, StructType}
import org.apache.avro.{Schema, SchemaBuilder}

import scala.util.Random

object FeatureGenerator {
  private val featureTypes = List(
    ("price", () => Random.nextInt(10000)),
    ("shop_id", () => s"Id${Random.nextInt(99999999)}"),
    ("count", () => Random.nextInt(10000)),
    ("name", () => s"Shop${Random.nextInt(1000)}"),
    ("active", () => Random.nextInt(2) == 1),
    ("quantity", () => Random.nextInt(1000)),
    ("days", () => Random.nextInt(30)),
    ("country_id", () => Random.nextInt(200)),
    ("favorites", () => Random.nextInt(5000))
  )

  private val prefixes = List(
    "search_cdc_listings_attributes",
    "search_cdc_shipping_profile_attributes",
    "search_beacons_listing_actions",
    "search_cdc_shop_data_attributes"
  )

  private val suffixes = List(
    "last",
    "1d",
    "7d",
    "30d"
  )

  def generateUniqueFeatureName(usedNames: Set[String]): String = {
    var name = ""
    do {
      val prefix = prefixes(Random.nextInt(prefixes.length))
      val (featureType, _) = featureTypes(Random.nextInt(featureTypes.length))
      val suffix = suffixes(Random.nextInt(suffixes.length))
      val counter = Random.nextInt(1000000)  // Add uniqueness
      name = s"${prefix}_${featureType}_${counter}_${suffix}"
    } while (usedNames.contains(name))
    name
  }

  def generateFeatureValue(allowNull: Boolean = true): Any = {
    if (allowNull && Random.nextFloat() < 0.1) {
      null
    } else {
      val (_, generator) = featureTypes(Random.nextInt(featureTypes.length))
      generator()
    }
  }

  def generateFeatureMap(size: Int): Map[String, Any] = {
    var features = Map[String, Any]()
    var usedNames = Set[String]()

    while (features.size < size) {
      val name = generateUniqueFeatureName(usedNames)
      val value = generateFeatureValue(allowNull = false)
      features += (name -> value)
      usedNames += name
    }

    features
  }

  def generateAvroSchema(featureMap: Map[String, Any]): Schema = {
    val fields: Array[(String, DataType)] =
      featureMap.map { case (name, value) =>
        val fieldType = value match {
          case _: Int => IntType
          case _: Long => LongType
          case _: String => StringType
          case _: Boolean => BooleanType
          case _ => StringType // fallback
        }
        (name, fieldType)
      }.toArray

    val chrononSchema: StructType =
      StructType.from("Features", fields)
    AvroConversions.fromChrononSchema(chrononSchema)
  }

  // Generate sample data sets of different sizes
  val testSizes = List(100, 200, 250, 500, 1000)

  lazy val testData: Map[Int, (Map[String, Any], Schema)] = testSizes.map { size =>
    val featureMap = generateFeatureMap(size)
    val schema = generateAvroSchema(featureMap)
    size -> (featureMap, schema)
  }.toMap
}