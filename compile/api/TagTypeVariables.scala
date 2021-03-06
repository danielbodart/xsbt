package xsbt.api

import xsbti.api._

object TagTypeVariables
{
	def apply(s: Source): scala.collection.Map[Int, (Int, Int)] = (new TagTypeVariables).tag(s)
}
private class TagTypeVariables extends NotNull
{
	private val tags = new scala.collection.mutable.HashMap[Int, (Int, Int)]
	private var level = 0
	private var index = 0

	def tag(s: Source): scala.collection.Map[Int, (Int, Int)] =
	{
		s.definitions.foreach(tagDefinition)
		tags
	}
	def tagDefinitions(ds: Seq[Definition]) = ds.foreach(tagDefinition)
	def tagDefinition(d: Definition)
	{
		d match
		{
			case c: ClassLike =>  tagClass(c)
			case f: FieldLike => tagField(f)
			case d: Def => tagDef(d)
			case t: TypeDeclaration => tagTypeDeclaration(t)
			case t: TypeAlias => tagTypeAlias(t)
		}
	}
	def tagClass(c: ClassLike): Unit =
		tagParameterizedDefinition(c) {
			tagType(c.selfType)
			tagStructure(c.structure)
		}
	def tagField(f: FieldLike)
	{
		tagType(f.tpe)
		tagAnnotations(f.annotations)
	}
	def tagDef(d: Def): Unit =
		tagParameterizedDefinition(d) {
			tagValueParameters(d.valueParameters)
			tagType(d.returnType)
		}
	def tagValueParameters(valueParameters: Seq[ParameterList]) = valueParameters.foreach(tagValueParameterList)
	def tagValueParameterList(list: ParameterList) = list.parameters.foreach(tagValueParameter)
	def tagValueParameter(parameter: MethodParameter) = tagType(parameter.tpe)
		
	def tagParameterizedDefinition[T <: ParameterizedDefinition](d: T)(tagExtra: => Unit)
	{
		tagAnnotations(d.annotations)
		scope {
			tagTypeParameters(d.typeParameters)
			tagExtra
		}
	}
	def tagTypeDeclaration(d: TypeDeclaration): Unit =
		tagParameterizedDefinition(d) {
			tagType(d.lowerBound)
			tagType(d.upperBound)
		}
	def tagTypeAlias(d: TypeAlias): Unit =
		tagParameterizedDefinition(d) {
			tagType(d.tpe)
		}
	
	def tagTypeParameters(parameters: Seq[TypeParameter]) = parameters.foreach(tagTypeParameter)
	def tagTypeParameter(parameter: TypeParameter)
	{
		recordTypeParameter(parameter.id)
		scope {
			tagTypeParameters(parameter.typeParameters)
			tagType(parameter.lowerBound)
			tagType(parameter.upperBound)
		}
	}
	def tagAnnotations(annotations: Seq[Annotation]) = tagTypes(annotations.map(_.base))
	
	def tagTypes(ts: Seq[Type]) = ts.foreach(tagType)
	def tagType(t: Type)
	{
		t match
		{
			case s: Structure => tagStructure(s)
			case e: Existential => tagExistential(e)
			case p: Polymorphic => tagPolymorphic(p)
			case a: Annotated => tagAnnotated(a)
			case p: Parameterized => tagParameterized(p)
			case p: Projection => tagProjection(p)
			case _: EmptyType | _: Singleton | _: ParameterRef => ()
		}
	}
	
	def tagExistential(e: Existential) = tagParameters(e.clause, e.baseType)
	def tagPolymorphic(p: Polymorphic) = tagParameters(p.parameters, p.baseType)
	def tagProjection(p: Projection) = tagType(p.prefix)
	def tagParameterized(p: Parameterized)
	{
		tagType(p.baseType)
		tagTypes(p.typeArguments)
	}
	def tagAnnotated(a: Annotated)
	{
		tagType(a.baseType)
		tagAnnotations(a.annotations)
	}
	def tagStructure(structure: Structure)
	{
		tagTypes(structure.parents)
		tagDefinitions(structure.declared)
		tagDefinitions(structure.inherited)
	}
	def tagParameters(parameters: Seq[TypeParameter], base: Type): Unit =
		scope {
			tagTypeParameters(parameters)
			tagType(base)
		}
	
	def scope(action: => Unit)
	{
		val saveIndex = index
		index = 0
		level += 1
		
		action
		
		level -= 1
		index = saveIndex
	}
	def recordTypeParameter(id: Int)
	{
		tags(id) = (level, index)
		index += 1
	}
}