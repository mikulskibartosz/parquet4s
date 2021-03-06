package com.github.mjakubowski84.parquet4s

import java.nio.charset.StandardCharsets
import java.math.MathContext

import org.apache.parquet.io.api.{Binary, RecordConsumer}
import org.apache.parquet.schema.Type
import java.nio.ByteBuffer

/**
  * Basic structure element which Parquet data is built from. Represents any data element that can be read from or
  * can be written to Parquet files.
  */
trait Value extends Any {

  /**
    * Writes the value content to Parquet
    * @param schema schema of that value
    * @param recordConsumer has to be used to write the data to the file
    */
  def write(schema: Type, recordConsumer: RecordConsumer): Unit

}

/**
  * Primitive value like integer or long.
  * @tparam T type of the value
  */
trait PrimitiveValue[T] extends Any with Value {

  /**
    * Content of the value
    */
  def value: T

}

object StringValue {
  def apply(binary: Binary): StringValue = StringValue(binary.toStringUsingUTF8)
}

// TODO redundant structure.... we should use BinarrValue for storing strings and do conversions in codec
case class StringValue(value: String) extends AnyVal with PrimitiveValue[String] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit =
    recordConsumer.addBinary(Binary.fromReusedByteArray(value.getBytes(StandardCharsets.UTF_8)))
}

case class CharValue(value: Char) extends AnyVal with PrimitiveValue[Char] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit =
    recordConsumer.addInteger(value)
}

case class LongValue(value: Long) extends AnyVal with PrimitiveValue[Long] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = recordConsumer.addLong(value)
}

case class IntValue(value: Int) extends AnyVal with PrimitiveValue[Int] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = recordConsumer.addInteger(value)
}

case class FloatValue(value: Float) extends AnyVal with PrimitiveValue[Float] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = recordConsumer.addFloat(value)
}

case class DoubleValue(value: Double) extends AnyVal with PrimitiveValue[Double] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = recordConsumer.addDouble(value)
}

object DecimalValue {
  val Scale = 18
  val Precision = 38
  val ByteArrayLength = 16
  private lazy val RescaleMathContext = new MathContext(Precision)

  private def rescale(original: BigDecimal) = BigDecimal.decimal(original.bigDecimal, RescaleMathContext).setScale(Scale)

  def apply(binary: Binary, scale: Int, mathContext: MathContext): DecimalValue =
    DecimalValue(BigDecimal(BigInt(binary.getBytes), scale, mathContext))
}

case class DecimalValue(value: BigDecimal) extends AnyVal with PrimitiveValue[BigDecimal] {
  import DecimalValue._

  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = {
    /*
      Decimal is stored as byte array of unscaled BigInteger.
      Scale and precision is stored seperately in metadata.
      Value needs to be rescaled with default scale and precision for BigDecimal before saving. 
    */
    val buf = ByteBuffer.allocate(ByteArrayLength)
    val unscaled = rescale(value).bigDecimal.unscaledValue().toByteArray()
    // BigInteger is stored in tail of byte array, sign is stored in unoccupied cells
    val sign: Byte = if (unscaled.head < 0) -1 else 0
    (0 until ByteArrayLength - unscaled.length).foreach(_ => buf.put(sign))
    buf.put(unscaled)
    recordConsumer.addBinary(Binary.fromReusedByteArray(buf.array()))
  }
}

object ShortValue {
  def apply(value: Int): ShortValue = ShortValue(value.toShort)
}

case class ShortValue(value: Short) extends AnyVal with PrimitiveValue[Short] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = recordConsumer.addInteger(value)
}

object ByteValue {
  def apply(value: Int): ByteValue = ByteValue(value.toByte)
}

case class ByteValue(value: Byte) extends AnyVal with PrimitiveValue[Byte] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = recordConsumer.addInteger(value)
}

object BinaryValue {
  def apply(binary: Binary): BinaryValue = BinaryValue(binary.getBytes)
}

case class BinaryValue(value: Array[Byte]) extends PrimitiveValue[Array[Byte]] {

  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = recordConsumer.addBinary(Binary.fromReusedByteArray(value))

  override def equals(obj: Any): Boolean =
    obj match {
      case other @ BinaryValue(otherValue) =>
        (other canEqual this) && value.sameElements(otherValue)
      case _ => false
    }
}

case class BooleanValue(value: Boolean) extends AnyVal with PrimitiveValue[Boolean] {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit = recordConsumer.addBoolean(value)
}

/**
  * Special instance of [[Value]] that represents lack of the value.
  * [[NullValue]] does not hold any data so it cannot be written.
  */
case object NullValue extends Value {
  override def write(schema: Type, recordConsumer: RecordConsumer): Unit =
    throw new UnsupportedOperationException("Null values cannot be written.")
}
