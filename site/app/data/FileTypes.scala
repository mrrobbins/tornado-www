
package data

import play.api.libs.{MimeTypes=>PlayMimeTypes}

object FileTypes {

	/** Maps from file extension to MIME type
	  */
	val MimeTypes = PlayMimeTypes.types
	
	/** Maps from MIME type to shortest file extension
	  */
	val Extensions = {
		// default mapping (extension -> MimeType) is M:1
		// Reverse mapping, filter to shortest ext for a given MimeType
		val typeList = MimeTypes.toList
		val swapped = typeList.map(_.swap)
		swapped.sortBy(_._2.length).reverse
	}.toMap

}
