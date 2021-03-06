/* sbt -- Simple Build Tool
 * Copyright 2009 Mark Harrah
 */
package sbt

import sbinary.{CollectionTypes, Format, JavaFormats}
import java.io.File
import Types.:+:

trait Cache[I,O]
{
	def apply(file: File)(i: I): Either[O, O => Unit]
}
trait SBinaryFormats extends CollectionTypes with JavaFormats with NotNull
{
	//TODO: add basic types from SBinary minus FileFormat
}
object Cache extends BasicCacheImplicits with SBinaryFormats with HListCacheImplicits
{
	def cache[I,O](implicit c: Cache[I,O]): Cache[I,O] = c
	def outputCache[O](implicit c: OutputCache[O]): OutputCache[O] = c
	def inputCache[O](implicit c: InputCache[O]): InputCache[O] = c

	def wrapInputCache[I,DI](implicit convert: I => DI, base: InputCache[DI]): InputCache[I] =
		new WrappedInputCache(convert, base)
	def wrapOutputCache[O,DO](implicit convert: O => DO, reverse: DO => O, base: OutputCache[DO]): OutputCache[O] =
		new WrappedOutputCache[O,DO](convert, reverse, base)

	def cached[I,O](file: File)(f: I => O)(implicit cache: Cache[I,O]): I => O =
		in =>
			cache(file)(in) match
			{
				case Left(value) => value
				case Right(store) =>
					val out = f(in)
					store(out)
					out
			}
}
trait BasicCacheImplicits extends NotNull
{
	implicit def basicInputCache[I](implicit format: Format[I], equiv: Equiv[I]): InputCache[I] =
		new BasicInputCache(format, equiv)
	implicit def basicOutputCache[O](implicit format: Format[O]): OutputCache[O] =
		new BasicOutputCache(format)

	implicit def ioCache[I,O](implicit input: InputCache[I], output: OutputCache[O]): Cache[I,O] =
		new SeparatedCache(input, output)
	implicit def defaultEquiv[T]: Equiv[T] = new Equiv[T] { def equiv(a: T, b: T) = a == b }
}
trait HListCacheImplicits
{
	implicit def hConsInputCache[H,T<:HList](implicit headCache: InputCache[H], tailCache: InputCache[T]): InputCache[H :+: T] =
		new HConsInputCache(headCache, tailCache)
	implicit lazy val hNilInputCache: InputCache[HNil] = new HNilInputCache

	implicit def hConsOutputCache[H,T<:HList](implicit headCache: OutputCache[H], tailCache: OutputCache[T]): OutputCache[H :+: T] =
		new HConsOutputCache(headCache, tailCache)
	implicit lazy val hNilOutputCache: OutputCache[HNil] = new HNilOutputCache
}
