package cz.vse.lhd.ontologycleanup.output

import java.io.{OutputStream, File}

import cz.vse.lhd.core.{RdfTriple, NTWriter, NTReader}
import cz.vse.lhd.ontologycleanup.{Conf, ManualMapping}

/**
 * Created by propan on 16. 5. 2015.
 */
class TypeOverrideOutput(input: File, manualMapping: ManualMapping) extends OutputMaker with OutputMakerHeader with OutputMakerFooter {

  private var mappedCount = 0
  private var notMappedCount = 0

  val header = "# Some types replaced according manually defined mapping"
  lazy val footer = "# DBpedia types overriden = " + mappedCount + ", not overriden = " + notMappedCount

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    NTWriter.fromIterator(
      it.map { stmt =>
        val overridenObject = manualMapping.getOverridingType(stmt.getObject.asResource().getURI)
        if (overridenObject != null) {
          mappedCount = mappedCount + 1
          RdfTriple(stmt).copy(`object` = overridenObject).toStatement
        } else {
          notMappedCount = notMappedCount + 1
          stmt
        }
      },
      output
    )
  }

}