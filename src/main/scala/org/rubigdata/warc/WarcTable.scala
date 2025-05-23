package org.rubigdata.warc

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.catalog.{Column, SupportsRead, Table, TableCapability}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.types.{ArrayType, MapType, StringType, StructField, StructType, TimestampType}
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util
import scala.collection.JavaConverters.setAsJavaSetConverter

import WarcRow._

class WarcTable(options: WarcOptions) extends Table with SupportsRead {

  lazy val sparkSession: SparkSession = SparkSession.active

  override def name(): String = s"warc [${options.path}]"

  override def schema(): StructType = getSchema

  override def columns(): Array[Column] = columnsFromSchema(getSchema)

  override def capabilities(): util.Set[TableCapability] = Set(TableCapability.BATCH_READ).asJava

  override def newScanBuilder(options: CaseInsensitiveStringMap): ScanBuilder = new WarcScanBuilder(sparkSession, this.options, getSchema)

  private def columnsFromSchema(schema: StructType): Array[Column] = {
    schema.fields.map(f => Column.create(f.name, f.dataType, f.nullable))
  }

  def getSchema: StructType = {
    val defaultFields = Seq(
      StructField(WARC_ID, StringType, nullable = false),
      StructField(WARC_TYPE, StringType, nullable = false),
      StructField(WARC_TARGET_URI, StringType, nullable = true),
      StructField(WARC_DATE, TimestampType, nullable = false),
      StructField(WARC_CONTENT_TYPE, StringType, nullable = false),
      StructField(WARC_HEADERS, MapType(StringType, ArrayType(StringType, containsNull = false), valueContainsNull = false), nullable = false),
    )

    val additionalFields = if (options.parseHTTP) {
      Seq(
        StructField(HTTP_CONTENT_TYPE, StringType, nullable = true),
        StructField(HTTP_HEADERS, MapType(StringType, ArrayType(StringType, containsNull = false), valueContainsNull = false), nullable = true),
        StructField(HTTP_BODY, StringType, nullable = true)
      )
    } else {
      Seq(StructField(WARC_BODY, StringType, nullable = false))
    }

    StructType(defaultFields ++ additionalFields)
  }

}
