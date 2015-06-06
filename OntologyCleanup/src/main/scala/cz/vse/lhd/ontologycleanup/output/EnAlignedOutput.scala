package cz.vse.lhd.ontologycleanup.output

import java.io.{File, OutputStream}

import cz.vse.lhd.core.{NTReader, NTWriter, RdfTriple}
import cz.vse.lhd.ontologycleanup.{Conf, LanguageMapping}

/**
 * Created by propan on 18. 5. 2015.
 */
class EnAlignedOutput(input: File) extends OutputMaker with OutputMakerHeader with OutputMakerFooter {

  private var mappedCount = 0
  private var notMappedCount = 0

  val header = "# Input file with objects replaced by their equivalents in English Wikipedia, if they exist"
  lazy val footer = "# DBpedia mapped = " + mappedCount + ", not mapped = " + notMappedCount

  def makeFile(output: OutputStream) = NTReader.fromFile(input) { it =>
    LanguageMapping.use { languageMapping =>
      NTWriter.fromIterator(
        it.map { stmt =>
          val stmtObject = stmt.getObject.asResource().getURI
          languageMapping.englishResource(stmtObject) match {
            case Some(englishResource) =>
              mappedCount = mappedCount + 1
              RdfTriple(stmt).copy(`object` = englishResource).toStatement
            case None =>
              notMappedCount = notMappedCount + 1
              stmt
          }
        },
        output
      )
    }
  }

}
