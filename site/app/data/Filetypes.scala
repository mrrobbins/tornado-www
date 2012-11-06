
package data

import play.api.libs.{MimeTypes=>PlayMimeTypes}

object FileTypes {

	val MimeTypes = PlayMimeTypes
	
	// default mapping (extension -> MimeType) is M:1
	// Reverse mapping, filter to shortest ext for a given MimeType
	val Extensions = {
		val typeList = MimeTypes.types.toList
		val swapped = typeList.map(_.swap)
		swapped.sortBy(_._2.length).reverse
	}.toMap

}
